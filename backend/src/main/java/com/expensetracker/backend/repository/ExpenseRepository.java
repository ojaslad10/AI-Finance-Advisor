package com.expensetracker.backend.repository;

import com.expensetracker.backend.model.Expense;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends MongoRepository<Expense, String> {
    List<Expense> findByUserId(String userId);
    Optional<Expense> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
    List<Expense> findByUserIdAndDateBetween(String userId, String startIsoDate, String endIsoDate);
}
