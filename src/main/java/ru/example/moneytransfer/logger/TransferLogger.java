package ru.example.moneytransfer.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.example.moneytransfer.model.Operation;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

@Component
public class TransferLogger {

    private static final Logger log = LoggerFactory.getLogger(TransferLogger.class);

    private static final String RESULT_SUCCESS = "SUCCESS";
    private static final String RESULT_FAILED = "FAILED";

    private final Path logFile;
    private final Object lock = new Object();

    public TransferLogger(@Value("${transfer-log.file:./data/transfer.log}") String logFilePath) {
        this.logFile = Path.of(logFilePath);
        try {
            if (logFile.getParent() != null) {
                Files.createDirectories(logFile.getParent());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать папку для лога: " + logFile, e);
        }
        log.info("Файл лога переводов: {}", logFile.toAbsolutePath());
    }

    /**
     * Записывает успешную операцию (result=SUCCESS).
     */
    public void logSuccess(Operation operation) {
        write(toJson(operation, RESULT_SUCCESS, null));
    }

    /**
     * Записывает ошибочную операцию с причиной (result=FAILED).
     */
    public void logError(Operation operation, String reason) {
        write(toJson(operation, RESULT_FAILED, reason));
    }

    private String toJson(Operation operation, String result, String reason) {
        return toJson(
                operation.operationId(),
                operation.cardFromNumber(),
                operation.cardToNumber(),
                operation.amountValue(),
                operation.commission(),
                result,
                reason
        );
    }

    private String toJson(String operationId, String cardFrom, String cardTo,
                          long amountKopecks, long commissionKopecks,
                          String result, String reason) {
        // Маскирование в единственной точке: наружу уходят только последние 4 цифры
        return "{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"operationId\":\"" + escape(operationId) + "\","
                + "\"cardFrom\":\"" + escape(maskCard(cardFrom)) + "\","
                + "\"cardTo\":\"" + escape(maskCard(cardTo)) + "\","
                + "\"amount\":" + amountKopecks + ","
                + "\"commission\":" + commissionKopecks + ","
                + "\"result\":\"" + result + "\""
                + (reason != null ? ",\"reason\":\"" + escape(reason) + "\"" : "")
                + "}";
    }

    /**
     * Записывает ошибочную операцию для стадии создания перевода,
     * когда объект Operation ещё не создан (result=FAILED).
     * Неизвестные поля записываются пустыми строками, чтобы формат файла оставался стабильным.
     */
    public void logFailed(String operationId, String cardFrom, String cardTo,
                          long amountKopecks, String reason) {
        write(toJson(
                operationId != null ? operationId : "",
                cardFrom != null ? cardFrom : "",
                cardTo != null ? cardTo : "",
                amountKopecks,
                0,          // комиссия на ошибочной операции не начислялась
                RESULT_FAILED,
                reason
        ));
    }

    /**
     * Маскирует номер карты: оставляет последние 4 цифры, остальное заменяет звёздами.
     * Пустые и короткие значения превращаются в "****".
     */
    static String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank() || cardNumber.length() < 4) {
            return "****";
        }
        return "**** " + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Экранирует кавычки и слеши, чтобы JSON-строка осталась валидной.
     */
    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void write(String json) {
        synchronized (lock) {
            try (Writer writer = Files.newBufferedWriter(logFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                writer.write(json);
                writer.write(System.lineSeparator());
            } catch (IOException e) {
                // Сбой логирования не должен ломать бизнес-логику перевода
                log.error("Ошибка записи в файл лога {}: {}", logFile, e.getMessage());
            }
        }
    }
}