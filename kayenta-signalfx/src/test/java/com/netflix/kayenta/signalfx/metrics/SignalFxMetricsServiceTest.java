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

import com.google.common.io.ByteStreams;
import com.netflix.kayenta.signalfx.service.SignalFxConverter;
import com.netflix.kayenta.signalfx.service.SignalFlowExecutionResult;
import org.junit.Before;
import org.junit.Test;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;

import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class SignalFxMetricsServiceTest {

    private SignalFxMetricsService signalFxMetricsService;


    @Before
    public void before() {
        signalFxMetricsService = SignalFxMetricsService.builder().build();
    }

    @Test
    public void test_that_getMetricSetFromSignalFlowResult_returns_the_expected_MetricSet() throws Exception {
        InputStream response = getClass().getClassLoader().getResourceAsStream("signalfx-signalflow-response.text");
        SignalFxConverter converter = new SignalFxConverter();
        TypedInput typedInput = new TypedByteArray("text/plain", ByteStreams.toByteArray(response));
        SignalFlowExecutionResult executionResult = (SignalFlowExecutionResult) converter.fromBody(typedInput, SignalFlowExecutionResult.class);

//        MetricSet metricSet = signalFxMetricsService.getMetricSetFromSignalFlowResult(executionResult);


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
}
