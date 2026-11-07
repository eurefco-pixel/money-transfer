package ru.example.moneytransfer.service;

import org.junit.jupiter.api.Test;
import ru.example.moneytransfer.config.TransferProperties;
import ru.example.moneytransfer.exception.InvalidCardNumberException;
import ru.example.moneytransfer.exception.ErrorInputData;
import ru.example.moneytransfer.exception.InvalidValidTillException;
import ru.example.moneytransfer.model.Amount;
import ru.example.moneytransfer.model.TransferRequest;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    ValidationService service = new ValidationService(new TransferProperties());

    // Валидные тестовые карты: 16 цифр, проходят Луна, разные между собой
    private static final String VALID_CARD_FROM = "4111111111111111";
    private static final String VALID_CARD_TO = "5555555555554444";
    private static final String INVALID_CARD_LUHN = "5555666677778881";

    /**
     * Порядок аргументов строго как в record:
     * cardFromNumber, cardFromValidTill, cardFromCVV, cardToNumber, amount
     */
    private TransferRequest request(String from, String till, String cvv, String to, Amount amount) {
        return new TransferRequest(from, till, cvv, to, amount);
    }

    @Test
    void testValidRequest() {
        var r = request(VALID_CARD_FROM, "12/99", "123", VALID_CARD_TO, new Amount(1000, "RUR"));
        assertDoesNotThrow(() -> service.validate(r));
    }

    @Test
    void testNullRequest() {
        assertThrows(ErrorInputData.class, () -> service.validate(null));
    }

    @Test
    void testInvalidCardNumber_LuhnFail() {
        var r = request(INVALID_CARD_LUHN, "12/99", "123", VALID_CARD_TO, new Amount(1000, "RUR"));
        assertThrows(InvalidCardNumberException.class, () -> service.validate(r));
    }

    @Test
    void testInvalidValidTill_Expired() {
        var r = request(VALID_CARD_FROM, "01/20", "123", VALID_CARD_TO, new Amount(1000, "RUR"));
        assertThrows(InvalidValidTillException.class, () -> service.validate(r));
    }

    @Test
    void testSameCard_throwsErrorInputData() {
        var r = request(VALID_CARD_FROM, "12/99", "123", VALID_CARD_FROM, new Amount(1000, "RUR"));
        assertThrows(ErrorInputData.class, () -> service.validate(r));
    }

    @Test
    void testSameCardWithSpaces_throwsErrorInputData() {
        // Регрессия: пробелы в номере не должны обходить проверку «не та же карта»
        var r = request("4111 1111 1111 1111", "12/99", "123", VALID_CARD_FROM, new Amount(1000, "RUR"));
        assertThrows(ErrorInputData.class, () -> service.validate(r));
    }
}