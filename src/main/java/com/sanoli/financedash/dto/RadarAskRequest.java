package com.sanoli.financedash.dto;

import jakarta.validation.constraints.NotBlank;

public record RadarAskRequest(
        @NotBlank(message = "question é obrigatório")
        String question
) {
}
