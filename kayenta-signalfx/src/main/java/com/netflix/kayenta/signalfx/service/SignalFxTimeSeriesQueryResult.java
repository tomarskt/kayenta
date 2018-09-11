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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.List;

/**
 * Typed response for SignalFx time series window query requests.
 * Setting ignore unknown to true, because I can't figure out the error field type and get the api to populate it.
 * Errors seem to return an error response and never populate the field in the 200 responses anyways.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignalFxTimeSeriesQueryResult {

    @JsonProperty("data")
    @JsonDeserialize(using = SignalFxTimeSeriesDataDeserialzer.class)
    private List<List<SignalFxDataPoint>> timeSeriesData;

}
