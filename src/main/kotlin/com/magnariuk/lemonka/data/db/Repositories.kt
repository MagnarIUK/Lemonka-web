package com.magnariuk.lemonka.data.db

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.security.SecureRandom
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

// One small repository per area. All functions are suspend and run in their own transaction.

object UserRepository {
    suspend fun byId(id: Int): User? = dbQuery {
        Users.selectAll().where { Users.id eq id }.singleOrNull()?.toUser()
    }

    suspend fun byUsername(username: String): User? = dbQuery {
        Users.selectAll().where { Users.username eq username }.singleOrNull()?.toUser()
    }

    suspend fun count(): Long = dbQuery { Users.selectAll().count() }

    suspend fun all(): List<User> = dbQuery { Users.selectAll().orderBy(Users.id).map { it.toUser() } }

    suspend fun create(username: String, passwordHash: String, role: Role = Role.User, invitedBy: Int? = null): Int =
        dbQuery { insertUser(username, passwordHash, role, invitedBy) }

    /**
     * Creates an account in one transaction: the first account ever needs no invite and becomes admin,
     * every other one must consume a usable invite code.
     */
    suspend fun register(username: String, passwordHash: String, inviteCode: String?): RegisterOutcome = dbQuery {
        if (Users.selectAll().where { Users.username eq username }.any()) return@dbQuery RegisterOutcome.UsernameTaken
        if (Users.selectAll().empty()) {
            return@dbQuery RegisterOutcome.Created(insertUser(username, passwordHash, Role.Admin, null))
        }
        val code = InviteRepository.normalize(inviteCode)
        val invite = Invites.selectAll().where { Invites.code eq code }.singleOrNull()?.toInvite()
            ?.takeIf { it.isUsable() } ?: return@dbQuery RegisterOutcome.InvalidInvite
        Invites.update({ Invites.id eq invite.id }) { it[uses] = invite.uses + 1 }
        RegisterOutcome.Created(insertUser(username, passwordHash, Role.User, invite.id))
    }

    suspend fun setPassword(id: Int, passwordHash: String) = dbQuery {
        Users.update({ Users.id eq id }) { it[password] = passwordHash }
    }

    suspend fun adminCount(): Long = dbQuery { Users.selectAll().where { Users.role eq Role.Admin.id }.count() }

    /** Deletes the account and everything it owns. */
    suspend fun delete(id: Int) = dbQuery {
        val projectIds = Projects.selectAll().where { Projects.user eq id }.map { it[Projects.id] }
        val actorIds = Actors.selectAll().where { Actors.user eq id }.map { it[Actors.id] }
        SubtitleFiles.deleteWhere { SubtitleFiles.project inList projectIds }
        val chars = Characters.selectAll().where { Characters.project inList projectIds }.map { it[Characters.id] }
        CharacterImages.deleteWhere { CharacterImages.character inList chars }
        Characters.deleteWhere { Characters.project inList projectIds }
        Characters.update({ Characters.actor inList actorIds }) { it[actor] = null }
        Assignments.deleteWhere { (Assignments.project inList projectIds) or (Assignments.actor inList actorIds) }
        Projects.deleteWhere { Projects.user eq id }
        Actors.deleteWhere { Actors.user eq id }
        Settings.deleteWhere { Settings.user eq id }
        Tokens.deleteWhere { Tokens.user eq id }
        Invites.deleteWhere { Invites.createdBy eq id }
        Users.deleteWhere { Users.id eq id }
    }

    suspend fun overview(): List<UserOverview> = dbQuery {
        val names = Users.selectAll().associate { it[Users.id] to it[Users.username] }
        val inviteOwners = Invites.selectAll().associate { it[Invites.id] to it[Invites.createdBy] }
        val now = nowSeconds()
        Users.selectAll().orderBy(Users.id).map { row ->
            val u = row.toUser()
            UserOverview(
                u,
                inviter = u.invitedBy?.let { inviteOwners[it] }?.let { names[it] },
                projects = Projects.selectAll().where { Projects.user eq u.id }.count(),
                sessions = Tokens.selectAll().where { (Tokens.user eq u.id) and (Tokens.validTo greater now) }.count(),
            )
        }
    }

