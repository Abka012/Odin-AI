document.addEventListener("DOMContentLoaded", function () {
    // Get all delete buttons
    const deleteButtons = document.querySelectorAll(".delete-btn");

    deleteButtons.forEach((button) => {
        button.addEventListener("click", function () {
            const row = this.closest("tr");
            row.remove();
            alert("Product deleted!");
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
