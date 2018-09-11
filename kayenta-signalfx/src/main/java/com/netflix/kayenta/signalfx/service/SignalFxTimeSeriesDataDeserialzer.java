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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Custom deserializer for api response, so that it can be represented better with logical classes rather
 * than list of lists of lists, etc.
 */
public class SignalFxTimeSeriesDataDeserialzer extends StdDeserializer<List<List<SignalFxDataPoint>>> {

    public SignalFxTimeSeriesDataDeserialzer() {
        this(null);
    }

    protected SignalFxTimeSeriesDataDeserialzer(Class<?> vc) {
        super(vc);
    }

    @Override
    public List<List<SignalFxDataPoint>> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<List<SignalFxDataPoint>> timeSeriesData = new LinkedList<>();

        TreeNode node = p.getCodec().readTree(p);

        ((ObjectNode) node).fields().forEachRemaining(entry -> {
            JsonNode timeSeriesResponseData = entry.getValue();
            List<SignalFxDataPoint> dataPoints = new LinkedList<>();
            timeSeriesResponseData.forEach(epochTimeInMillisValuePair -> {
                long epochTimeInMillis = epochTimeInMillisValuePair.get(0).longValue();
                Double value = epochTimeInMillisValuePair.get(1).doubleValue();
                dataPoints.add(new SignalFxDataPoint().setValue(value).setEpochTimeInMillis(epochTimeInMillis));
            });
            timeSeriesData.add(new ArrayList<>(dataPoints));
        });

        return timeSeriesData;
    }
}
