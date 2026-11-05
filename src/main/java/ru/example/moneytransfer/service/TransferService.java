package ru.example.moneytransfer.service;

import ru.example.moneytransfer.model.ConfirmationRequest;
import ru.example.moneytransfer.model.TransferRequest;

public interface TransferService {
    String transfer(TransferRequest request);

    String confirmOperation(ConfirmationRequest request);
}