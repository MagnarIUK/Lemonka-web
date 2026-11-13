package com.magnariuk.lemonka.features.admin

import com.magnariuk.lemonka.auth.user
import com.magnariuk.lemonka.data.db.*
import com.magnariuk.lemonka.features.profile.intParam
import com.magnariuk.lemonka.ui.components.Icon
import com.magnariuk.lemonka.ui.components.Toast
import com.magnariuk.lemonka.ui.components.ToastTone
import com.magnariuk.lemonka.ui.layout.MenuItem
import com.magnariuk.lemonka.web.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.div
import kotlin.time.Duration.Companion.days

/** Admin-only panel: invite codes, users, and global separators. */
object AdminFeature : Feature {
    override val menu = MenuItem("Адміністрування", "/admin", Icon.Shield) { it.isAdmin }

    override fun Route.adminRoutes() {
        route("/admin") {
            get { call.respondRedirect(AdminTab.Invites.path) }

            // ---- invites ----
            get("/invites") {
                val data = InvitesData.load()
                val base = call.baseUrl()
                call.respondPage("Запрошення") {
                    AdminTabs(AdminTab.Invites)
                    div("admin-stack") {
                        CreateInviteCard()
                        InvitesCard(data.invites, data.creators, data.usedBy, base)
                    }
                }
            }
            post("/invites") {
                val f = call.receiveParameters()
                val maxUses = f["maxUses"]?.toIntOrNull()?.coerceIn(1, 100) ?: 1
                val days = f["validDays"]?.toIntOrNull() ?: 7
                val invite = InviteRepository.create(call.user.id, maxUses, if (days > 0) days.days else null, f["note"])
                call.respondInvites(highlight = invite.id) { Toast("Запрошення ${invite.code} створено") }
            }
            post("/invites/{id}/revoke") {
                InviteRepository.revoke(call.intParam())
                call.respondInvites { Toast("Запрошення відкликано", ToastTone.Info) }
            }
            delete("/invites/{id}") {
                InviteRepository.delete(call.intParam())
                call.respondFragment { Toast("Запрошення видалено") }
            }

            // ---- users ----
            get("/users") {
                val users = UserRepository.overview()
                val me = call.user
                call.respondPage("Користувачі") {
                    AdminTabs(AdminTab.Users)
                    UsersCard(users, me)
                }
            }
            post("/users/{id}/role") {
                val role = Role.of(call.receiveParameters()["role"].orEmpty())
                val error = AdminService.setRole(call.user, call.intParam(), role)
                call.respondUsers { Toast(error ?: "Роль змінено", if (error == null) ToastTone.Success else ToastTone.Error) }
            }
            get("/users/merge") {
                val users = UserRepository.overview()
                val me = call.user
                call.respondFragment { MergeDialog(users, me) }
            }
            post("/users/merge") {
                val f = call.receiveParameters()
                val target = f["target"]?.toIntOrNull() ?: -1
                val sources = f.getAll("source").orEmpty().mapNotNull { it.toIntOrNull() }
                val (error, result) = AdminService.merge(call.user, target, sources)
                val users = UserRepository.overview()
                val me = call.user
                if (error != null) return@post call.respondFragment { MergeDialog(users, me, error) }
                val r = result!!
                call.respondFragment {
                    UsersCard(users, me, oob = true)
                    Toast(buildString {
                        append("Об'єднано: ${plural(sources.size, "акаунт", "акаунти", "акаунтів")}, ")
                        append("${plural(r.projects, "проєкт", "проєкти", "проєктів")}, ")
                        append("${plural(r.actorsMoved + r.actorsJoined, "актор", "актори", "акторів")}")
                        if (r.projectsRenamed > 0) append(" (перейменовано проєктів: ${r.projectsRenamed})")
                    })
                }
            }
            post("/users/{id}/reset-password") {
                val target = UserRepository.byId(call.intParam()) ?: throw NotFoundException()
                val password = AdminService.resetPassword(target.id)!!
                call.respondFragment { TempPasswordDialog(target.username, password) }
            }
            delete("/users/{id}") {
                val error = AdminService.deleteUser(call.user, call.intParam())
                call.respondUsers { Toast(error ?: "Користувача видалено", if (error == null) ToastTone.Success else ToastTone.Error) }
            }

            // ---- separators ----
            get("/separators") {
                val separators = SeparatorRepository.list()
                call.respondPage("Роздільники") {
                    AdminTabs(AdminTab.Separators)
                    SeparatorsCard(separators)
                }
            }
            post("/separators") {
                // Not trimmed on purpose: " and " with spaces can be a meaningful separator.
                val value = call.receiveParameters()["separator"].orEmpty()
                val error = when {
                    value.isBlank() -> "введіть роздільник"
                    !SeparatorRepository.add(value) -> "такий роздільник уже є"
                    else -> null
                }
                val separators = SeparatorRepository.list()
                call.respondFragment {
                    SeparatorsCard(separators, error)
                    if (error == null) Toast("Роздільник додано")
                }
            }
            delete("/separators/{id}") {
                SeparatorRepository.delete(call.intParam())
                val separators = SeparatorRepository.list()
                call.respondFragment { SeparatorsCard(separators); Toast("Роздільник видалено") }
            }
        }
    }

    private class InvitesData(val invites: List<Invite>, val creators: Map<Int, String>, val usedBy: Map<Int, List<String>>) {
        companion object {
            suspend fun load() = InvitesData(
                InviteRepository.all(),
                UserRepository.all().associate { it.id to it.username },
                InviteRepository.usedBy(),
            )
        }
    }

    private suspend fun ApplicationCall.respondInvites(highlight: Int? = null, extra: kotlinx.html.FlowContent.() -> Unit) {
        val data = InvitesData.load()
        val base = baseUrl()
        respondFragment { InvitesCard(data.invites, data.creators, data.usedBy, base, highlight); extra() }
    }

    private suspend fun ApplicationCall.respondUsers(extra: kotlinx.html.FlowContent.() -> Unit) {
        val users = UserRepository.overview()
        val me = UserRepository.byId(user.id) ?: user
        respondFragment { UsersCard(users, me); extra() }
    }
}
