# 🖥️ Keycloak UI: A Visual Reference

## What You Should See in Keycloak Admin Console

This guide shows you exactly what to look for when configuring cross-realm permissions.

---

## Before Configuration ❌

### Master Realm → Clients → iam-admin-service → Service Account Roles

```
┌─────────────────────────────────────────────┐
│ Service Account Roles                       │
├─────────────────────────────────────────────┤
│                                             │
│  realm-management (master)                  │
│  ├─ query-users                             │
│  ├─ manage-users                            │
│  └─ view-users                              │
│                                             │
│  ❌ NO bank-asia roles visible!             │
│                                             │
└─────────────────────────────────────────────┘

Problem: Service client can only manage master realm!
```

---

## After Configuration ✅

### Master Realm → Clients → iam-admin-service → Service Account Roles

```
┌─────────────────────────────────────────────┐
│ Service Account Roles                       │
├─────────────────────────────────────────────┤
│                                             │
│  realm-management (master)                  │
│  ├─ query-users                             │
│  ├─ manage-users                            │
│  └─ view-users                              │
│                                             │
│  ✨ realm-management (bank-asia)            │
│  ├─ query-users                             │ ← NEW!
│  ├─ manage-users                            │ ← NEW!
│  └─ view-users                              │ ← NEW!
│                                             │
│  ✅ Cross-realm permissions assigned!       │
│                                             │
└─────────────────────────────────────────────┘

Success: Service client can now manage bank-asia realm!
```

---

## Step-by-Step: Where to Click

### Step 1: Keycloak Admin Console
```
URL: http://localhost:8080/admin
Login with master admin credentials
```

### Step 2: Select Master Realm
```
TOP-LEFT DROPDOWN
┌──────────────────────────┐
│ Realm                    │ ← Click here
├──────────────────────────┤
│ Master         ← Current │
│ bank-asia      ← Others  │
└──────────────────────────┘

Make sure "Master" is selected!
```

### Step 3: Navigate to Clients
```
LEFT SIDEBAR
┌──────────────────────────┐
│ Master Realm             │
│ ├─ Clients         ← Click
│ ├─ Realms                │
│ ├─ Users                 │
│ └─ ...                   │
└──────────────────────────┘
```

### Step 4: Find iam-admin-service
```
CLIENTS LIST
┌──────────────────────────────────────────┐
│ Clients                                  │
├──────────────────────────────────────────┤
│ Search... (type "iam-admin-service")     │
├──────────────────────────────────────────┤
│ ✓ admin-cli                              │
│ ✓ iam-admin-service     ← Click THIS    │
│ ✓ realm-management                       │
└──────────────────────────────────────────┘
```

### Step 5: Go to Service Account Roles Tab
```
CLIENT DETAILS
┌──────────────────────────────────────────┐
│ iam-admin-service                        │
├──────────────────────────────────────────┤
│ Settings │ Credentials │ Roles │         │
│         │              │ Service Account Roles ← Click
├──────────────────────────────────────────┤
│ [Content of tab]                         │
└──────────────────────────────────────────┘
```

### Step 6: Assign Role Button
```
SERVICE ACCOUNT ROLES TAB
┌──────────────────────────────────────────┐
│ Service Account Roles                    │
├──────────────────────────────────────────┤
│ [Assign Role] ← Click HERE               │
│ [Available Roles]        [Assigned Roles]│
├──────────────────────────────────────────┤
│ ...                                      │
└──────────────────────────────────────────┘
```

### Step 7: Role Selection Dialog
```
ASSIGN ROLE
┌──────────────────────────────────────────┐
│ Assign Role                              │
├──────────────────────────────────────────┤
│ Search... (type "bank-asia")             │
├──────────────────────────────────────────┤
│ Available Roles                          │
├──────────────────────────────────────────┤
│ □ admin-realm (master)                   │
│ □ admin-realm (bank-asia)                │
│ □ realm-management (master) ← Already has
│ ✓ realm-management (bank-asia) ← SELECT! │
│ □ ...                                    │
│                                          │
│ realm-management (bank-asia) expanded:   │
│ ☐ create-client                          │
│ ✓ manage-users         ← Check this      │
│ ✓ query-users          ← Check this      │
│ ✓ manage-realm         ← Check this      │
│ ✓ view-realm           ← Check this      │
│ ...                                      │
│                                          │
│ [Assign] ← Final Click                   │
└──────────────────────────────────────────┘
```

---

## What the Dialogs Look Like

