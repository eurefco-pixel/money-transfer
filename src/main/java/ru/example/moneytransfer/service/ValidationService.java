package ru.example.moneytransfer.service;

import org.springframework.stereotype.Component;
import ru.example.moneytransfer.exception.ErrorInputData;
import ru.example.moneytransfer.exception.InvalidAmountException;
import ru.example.moneytransfer.exception.InvalidCardNumberException;
import ru.example.moneytransfer.exception.InvalidCvvException;
import ru.example.moneytransfer.exception.InvalidValidTillException;
import ru.example.moneytransfer.model.Amount;
import ru.example.moneytransfer.model.TransferRequest;

import java.time.YearMonth;

/**
 * Проверка входных данных (этап 3): алгоритм Луна, срок действия, CVV, сумма.
 * Любое нарушение формата -> ErrorInputData -> HTTP 400 {message, id}.
 * Проверки существования карты, баланса и валюты — в TransferService (HTTP 500).
 */
@Component
public class ValidationService {

    /** Публичная точка входа: вызывает все проверки по порядку. */
    public void validate(TransferRequest request) {
        if (request == null) {
            throw new ErrorInputData("Запрос не заполнен");
        }
        validateCardNumber(request.cardFromNumber(), "Карта отправителя");
        validateCardNumber(request.cardToNumber(), "Карта получателя");
        validateValidTill(request.cardFromValidTill());
        validateCvv(request.cardFromCVV());
        validateAmount(request.amount());
    }

    void validateCardNumber(String number, String label) {
        if (isBlank(number)) {
            throw new InvalidCardNumberException(label + ": номер карты не заполнен");
        }
        // Пробелы в номере нормализуем: люди копируют номера с пробелами
        String digits = number.replace(" ", "");
        if (!digits.matches("\\d{16}")) {
            throw new InvalidCardNumberException(label + ": номер карты должен состоять из 16 цифр");
        }
        if (!passesLuhnCheck(digits)) {
            throw new InvalidCardNumberException(label + ": некорректный номер карты");
        }
    }

    void validateValidTill(String validTill) {
        if (validTill == null || !validTill.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            throw new InvalidValidTillException("Срок действия должен быть в формате ММ/ГГ");
        }
        YearMonth cardMonth = YearMonth.of(
                2000 + Integer.parseInt(validTill.substring(3)),
                Integer.parseInt(validTill.substring(0, 2)));
        if (cardMonth.isBefore(YearMonth.now())) {
            throw new InvalidValidTillException("Срок действия карты истёк");
        }
    }

    void validateCvv(String cvv) {
        if (cvv == null || !cvv.matches("\\d{3}")) {
            throw new InvalidCvvException("CVV должен состоять из 3 цифр");
        }
    }

    void validateAmount(Amount amount) {
        if (amount == null) {
            throw new InvalidAmountException("Сумма перевода не указана");
        }
        if (amount.value() <= 0) {
            throw new InvalidAmountException("Сумма перевода должна быть больше нуля");
        }
        if (isBlank(amount.currency())) {
            throw new InvalidAmountException("Валюта не указана");
        }
    }

    /**
     * Алгоритм Луна: сумма цифр справа налево, где каждая вторая цифра
     * удваивается (если результат > 9 — вычитаем 9), должна быть кратна 10.
     */
    boolean passesLuhnCheck(String digits) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            int d = digits.charAt(digits.length() - 1 - i) - '0';
            if (i % 2 == 1) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
        }
        return sum % 10 == 0;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}