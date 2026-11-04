package ru.example.moneytransfer.service;

import ru.example.moneytransfer.logger.TransferLogger;
import ru.example.moneytransfer.model.*;
import ru.example.moneytransfer.repository.OperationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransferServiceImpl implements TransferService {

    private final ValidationService validationService;
    private final CodeGenerator codeGenerator;
    private final OperationRepository operationRepository;
    private final TransferLogger transferLogger;

    @Value("${transfer.commission-rate:0.01}")
    private double commissionRate;

    public TransferServiceImpl(ValidationService validationService,
                               CodeGenerator codeGenerator,
                               OperationRepository operationRepository,
                               TransferLogger transferLogger) {
        this.validationService = validationService;
        this.codeGenerator = codeGenerator;
        this.operationRepository = operationRepository;
        this.transferLogger = transferLogger;
    }

    @Override
    public TransferResponse transfer(TransferRequest request) {
        validationService.validate(request);
        // TODO этап 3: проверка карт, баланса, валюты; расчёт комиссии;
        //  сохранение операции PENDING_CONFIRMATION, вывод кода в лог консоли
        String operationId = codeGenerator.newOperationId();
        return new TransferResponse(operationId);
    }

    @Override
    public TransferResponse confirm(ConfirmationRequest request) {
        // TODO этап 3: проверка operationId, TTL, кода; списание/зачисление; запись в лог
        return new TransferResponse(request.operationId());
    }
}
