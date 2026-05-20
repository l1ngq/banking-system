package com.bank.currencies.repository;

import com.bank.currencies.entity.ExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, Long> {

    Optional<ExchangeRateEntity> findByBaseCurrencyAndTargetCurrency(
            String baseCurrency,
            String targetCurrency);

    List<ExchangeRateEntity> findAll();
}
