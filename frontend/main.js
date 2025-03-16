// Show/hide sections
function showSection(sectionId) {
    console.log(`Showing section: ${sectionId}`);
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });
    document.getElementById(sectionId).classList.add('active');
    if (sectionId === 'demand') {
        fetchDemandReport();
    }
}

// Load inventory on page load
document.addEventListener('DOMContentLoaded', () => {
    console.log('Page loaded, fetching inventory...');
    fetchInventory();

    document.getElementById('sortByStock').addEventListener('click', () => sortTable('stockLevel'));
    document.getElementById('sortByName').addEventListener('click', () => sortTable('productName'));
    document.getElementById('sortByExpiration').addEventListener('click', () => sortTable('daysUntilExpiration'));
});

// Fetch inventory from backend
function fetchInventory() {
    fetch('http://localhost:8080/api/inventory')
        .then(response => response.json())
        .then(items => {
            console.log('Inventory fetched:', items);
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

// Fetch and display demand report
function fetchDemandReport() {
    console.log('Fetching demand report...');
    const endpoints = [
        { url: 'reorder', bodyId: 'reorderListBody', cols: 3 },
        { url: 'supplier-scorecard', bodyId: 'supplierScorecardBody', cols: 2 },
        { url: 'expiration-alerts', bodyId: 'expirationAlertsBody', cols: 3 },
        { url: 'predict-stockouts', bodyId: 'stockoutPredictionsBody', cols: 2 }
    ];

    endpoints.forEach(endpoint => {
        fetch(`http://localhost:5000/${endpoint.url}`)
            .then(response => response.json())
            .then(data => {
                console.log(`Data for ${endpoint.url}:`, data);
                renderDemandTable(endpoint.url, endpoint.bodyId, endpoint.cols, data);
            })
            .catch(error => {
                console.error(`Error fetching ${endpoint.url}:`, error);
                renderDemandTable(endpoint.url, endpoint.bodyId, endpoint.cols, [{ error: `Failed to load ${endpoint.url.replace('-', ' ')}` }]);
            });
    });
}

function fetchDemandReport() {
    console.log('Fetching demand report...');
    const endpoints = [
        { url: 'reorder', bodyId: 'reorderListBody', cols: 3 },
        { url: 'supplier-scorecard', bodyId: 'supplierScorecardBody', cols: 2 },
        { url: 'expiration-alerts', bodyId: 'expirationAlertsBody', cols: 3 },
        { url: 'predict-stockouts', bodyId: 'stockoutPredictionsBody', cols: 2 }
    ];

    endpoints.forEach(endpoint => {
        fetch(`http://localhost:5000/${endpoint.url}`)
            .then(response => {
                console.log(`Response status for ${endpoint.url}: ${response.status}`);
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                return response.json();
            })
            .then(data => {
                console.log(`Data for ${endpoint.url}:`, data);
                renderDemandTable(endpoint.url, endpoint.bodyId, endpoint.cols, data);
            })
            .catch(error => {
                console.error(`Fetch error for ${endpoint.url}:`, error);
                renderDemandTable(endpoint.url, endpoint.bodyId, endpoint.cols, [{ error: `Failed to load ${endpoint.url.replace('-', ' ')}: ${error.message}` }]);
            });
    });
}

function renderDemandTable(endpoint, bodyId, colspan, data) {
    console.log(`Rendering ${endpoint} into ${bodyId} with data:`, data);
    const tbody = document.getElementById(bodyId);
    if (!tbody) {
        console.error(`Table body ${bodyId} not found in DOM`);
        return;
    }
    tbody.innerHTML = ''; // Clear previous content

    if (!data || data.length === 0) {
        console.log(`No data for ${endpoint}, showing 'No data' message`);
        const tr = document.createElement('tr');
        tr.innerHTML = `<td colspan="${colspan}" class="no-data">No ${endpoint.replace('-', ' ')} available</td>`;
        tbody.appendChild(tr);
        return;
    }

    if (data[0]?.error) {
        console.log(`Error detected in data for ${endpoint}: ${data[0].error}`);
        const tr = document.createElement('tr');
        tr.innerHTML = `<td colspan="${colspan}" class="error">${data[0].error}</td>`;
        tbody.appendChild(tr);
        return;
    }

    switch (endpoint) {
        case 'reorder':
            data.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${item.productName || 'N/A'}</td>
                    <td>${item.stockLevel || 'N/A'}</td>
                    <td>${item.reorderThreshold || 'N/A'}</td>
                `;
                tbody.appendChild(tr);
            });
            break;
        case 'supplier-scorecard':
            data.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${item.supplierName || 'N/A'}</td>
                    <td>$${item.price ? item.price.toFixed(2) : 'N/A'}</td>
                `;
                tbody.appendChild(tr);
            });
            break;
        case 'expiration-alerts':
            data.forEach(item => {
                const expiryDate = new Date(item.lifeExpectancy);
                const now = new Date();
                const daysLeft = Math.ceil((expiryDate - now) / (1000 * 60 * 60 * 24));
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${item.productName || 'N/A'}</td>
                    <td>${expiryDate.toLocaleDateString() || 'N/A'}</td>
                    <td class="${daysLeft < 30 ? 'urgent' : ''}">${daysLeft || 'N/A'} days</td>
                `;
                tbody.appendChild(tr);
            });
            break;
        case 'predict-stockouts':
            data.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${item.stockLevel || 'N/A'}</td>
                    <td class="${item.predictedStockout ? 'urgent' : ''}">${item.predictedStockout ? 'High Risk' : 'Low Risk'}</td>
                `;
                tbody.appendChild(tr);
            });
            break;
    }
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
            const today = new Date();
            items.forEach(item => {
                const expirationDate = new Date(item.lifeExpectancy);
                const timeDiff = expirationDate - today;
                item.daysUntilExpiration = Math.ceil(timeDiff / (1000 * 60 * 60 * 24));
            });

            items.sort((a, b) => {
                if (sortBy === 'productName') {
                    return a.productName.localeCompare(b.productName);
                } else if (sortBy === 'daysUntilExpiration') {
                    return a.daysUntilExpiration - b.daysUntilExpiration;
                } else {
                    return a.stockLevel - b.stockLevel;
                }
            });

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
    window.location.href = "login.html";
}