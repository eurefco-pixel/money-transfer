package ru.example.moneytransfer.service;

import org.springframework.stereotype.Component;
import ru.example.moneytransfer.config.TransferProperties;
import ru.example.moneytransfer.exception.ErrorInputData;
import ru.example.moneytransfer.exception.InvalidAmountException;
import ru.example.moneytransfer.exception.InvalidCardNumberException;
import ru.example.moneytransfer.exception.InvalidValidTillException;
import ru.example.moneytransfer.model.Amount;
import ru.example.moneytransfer.model.TransferRequest;

import java.time.YearMonth;

/**
 * Бизнес-валидация. Формат полей (16 цифр, ММ/ГГ, CVV, положительная сумма,
 * заполненность) проверяется Bean Validation на слое контроллера — см. TransferRequest и Amount.
 * Здесь остаются правила, которые на аннотации не переносятся:
 * алгоритм Луна, срок действия карты, поддерживаемые валюты и лимит, «не та же карта».
 */
@Component
public class ValidationService {

    private final TransferProperties properties;

    public ValidationService(TransferProperties properties) {
        this.properties = properties;
    }

    public void validate(TransferRequest request) {
        if (request == null) {
            throw new ErrorInputData("Запрос не заполнен");
        }
        validateNotSameCard(request);
        validateLuhn(request.cardFromNumber(), "Карта отправителя");
        validateLuhn(request.cardToNumber(), "Карта получателя");
        validateValidTillNotExpired(request.cardFromValidTill());
        validateAmountRules(request.amount());
    }


    private void validateNotSameCard(TransferRequest request) {
        // Сравниваем нормализованные номера: "4111 1111..." и "41111111..." — одна карта
        String from = request.cardFromNumber().replace(" ", "");
        String to = request.cardToNumber().replace(" ", "");
        if (from.equals(to)) {
            throw new ErrorInputData("Нельзя переводить на ту же карту");
        }
    }

    /**
     * Алгоритм Луна: сумма цифр справа налево, где каждая вторая цифра
     * удваивается (если результат > 9 — вычитаем 9), должна быть кратна 10.
     * Формат (16 цифр) гарантирован аннотацией @Pattern, поэтому только нормализация пробелов.
     */
    private void validateLuhn(String number, String label) {
        String digits = number.replace(" ", "");
        if (!passesLuhnCheck(digits)) {
            throw new InvalidCardNumberException(label + ": некорректный номер карты");
        }
    }

    private void validateValidTillNotExpired(String validTill) {
        YearMonth cardMonth = YearMonth.of(
                2000 + Integer.parseInt(validTill.substring(3)),
                Integer.parseInt(validTill.substring(0, 2)));
        if (cardMonth.isBefore(YearMonth.now())) {
            throw new InvalidValidTillException("Срок действия карты истёк");
        }
    }

    private void validateAmountRules(Amount amount) {
        String currency = amount.currency().trim().toUpperCase();
        if (!properties.getAllowedCurrencies().contains(currency)) {
            throw new InvalidAmountException(
                    "Валюта не поддерживается: " + amount.currency()
                            + ". Допустимые: " + properties.getAllowedCurrencies());
        }
        if (amount.value() > properties.getMaxAmountValue()) {
            throw new InvalidAmountException(
                    "Сумма перевода превышает лимит: " + properties.getMaxAmountValue() + " копеек");
        }
    }

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
}