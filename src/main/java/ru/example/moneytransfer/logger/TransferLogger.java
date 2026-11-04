package ru.example.moneytransfer.logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.example.moneytransfer.model.Operation;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Запись лога переводов в файл построчно в формате JSON.
 * Каждая строка — одна операция: успешная (result=SUCCESS) или ошибочная
 * (result=FAILED с полем reason).
 * Формат записи соответствует плану этапа 1:
 * дата/время | operationId | cardFrom | cardTo | сумма | комиссия | результат (+ причина).
 */
@Component
public class TransferLogger {

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
        System.out.println("[TRANSFER-LOG] Файл лога: " + logFile.toAbsolutePath());
    }

    /** Записывает успешную операцию. */
    public void log(Operation operation, String result) {
        write(toJson(operation, result, null));
    }

    /** Записывает ошибочную операцию с причиной. */
    public void logError(Operation operation, String result, String reason) {
        write(toJson(operation, result, reason));
    }

    private String toJson(Operation operation, String result, String reason) {
        return "{"
                + "\"timestamp\":\"" + operation.createdAt() + "\","
                + "\"operationId\":\"" + operation.operationId() + "\","
                + "\"cardFrom\":\"" + operation.cardFromNumber() + "\","
                + "\"cardTo\":\"" + operation.cardToNumber() + "\","
                + "\"amount\":" + operation.amountValue() + ","
                + "\"commission\":" + operation.commission() + ","
                + "\"result\":\"" + result + "\""
                + (reason != null ? ",\"reason\":\"" + escape(reason) + "\"" : "")
                + "}";
    }

    /** Экранирует кавычки и слеши, чтобы JSON-строка осталась валидной. */
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
                System.err.println("[TRANSFER-LOG] Ошибка записи в файл лога: " + e.getMessage());
            }
        }
    }
}