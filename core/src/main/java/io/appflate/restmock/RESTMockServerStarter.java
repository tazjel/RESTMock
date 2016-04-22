/*
 * Copyright (C) 2016 Appflate.io
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.appflate.restmock;

import com.squareup.okhttp.mockwebserver.MockResponse;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class RESTMockServerStarter {
    public static final int KEEP_ALIVE_TIME = 60;
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1,
            1,
            KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(1));

    private RESTMockServerStarter() {
    }

    public static void startSync(final RESTMockFileParser mocksFileParser) {
        // it has to be like that since Android prevents starting testServer on main Thread.
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RESTMockServer.init(mocksFileParser);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        try {
            threadPoolExecutor.shutdown();
            if (!threadPoolExecutor.awaitTermination(KEEP_ALIVE_TIME, TimeUnit.SECONDS)) {
                throw new RuntimeException(
                        "mock server didnt manage to start within the given timeout (60 seconds)");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}