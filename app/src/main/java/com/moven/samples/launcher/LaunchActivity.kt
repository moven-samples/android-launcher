package com.moven.samples.launcher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.support.v7.app.AppCompatActivity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_launch.*
import java.security.AccessController.getContext

/**
 * A launch screen that launches partner app with component and action
 */
class LaunchActivity : AppCompatActivity() {
    /**
     * Keep track of the launch task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserLaunchTask? = null
    private val LOG_TAG = "MovenLaunchActivity"         // adb logcat | grep MovenLaunchActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(LOG_TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        // Westpac working
        /*
        appid.setText("nz.co.westpac")
        email.setText("nz.co.westpac.one.ui.MasterActivity")
        password.setText("MovenHome")
        */

        appid.setText("com.bca")
        email.setText("com.bca.mobile.MainActivity")
        password.setText("MovenHome")

        //password.setText("ACTIVITY_MAIN")

        // Set up the launch form.
        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLaunch()
                return@OnEditorActionListener true
            }
            false
        })

        launch_button.setOnClickListener { attemptLaunch() }
        check_button.setOnClickListener { checkInstalled() }
    }

    private fun checkInstalled() {

        Log.i(LOG_TAG, "About to getPackageInfo");
        var pi: PackageInfo? = null
        try {
            pi = this.packageManager.getPackageInfo(appid.text.toString(), PackageManager.GET_ACTIVITIES)
        }
        catch (e: Exception) {
            Log.e(LOG_TAG, "getPackageInfo failed: " + e.toString())
        }

        if (pi == null) {
            Log.e(LOG_TAG, "getPackageInfo is NULL");
            val dlgAlert = AlertDialog.Builder(this)
                .setMessage("FAIL")
                .setTitle("Checker")
                .setPositiveButton("OK", null)
                .setCancelable(true)
            dlgAlert.create().show()
        }
        else {
            Log.i(LOG_TAG, "getPackageInfo is VALID: " + pi.toString());
            val dlgAlert = AlertDialog.Builder(this)
                .setMessage("Success!")
                .setTitle("Checker")
                .setPositiveButton("OK", null)
                .setCancelable(true)
            dlgAlert.create().show()
        }
    }

    /**
     * Attempts to sign in or register the account specified by the launch form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual launch attempt is made.
     */
    private fun attemptLaunch() {

        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        appid.error = null
        email.error = null
        password.error = null

        // Store values at the time of the launch attempt.
        val appidStr = appid.text.toString()
        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (TextUtils.isEmpty(appidStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = appid
            cancel = true
        }


        if (cancel) {
            // There was an error; don't attempt launch and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user launch attempt.
            showProgress(true)
            mAuthTask = UserLaunchTask(this, appidStr, emailStr, passwordStr)
            mAuthTask!!.execute(null as Void?)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        //TODO: Replace this with your own logic
        return email.length > 4
    }

    private fun isPasswordValid(password: String): Boolean {
        //TODO: Replace this with your own logic
        return password.length > 4
    }

    /**
     * Shows the progress UI and hides the launch form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            launch_form.visibility = if (show) View.GONE else View.VISIBLE
            launch_form.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            launch_form.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            launch_progress.visibility = if (show) View.VISIBLE else View.GONE
            launch_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            launch_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            launch_progress.visibility = if (show) View.VISIBLE else View.GONE
            launch_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    /**
     * Represents an asynchronous launch/registration task used to authenticate
     * the user.
     */
    inner class UserLaunchTask internal constructor(private val mContext: Context, private val mAppId: String, private val mEmail: String, private val mPassword: String) : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void): String? {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                start(mAppId, mEmail, mPassword); //Thread.sleep(2000)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error during start: " + e.toString())
                return e.toString()
            }

            return null
        }

        fun start(appid: String, component: String, action: String) {
            val launchIntent = Intent()

            launchIntent.action = action
            launchIntent.component = ComponentName(appid, component)
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK

            Log.i(LOG_TAG, "Component=" + launchIntent.component.flattenToString())

            mContext.startActivity(launchIntent)
        }

        override fun onPostExecute(success: String?) {
            mAuthTask = null
            showProgress(false)

            if (success == null) {
                //finish()
                val dlgAlert = AlertDialog.Builder(mContext)
                    .setMessage("Success!")
                    .setTitle("Launcher")
                    .setPositiveButton("OK", null)
                    .setCancelable(true)
                dlgAlert.create().show()
            } else {
                val dlgAlert = AlertDialog.Builder(mContext)
                        .setMessage("Failed: " + success)
                        .setTitle("Launcher")
                        .setPositiveButton("OK", null)
                        .setCancelable(true)
                dlgAlert.create().show()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }
}