    /**
     * Moves everything from [sources] into [target] and deletes the source accounts, in one transaction.
     * Projects with a clashing name get " (username)" added; actors with the same name become one actor.
     */
    suspend fun merge(target: Int, sources: List<Int>): MergeResult = dbQuery {
        var projects = 0; var actors = 0; var actorsJoined = 0; var renamed = 0
        val targetRow = Users.selectAll().where { Users.id eq target }.single()
        var makeAdmin = false
        for (src in sources.distinct().filter { it != target }) {
            val srcRow = Users.selectAll().where { Users.id eq src }.singleOrNull() ?: continue
            val srcName = srcRow[Users.username]
            if (srcRow[Users.role] == Role.Admin.id) makeAdmin = true

            // Actors: join by name, otherwise just move.
            val targetActors = Actors.selectAll().where { Actors.user eq target }.associate { it[Actors.actorName] to it[Actors.id] }
            Actors.selectAll().where { Actors.user eq src }.map { it.toActor() }.forEach { a ->
                val same = targetActors[a.name]
                if (same == null) {
                    Actors.update({ Actors.id eq a.id }) { it[user] = target }; actors++
                } else {
                    Characters.update({ Characters.actor eq a.id }) { it[actor] = same }
                    Assignments.update({ Assignments.actor eq a.id }) { it[actor] = same }
                    Actors.deleteWhere { Actors.id eq a.id }; actorsJoined++
                }
            }

            // Projects: move, renaming on a name clash.
            val taken = Projects.selectAll().where { Projects.user eq target }.map { it[Projects.name] }.toMutableSet()
            Projects.selectAll().where { Projects.user eq src }.map { it.toProject() }.forEach { p ->
                var name = p.name
                if (name in taken) {
                    name = "${p.name} ($srcName)"
                    var n = 2
                    while (name in taken) { name = "${p.name} ($srcName $n)"; n++ }
                    renamed++
                }
                taken += name
                Projects.update({ Projects.id eq p.id }) { it[user] = target; it[Projects.name] = name }
                projects++
            }

            Invites.update({ Invites.createdBy eq src }) { it[createdBy] = target }
            Settings.deleteWhere { Settings.user eq src }
            Tokens.deleteWhere { Tokens.user eq src }
            Users.deleteWhere { Users.id eq src }
        }

        // A joined actor may now be assigned to the same project twice - keep one.
        val actorIds = Actors.selectAll().where { Actors.user eq target }.map { it[Actors.id] }
        Assignments.selectAll().where { Assignments.actor inList actorIds }.map { it.toAssignment() }
            .groupBy { it.actor to it.project }.values.forEach { dupes ->
                dupes.drop(1).forEach { d -> Assignments.deleteWhere { Assignments.id eq d.id } }
            }

        if (makeAdmin && targetRow[Users.role] != Role.Admin.id) {
            Users.update({ Users.id eq target }) { it[role] = Role.Admin.id }
        }
        MergeResult(projects, actors, actorsJoined, renamed)
    }

    suspend fun setRole(id: Int, role: Role) = dbQuery {
        Users.update({ Users.id eq id }) { it[Users.role] = role.id }
    }

    private fun insertUser(username: String, passwordHash: String, role: Role, invitedBy: Int?) = Users.insert {
        it[Users.username] = username
        it[password] = passwordHash
        it[Users.role] = role.id
        it[createdAt] = nowSeconds()
        it[Users.invitedBy] = invitedBy
    }[Users.id]
}

data class MergeResult(val projects: Int, val actorsMoved: Int, val actorsJoined: Int, val projectsRenamed: Int)

/** One row of the admin users table. */
data class UserOverview(val user: User, val inviter: String?, val projects: Long, val sessions: Long)

sealed interface RegisterOutcome {
    data class Created(val userId: Int) : RegisterOutcome
    data object UsernameTaken : RegisterOutcome
    data object InvalidInvite : RegisterOutcome
}

object InviteRepository {
    // No 0/O/1/I so codes are easy to read out and type.
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun generateCode() = (1..8).map { ALPHABET[random.nextInt(ALPHABET.length)] }
        .joinToString("").chunked(4).joinToString("-")

    /** Accepts codes typed in lower case, with spaces, or without the dash. */
    fun normalize(code: String?): String {
        val raw = code.orEmpty().uppercase().filter { it.isLetterOrDigit() }
        return if (raw.length == 8) raw.chunked(4).joinToString("-") else raw
    }

    suspend fun create(createdBy: Int, maxUses: Int = 1, validFor: Duration? = 7.days, note: String? = null): Invite =
        dbQuery {
            val now = nowSeconds()
            Invites.insert {
                it[code] = generateCode()
                it[Invites.createdBy] = createdBy
                it[createdAt] = now
                it[expiresAt] = validFor?.let { d -> now + d.inWholeSeconds }
                it[Invites.maxUses] = maxUses.coerceAtLeast(1)
                it[Invites.note] = note?.takeIf { n -> n.isNotBlank() }?.take(200)
            }.resultedValues!!.single().toInvite()
        }

