# Diagnostic Script: Cross-Realm User Access Testing
# Run this to verify Keycloak configuration (Windows PowerShell)

param(
    [string]$KeycloakUrl = "http://localhost:8080",
    [string]$MasterClientId = "iam-admin-service",
    [string]$MasterClientSecret = "0N8BiHdbgUCUiFO94swgkrHRFJpUW0uw",
    [string]$TargetRealm = "bank-asia",
    [string]$SpringAppUrl = "http://localhost:8081/iam-admin-service"
)

# Helper function to print status
function Print-Status {
    param(
        [string]$Status,
        [string]$Message
    )

    $colors = @{
        '✓' = 'Green'
        '✗' = 'Red'
        '⚠' = 'Yellow'
        'ℹ' = 'Cyan'
    }

    $color = $colors[$Status]
    if (-not $color) { $color = 'White' }

    Write-Host "[$Status] " -NoNewline -ForegroundColor $color
    Write-Host $Message
}

function Print-Section {
    param([string]$Title)
    Write-Host ""
    Write-Host "━━━ $Title ━━━" -ForegroundColor Cyan
}

# Header
Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Keycloak Cross-Realm User Access Diagnostic Tool         ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Test 1: Keycloak Connectivity
Print-Section "Test 1: Keycloak Connectivity"
try {
    $response = Invoke-WebRequest -Uri $KeycloakUrl -UseBasicParsing -ErrorAction SilentlyContinue
    Print-Status "✓" "Keycloak is reachable at $KeycloakUrl"
} catch {
    Print-Status "✗" "Cannot reach Keycloak at $KeycloakUrl"
    exit 1
}

# Test 2: Get Master Token
Print-Section "Test 2: Get Master Realm Token"
try {
    $tokenUrl = "$KeycloakUrl/realms/master/protocol/openid-connect/token"
    $body = @{
        grant_type    = "client_credentials"
        client_id     = $MasterClientId
        client_secret = $MasterClientSecret
    }

    $tokenResponse = Invoke-WebRequest -Uri $tokenUrl `
        -Method Post `
        -Body $body `
        -ContentType "application/x-www-form-urlencoded" `
        -UseBasicParsing | ConvertFrom-Json

    $masterToken = $tokenResponse.access_token
    if (-not $masterToken) {
        Print-Status "✗" "Failed to get token: $($tokenResponse.error)"
        exit 1
    }

    Print-Status "✓" "Master token obtained successfully"
    Print-Status "ℹ" "Token: $($masterToken.Substring(0, 50))..."

} catch {
    Print-Status "✗" "Failed to get token: $_"
    exit 1
}

# Test 3: Decode Token and Check Roles
Print-Section "Test 3: Analyze Token Payload"
try {
    # Decode JWT
    $parts = $masterToken.Split('.')
    $payload = $parts[1]

    # Add padding if needed
    $padding = 4 - ($payload.Length % 4)
    if ($padding -ne 4) {
        $payload += '=' * $padding
    }

    $decodedBytes = [System.Convert]::FromBase64String($payload)
    $payloadDecoded = [System.Text.Encoding]::UTF8.GetString($decodedBytes) | ConvertFrom-Json

    Print-Status "✓" "Token is valid JWT"

    # Check realm_access roles
    Print-Status "ℹ" "Master Realm Roles:"
    if ($payloadDecoded.realm_access.roles) {
        $payloadDecoded.realm_access.roles | ForEach-Object { Write-Host "    - $_" }
    } else {
        Write-Host "    (none)"
    }

    # Check for bank-asia realm roles
    Print-Status "ℹ" "Checking for Bank-Asia Realm Permissions..."
    $bankingRoles = $payloadDecoded.resource_access."bank-asia-realm-management".roles

    if (-not $bankingRoles) {
        Print-Status "✗" "NO bank-asia realm roles found in token!"
        Print-Status "⚠" "SOLUTION: Assign bank-asia realm-management roles to iam-admin-service client"
        Write-Host "    1. Keycloak Admin Console → Master Realm → Clients → $MasterClientId"
        Write-Host "    2. Click 'Service Account Roles' tab"
        Write-Host "    3. Click 'Assign Role'"
        Write-Host "    4. Select 'bank-asia realm-management'"
        Write-Host "    5. Assign: manage-users, query-users, view-users"
    } else {
        Print-Status "✓" "Bank-Asia realm roles found!"
        Print-Status "ℹ" "Bank-Asia Roles:"
        $bankingRoles | ForEach-Object { Write-Host "    - $_" }
    }

} catch {
    Print-Status "✗" "Failed to decode token: $_"
}

