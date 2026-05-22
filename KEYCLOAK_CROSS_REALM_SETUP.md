# Keycloak Cross-Realm Admin Setup (Master → Bank-Asia)

## Problem
Service client in **master realm** cannot query users from **bank-asia realm** even with admin roles assigned.

## Root Cause
The service client needs:
1. ✅ **Token from master realm** (already working)
2. ❌ **Permissions to manage bank-asia realm** (missing)

---

## Solution: Step-by-Step Keycloak Configuration

### Step 1: Configure Master Realm Service Client
**Realm:** Master  
**Client:** iam-admin-service  

#### The service client should have these **Service Roles** (already assigned):
- ✅ `realm-management` → `query-users`
- ✅ `realm-management` → `view-users`
- ✅ `realm-management` → `manage-users`
- ✅ `realm-management` → `view-realm`

**Why?** These roles allow the client to authenticate and perform actions within the master realm scope.

---

### Step 2: Grant Bank-Asia Realm Permissions (CRITICAL!)

The master realm's `iam-admin-service` client must also have permissions in the **bank-asia realm**.

#### Option A: Via Role Mapping in Master Realm (Recommended)
1. Go to **Master Realm** → **Clients** → **iam-admin-service** → **Service Account Roles**
2. In the "Role Mapping" tab, look for:
   - **`bank-asia realm-management`** → `manage-users`
   - **`bank-asia realm-management`** → `query-users`
   - **`bank-asia realm-management`** → `view-users`
   - **`bank-asia realm-management`** → `view-realm`

3. Add these roles if not present:
   ```
   Click "Assign Role" 
   → Select "realm-management" (from bank-asia)
   → Select manage-users, query-users, view-users
   → Assign
   ```

#### Option B: Via Bank-Asia Realm Role Mapping
1. Go to **Bank-Asia Realm** → **Clients** → **realm-management** (not iam-admin-service, but the realm-management client in bank-asia)
2. Go to **Service Accounts Roles**
3. Find the **iam-admin-service** client from master realm and assign roles

---

### Step 3: Verify Client Roles in Master Realm

After assigning roles, verify the JWT payload includes bank-asia realm roles:

```bash
# Get access token
curl -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=iam-admin-service" \
  -d "client_secret=0N8BiHdbgUCUiFO94swgkrHRFJpUW0uw"

# Decode the token at jwt.io and look for:
{
  "realm_access": {
    "roles": ["manage-users", "query-users", "view-users"]
  },
  "resource_access": {
    "bank-asia-realm-management": {
      "roles": ["manage-users", "query-users", "view-users"]
    }
  }
}
```

---

### Step 4: Check Client Permissions in Bank-Asia Realm

1. Go to **Bank-Asia Realm** → **Clients** → Filter for **iam-admin-service** from master realm
   - *Note: Cross-realm clients appear as "master/iam-admin-service"*

2. If not visible, check:
   - Go to **Bank-Asia Realm** → **Clients** → **realm-management** client
   - Go to **Service Accounts Roles** tab
   - Look for roles assigned to master realm's iam-admin-service

---

## Alternative: If Roles Won't Propagate

If cross-realm role assignments don't work, use this workaround:

### Create a Dedicated Client in Bank-Asia Realm

1. **Create new client in bank-asia realm:**
   - **Name:** `iam-admin-service-bank-asia`
   - **Client Type:** Service Account (toggle on)
   - **Enabled:** Yes

2. **Assign Service Roles:**
   - realm-management → manage-users
   - realm-management → query-users
   - realm-management → view-users
   - realm-management → view-realm

3. **Get dedicated token for bank-asia:**
   ```bash
   curl -X POST http://localhost:8080/realms/bank-asia/protocol/openid-connect/token \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=client_credentials" \
     -d "client_id=iam-admin-service-bank-asia" \
     -d "client_secret=<client_secret>"
   ```

---

## XML Configuration for Bulk Assignment (if UI doesn't work)

If Keycloak UI won't assign cross-realm roles, export the client configuration:

```xml
<!-- Master Realm iam-admin-service Client -->
<serviceAccountClientRoles>
  <client id="realm-management">
    <role>query-users</role>
    <role>view-users</role>
    <role>manage-users</role>
  </client>
  <client id="bank-asia-realm-management">
    <role>query-users</role>
    <role>view-users</role>
    <role>manage-users</role>
  </client>
</serviceAccountClientRoles>
```

---

## Troubleshooting Checklist

| Issue | Check This |
|-------|-----------|
| Still can't see bank-asia users | ✅ Master token includes `resource_access.bank-asia-realm-management.roles` |
| 403 Forbidden errors | ✅ Token has role permissions (decode JWT) |
| Users found but empty list | ✅ Users actually exist in bank-asia realm |
| Role mapping not visible | ✅ Cross-realm roles may need manual federation |

---

## Testing the Full Flow

```bash
#!/bin/bash

# 1. Get master realm token
TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=iam-admin-service" \
  -d "client_secret=0N8BiHdbgUCUiFO94swgkrHRFJpUW0uw" | jq -r '.access_token')

echo "Token: $TOKEN"

# 2. Query users in bank-asia realm
curl -X GET "http://localhost:8080/admin/realms/bank-asia/users" \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.'

# 3. Test your app endpoint
curl -X GET "http://localhost:8081/iam-admin-service/api/v1/users" \
  -H "Authorization: Bearer <frontend_token>" \
  | jq '.'
```

---

## Best Practices

1. **Always use Service Accounts** for server-to-server communication (✅ you're doing this)
2. **Assign Minimal Roles** - Only grant what's needed (security principle)
3. **Cross-Realm Roles** - Must be explicitly assigned in Keycloak
4. **Token Caching** - Your code already caches tokens (✅ good!)
5. **Test with jwt.io** - Always decode tokens to verify roles

---

## Next Steps

1. ✅ Add bank-asia realm roles to master realm's iam-admin-service client
2. ✅ Verify token includes cross-realm roles
3. ✅ Test the API endpoint
4. ✅ Monitor logs for authorization errors

