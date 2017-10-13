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
import android.net.Uri
import android.os.Build
import com.xing.android.adtracker.*
import java.io.IOException
import java.net.MalformedURLException

/**
 * XNGAdTrackerInterface implementation that tracks the installation by calling the XING AdManager tracking endpoint
 */
class XNGAdTracker(private val versionInfo: VersionInfo, private val httpRequester: HttpRequester) : IXNGAdTracker {

    private val TRACKING_URL = "https://xing.com/rest/xas/ads/install"
    private val USER_AGENT_HEADER = "User-Agent"

    companion object {
        const val TRACKING_PARAM_APP_ID = "appid"
        const val TRACKING_PARAM_CONVERSION_ID = "googleadid"
        const val REFERRER_PARAM_CONVERSION_ID = "xing_conversion_id"
        const val USER_AGENT_PREFIX = "XING-TRACK-ANDROID/"
    }

    private val TAG = "XNGAdTracker"

    private val USER_AGENT_FORMAT = "$USER_AGENT_PREFIX%1\$s Device/%2\$s OS Version/%3\$s"

    @TrackingResult
    override fun trackAppInstall(context: Context, referrer: String):  Long {
        try {
            Logger.log(TAG, "trackAppInstall ", referrer, ", ", versionInfo.appId(context))
            val referrerParams = Uri.parse("?" + referrer)
            val adId = referrerParams.getQueryParameter(REFERRER_PARAM_CONVERSION_ID)
            val url = Uri.parse(TRACKING_URL).buildUpon()
                    .appendQueryParameter(TRACKING_PARAM_CONVERSION_ID, adId)
                    .appendQueryParameter(TRACKING_PARAM_APP_ID, versionInfo.appId(context))
                    .build().toString()

            val userAgent = String.format(USER_AGENT_FORMAT, versionInfo.appVersion(context), Build.PRODUCT, Build.VERSION.RELEASE)
            Logger.log(TAG, "userAgent: " + userAgent)

            val responseCode = httpRequester.get(url, mapOf(USER_AGENT_HEADER to userAgent))

            if (200 != responseCode) {
                return TrackingResults.FAILED_SERVER_RESPONSE
            }
            Logger.log(TAG, "trackAppInstall: success")
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            // won't be able to recover from this
            return TrackingResults.FAILED_UNRECOVARABLE
        } catch (e: IOException) {
            e.printStackTrace()
            return TrackingResults.FAILED_SERVER_RESPONSE
        }
        return TrackingResults.RESULT_SUCCESS
    }
}
