# 🎯 Cross-Realm User Access: Complete Solution

## Your Problem
✗ Service client in **master realm** cannot query users from **bank-asia realm**

```
curl http://localhost:8080/admin/realms/bank-asia/users \
  -H "Authorization: Bearer <master_token>"
  
Result: 403 Forbidden OR empty list
```

---

## Root Cause Analysis

### Your Current Setup
```yaml
# application.yml
app:
  keycloak:
    admin:
      admin-realm: master              ← Token fetched here
      realm: bank-asia                 ← Users queried here
      client-id: iam-admin-service
      client-secret: <secret>
```

### What's Missing
The **iam-admin-service** service client needs:
- ✅ Admin roles in master realm (already configured)
- ❌ **Cross-realm permissions for bank-asia** (MISSING!)

---

## ✅ SOLUTION: Add Cross-Realm Permissions

### Method 1: UI-Based (Recommended for First Time)

#### Step 1: Open Keycloak Admin Console
```
1. Go to: http://localhost:8080/admin
2. Login with master admin credentials
```

#### Step 2: Select Master Realm
```
Top-left dropdown: "Master" (make sure it's selected)
```

#### Step 3: Navigate to Service Client
```
Left sidebar → Clients → iam-admin-service
```

#### Step 4: Go to Service Account Roles Tab
```
Click: "Service Account Roles" tab (near "Settings")
```

#### Step 5: Assign Bank-Asia Permissions
```
1. Click "Assign Role" button
2. Look for: "bank-asia realm-management"
3. Select ALL these roles:
   ☑ manage-users
   ☑ query-users
   ☑ view-users
   ☑ view-realm
4. Click "Assign"
```

#### Step 6: Verify Assignment
```
You should now see under "Service Account Roles":
├─ realm-management (master)
│  ├─ query-users
│  └─ manage-users
│
└─ realm-management (bank-asia) ✨ NEW
   ├─ manage-users
   ├─ query-users
   └─ view-users
```

---

## 🧪 Verify the Fix

### Windows PowerShell (Recommended for You)

```powershell
# Copy the full test-cross-realm.ps1 file to your project root
# Then run:

.\test-cross-realm.ps1

# Or with custom parameters:
.\test-cross-realm.ps1 `
  -KeycloakUrl "http://localhost:8080" `
  -MasterClientId "iam-admin-service" `
  -MasterClientSecret "0N8BiHdbgUCUiFO94swgkrHRFJpUW0uw" `
  -TargetRealm "bank-asia" `
  -SpringAppUrl "http://localhost:8081/iam-admin-service"
```

### Bash (Alternative)

```bash
chmod +x test-cross-realm.sh
./test-cross-realm.sh
```

### Manual Verification

```bash
# 1. Get master token
$token = curl.exe -s -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=client_credentials" `
  -d "client_id=iam-admin-service" `
  -d "client_secret=0N8BiHdbgUCUiFO94swgkrHRFJpUW0uw" | ConvertFrom-Json

# Token
$token.access_token

# 2. Query bank-asia users
curl.exe -X GET "http://localhost:8080/admin/realms/bank-asia/users" `
  -H "Authorization: Bearer $($token.access_token)"
```

---

## 🔍 What To Look For

### Success Indicators ✅

1. **HTTP 200 Response** - Not 403 or 404
2. **User List Returned** - Even if empty initially
3. **Token Contains Bank-Asia Roles**
   ```json
   {
     "resource_access": {
       "bank-asia-realm-management": {
         "roles": ["manage-users", "query-users", "view-users"]
       }
     }
   }
   ```

### Failure Indicators ❌

| Event | Reason | Solution |
|-------|--------|----------|
| HTTP 403 Forbidden | Missing bank-asia permissions | Assign roles in Service Account Roles |
| HTTP 404 Not Found | Bank-asia realm doesn't exist | Verify realm name in Keycloak |
| Empty user list | Correct permissions but no users | Create test users in bank-asia realm |
| Wrong token format | Token caching issue | Clear app cache, get fresh token |

---

## 🧠 How This Works (Technical Explanation)

### Before Fix (What's Happening Now)
```
1. iam-admin-service requests token
   POST /realms/master/protocol/openid-connect/token
   
2. Keycloak returns token with roles:
   {
     "realm_access": { "roles": ["manage-users"] },
     "resource_access": {}  ← NO bank-asia roles!
   }

3. Service tries to query bank-asia users
   GET /admin/realms/bank-asia/users
   Header: Bearer <token>
   
4. Keycloak checks token for bank-asia permissions
   Finds none → Returns 403 Forbidden
```

### After Fix (What Will Happen)
```
1. iam-admin-service requests token
   POST /realms/master/protocol/openid-connect/token
   
