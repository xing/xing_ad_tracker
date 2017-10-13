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

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.xing.android.adtracker.*

abstract class XNGAdTrackerReceiverBase
(internal var versionInfo: VersionInfo = VersionInfoImpl())
    : WakefulBroadcastReceiver() {

    /**
     * factory method to obtain an InstallReferrerTrackingScheduler implementation suitable for the platform and/or configuration
     */
    @SuppressLint("NewApi")
    protected fun buildReferrerTracker(context: Context): InstallReferrerTrackingScheduler =
            if (versionInfo.osVersion() >= Build.VERSION_CODES.LOLLIPOP)
                XNGAdTrackingSchedulerV21(context)
            else
                XNGAdTrackingSchedulerPreV21(context)

    /**
     * fetches the list of BroadcastReceivers from the metadata of the XNGAdTrackerReceiver manifest entry and forwards the intent
     * to these receivers by calling their onReceive() method
     */
    protected fun forwardBroadcast(context: Context, intent: Intent) {
        try {
            val activityInfo = context.packageManager.getReceiverInfo(
                    ComponentName(context, XNGAdTrackerReceiver::class.java), PackageManager.GET_META_DATA)

            activityInfo.metaData.keySet().forEach {
                val receiverClassName = activityInfo.metaData.getString(it)
                receiverClassName?.let { if (receiverClassName.isNotEmpty()) callReceiver(context, intent, receiverClassName) }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.log(XNGAdTrackerReceiverBase.TAG, "error retrieving receiver metadata:\n", e)
        }
    }

    /**
     * calls another BroadcastReceiver implementation given by its class name by instantiating it with a default constructor call
     * and calling it's onReceive() method
     *
     * @param context context to use for the onReceive() call
     * @param intent intent to forward to the onReceive() call
     * @param receiverClassName class name of the BroadcastReceiver implementation to instantiate by
     *
     */
    private fun callReceiver(context: Context, intent: Intent, receiverClassName: String) {
        try {
            val receiverClass = Class.forName(receiverClassName)
            if (BroadcastReceiver::class.java.isAssignableFrom(receiverClass)) {
                Logger.log(XNGAdTrackerReceiverBase.TAG, "forwarding broadcast intent to receiver ", receiverClassName)
                val receiver = receiverClass.newInstance() as BroadcastReceiver
                receiver.onReceive(context, intent)
            }
        } catch (e: ClassNotFoundException) {
            Logger.log(XNGAdTrackerReceiverBase.TAG, "callReceiver failed with exception:\n", e)
        } catch (e: IllegalAccessException) {
            Logger.log(XNGAdTrackerReceiverBase.TAG, "callReceiver failed with exception:\n", e)
        } catch (e: InstantiationException) {
            Logger.log(XNGAdTrackerReceiverBase.TAG, "callReceiver failed with exception:\n", e)
        }
    }

    companion object {
        private val TAG = XNGAdTrackerReceiverBase::class.java.simpleName
    }
}