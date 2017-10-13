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

import com.xing.android.adtracker.Logger
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

/**
 * default implementation of HttpRequester using HttpURLConnection
 */
class HttpURLConnectionRequester : HttpRequester {
    @Throws(IOException::class, MalformedURLException::class)
    override fun get(urlString: String, headers: Map<String, String>): Int {
        val url = URL(urlString)
        val urlConnection : HttpURLConnection = url.openConnection() as HttpURLConnection
        headers.forEach{(key, value) -> urlConnection.addRequestProperty(key, value)}

        urlConnection.requestMethod = "GET"
        urlConnection.connectTimeout = 10000
        urlConnection.readTimeout = 10000

        Logger.log("HttpURLConnectionRequester", "calling server URL ", url)
        urlConnection.connect()
        val responseCode = urlConnection.responseCode
        Logger.log("HttpURLConnectionRequester", "server response code ", responseCode)
        urlConnection.disconnect()
        return responseCode
    }
}
