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

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

class MatchableCallsRequestDispatcher extends Dispatcher {
    private List<MatchableCall> matchableCalls;
    private List<RecordedRequest> requestsHistory = new LinkedList<>();

    public MatchableCallsRequestDispatcher() {
        matchableCalls = new LinkedList<>();
    }

    @Override
    public MockResponse dispatch(RecordedRequest recordedRequest) throws InterruptedException {
        requestsHistory.add(recordedRequest);
        RESTMockServer.getLogger().log("-> New Request:\t" + recordedRequest);
        List<MatchableCall> matchedRequests = getMatchedRequests(recordedRequest);
        if (matchedRequests.size() == 1) {
            RESTMockServer.getLogger().log("<- Response:\t" + matchedRequests.get(0).response);
            return matchedRequests.get(0).response;
        } else if (matchedRequests.size() > 1) {
            String message = prepareTooManyMatchesMessage(recordedRequest, matchedRequests);
            RESTMockServer.getLogger().error("<- Response ERROR:\t" + message);
            return createErrorResponse(
                    new IllegalStateException(message));
        } else {
            RESTMockServer.getLogger().error("<- Response ERROR:\t" + RESTMockServer.RESPONSE_NOT_MOCKED + ": " + recordedRequest);
            MockResponse mockResponse =
                    new MockResponse().setResponseCode(500);
            if (!recordedRequest.getMethod().equals("HEAD")) {
                mockResponse.setBody(RESTMockServer.RESPONSE_NOT_MOCKED);
            }
            
            return mockResponse;
        }
    }

    private String prepareTooManyMatchesMessage(RecordedRequest recordedRequest,
                                                final List<MatchableCall> matchedRequests) {
        StringBuilder sb =
                new StringBuilder(RESTMockServer.MORE_THAN_ONE_RESPONSE_ERROR + recordedRequest + ": ");
        for (MatchableCall match : matchedRequests) {
            sb.append(match.requestMatcher.toString()).append("\n");
        }
        return sb.toString();
    }

    private List<MatchableCall> getMatchedRequests(RecordedRequest recordedRequest) {
        List<MatchableCall> matched = new LinkedList<>();
        for (MatchableCall request : matchableCalls) {
            if (request.requestMatcher.matches(recordedRequest)) {
                matched.add(request);
            }
        }
        return matched;
    }

    MockResponse createErrorResponse(Exception e) {
        MockResponse response = new MockResponse();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        response.setBody(sw.toString());
        response.setResponseCode(500);
        return response;
    }

    void addMatchableCall(MatchableCall matchableCall) {
        RESTMockServer.getLogger().log("## Adding new response for:\t" + matchableCall.requestMatcher);
        if (!matchableCalls.contains(matchableCall)) {
            matchableCalls.add(matchableCall);
        }
    }

    void removeAllMatchableCalls() {
        RESTMockServer.getLogger().log("## Removing all responses");
        matchableCalls.clear();
    }

    boolean removeMatchableCall(final MatchableCall call) {
        RESTMockServer.getLogger().log("## Removing response for:\t" + call.requestMatcher);
        return matchableCalls.remove(call);
    }

    List<RecordedRequest> getRequestHistory() {
        return requestsHistory;
    }


    void clearHistoricalRequests() {
        requestsHistory.clear();
    }
}