# Test 4: Query Master Realm Users
Print-Section "Test 4: Query Master Realm Users"
try {
    $headers = @{
        "Authorization" = "Bearer $masterToken"
    }

    $masterUsersUrl = "$KeycloakUrl/admin/realms/master/users"
    $masterUsers = Invoke-WebRequest -Uri $masterUsersUrl `
        -Headers $headers `
        -UseBasicParsing | ConvertFrom-Json

    $count = @($masterUsers).Count
    if ($count -gt 0) {
        Print-Status "✓" "Can query master realm users ($count users found)"
    } else {
        Print-Status "⚠" "No users in master realm (or permission denied)"
    }

} catch {
    Print-Status "✗" "Failed to query master users: $_"
}

# Test 5: Query Bank-Asia Realm Users
Print-Section "Test 5: Query Bank-Asia Realm Users"
try {
    $headers = @{
        "Authorization" = "Bearer $masterToken"
    }

    $bankingUsersUrl = "$KeycloakUrl/admin/realms/$TargetRealm/users"

    # First, get HTTP status code
    $httpCall = @{
        Uri             = $bankingUsersUrl
        Headers         = $headers
        UseBasicParsing = $true
        ErrorAction     = "SilentlyContinue"
    }

    $response = Invoke-WebRequest @httpCall
    $statusCode = $response.StatusCode

    if ($statusCode -eq 200) {
        $bankingUsers = $response | ConvertFrom-Json
        $count = @($bankingUsers).Count

        if ($count -gt 0) {
            Print-Status "✓" "✨ Can query bank-asia realm users ($count users found)"
            Print-Status "ℹ" "Sample users from bank-asia:"
            $bankingUsers | ForEach-Object {
                $userInfo = @{
                    id       = $_.id
                    username = $_.username
                    email    = $_.email
                    enabled  = $_.enabled
                } | ConvertTo-Json
                Write-Host "    $userInfo"
            } | Select-Object -First 5
        } else {
            Print-Status "⚠" "Query successful (200 OK) but no users found"
            Print-Status "ℹ" "Verify that users exist in bank-asia realm"
        }
    } else {
        Print-Status "✗" "Query failed with HTTP $statusCode"
    }

} catch {
    $errorMessage = $_.Exception.Message
    if ($errorMessage -like "*403*") {
        Print-Status "✗" "Permission Denied (403) - Service client doesn't have bank-asia permissions"
        Print-Status "⚠" "SOLUTION: Follow the role assignment steps from Test 3"
    } elseif ($errorMessage -like "*404*") {
        Print-Status "✗" "Bank-Asia realm not found (404)"
    } else {
        Print-Status "✗" "Query failed: $errorMessage"
    }
}

# Test 6: Spring Boot App Connectivity
Print-Section "Test 6: Spring Boot App Connectivity"
try {
    $appHealthUrl = "$SpringAppUrl/actuator/health"
    $appHealth = Invoke-WebRequest -Uri $appHealthUrl `
        -UseBasicParsing | ConvertFrom-Json

    if ($appHealth.status -eq "UP") {
        Print-Status "✓" "Spring Boot app is running"
    } else {
        Print-Status "⚠" "Spring Boot app status: $($appHealth.status)"
    }

} catch {
    Print-Status "✗" "Spring Boot app is not responding"
    Print-Status "ℹ" "Make sure the app is running at $SpringAppUrl"
}

# Summary
Print-Section "Summary"

if ($payloadDecoded.resource_access."bank-asia-realm-management".roles -contains "manage-users") {
    Write-Host "✓ Cross-realm setup appears to be WORKING!" -ForegroundColor Green
    Write-Host "  ✓ Master token includes bank-asia permissions" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Green
    Write-Host "  1. Test your Spring Boot endpoints with a valid JWT token"
    Write-Host "  2. Verify @PreAuthorize roles match the JWT token roles"
    Write-Host "  3. Check app logs for any authorization errors"
} else {
    Write-Host "✗ Cross-realm setup needs configuration" -ForegroundColor Red
    Write-Host ""
    Write-Host "Quick Fix:" -ForegroundColor Yellow
    Write-Host "  1. Open Keycloak Admin Console"
    Write-Host "  2. Realm: Master → Clients → $MasterClientId"
    Write-Host "  3. Tab: Service Account Roles"
    Write-Host "  4. Click: Assign Role"
    Write-Host "  5. Search: bank-asia realm-management"
    Write-Host "  6. Assign: manage-users, query-users, view-users"
    Write-Host "  7. Click: Assign"
    Write-Host "  8. Re-run this script to verify"
}

# Debug Information
Print-Section "Debug Information"
Print-Status "ℹ" "Keycloak URL: $KeycloakUrl"
Print-Status "ℹ" "Master Client: $MasterClientId"
Print-Status "ℹ" "Target Realm: $TargetRealm"
Print-Status "ℹ" "Spring App URL: $SpringAppUrl"

Print-Section "Full Token Payload"
$payloadDecoded | ConvertTo-Json | Write-Host

Write-Host ""
Write-Host "Diagnostic complete!" -ForegroundColor Green
Write-Host ""

