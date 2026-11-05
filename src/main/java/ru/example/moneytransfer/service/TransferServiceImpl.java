package ru.example.moneytransfer.service;

import org.springframework.stereotype.Service;
import ru.example.moneytransfer.exception.ErrorInputData;
import ru.example.moneytransfer.exception.InsufficientFundsException;
import ru.example.moneytransfer.exception.InvalidConfirmationCodeException;
import ru.example.moneytransfer.exception.OperationNotFoundException;
import ru.example.moneytransfer.logger.TransferLogger;
import ru.example.moneytransfer.model.Amount;
import ru.example.moneytransfer.model.ConfirmationRequest;
import ru.example.moneytransfer.model.Operation;
import ru.example.moneytransfer.model.TransferRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransferServiceImpl implements TransferService {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.01");

    private final ValidationService validationService;
    private final CardStorageService cardStorage;
    private final TransferLogger logger;

    private final Map<String, Operation> operations = new ConcurrentHashMap<>();

    public TransferServiceImpl(ValidationService validationService,
                               CardStorageService cardStorage,
                               TransferLogger logger) {
        this.validationService = validationService;
        this.cardStorage = cardStorage;
        this.logger = logger;
    }

    @Override
    public String transfer(TransferRequest request) {
        validationService.validate(request);

        String from = request.cardFromNumber().replace(" ", "");
        String to = request.cardToNumber().replace(" ", "");
        long amountKopecks = toKopecks(request.amount());
        long commission = commissionKopecks(amountKopecks);

        cardStorage.getBalance(from);
        cardStorage.getBalance(to);

        if (cardStorage.getBalance(from) < amountKopecks + commission) {
            throw new InsufficientFundsException("Недостаточно средств на карте отправителя");
        }

        String operationId = UUID.randomUUID().toString();
        Operation operation = new Operation(
                operationId,
                from,
                to,
                amountKopecks,
                commission,
                LocalDateTime.now().toString(),
                false
        );
        operations.put(operationId, operation);
        return operationId;
    }

    @Override
    public String confirmOperation(ConfirmationRequest request) {
        if (request == null || request.operationId() == null || request.code() == null) {
            throw new ErrorInputData("Не заполнены данные подтверждения");
        }

        Operation operation = operations.get(request.operationId());
        if (operation == null) {
            throw new OperationNotFoundException("Операция не найдена: " + request.operationId());
        }
        if (operation.confirmed()) {
            throw new ErrorInputData("Операция уже подтверждена");
        }
        if (!"0000".equals(request.code())) {
            logger.logError(operation, "FAILED", "Неверный код подтверждения");
            throw new InvalidConfirmationCodeException("Неверный код подтверждения");
        }

        try {
            cardStorage.debit(operation.cardFromNumber(), operation.amountValue() + operation.commission());
            cardStorage.credit(operation.cardToNumber(), operation.amountValue());
        } catch (RuntimeException e) {
            logger.logError(operation, "FAILED", e.getMessage());
            throw e;
        }

        operations.put(request.operationId(),
                new Operation(operation.operationId(), operation.cardFromNumber(),
                        operation.cardToNumber(), operation.amountValue(),
                        operation.commission(), operation.createdAt(), true));

        logger.log(operation, "SUCCESS");
        return request.operationId();
    }

    private long toKopecks(Amount amount) {
        return BigDecimal.valueOf(amount.value())
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private long commissionKopecks(long amountKopecks) {
        return BigDecimal.valueOf(amountKopecks)
                .multiply(COMMISSION_RATE)
                .setScale(0, RoundingMode.CEILING)
                .max(BigDecimal.ONE)
                .longValueExact();
    }
}