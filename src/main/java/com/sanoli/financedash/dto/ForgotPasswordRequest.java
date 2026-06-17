package com.sanoli.financedash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "email é obrigatório")
        @Email(message = "email deve ser válido")
        String email
) {
}
