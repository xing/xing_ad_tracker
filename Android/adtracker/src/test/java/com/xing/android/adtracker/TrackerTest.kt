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

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import com.nhaarman.mockito_kotlin.*
import com.xing.android.adtracker.internal.*
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.*


class OtherReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        calledIntents.add(intent)
    }

    companion object {
        var calledIntents = ArrayList<Intent>()
    }
}

@RunWith(RobolectricTestRunner::class)
class TrackerTest {
    private val TEST_CONVERSION_ID = "test_conversion_id"
    private val TEST_APP_ID = "com.example.appid"
    private val TEST_APP_VERSION = "1.0.3"

    private val stringStringMapArgumentCaptor: KArgumentCaptor<Map<String, String>> = argumentCaptor()

    @Mock private lateinit var mockHttpRequester: HttpRequester
    @Mock private lateinit var mockVersionInfo: VersionInfo
    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockPackageManager: PackageManager
    @Mock private lateinit var mockJobScheduler: JobScheduler

    private val packageInfo = PackageInfo()

    init {
        packageInfo.apply {
            packageName = TEST_APP_ID
            versionCode = 4711
            versionName = "1.1.0"
        }
    }

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        whenever(mockVersionInfo.appId(any())).thenReturn(TEST_APP_ID)
        whenever(mockVersionInfo.appVersion(any())).thenReturn(TEST_APP_VERSION)
        whenever(mockVersionInfo.osVersion()).thenReturn(Build.VERSION_CODES.LOLLIPOP)
        whenever(mockContext.packageManager).thenReturn(mockPackageManager)
        whenever(mockContext.packageName).thenReturn(TEST_APP_ID)
        whenever(mockPackageManager.getPackageInfo(eq(TEST_APP_ID), any())).thenReturn(packageInfo)
        whenever(mockContext.getSystemService(Context.JOB_SCHEDULER_SERVICE)).thenReturn(mockJobScheduler)
        val receiverInfo = ActivityInfo()
        receiverInfo.metaData = Bundle()
        whenever(mockPackageManager.getReceiverInfo(ComponentName(mockContext, XNGAdTrackerReceiver::class.java), PackageManager.GET_META_DATA))
                .thenReturn(receiverInfo)
    }

    /**
     * tests whether the [IXNGAdTracker.trackAppInstall] returns the expected results
     * depending on the possible server request results (OK, error or IOException)
     */
    @Test
    fun testServerResponses() {

        whenever(mockHttpRequester.get(any(), any())).thenReturn(200)
        val xngAdTracker = XNGAdTracker(mockVersionInfo, mockHttpRequester)
        var result = xngAdTracker.trackAppInstall(mockContext, buildReferrer())
        assertEquals("request succeeded when server responds with 200", TrackingResults.RESULT_SUCCESS, result)

        whenever(mockHttpRequester.get(any(), any())).thenReturn(500)
        result = xngAdTracker.trackAppInstall(mockContext, buildReferrer())
        assertEquals("request failed when server responds with error", TrackingResults.FAILED_SERVER_RESPONSE, result)

        whenever(mockHttpRequester.get(any(), any())).thenThrow(IOException("mock IO Exception"))
        result = xngAdTracker.trackAppInstall(mockContext, buildReferrer())
        assertEquals("request failed when IO exception occurred", TrackingResults.FAILED_SERVER_RESPONSE, result)
    }


    /**
     * tests whether calling [IXNGAdTracker.trackAppInstall] sets the correct parameters for
     * the HTTP request to the backend.
     */
    @Test
    fun testHttpRequestParameters() {

        whenever(mockHttpRequester.get(any(), any())).thenReturn(200)

        val xngAdTracker = XNGAdTracker(mockVersionInfo, mockHttpRequester)
        xngAdTracker.trackAppInstall(mockContext, buildReferrer())

        verifyHttpRequest()
    }

    /**
     * tests whether a service intent is started for the install tracking on platform versions below 21
     */
    @Test
    fun receiverStartsPreV21Service() {

        whenever(mockVersionInfo.osVersion()).thenReturn(Build.VERSION_CODES.KITKAT)

        val serviceIntent = XNGAdTrackerIntentService.buildIntent(mockContext, buildReferrer(), 0)
        whenever(mockContext.startService(eq(serviceIntent))).thenReturn(serviceIntent.component)

        val xngAdTrackerReceiver = XNGAdTrackerReceiver()
        xngAdTrackerReceiver.versionInfo = mockVersionInfo
        xngAdTrackerReceiver.onReceive(mockContext, buildReferrerIntent())

        val intentCaptor: KArgumentCaptor<Intent> = argumentCaptor()
        verify(mockContext, times(1)).startService(intentCaptor.capture())
        assertThat(intentCaptor.firstValue, IntentFilterMatcher(serviceIntent))
    }

    /**
     * tests whether the receiver forwards the intent to other receivers registered in its metadata
     */
    @Test
    fun receiverForwardsToOtherReceivers() {
        mockOtherRegisteredReceiverMetadata()

        val xngAdTrackerReceiver = XNGAdTrackerReceiver()
        xngAdTrackerReceiver.versionInfo = mockVersionInfo
        xngAdTrackerReceiver.onReceive(mockContext, buildReferrerIntent())
        assertEquals("all other registered receivers were called", 2, OtherReceiver.calledIntents.size.toLong())
    }

    /**
     * verifies that the receiver does not attempt to forward the intent to other receivers registered in its metadata when it is
     * called via the handleBroadcastIntent API
     */
    @Test
    fun receiverDoesNotForwardToOtherReceiversWhenCalledDirectly() {
        mockOtherRegisteredReceiverMetadata()

        XNGAdTrackerReceiver.handleBroadcastIntent(mockContext, buildReferrerIntent())
        assertEquals("other registered receivers were not called when invoked via handleBroadcastIntent", 0,
                OtherReceiver.calledIntents.size.toLong())
    }

    /**
     * verifies that JobScheduler is called properly to add a job for the install tracking on Android platform version 21 and above
     */
    @Test
    fun jobSchedulerIsSetupProperly() {

        val intent = buildReferrerIntent()

        whenever(mockJobScheduler.schedule(any())).thenReturn(JobScheduler.RESULT_SUCCESS)

        val xngAdTrackerReceiver = XNGAdTrackerReceiver()
        xngAdTrackerReceiver.versionInfo = mockVersionInfo
        xngAdTrackerReceiver.onReceive(mockContext, intent)

        val jobInfoArgumentCaptor: KArgumentCaptor<JobInfo> = argumentCaptor()
        verify(mockJobScheduler, times(1)).schedule(jobInfoArgumentCaptor.capture())

        assertTrue("job scheduled with correct referrer", jobInfoArgumentCaptor.firstValue.extras.get("referrer") == buildReferrer())
    }

    @Test
    fun v21ServiceJobSendsRequestCorrectly() {
        val jobParams = mock<JobParameters>()
        val extras = PersistableBundle()
        extras.putString(XNGAdTrackerJobService.EXTRA_REFERRER, buildReferrer())
        whenever(jobParams.extras).thenReturn(extras)

        XNGAdTrackerJobService(XNGAdTracker(mockVersionInfo, mockHttpRequester)).processJob(jobParams)
        verifyHttpRequest()
    }


    /**
     * verify that the pre-V21 service properly registers the request for connectivity receiver on connectivity failure
     */
    @Test
    fun preV21ServiceConnectivityFailure() {

        whenever(mockVersionInfo.osVersion()).thenReturn(Build.VERSION_CODES.KITKAT)

        mockConnectivityStatus(false)

        val mockSharedPreferencesEditor: SharedPreferences.Editor = mockSharedPreferencesEditor()

        val referrer = buildReferrer()
        XNGAdTrackerIntentService(XNGAdTracker(mockVersionInfo, mockHttpRequester)).injectBaseContext(mockContext)
                .trackInstallReferrer(referrer, 0)

        // service must store tracking info into the shared preferences for the next attempt
        val referrerCaptor: KArgumentCaptor<String> = argumentCaptor()
        val attemptCaptor: KArgumentCaptor<Int> = argumentCaptor()
        val reasonCaptor: KArgumentCaptor<Long> = argumentCaptor()
        verify(mockSharedPreferencesEditor, times(1)).putString(eq(XNGAdTrackerIntentService.EXTRA_REFERRER), referrerCaptor.capture())
        verify(mockSharedPreferencesEditor, times(1)).putLong(eq(XNGAdTrackerIntentService.EXTRA_REASON_FOR_FAILURE), reasonCaptor.capture())
        verify(mockSharedPreferencesEditor, times(1)).putInt(eq(XNGAdTrackerIntentService.EXTRA_ATTEMPT), attemptCaptor.capture())


        assertEquals("attempt should be 1", 1, attemptCaptor.firstValue)
        assertEquals("reason for failure should be FAILED_CONNECTIVITY}", TrackingResults.FAILED_CONNECTIVITY, reasonCaptor.firstValue)
        assertEquals("referrer should be \'$referrer\'", referrer, referrerCaptor.firstValue)

        verify(mockPackageManager, times(1)).setComponentEnabledSetting(ComponentName(mockContext, ConnectivityReceiver::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    }

    /**
     * verify that the pre-V21 service properly schedules an alarm manager event on request failure
     */
    @Test
    fun preV21ServiceRequestFailureBackoff() {

        whenever(mockVersionInfo.osVersion()).thenReturn(Build.VERSION_CODES.KITKAT)
        mockConnectivityStatus(true)

        whenever(mockHttpRequester.get(any(), any())).thenReturn(500)

        val mockAlarmManager: AlarmManager = mock()
        whenever(mockContext.getSystemService(Context.ALARM_SERVICE)).thenReturn(mockAlarmManager)

        mockSharedPreferencesEditor()

        val referrer = buildReferrer()

        val attempt = 3
        val minTime = System.currentTimeMillis() + XNGAdTrackerIntentService.exponentialBackoffMillis(attempt + 1)
        // allow some time for processing
        val maxTime = minTime + 5000L

        XNGAdTrackerIntentService(XNGAdTracker(mockVersionInfo, mockHttpRequester)).injectBaseContext(mockContext)
                .trackInstallReferrer(referrer, attempt)

        val typeCaptor: KArgumentCaptor<Int> = argumentCaptor()
        val timeCaptor: KArgumentCaptor<Long> = argumentCaptor()

        verify(mockAlarmManager, times(1)).set(typeCaptor.capture(), timeCaptor.capture(), any())

        assertEquals("Alarm type shall be RTC", AlarmManager.RTC, typeCaptor.firstValue)
        assertTrue("Alarm time shall correspond to attempt backoff",
                timeCaptor.firstValue in minTime..maxTime)
        // pendingIntent cannot be verified as framework stubs will not create it

        XNGAdTrackerIntentService(XNGAdTracker(mockVersionInfo, mockHttpRequester)).injectBaseContext(mockContext)
                .trackInstallReferrer(referrer, attempt)
    }

    /**
     * verify that the pre-V21 service properly schedules an alarm manager event on request failure
     */
    @Test
    fun preV21ServiceGivesUpAfterMaxAttempts() {

        whenever(mockVersionInfo.osVersion()).thenReturn(Build.VERSION_CODES.KITKAT)
        mockConnectivityStatus(true)

        val mockSharedPreferencesEditor = mockSharedPreferencesEditor()
        val referrer = buildReferrer()
        val attempt = 10 // set to MAX_ATTEMPTS

        // 1. test in case that server request fails in last attempt
        val mockAlarmManager: AlarmManager = mock()
        whenever(mockContext.getSystemService(Context.ALARM_SERVICE)).thenReturn(mockAlarmManager)

        whenever(mockHttpRequester.get(any(), any())).thenReturn(500)
        XNGAdTrackerIntentService(XNGAdTracker(mockVersionInfo, mockHttpRequester)).injectBaseContext(mockContext)
                .trackInstallReferrer(referrer, attempt)

        // check that the shared preferences were cleared
        verify(mockSharedPreferencesEditor, times(1)).clear()

        // check that no alarm was set
        verify(mockAlarmManager, never()).set(any(), any(), any())

        // 2. test in case that last attempt hits no connectivity
        mockConnectivityStatus(true)
        XNGAdTrackerIntentService(XNGAdTracker(mockVersionInfo, mockHttpRequester)).injectBaseContext(mockContext)
                .trackInstallReferrer(referrer, attempt)

        // check that no receiver was registered
        verify(mockContext, never()).registerReceiver(any(), any())
    }

    @Test
    fun exponentialBackoffCalculation() {
        assertEquals("exponential backoff time is calculated properly", 16000, XNGAdTrackerIntentService.exponentialBackoffMillis(4))
        assertEquals("exponential backoff time is calculated properly", 64000, XNGAdTrackerIntentService.exponentialBackoffMillis(6))
        assertEquals("exponential backoff time is calculated properly", 1024000, XNGAdTrackerIntentService.exponentialBackoffMillis(10))
    }

    /**
     * verify that a HTTP request with correct URL and headers was sent
     */
    private fun verifyHttpRequest() {
        val urlArgumentCaptor: KArgumentCaptor<String> = argumentCaptor()

        verify(mockHttpRequester, times(1)).get(urlArgumentCaptor.capture(), stringStringMapArgumentCaptor.capture())

        assertThat<String>("Request has necessary URI parameters", urlArgumentCaptor.firstValue,
                object : BaseMatcher<String>() {
                    override fun matches(item: Any): Boolean {
                        val uri = Uri.parse(item.toString())
                        return uri.getQueryParameter(XNGAdTracker.TRACKING_PARAM_APP_ID) == TEST_APP_ID
                                && uri.getQueryParameter(XNGAdTracker.TRACKING_PARAM_CONVERSION_ID) == TEST_CONVERSION_ID
                    }

                    override fun describeTo(description: Description) {}
                })

        val allValues = stringStringMapArgumentCaptor.allValues
        assertEquals("Headers were set", 1, allValues.size.toLong())
        val userAgent = allValues.first()["User-Agent"]

        assertTrue("Request has User Agent with required prefix set",
                userAgent != null && userAgent.startsWith(XNGAdTracker.USER_AGENT_PREFIX))
    }

    // mock helpers

    /**
     * mock the SharedPreferences.Editor so arguments can be captured
     */
    @SuppressLint("CommitPrefEdits")
    private fun mockSharedPreferencesEditor(): SharedPreferences.Editor {
        val mockSharedPreferences: SharedPreferences = mock()
        val mockSharedPreferencesEditor: SharedPreferences.Editor = mock()
        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockSharedPreferences)
        whenever(mockSharedPreferences.edit()).thenReturn(mockSharedPreferencesEditor)
        whenever(mockSharedPreferencesEditor.putString(any(), any())).thenReturn(mockSharedPreferencesEditor)
        whenever(mockSharedPreferencesEditor.putInt(any(), any())).thenReturn(mockSharedPreferencesEditor)
        whenever(mockSharedPreferencesEditor.putLong(any(), any())).thenReturn(mockSharedPreferencesEditor)
        whenever(mockSharedPreferencesEditor.clear()).thenReturn(mockSharedPreferencesEditor)
        return mockSharedPreferencesEditor
    }

    /**
     * mocks other receivers registered in the metadata of the XNGAdTrackerReceiver manifest entry
     */
    private fun mockOtherRegisteredReceiverMetadata() {
        val receiverInfo = ActivityInfo()
        receiverInfo.metaData = Bundle()
        receiverInfo.metaData.putString("otherReceiver1", OtherReceiver::class.java.name)
        receiverInfo.metaData.putString("otherReceiver2", OtherReceiver::class.java.name)
        OtherReceiver.calledIntents.clear()
        whenever(mockPackageManager.getReceiverInfo(ComponentName(mockContext, XNGAdTrackerReceiver::class.java), PackageManager.GET_META_DATA))
                .thenReturn(receiverInfo)
    }

    /**
     * mocks the ConnectivityManager's activeNetworkInfo
     */

    private fun mockConnectivityStatus(connected: Boolean) {
        val mockConnectivityManager: ConnectivityManager = mock()
        whenever(mockContext.getSystemService(eq(Context.CONNECTIVITY_SERVICE))).thenReturn(mockConnectivityManager)

        val mockNetworkInfo: NetworkInfo = mock()
        whenever(mockConnectivityManager.activeNetworkInfo).thenReturn(mockNetworkInfo)

        whenever(mockNetworkInfo.isConnectedOrConnecting).thenReturn(connected)
        whenever(mockNetworkInfo.isConnected).thenReturn(connected)
    }

    private fun buildReferrerIntent()
            = Intent(XNGAdTrackerReceiver.ACTION_INSTALL_REFERRER).putExtra(XNGAdTrackerReceiver.EXTRA_REFERRER, buildReferrer())

    private fun buildReferrer()
            = "utm_source=xing&utm_medium=newsfeed&xing_conversion_id=" + TEST_CONVERSION_ID

}