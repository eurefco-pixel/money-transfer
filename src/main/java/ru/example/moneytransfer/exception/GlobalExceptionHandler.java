package ru.example.moneytransfer.exception;

import ru.example.moneytransfer.dto.ErrorResponse;
import ru.example.moneytransfer.logger.TransferLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Единый маппинг исключений на формат спецификации {message, id, code}.
 * id — хэш сообщения: стабилен при одинаковом тексте, различается для разных ошибок.
 * code — HTTP-статус ответа.
 * Неотпарсенные и не прошедшие валидацию запросы тоже считаются операциями:
 * логируются в transfer.log как FAILED.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final TransferLogger transferLogger;

    public GlobalExceptionHandler(TransferLogger transferLogger) {
        this.transferLogger = transferLogger;
    }

    @ExceptionHandler(ErrorInputData.class)
    public ResponseEntity<ErrorResponse> handleInput(ErrorInputData e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(OperationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOperationNotFound(OperationNotFoundException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());  // 400 — соответствует "Error input data"
    }

    // Неверный код подтверждения — 400. Клиент прислал не то.
    @ExceptionHandler(InvalidConfirmationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidConfirmationCode(InvalidConfirmationCodeException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // Нехватка средств — 500 по протоколу (Error transfer)
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    @ExceptionHandler(ErrorTransfer.class)
    public ResponseEntity<ErrorResponse> handleTransfer(ErrorTransfer e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    // Непредвиденное — 500 и стектрейс в лог приложения
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception e) {
        log.error("Непредвиденная ошибка", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
    }

    // Несуществующий путь — 404, а не 500 (NoResourceFoundException иначе ловится catch-all)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "Ресурс не найден");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableJson(HttpMessageNotReadableException e) {
        // Запрос не разобран до сервисного слоя — логируем здесь, до TransferServiceImpl не доходит
        transferLogger.logFailed(null, "", "", 0, "Некорректный JSON в запросе");
        return build(HttpStatus.BAD_REQUEST, "Некорректный JSON в запросе");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getDefaultMessage())
                .findFirst()
                .orElse("Некорректное тело запроса");
        transferLogger.logFailed(null, "", "", 0, message);
        return build(HttpStatus.BAD_REQUEST, message);
    }

    // Единая сборка ответа: id — хэш сообщения, code — HTTP-статус
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(message, message.hashCode(), status.value()));
    }
}