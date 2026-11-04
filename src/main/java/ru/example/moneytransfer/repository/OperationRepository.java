package ru.example.moneytransfer.repository;

import ru.example.moneytransfer.model.Operation;

import java.util.Optional;

/** Реестр операций: в памяти с TTL — этап 3. */
public interface OperationRepository {

    void save(Operation operation);

    Optional<Operation> findById(String operationId);

    void update(Operation operation);
}
