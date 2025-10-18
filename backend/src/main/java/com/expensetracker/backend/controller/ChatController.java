package com.expensetracker.backend.controller;

import com.expensetracker.backend.dto.ChatRequest;
import com.expensetracker.backend.dto.ChatResponse;
import com.expensetracker.backend.service.OpenAiAndMlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private OpenAiAndMlService aiService;

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest req) {
        if (req.getUserId() == null || req.getUserId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "userId required"));
        }

        Map<String,Object> analysis = aiService.analyzeWithMl(req.getUserId(), req.getWindow());

        String reply = aiService.generateOpenAiReply(req.getMessage(), analysis);

        ChatResponse resp = new ChatResponse(true, reply, Map.of("analysis_hint", analysis.getOrDefault("overall_advice_hint", analysis)));
        return ResponseEntity.ok(resp);
    }
}
