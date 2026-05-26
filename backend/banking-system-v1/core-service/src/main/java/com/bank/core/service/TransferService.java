package com.bank.core.service;

import com.bank.common.dto.UniversalResponse;
import com.bank.common.enums.AccountStatus;
import com.bank.common.enums.TransactionStatus;
import com.bank.common.enums.TransactionType;
import com.bank.common.event.TransactionEvent;
import com.bank.common.exception.ConflictException;
import com.bank.common.exception.CurrencyServiceUnavailableException;
import com.bank.common.exception.InsufficientFundsException;
import com.bank.common.exception.NotFoundException;
import com.bank.core.client.CurrenciesClient;
import com.bank.core.dto.ConversionResult;
import com.bank.core.dto.TransactionDto;
import com.bank.core.dto.TransferRequest;
import com.bank.core.entity.BankAccountEntity;
import com.bank.core.entity.TransactionEntity;
import com.bank.core.kafka.producer.TransactionEventProducer;
import com.bank.core.mapper.TransactionMapper;
import com.bank.core.repository.BankAccountRepository;
import com.bank.core.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Слой бизнес логики переводов
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransferService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final CurrenciesClient currenciesClient;
    private final TransactionEventProducer transactionEventProducer;

    public UniversalResponse<TransactionDto> transfer(TransferRequest request, UUID currentUserId) {
        log.info("Request to transfer: {}", request);

        Long fromAccountId = request.getFromAccountId();
        String toAccountNumber = normalizeAccountNumber(request.getToAccountNumber());
        BankAccountEntity recipientAccount = bankAccountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> new NotFoundException("Счёт получателя не найден"));
        Long toAccountId = recipientAccount.getId();

        if (fromAccountId.equals(toAccountId)) {
            throw new ConflictException("Нельзя выполнить перевод на тот же самый счёт");
        }

        Long firstId = Math.min(fromAccountId, toAccountId);
        Long secondId = Math.max(fromAccountId, toAccountId);

        BankAccountEntity first = bankAccountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new NotFoundException("Счёт не найден: " + firstId));
        BankAccountEntity second = bankAccountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new NotFoundException("Счёт не найден: " + secondId));

        BankAccountEntity fromAccount = first.getId().equals(fromAccountId) ? first : second;
        BankAccountEntity toAccount = first.getId().equals(fromAccountId) ? second : first;

        if (!fromAccount.getUserId().equals(currentUserId)) {
            throw new ConflictException("Счёт не принадлежит текущему пользователю");
        }

        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new ConflictException("Счёт отправителя закрыт");
        }

        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new ConflictException("Счёт получателя закрыт");
        }

        if (!request.getCurrency().equals(fromAccount.getCurrency())) {
            throw new ConflictException("Валюта перевода не совпадает с валютой счёта отправителя");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Недостаточно средств");
        }

        BigDecimal convertedAmount = request.getAmount();
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            convertedAmount = convertCurrency(
                    fromAccount.getCurrency().name(),
                    toAccount.getCurrency().name(),
                    request.getAmount());
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(convertedAmount));

        TransactionEntity transaction = TransactionEntity.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(request.getAmount())
                .convertedAmount(convertedAmount)
                .currency(fromAccount.getCurrency())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .build();
        transaction = transactionRepository.save(transaction);
        transactionEventProducer.send(TransactionEvent.builder()
                .transactionId(transaction.getId())
                .userId(currentUserId)
                .amount(request.getAmount())
                .currency(fromAccount.getCurrency().name())
                .type(TransactionType.TRANSFER.name())
                .timestamp(Instant.now())
                .build());

        return new UniversalResponse<>(transactionMapper.toDto(transaction));
    }

    public UniversalResponse<List<TransactionDto>> getHistory(Long accountId, UUID currentUserId) {
        log.info("Request to get transfer history by accountId: {}", accountId);

        BankAccountEntity account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Счёт не найден: " + accountId));

        if (!account.getUserId().equals(currentUserId)) {
            throw new ConflictException("Счёт не принадлежит текущему пользователю");
        }

        List<TransactionDto> transactions = transactionRepository
                .findAllByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId)
                .stream()
                .map(transactionMapper::toDto)
                .toList();
        return new UniversalResponse<>(transactions);
    }

    private BigDecimal convertCurrency(String from, String to, BigDecimal amount) {
        try {
            UniversalResponse<ConversionResult> response = currenciesClient.convert(from, to, amount);
            if (response == null || response.getData() == null) {
                throw new CurrencyServiceUnavailableException(
                        "Сервис конвертации вернул пустой ответ"
                );
            }
            return response.getData().getConvertedAmount();
        } catch (CurrencyServiceUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Currencies service недоступен: {}", ex.getMessage());
            throw new CurrencyServiceUnavailableException(
                    "Сервис конвертации недоступен, попробуйте позже",
                    ex
            );
        }
    }

    private String normalizeAccountNumber(String accountNumber) {
        return accountNumber == null ? null : accountNumber.replaceAll("\\s+", "");
    }
}
