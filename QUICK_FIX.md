# ⚡ Quick Reference: Cross-Realm Fix (2 Minutes)

## 🎯 The Problem
Your service client in **master realm** can't see users in **bank-asia realm**

## ✅ The Fix (3 Steps)

### Step 1: Open Keycloak Admin
```
http://localhost:8080/admin
```

### Step 2: Assign Bank-Asia Permissions
```
Clients → iam-admin-service 
  → Service Account Roles 
    → Assign Role
      → bank-asia realm-management
        ✓ manage-users
        ✓ query-users
        ✓ view-users
```

### Step 3: Test It Works
```powershell
# Windows PowerShell
.\test-cross-realm.ps1

# Or manually
$token = curl.exe -s -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=client_credentials" `
  -d "client_id=iam-admin-service" `
  -d "client_secret=0N8BiHdbgUCUiFO94swgkrHRFJpUW0uw" | ConvertFrom-Json

curl.exe "http://localhost:8080/admin/realms/bank-asia/users" `
  -H "Authorization: Bearer $($token.access_token)"
```

## 🟢 Success = Users List (HTTP 200)

## 🔴 Still Doesn't Work?

1. ✅ Verify bank-asia roles in "Service Account Roles" tab
2. ✅ Refresh token cache (restart Spring app)
3. ✅ Decode token at jwt.io and check for bank-asia permissions
4. ✅ Check Keycloak logs

---

**That's it! The service client just needs cross-realm permissions.** 🚀

