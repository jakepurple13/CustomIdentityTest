package com.gabb.contentsynctest

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import com.gabb.contentsynctest.ui.theme.CustomIdentityTestTheme

class MainActivity : ComponentActivity() {
    private val accountManager by lazy { AccountManager.get(this) }

    private val accounts = mutableStateListOf<Account>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        accountManager.getAccountsByType("com.gabb.account.type")
            .onEach {
                println(it)
                ContentResolver.setIsSyncable(
                    it,
                    "com.gabb.contentsynctest.provider",
                    1
                )
            }
            .let { accounts.addAll(it) }
        setContent {
            CustomIdentityTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        accounts.forEach {
                            Text(text = it.toString())
                        }
                    }
                }
            }
        }
    }
}
