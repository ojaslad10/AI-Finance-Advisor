package com.expensetracker.backend.controller;

import com.expensetracker.backend.model.Expense;
import com.expensetracker.backend.model.ExpenseRequest;
import com.expensetracker.backend.model.User;
import com.expensetracker.backend.repository.ExpenseRepository;
import com.expensetracker.backend.repository.UserRepository;
import com.expensetracker.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/expenses")
@CrossOrigin(origins = "*")
public class ExpenseController {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> addExpense(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Expense expenseReq
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Missing Authorization header"));
        }

        String jwt = authHeader.substring(7);
        if (!JwtUtil.isTokenValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid or expired token"));
        }

        String email = JwtUtil.extractEmail(jwt);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        String idempotency = expenseReq.getIdempotencyKey();
        if (idempotency != null && !idempotency.isBlank()) {
            Optional<Expense> existing = expenseRepository.findByUserIdAndIdempotencyKey(user.getId(), idempotency);
            if (existing.isPresent()) {
                Expense e = existing.get();
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Already recorded",
                        "expense", e,
                        "newBalance", user.getBankBalance()
                ));
            } else {
                expenseReq.setIdempotencyKey(idempotency);
            }
        }

        if (expenseReq.getDate() == null || expenseReq.getDate().isBlank()) {
            expenseReq.setDate(LocalDate.now().toString());
        }
        if (expenseReq.getCategory() == null || expenseReq.getCategory().isBlank()) {
            expenseReq.setCategory("Other");
        }

        expenseReq.setUserId(user.getId());
        Expense savedExpense = expenseRepository.save(expenseReq);

        double amt = expenseReq.getAmount();
        double newBalance = user.getBankBalance();
        if (amt > 0) {
            newBalance = user.getBankBalance() + amt;
        } else {
            newBalance = user.getBankBalance() - Math.abs(amt);
        }
        user.setBankBalance(newBalance);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Expense saved and balance updated",
                "expense", savedExpense,
                "newBalance", newBalance
        ));
    }

    @GetMapping
    public ResponseEntity<?> getExpenses(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Missing Authorization header"));
        }

        String jwt = authHeader.substring(7);
        if (!JwtUtil.isTokenValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Invalid or expired token"));
        }

        String email = JwtUtil.extractEmail(jwt);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "User not found"));
        }

        List<Expense> list;
        if (start != null && end != null) {
            list = expenseRepository.findByUserIdAndDateBetween(user.getId(), start.toString(), end.toString());
        } else {
            list = expenseRepository.findByUserId(user.getId());
        }

        return ResponseEntity.ok(Map.of("success", true, "expenses", list));
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Missing Authorization header"));
        }

        String jwt = authHeader.substring(7);
        if (!JwtUtil.isTokenValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Invalid or expired token"));
        }

        String email = JwtUtil.extractEmail(jwt);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "User not found"));
        }

        List<Expense> expenses;
        if (start != null && end != null) {
            expenses = expenseRepository.findByUserIdAndDateBetween(user.getId(), start, end);
        } else {
            // default: get all user expenses
            expenses = expenseRepository.findByUserId(user.getId());
        }

        double totalIncome = 0.0;
        double totalExpense = 0.0;
        double todaysExpense = 0.0;
        String todayIso = LocalDate.now().toString(); // yyyy-MM-dd

        for (Expense e : expenses) {
            double amt = e.getAmount();
            if (amt > 0) {
                totalIncome += amt;
            } else {
                totalExpense += Math.abs(amt);
                if (todayIso.equals(e.getDate())) {
                    todaysExpense += Math.abs(amt);
                }
            }
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("totalIncome", totalIncome);
        resp.put("totalExpense", totalExpense);
        resp.put("todaysExpense", todaysExpense);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Missing Authorization header"));
        }
        String jwt = authHeader.substring(7);
        if (!JwtUtil.isTokenValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Invalid or expired token"));
        }
        String email = JwtUtil.extractEmail(jwt);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "User not found"));
        }
        return ResponseEntity.ok(Map.of("success", true, "balance", user.getBankBalance()));
    }

    @PostMapping("/balance")
    public ResponseEntity<?> setBalance(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> payload
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Missing Authorization header"));
        }
        String jwt = authHeader.substring(7);
        if (!JwtUtil.isTokenValid(jwt)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Invalid or expired token"));
        }
        String email = JwtUtil.extractEmail(jwt);
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "User not found"));
        }

        Double newBalance = null;
        try {
            Object val = payload.get("balance");
            if (val instanceof Number) {
                newBalance = ((Number) val).doubleValue();
            } else if (val instanceof String) {
                newBalance = Double.parseDouble((String) val);
            }
        } catch (Exception ignored) {}

        if (newBalance == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing or invalid 'balance' field"));
        }

        user.setBankBalance(newBalance);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Balance updated", "balance", newBalance));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addExpense(@RequestBody ExpenseRequest request) {
        Expense expense = new Expense();
        expense.setAmount(request.getAmount());
        expense.setBank(request.getBank());
        expense.setAccount(request.getAccount());
        expense.setReceiver(request.getReceiver());
        expense.setCategory(request.getCategory());
        expense.setDate(request.getDate());

        expenseRepository.save(expense);
        return ResponseEntity.ok(Map.of("success", true, "message", "Expense added successfully"));
    }

}