### Role Selection - Before
```
Available Roles to Assign              Assigned Roles to This Client
┌──────────────────────────────┐      ┌──────────────────────────────┐
│ ☐ admin-realm (master)       │      │ ✓ realm-management (master)  │
│ ☐ admin-realm (bank-asia)    │      │ - manage-users               │
│ ☐ realm-management (master)  │      │ - query-users                │
│ ☐ realm-management (bank-asia)│ →   │ - view-users                 │
│ ☐ ...                        │      │                              │
└──────────────────────────────┘      └──────────────────────────────┘

Problem: bank-asia roles not assigned!
```

### Role Selection - After
```
Available Roles to Assign              Assigned Roles to This Client
┌──────────────────────────────┐      ┌──────────────────────────────┐
│ ☐ admin-realm (master)       │      │ ✓ realm-management (master)  │
│ ☐ admin-realm (bank-asia)    │      │ - manage-users               │
│ ☐ realm-management (master)  │      │ - query-users                │
│ ✓ realm-management (bank-asia)│ →   │ - view-users                 │
│ ☐ ...                        │      │ ✓ realm-management (bank-asia)
│                              │      │ - manage-users               │
│  Roles inside selection:     │      │ - query-users                │
│  ✓ manage-users              │      │ - view-users                 │
│  ✓ query-users               │      │ - ...                        │
│  ✓ view-realm                │      │                              │
│  ✓ view-users                │      │                              │
└──────────────────────────────┘      └──────────────────────────────┘

✅ Success: bank-asia roles now assigned!
```

---

## Verification Checklist in UI

### ✅ Things to Verify

```
□ Master Realm selected (top-left dropdown)
□ iam-admin-service client found in Clients list
□ Service Account Roles tab is visible
□ Can see both "realm-management (master)" and "realm-management (bank-asia)"
□ realm-management (bank-asia) has these roles:
  ✓ manage-users
  ✓ query-users
  ✓ view-users
  ✓ (view-realm optional)
```

---

## Common UI Issues & Fixes

### Issue 1: Can't Find iam-admin-service Client
```
SOLUTION:
1. Make sure you're in MASTER realm (top-left)
2. Use search box: "iam-admin-service"
3. If still not found:
   - Go to Realm Settings
   - Check realm name is "master"
   - Create the client if missing
```

### Issue 2: Service Account Roles Tab Not Showing
```
SOLUTION:
1. Make sure "iam-admin-service" is selected (you're viewing it)
2. Look for tabs near "Settings"
3. Tabs should be: Settings | Credentials | Roles | Service Account Roles
4. If missing, click "..." menu → "Service Account Roles"
```

### Issue 3: bank-aria realm-management Not in Role List
```
SOLUTION:
1. Verify bank-asia realm exists (see Realms in left sidebar)
2. Clear browser cache: Ctrl+F5
3. Log out and log back in
4. Try again with new dropdown
5. If still missing, create bank-asia realm or check name spelling
```

### Issue 4: Assigned Roles Don't Appear in Service Account Roles
```
SOLUTION:
1. Refresh page: F5
2. Click away and back to tab
3. Restart Keycloak container (if Docker)
4. Logout and login again
5. Clear browser cookies
```

---

## JWT Token Verification

After assigning roles, your JWT token should contain:

### Token Structure (via jwt.io)
```
HEADER (visible at jwt.io):
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "..."
}

PAYLOAD (this is what we check):
{
  "iss": "http://localhost:8080/realms/master",
  "realm_access": {
    "roles": ["manage-users", "query-users", "view-users"]
  },
  "resource_access": {
    "realm-management": {
      "roles": ["manage-users", "query-users"]
    },
    "bank-asia-realm-management": {    ← NEW! Must be here!
      "roles": ["manage-users", "query-users", "view-users"]
    }
  }
}
```

### How to Check Your Token

1. Run: `.\test-cross-realm.ps1`
2. Go to: https://jwt.io/
3. Paste token in left box
4. Look at Payload (right side) for `resource_access`
5. Must have `"bank-asia-realm-management"` entry
6. That entry must have the roles we assigned

---

## Visual Checklist

Print this out and check off as you go:

