from flask import Flask, jsonify
import random

app = Flask(__name__)

# Mock historical data or logic (replace with real AI model later)
def get_forecasted_demand(product_id):
    # Simulate demand based on product_id (e.g., higher demand for popular items)
    base_demand = 50
    if product_id == "Laptop" or product_id == "Keyboard":
        base_demand += random.randint(10, 20)  # Higher demand for tech items
    elif product_id == "Mouse":
        base_demand += random.randint(5, 15)   # Moderate demand
    return base_demand + random.randint(-5, 5)  # Add some randomness

@app.route('/predict/<product_id>')
def predict(product_id):
    forecasted_demand = get_forecasted_demand(product_id)
    return jsonify({
        "productId": product_id,
        "forecastedDemand": forecasted_demand
    })

if __name__ == '__main__':
    app.run(port=5000, debug=True)