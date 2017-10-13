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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import com.xing.android.adtracker.Logger
import com.xing.android.adtracker.WakefulBroadcastReceiver

/**
 * Broadcast Receiver for connectivity changes, will be registered when no connectivity is available when an attempt is made
 */
internal class ConnectivityReceiver : WakefulBroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.log("ConnectivityReceiver", "onReceive: ", if (isConnected(context, false)) "connected" else "not connected")
        if (isConnected(context, false)) {
            setEnabled(context, false)
            val startIntent = XNGAdTrackerIntentService.getRetryIntent(context)
            startIntent?.let {
                startWakefulService(context, startIntent)
            }
        }

        // if still not connected, keep receiver enabled until connectivity comes back
    }

    companion object {
        private val TAG = ConnectivityReceiver::class.java.simpleName
        @JvmStatic internal fun setEnabled(context: Context, enabled: Boolean) {
            Logger.log(TAG, if (enabled) "enabling" else "disabling", " connectivity receiver...")
            context.packageManager.setComponentEnabledSetting(componentName(context),
                    if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
        }

        private fun componentName(context: Context) = ComponentName(context, ConnectivityReceiver::class.java)
    }

}


/**
 * returns connection state, will be checked before making a request
 */
internal fun isConnected(context: Context, acceptConnecting: Boolean): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && if (acceptConnecting) activeNetworkInfo.isConnectedOrConnecting else activeNetworkInfo.isConnected
}