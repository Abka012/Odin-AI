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

    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAllItems() {
        List<InventoryItem> items = inventoryService.getAllItems();
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> getItemById(@PathVariable String id) {
        System.out.println("Fetching item with ID: " + id);
        Optional<InventoryItem> itemOpt = inventoryService.getItem(id);
        return itemOpt.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.notFound().build());
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
        if (inventoryService.getAllItems().stream()
                .filter(i -> i.getProductName().equals(item.getProductName()))
                .count() == 1) {
            double totalValue = inventoryService.getTotalInventoryValue();
            response.put("totalInventoryValue", totalValue);
            response.put("message", "Item stock updated, total inventory value returned");
        } else {
            response.put("message", "New item added");
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        System.out.println("Deleting item with ID: " + id);
        try {
            inventoryService.deleteItem(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            System.out.println("Error deleting item: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

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

    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> updateItem(@PathVariable String id, @Valid @RequestBody InventoryItem item) {
        System.out.println("Updating item with ID: " + id + " to: " + item);
        InventoryItem updatedItem = inventoryService.updateItem(id, item);
        return ResponseEntity.ok(updatedItem);
    }

    @GetMapping("/forecast/{productName}")
    public ResponseEntity<Double> getDemandForecast(@PathVariable String productName) {
        double forecast = inventoryService.getDemandForecast(productName);
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}