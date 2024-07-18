package com.gabb.customidentitytest

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry

const val PACKAGE_NAME = "com.gabb.customidentitytest"
const val CREATE_PASSKEY_INTENT = "com.gabb.customidentitytest.CREATE_PASSKEY"
const val UNIQUE_REQ_CODE = 100
const val PERSONAL_ACCOUNT_ID = "personal"
const val FAMILY_ACCOUNT_ID = "family"
const val EXTRA_KEY_ACCOUNT_ID = "account_id"
const val UNLOCK_INTENT = "com.gabb.customidentitytest.UNLOCK"
const val UNIQUE_REQUEST_CODE = 101

//TODO: We should look at the identity samples https://github.com/android/identity-samples/tree/main
// for more information, more specifically, the MyVault


@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class MyCredentialProvider : CredentialProviderService() {

    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>,
    ) {
        val response: BeginCreateCredentialResponse? = processCreateCredentialRequest(request)
        if (response != null) {
            callback.onResult(response)
        } else {
            callback.onError(CreateCredentialUnknownException())
        }
    }

    fun processCreateCredentialRequest(request: BeginCreateCredentialRequest): BeginCreateCredentialResponse? {
        when (request) {
            is BeginCreatePublicKeyCredentialRequest -> {
                // Request is passkey type
                return handleCreatePasskeyQuery(request)
            }
        }
        // Request not supported
        return null
    }

    private fun handleCreatePasskeyQuery(
        request: BeginCreatePublicKeyCredentialRequest,
    ): BeginCreateCredentialResponse {

        // Adding two create entries - one for storing credentials to the 'Personal'
        // account, and one for storing them to the 'Family' account. These
        // accounts are local to this sample app only.
        val createEntries: MutableList<CreateEntry> = mutableListOf()
        createEntries.add(
            CreateEntry(
                PERSONAL_ACCOUNT_ID,
                createNewPendingIntent(PERSONAL_ACCOUNT_ID, CREATE_PASSKEY_INTENT)
            )
        )

        createEntries.add(
            CreateEntry(
                FAMILY_ACCOUNT_ID,
                createNewPendingIntent(FAMILY_ACCOUNT_ID, CREATE_PASSKEY_INTENT)
            )
        )

        return BeginCreateCredentialResponse(createEntries)
    }

    private fun createNewPendingIntent(accountId: String, action: String): PendingIntent {
        val intent = Intent(action).setPackage(PACKAGE_NAME)

        // Add your local account ID as an extra to the intent, so that when
        // user selects this entry, the credential can be saved to this
        // account
        intent.putExtra(EXTRA_KEY_ACCOUNT_ID, accountId)

        return PendingIntent.getActivity(
            applicationContext,
            UNIQUE_REQ_CODE,
            intent,
            (PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        )
    }

    private val unlockEntryTitle = "Authenticate to continue"

    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        if (false) {
            callback.onResult(
                BeginGetCredentialResponse(
                    authenticationActions = mutableListOf(
                        AuthenticationAction(
                            unlockEntryTitle, createUnlockPendingIntent()
                        )
                    )
                )
            )
            return
        }
        try {
            val response = processGetCredentialsRequest(request)
            callback.onResult(response)
        } catch (e: GetCredentialException) {
            callback.onError(GetCredentialUnknownException())
        }
    }

    private fun createUnlockPendingIntent(): PendingIntent {
        val intent = Intent(UNLOCK_INTENT).setPackage(PACKAGE_NAME)
        return PendingIntent.getActivity(
            applicationContext, UNIQUE_REQUEST_CODE, intent, (
                    PendingIntent.FLAG_MUTABLE
                            or PendingIntent.FLAG_UPDATE_CURRENT
                    )
        )
    }

    companion object {
        // These intent actions are specified for corresponding activities
        // that are to be invoked through the PendingIntent(s)
        private const val GET_PASSKEY_INTENT_ACTION = "PACKAGE_NAME.GET_PASSKEY"
        private const val GET_PASSWORD_INTENT_ACTION = "PACKAGE_NAME.GET_PASSWORD"

    }

    fun processGetCredentialsRequest(
        request: BeginGetCredentialRequest,
    ): BeginGetCredentialResponse {
        println(request)
        println(request.callingAppInfo?.packageName)
        println(request.callingAppInfo?.origin)
        val callingPackage = request.callingAppInfo?.packageName
        val credentialEntries: MutableList<CredentialEntry> = mutableListOf()

        for (option in request.beginGetCredentialOptions) {
            when (option) {
                is BeginGetPasswordOption -> {
                    credentialEntries.addAll(
                        populatePasswordData(
                            callingPackage.orEmpty(),
                            option
                        )
                    )
                }

                is BeginGetPublicKeyCredentialOption -> {
                    credentialEntries.addAll(
                        populatePasskeyData(
                            request.callingAppInfo!!,
                            option
                        )
                    )
                }

                else -> {
                    Log.i("TAG", "Request not supported")
                }
            }
        }
        return BeginGetCredentialResponse(credentialEntries)
    }

    private fun populatePasskeyData(
        callingAppInfo: CallingAppInfo,
        option: BeginGetPublicKeyCredentialOption,
    ): List<CredentialEntry> {
        val passkeyEntries: MutableList<CredentialEntry> = mutableListOf()
        //val request = PublicKeyCredentialRequestOptions(option.requestJson)
        // Get your credentials from database where you saved during creation flow
        //val creds = <getCredentialsFromInternalDb(request.rpId)>
        val passkeys = listOf(
            PasswordInfo("demo@example.com", "demo")
        )
        //val passkeys = creds.passkeys
        for (passkey in passkeys) {
            val data = Bundle()
            data.putString("credId", passkey.username)
            passkeyEntries.add(
                PublicKeyCredentialEntry(
                    context = applicationContext,
                    username = passkey.username,
                    pendingIntent = createNewPendingIntent(
                        GET_PASSKEY_INTENT_ACTION,
                        data
                    ),
                    beginGetPublicKeyCredentialOption = option,
                    displayName = passkey.username,//passkey.displayName,
                    //icon = passkey.icon
                )
            )
        }
        return passkeyEntries
    }

    // Fetch password credentials and create password entries to populate to
// the user
    private fun populatePasswordData(
        callingPackage: String,
        option: BeginGetPasswordOption,
    ): List<CredentialEntry> {
        val passwordEntries: MutableList<CredentialEntry> = mutableListOf()

        // Get your password credentials from database where you saved during
        // creation flow
        //val creds = <getCredentialsFromInternalDb(callingPackage)>
        val passwords = listOf(
            PasswordInfo("demo@example.com", "demo")
        )// creds.passwords
        for (password in passwords) {
            passwordEntries.add(
                PasswordCredentialEntry(
                    context = applicationContext,
                    username = password.username,
                    pendingIntent = createNewPendingIntent(
                        GET_PASSWORD_INTENT_ACTION
                    ),
                    beginGetPasswordOption = option,
                    displayName = password.username,
                    //icon = Icon.//password.icon
                )
            )
        }
        return passwordEntries
    }

    data class PasswordInfo(val username: String, val password: String)

    private fun createNewPendingIntent(
        action: String,
        extra: Bundle? = null,
    ): PendingIntent {
        val intent = Intent(action).setPackage(PACKAGE_NAME)
        if (extra != null) {
            intent.putExtra("CREDENTIAL_DATA", extra)
        }

        return PendingIntent.getActivity(
            applicationContext, UNIQUE_REQUEST_CODE, intent,
            (PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        )
    }

    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>,
    ) {
        // TODO("Not yet implemented")
    }
}