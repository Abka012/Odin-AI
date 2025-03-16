package com.odin.ai.controller;

import com.odin.ai.model.InventoryItem;
import com.odin.ai.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reorder")
public class InventoryReorder {

    @Autowired
    private InventoryService inventoryService;
    
    @GetMapping
    public List<InventoryDTO> getLowStockItems() {
        List<InventoryItem> allItems = inventoryService.getAllItems();
        return InventoryDTO.filterAndConvert(allItems); 
    }
}
