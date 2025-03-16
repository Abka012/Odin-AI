# Define paths (adjusted for your environment)
$BackendDir = "C:\Users\Romar\OneDrive\Desktop\Odin-AI\backend\ai"
$FlaskDir = "C:\Users\Romar\OneDrive\Desktop\Odin-AI\backend\ai\src\AI"  # Directory containing app.py
$FlaskFile = "app.py"  # File name of Flask app
$PythonPath = "python"  # Use python3 if needed
$FrontendDir = "C:\Users\Romar\OneDrive\Desktop\Odin-AI\backend\ai\src\main\resources\static"  # Directory containing index.html

# Function to check if a port is in use
function Test-Port {
    param ($Port)
    if (Test-NetConnection -ComputerName localhost -Port $Port -InformationLevel Quiet -ErrorAction SilentlyContinue) {
        Write-Host "Port $Port is already in use. Please free it before running."
        exit 1
    }
}

# Check required ports
Test-Port 8080
Test-Port 5000

# Start Flask ML service
Write-Host "Starting Flask ML service..."
if (-not (Test-Path "$FlaskDir\$FlaskFile")) {
    Write-Host "Flask app not found at $FlaskDir\$FlaskFile! Please update the path in the script."
    exit 1
}
Set-Location -Path $FlaskDir -ErrorAction Stop
Start-Process -NoNewWindow -FilePath $PythonPath -ArgumentList $FlaskFile -RedirectStandardOutput "flask.log" -RedirectStandardError "flask.err"
Start-Sleep -Seconds 2
$flaskProcess = Get-Process -Name "python" -ErrorAction SilentlyContinue
if (-not $flaskProcess) {
    Write-Host "Failed to start Flask. Check $FlaskDir\flask.log and flask.err for details."
    exit 1
}
$FlaskPid = $flaskProcess.Id
Write-Host "Flask running on http://localhost:5001 (PID: $FlaskPid)"

# Start Spring Boot backend
Write-Host "Starting Spring Boot backend..."
if (-not (Test-Path $BackendDir)) {
    Write-Host "Backend directory not found at $BackendDir!"
    exit 1
}
Set-Location -Path $BackendDir -ErrorAction Stop
Start-Process -NoNewWindow -FilePath "mvn" -ArgumentList "spring-boot:run -Djdk.tls.client.protocols=TLSv1.2" -RedirectStandardOutput "spring.log" -RedirectStandardError "spring.err"
Start-Sleep -Seconds 5
$springProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue
if (-not $springProcess) {
    Write-Host "Failed to start Spring Boot. Check $BackendDir\spring.log and spring.err for details."
    exit 1
}
$SpringPid = $springProcess.Id
Write-Host "Spring Boot running on http://localhost:8080 (PID: $SpringPid)"

# Optional: Start a simple HTTP server for frontend (if not served by Spring Boot)
# Uncomment the block below if you need a separate frontend server

Write-Host "Starting frontend server..."
if (-not (Test-Path "$FrontendDir\index.html")) {
    Write-Host "Frontend index.html not found at $FrontendDir! Skipping frontend start."
} else {
    Set-Location -Path $FrontendDir -ErrorAction Stop
    Start-Process -NoNewWindow -FilePath $PythonPath -ArgumentList "-m", "http.server", "8081" -RedirectStandardOutput "frontend.log" -RedirectStandardError "frontend.err"
    Start-Sleep -Seconds 2
    $frontendProcess = Get-Process -Name "python" | Where-Object { $_.Id -ne $FlaskPid } -ErrorAction SilentlyContinue
    if (-not $frontendProcess) {
        Write-Host "Failed to start frontend server. Check $FrontendDir\frontend.log and frontend.err for details."
    } else {
        $FrontendPid = $frontendProcess.Id
        Write-Host "Frontend running on http://localhost:8081 (PID: $FrontendPid)"
    }
}


Write-Host "All services started successfully!"
Write-Host "Press Ctrl+C to stop all services."
Write-Host "Logs are in $FlaskDir\flask.log and $BackendDir\spring.log"

# Wait for Ctrl+C and clean up
try {
    while ($true) { Start-Sleep -Seconds 1 }
} finally {
    Write-Host "Stopping services..."
    Stop-Process -Name "python" -Force -ErrorAction SilentlyContinue
    Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue
    Write-Host "Services stopped."
}