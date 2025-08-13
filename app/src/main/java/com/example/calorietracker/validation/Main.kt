package com.example.calorietracker.validation

/**
 * Main entry point for executing comprehensive code validation
 */
fun main(args: Array<String>) {
    val projectPath = if (args.isNotEmpty()) args[0] else "."
    
    println("🔍 Comprehensive Code Validation System")
    println("======================================")
    
    val executor = ValidationExecutor()
    val result = executor.executeComprehensiveValidation(projectPath)
    
    // Exit with appropriate code
    val exitCode = if (result.executionError != null) 1 else 0
    kotlin.system.exitProcess(exitCode)
}