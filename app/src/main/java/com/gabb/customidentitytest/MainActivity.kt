package com.gabb.customidentitytest

import android.R.id.text1
import android.R.id.text2
import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OperationCanceledException
import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gabb.customidentitytest.AccountUtils.getAccount
import com.gabb.customidentitytest.ui.theme.CustomIdentityTestTheme


class MainActivity : ComponentActivity() {
    private val accountManager by lazy { AccountManager.get(this) }
    private var authToken by mutableStateOf<String?>("")
    private val textToShow = mutableStateListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        accountManager.getAuthTokenByFeatures(
            AccountUtils.ACCOUNT_TYPE,
            AccountUtils.AUTH_TOKEN_TYPE,
            null,
            this,
            null,
            null,
            { result ->
                try {
                    val bundle = result.result

                    val intent = bundle[AccountManager.KEY_INTENT] as Intent?
                    if (null != intent) {
                        startActivityForResult(intent, 1)
                    } else {
                        authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN)
                        val accountName = bundle.getString(AccountManager.KEY_ACCOUNT_NAME)

                        textToShow.add("Retrieved auth token: $authToken")
                        textToShow.add("Saved account name: $accountName")

                        // If the logged account didn't exist, we need to create it on the device
                        var account = getAccount(this@MainActivity, accountName)
                        if (null == account) {
                            account = Account(accountName, AccountUtils.ACCOUNT_TYPE)
                            accountManager.addAccountExplicitly(
                                account,
                                bundle.getString(LoginActivity.PARAM_USER_PASSWORD),
                                null
                            )
                            accountManager.setAuthToken(
                                account,
                                AccountUtils.AUTH_TOKEN_TYPE,
                                authToken
                            )
                        }

                        ContentResolver.setIsSyncable(account, "com.gabb.customidentitytest", 1)

                        accountManager.setUserData(account, "asdf", "Hello there!")

                        val randomValue = accountManager.getUserData(account, "asdf")
                        textToShow.add("Random value: adsf: $randomValue")
                    }
                } catch (e: OperationCanceledException) {
                    // If signup was cancelled, force activity termination
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            null
        )
        setContent {
            CustomIdentityTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Stuff(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    @Composable
    fun Stuff(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            textToShow.forEach {
                Text(text = it)
            }
        }
    }

}