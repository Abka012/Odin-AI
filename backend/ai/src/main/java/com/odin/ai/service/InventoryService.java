package com.odin.ai.service;

import com.odin.ai.model.InventoryItem;
import com.odin.ai.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    // RestTemplate for calling the Flask forecasting service
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InventoryItem addItem(InventoryItem item) {
        // Check if an item with the same productName exists
        Optional<InventoryItem> existingItem = inventoryRepository.findByProductName(item.getProductName());
        if (existingItem.isPresent()) {
            // Update existing item's stock level
            InventoryItem currentItem = existingItem.get();
            currentItem.setStockLevel(currentItem.getStockLevel() + item.getStockLevel());
            return inventoryRepository.save(currentItem); // Save updated item
        } else {
            // Add new item
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
        List<InventoryItem> items = inventoryRepository.findAll();
        return items.stream()
                .mapToDouble(item -> item.getPrice() * item.getStockLevel())
                .sum();
    }

    public List<InventoryItem> getItemsByCategory(String category) {
        return inventoryRepository.findAll().stream()
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

    /**
     * Checks if an item is nearing expiration and logs a warning.
     * @param item The inventory item to check.
     */
    private void checkExpiration(InventoryItem item) {
        LocalDateTime now = LocalDateTime.now();
        if (item.getLifeExpectancy() != null && item.getLifeExpectancy().isBefore(now.plusMonths(2))) {
            System.out.println("Warning: " + item.getProductName() + " nearing expiration on " + item.getLifeExpectancy());
        }
    }

    /**
     * Retrieves the forecasted demand for a product from the Flask AI service.
     * @param productName The name of the product to forecast demand for.
     * @return The forecasted demand as a double, or -1 if an error occurs.
     */
    public double getDemandForecast(String productName) { // Changed return type to double
        try {
            String response = restTemplate.getForObject("http://localhost:5000/predict/" + productName, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.get("forecastedDemand").asDouble(); // Changed to asDouble
        } catch (Exception e) {
            System.err.println("Error fetching forecast for " + productName + ": " + e.getMessage());
            return -1.0; // Changed to -1.0
        }
    }

    /**
     * Optimizes the stock level for a product based on the forecasted demand.
     * @param productName The name of the product to optimize.
     * @return A message indicating the result of the optimization.
     */
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

    /**
     * Gets items that need reordering.
     * @return List of items below reorder threshold.
     */
    public List<InventoryItem> getItemsNeedingReorder() {
        return getAllItems().stream()
                .filter(item -> item.getStockLevel() <= item.getReorderThreshold())
                .collect(Collectors.toList());
    }

    /**
     * Gets items nearing expiration within a specified number of months.
     * @param months The number of months to check ahead.
     * @return List of items nearing expiration.
     */
    public List<InventoryItem> getItemsNearingExpiration(int months) {
        LocalDateTime threshold = LocalDateTime.now().plusMonths(months);
        return getAllItems().stream()
                .filter(item -> item.getLifeExpectancy() != null && item.getLifeExpectancy().isBefore(threshold))
                .collect(Collectors.toList());
    }
}