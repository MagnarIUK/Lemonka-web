package com.magnariuk.lemonka.features.admin

import at.favre.lib.crypto.bcrypt.BCrypt
import com.magnariuk.lemonka.data.db.*
import java.security.SecureRandom

/** Rules for admin actions. Every function returns an error message, or null on success. */
object AdminService {
    private val random = SecureRandom()

    suspend fun setRole(actor: User, targetId: Int, role: Role): String? {
        val target = UserRepository.byId(targetId) ?: return "користувача не знайдено"
        if (target.role == role) return null
        if (target.isAdmin && role != Role.Admin && UserRepository.adminCount() <= 1)
            return "не можна забрати права в останнього адміністратора"
        UserRepository.setRole(target.id, role)
        return null
    }

    suspend fun deleteUser(actor: User, targetId: Int): String? {
        val target = UserRepository.byId(targetId) ?: return "користувача не знайдено"
        if (target.id == actor.id) return "свій акаунт можна видалити у профілі"
        if (target.isAdmin && UserRepository.adminCount() <= 1) return "не можна видалити останнього адміністратора"
        UserRepository.delete(target.id)
        return null
    }

    /** Merges [sourceIds] into [targetId]. Returns an error message, or the result. */
    suspend fun merge(actor: User, targetId: Int, sourceIds: List<Int>): Pair<String?, MergeResult?> {
        val target = UserRepository.byId(targetId) ?: return "оберіть акаунт, який залишиться" to null
        val sources = sourceIds.filter { it != target.id }.distinct()
        if (sources.isEmpty()) return "оберіть акаунти, які треба приєднати" to null
        if (actor.id in sources) return "ваш поточний акаунт може бути лише тим, що залишається" to null
        if (sources.any { UserRepository.byId(it) == null }) return "користувача не знайдено" to null
        return null to UserRepository.merge(target.id, sources)
    }

    /** Sets a random temporary password and logs the user out everywhere. Returns the new password. */
    suspend fun resetPassword(targetId: Int): String? {
        val target = UserRepository.byId(targetId) ?: return null
        val alphabet = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val password = (1..12).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
        UserRepository.setPassword(target.id, BCrypt.withDefaults().hashToString(12, password.toCharArray()))
        TokenRepository.deleteAll(target.id)
        return password
    }
}
