package com.gabb.customidentitytest

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.activity.ComponentActivity
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date

class AccountAuthenticator(
    private val mContext: Context,
) : AbstractAccountAuthenticator(mContext) {
    @Throws(NetworkErrorException::class)
    override fun addAccount(
        response: AccountAuthenticatorResponse,
        accountType: String,
        authTokenType: String?,
        requiredFeatures: Array<String>?,
        options: Bundle,
    ): Bundle {
        val reply = Bundle()

        val intent = Intent(mContext, LoginActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, accountType)
        intent.putExtra(LoginActivity.ARG_AUTH_TOKEN_TYPE, authTokenType)
        intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true)

        // return our AccountAuthenticatorActivity
        reply.putParcelable(AccountManager.KEY_INTENT, intent)

        return reply
    }

    @Throws(NetworkErrorException::class)
    override fun confirmCredentials(
        arg0: AccountAuthenticatorResponse,
        arg1: Account, arg2: Bundle,
    ): Bundle? {
        return null
    }

    override fun editProperties(arg0: AccountAuthenticatorResponse, arg1: String): Bundle? {
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun getAuthToken(
        response: AccountAuthenticatorResponse,
        account: Account,
        authTokenType: String,
        options: Bundle,
    ): Bundle {
        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        val am = AccountManager.get(mContext)

        var authToken = am.peekAuthToken(account, authTokenType)


        // Lets give another try to authenticate the user
        if (null != authToken) {
            if (authToken.isEmpty()) {
                val password = am.getPassword(account)
                if (password != null) {
                    authToken = AccountUtils.mServerAuthenticator.signIn(account.name, password)
                }
            }
        }


        // If we get an authToken - we return it
        if (null != authToken) {
            if (authToken.isNotEmpty()) {
                val result = Bundle()
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
                result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
                return result
            }
        }


        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity.
        val intent = Intent(mContext, LoginActivity::class.java)
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
        intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, account.type)
        intent.putExtra(LoginActivity.ARG_AUTH_TOKEN_TYPE, authTokenType)


        // This is for the case multiple accounts are stored on the device
        // and the AccountPicker dialog chooses an account without auth token.
        // We can pass out the account name chosen to the user of write it
        // again in the Login activity intent returned.
        if (null != account) {
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, account.name)
        }

        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)

        return bundle
    }

    override fun getAuthTokenLabel(arg0: String): String? {
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun hasFeatures(
        arg0: AccountAuthenticatorResponse, arg1: Account,
        arg2: Array<String>,
    ): Bundle? {
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun updateCredentials(
        arg0: AccountAuthenticatorResponse,
        arg1: Account, arg2: String, arg3: Bundle,
    ): Bundle? {
        return null
    }
}

object AccountUtils {
    const val ACCOUNT_TYPE: String = "com.gabb.account.type"
    const val AUTH_TOKEN_TYPE: String = "com.gabb.account.type.aaa"

    var mServerAuthenticator: IServerAuthenticator = MyServerAuthenticator()

    fun getAccount(context: Context?, accountName: String?): Account? {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
        for (account in accounts) {
            if (account.name.equals(accountName, ignoreCase = true)) {
                return account
            }
        }
        return null
    }
}

class AuthenticatorService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        var binder: IBinder? = null
        if (intent.action == AccountManager.ACTION_AUTHENTICATOR_INTENT) {
            binder = authenticator!!.iBinder
        }
        return binder
    }

    private val authenticator: AccountAuthenticator?
        get() {
            if (null == sAccountAuthenticator) {
                sAccountAuthenticator = AccountAuthenticator(this)
            }
            return sAccountAuthenticator
        }

    companion object {
        private var sAccountAuthenticator: AccountAuthenticator? = null
    }
}

interface IServerAuthenticator {
    /**
     * Tells the server to create the new user and return its auth token.
     * @param email
     * @param username
     * @param password
     * @return Access token
     */
    fun signUp(email: String?, username: String?, password: String?): String?

    /**
     * Logs the user in and returns its auth token.
     * @param email
     * @param password
     * @return Access token
     */
    fun signIn(email: String?, password: String?): String?
}

