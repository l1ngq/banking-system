package com.bank.core.controller;

import com.bank.core.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AccountControllerIT extends BaseTest {

    @Test
    void createAccountWithUserTokenReturnsSuccessResponse() {
        HttpHeaders headers = bearerHeaders(USER_TOKEN);
        String body = """
                {"currency":"USD","type":"CHECKING"}
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/accounts",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"code\":0");
        assertThat(bankAccountRepository.findAll()).hasSize(1);
    }

    @Test
    void createAccountWithoutTokenReturnsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"currency":"USD","type":"CHECKING"}
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/accounts",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminEndpointWithUserTokenReturnsForbidden() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/admin/accounts",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(USER_TOKEN)),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
