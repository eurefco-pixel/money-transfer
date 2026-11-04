package ru.example.moneytransfer.service;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Генерация operationId (UUID) и 4-значного кода подтверждения. */
@Component
public class CodeGenerator {

    public String newOperationId() {
        return UUID.randomUUID().toString();
    }

    public String newConfirmationCode() {
        return String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
