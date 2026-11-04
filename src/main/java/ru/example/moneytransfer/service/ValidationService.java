package ru.example.moneytransfer.service;

import ru.example.moneytransfer.model.TransferRequest;
import org.springframework.stereotype.Component;

/**
 * Проверка входных данных (этап 3): алгоритм Луна, срок действия, CVV, сумма.
 * Этап 2 — каркас с базовой проверкой на null.
 */
@Component
public class ValidationService {

    public void validate(TransferRequest request) {
        if (request == null
                || isBlank(request.cardFromNumber())
                || isBlank(request.cardToNumber())
                || isBlank(request.cardFromCVV())
                || isBlank(request.cardFromValidTill())
                || request.amount() == null) {
            throw new ru.example.moneytransfer.exception.ErrorInputData("Не заполнены данные перевода");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
