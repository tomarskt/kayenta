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

package com.netflix.kayenta.signalfx.metrics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.netflix.kayenta.signalfx.service.SignalFxDataPoint;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.minBy;
import static java.util.stream.Collectors.summingDouble;

/**
 * Kayenta expects data to be aggregated for a unique metric / tag, dimension combo per canary-scope.
 * SignalFx /v1/timeseries?query api returns the raw data sets un-aggregated.
 * This enum contains logic in the form of Stream collectors that can reduce the multiple data
 * sets into a single data set.
 *
 * @see <a href="https://developers.signalfx.com/reference#timeserieswindow">SignalFx /timeserieswindow API Docs</a> for more information
 */
public enum SignalFxTimeSeriesReducer {

    /**
     * Averages the data point values at any given epoch time stamp.
     */
    AVERAGE("avg", groupingBy(SignalFxDataPoint::getEpochTimeInMillis, averagingDouble(SignalFxDataPoint::getValue))),

    /**
     * Sums the data point values at any given epoch time stamp.
     */
    SUM("sum", groupingBy(SignalFxDataPoint::getEpochTimeInMillis, summingDouble(SignalFxDataPoint::getValue))),

    /**
     * Finds the minimum data point value at any given epoch time stamp.
     */
    MINIMUM("min", groupingBy(
            SignalFxDataPoint::getEpochTimeInMillis,
            collectingAndThen(
                    minBy(comparing(SignalFxDataPoint::getValue)),
                    dataPoint -> dataPoint.orElseThrow(() -> new RuntimeException("min optional was empty")).getValue()
            )
    )),

    /**
     * Finds the maximum data point value at any given epoch time stamp.
     */
    MAXIMUM("max", groupingBy(
            SignalFxDataPoint::getEpochTimeInMillis,
            collectingAndThen(
                    maxBy(comparing(SignalFxDataPoint::getValue)),
                    dataPoint -> dataPoint.orElseThrow(() -> new RuntimeException("max optional was empty")).getValue()
            )
    ));

    private String value;

    /**
     * Takes the time series results from the SignalFx v1/timeseries API and reduces it to a single aggregated time series.
     *
     * This can be made to return an ordered map (TreeMap) so that result.values() returns that data points.
     * However since the data has to be iterated over anyways to fill sparse data with NaNs it doesn't add much.
     * So the data is left unordered.
     *
     * @param timeSeriesData The data from the SignalFx v1/timeseries API call.
     * @return A map of epoch ts to reduced value.
     */
    public Map<Long, Double> reduce(List<List<SignalFxDataPoint>> timeSeriesData) {
        return timeSeriesData.stream()
                .flatMap(Collection::stream)
                .collect(collector);
    }

    private Collector<Object, Object, Map<Long, Double>> collector;

    @JsonValue
    public String toString() {
        return value;
    }

    SignalFxTimeSeriesReducer(String value, Collector collector) {
        this.value = value;
        this.collector = collector;
    }

    @JsonCreator
    SignalFxTimeSeriesReducer fromString(String value) {
        return Arrays.stream(SignalFxTimeSeriesReducer.values())
                .filter(method -> value.equalsIgnoreCase(method.toString()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        String.format("%s is not a valid SignalFx Time Series Reduction Method. Possible Values: [ '%s' ]",
                                value,
                                String.join("', '", Arrays.stream(SignalFxTimeSeriesReducer.values())
                                        .map(SignalFxTimeSeriesReducer::toString).collect(Collectors.toList()))
                        )
                ));
    }
}
