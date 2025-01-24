package com.magnariuk.lemonkasubstoolw.data.api.database

import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val session: HttpSession,
    @Autowired private val dbc: DbController
) {
    val db = dbc.getDB()
    private val passwordEncoder = BCryptPasswordEncoder()

    fun login(username: String, password: String): String{
        val user = db.getUser(username)
        if(user != null){
            if(isPasswordValid(user, password)){
                session.setAttribute("user", user)
                return "s"
            } else{
                return "e:pnv"
            }
        } else{
            return "e:unf"
        }
    }

    fun getLoggedInUser(): User? {
        return session.getAttribute("user") as? User
    }

    fun logout() {
        session.invalidate()
        session.setAttribute("user", null)
    }

    fun register(username: String, password: String): String{
        if(db.getUser(username) != null){
            return "e:uax"
        } else{
            db.createUser(username, hashPassword(password))
            login(username, password)
            return "s"
        }
    }

    fun getUserByUsername(username: String): User? {
        return db.getUser(username)
    }

    fun hashPassword(password: String): String {
        return passwordEncoder.encode(password)
    }

    fun isPasswordValid(user: User, rawPassword: String): Boolean {
        return passwordEncoder.matches(rawPassword, user.password)
    }

}