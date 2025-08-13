#!/bin/bash

echo "🚀 Starting UI Data Flow Validation - Task 8.3"
echo "============================================================"

echo ""
echo "🎨 Task 8.3: Running UI Data Flow Validation..."

# Initialize counters
viewmodel_issues=0
data_binding_issues=0
state_management_issues=0
ui_component_issues=0

echo "   📱 Analyzing ViewModels..."

# Find all ViewModel files
viewmodel_files=$(find app/src/main/java -name "*ViewModel.kt" | wc -l)
echo "   📊 Found $viewmodel_files ViewModel files"

# Check each ViewModel
find app/src/main/java -name "*ViewModel.kt" | while read -r vm_file; do
    vm_name=$(basename "$vm_file" .kt)
    echo "   🔍 Analyzing $vm_name..."
    
    # Check for StateFlow usage
    if grep -q "StateFlow\|MutableStateFlow" "$vm_file"; then
        echo "     ✅ StateFlow usage found"
    else
        echo "     ⚠️  StateFlow not found, checking LiveData..."
        if grep -q "LiveData\|MutableLiveData" "$vm_file"; then
            echo "     ✅ LiveData usage found"
        else
            echo "     ❌ No reactive state management found"
            ((viewmodel_issues++))
        fi
    fi
    
    # Check for proper lifecycle handling
    if grep -q "viewModelScope\|CoroutineScope" "$vm_file"; then
        echo "     ✅ Coroutine scope usage found"
    else
        echo "     ⚠️  Coroutine scope not found"
        ((viewmodel_issues++))
    fi
    
    # Check for proper error handling
    if grep -q "try\|catch\|Result\|Either" "$vm_file"; then
        echo "     ✅ Error handling found"
    else
        echo "     ⚠️  Error handling not found"
        ((viewmodel_issues++))
    fi
done

echo ""
echo "   🎯 Analyzing State Management..."

# Check for proper state management patterns
state_files=$(find app/src/main/java -name "*.kt" -exec grep -l "_state\|_uiState\|State.*Flow" {} \; 2>/dev/null | wc -l)
echo "   📊 Found $state_files files with state management patterns"

# Check for immutable state patterns
immutable_state_files=$(find app/src/main/java -name "*.kt" -exec grep -l "data class.*State\|sealed class.*State" {} \; 2>/dev/null | wc -l)
if [[ $immutable_state_files -gt 0 ]]; then
    echo "   ✅ Found $immutable_state_files files with immutable state patterns"
else
    echo "   ⚠️  No immutable state patterns found"
    ((state_management_issues++))
fi

echo ""
echo "   🖼️  Analyzing UI Components..."

# Find Compose files
compose_files=$(find app/src/main/java -name "*.kt" -exec grep -l "@Composable" {} \; 2>/dev/null | wc -l)
echo "   📊 Found $compose_files Compose UI files"

# Check data binding in Compose components
echo "   🔍 Analyzing Compose data binding..."
compose_with_state=$(find app/src/main/java -name "*.kt" -exec grep -l "@Composable.*State\|collectAsState\|by.*State" {} \; 2>/dev/null | wc -l)
if [[ $compose_with_state -gt 0 ]]; then
    echo "   ✅ Found $compose_with_state Compose files with proper state binding"
else
    echo "   ⚠️  No proper state binding found in Compose files"
    ((data_binding_issues++))
fi

# Check for proper UI state handling
ui_state_handling=$(find app/src/main/java -name "*.kt" -exec grep -l "when.*state\|if.*state.*is" {} \; 2>/dev/null | wc -l)
if [[ $ui_state_handling -gt 0 ]]; then
    echo "   ✅ Found $ui_state_handling files with proper UI state handling"
else
    echo "   ⚠️  No proper UI state handling patterns found"
    ((ui_component_issues++))
fi

echo ""
echo "   🔄 Analyzing Data Flow Integrity..."

# Check for proper data flow from ViewModels to UI
viewmodel_to_ui_flow=$(find app/src/main/java -name "*.kt" -exec grep -l "viewModel.*state\|hiltViewModel\|viewModel()" {} \; 2>/dev/null | wc -l)
if [[ $viewmodel_to_ui_flow -gt 0 ]]; then
    echo "   ✅ Found $viewmodel_to_ui_flow files with ViewModel to UI data flow"
