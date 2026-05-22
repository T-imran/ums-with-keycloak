#!/bin/bash
# Diagnostic Script: Cross-Realm User Access Testing
# Run this to verify Keycloak configuration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
KEYCLOAK_URL="${1:-http://localhost:8080}"
MASTER_CLIENT_ID="${2:-iam-admin-service}"
MASTER_CLIENT_SECRET="${3:-0N8BiHdbgUCUiFO94swgkrHRFJpUW0uw}"
TARGET_REALM="bank-asia"
SPRING_APP_URL="${4:-http://localhost:8081/iam-admin-service}"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Keycloak Cross-Realm User Access Diagnostic Tool         ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}\n"

# Function to print status
print_status() {
    local status=$1
    local message=$2
    if [ "$status" = "✓" ]; then
        echo -e "${GREEN}[✓]${NC} $message"
    elif [ "$status" = "✗" ]; then
        echo -e "${RED}[✗]${NC} $message"
    elif [ "$status" = "⚠" ]; then
        echo -e "${YELLOW}[⚠]${NC} $message"
    elif [ "$status" = "ℹ" ]; then
        echo -e "${BLUE}[ℹ]${NC} $message"
    fi
}

# Test 1: Keycloak Connectivity
echo -e "${BLUE}━━━ Test 1: Keycloak Connectivity ━━━${NC}"
if curl -s "${KEYCLOAK_URL}" > /dev/null 2>&1; then
    print_status "✓" "Keycloak is reachable at $KEYCLOAK_URL"
else
    print_status "✗" "Cannot reach Keycloak at $KEYCLOAK_URL"
    exit 1
fi

# Test 2: Get Master Token
echo -e "\n${BLUE}━━━ Test 2: Get Master Realm Token ━━━${NC}"
TOKEN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=${MASTER_CLIENT_ID}" \
  -d "client_secret=${MASTER_CLIENT_SECRET}")

MASTER_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token' 2>/dev/null)
ERROR=$(echo "$TOKEN_RESPONSE" | jq -r '.error' 2>/dev/null)

if [ "$ERROR" != "null" ] && [ ! -z "$ERROR" ]; then
    print_status "✗" "Failed to get token: $ERROR"
    print_status "ℹ" "Response: $(echo $TOKEN_RESPONSE | jq '.')"
    exit 1
elif [ -z "$MASTER_TOKEN" ] || [ "$MASTER_TOKEN" = "null" ]; then
    print_status "✗" "Token is empty"
    exit 1
else
    print_status "✓" "Master token obtained successfully"
    print_status "ℹ" "Token: ${MASTER_TOKEN:0:50}..."
fi

# Test 3: Decode Token and Check Roles
echo -e "\n${BLUE}━━━ Test 3: Analyze Token Payload ━━━${NC}"
PAYLOAD=$(echo "$MASTER_TOKEN" | cut -d. -f2)
PAYLOAD_DECODED=$(echo "$PAYLOAD" | base64 -d 2>/dev/null || echo "Failed to decode")

if echo "$PAYLOAD_DECODED" | jq . > /dev/null 2>&1; then
    print_status "✓" "Token is valid JWT"

    # Extract realm_access roles
    print_status "ℹ" "Master Realm Roles:"
    echo "$PAYLOAD_DECODED" | jq '.realm_access.roles[]' 2>/dev/null | sed 's/^/    - /'

    # Check for bank-asia realm roles
    print_status "ℹ" "Checking for Bank-Asia Realm Permissions..."
    BANKING_ROLES=$(echo "$PAYLOAD_DECODED" | jq '.resource_access."bank-asia-realm-management".roles' 2>/dev/null)

    if [ "$BANKING_ROLES" = "null" ] || [ -z "$BANKING_ROLES" ]; then
        print_status "✗" "NO bank-asia realm roles found in token!"
        print_status "⚠" "SOLUTION: Assign bank-asia realm-management roles to iam-admin-service client"
        echo -e "    1. Keycloak Admin Console → Master Realm → Clients → iam-admin-service"
        echo -e "    2. Click 'Service Account Roles' tab"
        echo -e "    3. Click 'Assign Role'"
        echo -e "    4. Select 'bank-asia realm-management'"
        echo -e "    5. Assign: manage-users, query-users, view-users"
    else
        print_status "✓" "Bank-Asia realm roles found!"
        print_status "ℹ" "Bank-Asia Roles:"
        echo "$PAYLOAD_DECODED" | jq '.resource_access."bank-asia-realm-management".roles[]' 2>/dev/null | sed 's/^/    - /'
    fi
else
    print_status "✗" "Failed to decode token"
fi

# Test 4: Query Master Realm Users
echo -e "\n${BLUE}━━━ Test 4: Query Master Realm Users ━━━${NC}"
MASTER_USERS=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/master/users" \
  -H "Authorization: Bearer ${MASTER_TOKEN}" \
  2>/dev/null)

