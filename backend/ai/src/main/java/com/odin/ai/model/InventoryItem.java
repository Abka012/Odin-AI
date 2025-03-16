package com.odin.ai.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Document(collection = "inventory")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryItem {
    @Id
    private String id;

    @NotNull(message = "Product name cannot be null")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    private String productName;

    @NotNull(message = "Product type cannot be null")
    private String productType;

    @Positive(message = "Stock level must be positive")
    private double stockLevel; // Kept as double as per your update

    @Positive(message = "Reorder threshold must be positive")
    private int reorderThreshold;

    @Positive(message = "Price must be positive")
    private double price;

    private LocalDateTime dateAdded;

    private LocalDateTime lifeExpectancy;

    @NotNull(message = "Supplier name cannot be null")
    @Size(min = 2, max = 100, message = "Supplier name must be between 2 and 100 characters")
    private String supplierName;

    @NotNull(message = "Category cannot be null")
    @Size(min = 2, max = 50, message = "Category must be between 2 and 50 characters")
    private String category;

    private boolean isActive;
}