package com.odin.ai.dto;

public class InventoryDTO {
    private String item;
    private int stockLevel;
    private int price;

    public InventoryDTO(String item, int stockLevel, int price) {
        this.item = item;
        this.stockLevel = stockLevel;
        this.price = price;
    }

    public String getItem() { return item; }
    public int getStockLevel() { return stockLevel; }
    public int getPrice() { return price; }
    
    public static List<InventoryDTO> filterAndConvert(List<InventoryItem> inventoryItems) {
        return inventoryItems.stream().filter(item -> item.getStockLevel() < 10).map(item -> new InventoryDTO(item.getItem(), item.getStockLevel(), item.getPrice())).collect(Collectors.toList());
    }
}
