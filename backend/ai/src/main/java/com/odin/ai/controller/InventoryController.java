package com.odin.ai.controller;

import com.odin.ai.model.InventoryItem;
import com.odin.ai.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    // Get all items
    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAllItems() {
        List<InventoryItem> items = inventoryService.getAllItems();
        return ResponseEntity.ok(items);
    }

    // Get items by category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<InventoryItem>> getItemsByCategory(@PathVariable String category) {
        List<InventoryItem> items = inventoryService.getItemsByCategory(category);
        return ResponseEntity.ok(items);
    }

    // Add a new item or update existing, return total value if updated
    @PostMapping
    public ResponseEntity<Map<String, Object>> addItem(@Valid @RequestBody InventoryItem item) {
        InventoryItem savedItem = inventoryService.addItem(item);
        Map<String, Object> response = new HashMap<>();
        response.put("item", savedItem);

        // Check if this was an update (item existed before)
        if (inventoryService.getAllItems().stream()
                .filter(i -> i.getProductName().equals(item.getProductName()))
                .count() == 1) { // Only one item with this name should exist due to addItem logic
            double totalValue = inventoryService.getTotalInventoryValue();
            response.put("totalInventoryValue", totalValue);
            response.put("message", "Item stock updated, total inventory value returned");
        } else {
            response.put("message", "New item added");
        }

        return ResponseEntity.ok(response);
    }

    // Check reorder status for an item
    @GetMapping("/{id}/reorder")
    public ResponseEntity<Boolean> checkStockAndReorder(@PathVariable String id) {
        boolean needsReorder = inventoryService.checkStockAndReorder(id);
        return ResponseEntity.ok(needsReorder);
    }

    // Reduce stock level
    @PutMapping("/{id}/reduce")
    public ResponseEntity<InventoryItem> reduceStock(@PathVariable String id, @RequestParam double quantity) {
        InventoryItem updatedItem = inventoryService.reduceStock(id, quantity);
        return ResponseEntity.ok(updatedItem);
    }

    // Get demand forecast
    @GetMapping("/forecast/{productName}")
    public ResponseEntity<Double> getDemandForecast(@PathVariable String productName) {
        double forecast = inventoryService.getDemandForecast(productName);
        return ResponseEntity.ok(forecast);
    }

    // Optimize stock based on forecast
    @PostMapping("/optimize/{productName}")
    public ResponseEntity<String> optimizeStock(@PathVariable String productName) {
        String result = inventoryService.optimizeStock(productName);
        return ResponseEntity.ok(result);
    }

    // Get items needing reorder
    @GetMapping("/reorder-needed")
    public ResponseEntity<List<InventoryItem>> getItemsNeedingReorder() {
        List<InventoryItem> items = inventoryService.getItemsNeedingReorder();
        return ResponseEntity.ok(items);
    }

    // Get items nearing expiration
    @GetMapping("/expiring-soon")
    public ResponseEntity<List<InventoryItem>> getItemsNearingExpiration(@RequestParam(defaultValue = "2") int months) {
        List<InventoryItem> items = inventoryService.getItemsNearingExpiration(months);
        return ResponseEntity.ok(items);
    }
}