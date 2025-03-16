from flask import Flask, jsonify
from forecast_service import (
    read_data_set,
    automate_reordering,
    supplier_scorecard,
    expiration_alerts,
)
from prediction import predict_stockouts

app = Flask(__name__)

# Load dataset once when the server starts
df = read_data_set("grocery_dataset.csv")


# API Routes
@app.route("/reorder", methods=["GET"])
def get_reorder_list():
    reorder_list = automate_reordering(df)
    return jsonify(reorder_list)

@app.route("/supplier-scorecard", methods=["GET"])
def get_supplier_scorecard():
    scorecard = supplier_scorecard(df)
    return jsonify(scorecard)

@app.route("/expiration-alerts", methods=["GET"])
def get_expiration_alerts():
    alerts = expiration_alerts(df)
    return jsonify(alerts)

@app.route("/predict-stockouts", methods=["GET"])
def predict_stockouts_api():
    y_pred, X_test = predict_stockouts()
    X_test["Predicted_Stockout"] = y_pred
    return jsonify(X_test[["Stock_Quantity", "Sales_Volume", "Predicted_Stockout"]].head(10).to_dict())


# Main entry point: Either run as API or as standalone script
if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1 and sys.argv[1] == "server":
        print("Starting Flask API on port 5000...")
        app.run(debug=True, port=5000)
    else:
        print("Running standalone analysis...")
        # Standalone script logic
        reorder_list = automate_reordering(df)
        print("\nReorder List:")
        print(reorder_list)

        scorecard = supplier_scorecard(df)
        print("\nSupplier Scorecard:")
        print(scorecard)

        alerts = expiration_alerts(df)
        print("\nExpiration Alerts:")
        print(alerts)

        print("\nTraining AI Model for Stockout Prediction...")
        y_pred, X_test = predict_stockouts()

        print("\nSample Predictions:")
        X_test["Predicted_Stockout"] = y_pred
        print(X_test[["Stock_Quantity", "Sales_Volume", "Predicted_Stockout"]].head(10))
