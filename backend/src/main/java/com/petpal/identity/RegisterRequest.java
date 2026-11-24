package com.petpal.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @Pattern(regexp = "^([+0-9 ()-]{6,30})?$", message = "must contain only digits, spaces, +, - and brackets")
        String phone,
        @NotBlank @Size(min = 2, max = 80) String city,
        @NotNull AccountType accountType) {
}
