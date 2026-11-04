package ru.example.moneytransfer.model;

/** Тело POST /confirmOperation. */
public record ConfirmationRequest(String operationId, String code) {
}
