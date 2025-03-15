const { createApp } = Vue;

createApp({
    data() {
        return {
            email: "",
            password: "",
            showPassword: false,
            errorMessage: ""
        };
    },
    methods: {
        togglePassword() {
            this.showPassword = !this.showPassword;
        },
        login() {
            // Simulated authentication (replace with real API call)
            if (this.email === "admin" && this.password === "123") {
                alert("✅ Login successful! Redirecting...");
                window.location.href = "index.html"; // Redirect to dashboard
            } else {
                this.errorMessage = "❌ Invalid email or password.";
            }
        }
    }
}).mount("#app");
