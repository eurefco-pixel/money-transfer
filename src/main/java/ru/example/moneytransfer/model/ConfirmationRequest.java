package ru.example.moneytransfer.model;

public record ConfirmationRequest (String operationId, String code) {
}