package com.bank.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AccountListDto {

    private List<AccountDto> accounts;
    private int total;
}
