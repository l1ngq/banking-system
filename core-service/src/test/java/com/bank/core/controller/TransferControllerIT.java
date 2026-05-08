package com.bank.core.controller;

import com.bank.common.enums.Currency;
import com.bank.core.BaseTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TransferControllerIT extends BaseTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void transferChangesBalancesAndCanBeCheckedThroughMyAccounts() throws Exception {
        UUID receiverId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        createUser(USER_ID, "user@bank.com");
        createUser(receiverId, "receiver@bank.com");
        var fromAccount = createAccount(USER_ID, Currency.USD, new BigDecimal("1000.00"));
        var toAccount = createAccount(receiverId, Currency.USD, BigDecimal.ZERO);

        String body = """
                {"fromAccountId":%d,"toAccountId":%d,"amount":100.00,"currency":"USD"}
                """.formatted(fromAccount.getId(), toAccount.getId());

        ResponseEntity<String> transferResponse = restTemplate.exchange(
                "/api/transfers",
                HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(USER_TOKEN)),
                String.class);

        assertThat(transferResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(transferResponse.getBody()).contains("\"code\":0");

        ResponseEntity<String> accountsResponse = restTemplate.exchange(
                "/api/accounts/my",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(USER_TOKEN)),
                String.class);
        JsonNode root = objectMapper.readTree(accountsResponse.getBody());

        assertThat(accountsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(root.path("data").path("accounts").get(0).path("balance").decimalValue())
                .isEqualByComparingTo("900.00");
    }

    @Test
    void transferReturnsUnprocessableEntityWhenInsufficientFunds() {
        UUID receiverId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        createUser(USER_ID, "user@bank.com");
        createUser(receiverId, "receiver@bank.com");
        var fromAccount = createAccount(USER_ID, Currency.USD, new BigDecimal("50.00"));
        var toAccount = createAccount(receiverId, Currency.USD, BigDecimal.ZERO);

        String body = """
                {"fromAccountId":%d,"toAccountId":%d,"amount":100.00,"currency":"USD"}
                """.formatted(fromAccount.getId(), toAccount.getId());

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/transfers",
                HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(USER_TOKEN)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).contains("\"code\":4220");
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
