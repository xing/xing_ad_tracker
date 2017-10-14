/*
 * Copyright (C) 2017 XING SE (http://xing.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xing.android.adtracker.internal

import android.app.AlarmManager
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.xing.android.adtracker.*
import java.util.*

/**
 * IntentService used by XNGAdTrackerPreV21 to be called by broadcast receiver which will schedule further attempts
 * via ConnectivityManager broadcasts or AlarmManager of Android versions below 21 (Lollipop)
 *
 * @param xngAdTracker  IXNGAdTracker instance to perform the actual tracking endpoint call
 */
class XNGAdTrackerIntentService(
        private val xngAdTracker: IXNGAdTracker = XNGAdTracker(VersionInfoImpl(), HttpURLConnectionRequester())) :
        IntentService(XNGAdTrackerIntentService::class.java.simpleName) {

    companion object {
        /**
         * builds an intent to call the IntentService for a tracking attempt
         * @param context
         * @param referrer: XING referrer information passed in from install referrer broadcast intent
         * @param previousAttempts: counter of previous attempts (0 on the first attempt)
         */
        @JvmStatic fun buildIntent(context: Context, referrer: String, previousAttempts: Int): Intent {
            val serviceIntent = Intent(context, XNGAdTrackerIntentService::class.java)
            serviceIntent.putExtra(EXTRA_REFERRER, referrer)
            serviceIntent.putExtra(EXTRA_ATTEMPT, previousAttempts)
            return serviceIntent
        }

        /**
         * calculate exponential backoff time in milliseconds for the given attempt
         * @param attempt the attempt for which to calculate the backoff time
         */
        internal fun exponentialBackoffMillis(attempt: Int) = Math.pow(2.0, attempt.toDouble()).toLong() * 1000
        internal const val MAX_ATTEMPTS = 10
        internal const val EXTRA_REASON_FOR_FAILURE = "reasonForFailure"
        internal const val EXTRA_ATTEMPT = "attempt"
        internal const val EXTRA_REFERRER = "referrer"
        internal val TAG = XNGAdTrackerIntentService::class.java.simpleName
        internal val PREFERENCE_FILE_NAME = XNGAdTrackerIntentService::class.java.name

        /**
         * builds an intent for a subsequent tracking attempt from the information stored by the previous unsuccessful
         * attempt.
         * @return the intent or null if no information was stored
         */
        fun getRetryIntent(context: Context): Intent? {
            val prefs = context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)
            val referrer = prefs.getString(EXTRA_REFERRER, null)
            val attempt = prefs.getInt(EXTRA_ATTEMPT, 0)
            return if (!TextUtils.isEmpty(referrer)) buildIntent(context, referrer, attempt) else null
        }
    }
    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        val referrer = intent.getStringExtra(EXTRA_REFERRER)
        val attempt = intent.getIntExtra(EXTRA_ATTEMPT, 0)
        Logger.log(TAG, "onHandleIntent: ", referrer)

        trackInstallReferrer(referrer, attempt)

        XNGAdTrackerReceiver.completeWakefulIntent(intent)
    }

    /**
     * performs the actual tracking call if preconditions are met and defers the call on failure
     */
    internal fun trackInstallReferrer(referrer: String, attempt: Int) {
        if (!isConnected(this, true)) {
            Logger.log(TAG, "trackInstallReferrer first attempt: not connected")
            deferTrackInstallReferrer(referrer, attempt, TrackingResults.FAILED_CONNECTIVITY)
            return
        }

        val result = xngAdTracker.trackAppInstall(this, referrer)
        when (result) {
            TrackingResults.RESULT_SUCCESS, TrackingResults.FAILED_UNRECOVARABLE -> cleanup()
            else -> deferTrackInstallReferrer(referrer, attempt, result)
        }
    }

    /**
     * used to allow injecting context for testing
     */
    internal fun injectBaseContext(base: Context): XNGAdTrackerIntentService {
        attachBaseContext(base)
        return this
    }

    /**
     * defers the tracking to a later attempt after a failure, storing the information so that it can be retrieved for the
     * next attempt
     *
     * @param referrer the referrer information
     * @param attemptsBefore the number of (unsuccessful) attempts before the current failure
     * @param reasonForFailure the reason for the failure (may be taken into account for the retry strategy)
     */
    private fun deferTrackInstallReferrer(referrer: String, attemptsBefore: Int, @TrackingResult reasonForFailure: Long) {
        val attempt = attemptsBefore + 1
        Logger.log(TAG, "deferTrackInstallReferrer attempt ", attempt)
        if (attempt > MAX_ATTEMPTS) {
            Logger.log(TAG, "number of attempts exhausted")
            // give up
            cleanup()
            return
        }

        storeTrackingParameters(referrer, reasonForFailure, attempt)

        when (reasonForFailure) {
            TrackingResults.FAILED_CONNECTIVITY -> {
                // failed due to missing connectivity: register to be notified when connectivity comes back
                ConnectivityReceiver.setEnabled(this, true)
            }
            TrackingResults.FAILED_SERVER_RESPONSE -> {
                // failed due to server response: try again in a little while with exponential backoff
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val nextAttemptTime = System.currentTimeMillis() + exponentialBackoffMillis(attempt)
                val pendingIntent = PendingIntent.getService(this,
                        1,
                        buildIntent(this, referrer, attempt),
                        PendingIntent.FLAG_ONE_SHOT)
                Logger.log(TAG, "deferTrackInstallReferrer setting alarm to ", Date(nextAttemptTime))
                alarmManager.set(AlarmManager.RTC, nextAttemptTime, pendingIntent)
            }
        }
    }

    /**
     * stores information about a tracking attempt in the shared preferences, so that it can be retrieved for
     * a potential next attempt
     */
    private fun storeTrackingParameters(referrer: String, reasonForFailure: Long, attempt: Int) {
        getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE).edit().putString(EXTRA_REFERRER, referrer)
                .putLong(EXTRA_REASON_FOR_FAILURE, reasonForFailure)
                .putInt(EXTRA_ATTEMPT, attempt).apply()
    }

    /**
     * call cleanup when tracking is done or retries are exhausted to clear out shared preferences and make
     * sure the connectivity receiver is disabled
     */
    private fun cleanup() {
        Logger.log(TAG, "clearing stored values")
        // remove shared preferences, if any were set
        getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        ConnectivityReceiver.setEnabled(this, false)
    }

}