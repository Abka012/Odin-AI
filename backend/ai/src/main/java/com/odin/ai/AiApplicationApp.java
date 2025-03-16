package com.odin.ai;

import com.odin.ai.model.InventoryItem;
import com.odin.ai.service.InventoryService;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.odin.ai.repository")
public class AiApplicationApp {
    public static void main(String[] args) {
        System.out.println("Starting AiApplicationApp...");

        // Load .env file for MongoDB configuration
        Dotenv dotenv = Dotenv.configure()
                .directory("./") // Assumes .env is in project root
                .ignoreIfMissing() // Won't fail if .env is missing
                .load();
        String mongoUri = dotenv.get("DATABASE_URL");
        System.out.println("Loaded DATABASE_URL: " + mongoUri);
        if (mongoUri == null) {
            System.err.println("WARNING: DATABASE_URL not found in .env! Ensure .env is in project root or use application.properties.");
        }

        // Set MongoDB properties before Spring starts
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.data.mongodb.uri", mongoUri != null ? mongoUri : "mongodb://localhost:27017/retail_inventory");
        properties.put("spring.data.mongodb.database", "retail_inventory");
        SpringApplication app = new SpringApplication(AiApplicationApp.class);
        app.setDefaultProperties(properties);

        ApplicationContext context;
        try {
            context = app.run(args);
        } catch (Exception e) {
            System.err.println("Failed to start application: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        InventoryService service = context.getBean(InventoryService.class);
        System.out.println("Application started successfully!");

        LocalDateTime now = LocalDateTime.now();

        System.out.println("Adding test items...");
        addItemWithRetry(service, new InventoryItem(null, "Milk", "Dairy", 10.0, 5, 999.99, now, now.plusMonths(1), "FarmFresh", "Beverages", true));
        addItemWithRetry(service, new InventoryItem(null, "Chicken", "Meat", 50.0, 10, 29.99, now, now.plusMonths(3), "MeatCo", "Protein", true));
        addItemWithRetry(service, new InventoryItem(null, "Apple Juice", "Natural Juice", 3.0, 5, 59.99, now, now.plusMonths(6), "JuiceWorks", "Beverages", true));
        System.out.println("Test items added successfully!");

        System.out.println("\n=== Inventory Status ===");
        try {
            List<InventoryItem> items = service.getAllItems();
            if (items.isEmpty()) {
                System.out.println("No items found in inventory.");
            }
            for (InventoryItem item : items) {
                System.out.printf("Item: %s%n", item.getProductName());
                System.out.printf("Stock Level: %.1f%n", item.getStockLevel());
                System.out.printf("Price: $%.2f%n", item.getPrice());
                System.out.printf("Supplier: %s%n", item.getSupplierName());
                System.out.printf("Date Added: %s%n", item.getDateAdded());
                System.out.printf("Life Expectancy: %s%n", item.getLifeExpectancy());
                boolean needsReorder = service.checkStockAndReorder(item.getId());
                System.out.printf("Needs reorder: %b%n", needsReorder);
                boolean nearingExpiration = item.getLifeExpectancy() != null && item.getLifeExpectancy().isBefore(now.plusMonths(2));
                System.out.printf("Nearing expiration: %b%n", nearingExpiration);
                System.out.println("------------------------");
            }

            System.out.println("\n=== Items in Beverages Category ===");
            List<InventoryItem> beverages = service.getItemsByCategory("Beverages");
            if (beverages.isEmpty()) {
                System.out.println("No beverages found.");
            } else {
                beverages.forEach(item ->
                    System.out.printf("%s - $%.2f (Expires: %s)%n", item.getProductName(), item.getPrice(), item.getLifeExpectancy())
                );
            }

            double totalValue = service.getTotalInventoryValue();
            System.out.printf("%nTotal Inventory Value: $%.2f%n", totalValue);
        } catch (Exception e) {
            System.err.println("Failed to process inventory data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addItemWithRetry(InventoryService service, InventoryItem item) {
        int retries = 3;
        for (int i = 0; i < retries; i++) {
            try {
                service.addItem(item);
                System.out.println("Added: " + item.getProductName());
                return;
            } catch (Exception e) {
                System.err.println("Attempt " + (i + 1) + " failed to add " + item.getProductName() + ": " + e.getMessage());
                if (i < retries - 1) {
                    try {
                        Thread.sleep(5000); // Wait 5 seconds before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        System.err.println("Retry interrupted: " + ie.getMessage());
                    }
                }
            }
        }
        System.err.println("Failed to add " + item.getProductName() + " after " + retries + " attempts.");
    }
}