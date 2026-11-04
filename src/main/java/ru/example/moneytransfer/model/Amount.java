package ru.example.moneytransfer.model;

/**
 * Сумма перевода. value — в копейках (long), чтобы избежать ошибок
 * арифметики с плавающей точкой.
 */
public record Amount(long value, String currency) {
}
