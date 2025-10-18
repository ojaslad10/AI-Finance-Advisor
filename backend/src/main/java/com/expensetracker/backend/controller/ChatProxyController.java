package com.expensetracker.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatProxyController {
    private static final Logger log = LoggerFactory.getLogger(ChatProxyController.class);

    private RestTemplate createRestTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        return new RestTemplate(factory);
    }

    @PostMapping("/chat/proxy")
    public ResponseEntity<?> proxyChat(@RequestHeader(value="Authorization", required=false) String auth,
                                       @RequestBody Map<String,Object> body) {
        log.info("Entered ChatProxyController.proxyChat");
        log.info("Incoming auth header present: {}", auth != null && !auth.isEmpty());
        log.info("Incoming body: {}", body);

        RestTemplate rest = createRestTemplate();
        String pythonUrl = "http://127.0.0.1:8000/api/chat";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (auth != null && !auth.isBlank()) headers.set("Authorization", auth);

        HttpEntity<Map<String,Object>> req = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> resp = rest.postForEntity(pythonUrl, req, String.class);

            log.info("Upstream responded: status={}, contentType={}", resp.getStatusCode(), resp.getHeaders().getContentType());
            log.debug("Upstream body (truncated to 2000 chars): {}", resp.getBody() == null ? "null"
                    : (resp.getBody().length() > 2000 ? resp.getBody().substring(0,2000) + "..." : resp.getBody()));

            HttpHeaders out = new HttpHeaders();
            if (resp.getHeaders().getContentType() != null) out.setContentType(resp.getHeaders().getContentType());
            return new ResponseEntity<>(resp.getBody(), out, resp.getStatusCode());

        } catch (HttpStatusCodeException e) {
            // upstream returned 4xx or 5xx
            log.error("Upstream returned HTTP error status: {}", e.getStatusCode(), e);
            String upstreamBody = e.getResponseBodyAsString();
            Map<String, String> err = Map.of(
                    "error", "Upstream returned error",
                    "status", String.valueOf(e.getStatusCode().value()),
                    "upstreamBody", upstreamBody == null ? "null" : upstreamBody
            );
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(err);
        } catch (ResourceAccessException e) {
            // connection/timeouts
            log.error("Proxy cannot reach Python service (ResourceAccessException)", e);
            Map<String, String> err = Map.of(
                    "error", "Proxy cannot reach Python service",
                    "exception", e.getClass().getName(),
                    "message", e.getMessage() == null ? "null" : e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(err);
        } catch (Exception e) {
            log.error("Unexpected proxy exception", e);
            Map<String, String> err = Map.of(
                    "error", "Proxy to Python failed",
                    "exception", e.getClass().getName(),
                    "message", e.getMessage() == null ? "null" : e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }
}
