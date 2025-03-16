package com.odin.ai.repository;

import com.odin.ai.model.InventoryItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends MongoRepository<InventoryItem, String> {
    List<InventoryItem> findByCategory(String category);
    Optional<InventoryItem> findByProductName(String productName);
}