package ru.example.moneytransfer.model;

/** Жизненный цикл операции перевода. */
public enum OperationStatus {
    PENDING_CONFIRMATION, COMPLETED, FAILED, EXPIRED
}
