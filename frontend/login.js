
document.addEventListener("DOMContentLoaded", function () {
  const app = {
    data: {
      email: "",
      password: "",
      showPassword: false,
      errorMessage: ""
    },
    togglePassword: function () {
      this.data.showPassword = !this.data.showPassword;
      document.getElementById("password").type = this.data.showPassword ? "text" : "password";
    },
    login: function () {
      const emailInput = document.getElementById("email").value;
      const passwordInput = document.getElementById("password").value;

      if (emailInput === "admin@example.com" && passwordInput === "123") {
        alert("Login successful!");
      } else {
        alert("Invalid email or password.");
      }
    }
  };

  // Attach event listeners
  document.getElementById("toggle-password").addEventListener("click", function () {
    app.togglePassword();
  });

  document.getElementById("login-btn").addEventListener("click", function () {
    app.login();
  });
});
