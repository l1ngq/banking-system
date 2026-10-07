package com.bank.core.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RenameAccountRequest {

    @Size(max = 40, message = "Название счёта не может быть длиннее 40 символов")
    private String displayName;
}