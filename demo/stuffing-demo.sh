#!/usr/bin/env bash
# Credential-stuffing demo for java-login-fingerprint.
#
# Fires 5 failed logins from ONE simulated device (the same request id, so the
# stub Fingerprint service maps them to one visitor id), then shows the 6th
# attempt blocked by the gate with 429, while a different device can still
# log in fine.
#
# Usage: start the app first (./mvnw spring-boot:run), then run this script.
# Requires curl. On Windows, use stuffing-demo.ps1 instead.

BASE="${BASE:-http://localhost:8080}"

echo "--- 5 failed logins from one device (expect 401 each) ---"
for i in 1 2 3 4 5; do
  echo -n "attempt $i: "
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$BASE/api/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"demo","password":"wrong-password","requestId":"demo-stuffing-device"}'
done

echo "--- 6th attempt from the same device (expect 429, blocked) ---"
curl -s -X POST "$BASE/api/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"wrong-password","requestId":"demo-stuffing-device"}'
echo

echo "--- A different device logs in fine (expect 200) ---"
curl -s -X POST "$BASE/api/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"password123","requestId":"demo-good-device"}'
echo

echo "--- A bot visit is rejected outright (expect 403) ---"
curl -s -X POST "$BASE/api/login" \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"password123","requestId":"demo-bot-device"}'
echo
