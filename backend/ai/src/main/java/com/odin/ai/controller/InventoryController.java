package com.odin.ai.controller;

import com.odin.ai.model.InventoryItem;
import com.odin.ai.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/inventory")
@Validated
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @PostMapping
    public InventoryItem addItem(@RequestBody InventoryItem item) {
        return inventoryService.addItem(item);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> getItem(@PathVariable String id) {
        Optional<InventoryItem> item = inventoryService.getItem(id);
        return item.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<InventoryItem> getAllItems() {
        return inventoryService.getAllItems();
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryItem> updateItem(@PathVariable String id, @RequestBody InventoryItem item) {
        try {
            InventoryItem updatedItem = inventoryService.updateItem(id, item);
            return ResponseEntity.ok(updatedItem);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        try {
            inventoryService.deleteItem(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/reduce-stock")
    public ResponseEntity<InventoryItem> reduceStock(@PathVariable String id, @RequestParam int quantity) {
        try {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }
            InventoryItem item = inventoryService.reduceStock(id, quantity);
            return ResponseEntity.ok(item);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @GetMapping("/{id}/check-reorder")
    public ResponseEntity<Boolean> checkStockAndReorder(@PathVariable String id) {
        boolean needsReorder = inventoryService.checkStockAndReorder(id);
        return ResponseEntity.ok(needsReorder);
    }

    @GetMapping("/optimize/{productName}")
    public ResponseEntity<String> optimizeStock(@PathVariable String productName) {
        String result = inventoryService.optimizeStock(productName);
        return ResponseEntity.ok(result);
    }
}