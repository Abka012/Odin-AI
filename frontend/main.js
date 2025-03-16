document.addEventListener("DOMContentLoaded", function () {
    // Get all delete buttons
    const deleteButtons = document.querySelectorAll(".delete-btn");

    deleteButtons.forEach((button) => {
        button.addEventListener("click", function () {
            const row = this.closest("tr");
            row.remove();
        });
    });

    // Get all edit buttons
    const editButtons = document.querySelectorAll(".edit-btn");

    editButtons.forEach((button) => {
        button.addEventListener("click", function () {
            alert("Edit functionality coming soon!");
        });
    });
});
function logout() {
     
        window.location.href = "login.html"; // Redirect to login page (change as needed)
    
}
function showSection(sectionId) {
    // Hide all sections
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });

    // Show the selected section
    document.getElementById(sectionId).classList.add('active');

    // Update URL without scrolling
    history.pushState(null, null, `#${sectionId}`);
}

// Ensure the correct section is shown when the page loads
document.addEventListener("DOMContentLoaded", () => {
    const sectionId = location.hash.substring(1) || 'home';
    showSection(sectionId);
});

