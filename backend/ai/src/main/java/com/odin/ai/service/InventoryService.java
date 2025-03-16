package com.odin.ai.service;

import com.odin.ai.model.InventoryItem;
import com.odin.ai.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Existing CRUD methods (unchanged)
    public InventoryItem addItem(InventoryItem item) {
        Optional<InventoryItem> existingItem = inventoryRepository.findByProductName(item.getProductName());
        if (existingItem.isPresent()) {
            InventoryItem currentItem = existingItem.get();
            currentItem.setStockLevel(currentItem.getStockLevel() + item.getStockLevel());
            return inventoryRepository.save(currentItem);
        } else {
            return inventoryRepository.save(item);
        }
    }

    public Optional<InventoryItem> getItem(String id) {
        return inventoryRepository.findById(id);
    }

    public List<InventoryItem> getAllItems() {
        return inventoryRepository.findAll();
    }

    public double getTotalInventoryValue() {
        return getAllItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getStockLevel())
                .sum();
    }

    public List<InventoryItem> getItemsByCategory(String category) {
        return getAllItems().stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public InventoryItem updateItem(String id, InventoryItem item) {
        item.setId(id);
        return inventoryRepository.save(item);
    }

    public void deleteItem(String id) {
        inventoryRepository.deleteById(id);
    }

    public boolean checkStockAndReorder(String id) {
        Optional<InventoryItem> itemOpt = inventoryRepository.findById(id);
        if (itemOpt.isPresent()) {
            InventoryItem item = itemOpt.get();
            if (item.getStockLevel() <= item.getReorderThreshold()) {
                System.out.println("Reorder needed for " + item.getProductName() + " (Current: " + item.getStockLevel() + ")");
                return true;
            }
        }
        return false;
    }

    public InventoryItem reduceStock(String id, double quantity) {
        Optional<InventoryItem> itemOpt = inventoryRepository.findById(id);
        if (itemOpt.isPresent()) {
            InventoryItem item = itemOpt.get();
            if (item.getStockLevel() >= quantity) {
                item.setStockLevel(item.getStockLevel() - quantity);
                inventoryRepository.save(item);
                checkStockAndReorder(id);
                checkExpiration(item);
                return item;
            }
            throw new RuntimeException("Insufficient stock for " + item.getProductName());
        }
        throw new RuntimeException("Item not found");
    }

    private void checkExpiration(InventoryItem item) {
        LocalDateTime now = LocalDateTime.now();
        if (item.getLifeExpectancy() != null && item.getLifeExpectancy().isBefore(now.plusMonths(2))) {
            System.out.println("Warning: " + item.getProductName() + " nearing expiration on " + item.getLifeExpectancy());
        }
    }

    public double getDemandForecast(String productName) {
        try {
            String response = restTemplate.getForObject("http://localhost:5000/predict/" + productName, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.get("forecastedDemand").asDouble();
        } catch (Exception e) {
            System.err.println("Error fetching forecast for " + productName + ": " + e.getMessage());
            return -1.0;
        }
    }

    public String optimizeStock(String productName) {
        double forecastedDemand = getDemandForecast(productName);
        if (forecastedDemand == -1.0) {
            return "Failed to optimize stock: Could not fetch forecast";
        }
        Optional<InventoryItem> itemOpt = getAllItems().stream()
                .filter(item -> item.getProductName().equals(productName))
                .findFirst();
        if (itemOpt.isPresent()) {
            InventoryItem item = itemOpt.get();
            if (item.getStockLevel() < forecastedDemand) {
                item.setStockLevel(forecastedDemand);
                updateItem(item.getId(), item);
                return "Stock optimized for " + productName + " to " + forecastedDemand;
            }
            return "Stock sufficient for " + productName + " (Current: " + item.getStockLevel() + ", Forecast: " + forecastedDemand + ")";
        }
        return "Product " + productName + " not found";
    }

    public List<InventoryItem> getItemsNeedingReorder() {
        return getAllItems().stream()
                .filter(item -> item.getStockLevel() <= item.getReorderThreshold())
                .collect(Collectors.toList());
    }

    public List<InventoryItem> getItemsNearingExpiration(int months) {
        LocalDateTime threshold = LocalDateTime.now().plusMonths(months);
        return getAllItems().stream()
                .filter(item -> item.getLifeExpectancy() != null && item.getLifeExpectancy().isBefore(threshold))
                .collect(Collectors.toList());
    }

    // ML-driven methods
    public List<Map<String, Object>> getReorderList() {
        try {
            String response = restTemplate.getForObject("http://localhost:5000/reorder", String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            return objectMapper.convertValue(jsonNode, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            System.err.println("Error fetching reorder list: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getSupplierScorecard() {
        try {
            String response = restTemplate.getForObject("http://localhost:5000/supplier-scorecard", String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            return objectMapper.convertValue(jsonNode, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            System.err.println("Error fetching supplier scorecard: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getExpirationAlerts() {
        try {
            String response = restTemplate.getForObject("http://localhost:5000/expiration-alerts", String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            return objectMapper.convertValue(jsonNode, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            System.err.println("Error fetching expiration alerts: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> getStockoutPredictions() {
        try {
            String response = restTemplate.getForObject("http://localhost:5000/predict-stockouts", String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            return objectMapper.convertValue(jsonNode, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            System.err.println("Error fetching stockout predictions: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}