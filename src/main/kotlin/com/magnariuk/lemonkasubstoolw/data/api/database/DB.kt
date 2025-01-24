package com.magnariuk.lemonkasubstoolw.data.api.database


import com.google.gson.Gson
import com.magnariuk.lemonkasubstoolw.data.Classes.Cache
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.FileReader
import java.io.IOException

open class DatabaseController{

    fun databaseExists(dbPath: String): Boolean {
        val file = File(dbPath)
        return file.exists() && !file.isDirectory
    }

    fun init() {
        val dbPath = System.getenv("DB_PATH")?: "cache.db"
        val dbExists = databaseExists(dbPath)
        try {
            Database.connect("jdbc:sqlite:$dbPath")
            transaction {
                addLogger(StdOutSqlLogger)
                SchemaUtils.create(Projects, Actors, Assignments, Characters, Separators, Settings)
            }
        } catch (e: Exception) {
            println("Помилка підключення до бази даних: ${e.message}")
        }

        when {
            dbExists -> {
                println("База даних існує і підключена")
            }
            !dbExists -> {
                println("База даних створена і таблиці ініціалізовані")
            }
        }
    }

    fun <T> dbQuery(block: () -> T): T {
        return transaction {
            addLogger(StdOutSqlLogger)
            block()
        }
    }

}

open class DB(private val dbController: DatabaseController) {

    fun getUser(id: Int): User {
        return dbController.dbQuery {
            Users.selectAll().where { Users.id eq id }.map { Format().toUser(it) }.first()
        }
    }
    fun getUser(username: String): User? {
        return dbController.dbQuery {
            Users.selectAll().where { Users.username eq username }.map { Format().toUser(it) }.firstOrNull()
        }
    }

    fun createUser(username: String, password: String): Int {
        return dbController.dbQuery {
            Users.insert {
                it[Users.username] = username
                it[Users.password] = password
            } get Users.id
        }
    }


    fun getSettings(): Setting {
        return dbController.dbQuery {
            Settings.selectAll().where(Settings.id eq 1).first().let { Format().toSetting(it) }
        }
    }

    fun getSeparators(): List<Separator> {
        return dbController.dbQuery {
            Separators.selectAll().map { Format().toSeparator(it) }
        }
    }

    fun createProject(name: String, user: User): Int {
        return dbController.dbQuery {
            Projects.insert {
                it[Projects.name] = name
                it[Projects.user] = user.id
            } get Projects.id
        }
    }
    fun getProject(id: Int): Project {
        return dbController.dbQuery {
            Projects.selectAll().where { Projects.id eq id }.map { Format().toProject(it) }.first()
        }
    }
    fun getProjectByName(name: String): Project? {
        return dbController.dbQuery {
            Projects.selectAll().where { Projects.name eq name }.map { Format().toProject(it) }.firstOrNull()
        }
    }

    fun getProjectsByUser(user: User): List<Project> {
        return dbController.dbQuery {
            Projects.selectAll().where { Projects.user eq user.id }.map { Format().toProject(it) }
        }
    }


    fun getProjects(user: User): List<Project> {
        return dbController.dbQuery {
            Projects.selectAll().where { Projects.user eq user.id }.map { Format().toProject(it) }
        }
    }

    fun addActor(actorName: String, user: User) {
        dbController.dbQuery {
            Actors.insert {
                it[Actors.actorName] = actorName
                it[Actors.user] = user.id
            }
        }
    }

    fun removeActor(actor: Int) {
        dbController.dbQuery {
            Actors.deleteWhere { Actors.id eq actor }
            Assignments.deleteWhere { Assignments.actor eq actor }
            Characters.update({ Characters.actor eq actor }) {
                it[Characters.actor] = null
            }
        }
    }

    fun getActor(id: Int): Actor {
        return dbController.dbQuery {
            Actors.selectAll().where { Actors.id eq id }.map { Format().toActor(it) }.first()
        }
    }

