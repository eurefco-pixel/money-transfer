package ru.example.moneytransfer.service;

import ru.example.moneytransfer.model.ConfirmationRequest;
import ru.example.moneytransfer.model.TransferRequest;
import ru.example.moneytransfer.model.TransferResponse;

public interface TransferService {

    /** POST /transfer — валидация, проверка карт, создание операции, генерация кода. */
    TransferResponse transfer(TransferRequest request);

    /** POST /confirmOperation — проверка кода, списание и зачисление. */
    TransferResponse confirm(ConfirmationRequest request);
}