```
STEP 1: Keycloak Admin Console
□ Keycloak running at http://localhost:8080
□ Can access admin console with password
□ Logged in successfully

STEP 2: Navigate to Service Client
□ Top-left dropdown shows "Master" realm
□ Left sidebar shows Clients option
□ Clients list shows "iam-admin-service"
□ Clicked on iam-admin-service to view details

STEP 3: Service Account Roles Tab
□ Viewing iam-admin-service client details
□ Can see "Service Account Roles" tab
□ Tab shows existing master realm roles
□ Can see "Assign Role" button

STEP 4: Assign Bank-Asia Roles
□ Clicked "Assign Role" button
□ Can see "bank-asia realm-management" in list
□ Selected realm-management (bank-asia)
□ Checked manage-users ✓
□ Checked query-users ✓
□ Checked view-users ✓
□ Clicked "Assign" button

STEP 5: Verify Assignment
□ Back in Service Account Roles tab
□ Can see both:
  - realm-management (master)
  - realm-management (bank-asia) ✨
□ bank-asia section shows assigned roles

STEP 6: Test & Verify
□ Ran: .\test-cross-realm.ps1
□ Diagnostic shows all ✓ marks
□ Token includes bank-asia roles
□ Can query bank-asia users (HTTP 200)
```

---

## Quick Reference Card

### 🎯 What Should Be Assigned
```
Realm: Master
Client: iam-admin-service
Service Account Role: realm-management (bank-asia)
  ✓ manage-users
  ✓ query-users
  ✓ view-users
```

### 🚫 What Should NOT Be Changed
```
❌ Don't change Master realm settings
❌ Don't create new client (use existing one)
❌ Don't modify bank-asia realm
❌ Don't change app code or application.yml
```

### ✅ After Assignment, You Should See
```
JWT Token includes:
  "resource_access": {
    "bank-asia-realm-management": {
      "roles": [...]
    }
  }

API Response:
  HTTP 200 OK + list of users from bank-asia
```

---

## Troubleshooting Decision Tree

```
START: Why can't I access bank-asia users?
│
├─ HTTP 403 Forbidden
│  └─ → Missing permissions
│     └─ → Did you assign bank-asia realm-management roles?
│        ├─ YES → Restart app to clear token cache
│        └─ NO → Go assign them now!
│
├─ HTTP 404 Not Found
│  └─ → Realm doesn't exist or wrong name
│     └─ → Verify bank-asia realm exists
│
├─ Empty list (HTTP 200)
│  └─ → Correct permissions but no users
│     └─ → Create test users in bank-asia
│
└─ Token doesn't have bank-asia roles
   └─ → Assignment didn't work
      └─ → Verify in UI that roles are assigned
         └─ → Try refreshing page, logging out, restarting Keycloak
```

---

## Screenshots Description

If you're looking at the Keycloak UI, here's where to find things:

### Screen 1: Keycloak Home
```
URL: http://localhost:8080
Shows: Keycloak login or homepage
Action: Click "Administration Console"
```

### Screen 2: Master Realm Dashboard
```
Shows: Master realm info
Left Sidebar: Shows Configure, Manage sections
Action: Look for "Clients" in left sidebar
```

### Screen 3: Clients List
```
Shows: All clients in realm
Visible: iam-admin-service should be here
Action: Click "iam-admin-service" to view details
```

### Screen 4: Client Details - Settings Tab
```
Shows: Client configuration
Visible: Multiple tabs at top
Tabs: Settings | Credentials | Roles | Service Account Roles
Action: Click "Service Account Roles" tab
```

### Screen 5: Service Account Roles Tab
```
Shows: Currently assigned roles
Left side: Available roles to assign
Right side: Assigned roles to this client
Action: Click "Assign Role" to add bank-asia permissions
```

### Screen 6: Role Assignment Dialog
```
Shows: List of roles available to assign
Options: Search for "bank-asia"
Result: See "realm-management (bank-asia)"
Action: Select it and click "Assign"
```

---

## Reference Quick Links

In Keycloak Admin Console:

| Where | How to Get There |
|-------|------------------|
| **Master Realm Home** | Click Master logo or top-left dropdown |
| **Clients List** | Left sidebar → Clients |
| **Service Client Details** | Clients → iam-admin-service |
| **Service Account Roles** | Client details → Service Account Roles tab |
| **Assign Roles** | Service Account Roles tab → Assign Role button |
| **Realms List** | Left sidebar → Realms |
| **Events Log** | Left sidebar → Events (for debugging) |
| **Security Events** | Left sidebar → Sessions (if issues) |

---

## Summary

The entire process is:
1. **Master Realm** → **Clients** → **iam-admin-service**
2. **Service Account Roles** tab
3. **Assign Role** button
4. Select **bank-asia realm-management**
5. Check: manage-users, query-users, view-users
6. **Assign** button

Then test with `.\test-cross-realm.ps1`

**That's it! You've got this! 🚀**

