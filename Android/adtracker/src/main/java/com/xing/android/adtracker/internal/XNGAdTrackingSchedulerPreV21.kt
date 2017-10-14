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

import android.content.Context
import com.xing.android.adtracker.*

private val TAG = "XNGAdTrackerPreV21"
/**
 * {@link InstallReferrerTrackingScheduler} implementation for OS Version below 21 (Lollipop). Implemented by starting
 * {@link XNGAdTrackerIntentService}
 */
class XNGAdTrackingSchedulerPreV21(val context: Context) : InstallReferrerTrackingScheduler {

    override fun scheduleTracking(referrer: String): Boolean {
        try {
            val component = context.startService(XNGAdTrackerIntentService.buildIntent(context, referrer, 0))
            Logger.log(TAG, "trackReferrer(", referrer, ") => ", component)
            return component != null
        } catch (e: Exception) {
            Logger.log(TAG, "trackReferrer(", referrer, ") threw exception:\n", e)
        }
        return false
    }
}

