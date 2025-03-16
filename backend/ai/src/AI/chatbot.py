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
import google.generativeai as genai
from dotenv import load_dotenv

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

load_dotenv()

genai.configure(api_key=os.getenv('GEMINI_API_KEY'))

## Get Stock Quantity from Product Name
def get_stock_info(df, product_name):
    result = df[df['Product_Name'].str.lower() == product_name.lower()]
    if not result.empty:
        product_name = result.iloc[0]['Product_Name']
        stock_quantity = result.iloc[0]['Stock_Quantity']
        sales_volume = result.iloc[0]['Sales_Volume']
        return product_name, stock_quantity, sales_volume
    else:
        return "Product not found", "N/A"
    
def get_recommendations(df, product_name):
    product, stock, sales = get_stock_info(df, product_name)

    generation_config = {
    "temperature":0.7,                  
    "top_p":0.9,                        
    "top_k":40,                          
    "max_output_tokens":5000,            
    "response_mime_type":"text/plain"   
    }

    system_instruction = f"""
    You are a restocking assistant AI. Your task is to:
    1. Analyze the current stock levels and sales volume of a food item.
    2. Recommend if the restock order quantity should increase or decrease based on the stock-to-sales ratio.
    3. Provide recommendations on optimal order quantities to prevent shortages or overstocking.
    4. Consider any special factors such as upcoming promotions or seasonal demand.

    When responding:
    - Be concise yet informative.
    - Provide clear suggestions based on data analysis.
    - Use simple language for general users but include necessary details for more advanced inquiries.
    - Format responses with bullet points or tables where applicable for clarity.
    """

    model = genai.GenerativeModel(
    model_name="gemini-2.0-flash",
    generation_config=generation_config, 
    system_instruction=system_instruction
    )

    chat = model.start_chat(history=[])

    if not product:
        pass
    query = f"""
        Based on the {stock} and {sales} for {product}, it is recommended that:

        Please provide:
        - An assessment of the current stock and sales volume.
        - Recommendations on whether the restock order quantity should increase or decrease.
        - A structured table with recommended restock quantities based on sales trends and stock levels.
        """
    response = chat.send_message(query)

    chat.history.append({"role": "user", "parts": [product_name]})
    chat.history.append({"role": "model", "parts": [response]})

    print(response.text)

# Chat Function
def chat():
    df = read_data_set()
    print("Welcome, enter a product name to get stock quantity! (type quit to exit) \n")
    while True:
        inp = input("You: ")
        if inp.lower() == "quit":
            break
        get_recommendations(df, inp)

chat()
