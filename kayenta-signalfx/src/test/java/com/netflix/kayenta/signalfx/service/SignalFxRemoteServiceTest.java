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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import retrofit.converter.JacksonConverter;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SignalFxRemoteServiceTest {

    @Test
    public void test_that_signalfx_time_series_json_response_can_be_deserialized_successfully() throws Exception {
        InputStream response = getClass().getClassLoader().getResourceAsStream("signalfx-time-series-window-response.json");
        JacksonConverter converter = new JacksonConverter(new ObjectMapper());
        TypedInput typedInput = new TypedByteArray("application/json", ByteStreams.toByteArray(response));
        SignalFxTimeSeriesQueryResult result = (SignalFxTimeSeriesQueryResult) converter
                .fromBody(typedInput, SignalFxTimeSeriesQueryResult.class);

        assertNotNull(result);
        assertEquals(6, result.getTimeSeriesData().size());
    }

}
