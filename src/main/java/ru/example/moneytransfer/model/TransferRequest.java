package ru.example.moneytransfer.model;

/**
 * Тело POST /transfer — строго по OpenAPI-спецификации.
 */
public record TransferRequest(
        String cardFromNumber,
        String cardFromValidTill,
        String cardFromCVV,
        String cardToNumber,
        Amount amount
) {
}