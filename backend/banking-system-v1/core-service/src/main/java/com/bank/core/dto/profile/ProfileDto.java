package com.bank.core.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {

    private String fullName;
    private String phone;
    private String city;
    private String email;
    private String role;
}
