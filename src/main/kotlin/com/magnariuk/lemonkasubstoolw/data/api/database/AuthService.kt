package com.magnariuk.lemonkasubstoolw.data.api.database

import com.magnariuk.lemonkasubstoolw.data.util.td
import com.vaadin.flow.server.VaadinService
import jakarta.servlet.http.Cookie
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

    fun setCookie(name: String, value: String, expire: Int) {
        val response = VaadinService.getCurrentResponse()
        val cookie= Cookie(name, value)
        cookie.path = "/"
        cookie.isHttpOnly = true
        cookie.maxAge = expire
        response?.addCookie(cookie)
    }
    fun removeCookie(name: String) {
        val response = VaadinService.getCurrentResponse()
        val cookie = Cookie(name, "")
        cookie.path = "/"
        cookie.maxAge = 0
        response?.addCookie(cookie)
    }

    fun getCookie(name: String): Cookie? {
        val cookies = VaadinService.getCurrentRequest()?.cookies ?: return null
        return cookies.find { it.name == name }
    }

    fun loginWithToken(): String{
        val _token = getCookie("token")
        if(_token != null){
            val loginFile = db.getToken(_token.value)
            if(loginFile != null){
                if(loginFile.valid_to!! > System.currentTimeMillis()/1000){
                    val us = db.getUser(loginFile.user!!)
                    session.setAttribute("user", us)
                    return "s"
                } else{
                    return "e:te"
                }
            } else{
                return "e:tnf"
            }
        } else{
            return "e:ctnf"
        }
    }


    fun login(username: String, password: String): String{
        val user = db.getUser(username)
        if(user != null){
            if(isPasswordValid(user, password)){
                session.setAttribute("user", user)
                val t = db.createToken(user.id)
                setCookie("token", t!!.token!!, 5.td)
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
        removeCookie("token")
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