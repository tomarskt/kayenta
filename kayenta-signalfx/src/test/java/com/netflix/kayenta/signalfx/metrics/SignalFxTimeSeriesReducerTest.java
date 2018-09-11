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
import com.netflix.kayenta.signalfx.service.SignalFxDataPoint;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SignalFxTimeSeriesReducerTest {

    private static final List<List<SignalFxDataPoint>> timeSeriesData = ImmutableList.of(
            ImmutableList.of(
                    new SignalFxDataPoint(12L, 1D),
                    new SignalFxDataPoint(13L, 0D),
                    new SignalFxDataPoint(14L, 1D)
            ),
            ImmutableList.of(
                    new SignalFxDataPoint(10L, 5D),
                    new SignalFxDataPoint(12L, 1D),
                    new SignalFxDataPoint(13L, 0D),
                    new SignalFxDataPoint(14L, 2D)
            )
    );

    @Test
    public void test_that_avg_collector_works_as_expected() {
        Map<Long, Double> expected = ImmutableMap.of(
                10L, 5D,
                12L, 1D,
                13L, 0D,
                14L, 1.5D
        );

        Map<Long, Double> actual = SignalFxTimeSeriesReducer.AVERAGE.reduce(timeSeriesData);

        assertEquals(expected, actual);
    }

    @Test
    public void test_that_sum_collector_works_as_expected() {
        Map<Long, Double> expected = ImmutableMap.of(
                10L, 5D,
                12L, 2D,
                13L, 0D,
                14L, 3D
        );

        Map<Long, Double> actual = SignalFxTimeSeriesReducer.SUM.reduce(timeSeriesData);

        assertEquals(expected, actual);
    }

    @Test
    public void test_that_min_collector_works_as_expected() {
        Map<Long, Double> expected = ImmutableMap.of(
                10L, 5D,
                12L, 1D,
                13L, 0D,
                14L, 1D
        );

        Map<Long, Double> actual = SignalFxTimeSeriesReducer.MINIMUM.reduce(timeSeriesData);

        assertEquals(expected, actual);
    }

    @Test
    public void test_that_max_collector_works_as_expected() {
        Map<Long, Double> expected = ImmutableMap.of(
                10L, 5D,
                12L, 1D,
                13L, 0D,
                14L, 2D
        );

        Map<Long, Double> actual = SignalFxTimeSeriesReducer.MAXIMUM.reduce(timeSeriesData);

        assertEquals(expected, actual);
    }

}
