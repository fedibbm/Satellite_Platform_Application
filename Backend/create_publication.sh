#!/bin/bash

# Script to create a publication via the API

BASE_URL="http://localhost:8080"

echo "Step 1: Registering/Login user..."

# Try to register a new user (if it already exists, login instead)
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/api/user/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_user",
    "email": "test@example.com",
    "password": "Test123456",
    "roles": ["ROLE_USER"]
  }')

echo "Register response: $REGISTER_RESPONSE"

# Login to get token
echo -e "\nStep 2: Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/user/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "Test123456"
  }')

echo "Login response: $LOGIN_RESPONSE"

# Extract token
TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.data.token // .token // empty')

if [ -z "$TOKEN" ] || [ "$TOKEN" == "null" ]; then
  echo -e "\n❌ Failed to get authentication token. Please check your credentials or backend logs."
  echo "Response was: $LOGIN_RESPONSE"
  exit 1
fi

echo -e "\n✅ Got authentication token: ${TOKEN:0:50}..."

# Create publication
echo -e "\nStep 3: Creating publication..."
PUBLICATION_RESPONSE=$(curl -s -X POST "$BASE_URL/api/community/publications" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Understanding NDVI in Satellite Imagery Analysis",
    "description": "A comprehensive guide to understanding and applying Normalized Difference Vegetation Index in satellite imagery analysis for agricultural monitoring and environmental assessment",
    "content": "<h1>Introduction to NDVI</h1><p>The Normalized Difference Vegetation Index (NDVI) is one of the most widely used indicators for assessing vegetation health and vigor from satellite imagery. This powerful metric has revolutionized how we monitor agricultural lands, forests, and ecosystems worldwide.</p><h2>What is NDVI?</h2><p>NDVI is a numerical indicator that uses the visible and near-infrared bands of the electromagnetic spectrum to analyze remote sensing measurements. The index ranges from -1 to +1, where higher values indicate healthier vegetation.</p><h2>How NDVI Works</h2><p>NDVI is calculated using the formula:</p><p><strong>NDVI = (NIR - Red) / (NIR + Red)</strong></p><p>Where:</p><ul><li><strong>NIR</strong> is the near-infrared band (typically 0.76-0.9 μm)</li><li><strong>Red</strong> is the red visible band (typically 0.6-0.7 μm)</li></ul><h2>Understanding NDVI Values</h2><ul><li><strong>-1 to 0</strong>: Water, clouds, snow, bare soil</li><li><strong>0 to 0.2</strong>: Bare soil, rocks, sand</li><li><strong>0.2 to 0.5</strong>: Sparse vegetation, grasslands, senescing crops</li><li><strong>0.5 to 0.8</strong>: Moderate to dense vegetation</li><li><strong>0.8 to 1.0</strong>: Very dense, healthy vegetation</li></ul><h2>Applications in Agriculture</h2><p>Farmers and agronomists use NDVI for:</p><ul><li>Crop health monitoring</li><li>Identifying areas of stress or disease</li><li>Optimizing irrigation and fertilization</li><li>Yield prediction and forecasting</li><li>Precision agriculture applications</li></ul><h2>Conclusion</h2><p>NDVI remains a fundamental tool in remote sensing and precision agriculture. Start exploring NDVI with your own satellite imagery today!</p>",
    "tags": ["NDVI", "Remote Sensing", "Vegetation Analysis", "Satellite Imagery", "Tutorial"],
    "status": "PUBLISHED",
    "readingTime": 8
  }')

echo "$PUBLICATION_RESPONSE" | jq '.'

# Check if successful
if echo "$PUBLICATION_RESPONSE" | jq -e '.data.id' > /dev/null 2>&1; then
  PUBLICATION_ID=$(echo "$PUBLICATION_RESPONSE" | jq -r '.data.id')
  echo -e "\n✅ Publication created successfully!"
  echo "Publication ID: $PUBLICATION_ID"
  echo "Status: PUBLISHED"
  echo -e "\nView it at: http://localhost:3000/community/publications/$PUBLICATION_ID"
else
  echo -e "\n❌ Failed to create publication"
  echo "Response: $PUBLICATION_RESPONSE"
fi
