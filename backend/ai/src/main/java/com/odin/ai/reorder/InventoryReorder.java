package com.odin.ai.reorder;

import com.odin.ai.model.InventoryItem;
import com.odin.ai.service.InventoryService;
import com.odin.ai.dto.InventoryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

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
