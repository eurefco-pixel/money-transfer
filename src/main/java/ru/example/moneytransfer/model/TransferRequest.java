package ru.example.moneytransfer.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Тело POST /transfer — строго по OpenAPI-спецификации.
 * Формат полей проверяется Bean Validation, бизнес-правила — в ValidationService.
 */
public record TransferRequest(
        @NotBlank(message = "Карта отправителя: номер карты не заполнен")
        @Pattern(regexp = "\\d{4}( ?\\d{4}){3}", message = "Карта отправителя: номер карты должен состоять из 16 цифр")
        String cardFromNumber,

        @NotNull(message = "Срок действия должен быть в формате ММ/ГГ")
        @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "Срок действия должен быть в формате ММ/ГГ")
        String cardFromValidTill,

        @NotNull(message = "CVV должен состоять из 3 цифр")
        @Pattern(regexp = "\\d{3}", message = "CVV должен состоять из 3 цифр")
        String cardFromCVV,

        @NotBlank(message = "Карта получателя: номер карты не заполнен")
        @Pattern(regexp = "\\d{4}( ?\\d{4}){3}", message = "Карта получателя: номер карты должен состоять из 16 цифр")
        String cardToNumber,

        @NotNull(message = "Сумма перевода не указана")
        @Valid
        Amount amount
) {
}