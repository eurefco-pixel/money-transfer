package ru.example.moneytransfer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Настройки перевода из application.yml (prefix transfer).
 * Инжектится в ValidationService и TransferServiceImpl.
 */
@Component
@ConfigurationProperties(prefix = "transfer")
public class TransferProperties {

    /**
     * Комиссия долей от суммы: 0.01 = 1%.
     */
    private BigDecimal commissionRate = new BigDecimal("0.01");

    /**
     * Время жизни неподтверждённой операции, минуты.
     */
    private long operationTtlMinutes = 5;

    /**
     * Допустимые валюты перевода.
     */
    private List<String> allowedCurrencies = List.of("RUR");

    /**
     * Максимальная сумма перевода, копейки.
     */
    private long maxAmountValue = 100_000_000L;

    /**
     * Фиксированный код подтверждения. Пусто (по умолчанию) — код генерируется
     * случайно для каждой операции. Задан — все операции принимают его
     * (удобно для тестов и демо-стенда).
     */
    private String confirmCode;

    /**
     * Максимум неверных вводов кода до блокировки операции.
     */
    private int maxConfirmAttempts = 3;

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public long getOperationTtlMinutes() {
        return operationTtlMinutes;
    }

    public void setOperationTtlMinutes(long operationTtlMinutes) {
        this.operationTtlMinutes = operationTtlMinutes;
    }

    public List<String> getAllowedCurrencies() {
        return allowedCurrencies;
    }

    public void setAllowedCurrencies(List<String> allowedCurrencies) {
        this.allowedCurrencies = allowedCurrencies;
    }

    public long getMaxAmountValue() {
        return maxAmountValue;
    }

    public void setMaxAmountValue(long maxAmountValue) {
        this.maxAmountValue = maxAmountValue;
    }

    public String getConfirmCode() {
        return confirmCode;
    }

    public void setConfirmCode(String confirmCode) {
        this.confirmCode = confirmCode;
    }

    public int getMaxConfirmAttempts() {
        return maxConfirmAttempts;
    }

    public void setMaxConfirmAttempts(int maxConfirmAttempts) {
        this.maxConfirmAttempts = maxConfirmAttempts;
    }
}