    suspend fun byId(id: Int): Invite? = dbQuery {
        Invites.selectAll().where { Invites.id eq id }.singleOrNull()?.toInvite()
    }

    suspend fun byCode(code: String): Invite? = dbQuery {
        Invites.selectAll().where { Invites.code eq normalize(code) }.singleOrNull()?.toInvite()
    }

    suspend fun all(): List<Invite> = dbQuery {
        Invites.selectAll().orderBy(Invites.id, SortOrder.DESC).map { it.toInvite() }
    }

    /** Usernames of everyone who registered with each invite, keyed by invite id. */
    suspend fun usedBy(): Map<Int, List<String>> = dbQuery {
        Users.selectAll().where { Users.invitedBy.isNotNull() }
            .groupBy({ it[Users.invitedBy]!! }, { it[Users.username] })
    }

    suspend fun revoke(id: Int) = dbQuery { Invites.update({ Invites.id eq id }) { it[revoked] = true } }

    suspend fun delete(id: Int) = dbQuery { Invites.deleteWhere { Invites.id eq id } }
}

object TokenRepository {
    suspend fun deleteAll(user: Int) = dbQuery { Tokens.deleteWhere { Tokens.user eq user } }

    val lifetime: Duration = 30.days
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private val random = SecureRandom()

    private fun generate(length: Int = 50) = buildString(length) {
        repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    suspend fun create(user: Int): Token = dbQuery {
        Tokens.insert {
            it[Tokens.user] = user
            it[token] = generate()
            it[validTo] = System.currentTimeMillis() / 1000 + lifetime.inWholeSeconds
        }.resultedValues!!.single().toToken()
    }

    /** Returns the token only if it exists and has not expired. */
    suspend fun findValid(token: String): Token? = dbQuery {
        Tokens.selectAll().where { Tokens.token eq token }.singleOrNull()?.toToken()
    }?.takeIf { it.validTo > System.currentTimeMillis() / 1000 }

    suspend fun delete(token: String) = dbQuery {
        Tokens.deleteWhere { Tokens.token eq token }
    }

    /** Active sessions of a user (each login on a device is one). */
    suspend fun activeCount(user: Int): Long = dbQuery {
        Tokens.selectAll().where { (Tokens.user eq user) and (Tokens.validTo greater nowSeconds()) }.count()
    }

    /** Logs the user out everywhere except the current session. */
    suspend fun deleteOthers(user: Int, keep: String) = dbQuery {
        Tokens.deleteWhere { (Tokens.user eq user) and (Tokens.token neq keep) }
    }
}

object SettingsRepository {
    /** Gets the user's settings, creating defaults on first access. */
    suspend fun get(user: Int): Setting = dbQuery {
        Settings.selectAll().where { Settings.user eq user }.firstOrNull()?.toSetting()
            ?: Settings.insert {
                it[Settings.user] = user
                it[hideSelected] = true
            }.resultedValues!!.single().toSetting()
    }

    suspend fun setHideSelected(user: Int, value: Boolean) = dbQuery {
        Settings.update({ Settings.user eq user }) { it[hideSelected] = value }
    }
}

object SeparatorRepository {
    suspend fun all(): List<String> = dbQuery {
        Separators.selectAll().orderBy(Separators.id).map { it[Separators.separator] }
    }

    suspend fun list(): List<Separator> = dbQuery {
        Separators.selectAll().orderBy(Separators.id).map { it.toSeparator() }
    }

    /** Returns false if the separator already exists. */
    suspend fun add(value: String): Boolean = dbQuery {
        if (Separators.selectAll().where { Separators.separator eq value }.any()) return@dbQuery false
        Separators.insert { it[separator] = value }
        true
    }

    suspend fun delete(id: Int) = dbQuery { Separators.deleteWhere { Separators.id eq id } }
}

object ProjectRepository {
    suspend fun create(name: String, user: Int): Project = dbQuery {
        Projects.insert {
            it[Projects.name] = name
            it[Projects.user] = user
        }.resultedValues!!.single().toProject()
    }

    suspend fun byId(id: Int): Project? = dbQuery {
        Projects.selectAll().where { Projects.id eq id }.singleOrNull()?.toProject()
    }

