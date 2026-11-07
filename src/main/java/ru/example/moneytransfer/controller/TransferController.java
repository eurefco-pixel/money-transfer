package ru.example.moneytransfer.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.example.moneytransfer.model.ConfirmationRequest;
import ru.example.moneytransfer.model.TransferRequest;
import ru.example.moneytransfer.model.TransferResponse;
import ru.example.moneytransfer.service.TransferService;

@RestController
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request) {
        String operationId = transferService.transfer(request);
        return new TransferResponse(operationId);
    }

    @PostMapping("/confirmOperation")
    public TransferResponse confirmOperation(@Valid @RequestBody ConfirmationRequest request) {
        String operationId = transferService.confirmOperation(request);
        return new TransferResponse(operationId);
    }
}