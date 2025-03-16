function toggleDropdown(event) {
    event.preventDefault();
    const dropdown = event.target.closest('.dropdown');
    const menu = dropdown.querySelector('.dropdown-menu');
    menu.classList.toggle('active');
}

function toggleSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const mainContent = document.querySelector('.main-content');
    sidebar.classList.toggle('active');
    mainContent.classList.toggle('expanded');
}

function showSection(sectionId) {
    console.log(`Showing section: ${sectionId}`);
    
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });
    const targetSection = document.getElementById(sectionId);
    if (targetSection) {
        targetSection.classList.add('active');
    } else {
        console.error(`Section with ID ${sectionId} not found`);
        return;
    }

    document.querySelectorAll('.nav-links a').forEach(link => {
        link.classList.remove('active');
    });
    const activeLink = document.querySelector(`.nav-links a[onclick*="'${sectionId}'"]`);
    if (activeLink) activeLink.classList.add('active');

    const sectionTitle = {
        'home': 'Inventory Management',
        'reorder': 'Reorder List',
        'supplier': 'Supplier Scorecard',
        'expiration': 'Expiration Alerts',
        'stockout': 'Stockout Predictions',
        'recommendations': 'Restocking Recommendations',
        'profile': 'Profile'
    };
    document.getElementById('section-title').textContent = sectionTitle[sectionId] || 'StockXpert';

    switch (sectionId) {
        case 'home': fetchInventory(); break;
        case 'reorder': fetchReorderList(); break;
        case 'supplier': fetchSupplierScorecard(); break;
        case 'expiration': fetchExpirationAlerts(); break;
        case 'stockout': fetchStockoutPredictions(); break;
        case 'recommendations': document.getElementById('recommendationsBody').innerHTML = ''; break;
        case 'profile': break;
    }
}

document.addEventListener("DOMContentLoaded", function () {
    document.getElementById("sortByStock").addEventListener("click", function () {
        sortInventory("stock");
    });
    document.getElementById("sortByName").addEventListener("click", function () {
        sortInventory("name");
    });
    document.getElementById("sortByExpiration").addEventListener("click", function () {
        sortInventory("expiry");
    });
});

function sortInventory(criteria) {
    let tableBody = document.getElementById("inventory-table-body");
    let rows = Array.from(tableBody.getElementsByTagName("tr"));

    rows.sort((rowA, rowB) => {
        let cellA, cellB;
        switch (criteria) {
            case "stock":
                cellA = parseFloat(rowA.cells[2].innerText) || 0;
                cellB = parseFloat(rowB.cells[2].innerText) || 0;
                return cellA - cellB;
            case "name":
                cellA = rowA.cells[1].innerText.toLowerCase();
                cellB = rowB.cells[1].innerText.toLowerCase();
                return cellA.localeCompare(cellB);
            case "expiry":
                cellA = new Date(rowA.cells[5].innerText);
                cellB = new Date(rowB.cells[5].innerText);
                return cellA - cellB;
            default:
                return 0;
        }
    });

    tableBody.innerHTML = "";
    rows.forEach(row => tableBody.appendChild(row));
}

function fetchInventory() {
    fetch('http://localhost:8080/api/inventory', { mode: 'cors' })
        .then(response => response.json())
        .then(data => {
            const tbody = document.getElementById('inventory-table-body');
            tbody.innerHTML = '';
            data.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${item.id}</td>
                    <td>${item.productName}</td>
                    <td>${item.stockLevel}</td>
                    <td>$${item.price.toFixed(2)}</td>
                    <td>${item.dateAdded.split('T')[0]}</td>
                    <td>${item.lifeExpectancy.split('T')[0]}</td>
                    <td>${item.supplierName}</td>
                    <td>
                        <button onclick="editProduct('${item.id}')">Edit</button>
                        <button onclick="deleteProduct('${item.id}')">Delete</button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        })
        .catch(error => console.error('Error fetching inventory:', error));
}

