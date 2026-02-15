#!/bin/bash

# Test Event-Driven Workflow Triggers
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

API_URL="http://localhost:9090"
TOKEN="eyJhbGciOiJIUzUxMiJ9.eyJuYW1lIjoidGhlbWF0aWNpYW4xIiwiZW1haWwiOiJ0aGVtYXRpY2lhbkBleGFtcGxlLmNvbSIsInJvbGVzIjpbIlJPTEVfVEhFTUFUSUNJQU4iXSwic3ViIjoidGhlbWF0aWNpYW5AZXhhbXBsZS5jb20iLCJpYXQiOjE3NzExNTM4OTcsImV4cCI6MTc3MTI0MDI5N30.n2YjnoAQZvdHNzRS7z5PPDcbgwo0yb20Sgjd7jlakwvknmfkO8dBT1zKW9RP7L-m2VBL7PkaHhsjGeaBw2wrVg"

echo -e "${YELLOW}=== Testing Event-Driven Workflow Triggers ===${NC}\n"

# Test 1: Webhook Health
echo -e "${YELLOW}Test 1: Webhook Health Check${NC}"
curl -s $API_URL/api/webhooks/health | jq .
echo ""

# Test 2: Create Scheduled Trigger
echo -e "${YELLOW}Test 2: Create Scheduled Trigger (Every 5 min)${NC}"
SCHEDULED=$(curl -s -X POST $API_URL/api/workflow/triggers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "workflowDefinitionId": "test-workflow-1234",
    "projectId": "test-project-456",
    "name": "Test Scheduled Trigger2",
    "description": "Runs every 5 minutes",
    "type": "SCHEDULED",
    "config": {
      "cronExpression": "0 */5 * * * *",
      "timezone": "UTC"
    },
    "defaultInputs": {"testMode": true}
  }')
echo "$SCHEDULED" | jq .
TRIGGER_ID=$(echo "$SCHEDULED" | jq -r '.id // empty')
echo ""

# Test 3: Create Webhook Trigger
echo -e "${YELLOW}Test 3: Create Webhook Trigger${NC}"
WEBHOOK=$(curl -s -X POST $API_URL/api/workflow/triggers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "workflowDefinitionId": "test-workflow-1234",
    "projectId": "test-project-456",
    "name": "Test Webhook Trigger",
    "type": "WEBHOOK",
    "config": {
      "allowedMethods": ["POST"]
    }
  }')
echo "$WEBHOOK" | jq .
WEBHOOK_ID=$(echo "$WEBHOOK" | jq -r '.id // empty')
WEBHOOK_URL=$(echo "$WEBHOOK" | jq -r '.webhookUrl // empty')
WEBHOOK_SECRET=$(echo "$WEBHOOK" | jq -r '.config.webhookSecret // empty')
echo ""

# Test 4: Call Webhook
if [[ -n "$WEBHOOK_ID" ]]; then
    echo -e "${YELLOW}Test 4: Call Webhook${NC}"
    curl -s -X POST $API_URL$WEBHOOK_URL \
      -H "Content-Type: application/json" \
      -H "X-Webhook-Secret: $WEBHOOK_SECRET" \
      -d '{"imageId": "test-123"}' | jq .
    echo ""
fi

# Test 5: Get Trigger Stats
if [[ -n "$TRIGGER_ID" ]]; then
    echo -e "${YELLOW}Test 5: Get Trigger Statistics${NC}"
    curl -s $API_URL/api/workflow/triggers/$TRIGGER_ID/stats \
      -H "Authorization: Bearer $TOKEN" | jq .
    echo ""
fi

echo -e "${GREEN}=== Tests Complete ===${NC}"
echo "Created Triggers:"
[[ -n "$TRIGGER_ID" ]] && echo "  Scheduled: $TRIGGER_ID"
[[ -n "$WEBHOOK_ID" ]] && echo "  Webhook: $WEBHOOK_ID"