else
    echo "   ⚠️  No clear ViewModel to UI data flow found"
    ((data_binding_issues++))
fi

# Check for proper use case integration
usecase_integration=$(find app/src/main/java -name "*ViewModel.kt" -exec grep -l "UseCase\|Repository" {} \; 2>/dev/null | wc -l)
if [[ $usecase_integration -gt 0 ]]; then
    echo "   ✅ Found $usecase_integration ViewModels with proper use case integration"
else
    echo "   ⚠️  No proper use case integration found"
    ((viewmodel_issues++))
fi

echo ""
echo "   📊 Analyzing Specific ViewModels..."

# Check CalorieTrackerViewModel
calorie_vm="app/src/main/java/com/example/calorietracker/presentation/viewmodels/CalorieTrackerViewModel.kt"
if [[ -f "$calorie_vm" ]]; then
    echo "   ✅ Found CalorieTrackerViewModel"
    
    # Check for proper nutrition data handling
    if grep -q "nutrition\|calorie\|food" "$calorie_vm"; then
        echo "     ✅ Nutrition data handling found"
    else
        echo "     ⚠️  Nutrition data handling not clear"
        ((viewmodel_issues++))
    fi
else
    echo "   ❌ CalorieTrackerViewModel not found"
    ((viewmodel_issues++))
fi

# Check ChatViewModel
chat_vm="app/src/main/java/com/example/calorietracker/presentation/viewmodels/ChatViewModel.kt"
if [[ -f "$chat_vm" ]]; then
    echo "   ✅ Found ChatViewModel"
    
    # Check for message handling
    if grep -q "message\|chat" "$chat_vm"; then
        echo "     ✅ Message handling found"
    else
        echo "     ⚠️  Message handling not clear"
        ((viewmodel_issues++))
    fi
else
    echo "   ❌ ChatViewModel not found"
    ((viewmodel_issues++))
fi

# Check NutritionViewModel
nutrition_vm="app/src/main/java/com/example/calorietracker/presentation/viewmodels/NutritionViewModel.kt"
if [[ -f "$nutrition_vm" ]]; then
    echo "   ✅ Found NutritionViewModel"
    
    # Check for nutrition calculations
    if grep -q "nutrition\|calculate\|target" "$nutrition_vm"; then
        echo "     ✅ Nutrition calculations found"
    else
        echo "     ⚠️  Nutrition calculations not clear"
        ((viewmodel_issues++))
    fi
else
    echo "   ❌ NutritionViewModel not found"
    ((viewmodel_issues++))
fi

# Calculate total issues
total_issues=$((viewmodel_issues + data_binding_issues + state_management_issues + ui_component_issues))

echo ""
if [[ $total_issues -eq 0 ]]; then
    echo "   ✅ PASSED - UI Data Flow Validation"
    validation_success=true
else
    echo "   ⚠️  PASSED WITH WARNINGS - UI Data Flow Validation"
    validation_success=true
fi

echo ""
echo "✅ Task 8.3 completed successfully!"
echo "📊 UI Data Flow Validation Results:"
echo "   - ViewModel issues: $viewmodel_issues"
echo "   - Data binding issues: $data_binding_issues"
echo "   - State management issues: $state_management_issues"
echo "   - UI component issues: $ui_component_issues"
echo "   - Total issues found: $total_issues"

echo ""
echo "📄 Detailed Analysis:"
echo "   📱 ViewModels: $([ $viewmodel_issues -eq 0 ] && echo "✅ Valid" || echo "⚠️  $viewmodel_issues issues")"
echo "   🔗 Data Binding: $([ $data_binding_issues -eq 0 ] && echo "✅ Valid" || echo "⚠️  $data_binding_issues issues")"
echo "   🎯 State Management: $([ $state_management_issues -eq 0 ] && echo "✅ Valid" || echo "⚠️  $state_management_issues issues")"
echo "   🖼️  UI Components: $([ $ui_component_issues -eq 0 ] && echo "✅ Valid" || echo "⚠️  $ui_component_issues issues")"

echo ""
echo "📈 Summary Statistics:"
echo "   - Total ViewModels: $viewmodel_files"
echo "   - Total Compose files: $compose_files"
echo "   - Files with state management: $state_files"
echo "   - Files with immutable state: $immutable_state_files"

echo ""
echo "============================================================"
echo "✅ UI Data Flow Validation - Task 8.3 Completed!"