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

/**
 * abstraction interface for getting the application id and version and OS version, mainly to allow
 * mocking for tests
 */
interface VersionInfo{
    /**
     * @return application version string
     */
    fun appVersion(context: Context): String

    /**
     * application package id (must be the ID with which the app is listed in PlayStore, usually context.getPackageName()
     */
    fun appId(context: Context): String

    /**
     * the SDK integer of the Android version
     */
    fun osVersion(): Int
}
