package ru.example.moneytransfer.model;

import java.time.LocalDateTime;

/** Незавершённая/завершённая операция перевода (реестр в памяти). */
public record Operation(
        String operationId,
        String cardFromNumber,
        String cardToNumber,
        long amountValue,      // копейки
        String currency,
        long commission,       // копейки
        String code,           // код подтверждения
        OperationStatus status,
        LocalDateTime createdAt
) {
}
