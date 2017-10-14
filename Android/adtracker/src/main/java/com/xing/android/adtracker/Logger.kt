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

import android.text.TextUtils
import android.util.Log

/**
 * logging wrapper to enable / disable logging e.g. for debugging purposes
 */
object Logger {

    @JvmStatic var loggingEnabled = false

    fun log(tag: String, vararg parts: Any) {
        if (!loggingEnabled) {
            return
        }
        Log.d(tag, TextUtils.join("", parts))
    }
}