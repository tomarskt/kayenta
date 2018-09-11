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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.netflix.kayenta.canary.providers.metrics.QueryPair;
import com.netflix.kayenta.canary.providers.metrics.SignalFxCanaryMetricSetQueryConfig;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.signalfx.service.SignalFxDataPoint;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.lang.Double.NaN;
import static org.junit.Assert.*;

public class SignalFxMetricsServiceTest {

    private SignalFxMetricsService signalFxMetricsService;


    @Before
    public void before() {
        signalFxMetricsService = SignalFxMetricsService.builder().build();
    }

    @Test
    public void test_that_calculateIndex_calculates_index_properly() {
        long start = 1536785994100L;
        long resolution = 1000L;

        assertEquals(0, signalFxMetricsService.calculateIndex(start, resolution, 1536785994151L));
        assertEquals(5, signalFxMetricsService.calculateIndex(start, resolution, 1536785999151L));
    }

    @Test
    public void test_that_transformAndFillData_returns_the_expected_double_array_from_a_sparse_result() {
        long start = 10;
        long end = 15;
        long resolution = 1;
        Map<Long, Double> data = ImmutableMap.of(
                12L, 5D,
                14L, 10D
        );

        Double[] expected = { NaN, NaN, 5D, NaN, 10D, NaN };
        Double[] actual = signalFxMetricsService.transformAndFillData(start, end, resolution, data);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void test_that_transformAndFillData_returns_the_expected_double_array_from_a_complete_result() {
        long start = 10;
        long end = 15;
        long resolution = 1;
        Map<Long, Double> data = new HashMap<>();
        data.put(10L, 0D);
        data.put(11L, 1D);
        data.put(12L, 2D);
        data.put(13L, 3D);
        data.put(14L, 4D);
        data.put(15L, 5D);

        Double[] expected = { 0D, 1D, 2D, 3D, 4D, 5D };
        Double[] actual = signalFxMetricsService.transformAndFillData(start, end, resolution, data);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void test_that_reduceAndConvertTimeSeriesData_returns_the_expected_MetricSet() {
        long start = 10;
        long end = 15;
        long resolution = 1;
        long canaryStepLengthInSeconds = 6;
        String query = "find foo";
        String metricName = "foo";
        SignalFxCanaryMetricSetQueryConfig queryConfig = SignalFxCanaryMetricSetQueryConfig.builder()
                .metricName(metricName)
                .timeSeriesReducer(SignalFxTimeSeriesReducer.SUM)
                .queryPairs(ImmutableList.of(new QueryPair().setValue("bop").setKey("bam")))
                .build();

        List<List<SignalFxDataPoint>> timeSeriesData = ImmutableList.of(
                ImmutableList.of(
                        new SignalFxDataPoint(12L, 1D),
                        new SignalFxDataPoint(13L, 0D),
                        new SignalFxDataPoint(14L, 1D)
                ),
                ImmutableList.of(
                        new SignalFxDataPoint(10L, 5D),
                        new SignalFxDataPoint(12L, 1D),
                        new SignalFxDataPoint(13L, 0D),
                        new SignalFxDataPoint(14L, 1D)
                )
        );

        Double[] expectedData = { 5D, NaN, 2D, 0D, 2D, NaN };

        MetricSet metricSet = signalFxMetricsService
                .reduceAndConvertTimeSeriesData(timeSeriesData, metricName, queryConfig, query,
                        start, end, resolution, canaryStepLengthInSeconds);

        assertEquals(metricName, metricSet.getName());
        assertEquals(start, metricSet.getStartTimeMillis());
        assertEquals(Instant.ofEpochMilli(start).toString(), metricSet.getStartTimeIso());
        assertEquals(end, metricSet.getEndTimeMillis());
        assertEquals(Instant.ofEpochMilli(end).toString(), metricSet.getEndTimeIso());
        assertEquals(canaryStepLengthInSeconds * 1000, metricSet.getStepMillis());
        assertEquals(ImmutableMap.of("bam", "bop"), metricSet.getTags());
        assertEquals(ImmutableMap.of("query", query), metricSet.getAttributes());

        assertEquals(Arrays.asList(expectedData), metricSet.getValues());
    }

    @Test
    public void test_that_buildQuery_builds_the_expected_query() {
        String metricName = "my-awesome-metric";
        List<QueryPair> queryPairs = ImmutableList.of(
                new QueryPair().setKey("foo").setValue("bar")
        );
        String scope = "canary-scope:\"control\" AND env:\"production\"";

        String expected = "sf_metric:\"my-awesome-metric\" AND foo:\"bar\" AND canary-scope:\"control\" AND env:\"production\"";
        String actual = signalFxMetricsService.buildQuery(metricName, queryPairs, scope);

        assertEquals(expected, actual);
    }

    @Test
    public void test_that_getResolution_returns_1_seconds_for_steps_less_than_30_minutes() {
        long expected = Duration.ofSeconds(1).toMillis();
        assertEquals(expected, signalFxMetricsService.getResolution(TimeUnit.MINUTES.toSeconds(29)));
    }

    @Test
    public void test_that_getResolution_returns_1_minute_for_steps_less_than_1_day() {
        long expected = Duration.ofMinutes(1).toMillis();
        assertEquals(expected, signalFxMetricsService.getResolution(TimeUnit.HOURS.toSeconds(23)));
    }

    @Test
    public void test_that_getResolution_returns_5_minutes_for_steps_less_than_7_days() {
        long expected = Duration.ofMinutes(5).toMillis();
        assertEquals(expected, signalFxMetricsService.getResolution(TimeUnit.DAYS.toSeconds(6)));
    }

    @Test
    public void test_that_getResolution_returns_1_hour_for_steps_greater_than_or_equal_to_1_week() {
        long expected = Duration.ofHours(1).toMillis();
        assertEquals(expected, signalFxMetricsService.getResolution(TimeUnit.DAYS.toSeconds(7)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_that_validateResolution_throws_exception_on_non_valid_resolution() {
        signalFxMetricsService.validateResolution(12948L);
    }
}
