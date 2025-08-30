#!/bin/bash

echo "=== Testing Make.com Connectivity ==="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

WEBHOOK_ID="653st2c10rmg92nlltf3y0m8sggxaac6"
MAKE_HOST="hook.us2.make.com"
MAKE_URL="https://${MAKE_HOST}/${WEBHOOK_ID}"

# 1. Test DNS resolution
echo "1. Testing DNS resolution for ${MAKE_HOST}..."
if host ${MAKE_HOST} > /dev/null 2>&1; then
    IP=$(host ${MAKE_HOST} | grep "has address" | head -1 | awk '{print $4}')
    echo -e "${GREEN}✓ DNS resolution successful: ${IP}${NC}"
else
    echo -e "${RED}✗ DNS resolution failed${NC}"
    echo "   Trying DNS-over-HTTPS..."
    DOH_RESPONSE=$(curl -s "https://cloudflare-dns.com/dns-query?name=${MAKE_HOST}&type=A" -H "accept: application/dns-json")
    if [[ $DOH_RESPONSE == *"Answer"* ]]; then
        IP=$(echo $DOH_RESPONSE | grep -o '"data":"[^"]*"' | head -1 | cut -d'"' -f4)
        echo -e "${GREEN}✓ DNS-over-HTTPS successful: ${IP}${NC}"
    else
        echo -e "${RED}✗ DNS-over-HTTPS also failed${NC}"
    fi
fi
echo ""

# 2. Test basic connectivity
echo "2. Testing HTTPS connectivity to Make.com..."
if curl -s -o /dev/null -w "%{http_code}" --connect-timeout 10 ${MAKE_URL} | grep -q "200\|404\|405"; then
    echo -e "${GREEN}✓ HTTPS connection successful${NC}"
else
    echo -e "${RED}✗ HTTPS connection failed${NC}"
fi
echo ""

# 3. Test with browser-like headers
echo "3. Testing with browser headers..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "User-Agent: Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36" \
    -H "Accept: application/json, text/plain, */*" \
    -H "Accept-Language: ru-RU,ru;q=0.9,en-US;q=0.8" \
    --connect-timeout 10 \
    ${MAKE_URL})

if [[ $RESPONSE == "200" ]] || [[ $RESPONSE == "404" ]] || [[ $RESPONSE == "405" ]]; then
    echo -e "${GREEN}✓ Browser headers test successful (HTTP ${RESPONSE})${NC}"
else
    echo -e "${RED}✗ Browser headers test failed (HTTP ${RESPONSE})${NC}"
fi
echo ""

# 4. Test POST request
echo "4. Testing POST request..."
TEST_JSON='{"test":true,"timestamp":'$(date +%s)'}'
POST_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X POST \
    -H "Content-Type: application/json" \
    -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" \
    -d "${TEST_JSON}" \
    --connect-timeout 15 \
    ${MAKE_URL} | tail -1)

if [[ $POST_RESPONSE == "200" ]]; then
    echo -e "${GREEN}✓ POST request successful${NC}"
elif [[ $POST_RESPONSE == "405" ]]; then
    echo -e "${YELLOW}⚠ POST received but method not allowed (normal for some webhooks)${NC}"
else
    echo -e "${RED}✗ POST request failed (HTTP ${POST_RESPONSE})${NC}"
fi
echo ""

# 5. Test multipart request
echo "5. Testing multipart/form-data request..."
# Create a small test file
echo "test" > /tmp/test_make.txt
MULTIPART_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -X POST \
    -F "file=@/tmp/test_make.txt" \
    -F "test=true" \
    -H "User-Agent: calorietracker/1.0" \
    --connect-timeout 15 \
    ${MAKE_URL} | tail -1)
rm /tmp/test_make.txt

if [[ $MULTIPART_RESPONSE == "200" ]]; then
    echo -e "${GREEN}✓ Multipart request successful${NC}"
else
    echo -e "${RED}✗ Multipart request failed (HTTP ${MULTIPART_RESPONSE})${NC}"
fi
echo ""

# Summary
echo "=== SUMMARY ==="
echo "If DNS fails but DNS-over-HTTPS works → DNS blocking by ISP"
echo "If connection fails but VPN works → DPI or SNI blocking"
echo "If only multipart fails → Specific protocol blocking"
echo ""
echo "Run this script with and without VPN to compare results."
