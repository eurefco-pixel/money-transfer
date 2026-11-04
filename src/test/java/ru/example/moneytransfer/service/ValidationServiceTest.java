package ru.example.moneytransfer.service;

import org.junit.jupiter.api.Test;
import ru.example.moneytransfer.exception.InvalidAmountException;
import ru.example.moneytransfer.exception.InvalidCardNumberException;
import ru.example.moneytransfer.exception.InvalidCvvException;
import ru.example.moneytransfer.exception.ErrorInputData;
import ru.example.moneytransfer.exception.InvalidValidTillException;
import ru.example.moneytransfer.model.Amount;
import ru.example.moneytransfer.model.TransferRequest;

import static org.junit.jupiter.api.Assertions.*;

class ValidationServiceTest {

    private final ValidationService service = new ValidationService();

    // Валидная тестовая карта: 16 цифр, проходит Луна
    private static final String VALID_CARD = "4111111111111111";
    private static final String INVALID_CARD_SHORT = "12345";
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
        var r = request(VALID_CARD, "12/99", "123", VALID_CARD, new Amount(1000, "RUR"));
        assertDoesNotThrow(() -> service.validate(r));
    }

    @Test
    void testNullRequest() {
        assertThrows(ErrorInputData.class, () -> service.validate(null));
    }

    @Test
    void testInvalidCardNumber_WrongLength() {
        var r = request(INVALID_CARD_SHORT, "12/99", "123", VALID_CARD, new Amount(1000, "RUR"));
        var ex = assertThrows(InvalidCardNumberException.class, () -> service.validate(r));
        assertTrue(ex.getMessage().contains("16 цифр"));
    }

    @Test
    void testInvalidCardNumber_LuhnFail() {
        var r = request(INVALID_CARD_LUHN, "12/99", "123", VALID_CARD, new Amount(1000, "RUR"));
        assertThrows(InvalidCardNumberException.class, () -> service.validate(r));
    }

    @Test
    void testInvalidValidTill_WrongFormat() {
        var r = request(VALID_CARD, "01-20", "123", VALID_CARD, new Amount(1000, "RUR"));
        assertThrows(InvalidValidTillException.class, () -> service.validate(r));
    }

    @Test
    void testInvalidValidTill_Expired() {
        var r = request(VALID_CARD, "01/20", "123", VALID_CARD, new Amount(1000, "RUR"));
        assertThrows(InvalidValidTillException.class, () -> service.validate(r));
    }

    @Test
    void testInvalidCvv() {
        var r = request(VALID_CARD, "12/99", "12", VALID_CARD, new Amount(1000, "RUR"));
        assertThrows(InvalidCvvException.class, () -> service.validate(r));
    }

    @Test
    void testInvalidAmount_Zero() {
        var r = request(VALID_CARD, "12/99", "123", VALID_CARD, new Amount(0, "RUR"));
        assertThrows(InvalidAmountException.class, () -> service.validate(r));
    }

    @Test
    void testInvalidAmount_Negative() {
        var r = request(VALID_CARD, "12/99", "123", VALID_CARD, new Amount(-100, "RUR"));
        assertThrows(InvalidAmountException.class, () -> service.validate(r));
    }
}