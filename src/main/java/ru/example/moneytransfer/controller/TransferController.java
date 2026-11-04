package ru.example.moneytransfer.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.example.moneytransfer.model.ConfirmationRequest;
import ru.example.moneytransfer.model.TransferRequest;
import ru.example.moneytransfer.service.TransferService;

@RestController
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    public String transfer(@RequestBody TransferRequest request) {
        return transferService.transfer(request);
    }

    @PostMapping("/confirmOperation")
    public String confirmOperation(@RequestBody ConfirmationRequest request) {
        return transferService.confirmOperation(request);
    }
}