package com.magnariuk.lemonkasubstoolw.data.api

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import java.io.*
import java.security.GeneralSecurityException
import java.util.*


object GoogleSpreadsheetAPI {
    private const val APPLICATION_NAME = "Lemonka Tools"
    private val JSON_FACTORY: JsonFactory = GsonFactory.getDefaultInstance()
    private const val TOKENS_DIRECTORY_PATH = "tokens"

    private val SCOPES = listOf(SheetsScopes.SPREADSHEETS_READONLY)
    private const val CREDENTIALS_FILE_PATH = "/client_secret_813600793486-55rjhj7auarcudjtsfeitra3hqt2it6m.apps.googleusercontent.com.json"

    @Throws(IOException::class)
    private fun getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential {
        val `in` = GoogleSpreadsheetAPI::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
            ?: throw FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH)
        val clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, InputStreamReader(`in`))

        val flow = GoogleAuthorizationCodeFlow.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()
        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    @JvmStatic
    fun getTable(id: String){
        val HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport()
        val spreadsheetId = id
        val range = "Расписание!A:K"
        val service =
            Sheets.Builder(HTTP_TRANSPORT,
                JSON_FACTORY,
                getCredentials(HTTP_TRANSPORT)
            )
                .setApplicationName(APPLICATION_NAME)
                .build()
        val response = service.spreadsheets().values()[spreadsheetId, range]
            .execute()
        val values = response.getValues()
        if (values == null || values.isEmpty()) {
            println("No data found.")
        } else {
            println("Name, Major")
            println(values)
        }
    }
}


