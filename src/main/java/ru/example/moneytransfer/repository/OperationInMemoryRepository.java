package ru.example.moneytransfer.repository;

import ru.example.moneytransfer.model.Operation;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class OperationInMemoryRepository implements OperationRepository {

    private final Map<String, Operation> operations = new ConcurrentHashMap<>();

    @Override
    public void save(Operation operation) {
        operations.put(operation.operationId(), operation);
    }

    @Override
    public Optional<Operation> findById(String operationId) {
        return Optional.ofNullable(operations.get(operationId));
    }

    @Override
    public void update(Operation operation) {
        operations.put(operation.operationId(), operation);
    }
}
