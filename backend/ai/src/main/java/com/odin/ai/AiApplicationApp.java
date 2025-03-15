package com.odin.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.odin.ai.model.InventoryItem;
import com.odin.ai.service.InventoryService;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication
@EnableMongoRepositories
public class AiApplicationApp {
    public static void main(String[] args) {
        try {
            System.out.println("Starting AiApplicationApp...");
            ApplicationContext context = SpringApplication.run(AiApplicationApp.class, args);
            InventoryService service = context.getBean(InventoryService.class);
            System.out.println("Application started successfully!");

            LocalDateTime now = LocalDateTime.now();

            System.out.println("Adding test items...");
            addItemWithErrorHandling(service, new InventoryItem(null, "Milk", "Dairy", 10.0, 5, 999.99, now, now.plusMonths(1), "FarmFresh", "Beverages", true));
            addItemWithErrorHandling(service, new InventoryItem(null, "Chicken", "Meat", 50.0, 10, 29.99, now, now.plusMonths(3), "MeatCo", "Protein", true));
            addItemWithErrorHandling(service, new InventoryItem(null, "Apple Juice", "Natural Juice", 3.0, 5, 59.99, now, now.plusMonths(6), "JuiceWorks", "Beverages", true));
            System.out.println("Test items added successfully!");

            System.out.println("\n=== Inventory Status ===");
            List<InventoryItem> items = service.getAllItems();
            for (InventoryItem item : items) {
                System.out.printf("Item: %s%n", item.getProductName());
                System.out.printf("Stock Level: %.1f%n", item.getStockLevel());
                System.out.printf("Price: $%.2f%n", item.getPrice());
                System.out.printf("Supplier: %s%n", item.getSupplierName());
                boolean needsReorder = service.checkStockAndReorder(item.getId());
                System.out.printf("Needs reorder: %b%n", needsReorder);
                boolean nearingExpiration = item.getLifeExpectancy().isBefore(now.plusMonths(2));
                System.out.printf("Nearing expiration: %b%n", nearingExpiration);
                System.out.println("------------------------");
            }

            System.out.println("\n=== Items in Beverages Category ===");
            List<InventoryItem> beverages = service.getItemsByCategory("Beverages");
            beverages.forEach(item -> 
                System.out.printf("%s - $%.2f%n", item.getProductName(), item.getPrice())
            );

            double totalValue = items.stream()
                .mapToDouble(item -> item.getPrice() * item.getStockLevel())
                .sum();
            System.out.printf("%nTotal Inventory Value: $%.2f%n", totalValue);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addItemWithErrorHandling(InventoryService service, InventoryItem item) {
        try {
            service.addItem(item);
            System.out.println("Added: " + item.getProductName());
        } catch (Exception e) {
            System.err.println("Failed to add " + item.getProductName() + ": " + e.getMessage());
        }
    }
}