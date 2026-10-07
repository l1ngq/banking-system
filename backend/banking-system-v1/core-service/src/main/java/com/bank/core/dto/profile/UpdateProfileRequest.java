package com.bank.core.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Имя не может быть пустым")
    @Size(max = 60, message = "Имя не может быть длиннее 60 символов")
    private String fullName;

    @Size(max = 20, message = "Телефон не может быть длиннее 20 символов")
    private String phone;

    @Size(max = 60, message = "Город не может быть длиннее 60 символов")
    private String city;
}
