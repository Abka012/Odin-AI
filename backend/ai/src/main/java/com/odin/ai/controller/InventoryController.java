package com.odin.ai.controller;

import com.odin.ai.model.InventoryItem;
import com.odin.ai.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "http://localhost:8080")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    // Core Inventory Endpoints (Home Section)
    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAllItems() {
        List<InventoryItem> items = inventoryService.getAllItems();
        return ResponseEntity.ok(items); // Populates inventory-table-body
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> getItemById(@PathVariable String id) {
        System.out.println("Fetching item with ID: " + id);
        Optional<InventoryItem> itemOpt = inventoryService.getItem(id);
        return itemOpt.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.notFound().build()); // For editProduct
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<InventoryItem>> getItemsByCategory(@PathVariable String category) {
        List<InventoryItem> items = inventoryService.getItemsByCategory(category);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addItem(@Valid @RequestBody InventoryItem item) {
        System.out.println("Received item: " + item);
        InventoryItem savedItem = inventoryService.addItem(item);
        Map<String, Object> response = new HashMap<>();
        response.put("item", savedItem);
        response.put("message", savedItem.getStockLevel() > 0 ? "New item added" : "Item stock updated");
        return ResponseEntity.ok(response); // Matches addProduct response expectation
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> updateItem(@PathVariable String id, @Valid @RequestBody InventoryItem item) {
        System.out.println("Updating item with ID: " + id + " to: " + item);
        InventoryItem updatedItem = inventoryService.updateItem(id, item);
        return ResponseEntity.ok(updatedItem); // Matches updateProduct expectation
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        System.out.println("Deleting item with ID: " + id);
        try {
            inventoryService.deleteItem(id);
            return ResponseEntity.noContent().build(); // Matches deleteProduct
        } catch (Exception e) {
            System.out.println("Error deleting item: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // Stock Management Endpoints
    @GetMapping("/{id}/reorder")
    public ResponseEntity<Boolean> checkStockAndReorder(@PathVariable String id) {
        boolean needsReorder = inventoryService.checkStockAndReorder(id);
        return ResponseEntity.ok(needsReorder);
    }

    @PutMapping("/{id}/reduce")
    public ResponseEntity<InventoryItem> reduceStock(@PathVariable String id, @RequestParam double quantity) {
        InventoryItem updatedItem = inventoryService.reduceStock(id, quantity);
        return ResponseEntity.ok(updatedItem);
    }

    // Forecasting and Optimization
    @GetMapping("/forecast/{productName}")
    public ResponseEntity<Double> getDemandForecast(@PathVariable String productName) {
        double forecast = inventoryService.getDemandForecast(productName);
        if (forecast == -1.0) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(null);
        }
        return ResponseEntity.ok(forecast);
    }

    @PostMapping("/optimize/{productName}")
    public ResponseEntity<String> optimizeStock(@PathVariable String productName) {
        String result = inventoryService.optimizeStock(productName);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/reorder-needed")
    public ResponseEntity<List<InventoryItem>> getItemsNeedingReorder() {
        List<InventoryItem> items = inventoryService.getItemsNeedingReorder();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/expiring-soon")
    public ResponseEntity<List<InventoryItem>> getItemsNearingExpiration(@RequestParam(defaultValue = "2") int months) {
        List<InventoryItem> items = inventoryService.getItemsNearingExpiration(months);
        return ResponseEntity.ok(items);
    }

    // ML-Driven Endpoints (Demand Report Section)
    @GetMapping("/reorder-list")
    public ResponseEntity<List<Map<String, Object>>> getReorderList() {
        List<Map<String, Object>> reorderList = inventoryService.getReorderList();
        return ResponseEntity.ok(reorderList); // Always return 200 OK, even if empty
    }

    @GetMapping("/supplier-scorecard")
    public ResponseEntity<List<Map<String, Object>>> getSupplierScorecard() {
        List<Map<String, Object>> scorecard = inventoryService.getSupplierScorecard();
        if (scorecard.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList(Map.of("error", "Failed to fetch supplier scorecard")));
        }
        return ResponseEntity.ok(scorecard);
    }

    @GetMapping("/expiration-alerts")
    public ResponseEntity<List<Map<String, Object>>> getExpirationAlerts() {
        List<Map<String, Object>> alerts = inventoryService.getExpirationAlerts();
        if (alerts.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList(Map.of("error", "Failed to fetch expiration alerts")));
        }
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/stockout-predictions")
    public ResponseEntity<List<Map<String, Object>>> getStockoutPredictions() {
        List<Map<String, Object>> predictions = inventoryService.getStockoutPredictions();
        if (predictions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Collections.singletonList(Map.of("error", "Failed to fetch stockout predictions")));
        }
        return ResponseEntity.ok(predictions);
    }

    // Exception Handling
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}