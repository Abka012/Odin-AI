<template>
    <nav class="sidebar">
      <ul>
        <li @click="navigateTo('inventory')">📦 Inventory</li>
        <li @click="navigateTo('trends')">📊 Market Trends</li>
        <li @click="navigateTo('expiry')">⏳ Expiry Tracking</li>
        <li @click="navigateTo('orders')">🛍️ Recommended Orders</li>
        <li @click="navigateTo('pricing')">💰 Pricing</li>
        <li @click="navigateTo('stock')">📈 Stock Tracking</li>
      </ul>
    </nav>
  </template>
  
  <script>
  export default {
    methods: {
      navigateTo(section) {
        this.$emit("change-section", section);
      }
    }
  };
  </script>
  