package ru.example.moneytransfer.model;

import jakarta.validation.constraints.NotBlank;

public record ConfirmationRequest(
        @NotBlank(message = "operationId не заполнен")
        String operationId,

        @NotBlank(message = "Код подтверждения не заполнен")
        String code
) {
}