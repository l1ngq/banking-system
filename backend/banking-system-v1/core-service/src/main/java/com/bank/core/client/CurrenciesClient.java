package com.bank.core.client;

import com.bank.common.dto.UniversalResponse;
import com.bank.core.dto.ConversionResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(
        name = "currencies-service",
        url = "${currencies.service.url}"
)
public interface CurrenciesClient {

    @GetMapping("/api/currencies/convert")
    UniversalResponse<ConversionResult> convert(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            @RequestParam("amount") BigDecimal amount
    );
}