    /** Scoped to the user: two users can now have projects with the same name. */
    suspend fun byName(name: String, user: Int): Project? = dbQuery {
        Projects.selectAll().where { (Projects.name eq name) and (Projects.user eq user) }.firstOrNull()?.toProject()
    }

    suspend fun forUser(user: Int): List<Project> = dbQuery {
        Projects.selectAll().where { Projects.user eq user }.map { it.toProject() }
    }

    /** Projects with how many characters and actors each has, for the profile page. */
    suspend fun overview(user: Int): List<ProjectOverview> = dbQuery {
        Projects.selectAll().where { Projects.user eq user }.orderBy(Projects.id).map { row ->
            val p = row.toProject()
            ProjectOverview(
                p,
                characters = Characters.selectAll().where { Characters.project eq p.id }.count(),
                actors = Assignments.selectAll().where { Assignments.project eq p.id }.count(),
            )
        }
    }

    suspend fun byShareToken(token: String): Project? = dbQuery {
        Projects.selectAll().where { Projects.shareToken eq token }.singleOrNull()?.toProject()
    }

    /** Turns the public link on with a fresh token (an old link stops working), or off with null. */
    suspend fun setShareToken(id: Int, token: String?) = dbQuery {
        Projects.update({ Projects.id eq id }) { it[shareToken] = token }
    }

    suspend fun rename(id: Int, name: String) = dbQuery {
        Projects.update({ Projects.id eq id }) { it[Projects.name] = name }
    }

    /** Deletes the project with its characters, actor assignments and stored subtitle file. */
    suspend fun delete(id: Int) = dbQuery {
        SubtitleFiles.deleteWhere { SubtitleFiles.project eq id }
        val chars = Characters.selectAll().where { Characters.project eq id }.map { it[Characters.id] }
        CharacterImages.deleteWhere { CharacterImages.character inList chars }
        Characters.deleteWhere { Characters.project eq id }
        Assignments.deleteWhere { Assignments.project eq id }
        Projects.deleteWhere { Projects.id eq id }
    }
}

data class ProjectOverview(val project: Project, val characters: Long, val actors: Long)

object ActorRepository {
    suspend fun add(name: String, user: Int) = dbQuery {
        Actors.insert {
            it[actorName] = name
            it[Actors.user] = user
        }
    }

    /** Deletes the actor, its project assignments, and unassigns its characters. */
    suspend fun remove(actor: Int) = dbQuery {
        Characters.update({ Characters.actor eq actor }) { it[Characters.actor] = null }
        Assignments.deleteWhere { Assignments.actor eq actor }
        Actors.deleteWhere { Actors.id eq actor }
    }

    suspend fun byId(id: Int): Actor? = dbQuery {
        Actors.selectAll().where { Actors.id eq id }.singleOrNull()?.toActor()
    }

    suspend fun forUser(user: Int): List<Actor> = dbQuery {
        Actors.selectAll().where { Actors.user eq user }.map { it.toActor() }
    }

    /** Actors with the number of projects each one is assigned to. */
    suspend fun overview(user: Int): List<Pair<Actor, Long>> = dbQuery {
        Actors.selectAll().where { Actors.user eq user }.orderBy(Actors.actorName).map { row ->
            val a = row.toActor()
            a to Assignments.selectAll().where { Assignments.actor eq a.id }.count()
        }
    }

    /** Scoped to the user: two users can now have actors with the same name. */
    suspend fun byName(name: String, user: Int): Actor? = dbQuery {
        Actors.selectAll().where { (Actors.actorName eq name) and (Actors.user eq user) }.firstOrNull()?.toActor()
    }
}

object AssignmentRepository {
    suspend fun add(actor: Int, project: Int) = dbQuery {
        Assignments.insert {
            it[Assignments.actor] = actor
            it[Assignments.project] = project
        }
    }

    /** Removes the actor from the project and unassigns their characters in it. */
    suspend fun remove(id: Int) = dbQuery {
        val a = Assignments.selectAll().where { Assignments.id eq id }.singleOrNull()?.toAssignment()
            ?: return@dbQuery
        Characters.update({ (Characters.actor eq a.actor) and (Characters.project eq a.project) }) {
            it[actor] = null
        }
        Assignments.deleteWhere { Assignments.id eq id }
    }

    suspend fun forProject(project: Int): List<Assignment> = dbQuery {
        Assignments.selectAll().where { Assignments.project eq project }.map { it.toAssignment() }
    }

