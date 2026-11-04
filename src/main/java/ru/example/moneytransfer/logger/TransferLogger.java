package ru.example.moneytransfer.logger;

import ru.example.moneytransfer.model.Operation;
import org.springframework.stereotype.Component;

/**
 * Запись лога переводов в файл (JSON-строки).
 * Этап 2 — заглушка (лог в консоль), файловая реализация — этап 3.
 */
@Component
public class TransferLogger {

    public void log(Operation operation, String result) {
        // TODO этап 3: писать строку формата
        //  дата|время|cardFrom|cardTo|сумма|комиссия|результат в transfer.log
        System.out.printf("[TRANSFER-LOG] %s %s %s -> %s amount=%d commission=%d result=%s%n",
                operation.createdAt(), operation.operationId(),
                operation.cardFromNumber(), operation.cardToNumber(),
                operation.amountValue(), operation.commission(), result);
    }
}
