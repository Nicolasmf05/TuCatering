package com.example.catering.data.repo.firebase

import com.example.catering.data.model.Role
import com.example.catering.data.model.User
import com.example.catering.data.repo.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val adminState = MutableStateFlow<User?>(null)

    private val firebaseUserFlow: Flow<User?> = callbackFlow {
        var registration: ListenerRegistration? = null
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            registration?.remove()
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                trySend(null)
                return@AuthStateListener
            }
            val docRef = firestore.collection(USERS_COLLECTION).document(firebaseUser.uid)
            registration = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(snapshot.toUser(firebaseUser.email))
                } else {
                    val now = System.currentTimeMillis()
                    val newUser = User(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email.orEmpty(),
                        displayName = firebaseUser.displayName.orEmpty(),
                        role = Role.CLIENT,
                        affiliation = null,
                        address = "",
                        profileDescription = "",
                        joinedAt = now
                    )
                    docRef.set(newUser.toFirestoreMap(), SetOptions.merge())
                    trySend(newUser)
                }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            registration?.remove()
            auth.removeAuthStateListener(listener)
        }
    }.distinctUntilChanged()

    override val currentUser: Flow<User?> = combine(adminState, firebaseUserFlow) { adminUser, firebaseUser ->
        adminUser ?: firebaseUser
    }.distinctUntilChanged()

    override suspend fun login(email: String, password: String): Result<User> {
        if (email.equals(ADMIN_USERNAME, ignoreCase = true) && password == ADMIN_PASSWORD) {
            val adminUser = ADMIN_USER
            adminState.value = adminUser
            return Result.success(adminUser)
        }
        adminState.value = null
        return runCatching {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: error("No se pudo iniciar sesión.")
            val doc = firestore.collection(USERS_COLLECTION).document(firebaseUser.uid).get().await()
            if (doc.exists()) {
                doc.toUser(firebaseUser.email)
            } else {
                val fallback = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email.orEmpty(),
                    displayName = firebaseUser.displayName.orEmpty(),
                    role = Role.CLIENT,
                    affiliation = null,
                    address = "",
                    profileDescription = "",
                    joinedAt = System.currentTimeMillis()
                )
                firestore.collection(USERS_COLLECTION).document(firebaseUser.uid)
                    .set(fallback.toFirestoreMap(), SetOptions.merge()).await()
                fallback
            }
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        role: Role,
        displayName: String,
        affiliation: String?,
        address: String
    ): Result<User> = runCatching {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: error("No se pudo crear la cuenta.")
        val profileUpdate = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()
        firebaseUser.updateProfile(profileUpdate).await()
        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email.orEmpty(),
            displayName = displayName,
            role = role,
            affiliation = affiliation,
            address = address,
            profileDescription = "",
            joinedAt = System.currentTimeMillis()
        )
        firestore.collection(USERS_COLLECTION).document(firebaseUser.uid)
            .set(user.toFirestoreMap())
            .await()
        user
    }

    override suspend fun logout() {
        if (adminState.value?.role == Role.ADMIN) {
            adminState.value = null
            return
        }
        auth.signOut()
    }

    private fun User.toFirestoreMap(): Map<String, Any?> = mapOf(
        "email" to email,
        "displayName" to displayName,
        "role" to role.name,
        "affiliation" to affiliation,
        "address" to address,
        "profileDescription" to profileDescription,
        "joinedAt" to joinedAt,
        "averageRating" to 0.0,
        "reviewCount" to 0,
        "recentReviews" to emptyList<Map<String, Any?>>(),
        "previousWorks" to emptyList<Map<String, Any?>>()
    )

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val ADMIN_USERNAME = "admin"
        private const val ADMIN_PASSWORD = "admin"
        private const val ADMIN_UID = "admin-local"
        private val ADMIN_USER = User(
            uid = ADMIN_UID,
            email = ADMIN_USERNAME,
            displayName = "Administrador",
            role = Role.ADMIN,
            affiliation = null,
            address = "",
            profileDescription = "",
            joinedAt = System.currentTimeMillis()
        )
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toUser(
    fallbackEmail: String?
): User {
    val roleName = getString("role") ?: Role.CLIENT.name
    val role = runCatching { Role.valueOf(roleName) }.getOrDefault(Role.CLIENT)
    return User(
        uid = id,
        email = getString("email") ?: fallbackEmail.orEmpty(),
        displayName = getString("displayName") ?: getString("email").orEmpty(),
        role = role,
        affiliation = getString("affiliation"),
        address = getString("address") ?: "",
        profileDescription = getString("profileDescription") ?: "",
        joinedAt = getJoinedAtTimestamp()
    )
}

private fun com.google.firebase.firestore.DocumentSnapshot.getJoinedAtTimestamp(): Long {
    val raw = get("joinedAt")
    return when (raw) {
        is Number -> raw.toLong()
        is com.google.firebase.Timestamp -> raw.toDate().time
        else -> 0L
    }
}
