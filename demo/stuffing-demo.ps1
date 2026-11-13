# Credential-stuffing demo for java-login-fingerprint (Windows PowerShell).
#
# Fires 5 failed logins from ONE simulated device (the same request id, so the
# stub Fingerprint service maps them to one visitor id), then shows the 6th
# attempt blocked by the gate with 429, while a different device can still
# log in fine.
#
# Usage: start the app first (.\mvnw.cmd spring-boot:run), then run this script.

$Base = if ($env:BASE) { $env:BASE } else { "http://localhost:8080" }

function Try-Login($Username, $Password, $RequestId) {
    $body = @{ username = $Username; password = $Password; requestId = $RequestId } | ConvertTo-Json
    try {
        $res = Invoke-RestMethod -Uri "$Base/api/login" -Method Post -Body $body -ContentType "application/json"
        return "200 $res"
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        $detail = ""
        try { $detail = ($_.ErrorDetails.Message | ConvertFrom-Json).error } catch { }
        return "$status $detail"
    }
}

Write-Host "--- 5 failed logins from one device (expect 401 each) ---"
for ($i = 1; $i -le 5; $i++) {
    Write-Host "attempt ${i}: $(Try-Login 'demo' 'wrong-password' 'demo-stuffing-device')"
}

Write-Host "--- 6th attempt from the same device (expect 429, blocked) ---"
Write-Host (Try-Login 'demo' 'wrong-password' 'demo-stuffing-device')

Write-Host "--- A different device logs in fine (expect 200) ---"
Write-Host (Try-Login 'demo' 'password123' 'demo-good-device')

Write-Host "--- A bot visit is rejected outright (expect 403) ---"
Write-Host (Try-Login 'demo' 'password123' 'demo-bot-device')
