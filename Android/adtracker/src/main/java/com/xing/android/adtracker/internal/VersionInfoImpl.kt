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
import android.content.pm.PackageManager
import android.os.Build

/**
 * default implementation for version info abstraction, getting app info from the package
 * and OS version from static Build.* info
 */
class VersionInfoImpl : VersionInfo {
    override fun appVersion(context: Context) =
    try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName + "(" + packageInfo.versionCode + ")"

    } catch (e: PackageManager.NameNotFoundException) {
        "unknown"
    }

    override fun appId(context: Context) = context.packageName!!

    override fun osVersion() = Build.VERSION.SDK_INT
}