class MyServerAuthenticator : IServerAuthenticator {
    override fun signUp(email: String?, username: String?, password: String?): String? {
        // TODO: register new user on the server and return its auth token
        return null
    }

    override fun signIn(email: String?, password: String?): String? {
        var authToken: String? = null
        val df: DateFormat = SimpleDateFormat("yyyyMMdd-HHmmss")

        if (mCredentialsRepo.containsKey(email)) {
            if (password == mCredentialsRepo[email]) {
                authToken = email + "-" + df.format(Date())
            }
        }

        return authToken
    }

    companion object {
        /**
         * A dummy authentication store containing known user names and passwords.
         * TODO: remove after connecting to a real authentication system.
         */
        private var mCredentialsRepo: Map<String?, String>

        init {
            val credentials: MutableMap<String?, String> = HashMap()
            credentials["demo@example.com"] = "demo"
            credentials["foo@example.com"] = "foobar"
            credentials["user@example.com"] = "pass"
            mCredentialsRepo = Collections.unmodifiableMap(credentials)
        }
    }
}

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
class LoginActivity : CustomAccountAuthenticatorActivity() {
    private var mAccountManager: AccountManager? = null

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserLoginTask? = null

    // Values for email and password at the time of the login attempt.
    private var mEmail: String? = null
    private var mPassword: String? = null

    // UI references.
    private var mEmailView: EditText? = null
    private var mPasswordView: EditText? = null
    private var mLoginFormView: View? = null
    private var mLoginStatusView: View? = null
    private var mLoginStatusMessageView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        mAccountManager = AccountManager.get(this)

        // Set up the login form.
        mEmail = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        mEmailView = findViewById<View>(R.id.email) as EditText
        mEmailView!!.setText(mEmail)

        mPasswordView = findViewById<EditText>(R.id.password)
        mPasswordView!!.setOnEditorActionListener(
            OnEditorActionListener { textView, id, keyEvent ->
                if (id == EditorInfo.IME_NULL) {
                    attemptLogin()
                    return@OnEditorActionListener true
                }
                false
            }
        )

        mLoginFormView = findViewById<View>(R.id.login_form)
        mLoginStatusView = findViewById<View>(R.id.login_status)
        mLoginStatusMessageView = findViewById<View>(R.id.login_status_message) as TextView

        findViewById<View>(R.id.sign_in_button).setOnClickListener { attemptLogin() }

