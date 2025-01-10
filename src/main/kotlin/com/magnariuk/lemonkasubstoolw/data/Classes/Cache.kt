package com.magnariuk.lemonkasubstoolw.data.Classes

data class Cache(
    var projects: MutableList<Project> = mutableListOf(),
    var actors: MutableList<String> = mutableListOf(),
    var hideSelected: Boolean = false,
    var separators: MutableList<String> = mutableListOf("and", ",")
)


data class Actor(
    var actorName: String = "",
    var characterNames: MutableList<String> = mutableListOf(),
)
data class User(
    var userName: String = "",
    var password: String = "",
)

data class Project(
    var name: String,
    var characters: MutableList<String> = mutableListOf(),
    var actors: MutableList<String> = mutableListOf(),
    var selected: MutableList<Actor> = mutableListOf(),
)
