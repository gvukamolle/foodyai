package com.example.calorietracker.auth

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
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
    val isEmailVerified: Boolean = false
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

    private fun createUserInFirestore(firebaseUser: FirebaseUser) {
        val newUser = UserData(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = firebaseUser.displayName ?: "Пользователь",
            photoUrl = firebaseUser.photoUrl?.toString(),
            isEmailVerified = firebaseUser.isEmailVerified,
            createdAt = Date(),
            lastLogin = Date()
        )
        firestore.collection("users").document(firebaseUser.uid).set(newUser)
            .addOnSuccessListener {
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
                // Создаем пользователя в Firestore, остальное сделает addAuthStateListener
                createUserInFirestore(user)
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