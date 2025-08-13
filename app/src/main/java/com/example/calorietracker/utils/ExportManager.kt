// Создать файл: app/src/main/java/com/example/calorietracker/utils/ExportManager.kt

package com.example.calorietracker.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.calorietracker.presentation.viewmodels.CalorieTrackerViewModel
import com.google.gson.GsonBuilder
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ExportManager {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToCSV(viewModel: CalorieTrackerViewModel, context: Context): File {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "foody_export_${System.currentTimeMillis()}.csv")

        file.bufferedWriter().use { writer ->
            writer.write("Дата,Прием пищи,Продукт,Вес (г),Калории,Белки,Жиры,Углеводы\n")

            // Экспортируем данные за последние 30 дней
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(30)
            var currentDate = startDate

            while (currentDate <= endDate) {
                val dateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val intake = com.example.calorietracker.data.DataRepository(context).getIntakeHistory(dateStr)

                intake?.meals?.forEach { meal ->
                    meal.foods.forEach { food ->
                        writer.write(
                            "$dateStr," +
                                    "${meal.type.displayName}," +
                                    "\"${food.name}\"," +
                                    "${food.weight}," +
                                    "${food.calories}," +
                                    "${NutritionFormatter.formatMacro(food.protein.toFloat())}," +
                                    "${NutritionFormatter.formatMacro(food.fat.toFloat())}," +
                                    "${NutritionFormatter.formatMacro(food.carbs.toFloat())}\n"
                        )
                    }
                }
                currentDate = currentDate.plusDays(1)
            }
        }

        return file
    }

    suspend fun exportToJSON(viewModel: CalorieTrackerViewModel, context: Context): File {
        val exportDir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "foody_export_${System.currentTimeMillis()}.json")

        val exportData = mutableMapOf<String, Any>()
        exportData["export_date"] = LocalDate.now().toString()
        exportData["user_profile"] = viewModel.userProfile

        val dailyData = mutableListOf<Map<String, Any>>()
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(30)
        var currentDate = startDate

        while (currentDate <= endDate) {
            val dateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val intake = com.example.calorietracker.data.DataRepository(context).getIntakeHistory(dateStr)

            intake?.let {
                dailyData.add(mapOf(
                    "date" to dateStr,
                    "calories" to it.calories,
                    "protein" to NutritionFormatter.formatMacro(it.protein),
                    "fat" to NutritionFormatter.formatMacro(it.fat),
                    "carbs" to NutritionFormatter.formatMacro(it.carbs),
                    "meals" to it.meals
                ))
            }
            currentDate = currentDate.plusDays(1)
        }

        exportData["daily_data"] = dailyData
        file.writeText(gson.toJson(exportData))
        return file
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = when (file.extension) {
                "csv" -> "text/csv"
                "json" -> "application/json"
                else -> "*/*"
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Экспорт данных"))
    }
}