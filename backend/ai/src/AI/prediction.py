import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, classification_report
from sklearn.preprocessing import StandardScaler
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader, TensorDataset
import matplotlib.pyplot as plt
from forecast_service import read_data_set


# Prepare dataset for AI
def prepare_dataset(df):
    # Calculate days until stockout
    df["Days_Until_Stockout"] = df["Stock_Quantity"] / (
        df["Sales_Volume"] / 30
    )  # Assuming monthly sales volume

    # Create target variable: 1 if stockout occurs within 7 days, else 0
    df["Stockout"] = (df["Days_Until_Stockout"] <= 7).astype(int)

    # Add rolling statistics
    df["Rolling_Avg_Sales"] = df["Sales_Volume"].rolling(window=7, min_periods=1).mean()

    return df


# Feature engineering
def feature_engineering(df):
    # Calculate lead time
    df["Lead_Time_Days"] = (
        pd.to_datetime("today") - pd.to_datetime(df["Last_Order_Date"])
    ).dt.days

    # Select features and target
    features = [
        "Stock_Quantity",
        "Sales_Volume",
        "Days_Until_Stockout",
        "Reorder_Level",
        "Lead_Time_Days",
        "Rolling_Avg_Sales",
    ]
    target = "Stockout"

    X = df[features]
    y = df[target]

    return X, y


# Define the Neural Network model in PyTorch
class NeuralNetwork(nn.Module):
    def __init__(self, input_size):
        super(NeuralNetwork, self).__init__()
        self.fc1 = nn.Linear(input_size, 64)
        self.fc2 = nn.Linear(64, 32)
        self.fc3 = nn.Linear(32, 1)
        self.dropout = nn.Dropout(0.2)
        self.sigmoid = nn.Sigmoid()

    def forward(self, x):
        x = torch.relu(self.fc1(x))
        x = self.dropout(x)
        x = torch.relu(self.fc2(x))
        x = self.dropout(x)
        x = self.fc3(x)
        return self.sigmoid(x)


def train_model(X, y):
    # Split data into training and testing sets
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )

    # Standardize the features (important for Neural Networks)
    scaler = StandardScaler()
    X_train = scaler.fit_transform(X_train)
    X_test = scaler.transform(X_test)

    # Convert data to PyTorch tensors
    X_train = torch.tensor(X_train, dtype=torch.float32)
    X_test = torch.tensor(X_test, dtype=torch.float32)
    y_train = torch.tensor(y_train.values, dtype=torch.float32).reshape(-1, 1)
    y_test = torch.tensor(y_test.values, dtype=torch.float32).reshape(-1, 1)

    # Create DataLoader for training
    train_dataset = TensorDataset(X_train, y_train)
    train_loader = DataLoader(train_dataset, batch_size=32, shuffle=True)

    # Initialize the model, loss function, and optimizer
    input_size = X_train.shape[1]
    model = NeuralNetwork(input_size)
    criterion = nn.BCELoss()  # Binary Cross-Entropy Loss
    optimizer = optim.Adam(model.parameters(), lr=0.001)

    # Training loop
    epochs = 50
    train_losses = []
    val_losses = []
    for epoch in range(epochs):
        model.train()
        running_loss = 0.0
        for inputs, labels in train_loader:
            optimizer.zero_grad()
            outputs = model(inputs)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()
            running_loss += loss.item()
        train_losses.append(running_loss / len(train_loader))

        # Validation
        model.eval()
        with torch.no_grad():
            val_outputs = model(X_test)
            val_loss = criterion(val_outputs, y_test)
            val_losses.append(val_loss.item())

        print(
            f"Epoch {epoch + 1}/{epochs}, Loss: {running_loss / len(train_loader)}, Val Loss: {val_loss.item()}"
        )

    # Plot training and validation loss
    plt.plot(train_losses, label="Training Loss")
    plt.plot(val_losses, label="Validation Loss")
    plt.xlabel("Epochs")
    plt.ylabel("Loss")
    plt.title("Training and Validation Loss")
    plt.legend()
    plt.show()

    # Evaluate the model
    model.eval()
    with torch.no_grad():
        y_pred_prob = model(X_test).numpy()
        y_pred = (y_pred_prob > 0.5).astype(
            int
        )  # Convert probabilities to binary predictions
        print("Accuracy:", accuracy_score(y_test, y_pred))
        print("Classification Report:")
        print(classification_report(y_test, y_pred))

    # Convert X_test back to a Pandas DataFrame
    X_test_df = pd.DataFrame(
        scaler.inverse_transform(X_test.numpy()), columns=X.columns
    )

    return y_pred, X_test_df


# Main function for prediction
def predict_stockouts():
    # Load dataset
    df = read_data_set("grocery_dataset.csv")

    # Prepare dataset for AI
    df = prepare_dataset(df)

    # Feature engineering
    X, y = feature_engineering(df)

    # Train and evaluate the PyTorch model
    print("\nTraining Neural Network Model with PyTorch...")
    y_pred, X_test = train_model(X, y)

    return y_pred, X_test
