package ru.example.moneytransfer.model;

/** Карта в хранилище. Баланс — в копейках. */
public record Card(
        String cardNumber,
        String validTill,
        String cvv,
        String currency,
        long balance
) {
}
