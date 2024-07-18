package com.gabb.credentialmanagertest

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import com.gabb.credentialmanagertest.ui.theme.CustomIdentityTestTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val credentialManager by lazy { CredentialManager.create(this) }
    private var credentialResponse by mutableStateOf<GetCredentialResponse?>(null)
    private var data by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CustomIdentityTestTheme {
                val scope = rememberCoroutineScope()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    /*val getCredRequest = GetCredentialRequest(
                                        listOf(
                                            GetPasswordOption(),
                                            GetPublicKeyCredentialOption(fetchAuthJsonFromServer())
                                        )
                                    )

                                    credentialResponse = credentialManager.getCredential(
                                        this@MainActivity,
                                        getCredRequest
                                    )*/

                                    data = getSavedCredentials(configureGetCredentialRequest())


                                }
                            }
                        ) { Text("Get Credentials") }

                        credentialResponse?.let {
                            Text(text = it.toString())
                            Text(it.credential.type)
                            Text(it.credential.data.toString())
                        }

                        Text(data.orEmpty())
                    }
                }
            }
        }
    }

    private suspend fun getSavedCredentials(getCredentialRequest: GetCredentialRequest): String? {
        val result = try {
            credentialManager.getCredential(
                this,
                getCredentialRequest,
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Auth", "getCredential failed with exception: " + e.message.toString())
            return null
        }

        if (result.credential is PublicKeyCredential) {
            val cred = result.credential as PublicKeyCredential
            return "Passkey: ${cred.authenticationResponseJson}"
        }
        if (result.credential is PasswordCredential) {
            val cred = result.credential as PasswordCredential
            return "Got Password - User:${cred.id} Password: ${cred.password}"
        }
        if (result.credential is CustomCredential) {
            //If you are also using any external sign-in libraries, parse them here with the
            // utility functions provided.
        }
        return null
    }

    private fun configureGetCredentialRequest(): GetCredentialRequest {
        val getPublicKeyCredentialOption =
            GetPublicKeyCredentialOption(fetchAuthJsonFromServer(), null)
        val getPasswordOption = GetPasswordOption()
        val getCredentialRequest = GetCredentialRequest(
            listOf(
                getPublicKeyCredentialOption,
                getPasswordOption
            )
        )
        return getCredentialRequest
    }

    private fun fetchAuthJsonFromServer(): String {
        return readFromAsset("AuthFromServer")
    }
}

fun Context.readFromAsset(fileName: String): String {
    var data = ""
    this.assets.open(fileName).bufferedReader().use {
        data = it.readText()
    }
    return data
}