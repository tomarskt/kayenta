/*
 * Copyright (c) 2018 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.kayenta.signalfx.service;

import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Query;

/**
 * Retrofit interface for SignalFx API calls.
 */
public interface SignalFxRemoteService {

    /**
     * Fetches time series data from SignalFx.
     *
     * @see <a href="https://developers.signalfx.com/reference#timeserieswindow">SignalFx /timeserieswindow API Docs</a>
     *
     * @param accessToken The SignalFx API Access token associated with the organization that you are querying.
     * @param query The Elasticsearch string query that specifies metric time series to retrieve.
     *              @see <a href="https://developers.signalfx.com/docs/metric-data-overview#section-searching-for-metrics-dimensions-or-properties">The SignalFx query overview</a> for more details.
     *              @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.1/query-dsl-query-string-query.html#_reserved_characters">Elasticsearch reserved charecters</a> for chars that must be escaped with '\\'
     * @param startEpochMilli Starting point of time window within which to find datapoints, in milliseconds since Unix epoch.
     * @param endEpochMilli End point of time window within which to find datapoints, in milliseconds since Unix epoch.
     * @param resolution The data resolution, in milliseconds, in which to return the data points.
     *                   Acceptable values are 1000 (1s), 60000 (1m), 300000 (5m), and 3600000 (1h).
     * @return SignalFxTimeSeries
     */
    @GET("/v1/timeserieswindow")
    SignalFxTimeSeriesQueryResult getTimeSeries(@Header("X-SF-TOKEN") String accessToken,
                                                @Query("query") String query,
                                                @Query("startMs") long startEpochMilli,
                                                @Query("endMs") long endEpochMilli,
                                                @Query("resolution") long resolution);

}
