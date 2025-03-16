// Show/hide sections
function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });
    document.getElementById(sectionId).classList.add('active');
}

// Load inventory on page load
document.addEventListener('DOMContentLoaded', () => {
    fetchInventory();

    // Add event listeners for sorting buttons
    document.getElementById('sortByStock').addEventListener('click', () => sortTable('stockLevel'));
    document.getElementById('sortByName').addEventListener('click', () => sortTable('productName'));
    document.getElementById('sortByExpiration').addEventListener('click', () => sortTable('daysUntilExpiration'));
});

// Fetch inventory from backend
function fetchInventory() {
    fetch('http://localhost:8080/api/inventory')
        .then(response => response.json())
        .then(items => {
            const tbody = document.getElementById('inventory-table-body');
            tbody.innerHTML = '';
            items.forEach(item => {
                const row = document.createElement('tr');
                row.classList.add('product-row');
                row.innerHTML = `
                    <td>${item.id}</td>
                    <td>${item.productName}</td>
                    <td>${item.stockLevel}</td>
                    <td>$${item.price.toFixed(2)}</td>
                    <td>${item.dateAdded.split('T')[0]}</td>
                    <td>${item.lifeExpectancy.split('T')[0]}</td>
                    <td>${item.supplierName}</td>
                    <td class="actions">
                        <button class="edit-btn" onclick="editProduct('${item.id}')">Edit</button>
                        <button class="delete-btn" onclick="deleteProduct('${item.id}')">Delete</button>
                    </td>
                `;
                tbody.appendChild(row);
            });
        })
        .catch(error => console.error('Error fetching inventory:', error));
}

// Show Add Product Form
function showAddProductForm() {
    document.getElementById('add-product-form').style.display = 'block';
}

// Hide Add Product Form
function hideAddProductForm() {
    const formDiv = document.getElementById('add-product-form');
    formDiv.style.display = 'none';
    const form = formDiv.querySelector('form');
    if (form) {
        form.reset();
        form.onsubmit = addProduct;
        document.querySelector('#add-product-form button[type="submit"]').textContent = 'Save';
    }
}

// Add Product
function addProduct(event) {
    event.preventDefault();
    const item = {
        productName: document.getElementById('productName').value,
        productType: document.getElementById('productType').value,
        stockLevel: parseFloat(document.getElementById('stockLevel').value),
        reorderThreshold: parseInt(document.getElementById('reorderThreshold').value),
        price: parseFloat(document.getElementById('price').value),
        dateAdded: document.getElementById('dateAdded').value + 'T00:00:00',
        lifeExpectancy: document.getElementById('lifeExpectancy').value + 'T00:00:00',
        supplierName: document.getElementById('supplierName').value,
        category: document.getElementById('category').value,
        isActive: true
    };

    console.log('Sending item:', item);

    fetch('http://localhost:8080/api/inventory', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(item)
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => { throw new Error(`Failed to add product: ${text}`); });
        }
        return response.json();
    })
    .then(data => {
        console.log('Response data:', data);
        alert(data.message);
        hideAddProductForm();
        fetchInventory();
    })
    .catch(error => {
        console.error('Error adding product:', error);
        alert('Failed to add product: ' + error.message);
    });
}

// Edit Product
function editProduct(id) {
    console.log('Fetching item with ID:', id);
    fetch(`http://localhost:8080/api/inventory/${id}`)
        .then(response => {
            console.log('Response status:', response.status);
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(`Failed to fetch product: ${response.status} - ${text}`);
                });
            }
            return response.json();
        })
        .then(item => {
            console.log('Fetched item:', item);
            document.getElementById('productName').value = item.productName;
            document.getElementById('productType').value = item.productType;
            document.getElementById('stockLevel').value = item.stockLevel;
            document.getElementById('reorderThreshold').value = item.reorderThreshold;
            document.getElementById('price').value = item.price;
            document.getElementById('dateAdded').value = item.dateAdded.split('T')[0];
            document.getElementById('lifeExpectancy').value = item.lifeExpectancy.split('T')[0];
            document.getElementById('supplierName').value = item.supplierName;
            document.getElementById('category').value = item.category;

            const form = document.querySelector('#add-product-form form');
            form.onsubmit = (event) => updateProduct(event, id);
            document.querySelector('#add-product-form button[type="submit"]').textContent = 'Update';
            showAddProductForm();
        })
        .catch(error => {
            console.error('Error fetching product:', error);
            alert('Failed to load product for editing: ' + error.message);
        });
}

