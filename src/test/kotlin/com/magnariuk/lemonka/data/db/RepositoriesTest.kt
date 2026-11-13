package com.magnariuk.lemonka.data.db

import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.*

class RepositoriesTest {
    @BeforeTest
    fun setUp() {
        val file = Files.createTempFile("lemonka-test", ".db").toFile().apply { deleteOnExit() }
        DatabaseFactory.init(file.absolutePath)
    }

    @Test
    fun fullFlow() = runBlocking {
        // Users table exists on a fresh DB (was the old bug)
        val uid = UserRepository.create("magnar", "hash")
        assertEquals("magnar", UserRepository.byUsername("magnar")?.username)

        // Default separators seeded, settings default created
        assertEquals(listOf(";", ",", "and", "та"), SeparatorRepository.all())
        assertTrue(SettingsRepository.get(uid).hideSelected)
        SettingsRepository.setHideSelected(uid, false)
        assertFalse(SettingsRepository.get(uid).hideSelected)

        // Tokens
        val token = TokenRepository.create(uid)
        assertEquals(50, token.token.length)
        assertEquals(uid, TokenRepository.findValid(token.token)?.user)
        TokenRepository.delete(token.token)
        assertNull(TokenRepository.findValid(token.token))

        // Projects / actors / characters / assignments
        val project = ProjectRepository.create("Show", uid)
        ActorRepository.add("Ivan", uid)
        val actor = ActorRepository.byName("Ivan", uid)!!
        CharacterRepository.addMissing(listOf("A", "B", "A"), project.id)
        CharacterRepository.addMissing(listOf("B", "C"), project.id)
        assertEquals(listOf("A", "B", "C"), CharacterRepository.forProject(project.id).map { it.name })

        AssignmentRepository.add(actor.id, project.id)
        val charA = CharacterRepository.byName("A", project.id)!!
        CharacterRepository.assign(charA.id, actor.id)
        assertEquals(listOf("A"), CharacterRepository.forActor(actor.id, project.id).map { it.name })

        // Removing the assignment unassigns the characters
        AssignmentRepository.remove(AssignmentRepository.find(project.id, actor.id)!!.id)
        assertTrue(CharacterRepository.forActor(actor.id, project.id).isEmpty())

        // Removing the actor cleans up
        AssignmentRepository.add(actor.id, project.id)
        ActorRepository.remove(actor.id)
        assertNull(ActorRepository.byId(actor.id))
        assertTrue(AssignmentRepository.forProject(project.id).isEmpty())
    }
}
