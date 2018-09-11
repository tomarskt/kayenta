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

package com.netflix.kayenta.canary.providers.metrics;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.netflix.kayenta.canary.CanaryMetricSetQueryConfig;
import com.netflix.kayenta.signalfx.metrics.SignalFxTimeSeriesReducer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.LinkedList;
import java.util.List;

@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("signalfx")
public class SignalFxCanaryMetricSetQueryConfig implements CanaryMetricSetQueryConfig {

    @NotNull
    @Getter
    private String metricName;

    @Builder.Default
    @Getter
    private List<QueryPair> queryPairs = new LinkedList<>();

    @Builder.Default
    @Getter
    private SignalFxTimeSeriesReducer timeSeriesReducer = SignalFxTimeSeriesReducer.AVERAGE;

    @Getter
    private Long resolutionMillis;

    @Override
    public String getServiceType() {
        return "signalfx";
    }

}
