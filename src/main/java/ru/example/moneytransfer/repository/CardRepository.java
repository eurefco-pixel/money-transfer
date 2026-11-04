package ru.example.moneytransfer.repository;

import ru.example.moneytransfer.model.Card;

import java.util.Optional;

/** Доступ к картам. Реализация по умолчанию — JSON-файл (см. application.yml). */
public interface CardRepository {

    Optional<Card> findByNumber(String cardNumber);

    /** Обновляет баланс карты (после успешного подтверждения). */
    void update(Card card);
}
