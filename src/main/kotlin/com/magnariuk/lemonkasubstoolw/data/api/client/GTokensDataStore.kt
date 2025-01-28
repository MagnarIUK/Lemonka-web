package com.magnariuk.lemonkasubstoolw.data.api.client

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.AbstractDataStore
import com.google.api.client.util.store.DataStore
import com.google.api.client.util.store.DataStoreFactory
import com.google.auth.oauth2.ClientId.fromJson
import com.magnariuk.lemonkasubstoolw.data.api.database.AuthService
import com.magnariuk.lemonkasubstoolw.data.util.td
import com.vaadin.flow.server.VaadinService
import jakarta.servlet.http.Cookie
import java.io.Serializable
/*

class CookieDataStoreFactory(private val authService: AuthService) : DataStoreFactory {
    override fun <V : Serializable?> getDataStore(id: String): DataStore<V> {
        return CookieDataStore(id, authService) as DataStore<V>
    }

    override fun toString(): String {
        return "CookieDataStoreFactory"
    }
}

class CookieDataStore<V : Serializable?>(private val id: String, private val authService: AuthService) : AbstractDataStore<V>(this) {
    override fun get(key: String): V? {
        val cookieValue: MutableMap<String, String> = mutableMapOf()
        cookieValue.put(authService.getCookie("$id:$key")!!.name, authService.getCookie("$id:$key")!!.value)
        return if (cookieValue != null) {
            StoredCredential().apply {
                fromJson(cookieValue as Map<String, Any>?)
            } as V
        } else {
            null
        }
    }

    override fun set(key: String, value: V): DataStore<V>? {
        val serializedValue = (value as StoredCredential).toString()
        authService.setCookie("$id:$key", serializedValue, 30.td)
        return this
    }

    override fun delete(key: String): DataStore<V>? {
        val value = get(key)
        authService.removeCookie("$id:$key")
        return this
    }

    override fun keySet(): Set<String> {
        // Implement if needed, otherwise return an empty set
        return emptySet()
    }

    override fun values(): Collection<V> {
        // Implement if needed, otherwise return an empty list
        return emptyList()
    }

    override fun clear(): DataStore<V>? {
        // Implement if needed
        return this
    }
}*/