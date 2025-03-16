import os
import requests
import google.generativeai as genai

# Configure Gemini API with your key
genai.configure(api_key='AIzaSyCpklPbv061VuVYX08zKyGQ-WJjCRd90LM')

# Fetch inventory data from Spring Boot API
def fetch_inventory_data():
    url = 'http://localhost:8080/api/inventory'
    try:
        response = requests.get(url)
        response.raise_for_status()  # Raise exception for bad status codes
        return response.json()  # List of inventory items
    except requests.exceptions.RequestException as e:
        print(f"Error fetching inventory from API: {str(e)}")
        return []

# Get stock info from product name using API data
def get_stock_info(inventory_data, product_name):
    if not inventory_data:
        return "Product not found", "N/A", "N/A", "N/A"
    
    # Search for product in the API response
    for item in inventory_data:
        if item.get('productName', '').lower() == product_name.lower():
            return (item['productName'], 
                    item.get('stockLevel', 'N/A'), 
                    item.get('salesVolume', 'N/A'),  # Adjust if your model differs
                    item.get('lifeExpectancy', 'N/A'))
    return "Product not found", "N/A", "N/A", "N/A"

# Get recommendation for a product
def get_recommendations(product_name):
    inventory_data = fetch_inventory_data()
    product, stock, sales, life_expectancy = get_stock_info(inventory_data, product_name)

    # Convert stock and sales to numeric for comparison
    try:
        stock = float(stock) if stock != "N/A" else 0
        sales = float(sales) if sales != "N/A" else 0
    except ValueError:
        stock, sales = 0, 0

    generation_config = {
        "temperature": 0.7,
        "top_p": 0.9,
        "top_k": 40,
        "max_output_tokens": 5000,
        "response_mime_type": "text/plain"
    }

    system_instruction = """
    You are a restocking Inventory assistant AI. Your task is to:
    1. Analyze the current stock levels and sales volume of a food item.
    2. Recommend if the restock order quantity should increase or decrease based on the stock-to-sales ratio.
    3. Provide recommendations on optimal order quantities to prevent shortages or overstocking.
    4. Consider any special factors such as upcoming promotions or seasonal demand.

    When responding:
    - Be concise yet informative, Keep points 1 sentence short.
    - Provide clear suggestions to the business owner/manager based on data analysis.
    - Use simple language for general users but include necessary details for more advanced inquiries.
    - Format responses with bullet points where applicable for clarity.
    - Make respone plan text 2.0 line spacing.
    - Use only letters and numbers when responding, No asteriks, numerous dashes etc.
    """

    try:
        model = genai.GenerativeModel(
            model_name="gemini-1.5-flash",
            generation_config=generation_config,
            system_instruction=system_instruction
        )
        chat = model.start_chat(history=[])

        if product == "Product not found":
            response_text = f"No data available for {product_name}."
        else:
            query = f"""
            Based on the stock level of {stock}, sales volume of {sales}, and life expectancy of {life_expectancy} for {product}, provide:
            - An assessment of the current stock, sales volume, and remaining shelf life.
            - Recommendations on whether the restock order quantity should increase or decrease.
            - A structured table with recommended restock quantities based on sales trends, stock levels, and expiration dates.
            """
            response = chat.send_message(query)
            response_text = response.text

            # Log stock status to console instead of sending WhatsApp
            if stock <= sales and stock != 0:
                print(f"Low stock alert for {product}: Stock ({stock}) <= Sales ({sales})")
            else:
                print(f"Stock for {product} is sufficient (Stock: {stock}, Sales: {sales})")

        return response_text

    except Exception as e:
        error_msg = f"Error generating recommendation: {str(e)}"
        print(error_msg)
        return error_msg
