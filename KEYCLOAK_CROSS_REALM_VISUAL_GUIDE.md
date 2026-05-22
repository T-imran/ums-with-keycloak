# Complete Visual Setup Guide: Bank-Asia User Access

## 🎯 Your Current Setup
```
Master Realm
  └─ iam-admin-service (Service Client)
       ├─ realm-management:query-users ✅
       ├─ realm-management:view-users ✅
       ├─ realm-management:manage-users ✅
       └─ ❌ NO permissions for bank-asia realm!

Bank-Asia Realm  
  └─ Users (CANNOT ACCESS YET)
```

## 🔧 What You Need to Do

### CRITICAL: Add Bank-Asia Permissions

The **iam-admin-service** client in **master realm** needs roles from **bank-asia realm**.

---

## ✅ SOLUTION 1: Cross-Realm Role Mapping (Recommended)

### In Keycloak Admin Console:

```
1. Login to Keycloak Admin Console
2. Select "Master" realm (top-left dropdown)
3. Go to: Clients → iam-admin-service
4. Click "Service Account Roles" tab
5. Click "Assign Role" button
6. Look for "bank-asia realm-management" in the list
   - If you see it, select these roles:
     ✓ manage-users
     ✓ query-users  
     ✓ view-users
     ✓ view-realm
   - Click "Assign"
```

### Expected Result After Assignment:
```
Service Account Roles (Master Realm)
├─ realm-management (master)
│  ├─ query-users
│  ├─ view-users
│  └─ manage-users
│
└─ realm-management (bank-asia) ✨ NEW
   ├─ query-users
   ├─ view-users
   ├─ manage-users
   └─ view-realm
```

---

## ⚙️ SOLUTION 2: If Bank-Asia Roles Don't Appear

### Create Dedicated Client in Bank-Asia Realm

**Master Realm Setup:**
```
Master Realm (unchanged)
  └─ iam-admin-service
       └─ realm-management:manage-users
```

**Additional Bank-Asia Setup:**
```
Bank-Asia Realm
  └─ Create NEW client: "iam-admin-service-banking"
       ├─ Client Type: Service Account (enabled)
       └─ Service Account Roles:
           ├─ realm-management:manage-users
           ├─ realm-management:query-users
           └─ realm-management:view-users
```

### Getting Tokens for Both Realms:

Update your **application.yml**:

```yaml
app:
  keycloak:
    admin:
      # Master realm (for master operations)
      master:
        server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
        realm: ${KEYCLOAK_ADMIN_REALM:master}
        client-id: ${KEYCLOAK_ADMIN_CLIENT_ID:iam-admin-service}
        client-secret: ${KEYCLOAK_ADMIN_CLIENT_SECRET:0N8BiHdbgUCUiFO94swgkrHRFJpUW0uw}
      
      # Bank-Asia realm (for bank-asia operations)
      banking:
        server-url: ${KEYCLOAK_SERVER_URL:http://localhost:8080}
        realm: ${KEYCLOAK_REALM:bank-asia}
        client-id: ${KEYCLOAK_BANKING_CLIENT_ID:iam-admin-service-banking}
        client-secret: ${KEYCLOAK_BANKING_CLIENT_SECRET:<get_from_keycloak>}
```

---

## 🧪 Verification Steps

### Step 1: Decode Your Access Token

```bash
# Get master realm token
TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=iam-admin-service" \
  -d "client_secret=0N8BiHdbgUCUiFO94swgkrHRFJpUW0uw" | jq -r '.access_token')

echo $TOKEN
```

### Step 2: Decode at JWT.IO
1. Go to https://jwt.io/
2. Paste the token in the left box
3. In "Payload" section (right side), look for:

**If Solution 1 worked (cross-realm roles):**
```json
{
  "realm_access": {
    "roles": ["default-roles-master", "manage-users", "query-users", "view-users"]
  },
  "resource_access": {
    "bank-asia-realm-management": {
      "roles": ["manage-users", "query-users", "view-users"]
    }
  }
}
```

**In Token Details:**
- ✅ Look for `resource_access` section
- ✅ Should have entry for `bank-asia-realm-management`
- ✅ Should list `manage-users`, `query-users`, `view-users`

### Step 3: Test the Users API

```bash
# With proper permissions, this should work:
curl -X GET "http://localhost:8080/admin/realms/bank-asia/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  | jq '.'

# Expected response (list of users):
[
  {
    "id": "user-id-123",
    "username": "testuser",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "enabled": true,
    "emailVerified": false
  }
]
```

### Step 4: Test Your Spring Boot App

