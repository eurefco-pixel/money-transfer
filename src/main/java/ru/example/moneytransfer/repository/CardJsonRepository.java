package ru.example.moneytransfer.repository;

import ru.example.moneytransfer.model.Card;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Заглушка этапа 2: карты держим в памяти (ConcurrentHashMap),
 * реализация загрузки/сохранения JSON-файла — этап 3.
 */
@Repository
public class CardJsonRepository implements CardRepository {

    private final Map<String, Card> cards = new ConcurrentHashMap<>();

    @Override
    public Optional<Card> findByNumber(String cardNumber) {
        return Optional.ofNullable(cards.get(cardNumber));
    }

    @Override
    public void update(Card card) {
        cards.put(card.cardNumber(), card);
    }
}