2. Keycloak returns token with roles:
   {
     "realm_access": { "roles": ["manage-users"] },
     "resource_access": {
       "bank-asia-realm-management": {
         "roles": ["manage-users", "query-users", "view-users"]
       }
     }
   }

3. Service tries to query bank-asia users
   GET /admin/realms/bank-asia/users
   Header: Bearer <token>
   
4. Keycloak checks token for bank-asia permissions
   Finds them → Returns users list (HTTP 200) ✅
```

---

## 🐛 Troubleshooting

### Issue 1: "I don't see bank-asia option when assigning roles"

**Possible Causes:**
- Bank-asia realm doesn't exist
- Realms aren't properly configured
- UI cache issue

**Solutions:**
```
1. Verify bank-asia realm exists:
   Go to: Master Realm → Realms dropdown → bank-asia should be there

2. Refresh browser cache
   Press: Ctrl+F5 or Cmd+Shift+R

3. Try logging out and logging back in

4. Check Keycloak logs:
   docker logs keycloak  (if using Docker)
```

### Issue 2: "Still getting 403 even after assigning roles"

**Possible Causes:**
- Roles assigned but token not refreshed
- Role assignment not complete
- Wrong roles selected

**Solutions:**
```
1. Restart your Spring Boot app
   This clears the token cache

2. Verify the right roles were assigned:
   Go back to Service Account Roles tab
   Make sure you see "realm-management (bank-asia)"

3. Get a fresh token and decode it at jwt.io

4. Check app logs:
   Look for: DEBUG logs from KeycloakAdminTokenService
```

### Issue 3: "Getting blank user list"

**Possible Causes:**
- Correct permissions but no users created
- Users in different realm
- Filter is hiding users

**Solutions:**
```
1. Create a test user in bank-asia realm:
   Go to: Bank-Asia Realm → Users → Create user

2. Query with verbose output:
   GET /admin/realms/bank-asia/users?max=100

3. Check if users actually exist:
   Go to: Bank-Asia Realm → Users tab (UI)
```

---

## 📋 Pre-Flight Checklist

Before testing, verify:

- [ ] Keycloak is running and accessible (http://localhost:8080)
- [ ] Master realm exists and has admin-service client
- [ ] Bank-asia realm exists
- [ ] Bank-asia realm has users (or create a test user)
- [ ] Spring Boot app is running (http://localhost:8081/iam-admin-service)
- [ ] test-cross-realm.ps1 file exists in project root

---

## 🚀 Testing Your Spring Boot API

Once cross-realm is working:

```bash
# 1. Get a valid JWT token for your user
# (Using your authentication flow)

$appToken = "<your_jwt_token>"

# 2. Test the listUsers endpoint
curl.exe -X GET "http://localhost:8081/iam-admin-service/api/v1/users" `
  -H "Authorization: Bearer $appToken" `
  -H "Content-Type: application/json"

# 3. Expected response (users from bank-asia realm):
[
  {
    "id": "uuid",
    "username": "testuser",
    "email": "test@bankaisa.com",
    "firstName": "Test",
    "lastName": "User",
    "enabled": true,
    "roles": ["ROLE_ADMIN"]
  }
]
```

---

## 📚 Resource Files in This Project

| File | Purpose |
|------|---------|
| **KEYCLOAK_CROSS_REALM_SETUP.md** | Detailed technical setup |
| **KEYCLOAK_CROSS_REALM_VISUAL_GUIDE.md** | Step-by-step with images |
| **test-cross-realm.ps1** | 🪟 Automated diagnostics (Windows PowerShell) |
| **test-cross-realm.sh** | 🐧 Automated diagnostics (Bash) |
| **application.yml** | Your app configuration |

---

## 🎓 Key Takeaways

1. **Service clients in master realm need cross-realm permissions** to manage other realms
2. **Permissions are assigned via "Service Account Roles"** in Keycloak UI
3. **Token caching means you need to refresh** after making changes
4. **Always verify by decoding JWT at jwt.io** to confirm role assignment worked

---

## ✨ Next Steps

1. **Assign bank-asia permissions** (see Method 1 above)
2. **Run diagnostic script** (`.\test-cross-realm.ps1`)
3. **Verify success** (should see bank-asia users)
4. **Test your Spring Boot API** (should return users list)
5. **Check logs** for any remaining issues

---

## 🆘 Still Having Issues?

If this doesn't work:

1. ✅ Run `test-cross-realm.ps1` and share the output
2. ✅ Check Keycloak logs
3. ✅ Verify realm names are correct
4. ✅ Decode JWT token at jwt.io and verify roles
5. ✅ Check Spring Boot app logs for authorization errors

**Common cause:** Forgetting to assign bank-asia realm-management roles to the service client.

---

**You've got this! 🚀 Just assign those cross-realm roles and everything will work!**

