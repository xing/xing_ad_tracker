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

package com.xing.android.adtracker

import android.content.Context
import android.support.annotation.IntDef

/**
 * results of tracking call attempt
 */
object TrackingResults {
    const val RESULT_SUCCESS = 0L
    const val FAILED_CONNECTIVITY = 1L
    const val FAILED_SERVER_RESPONSE = 2L
    const val FAILED_UNRECOVARABLE = 3L
}

@IntDef(TrackingResults.RESULT_SUCCESS, TrackingResults.FAILED_CONNECTIVITY, TrackingResults.FAILED_SERVER_RESPONSE, TrackingResults.FAILED_UNRECOVARABLE)
annotation class TrackingResult

/**
 * classes implementing this interface perform the actual referrer tracking call to the backend endpoint
 */
interface IXNGAdTracker {
    @TrackingResult fun trackAppInstall(context: Context, referrer: String): Long
}