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
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import com.xing.android.adtracker.internal.XNGAdTrackerReceiverBase

/**
 * [BroadcastReceiver][android.content.BroadcastReceiver] implementation that initiates the conversion tracking for the
 * XING App Install Ads installation.
 *
 *
 * You must either add this receiver to the application's manifest, or call [XNGAdTrackerReceiver.handleBroadcastIntent]
 * from an already existing BroadcastReceiver that is registered for the com.android.vending.INSTALL_REFERRER action.
 *
 *
 * To add this receiver to the application's manifest, add the following lines within the `<application>` section of the manifest:
 * <pre>
 * {@code
 *     <receiver android:name="com.xing.android.adtracker.AppInstallAdTrackingReceiver"
 *          android:enabled="true"
 *          android:exported="true">
 *          <intent-filter>
 *              <action android:name="com.android.vending.INSTALL_REFERRER" />
 *          </intent-filter>
 *      </receiver>
 * }
 * </pre>
 * Only one install referrer receiver will be called by the Android system. Therefore, you should have only one BroadcastReceiver registered
 * in the manifest with the above intent filter configuration. In order to have additional receivers (e.g. from third party tracking
 * components) invoked, you can add a list of such other receivers in the metadata of the [XNGAdTrackerReceiver] in the manifest as follows
 * (the meta-data names can be arbitrary strings and the values must be the fully qualified class names of the BroadcastReceiver implementations
 * to invoke):
 * <pre>
 * {@code
 *      <receiver android:name="com.xing.android.adtracker.XNGAdTrackerReceiver"
 *          android:enabled="true" android:exported="true">
 *          <meta-data android:name="forward.to.receiver.1" android:value="com.example.anyadvertisedapp.Receiver"/>
 *          <meta-data android:name="forward.to.receiver.2" android:value="com.example.yet.another.Receiver"/>
 *          <intent-filter>
 *              <action android:name="com.android.vending.INSTALL_REFERRER" />
 *          </intent-filter>
 *      </receiver>
 * }
 * </pre>
 */
class XNGAdTrackerReceiver : XNGAdTrackerReceiverBase() {

    override fun onReceive(context: Context, intent: Intent) {

        if (ACTION_INSTALL_REFERRER != intent.action) {
            return
        }

        try {
            val referrer = intent.getStringExtra(EXTRA_REFERRER)
            Logger.log(TAG, "onReceive: referrer=", referrer)
            if (TextUtils.isEmpty(referrer)) {
                Logger.log(TAG, "no referrer, bailing out")
                return
            }

            val referrerParams = Uri.parse("?" + referrer)
            val shallHandle = UTM_SOURCE_VALUE_XING == referrerParams.getQueryParameter(UTM_SOURCE_PARAM)
            if (!shallHandle) {
                Logger.log(TAG, "referrer not from XING source, bailing out")
                return
            }

            buildReferrerTracker(context).scheduleTracking(referrer)
        }
        catch (e: Exception){
            Logger.log(TAG, "Exception occurred while processing referrer intent:\n", e)
        } finally {
            /*
             * if we have been called via {@link #handleBroadcastIntent(Context, Intent)}, do not forward the intent to other
             * receivers as we're not the main receiver registered in the manifest
             */
            if (!intent.getBooleanExtra(EXTRA_NO_BROADCAST_FORWARDING, false)) {
                forwardBroadcast(context, intent)
            }
        }
    }

    companion object {
        private val TAG = XNGAdTrackerReceiver::class.java.simpleName
        internal val UTM_SOURCE_PARAM = "utm_source"
        internal val UTM_SOURCE_VALUE_XING = "xing"
        internal const val EXTRA_NO_BROADCAST_FORWARDING = "com.xing.android.adtracker.EXTRA_NO_BROADCAST_FORWARDING"
        internal const val ACTION_INSTALL_REFERRER = "com.android.vending.INSTALL_REFERRER"
        internal const val EXTRA_REFERRER = "referrer"

        /**
         * If you have other third party install referrer receivers (i.e. by including other tracking libraries), the install referrer
         * broadcast will only reach the first receiver. Therefore, the broadcast must be forwarded to this receiver, which can be
         * done by calling this method.
         *
         * @param context the context parameter from the calling [BroadcastReceiver][android.content.BroadcastReceiver]'s
         * [ onReceive(Context, Intent)][android.content.BroadcastReceiver.onReceive] implementation
         * @param intent  the intent parameter from the calling [BroadcastReceiver][android.content.BroadcastReceiver]'s
         * [ onReceive(Context, Intent)][android.content.BroadcastReceiver.onReceive] implementation
         */
        @JvmStatic fun handleBroadcastIntent(context: Context, intent: Intent) {
            XNGAdTrackerReceiver().onReceive(context, Intent(intent).putExtra(EXTRA_NO_BROADCAST_FORWARDING, true))
        }

        @JvmStatic internal fun completeWakefulIntent(intent: Intent){
            WakefulBroadcastReceiver.completeWakefulIntent(intent)
        }
    }
}