    fun getActors(user: User): List<Actor> {
        return dbController.dbQuery {
            Actors.selectAll().where { Actors.user eq user.id }.map { Format().toActor(it) }
        }
    }
    fun getActorByName(name: String): Actor? {
        return dbController.dbQuery {
            Actors.selectAll().where { Actors.actorName eq name }.map { Format().toActor(it) }.firstOrNull()
        }
    }

    fun addAssignment(actor: Int, project: Int) {
        dbController.dbQuery {
            Assignments.insert {
                it[Assignments.actor] = actor
                it[Assignments.project] = project
            }
        }
    }

    fun removeAssignment(id: Int) {
        dbController.dbQuery {
            val asi = Assignments.selectAll().where { Assignments.id eq id }.first().let { Format().toAssignment(it) }
            Characters.update({ Characters.actor eq asi.actor and (Characters.project eq asi.project) }) {
                it[actor] = null
            }
            Assignments.deleteWhere { Assignments.id eq id }
        }
    }

    fun getProjectAssignments(project: Int): List<Assignment> {
        return dbController.dbQuery {
            Assignments.selectAll().where(Assignments.project eq project).map { Format().toAssignment(it) }
        }
    }
    fun getProjectAssignmentsByActor(project: Int, actor: Int): Assignment? {
        return dbController.dbQuery {
            Assignments.selectAll().where((Assignments.actor eq actor) and(Assignments.project eq project)).firstOrNull()?.let { Format().toAssignment(it) }
        }
    }

    fun assignCharacter(actor: Int?, character: Int) {
        dbController.dbQuery {
            Characters.update({ Characters.id eq character }) {
                it[Characters.actor] = actor
            }
        }
    }

    fun addCharacter(name: String, project: Int) {
        dbController.dbQuery {
            Characters.insert {
                it[Characters.name] = name
                it[Characters.project] = project
            }
        }
    }

    fun getProjectCharacters(project: Int): List<Character> {
        return dbController.dbQuery {
            Characters.selectAll().where(Characters.project eq project).map { Format().toCharacter(it) }
        }
    }

    fun getCharacterByName(name: String, project: Int): Character? {
        return dbController.dbQuery {
            Characters.selectAll().where { Characters.name eq name and(Characters.project eq project) }.map { Format().toCharacter(it) }.firstOrNull()
        }
    }

    fun getCharactersByActor(actor: Int, project: Int): List<Character> {
        return dbController.dbQuery {
            Characters.selectAll().where { Characters.actor eq actor and(Characters.project eq project) }.map { Format().toCharacter(it) }
        }
    }



}

open class Format {
    fun toProject(row: ResultRow): Project {
        return Project(
            id = row[Projects.id],
            name = row[Projects.name],
            user = row[Projects.user]
        )
    }
    fun toUser(row: ResultRow): User {
        val gson = Gson()
        try {
            val ud = gson.fromJson(row[Users.userData], UserData::class.java)
            return User(
                id = row[Users.id],
                username = row[Users.username],
                password = row[Users.password],
                userData = ud
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return User(
            id = row[Users.id],
            username = row[Users.username],
            password = row[Users.password],
            userData = null
        )
    }

    fun toActor(row: ResultRow): Actor {
        return Actor(
            id = row[Actors.id],
            actorName = row[Actors.actorName],
            user = row[Actors.user]
        )
    }

    fun toAssignment(row: ResultRow): Assignment {
        return Assignment(
            id = row[Assignments.id],
            actor = row[Assignments.actor],
            project = row[Assignments.project]
        )
    }
    fun toCharacter(row: ResultRow): Character {
        return Character(
            id = row[Characters.id],
            name = row[Characters.name],
            project = row[Characters.project],
            actor = row[Characters.actor]
        )
    }
    fun toSeparator(row: ResultRow): Separator {
        return Separator(
            id = row[Separators.id],
            separator = row[Separators.separator]
        )
    }
    fun toSetting(row: ResultRow): Setting {
        return Setting(
            id = row[Settings.id],
            hideSelected = row[Settings.hideSelected]
        )
    }

}
