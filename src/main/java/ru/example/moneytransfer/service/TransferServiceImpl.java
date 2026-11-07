package ru.example.moneytransfer.service;

import org.springframework.stereotype.Service;
import ru.example.moneytransfer.config.TransferProperties;
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
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TransferServiceImpl implements TransferService {

    private final ValidationService validationService;
    private final CardStorageService cardStorage;
    private final TransferLogger logger;
    private final TransferProperties properties;

    private final Map<String, Operation> operations = new ConcurrentHashMap<>();

    /**
     * Замок для атомарности confirm: проверка «операция уже подтверждена»,
     * списание средств и пометка операции подтверждённой должны выполняться
     * как единое целое — иначе два параллельных confirm спишут деньги дважды.
     */
    private final Object confirmLock = new Object();

    private static final SecureRandom CODE_RANDOM = new SecureRandom();

    public TransferServiceImpl(ValidationService validationService,
                               CardStorageService cardStorage,
                               TransferLogger logger,
                               TransferProperties properties) {
        this.validationService = validationService;
        this.cardStorage = cardStorage;
        this.logger = logger;
        this.properties = properties;
    }

    @Override
    public String transfer(TransferRequest request) {
        // Локальные переменные заполняются по мере прохождения проверок:
        // если ошибка случится раньше, в лог уйдут пустые/нулевые значения — формат стабилен
        String from = "";
        String to = "";
        long amountKopecks = 0;
        try {
            validationService.validate(request);

            from = request.cardFromNumber().replace(" ", "");
            to = request.cardToNumber().replace(" ", "");
            cardStorage.validateCardExists(from);
            cardStorage.validateCvv(from, request.cardFromCVV());
            amountKopecks = toKopecks(request.amount());
            long commission = commissionKopecks(amountKopecks);

            long fromBalance = cardStorage.getBalance(from);
            cardStorage.getBalance(to);

            if (fromBalance < amountKopecks + commission) {
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
                    false,
                    0,
                    resolveConfirmCode()
            );
            operations.put(operationId, operation);
            return operationId;
        } catch (RuntimeException e) {
            // Единая точка логирования неудач на этапе перевода:
            // валидация, карта не найдена, неверный CVV, недостаток средств — всё в transfer.log
            logger.logFailed(null, from, to, amountKopecks, e.getMessage());
            throw e;
        }
    }

    @Override
    public String confirmOperation(ConfirmationRequest request) {
        if (request == null || request.operationId() == null || request.code() == null) {
            logger.logFailed(null, "", "", 0, "Не заполнены данные подтверждения");
            throw new ErrorInputData("Не заполнены данные подтверждения");
        }

        synchronized (confirmLock) {
            Operation operation = operations.get(request.operationId());
            if (operation == null) {
                logger.logFailed(request.operationId(), "", "", 0, "Операция не найдена");
                throw new OperationNotFoundException("Операция не найдена: " + request.operationId());
            }
            if (operation.confirmed()) {
                logger.logError(operation, "Операция уже подтверждена");
                throw new ErrorInputData("Операция уже подтверждена");
            }
            // Время жизни операции: просроченную подтверждать нельзя
            LocalDateTime createdAt = LocalDateTime.parse(operation.createdAt());
            if (createdAt.plusMinutes(properties.getOperationTtlMinutes()).isBefore(LocalDateTime.now())) {
                logger.logError(operation, "Время жизни операции истекло");
                throw new ErrorInputData("Время жизни операции истекло");
            }
            // Исчерпаны попытки: операция блокируется даже при верном коде
            int maxAttempts = properties.getMaxConfirmAttempts();
            if (operation.attempts() >= maxAttempts) {
                logger.logError(operation, "Превышено число попыток ввода кода");
                throw new ErrorInputData("Превышено число попыток ввода кода");
            }
            String expected = fixedConfirmCode().orElse(operation.confirmCode());
            if (!expected.equals(request.code())) {
                Operation failed = withAttempts(operation, operation.attempts() + 1);
                operations.put(request.operationId(), failed);
                logger.logError(failed, "Неверный код подтверждения (попытка "
                        + (operation.attempts() + 1) + " из " + maxAttempts + ")");
                throw new InvalidConfirmationCodeException("Неверный код подтверждения");
            }

            try {
                cardStorage.debit(operation.cardFromNumber(), operation.amountValue() + operation.commission());
                cardStorage.credit(operation.cardToNumber(), operation.amountValue());
            } catch (RuntimeException e) {
                logger.logError(operation, e.getMessage());
                throw e;
            }

            operations.put(request.operationId(),
                    new Operation(operation.operationId(), operation.cardFromNumber(),
                            operation.cardToNumber(), operation.amountValue(),
                            operation.commission(), operation.createdAt(), true,
                            operation.attempts(), operation.confirmCode()));

            logger.logSuccess(operation);
            return request.operationId();
        }
    }

    /**
     * Код подтверждения: если задан настройкой transfer.confirm-code — он общий
     * для всех операций (тесты, демо). Иначе — случайный четырёхзначный на операцию.
     */
    private String resolveConfirmCode() {
        return fixedConfirmCode().orElseGet(() -> String.format("%04d", CODE_RANDOM.nextInt(10_000)));
    }

    private java.util.Optional<String> fixedConfirmCode() {
        String fixed = properties.getConfirmCode();
        return (fixed != null && !fixed.isBlank()) ? java.util.Optional.of(fixed) : java.util.Optional.empty();
    }

    private Operation withAttempts(Operation op, int attempts) {
        return new Operation(op.operationId(), op.cardFromNumber(), op.cardToNumber(),
                op.amountValue(), op.commission(), op.createdAt(), op.confirmed(),
                attempts, op.confirmCode());
    }

    private long toKopecks(Amount amount) {
        return amount.value();
    }

    private long commissionKopecks(long amountKopecks) {
        return BigDecimal.valueOf(amountKopecks)
                .multiply(properties.getCommissionRate())
                .setScale(0, RoundingMode.CEILING)
                .max(BigDecimal.ONE)
                .longValueExact();
    }
}