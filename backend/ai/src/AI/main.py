from forecast_service import (
    read_data_set,
    automate_reordering,
    supplier_scorecard,
    expiration_alerts,
)
from prediction import predict_stockouts


def main():
    # Load dataset
    df = read_data_set("grocery_dataset.csv")

    # Automate Reordering
    reorder_list = automate_reordering(df)
    print("Reorder List:")
    print(reorder_list)

    # Supplier Scorecard
    scorecard = supplier_scorecard(df)
    print("Supplier Scorecard:")
    print(scorecard)

    # Expiration Alerts
    alerts = expiration_alerts(df)
    print("Expiration Alerts:")
    print(alerts)

    # Predict Stockouts using AI
    print("\nTraining AI Model for Stockout Prediction...")
    y_pred, X_test = predict_stockouts()

    # Display predictions
    print("\nSample Predictions:")
    X_test["Predicted_Stockout"] = y_pred
    print(X_test[["Stock_Quantity", "Sales_Volume", "Predicted_Stockout"]].head(10))


if __name__ == "__main__":
    main()
