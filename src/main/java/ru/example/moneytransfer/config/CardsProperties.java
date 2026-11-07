package ru.example.moneytransfer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки хранилища карт из application.yml (prefix cards).
 */
@Component
@ConfigurationProperties(prefix = "cards")
public class CardsProperties {

    /**
     * Путь к файлу карт.
     */
    private String file = "./data/cards.json";

    /**
     * Сохранять изменения балансов обратно в файл.
     */
    private boolean persistOnChange = true;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public boolean isPersistOnChange() {
        return persistOnChange;
    }

    public void setPersistOnChange(boolean persistOnChange) {
        this.persistOnChange = persistOnChange;
    }
}