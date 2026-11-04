package ru.example.moneytransfer.service;

import org.springframework.stereotype.Component;
import ru.example.moneytransfer.exception.CardNotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище карт в памяти. Баланс хранится в копейках, чтобы не терять
 * точность при расчёте комиссии.
 */
@Component
public class CardStorageService {

    private final Map<String, Long> balances = new ConcurrentHashMap<>(Map.of(
            "4111111111111111", 100_000_00L, // 100 000 руб.
            "5555555555554444", 50_000_00L   // 50 000 руб.
    ));

    public long getBalance(String cardNumber) {
        Long balance = balances.get(cardNumber);
        if (balance == null) {
            throw new CardNotFoundException("Карта не найдена: " + cardNumber);
        }
        return balance;
    }

    public void debit(String cardNumber, long amountKopecks) {
        long balance = getBalance(cardNumber);
        if (balance < amountKopecks) {
            throw new ru.example.moneytransfer.exception.InsufficientFundsException(
                    "Недостаточно средств на карте: " + cardNumber);
        }
        balances.put(cardNumber, balance - amountKopecks);
    }

    public void credit(String cardNumber, long amountKopecks) {
        getBalance(cardNumber); // проверка существования
        balances.merge(cardNumber, amountKopecks, Long::sum);
    }
}