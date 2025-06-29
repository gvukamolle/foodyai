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
    val isSetupComplete: Boolean = false // <-- ДОБАВЬ ЭТО!
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
                loadUserData(firebaseUser)
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

    private fun loadUserData(firebaseUser: FirebaseUser) {
        firestore.collection("users").document(firebaseUser.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Пользователь найден в Firestore, загружаем его данные
                    val userData = document.toObject(UserData::class.java)
                    _currentUser.value = userData
                } else {
                    // Пользователя нет в Firestore, создаем новую запись
                    createUserInFirestore(firebaseUser)
                }
                _authState.value = AuthState.AUTHENTICATED
            }
            .addOnFailureListener {
                // Ошибка загрузки данных
                _authState.value = AuthState.UNAUTHENTICATED
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
                SubscriptionPlan.PRO -> Calendar.getInstance().apply { add(Calendar.MONTH, 1) }.time
                SubscriptionPlan.PREMIUM -> Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.time
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
            "unlimited_ai" -> plan == SubscriptionPlan.PRO || plan == SubscriptionPlan.PREMIUM
            "export_data" -> plan == SubscriptionPlan.PRO || plan == SubscriptionPlan.PREMIUM
            "fitness_integration" -> plan == SubscriptionPlan.PREMIUM
            else -> false // Неизвестная фича по умолчанию недоступна
        }
    }
}