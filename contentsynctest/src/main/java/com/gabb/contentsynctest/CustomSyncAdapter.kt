package com.gabb.contentsynctest

import android.accounts.Account
import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProvider
import android.content.ContentProviderClient
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SyncResult
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.IBinder

class CustomSyncAdapter(
    context: Context?,
    autoInitialize: Boolean,
) : AbstractThreadedSyncAdapter(context, autoInitialize) {
    override fun onPerformSync(
        account: Account?,
        extras: Bundle?,
        authority: String?,
        provider: ContentProviderClient?,
        syncResult: SyncResult?,
    ) {

    }
}

class CustomSyncProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        strings: Array<String>?,
        s: String?,
        strings1: Array<String>?,
        s1: String?,
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        contentValues: ContentValues?,
        s: String?,
        strings: Array<String>?,
    ): Int {
        return 0
    }
}

class CustomSyncService : Service() {
    override fun onCreate() {
        super.onCreate()
        synchronized(sSyncAdapterLock) {
            sSyncAdapter = CustomSyncAdapter(applicationContext, true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return sSyncAdapter?.syncAdapterBinder
    }

    companion object {
        private var sSyncAdapter: CustomSyncAdapter? = null
        private val sSyncAdapterLock = Any()
    }
}