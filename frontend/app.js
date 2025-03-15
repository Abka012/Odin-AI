const { createApp } = Vue;

createApp({
    data() {
        return {
            marketTrends: [],
            inventory: [
                { name: "Apples", stock: 50, price: 2.5, life: 7 },
                { name: "Bananas", stock: 30, price: 1.8, life: 5 },
                { name: "Milk", stock: 20, price: 3.0, life: 10 }
            ],
            discountRecommendations: []
        };
    },
    methods: {
        async fetchMarketTrends() {
            try {
                const response = await fetch("https://api.example.com/market-trends");
                const data = await response.json();
                this.marketTrends = data.trends; // Assuming API returns `{ trends: [...] }`
            } catch (error) {
                console.error("Error fetching AI trends:", error);
                this.marketTrends = [
                    { item: "Apples", trend: "Stable" },
                    { item: "Bananas", trend: "Rising" },
                    { item: "Milk", trend: "Falling" }
                ];
            }
        },
        restockItem(item) {
            item.stock += 10;
            this.calculateDiscounts();
        },
        calculateDiscounts() {
            this.discountRecommendations = this.inventory
                .filter(item => item.stock > 40)
                .map(item => ({ item: item.name, percentage: 15 }));
        }
    },
    mounted() {
        this.fetchMarketTrends();
        this.calculateDiscounts();
    }
}).mount("#app");