        if (null != mEmail) {
            if (mEmail!!.isNotEmpty()) {
                mPasswordView!!.requestFocus()
            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    fun attemptLogin() {
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        mEmailView!!.error = null
        mPasswordView!!.error = null

        // Store values at the time of the login attempt.
        mEmail = mEmailView!!.text.toString()
        mPassword = mPasswordView!!.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView!!.error = "Error"
            focusView = mPasswordView
            cancel = true
        } else if (mPassword!!.length < 4) {
            mPasswordView!!.error = "getString(R.string.error_invalid_password)"
            focusView = mPasswordView
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mEmail)) {
            mEmailView!!.error = "getString(R.string.error_field_required)"
            focusView = mEmailView
            cancel = true
        } else if (!mEmail!!.contains("@")) {
            mEmailView!!.error = "getString(R.string.error_invalid_email)"
            focusView = mEmailView
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView!!.setText("R.string.login_progress_signing_in")
            showProgress(true)
            mAuthTask = UserLoginTask()
            mAuthTask!!.execute(null as Void?)
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        val shortAnimTime = 5

        mLoginStatusView!!.visibility = View.VISIBLE
        mLoginStatusView!!.animate().setDuration(shortAnimTime.toLong())
            .alpha((if (show) 1 else 0).toFloat())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mLoginStatusView!!.visibility = if (show) View.VISIBLE
                    else View.GONE
                }
            })

        mLoginFormView!!.visibility = View.VISIBLE
        mLoginFormView!!.animate().setDuration(shortAnimTime.toLong())
            .alpha((if (show) 0 else 1).toFloat())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mLoginFormView!!.visibility = if (show) View.GONE
                    else View.VISIBLE
                }
            })
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    inner class UserLoginTask : AsyncTask<Void?, Void?, Intent>() {
        override fun doInBackground(vararg params: Void?): Intent {
            // TODO: attempt authentication against a network service.

            val authToken = AccountUtils.mServerAuthenticator.signIn(mEmail, mPassword)

            val res = Intent()

            res.putExtra(AccountManager.KEY_ACCOUNT_NAME, mEmail)
            res.putExtra(AccountManager.KEY_ACCOUNT_TYPE, AccountUtils.ACCOUNT_TYPE)
            res.putExtra(AccountManager.KEY_AUTHTOKEN, authToken)
            res.putExtra(PARAM_USER_PASSWORD, mPassword)

            return res
        }

        override fun onPostExecute(intent: Intent) {
            mAuthTask = null
            showProgress(false)

            if (null == intent.getStringExtra(AccountManager.KEY_AUTHTOKEN)) {
                mPasswordView!!.error = "getString(R.string.error_incorrect_password)"
                mPasswordView!!.requestFocus()
            } else {
                finishLogin(intent)
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }

        private fun finishLogin(intent: Intent) {
            val accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            val accountPassword = intent.getStringExtra(PARAM_USER_PASSWORD)
            val account =
                Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE))
            val authToken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN)

            if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
                // Creating the account on the device and setting the auth token we got
                // (Not setting the auth token will cause another call to the server to authenticate the user)
                mAccountManager!!.addAccountExplicitly(account, accountPassword, null)
                mAccountManager!!.setAuthToken(account, AccountUtils.AUTH_TOKEN_TYPE, authToken)
            } else {
                mAccountManager!!.setPassword(account, accountPassword)
            }

            mAccountManager!!.setUserData(account, "asdf", "Hello there!")

            ContentResolver.setIsSyncable(account, "com.gabb.customidentitytest", 1)

            lifecycleScope.launch {
                runCatching {
                    CredentialManager.create(this@LoginActivity).saveCredential(
                        this@LoginActivity,
                        accountName!!,
                        accountPassword!!
                    )
                }.onFailure { it.printStackTrace() }
            }

            setAccountAuthenticatorResult(intent.extras)
            setResult(RESULT_OK, intent)

            finish()
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        super.onBackPressed()
    }

    companion object {
        const val ARG_ACCOUNT_TYPE: String = "accountType"
        const val ARG_AUTH_TOKEN_TYPE: String = "authTokenType"
        const val ARG_IS_ADDING_NEW_ACCOUNT: String = "isAddingNewAccount"
        const val PARAM_USER_PASSWORD: String = "password"
    }
}

open class CustomAccountAuthenticatorActivity : ComponentActivity() {
    private var mAccountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var mResultBundle: Bundle? = null

    /**
     * Set the result that is to be sent as the result of the request that caused this
     * Activity to be launched. If result is null or this method is never called then
     * the request will be canceled.
     * @param result this is returned as the result of the AbstractAccountAuthenticator request
     */
    fun setAccountAuthenticatorResult(result: Bundle?) {
        mResultBundle = result
    }

    /**
     * Retrieves the AccountAuthenticatorResponse from either the intent of the icicle, if the
     * icicle is non-zero.
     * @param icicle the save instance data of this Activity, may be null
     */
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)


        mAccountAuthenticatorResponse = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(
                AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                AccountAuthenticatorResponse::class.java
            )
        } else {
            intent.getParcelableExtra(
                AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE
            )
        }

        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse!!.onRequestContinued()
        }
    }

    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result isn't present.
     */
    override fun finish() {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse!!.onResult(mResultBundle)
            } else {
                mAccountAuthenticatorResponse!!.onError(
                    AccountManager.ERROR_CODE_CANCELED,
                    "canceled"
                )
            }
            mAccountAuthenticatorResponse = null
        }
        super.finish()
    }
}

suspend fun CredentialManager.saveCredential(context: Context, username: String, password: String) {
    try {
        //Ask the user for permission to add the credentials to their store
        createCredential(
            context = context,
            request = CreatePasswordRequest(username, password),
        )
        Log.v("CredentialTest", "Credentials successfully added")
    } catch (e: CreateCredentialCancellationException) {
        //do nothing, the user chose not to save the credential
        Log.v("CredentialTest", "User cancelled the save")
    } catch (e: CreateCredentialException) {
        Log.v("CredentialTest", "Credential save error", e)
    }
}