// Update Product
function updateProduct(event, id) {
    event.preventDefault();
    const item = {
        productName: document.getElementById('productName').value,
        productType: document.getElementById('productType').value,
        stockLevel: parseFloat(document.getElementById('stockLevel').value),
        reorderThreshold: parseInt(document.getElementById('reorderThreshold').value),
        price: parseFloat(document.getElementById('price').value),
        dateAdded: document.getElementById('dateAdded').value + 'T00:00:00',
        lifeExpectancy: document.getElementById('lifeExpectancy').value + 'T00:00:00',
        supplierName: document.getElementById('supplierName').value,
        category: document.getElementById('category').value,
        isActive: true
    };

    console.log('Updating item:', item);

    fetch(`http://localhost:8080/api/inventory/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(item)
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => { throw new Error(`Failed to update product: ${text}`); });
        }
        return response.json();
    })
    .then(data => {
        console.log('Updated item:', data);
        alert('Product updated successfully');
        hideAddProductForm();
        fetchInventory();
    })
    .catch(error => {
        console.error('Error updating product:', error);
        alert('Failed to update product: ' + error.message);
    });
}

// Delete Product
function deleteProduct(id) {
    if (confirm('Are you sure you want to delete this product?')) {
        fetch(`http://localhost:8080/api/inventory/${id}`, {
            method: 'DELETE'
        })
        .then(response => {
            console.log('Delete response status:', response.status);
            if (!response.ok) {
                return response.text().then(text => { throw new Error(`Failed to delete product: ${text}`); });
            }
            fetchInventory();
            alert('Product deleted successfully');
        })
        .catch(error => {
            console.error('Error deleting product:', error);
            alert('Failed to delete product: ' + error.message);
        });
    }
}

// Sort Table by specified field
function sortTable(sortBy = 'stockLevel') {
    fetch('http://localhost:8080/api/inventory')
        .then(response => response.json())
        .then(items => {
            // Add daysUntilExpiration to each item
            const today = new Date();
            items.forEach(item => {
                const expirationDate = new Date(item.lifeExpectancy);
                const timeDiff = expirationDate - today;
                item.daysUntilExpiration = Math.ceil(timeDiff / (1000 * 60 * 60 * 24)); // Convert ms to days
            });

            // Sort based on the specified field
            items.sort((a, b) => {
                if (sortBy === 'productName') {
                    return a.productName.localeCompare(b.productName); // Alphabetical sort
                } else if (sortBy === 'daysUntilExpiration') {
                    return a.daysUntilExpiration - b.daysUntilExpiration; // Numeric sort (ascending)
                } else {
                    return a.stockLevel - b.stockLevel; // Default: stockLevel (ascending)
                }
            });

            // Update the table
            const tbody = document.getElementById('inventory-table-body');
            tbody.innerHTML = '';
            items.forEach(item => {
                const row = document.createElement('tr');
                row.classList.add('product-row');
                row.innerHTML = `
                    <td>${item.id}</td>
                    <td>${item.productName}</td>
                    <td>${item.stockLevel}</td>
                    <td>$${item.price.toFixed(2)}</td>
                    <td>${item.dateAdded.split('T')[0]}</td>
                    <td>${item.lifeExpectancy.split('T')[0]}</td>
                    <td>${item.supplierName}</td>
                    <td class="actions">
                        <button class="edit-btn" onclick="editProduct('${item.id}')">Edit</button>
                        <button class="delete-btn" onclick="deleteProduct('${item.id}')">Delete</button>
                    </td>
                `;
                tbody.appendChild(row);
            });
        })
        .catch(error => console.error('Error sorting inventory:', error));
}

// Logout (Placeholder)
function logout() {
    alert('Logout functionality to be implemented');
}