function fetchReorderList() {
    fetch('http://localhost:5000/reorder', { mode: 'cors' })
        .then(response => response.json())
        .then(data => {
            const tbody = document.getElementById('reorderListBody');
            tbody.innerHTML = '';
            data.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${item.productName}</td>
                    <td>${item.stockLevel}</td>
                    <td>${item.reorderThreshold}</td>
                `;
                tbody.appendChild(tr);
            });
        })
        .catch(error => console.error('Error fetching reorder list:', error));
}

function fetchSupplierScorecard() {
    fetch('http://localhost:5000/supplier-scorecard', { mode: 'cors' })
        .then(response => response.json())
        .then(data => {
            const tbody = document.getElementById('supplierScorecardBody');
            tbody.innerHTML = '';
            data.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${item.supplierName}</td>
                    <td>$${item.price.toFixed(2)}</td>
                `;
                tbody.appendChild(tr);
            });
        })
        .catch(error => console.error('Error fetching supplier scorecard:', error));
}

function fetchExpirationAlerts() {
    fetch('http://localhost:5000/expiration-alerts', { mode: 'cors' })
        .then(response => response.json())
        .then(data => {
            const tbody = document.getElementById('expirationAlertsBody');
            tbody.innerHTML = '';
            const now = new Date();
            data.forEach(item => {
                const expiryDate = new Date(item.lifeExpectancy);
                const daysLeft = Math.ceil((expiryDate - now) / (1000 * 60 * 60 * 24));
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${item.productName}</td>
                    <td>${expiryDate.toLocaleDateString()}</td>
                    <td>${daysLeft}</td>
                `;
                tbody.appendChild(tr);
            });
        })
        .catch(error => console.error('Error fetching expiration alerts:', error));
}

function fetchStockoutPredictions() {
    fetch('http://localhost:5000/predict-stockouts', { mode: 'cors' })
        .then(response => response.json())
        .then(data => {
            const tbody = document.getElementById('stockoutPredictionsBody');
            tbody.innerHTML = '';
            data.forEach(item => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${item.stockLevel}</td>
                    <td>${item.predictedStockout ? 'High' : 'Low'}</td>
                `;
                tbody.appendChild(tr);
            });
        })
        .catch(error => console.error('Error fetching stockout predictions:', error));
}

