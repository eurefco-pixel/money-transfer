package ru.example.moneytransfer.controller;

import ru.example.moneytransfer.model.ConfirmationRequest;
import ru.example.moneytransfer.model.TransferRequest;
import ru.example.moneytransfer.model.TransferResponse;
import ru.example.moneytransfer.service.TransferService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Транспортный слой: только HTTP, без логики.
 * Пути — по спецификации OpenAPI (FRONT добавляет их сам).
 */
@RestController
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    public TransferResponse transfer(@RequestBody TransferRequest request) {
        return transferService.transfer(request);
    }

    @PostMapping("/confirmOperation")
    public TransferResponse confirm(@RequestBody ConfirmationRequest request) {
        return transferService.confirm(request);
    }
}
