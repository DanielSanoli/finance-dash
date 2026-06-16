package com.sanoli.financedash.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "name é obrigatório")
        String name,

        @NotBlank(message = "email é obrigatório")
        @Email(message = "email deve ser válido")
        String email,

        @NotBlank(message = "password é obrigatório")
        @Size(min = 8, message = "password deve ter pelo menos 8 caracteres")
        String password
) {
}
