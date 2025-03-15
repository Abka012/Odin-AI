package com.odin.ai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/{productId}")
    public ResponseEntity<String> getDemandForecast(@PathVariable String productId) {
        try {
            // Call the Flask API
            String response = restTemplate.getForObject("http://localhost:5000/predict/" + productId, String.class);

            // Parse the JSON response to extract forecastedDemand
            JsonNode jsonNode = objectMapper.readTree(response);
            int forecastedDemand = jsonNode.get("forecastedDemand").asInt();

            // Return the forecast as a string
            return ResponseEntity.ok("Forecasted demand for " + productId + ": " + forecastedDemand);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching forecast: " + e.getMessage());
        }
    }
}