package com.sanoli.financedash.dto;

import com.sanoli.financedash.domain.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CategoryRequest(
        @NotBlank(message = "name é obrigatório")
        String name,

        @NotNull(message = "type é obrigatório")
        TransactionType type,

        @NotBlank(message = "color é obrigatório")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "color deve estar no formato hexadecimal #RRGGBB")
        String color
) {
}

