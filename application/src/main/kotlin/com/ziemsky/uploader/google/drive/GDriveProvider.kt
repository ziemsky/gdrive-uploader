package com.ziemsky.uploader.google.drive

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.InputStreamReader
import java.nio.file.Path

class GDriveProvider(
        val applicationUserName: String,
        val tokensDirectory: Path,
        val credentialsFilePath: Path,
        val applicationName: String
) {

    fun drive(): Drive {

        val credential: Credential = credential(
                applicationUserName,
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_METADATA_READONLY),
                tokensDirectory,
                credentialsFilePath)

        return Drive.Builder(credential.transport, credential.jsonFactory, credential)
                .setApplicationName(applicationName)
                .build()
    }

    private fun credential(applicationUserName: String,
                           httpTransport: HttpTransport,
                           jsonFactory: JsonFactory,
                           scopes: Collection<String>,
                           tokensDirectory: Path,
                           credentialsFilePath: Path): Credential {

        // todo validate inputs (missing values, files)
        // todo throw custom exceptions

        val credentialsInputStream = credentialsFilePath.toFile().inputStream() // todo try with resources
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(credentialsInputStream))

        val flow = GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                clientSecrets,
                scopes
        )
                .setDataStoreFactory(FileDataStoreFactory(tokensDirectory.toFile()))
                .setAccessType("offline")
                .build()

        return AuthorizationCodeInstalledApp(
                flow,
                LocalServerReceiver()
        ).authorize(applicationUserName)
    }
}