```bash
# Make sure you have a user with ROLE_ADMIN or ROLE_SUPER_ADMIN
curl -X GET "http://localhost:8081/iam-admin-service/api/v1/users" \
  -H "Authorization: Bearer <your_frontend_jwt_token>" \
  -H "Content-Type: application/json" \
  | jq '.'
```

---

## 🐛 Troubleshooting

### Issue 1: "bank-asia realm-management" Not In Role List
**Problem:** When assigning roles, you don't see bank-asia realm options.

**Solution:**
```bash
# Check if both realms are properly configured
# 1. Login to Keycloak Admin Console
# 2. Go to Master Realm → Clients → realm-management
# 3. Go to Service Accounts Roles
# 4. Check if bank-asia clients are listed

# If not visible, try:
# 1. Go to bank-asia realm (top-left)
# 2. Go to Clients → realm-management
# 3. Go to Scope tab
# 4. Enable "Full Scope Allowed" (if available)
```

### Issue 2: 403 Forbidden When Querying Bank-Asia Users
**Problem:** Token works but API returns 403.

**Solution:**
```bash
# Decode token and check roles
# If "bank-asia-realm-management" is NOT in resource_access, 
# the role assignment didn't work

# Try manual assignment via REST API:
curl -X POST "http://localhost:8080/admin/realms/master/users/SERVICE_ACCOUNT_USER_ID/role-mappings/clients/BANK_ASIA_REALM_MANAGEMENT_CLIENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '[
    {"id":"role-id-manage-users","name":"manage-users"},
    {"id":"role-id-query-users","name":"query-users"}
  ]'
```

### Issue 3: Database Shows No Users
**Problem:** API returns empty array even with proper permissions.

**Verification:**
```bash
# Make sure users actually exist in bank-asia realm
curl -X GET "http://localhost:8080/admin/realms/bank-asia/users?briefRepresentation=false" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.[] | {username, id, enabled}'

# If empty, create a test user first:
curl -X POST "http://localhost:8080/admin/realms/bank-asia/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username":"testuser",
    "email":"test@bankaisa.com",
    "firstName":"Test",
    "lastName":"User",
    "enabled":true
  }'
```

### Issue 4: Permission Works in Keycloak but Not in App
**Problem:** Keycloak API works with token, but Spring app still shows 403.

**Solution:**
- Check your `@PreAuthorize` annotations
- Verify the JWT token contains expected roles
- Check `SecurityConfig` allows the endpoint
- Enable debug logging:

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    com.erainfotech.ums: DEBUG
```

---

## 📋 Quick Checklist

Before testing, verify:

- [ ] Master realm has "iam-admin-service" client
- [ ] iam-admin-service has Service Roles assigned
- [ ] Bank-Asia realm exists and has users
- [ ] ✨ Bank-Asia realm-management roles are assigned to iam-admin-service
- [ ] Token decoding shows bank-asia roles in JWT
- [ ] Spring app has valid JWT auth token
- [ ] @PreAuthorize roles match JWT token roles
- [ ] Bank-Asia realm users exist in database

---

## 💡 Pro Tips

1. **Always Decode Tokens:** Before debugging permissions, always check JWT at jwt.io
2. **Cross-Realm Roles:** May take a few seconds to propagate - try refreshing token
3. **Service Account IDs:** Get via: `/admin/realms/master/clients/{client-id}/service-account-user`
4. **Role IDs:** Get via: `/admin/realms/bank-asia/roles`
5. **Check Permissions:** Use Keycloak logs at `/admin/realms/master/events`

---

## 🚀 Final Command to Test Everything

```bash
#!/bin/bash

echo "=== Getting Master Token ==="
TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=iam-admin-service" \
  -d "client_secret=0N8BiHdbgUCUiFO94swgkrHRFJpUW0uw" | jq -r '.access_token')

echo "Token obtained: ${TOKEN:0:50}..."

echo -e "\n=== Token Payload (decode at jwt.io) ==="
echo $TOKEN | cut -d. -f2 | base64 -d 2>/dev/null | jq '.resource_access'

echo -e "\n=== Querying Bank-Asia Users ==="
curl -s -X GET "http://localhost:8080/admin/realms/bank-asia/users" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.[] | {username, id, enabled}'

echo -e "\n=== Testing Spring App Endpoint ==="
# Replace with your actual frontend token
APP_TOKEN="your_app_jwt_token_here"
curl -s -X GET "http://localhost:8081/iam-admin-service/api/v1/users" \
  -H "Authorization: Bearer $APP_TOKEN" \
  | jq '.'
```

Save this as `test-cross-realm.sh` and run with `bash test-cross-realm.sh`

---

**You're almost there! Once you assign the bank-asia realm roles to your service client, everything will work.** ✨

