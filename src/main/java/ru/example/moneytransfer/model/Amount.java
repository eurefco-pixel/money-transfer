package ru.example.moneytransfer.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record Amount(
        @Positive(message = "Сумма перевода должна быть больше нуля")
        long value,

        @NotBlank(message = "Валюта не указана")
        String currency
) {
}