package ru.example.moneytransfer.exception;

import ru.example.moneytransfer.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Единый маппинг исключений на формат спецификации {message, id}.
 * id — хэш сообщения: стабилен при одинаковом тексте, различается для разных ошибок.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);  // ← добавлено

    @ExceptionHandler(ErrorInputData.class)
    public ResponseEntity<ErrorResponse> handleInput(ErrorInputData e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), e.getMessage().hashCode()));
    }

    @ExceptionHandler(OperationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOperationNotFound(OperationNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)  // 400 — соответствует "Error input data"
                .body(new ErrorResponse(e.getMessage(), e.getMessage().hashCode()));
    }

    // Неверный код подтверждения — 400. Клиент прислал не то.
    @ExceptionHandler(InvalidConfirmationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidConfirmationCode(InvalidConfirmationCodeException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(e.getMessage(), e.getMessage().hashCode()));
    }

    // Нехватка средств — 500 по протоколу (Error transfer)
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(e.getMessage(), e.getMessage().hashCode()));
    }

    @ExceptionHandler(ErrorTransfer.class)
    public ResponseEntity<ErrorResponse> handleTransfer(ErrorTransfer e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(e.getMessage(), e.getMessage().hashCode()));
    }

    // Непредвиденное — 500 и стектрейс в лог приложения
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception e) {
        log.error("Непредвиденная ошибка", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal error", 500));
    }

    // Несуществующий путь — 404, а не 500 (NoResourceFoundException иначе ловится catch-all)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Ресурс не найден", 404));
    }
}