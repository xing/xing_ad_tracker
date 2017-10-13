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

import android.app.job.JobScheduler
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import com.xing.android.adtracker.InstallReferrerTrackingScheduler
import com.xing.android.adtracker.Logger


private val TAG = XNGAdTrackingSchedulerV21::class.java.simpleName

/**
 * {@link InstallReferrerTrackingScheduler} implementation for OS Version 21 (Lollipop) and above. Implemented by scheduling a job for
 * {@link XNGAdTrackerJobService} using JobScheduler API
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class XNGAdTrackingSchedulerV21(val context: Context) : InstallReferrerTrackingScheduler {

    override fun scheduleTracking(referrer: String): Boolean {
        Logger.log(TAG, "scheduleJob: referrer=", referrer)

        val job = XNGAdTrackerJobService.buildJob(context, referrer)
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val result = jobScheduler.schedule(job)
        Logger.log(TAG, "JobScheduler.schedule() returned ", if (result == JobScheduler.RESULT_SUCCESS) "success" else "failure")
        return result == JobScheduler.RESULT_SUCCESS
    }
}

