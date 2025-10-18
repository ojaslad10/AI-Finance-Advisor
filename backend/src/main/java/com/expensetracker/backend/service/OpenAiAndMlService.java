package com.expensetracker.backend.service;

import com.expensetracker.backend.dto.ChatRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.util.*;

@Service
public class OpenAiAndMlService {

    private final RestTemplate rest;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ml.service.baseurl:http://localhost:8000}")
    private String mlBaseUrl;

    @Value("${openai.api.key}")
    private String openAiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openAiModel;

    public OpenAiAndMlService() {
        this.rest = new RestTemplate();
    }

    public Map<String, Object> analyzeWithMl(String userId, Integer window) {
        try {
            String url = String.format("%s/analyze/%s", mlBaseUrl.replaceAll("/+$", ""), userId);
            if (window != null) url += "?window=" + window;
            ResponseEntity<String> resp = rest.getForEntity(url, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                JsonNode json = mapper.readTree(resp.getBody());
                return mapper.convertValue(json, Map.class);
            }
        } catch (Exception e) {
            Map<String,Object> err = new HashMap<>();
            err.put("error", "ML service error: " + e.getMessage());
            return err;
        }
        return Collections.emptyMap();
    }

    public String generateOpenAiReply(String userMessage, Map<String,Object> mlAnalysis) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiKey);
            String systemPrompt = "You are a friendly financial advisor assistant that uses provided spending analysis and gives short, actionable advice in a friendly tone.";

            StringBuilder userBuilder = new StringBuilder();
            userBuilder.append("User query: ").append(userMessage).append("\n\n");
            userBuilder.append("ML analysis (JSON): ").append(mapper.writeValueAsString(mlAnalysis)).append("\n\n");
            userBuilder.append("Please give a concise answer (2-6 sentences) referencing the analysis where appropriate and one concrete action the user can take.");

            Map<String, Object> body = new HashMap<>();
            body.put("model", openAiModel);
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role","system","content", systemPrompt));
            messages.add(Map.of("role","user","content", userBuilder.toString()));
            body.put("messages", messages);
            body.put("max_tokens", 350);
            body.put("temperature", 0.7);

            HttpEntity<Map<String,Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = rest.postForEntity(url, request, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                JsonNode root = mapper.readTree(resp.getBody());
                JsonNode choice = root.path("choices").get(0);
                if (choice != null) {
                    String content = choice.path("message").path("content").asText("");
                    return content.trim();
                }
            } else {
                return "Sorry, the AI service returned a non-OK response.";
            }
        } catch (Exception e) {
            return "AI generation failed: " + e.getMessage();
        }
        return "Sorry, I could not generate a reply.";
    }
}
