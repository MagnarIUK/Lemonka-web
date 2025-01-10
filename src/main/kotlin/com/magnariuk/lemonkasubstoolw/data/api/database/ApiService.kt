package com.magnariuk.lemonkasubstoolw.data.api.database

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ApiService(
    @Autowired private val dbc: DbController
) {
    val db = dbc.getDB()

    fun getSettings() = db.getSettings()
    fun getSeparators() = db.getSeparators()
    fun createProject(name: String) = db.createProject(name)
    fun getProjects() = db.getProjects()
    fun getProject(id: Int) = db.getProject(id)
    fun getProjectByName(name: String) = db.getProjectByName(name)
    fun addActor(name: String) = db.addActor(name)
    fun removeActor(id: Int) = db.removeActor(id)
    fun getActors() = db.getActors()
    fun getActor(id: Int) = db.getActor(id)
    fun getActorByName(name: String) = db.getActorByName(name)
    fun addAssignment(actor: Int, project: Int) = db.addAssignment(actor, project)
    fun removeAssignment(id: Int) = db.removeAssignment(id)
    fun getProjectAssignments(project: Int) = db.getProjectAssignments(project)
    fun getProjectAssignmentsByActor(project: Int, actor: Int) = db.getProjectAssignmentsByActor(project, actor)

    fun assignActorToCharacter(actor: Int?, character: Int) = db.assignCharacter(actor, character)
    fun addCharacter(name: String, project: Int) = db.addCharacter(name, project)
    fun addCharacters(chars: List<String>, project: Int) {
        chars.forEach { db.addCharacter(it, project) }
    }
    fun getCharactersByActor(actor: Int, project: Int) = db.getCharactersByActor(actor, project)

    fun getCharacterByName(name: String, project: Int) = db.getCharacterByName(name, project)
    fun getProjectCharacters(project: Int) = db.getProjectCharacters(project)

}