    suspend fun find(project: Int, actor: Int): Assignment? = dbQuery {
        Assignments.selectAll().where { (Assignments.actor eq actor) and (Assignments.project eq project) }
            .firstOrNull()?.toAssignment()
    }
}

object CharacterRepository {
    suspend fun add(name: String, project: Int) = dbQuery {
        Characters.insert {
            it[Characters.name] = name
            it[Characters.project] = project
        }
    }

    /** Adds every name that isn't in the project yet. */
    suspend fun addMissing(names: Collection<String>, project: Int) = dbQuery {
        val existing = Characters.selectAll().where { Characters.project eq project }
            .map { it[Characters.name] }.toSet()
        names.filter { it !in existing }.distinct().forEach { n ->
            Characters.insert {
                it[name] = n
                it[Characters.project] = project
            }
        }
    }

    suspend fun byId(id: Int): Character? = dbQuery {
        Characters.selectAll().where { Characters.id eq id }.singleOrNull()?.toCharacter()
    }

    suspend fun delete(id: Int) = dbQuery {
        CharacterImages.deleteWhere { CharacterImages.character eq id }
        Characters.deleteWhere { Characters.id eq id }
    }

    suspend fun assign(character: Int, actor: Int?) = dbQuery {
        Characters.update({ Characters.id eq character }) { it[Characters.actor] = actor }
    }

    suspend fun forProject(project: Int): List<Character> = dbQuery {
        Characters.selectAll().where { Characters.project eq project }.map { it.toCharacter() }
    }

    suspend fun byName(name: String, project: Int): Character? = dbQuery {
        Characters.selectAll().where { (Characters.name eq name) and (Characters.project eq project) }
            .firstOrNull()?.toCharacter()
    }

    suspend fun forActor(actor: Int, project: Int): List<Character> = dbQuery {
        Characters.selectAll().where { (Characters.actor eq actor) and (Characters.project eq project) }
            .map { it.toCharacter() }
    }
}

data class SubtitleFile(val project: Int, val name: String, val content: String, val uploadedAt: Long)

object SubtitleFileRepository {
    /** Stores (or replaces) the project's working .ass file. */
    suspend fun save(project: Int, name: String, content: String) = dbQuery {
        SubtitleFiles.deleteWhere { SubtitleFiles.project eq project }
        SubtitleFiles.insert {
            it[SubtitleFiles.project] = project
            it[SubtitleFiles.name] = name
            it[SubtitleFiles.content] = content
            it[uploadedAt] = nowSeconds()
        }
    }

    suspend fun get(project: Int): SubtitleFile? = dbQuery {
        SubtitleFiles.selectAll().where { SubtitleFiles.project eq project }.singleOrNull()?.let {
            SubtitleFile(it[SubtitleFiles.project], it[SubtitleFiles.name], it[SubtitleFiles.content], it[SubtitleFiles.uploadedAt])
        }
    }

    /** File names per project id (without loading contents). */
    suspend fun names(projects: Collection<Int>): Map<Int, String> = dbQuery {
        SubtitleFiles.select(SubtitleFiles.project, SubtitleFiles.name)
            .where { SubtitleFiles.project inList projects }
            .associate { it[SubtitleFiles.project] to it[SubtitleFiles.name] }
    }
}

data class CharacterImage(val character: Int, val contentType: String, val bytes: ByteArray, val updatedAt: Long)

object CharacterImageRepository {
    suspend fun save(character: Int, contentType: String, bytes: ByteArray) = dbQuery {
        CharacterImages.deleteWhere { CharacterImages.character eq character }
        CharacterImages.insert {
            it[CharacterImages.character] = character
            it[CharacterImages.contentType] = contentType
            it[data] = ExposedBlob(bytes)
            it[updatedAt] = nowSeconds()
        }
    }

    suspend fun get(character: Int): CharacterImage? = dbQuery {
        CharacterImages.selectAll().where { CharacterImages.character eq character }.singleOrNull()?.let {
            CharacterImage(it[CharacterImages.character], it[CharacterImages.contentType], it[CharacterImages.data].bytes, it[CharacterImages.updatedAt])
        }
    }

    suspend fun delete(character: Int) = dbQuery { CharacterImages.deleteWhere { CharacterImages.character eq character } }

    /** character id → last update time (used as a cache-busting version in image URLs). */
    suspend fun versions(characters: Collection<Int>): Map<Int, Long> = dbQuery {
        CharacterImages.select(CharacterImages.character, CharacterImages.updatedAt)
            .where { CharacterImages.character inList characters }
            .associate { it[CharacterImages.character] to it[CharacterImages.updatedAt] }
    }
}
