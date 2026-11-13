package com.magnariuk.lemonka.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.magnariuk.lemonka.auth.AuthResult.Field
import com.magnariuk.lemonka.data.db.*

/** Result of a login or registration attempt. Error messages are the ones shown in the UI. */
sealed interface AuthResult {
    data class Success(val user: User, val token: Token) : AuthResult
    sealed class Failure(val field: Field, val message: String) : AuthResult
    data object UserNotFound : Failure(Field.Username, "користувача не знайдено")
    data object WrongPassword : Failure(Field.Password, "невірний пароль")
    data object UsernameTaken : Failure(Field.Username, "користувач з таким ім'ям вже існує")
    data object UsernameTooShort : Failure(Field.Username, "ім'я користувача занадто коротке")
    data object PasswordTooShort : Failure(Field.Password, "пароль занадто короткий")
    data object InvalidInvite : Failure(Field.Invite, "код запрошення недійсний або вже використаний")

    enum class Field { Username, Password, Invite, NewPassword, Repeat }
}

object AuthService {
    const val MIN_USERNAME = 4
    const val MIN_PASSWORD = 8
    private const val BCRYPT_COST = 12

    suspend fun login(username: String, password: String): AuthResult {
        val user = UserRepository.byUsername(username.trim()) ?: return AuthResult.UserNotFound
        // Also verifies the $2a$ hashes made by Spring's BCryptPasswordEncoder, so old accounts keep working.
        val ok = BCrypt.verifyer().verify(password.trim().toCharArray(), user.passwordHash).verified
        if (!ok) return AuthResult.WrongPassword
        return AuthResult.Success(user, TokenRepository.create(user.id))
    }

    /** The very first account needs no invite (and becomes admin); everyone after that does. */
    suspend fun inviteRequired(): Boolean = UserRepository.count() > 0

    suspend fun register(username: String, password: String, invite: String?): AuthResult {
        val name = username.trim()
        val pass = password.trim()
        when {
            name.length < MIN_USERNAME -> return AuthResult.UsernameTooShort
            pass.length < MIN_PASSWORD -> return AuthResult.PasswordTooShort
        }
        val hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, pass.toCharArray())
        return when (UserRepository.register(name, hash, invite)) {
            RegisterOutcome.UsernameTaken -> AuthResult.UsernameTaken
            RegisterOutcome.InvalidInvite -> AuthResult.InvalidInvite
            is RegisterOutcome.Created -> login(name, pass)
        }
    }

    fun checkPassword(user: User, password: String) =
        BCrypt.verifyer().verify(password.trim().toCharArray(), user.passwordHash).verified

    /** Returns an error message, or null when the password was changed. */
    suspend fun changePassword(user: User, current: String, new: String, repeat: String): Pair<Field, String>? = when {
        !checkPassword(user, current) -> Field.Password to "невірний пароль"
        new.trim().length < MIN_PASSWORD -> Field.NewPassword to "пароль занадто короткий"
        new.trim() != repeat.trim() -> Field.Repeat to "паролі не збігаються"
        else -> {
            UserRepository.setPassword(user.id, BCrypt.withDefaults().hashToString(BCRYPT_COST, new.trim().toCharArray()))
            null
        }
    }

    /** Deletes the user's account. Refuses to delete the last admin, so the app always has one. */
    suspend fun deleteAccount(user: User, password: String): String? = when {
        !checkPassword(user, password) -> "невірний пароль"
        user.isAdmin && UserRepository.adminCount() <= 1 -> "ви єдиний адміністратор - спершу призначте іншого"
        else -> { UserRepository.delete(user.id); null }
    }

    /** Resolves a session token to its user, or null if the token is unknown or expired. */
    suspend fun userFor(token: String): User? =
        TokenRepository.findValid(token)?.let { UserRepository.byId(it.user) }

    suspend fun logout(token: String) = TokenRepository.delete(token)
}
