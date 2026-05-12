package com.bank.currencies.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FrankfurterClient {

    private static final String BASE_URL = "https://api.frankfurter.app";

    private final WebClient.Builder webClientBuilder;

    public Map<String, BigDecimal> fetchRates(String base, List<String> targets) {
        log.info("Запрос курсов к {} для {}", base, targets);
        String symbols = String.join(",", targets);

        try {
            FrankfurterResponse response = webClientBuilder
                    .baseUrl(BASE_URL)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/latest")
                            .queryParam("from", base)
                            .queryParam("to", symbols)
                            .build())
                    .retrieve()
                    .bodyToMono(FrankfurterResponse.class)
                    .block();

            if (response == null || response.rates() == null) {
                return Collections.emptyMap();
            }
            return response.rates();
        } catch (Exception ex) {
            log.error("Ошибка при запросе курсов к {} для {}", base, targets, ex);
            return Collections.emptyMap();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FrankfurterResponse(Map<String, BigDecimal> rates) {
    }
}
