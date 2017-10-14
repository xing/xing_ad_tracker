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

import android.content.Intent

import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * Hamcrest matcher that matches intents using [Intent.filterEquals]
 */
internal class IntentFilterMatcher(private val intent: Intent) : BaseMatcher<Intent>() {

    override fun matches(item: Any) = item is Intent && intent.filterEquals(item)

    override fun describeTo(description: Description) {
        description.appendValue(intent)
    }
}
