package com.magnariuk.lemonka.features.profile

import com.magnariuk.lemonka.auth.AuthService
import com.magnariuk.lemonka.auth.UserSession
import com.magnariuk.lemonka.auth.user
import com.magnariuk.lemonka.data.db.*
import com.magnariuk.lemonka.ui.components.*
import com.magnariuk.lemonka.ui.layout.MenuItem
import com.magnariuk.lemonka.web.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.div

/** The user's own page: account, settings, and everything they own, with delete buttons. */
object ProfileFeature : Feature {
    override val menu = MenuItem("Профіль", "/profile", Icon.User)

    override fun Route.routes() {
        route("/profile") {
            get {
                val user = call.user
                val inviter = user.invitedBy?.let { InviteRepository.byId(it) }?.let { UserRepository.byId(it.createdBy) }
                val settings = SettingsRepository.get(user.id)
                val sessions = TokenRepository.activeCount(user.id)
                val projects = ProjectRepository.overview(user.id)
                val actors = ActorRepository.overview(user.id)
                call.respondPage("Профіль") {
                    ProfileHeader(user, inviter?.username)
                    div("profile-grid") {
                        Column(gap = 16) {
                            SettingsCard(settings, sessions)
                            PasswordCard()
                            DangerCard()
                        }
                        Column(gap = 16) {
                            ProjectsCard(projects)
                            ActorsCard(actors)
                        }
                    }
                }
            }

            // --- account ---
            post("/password") {
                val f = call.receiveParameters()
                val error = AuthService.changePassword(call.user, f["current"].orEmpty(), f["new"].orEmpty(), f["repeat"].orEmpty())
                call.respondFragment {
                    PasswordCard(error?.let { PasswordErrors(it.first, it.second) })
                    if (error == null) Toast("Пароль змінено")
                }
            }
            post("/settings") {
                val on = call.receiveParameters()["hideSelected"] == "true"
                SettingsRepository.setHideSelected(call.user.id, on)
                call.respondToast("Налаштування збережено")
            }
            post("/sessions/logout-others") {
                val current = call.sessions.get<UserSession>()!!.token
                val removed = TokenRepository.deleteOthers(call.user.id, current)
                call.respondToast(if (removed > 0) "Вийшли з інших сесій: $removed" else "Інших сесій немає", ToastTone.Info)
            }
            get("/delete") { call.respondFragment { DeleteAccountDialog() } }
            post("/delete") {
                val error = AuthService.deleteAccount(call.user, call.receiveParameters()["password"].orEmpty())
                if (error != null) return@post call.respondFragment { DeleteAccountDialog(error) }
                call.sessions.clear<UserSession>()
                call.hxRedirect("/login")
            }

            // --- projects & characters ---
            get("/projects/{id}") {
                val project = call.ownedProject()
                val characters = CharacterRepository.forProject(project.id)
                val actorNames = ActorRepository.forUser(call.user.id).associate { it.id to it.name }
                call.respondFragment { ProjectDialog(project, characters, actorNames) }
            }
            post("/projects/{id}/rename") {
                val project = call.ownedProject()
                val name = call.receiveParameters()["name"].orEmpty().trim()
                if (name.isEmpty()) return@post call.respondToast("Назва не може бути порожньою", ToastTone.Error)
                ProjectRepository.rename(project.id, name)
                val projects = ProjectRepository.overview(call.user.id)
                call.respondFragment { ProjectsCard(projects, oob = true); Toast("Проєкт перейменовано") }
            }
            delete("/projects/{id}") {
                val project = call.ownedProject()
                ProjectRepository.delete(project.id)
                call.respondFragment { Toast("Проєкт «${project.name}» видалено") }
            }
            delete("/characters/{id}") {
                val character = CharacterRepository.byId(call.intParam()) ?: throw NotFoundException()
                call.ownedProject(character.project)
                CharacterRepository.delete(character.id)
                val projects = ProjectRepository.overview(call.user.id)
                call.respondFragment { ProjectsCard(projects, oob = true); Toast("Персонажа «${character.name}» видалено") }
            }

            // --- actors ---
            post("/actors") {
                val name = call.receiveParameters()["name"].orEmpty().trim()
                val user = call.user
                val error = when {
                    name.isEmpty() -> "введіть ім'я"
                    ActorRepository.byName(name, user.id) != null -> "такий актор уже є"
                    else -> null
                }
                if (error == null) ActorRepository.add(name, user.id)
                val actors = ActorRepository.overview(user.id)
                call.respondFragment {
                    ActorsCard(actors, error)
                    if (error == null) Toast("Актора «$name» додано")
                }
            }
            delete("/actors/{id}") {
                val actor = ActorRepository.byId(call.intParam())?.takeIf { it.user == call.user.id }
                    ?: throw NotFoundException()
                ActorRepository.remove(actor.id)
                val projects = ProjectRepository.overview(call.user.id)
                call.respondFragment { ProjectsCard(projects, oob = true); Toast("Актора «${actor.name}» видалено") }
            }
        }
    }
}

/** Path parameter `{id}` as Int, or 404. */
fun ApplicationCall.intParam(name: String = "id"): Int = parameters[name]?.toIntOrNull() ?: throw NotFoundException()

/** The project with this id if it belongs to the current user; otherwise 404 (so ids of others don't leak). */
suspend fun ApplicationCall.ownedProject(id: Int = intParam()): Project =
    ProjectRepository.byId(id)?.takeIf { it.user == user.id } ?: throw NotFoundException()