MASTER_USER_COUNT=$(echo "$MASTER_USERS" | jq 'length' 2>/dev/null || echo "0")

if [ "$MASTER_USER_COUNT" -gt 0 ]; then
    print_status "✓" "Can query master realm users ($MASTER_USER_COUNT users found)"
else
    print_status "⚠" "No users in master realm (or permission denied)"
fi

# Test 5: Query Bank-Asia Realm Users
echo -e "\n${BLUE}━━━ Test 5: Query Bank-Asia Realm Users ━━━${NC}"
BANKING_USERS=$(curl -s -X GET "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/users" \
  -H "Authorization: Bearer ${MASTER_TOKEN}" \
  2>/dev/null)

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X GET "${KEYCLOAK_URL}/admin/realms/${TARGET_REALM}/users" \
  -H "Authorization: Bearer ${MASTER_TOKEN}" 2>/dev/null)

if [ "$HTTP_CODE" = "200" ]; then
    BANKING_USER_COUNT=$(echo "$BANKING_USERS" | jq 'length' 2>/dev/null || echo "0")
    if [ "$BANKING_USER_COUNT" -gt 0 ]; then
        print_status "✓" "✨ Can query bank-asia realm users ($BANKING_USER_COUNT users found)"
        print_status "ℹ" "Sample users from bank-asia:"
        echo "$BANKING_USERS" | jq '.[] | {id, username, email, enabled}' | head -20 | sed 's/^/    /'
    else
        print_status "⚠" "Query successful (200 OK) but no users found"
        print_status "ℹ" "Verify that users exist in bank-asia realm"
    fi
elif [ "$HTTP_CODE" = "403" ]; then
    print_status "✗" "Permission Denied (403) - Service client doesn't have bank-asia permissions"
    print_status "⚠" "SOLUTION: Follow the role assignment steps from Test 3"
elif [ "$HTTP_CODE" = "404" ]; then
    print_status "✗" "Bank-Asia realm not found (404)"
    print_status "⚠" "Verify realm exists or check realm name"
else
    print_status "✗" "Query failed with HTTP $HTTP_CODE"
    print_status "ℹ" "Response: $BANKING_USERS"
fi

# Test 6: Spring Boot App Connectivity
echo -e "\n${BLUE}━━━ Test 6: Spring Boot App Connectivity ━━━${NC}"
APP_HEALTH=$(curl -s "${SPRING_APP_URL}/actuator/health" 2>/dev/null)
APP_STATUS=$(echo "$APP_HEALTH" | jq -r '.status' 2>/dev/null)

if [ "$APP_STATUS" = "UP" ]; then
    print_status "✓" "Spring Boot app is running"
else
    print_status "✗" "Spring Boot app is not responding"
    print_status "ℹ" "Make sure the app is running at $SPRING_APP_URL"
fi

# Summary
echo -e "\n${BLUE}━━━ Summary ━━━${NC}"

if echo "$PAYLOAD_DECODED" | jq '.resource_access."bank-asia-realm-management"' 2>/dev/null | grep -q '"manage-users"'; then
    echo -e "${GREEN}✓ Cross-realm setup appears to be WORKING!${NC}"
    echo -e "  ${GREEN}✓${NC} Master token includes bank-asia permissions"

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "  ${GREEN}✓${NC} Can query bank-asia users successfully"
    fi

    echo -e "\n${GREEN}Next Steps:${NC}"
    echo -e "  1. Test your Spring Boot endpoints with a valid JWT token"
    echo -e "  2. Verify @PreAuthorize roles match the JWT token roles"
    echo -e "  3. Check app logs for any authorization errors"
else
    echo -e "${RED}✗ Cross-realm setup needs configuration${NC}"
    echo -e "\n${YELLOW}Quick Fix:${NC}"
    echo -e "  1. Open Keycloak Admin Console"
    echo -e "  2. Realm: Master → Clients → ${MASTER_CLIENT_ID}"
    echo -e "  3. Tab: Service Account Roles"
    echo -e "  4. Click: Assign Role"
    echo -e "  5. Search: bank-asia realm-management"
    echo -e "  6. Assign: manage-users, query-users, view-users"
    echo -e "  7. Click: Assign"
    echo -e "  8. Re-run this script to verify"
fi

# Additional debugging info
echo -e "\n${BLUE}━━━ Debug Information ━━━${NC}"
print_status "ℹ" "Keycloak URL: $KEYCLOAK_URL"
print_status "ℹ" "Master Client: $MASTER_CLIENT_ID"
print_status "ℹ" "Target Realm: $TARGET_REALM"
print_status "ℹ" "Spring App URL: $SPRING_APP_URL"

echo -e "\n${BLUE}━━━ Full Token Payload ━━━${NC}"
echo "$PAYLOAD_DECODED" | jq '.' 2>/dev/null | head -50

echo -e "\n${GREEN}Diagnostic complete!${NC}\n"

