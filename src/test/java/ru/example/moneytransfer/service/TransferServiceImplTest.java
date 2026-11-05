package ru.example.moneytransfer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.example.moneytransfer.exception.ErrorInputData;
import ru.example.moneytransfer.exception.InsufficientFundsException;
import ru.example.moneytransfer.exception.InvalidConfirmationCodeException;
import ru.example.moneytransfer.exception.OperationNotFoundException;
import ru.example.moneytransfer.logger.TransferLogger;
import ru.example.moneytransfer.model.Amount;
import ru.example.moneytransfer.model.ConfirmationRequest;
import ru.example.moneytransfer.model.TransferRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TransferServiceImplTest {

    private ValidationService validationService;
    private CardStorageService cardStorage;
    private TransferLogger logger;
    private TransferServiceImpl service;

    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        validationService = mock(ValidationService.class);
        cardStorage = mock(CardStorageService.class);
        logger = mock(TransferLogger.class);
        service = new TransferServiceImpl(validationService, cardStorage, logger);

        transferRequest = new TransferRequest(
                "4111111111111111", "12/27", "123",
                "5555555555554444",
                new Amount(1000, "RUR")
        );

        // Балансы по умолчанию: обе карты есть, средств хватает
        when(cardStorage.getBalance("4111111111111111")).thenReturn(500_000L);
        when(cardStorage.getBalance("5555555555554444")).thenReturn(50_000L);
    }

    @Test
    void transfer_returnsOperationId() {
        String operationId = service.transfer(transferRequest);

        assertNotNull(operationId);
        verify(validationService).validate(transferRequest);
    }

    @Test
    void confirmOperation_success_debitsAndCredits() {
        String opId = service.transfer(transferRequest);

        String result = service.confirmOperation(new ConfirmationRequest(opId, "0000"));

        assertEquals(opId, result);
        // списание: 1000 руб = 100000 коп + комиссия 10000 коп (1%)
        verify(cardStorage).debit("4111111111111111", 101_000L);
        verify(cardStorage).credit("5555555555554444", 100_000L);
        verify(logger).logSuccess(any());
    }

    @Test
    void confirmOperation_unknownId_throwsOperationNotFound() {
        service.transfer(transferRequest); // создаём операции, но подтверждаем чужой ID

        assertThrows(OperationNotFoundException.class,   // ← было ErrorInputData
                () -> service.confirmOperation(
                        new ConfirmationRequest("deadbeef-0000-0000-0000-000000000000", "0000")));
    }

    @Test
    void confirmOperation_wrongCode_throwsAndLogs() {
        String opId = service.transfer(transferRequest);

        assertThrows(InvalidConfirmationCodeException.class,
                () -> service.confirmOperation(new ConfirmationRequest(opId, "9999")));

        verify(logger).logError(any(), eq("Неверный код подтверждения"));
        // и убедимся, что деньги не списались
        verify(cardStorage, never()).debit(anyString(), anyLong());
    }

    @Test
    void confirmOperation_repeat_throwsErrorInputData() {
        String opId = service.transfer(transferRequest);
        service.confirmOperation(new ConfirmationRequest(opId, "0000"));

        assertThrows(ErrorInputData.class,
                () -> service.confirmOperation(new ConfirmationRequest(opId, "0000")));
    }

    @Test
    void transfer_insufficientFunds_throws() {
        when(cardStorage.getBalance("4111111111111111")).thenReturn(50_000L); // 500 руб < 1010 руб

        assertThrows(InsufficientFundsException.class,
                () -> service.transfer(transferRequest));
    }

    @Test
    void confirmOperation_blankRequest_throwsErrorInputData() {
        assertThrows(ErrorInputData.class,
                () -> service.confirmOperation(new ConfirmationRequest(null, null)));
    }
}