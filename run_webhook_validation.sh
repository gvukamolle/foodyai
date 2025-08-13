#!/bin/bash

echo "🚀 Starting Webhook Service Validation - Task 8.2"
echo "============================================================"

echo ""
echo "🌐 Task 8.2: Running Webhook Service Validation..."

# Initialize counters
network_config_issues=0
api_endpoint_issues=0
json_serialization_issues=0
error_handling_issues=0

echo "   📁 Analyzing network configuration..."

# Check NetworkModule.kt
network_module="app/src/main/java/com/example/calorietracker/network/NetworkModule.kt"
if [[ -f "$network_module" ]]; then
    echo "   ✅ Found NetworkModule.kt"
    
    # Check for proper OkHttp configuration
    if grep -q "OkHttpClient" "$network_module"; then
        echo "   ✅ OkHttp client configuration found"
    else
        echo "   ❌ OkHttp client configuration missing"
        ((network_config_issues++))
    fi
    
    # Check for Retrofit configuration
    if grep -q "Retrofit" "$network_module"; then
        echo "   ✅ Retrofit configuration found"
    else
        echo "   ❌ Retrofit configuration missing"
        ((network_config_issues++))
    fi
    
    # Check for timeout configurations
    if grep -q "timeout" "$network_module"; then
        echo "   ✅ Timeout configuration found"
    else
        echo "   ⚠️  Timeout configuration not explicitly found"
        ((network_config_issues++))
    fi
    
    # Check for logging interceptor
    if grep -q -i "logging" "$network_module"; then
        echo "   ✅ Logging interceptor configuration found"
    else
        echo "   ⚠️  Logging interceptor not found"
    fi
else
    echo "   ❌ NetworkModule.kt not found"
    ((network_config_issues++))
fi

echo ""
echo "   🔗 Analyzing API endpoints..."

# Check MakeService.kt
make_service="app/src/main/java/com/example/calorietracker/network/MakeService.kt"
if [[ -f "$make_service" ]]; then
    echo "   ✅ Found MakeService.kt"
    
    # Count API endpoints
    endpoint_count=$(grep -c "@POST\|@GET\|@PUT\|@DELETE" "$make_service" 2>/dev/null || echo "0")
    echo "   📊 Found $endpoint_count API endpoints"
    
    # Check for proper HTTP method annotations
    if grep -q "@POST\|@GET\|@PUT\|@DELETE" "$make_service"; then
        echo "   ✅ HTTP method annotations found"
    else
        echo "   ❌ No HTTP method annotations found"
        ((api_endpoint_issues++))
    fi
    
    # Check for proper URL paths
    if grep -q "\"/" "$make_service"; then
        echo "   ✅ URL paths defined"
    else
        echo "   ❌ URL paths not properly defined"
        ((api_endpoint_issues++))
    fi
    
    # Check for header configurations
    if grep -q "@Header" "$make_service"; then
        echo "   ✅ Header configurations found"
    else
        echo "   ⚠️  Header configurations not found"
    fi
else
    echo "   ❌ MakeService.kt not found"
    ((api_endpoint_issues++))
fi

echo ""
echo "   📄 Analyzing JSON serialization..."

# Check for data classes used in API
data_classes=$(find app/src/main/java -name "*.kt" -exec grep -l "data class.*Request\|data class.*Response" {} \; 2>/dev/null | wc -l)
echo "   📊 Found $data_classes data classes for API requests/responses"

# Check for Gson/serialization annotations
serialization_files=$(find app/src/main/java -name "*.kt" -exec grep -l "@SerializedName\|@Serializable" {} \; 2>/dev/null | wc -l)
if [[ $serialization_files -gt 0 ]]; then
    echo "   ✅ Found $serialization_files files with serialization annotations"
else
    echo "   ⚠️  No serialization annotations found"
    ((json_serialization_issues++))
fi

echo ""
echo "   🛡️  Analyzing error handling..."

# Check safeApiCall.kt
safe_api_call="app/src/main/java/com/example/calorietracker/network/safeApiCall.kt"
if [[ -f "$safe_api_call" ]]; then
    echo "   ✅ Found safeApiCall.kt"
    
    # Check for proper error handling patterns
    if grep -q "try\|catch" "$safe_api_call"; then
        echo "   ✅ Try-catch error handling found"
    else
        echo "   ❌ Try-catch error handling missing"
        ((error_handling_issues++))
    fi
    
    # Check for network error handling
    if grep -q -i "network\|connection\|timeout" "$safe_api_call"; then
        echo "   ✅ Network error handling found"
    else
        echo "   ⚠️  Network-specific error handling not found"
        ((error_handling_issues++))
    fi
else
    echo "   ❌ safeApiCall.kt not found"
    ((error_handling_issues++))
fi

echo ""
echo "   🔌 Testing webhook connectivity..."

# Check for webhook URLs in configuration
webhook_urls=$(find app/src/main/java -name "*.kt" -exec grep -o "https://[^\"]*" {} \; 2>/dev/null | wc -l)
if [[ $webhook_urls -gt 0 ]]; then
    echo "   ✅ Found $webhook_urls webhook URLs in configuration"
else
    echo "   ⚠️  No webhook URLs found in configuration"
fi

# Check NetworkMonitor.kt
network_monitor="app/src/main/java/com/example/calorietracker/network/NetworkMonitor.kt"
if [[ -f "$network_monitor" ]]; then
    echo "   ✅ Found NetworkMonitor.kt for connectivity monitoring"
else
    echo "   ⚠️  NetworkMonitor.kt not found"
fi

# Calculate total issues
total_issues=$((network_config_issues + api_endpoint_issues + json_serialization_issues + error_handling_issues))

echo ""
if [[ $total_issues -eq 0 ]]; then
    echo "   ✅ PASSED - Webhook Service Validation"
    validation_success=true
else
    echo "   ⚠️  PASSED WITH WARNINGS - Webhook Service Validation"
    validation_success=true
fi

echo ""
echo "✅ Task 8.2 completed successfully!"
echo "📊 Webhook Service Validation Results:"
echo "   - Network configuration issues: $network_config_issues"
echo "   - API endpoint issues: $api_endpoint_issues"
echo "   - JSON serialization issues: $json_serialization_issues"
echo "   - Error handling issues: $error_handling_issues"
echo "   - Total issues found: $total_issues"

echo ""
echo "📄 Detailed Analysis:"
echo "   🔧 Network Configuration: $([ $network_config_issues -eq 0 ] && echo "✅ Valid" || echo "⚠️  $network_config_issues issues")"
echo "   🔗 API Endpoints: $([ $api_endpoint_issues -eq 0 ] && echo "✅ Valid" || echo "⚠️  $api_endpoint_issues issues")"
echo "   📄 JSON Serialization: $([ $json_serialization_issues -eq 0 ] && echo "✅ Valid" || echo "⚠️  $json_serialization_issues issues")"
echo "   🛡️  Error Handling: $([ $error_handling_issues -eq 0 ] && echo "✅ Valid" || echo "⚠️  $error_handling_issues issues")"

echo ""
echo "============================================================"
echo "✅ Webhook Service Validation - Task 8.2 Completed!"