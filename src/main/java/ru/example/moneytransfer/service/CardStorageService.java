package ru.example.moneytransfer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import ru.example.moneytransfer.config.CardsProperties;
import ru.example.moneytransfer.exception.CardNotFoundException;
import ru.example.moneytransfer.exception.InsufficientFundsException;
import ru.example.moneytransfer.exception.InvalidCvvException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище карт: читается из файла, путь задаётся в application.yml.
 * Баланс хранится в копейках, чтобы не терять точность при расчёте комиссии.
 * <p>
 * Внимание: хранение CVV здесь — осознанное действие для проекта.
 * В реальных платёжных системах хранить CVV запрещено (PCI DSS).
 */
@Component
public class CardStorageService {

    private record CardInfo(long balanceKopecks, String validTill, String cvv) {
    }

    private final Path file;
    private final boolean persistOnChange;
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, CardInfo> cards = new ConcurrentHashMap<>();

    public CardStorageService(CardsProperties cardsProperties) {
        this.file = Path.of(cardsProperties.getFile());
        this.persistOnChange = cardsProperties.isPersistOnChange();
    }

    @PostConstruct
    void load() {
        if (Files.exists(file)) {
            try {
                Map<String, CardInfo> loaded = mapper.readValue(
                        file.toFile(), new TypeReference<Map<String, CardInfo>>() {
                        });
                cards.putAll(loaded);
            } catch (IOException e) {
                throw new IllegalStateException("Не удалось прочитать файл карт: " + file, e);
            }
        } else {
            // Файла нет — создаём с дефолтными картами, чтобы приложение не падало на старте
            cards.put("4111111111111111", new CardInfo(100_000_00L, "12/28", "123"));
            cards.put("5555555555554444", new CardInfo(50_000_00L, "06/27", "456"));
            if (persistOnChange) {
                save();
            }
        }
    }

    public void validateCvv(String cardNumber, String cvv) {
        CardInfo card = findCard(cardNumber);
        if (!card.cvv().equals(cvv)) {
            throw new InvalidCvvException("Неверный CVV");
        }
    }

    public void validateCardExists(String cardNumber) {
        findCard(cardNumber);
    }

    public long getBalance(String cardNumber) {
        return findCard(cardNumber).balanceKopecks();
    }

    /**
     * synchronized: read-modify-write баланса должен быть неделимым.
     */
    public synchronized void debit(String cardNumber, long amountKopecks) {
        CardInfo card = findCard(cardNumber);
        if (card.balanceKopecks() < amountKopecks) {
            throw new InsufficientFundsException("Недостаточно средств на карте: " + cardNumber);
        }
        cards.put(cardNumber, new CardInfo(card.balanceKopecks() - amountKopecks,
                card.validTill(), card.cvv()));
        if (persistOnChange) {
            save();
        }
    }

    /**
     * synchronized по той же причине, что и debit: атомарное начисление.
     */
    public synchronized void credit(String cardNumber, long amountKopecks) {
        validateCardExists(cardNumber);
        CardInfo card = cards.get(cardNumber);
        cards.put(cardNumber, new CardInfo(card.balanceKopecks() + amountKopecks,
                card.validTill(), card.cvv()));
        if (persistOnChange) {
            save();
        }
    }

    private void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), cards);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить файл карт: " + file, e);
        }
    }

    private CardInfo findCard(String cardNumber) {
        CardInfo card = cards.get(cardNumber);
        if (card == null) {
            throw new CardNotFoundException("Карта не найдена: " + cardNumber);
        }
        return card;
    }
}