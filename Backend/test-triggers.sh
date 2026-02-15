#!/bin/bash

# Quick Test Script for Event-Driven Triggers
# Phase 7 Testing

# Configuration
BASE_URL="http://localhost:9090"
TOKEN="eyJhbGciOiJIUzUxMiJ9.eyJuYW1lIjoidGhlbWF0aWNpYW4xIiwiZW1haWwiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInJvbGVzIjpbIlJPTEVfVEhFTUFUSUNJQU4iXSwic3ViIjoidGhlbWF0aWNpYW5AZXhhbXBsZS5jb20iLCJpYXQiOjE3NzExNTM4OTcsImV4cCI6MTc3MTI0MDI5N30.n2YjnoAQZvdHNzRS7z5PPDcbgwo0yb20Sgjd7jlakwvknmfkO8dBT1zKW9RP7L-m2VBL7PkaHhsjGeaBw2wrVg"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Phase 7: Event-Driven Triggers - Testing"
echo "=========================================="
echo ""

# Test 1: Webhook Health Check
echo -e "${YELLOW}Test 1: Webhook Health Check${NC}"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/webhooks/health")
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
BODY=$(echo "$HEALTH_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Webhook service is UP${NC}"
    echo "Response: $BODY"
else
    echo -e "${RED}✗ Webhook service failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
fi
echo ""

# Test 2: Create a Simple Scheduled Trigger (Every 5 minutes for testing)
echo -e "${YELLOW}Test 2: Create Scheduled Trigger (Every 5 minutes)${NC}"
echo "NOTE: Replace workflowDefinitionId and projectId with actual values from your system"
echo ""

read -p "Enter Workflow Definition ID: " WORKFLOW_ID
read -p "Enter Project ID: " PROJECT_ID

if [ -z "$WORKFLOW_ID" ] || [ -z "$PROJECT_ID" ]; then
    echo -e "${RED}✗ Workflow ID and Project ID are required${NC}"
    echo ""
else
    CREATE_TRIGGER_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -X POST "${BASE_URL}/api/workflow/triggers" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${TOKEN}" \
        -d "{
          \"workflowDefinitionId\": \"${WORKFLOW_ID}\",
          \"projectId\": \"${PROJECT_ID}\",
          \"name\": \"Test Trigger - Every 5 Min\",
          \"description\": \"Test scheduled trigger that runs every 5 minutes\",
          \"type\": \"SCHEDULED\",
          \"config\": {
            \"cronExpression\": \"0 */5 * * * *\",
            \"timezone\": \"UTC\",
            \"maxExecutions\": 5
          },
          \"defaultInputs\": {
            \"imageId\": \"test-image-001\",
            \"userId\": \"thematician@example.com\",
            \"projectId\": \"${PROJECT_ID}\",
            \"testMode\": true
          }
        }")
    
    HTTP_CODE=$(echo "$CREATE_TRIGGER_RESPONSE" | tail -n1)
    BODY=$(echo "$CREATE_TRIGGER_RESPONSE" | head -n-1)
    
    if [ "$HTTP_CODE" = "201" ]; then
        echo -e "${GREEN}✓ Scheduled trigger created successfully${NC}"
        echo "Response:"
        echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
        
        # Extract trigger ID
        TRIGGER_ID=$(echo "$BODY" | jq -r '.id' 2>/dev/null)
        if [ -n "$TRIGGER_ID" ] && [ "$TRIGGER_ID" != "null" ]; then
            echo ""
            echo -e "${GREEN}Trigger ID: $TRIGGER_ID${NC}"
            echo "Save this ID for further testing!"
            echo ""
            
            # Test 3: Get Trigger Statistics
            echo -e "${YELLOW}Test 3: Get Trigger Statistics${NC}"
            STATS_RESPONSE=$(curl -s -w "\n%{http_code}" \
                -X GET "${BASE_URL}/api/workflow/triggers/${TRIGGER_ID}/stats" \
                -H "Authorization: Bearer ${TOKEN}")
            
            HTTP_CODE=$(echo "$STATS_RESPONSE" | tail -n1)
            BODY=$(echo "$STATS_RESPONSE" | head -n-1)
            
            if [ "$HTTP_CODE" = "200" ]; then
                echo -e "${GREEN}✓ Statistics retrieved successfully${NC}"
                echo "Response:"
                echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
            else
                echo -e "${RED}✗ Failed to get statistics (HTTP $HTTP_CODE)${NC}"
                echo "Response: $BODY"
            fi
            echo ""
            
            # Instructions for waiting
            echo -e "${YELLOW}⏳ Waiting Instructions:${NC}"
            echo "The trigger will execute in approximately 5 minutes."
            echo "Wait 5-6 minutes, then run this command to check execution:"
            echo ""
            echo "curl -X GET \"${BASE_URL}/api/workflow/triggers/${TRIGGER_ID}/stats\" \\"
            echo "  -H \"Authorization: Bearer ${TOKEN}\" | jq"
            echo ""
            echo "Look for:"
            echo "  - executionCount: Should be 1 or more"
            echo "  - lastExecutedAt: Recent timestamp"
            echo "  - lastExecutionStatus: SUCCESS or FAILED"
            echo "  - nextExecutionTime: 5 minutes from last execution"
            echo ""
        fi
    else
        echo -e "${RED}✗ Failed to create trigger (HTTP $HTTP_CODE)${NC}"
        echo "Response: $BODY"
    fi
    echo ""
