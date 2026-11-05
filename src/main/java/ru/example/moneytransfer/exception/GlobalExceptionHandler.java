package ru.example.moneytransfer.exception;

import ru.example.moneytransfer.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Единый маппинг исключений на формат спецификации {message, id}.
 * id — хэш сообщения: стабилен при одинаковом тексте, различается для разных ошибок.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(ErrorTransfer.class)
    public ResponseEntity<ErrorResponse> handleTransfer(ErrorTransfer e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(e.getMessage(), e.getMessage().hashCode()));
    }

    // Непредвиденное — тоже 500, чтобы формат ответа не ломался
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Internal error", 500));
    }
}