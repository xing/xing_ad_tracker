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

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.support.annotation.RequiresApi
import android.text.TextUtils
import com.xing.android.adtracker.*
import java.util.concurrent.atomic.AtomicBoolean

private val TAG = XNGAdTrackerJobService::class.java.simpleName

/**
 * JobService to process tracking jobs. Will be used on API 21 and above only.
 * @param xngAdTracker  IXNGAdTracker instance to perform the actual tracking endpoint call
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
internal class XNGAdTrackerJobService(
        private val xngAdTracker: IXNGAdTracker = XNGAdTracker( VersionInfoImpl(),  HttpURLConnectionRequester())) : JobService() {

    private var needsReschedule = AtomicBoolean(true)

    companion object {
        internal const val EXTRA_REFERRER = "referrer"
        internal const val JOB_ID_TRACK_APP_INSTALL = 1

        internal fun buildJob(context: Context, referrer: String): JobInfo? {
            val extras = PersistableBundle()
            extras.putString(EXTRA_REFERRER, referrer)

            return JobInfo.Builder(JOB_ID_TRACK_APP_INSTALL, ComponentName(context, XNGAdTrackerJobService::class.java))
                    .setExtras(extras)
                    .setBackoffCriteria(1000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build()
        }
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Thread { jobFinished(params, processJob(params)) }.start()
        return true
    }

    override fun onStopJob(params: JobParameters) = needsReschedule.get()

    /**
     * actual job processing (sending the HTTP request)
     * @return true if the job needs to be rescheduled
     */
    internal fun processJob(params: JobParameters): Boolean {
        val referrer = params.extras.getString(EXTRA_REFERRER)

        return if (TextUtils.isEmpty(referrer)) {
            Logger.log(TAG, "jobWorker: no referrer")
            false
        } else {
            Logger.log(TAG, "jobWorker, referrer: ", referrer)
            val result = xngAdTracker.trackAppInstall(this, referrer!!)
            needsReschedule.set(result != TrackingResults.RESULT_SUCCESS && result != TrackingResults.FAILED_UNRECOVARABLE)
            Logger.log(TAG, "jobFinished(needsReschedule = ", needsReschedule.get(), ")")
            needsReschedule.get()
        }
    }
}