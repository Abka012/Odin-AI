#Import Libraries
import numpy as np
import os
import pandas as pd
import tensorflow
from tensorflow.keras import layers, models 
from tensorflow.keras.models import load_model 
import random 
import json 
import pickle
import nltk 
from nltk.stem.lancaster import LancasterStemmer

stemmer = LancasterStemmer() #Creates an instance of Lancaster Stemmer 
 
# Load dataset from CSV
def read_data_set():
    filepath = os.path.join(os.getcwd(), 'grocery_dataset.csv')
    df = pd.read_csv(filepath)
    return df

# Read and process training data from CSV
def process_data_from_csv(file_name):
    df = read_data_set(file_name)
    words = []
    labels = []
    docs_x = []
    docs_y = []
    
    for _, row in df.iterrows():
        wrds = nltk.word_tokenize(row['pattern'])
        words.extend(wrds)
        docs_x.append(wrds)
        docs_y.append(row['tag'])

        if row['tag'] not in labels:
            labels.append(row['tag'])
    
    words = [stemmer.stem(w.lower()) for w in words if w != "?"]
    words = sorted(list(set(words)))
    labels = sorted(labels)
    
    training = []
    output = []
    out_empty = [0 for _ in range(len(labels))]
    
    for x, doc in enumerate(docs_x):
        bag = []
        wrds = [stemmer.stem(w) for w in doc]
        
        for w in words:
            if w in wrds:
                bag.append(1)
            else:
                bag.append(0)
        
        output_row = out_empty[:]
        output_row[labels.index(docs_y[x])] = 1
        
        training.append(bag)
        output.append(output_row)
    
    training = np.array(training)
    output = np.array(output)
    
    with open("data.pickle", "wb") as f:
        pickle.dump((words, labels, training, output), f)
    
    return words, labels, training, output

# Load and train model using CSV data
try:
    with open("data.pickle", "rb") as f:
        words, labels, training, output = pickle.load(f)
except Exception as e:
    print(f"Error loading data: {e}")
    words, labels, training, output = process_data_from_csv("training_data.csv")


tensorflow.keras.backend.clear_session()

#Creates neural network model 
model = models.Sequential()
model.add(layers.Input(shape=(46,)))
model.add(layers.Dense(32, activation='relu'))
model.add(layers.Dense(16, activation='relu'))
model.add(layers.Dense(6, activation='softmax'))

#Save and load model
try:
    model = load_model("model.keras")

except Exception as e:
    print(f"Error loading model: {e}")
    
    model.compile(optimizer='adam',
                loss='categorical_crossentropy',
                metrics=['accuracy'])

    model.fit(training, output, epochs=1000, batch_size=8)
    model.save("model.keras")
    #model.summary()

## Get Stock Quantity from Product Name
def get_stock_quantity(df, product_name):
    result = df[df['Product_Name'].str.lower() == product_name.lower()]
    if not result.empty:
        return result.iloc[0]['Stock_Quantity']
    else:
        return "Product not found"

# Chat Function
def chat():
    df = read_data_set()
    print("Welcome, enter a product name to get stock quantity! (type quit to exit) \n")
    while True:
        inp = input("You: ")
        if inp.lower() == "quit":
            break
        stock_quantity = get_stock_quantity(df, inp)
        print(f"Stock Quantity: {stock_quantity}\n")

chat()