function fetchRecommendation(event) {
    event.preventDefault();
    const productName = document.getElementById('recommendationInput').value.trim();
    if (!productName) return;

    console.log(`Fetching recommendation for ${productName}...`);
    fetch(`http://localhost:5000/recommendation?product=${encodeURIComponent(productName)}`, {
        method: 'GET',
        mode: 'cors'
    })
    .then(response => {
        console.log(`Response status: ${response.status}`);
        if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
        return response.json();
    })
    .then(data => {
        console.log('Recommendation data:', data);
        const tbody = document.getElementById('recommendationsBody');
        tbody.innerHTML = '';
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td>${productName}</td>
            <td>${data.recommendation || 'No recommendation available'}</td>
        `;
        tbody.appendChild(tr);
        document.getElementById('recommendationInput').value = '';
    })
    .catch(error => {
        console.error('Error fetching recommendation:', error);
        const tbody = document.getElementById('recommendationsBody');
        tbody.innerHTML = `<tr><td colspan="2" class="error">Error: ${error.message}</td></tr>`;
    });
}

function showAddProductForm() {
    document.getElementById('add-product-form').style.display = 'block';
}

function hideAddProductForm() {
    document.getElementById('add-product-form').style.display = 'none';
}

function addProduct(event) {
    event.preventDefault();
    console.log('addProduct called');

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

    console.log('Item to save:', item);

    fetch('http://localhost:8080/api/inventory', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(item),
        mode: 'cors'
    })
    .then(response => {
        console.log('Response status:', response.status);
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(`Failed to add product: ${response.status} - ${text}`);
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('Product added successfully:', data);
        fetchInventory();
        hideAddProductForm();
        document.querySelector('#add-product-form form').reset();
        document.querySelector('#add-product-form form').onsubmit = addProduct;
    })
    .catch(error => {
        console.error('Error adding product:', error);
    });
}

function logout() {
    console.log('Logged out');
    // Add logout logic if needed
}

function updateProduct(event) {
    event.preventDefault();
    console.log('updateProduct called'); // Debug

    const item = {
        id: document.getElementById('productId').value, // Keep as String
        productName: document.getElementById('productName').value,
        productType: document.getElementById('productType').value,
        stockLevel: parseFloat(document.getElementById('stockLevel').value),
        reorderThreshold: parseInt(document.getElementById('reorderThreshold').value),
        price: parseFloat(document.getElementById('price').value),
        dateAdded: document.getElementById('dateAdded').value + 'T00:00:00',
        lifeExpectancy: document.getElementById('lifeExpectancy').value + 'T00:00:00',
        supplierName: document.getElementById('supplierName').value,
        category: document.getElementById('category').value,
        isActive: true // Ensure consistency
    };

    if (!item.id) {
        console.error('Product ID is required for update');
        return;
    }

    console.log('Item to update:', item); // Debug

    fetch(`http://localhost:8080/api/inventory/${item.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'applicationItems: application/json' },
        body: JSON.stringify(item),
        mode: 'cors'
    })
    .then(response => {
        console.log('Update response status:', response.status); // Debug
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(`Failed to update product: ${response.status} - ${text}`);
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('Product updated successfully:', data);
        fetchInventory();
        hideAddProductForm();
        document.querySelector('#add-product-form form').reset();
        document.querySelector('#add-product-form form').onsubmit = addProduct; // Reset to add mode
    })
    .catch(error => console.error('Error updating product:', error));
}

function deleteProduct(id) {
    if (!confirm('Are you sure you want to delete this product?')) return;

    console.log('Deleting item with ID:', id); // Debug

    fetch(`http://localhost:8080/api/inventory/${id}`, {
        method: 'DELETE',
        mode: 'cors'
    })
    .then(response => {
        console.log('Delete response status:', response.status); // Debug
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(`Failed to delete product: ${response.status} - ${text}`);
            });
        }
        console.log('Product deleted');
        fetchInventory();
    })
    .catch(error => console.error('Error deleting product:', error));
}

function editProduct(id) {
    fetch(`http://localhost:8080/api/inventory/${id}`, { mode: 'cors' })
        .then(response => {
            if (!response.ok) throw new Error(`Failed to fetch item: ${response.status}`);
            return response.json();
        })
        .then(item => {
            console.log('Fetched item for edit:', item); // Debug
            document.getElementById('productName').value = item.productName;
            document.getElementById('productType').value = item.productType;
            document.getElementById('stockLevel').value = item.stockLevel;
            document.getElementById('reorderThreshold').value = item.reorderThreshold;
            document.getElementById('price').value = item.price;
            // Convert LocalDateTime to YYYY-MM-DD
            document.getElementById('dateAdded').value = item.dateAdded.split('T')[0];
            document.getElementById('lifeExpectancy').value = item.lifeExpectancy.split('T')[0];
            document.getElementById('supplierName').value = item.supplierName;
            document.getElementById('category').value = item.category;
            let form = document.querySelector('#add-product-form form');
            let idInput = document.getElementById('productId');
            if (!idInput) {
                idInput = document.createElement('input');
                idInput.type = 'hidden';
                idInput.id = 'productId';
                form.appendChild(idInput);
            }
            idInput.value = id; // Keep as String
            form.onsubmit = updateProduct;
            showAddProductForm();
        })
        .catch(error => console.error('Error fetching product for edit:', error));
}

// Initial load
fetchInventory();