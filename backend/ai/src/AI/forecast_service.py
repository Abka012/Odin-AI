import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from datetime import datetime, timedelta
from sklearn.linear_model import LinearRegression


# Load dataset
def read_data_set(file_name):
    df = pd.read_csv(file_name)
    return df


# Automate Reordering
def automate_reordering(df):
    # Identify products that need reordering
    reorder_list = df[df["Stock_Quantity"] < df["Reorder_Level"]]
    reorder_list = reorder_list[
        [
            "Product_ID",
            "Product_Name",
            "Stock_Quantity",
            "Reorder_Level",
            "Reorder_Quantity",
        ]
    ]

    # Add a column for the required reorder quantity
    reorder_list["Required_Reorder_Quantity"] = (
        reorder_list["Reorder_Quantity"] - reorder_list["Stock_Quantity"]
    )

    return reorder_list


# Supplier Scorecard
def supplier_scorecard(df):
    # Calculate supplier performance metrics
    supplier_performance = (
        df.groupby("Supplier_Name")
        .agg(
            {
                "Sales_Volume": "sum",  # Total sales volume
                "Stock_Quantity": "mean",  # Average stock quantity
                "Last_Order_Date": lambda x: (
                    datetime.today() - pd.to_datetime(x).max()
                ).days,  # Lead time
            }
        )
        .reset_index()
    )

    # Rename columns
    supplier_performance.columns = [
        "Supplier_Name",
        "Total_Sales_Volume",
        "Average_Stock_Quantity",
        "Lead_Time_Days",
    ]

    return supplier_performance


# Expiration Alerts
def expiration_alerts(df, days_threshold=30):
    # Identify products nearing expiration
    today = datetime.today()
    expiring_soon = df[
        pd.to_datetime(df["Expiration_Date"]) < today + timedelta(days=days_threshold)
    ]
    expiring_soon = expiring_soon[
        ["Product_ID", "Product_Name", "Expiration_Date", "Stock_Quantity"]
    ]

    return expiring_soon


# Main Function
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


# Run the program
if __name__ == "__main__":
    main()