fi

# Test 4: Create Webhook Trigger
echo -e "${YELLOW}Test 4: Create Webhook Trigger${NC}"
if [ -z "$WORKFLOW_ID" ] || [ -z "$PROJECT_ID" ]; then
    echo -e "${YELLOW}Skipping (no workflow/project ID provided)${NC}"
else
    CREATE_WEBHOOK_RESPONSE=$(curl -s -w "\n%{http_code}" \
        -X POST "${BASE_URL}/api/workflow/triggers" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${TOKEN}" \
        -d "{
          \"workflowDefinitionId\": \"${WORKFLOW_ID}\",
          \"projectId\": \"${PROJECT_ID}\",
          \"name\": \"Test Webhook Trigger\",
          \"description\": \"Test webhook for external integration\",
          \"type\": \"WEBHOOK\",
          \"config\": {
            \"allowedMethods\": [\"POST\"],
            \"bodyMapping\": {
              \"satelliteId\": \"satelliteId\",
              \"imageUrl\": \"imageUrl\"
            }
          },
          \"defaultInputs\": {
            \"source\": \"external_test\"
          }
        }")
    
    HTTP_CODE=$(echo "$CREATE_WEBHOOK_RESPONSE" | tail -n1)
    BODY=$(echo "$CREATE_WEBHOOK_RESPONSE" | head -n-1)
    
    if [ "$HTTP_CODE" = "201" ]; then
        echo -e "${GREEN}✓ Webhook trigger created successfully${NC}"
        echo "Response:"
        echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
        
        # Extract trigger ID and webhook secret
        WEBHOOK_TRIGGER_ID=$(echo "$BODY" | jq -r '.id' 2>/dev/null)
        WEBHOOK_SECRET=$(echo "$BODY" | jq -r '.config.webhookSecret' 2>/dev/null)
        WEBHOOK_URL=$(echo "$BODY" | jq -r '.webhookUrl' 2>/dev/null)
        
        if [ -n "$WEBHOOK_TRIGGER_ID" ] && [ "$WEBHOOK_TRIGGER_ID" != "null" ]; then
            echo ""
            echo -e "${GREEN}Webhook Details:${NC}"
            echo "  Trigger ID: $WEBHOOK_TRIGGER_ID"
            echo "  Webhook URL: ${BASE_URL}${WEBHOOK_URL}"
            echo "  Webhook Secret: $WEBHOOK_SECRET"
            echo ""
            
            # Test 5: Call the Webhook
            echo -e "${YELLOW}Test 5: Call the Webhook${NC}"
            WEBHOOK_CALL_RESPONSE=$(curl -s -w "\n%{http_code}" \
                -X POST "${BASE_URL}${WEBHOOK_URL}" \
                -H "Content-Type: application/json" \
                -H "X-Webhook-Secret: ${WEBHOOK_SECRET}" \
                -d '{
                  "satelliteId": "SAT-001",
                  "imageUrl": "https://example.com/test-image.tif",
                  "timestamp": "2026-02-15T10:00:00Z"
                }')
            
            HTTP_CODE=$(echo "$WEBHOOK_CALL_RESPONSE" | tail -n1)
            BODY=$(echo "$WEBHOOK_CALL_RESPONSE" | head -n-1)
            
            if [ "$HTTP_CODE" = "200" ]; then
                echo -e "${GREEN}✓ Webhook called successfully${NC}"
                echo "Response:"
                echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
            else
                echo -e "${RED}✗ Webhook call failed (HTTP $HTTP_CODE)${NC}"
                echo "Response: $BODY"
            fi
            echo ""
        fi
    else
        echo -e "${RED}✗ Failed to create webhook trigger (HTTP $HTTP_CODE)${NC}"
        echo "Response: $BODY"
    fi
fi
echo ""

# Test 6: Get All Enabled Triggers
echo -e "${YELLOW}Test 6: Get All Enabled Triggers${NC}"
ENABLED_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X GET "${BASE_URL}/api/workflow/triggers/enabled" \
    -H "Authorization: Bearer ${TOKEN}")

HTTP_CODE=$(echo "$ENABLED_RESPONSE" | tail -n1)
BODY=$(echo "$ENABLED_RESPONSE" | head -n-1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Retrieved enabled triggers${NC}"
    TRIGGER_COUNT=$(echo "$BODY" | jq '. | length' 2>/dev/null || echo "?")
    echo "Found $TRIGGER_COUNT enabled trigger(s)"
    echo ""
    echo "Response:"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
else
    echo -e "${RED}✗ Failed to get enabled triggers (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
fi
echo ""

# Summary
echo "=========================================="
echo "Testing Summary"
echo "=========================================="
echo ""
echo "✓ Tests completed!"
echo ""
echo "Next Steps:"
echo "1. Wait 5-6 minutes for scheduled trigger to execute"
echo "2. Check trigger statistics with the command provided above"
echo "3. Check MongoDB collections:"
echo "   - workflow_triggers"
echo "   - workflow_events"
echo "   - execution_history"
echo ""
echo "4. To test events manually, you can call:"
echo "   (This requires adding WorkflowEventPublisher to your controllers)"
echo ""
echo "For more tests, use the HTTP files:"
echo "  - Backend/http/workflow/Triggers.http"
echo "  - Backend/http/workflow/Webhooks.http"
echo ""
