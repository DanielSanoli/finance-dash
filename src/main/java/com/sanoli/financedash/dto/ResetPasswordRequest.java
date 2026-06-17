package com.sanoli.financedash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "token é obrigatório")
        String token,

        @NotBlank(message = "password é obrigatório")
        @Size(min = 8, message = "password deve ter pelo menos 8 caracteres")
        String password
) {
}
