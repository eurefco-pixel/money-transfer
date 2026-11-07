package ru.example.moneytransfer.logger;

import org.junit.jupiter.api.Test;
import ru.example.moneytransfer.model.Operation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferLoggerTest {

    private static final String FULL_CARD = "4111111111111111";

    @Test
    void maskCard_leavesLastFourDigits() {
        assertEquals("**** 1111", TransferLogger.maskCard(FULL_CARD));
    }

    @Test
    void maskCard_handlesEmptyAndShort() {
        assertEquals("****", TransferLogger.maskCard(null));
        assertEquals("****", TransferLogger.maskCard(""));
        assertEquals("****", TransferLogger.maskCard("123"));
    }

    @Test
    void logEntries_doNotContainFullCardNumbers() throws Exception {
        Path temp = Files.createTempDirectory("transfer-log-test");
        TransferLogger logger = new TransferLogger(temp.resolve("transfer.log").toString());

        logger.logSuccess(new Operation("op-1", FULL_CARD, "5555555555554444",
                100_000, 1_000, "2026-11-07T20:00:00", true, 0, null));
        logger.logFailed(null, FULL_CARD, "5555555555554444", 0, "Неверный CVV");

        List<String> lines = Files.readAllLines(temp.resolve("transfer.log"));
        assertEquals(2, lines.size());
        for (String line : lines) {
            assertFalse(line.contains(FULL_CARD), "Полный номер в логе: " + line);
            assertFalse(line.contains("5555555555554444"), "Полный номер в логе: " + line);
            assertTrue(line.contains("\"cardFrom\":\"**** 1111\""), line);
        }
    }
}