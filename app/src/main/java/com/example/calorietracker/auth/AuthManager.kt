package com.example.calorietracker.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CompletableJob
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.tasks.await
import android.util.Log

// Модель данных пользователя
data class UserData(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val subscriptionPlan: SubscriptionPlan = SubscriptionPlan.FREE,
    val subscriptionExpiry: Date? = null,
    val createdAt: Date = Date(),
    val lastLogin: Date = Date(),
    val isEmailVerified: Boolean = false,
    val isSetupComplete: Boolean = false, // <-- ДОБАВЬ ЭТО!
    val aiUsageCount: Int = 0,  // Новое поле для отслеживания использования AI
    val aiUsageResetDate: Long = 0 // Дата сброса счетчика
)

class AuthManager(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = Firebase.firestore

    // Поток с текущим пользователем (наши данные UserData)
    private val _currentUser = MutableStateFlow<UserData?>(null)
    val currentUser: StateFlow<UserData?> = _currentUser.asStateFlow()

    // Поток с состоянием аутентификации
    private val _authState = MutableStateFlow(AuthState.LOADING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    enum class AuthState {
        LOADING, AUTHENTICATED, UNAUTHENTICATED
    }

    init {
        // Слушаем изменения состояния аутентификации Firebase
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                // Если пользователь вошел, загружаем его данные из Firestore
                CoroutineScope(Dispatchers.IO).launch {
                    // пока загружаем данные отображаем состояние LOADING
                    _authState.value = AuthState.LOADING
                    loadUserData(firebaseUser)
                }
            } else {
                // Если пользователь вышел
                _currentUser.value = null
                _authState.value = AuthState.UNAUTHENTICATED
            }
        }
    }

    fun updateDisplayName(newName: String): Job {
        val user = auth.currentUser ?: return Job().apply { complete() } // <-- ИСПРАВЛЕНО
        val uid = user.uid

        return CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Обновляем имя в Firebase Authentication
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build()
                user.updateProfile(profileUpdates).await()

                // 2. Обновляем имя в Firestore
                firestore.collection("users").document(uid).update("displayName", newName).await()

                // 3. Обновляем локальное состояние в главном потоке
                withContext(Dispatchers.Main) {
                    _currentUser.value = _currentUser.value?.copy(displayName = newName)
                }
            } catch (e: Exception) {
                // Обработка ошибок, если что-то пошло не так
                e.printStackTrace()
            }
        }
    }

    private suspend fun loadUserData(firebaseUser: FirebaseUser) {
        try {
            Log.d("AuthManager", "Loading user data for UID: ${firebaseUser.uid}")
            
            val document = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            if (document.exists()) {
                // Пользователь существует - загружаем его данные
                _currentUser.value = UserData(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = document.getString("displayName") ?: firebaseUser.displayName ?: "Пользователь",
                    isSetupComplete = document.getBoolean("isSetupComplete") ?: false,
                    subscriptionPlan = try {
                        SubscriptionPlan.valueOf(document.getString("subscriptionPlan") ?: "FREE")
                    } catch (e: Exception) {
                        SubscriptionPlan.FREE
                    },
                    aiUsageCount = document.getLong("aiUsageCount")?.toInt() ?: 0,
                    aiUsageResetDate = document.getLong("aiUsageResetDate") ?: 0
                )
                
                // Обновляем lastLogin
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .update("lastLogin", Date())
                    
            } else {
                // Новый пользователь - создаем запись
                val newUserData = UserData(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "Пользователь",
                    isSetupComplete = false,
                    subscriptionPlan = SubscriptionPlan.FREE,
                    aiUsageCount = 0,
                    aiUsageResetDate = System.currentTimeMillis(),
                    createdAt = Date(),
                    lastLogin = Date()
                )

                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(newUserData)
                    .await()

                _currentUser.value = newUserData
            }
            // После успешной загрузки данных отмечаем пользователя авторизованным
            withContext(Dispatchers.Main) {
                _authState.value = AuthState.AUTHENTICATED
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error loading user data for ${firebaseUser.uid}", e)
            
            // Если это ошибка прав доступа, попробуем создать нового пользователя
            if (e is com.google.firebase.firestore.FirebaseFirestoreException && 
                e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.w("AuthManager", "Permission denied, trying to create new user document")
                
                try {
                    // Попытка создать нового пользователя
                    val newUserData = UserData(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "Пользователь",
                        isSetupComplete = false,
                        subscriptionPlan = SubscriptionPlan.FREE,
                        aiUsageCount = 0,
                        aiUsageResetDate = System.currentTimeMillis(),
                        createdAt = Date(),
                        lastLogin = Date()
                    )
                    
                    firestore.collection("users")
                        .document(firebaseUser.uid)
                        .set(newUserData)
                        .await()
                        
                    _currentUser.value = newUserData
                    withContext(Dispatchers.Main) {
                        _authState.value = AuthState.AUTHENTICATED
                    }
                } catch (createError: Exception) {
                    Log.e("AuthManager", "Failed to create user document", createError)
                    withContext(Dispatchers.Main) {
                        _authState.value = AuthState.UNAUTHENTICATED
                    }
                }
            } else {
                // Другая ошибка - просто выходим
                withContext(Dispatchers.Main) {
                    _authState.value = AuthState.UNAUTHENTICATED
                }
            }
        }
    }

    suspend fun updateSubscriptionPlan(newPlan: SubscriptionPlan): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            firestore.collection("users")
                .document(user.uid)
                .update("subscriptionPlan", newPlan.name)
                .await()

            _currentUser.value = _currentUser.value?.copy(subscriptionPlan = newPlan)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserData(userData: UserData): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))

            val updates = hashMapOf<String, Any>(
                "aiUsageCount" to userData.aiUsageCount,
                "aiUsageResetDate" to userData.aiUsageResetDate
            )

            firestore.collection("users")
                .document(user.uid)
                .update(updates)
                .await()

            _currentUser.value = userData

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserSetupComplete(isComplete: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            firestore.collection("users").document(uid).update("isSetupComplete", isComplete).await()
            // Обновляем локального юзера
            _currentUser.value = _currentUser.value?.copy(isSetupComplete = isComplete)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createUserInFirestore(firebaseUser: FirebaseUser, initialDisplayName: String? = null) {
        val newUser = UserData(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            // Приоритет: имя из формы, потом имя из Firebase, потом "Пользователь"
            displayName = initialDisplayName ?: firebaseUser.displayName ?: "Пользователь",
            photoUrl = firebaseUser.photoUrl?.toString(),
            isEmailVerified = firebaseUser.isEmailVerified,
            isSetupComplete = false, // Для новых пользователей всегда false
            createdAt = Date(),
            lastLogin = Date()
        )
        firestore.collection("users").document(firebaseUser.uid).set(newUser)
            .addOnSuccessListener {
                // Успешно создали, обновляем локальное состояние
                _currentUser.value = newUser
            }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            Result.success(Unit) // Успех, остальное сделает addAuthStateListener
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<Unit> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { user ->
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
                user.sendEmailVerification().await()
                // Создаем запись в Firestore, ПЕРЕДАВАЯ ИМЯ НАПРЯМУЮ
                createUserInFirestore(user, initialDisplayName = displayName)
                Result.success(Unit)
            } ?: Result.failure(Exception("Не удалось создать пользователя"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            Result.success(Unit) // Успех, остальное сделает addAuthStateListener
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        // Очищаем локальные данные при выходе
        _currentUser.value = null
        _authState.value = AuthState.UNAUTHENTICATED
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Пользователь не авторизован"))
        val email = user.email ?: return Result.failure(Exception("Email не найден"))
        return try {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            // Удаляем данные из Firestore
            firestore.collection("users").document(user.uid).delete().await()
            // Удаляем связанные данные (например, историю)
            deleteRelatedUserData(user.uid)
            // Удаляем аккаунт из Firebase Auth
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun deleteRelatedUserData(userId: String) {
        // Здесь можно добавить логику удаления других данных, например:
        // firestore.collection("food_history").whereEqualTo("userId", userId)...
    }

    suspend fun updateSubscription(plan: SubscriptionPlan): Result<Unit> {
        val user = _currentUser.value ?: return Result.failure(Exception("Пользователь не авторизован"))
        return try {
            val expiryDate = when (plan) {
                SubscriptionPlan.FREE -> null
                SubscriptionPlan.PRO -> Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time
            }

            val updates = mapOf(
                "subscriptionPlan" to plan.name,
                "subscriptionExpiry" to expiryDate
            )

            firestore.collection("users").document(user.uid).update(updates).await()

            // Обновляем локальное состояние пользователя
            _currentUser.value = user.copy(
                subscriptionPlan = plan,
                subscriptionExpiry = expiryDate
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isFeatureAvailable(featureKey: String): Boolean {
        val plan = _currentUser.value?.subscriptionPlan ?: return false
        return when (featureKey) {
            "unlimited_ai" -> plan == SubscriptionPlan.PRO
            "export_data" -> plan == SubscriptionPlan.PRO
            "fitness_integration" -> plan == SubscriptionPlan.PRO
            else -> false // Неизвестная фича по умолчанию недоступна
        }
    }
}