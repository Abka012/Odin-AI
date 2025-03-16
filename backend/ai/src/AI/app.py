from flask import Flask, jsonify, Response
import pandas as pd
import requests
from datetime import datetime

app = Flask(__name__)

def fetch_inventory_data():
    try:
        response = requests.get("http://localhost:8080/api/inventory")
        response.raise_for_status()
        data = response.json()
        if not data:
            print("No inventory data received from Spring Boot")
            return pd.DataFrame()
        df = pd.DataFrame(data)
        df['dateAdded'] = pd.to_datetime(df['dateAdded'], format='ISO8601')
        df['lifeExpectancy'] = pd.to_datetime(df['lifeExpectancy'], format='ISO8601')
        return df
    except Exception as e:
        print(f"Error fetching inventory data: {e}")
        return pd.DataFrame()

def format_reorder_list(data):
    if not data:
        return "- No items currently need reordering."
    return "\n".join([f"- {item['productName']}: Stock {item['stockLevel']} (Threshold {item['reorderThreshold']})" for item in data])

def format_supplier_scorecard(data):
    if not data:
        return "- No supplier data available."
    return "\n".join([f"- {item['supplierName']}: Average Price ${item['price']:.2f}" for item in data])

def format_expiration_alerts(data):
    if not data:
        return "- No expiration alerts."
    now = datetime.now()
    return "\n".join([
        f"- Product: {item['productName']}\n  - Expires: {item['lifeExpectancy'].strftime('%B %d, %Y')}\n  - Days Left: {((item['lifeExpectancy'] - now).days)}\n  - Supplier: {item['supplierName']}\n  - Price: ${item['price']:.2f}"
        for item in data
    ])

def format_stockout_predictions(data):
    if not data:
        return "- No stockout predictions available."
    return "\n".join([f"- Stock Level: {item['stockLevel']} - Risk: {'High' if item['predictedStockout'] else 'Low'}" for item in data])

@app.route("/demand-report-text", methods=["GET"])
def get_demand_report_text():
    df = fetch_inventory_data()
    reorder_items = df[df['stockLevel'] < df['reorderThreshold']].to_dict(orient="records") if not df.empty else []
    scorecard = df.groupby('supplierName').agg({'price': 'mean'}).reset_index().to_dict(orient="records") if not df.empty else []
    now = datetime.now()
    alerts = df[df['lifeExpectancy'] < pd.Timestamp(now) + pd.Timedelta(days=60)].to_dict(orient="records") if not df.empty else []
    predictions = df.assign(predictedStockout=df['stockLevel'] < 10)[['stockLevel', 'predictedStockout']].to_dict(orient="records") if not df.empty else []

    report = f"""📈 Demand Report
Analyze real-time supply and demand trends.

Reorder List
{format_reorder_list(reorder_items)}

Supplier Scorecard
{format_supplier_scorecard(scorecard)}

Expiration Alerts
{format_expiration_alerts(alerts)}

Stockout Predictions
{format_stockout_predictions(predictions)}
"""
    return Response(report, mimetype="text/plain")

# Existing JSON endpoints unchanged
@app.route("/reorder", methods=["GET"])
def get_reorder_list():
    df = fetch_inventory_data()
    if df.empty:
        return jsonify([]), 200
    reorder_items = df[df['stockLevel'] < df['reorderThreshold']]
    return jsonify(reorder_items.to_dict(orient="records"))

@app.route("/supplier-scorecard", methods=["GET"])
def get_supplier_scorecard():
    df = fetch_inventory_data()
    if df.empty:
        return jsonify([]), 200
    scorecard = df.groupby('supplierName').agg({'price': 'mean'}).reset_index()
    return jsonify(scorecard.to_dict(orient="records"))

@app.route("/expiration-alerts", methods=["GET"])
def get_expiration_alerts():
    df = fetch_inventory_data()
    if df.empty:
        return jsonify([]), 200
    now = datetime.now()
    alerts = df[df['lifeExpectancy'] < pd.Timestamp(now) + pd.Timedelta(days=60)]
    return jsonify(alerts.to_dict(orient="records"))

@app.route("/predict-stockouts", methods=["GET"])
def predict_stockouts_api():
    df = fetch_inventory_data()
    if df.empty:
        return jsonify([]), 200
    df['predictedStockout'] = df['stockLevel'] < 10
    return jsonify(df[['stockLevel', 'predictedStockout']].to_dict(orient="records"))

if __name__ == "__main__":
    print("Starting Flask API on port 5000...")
    app.run(debug=True, port=5000)