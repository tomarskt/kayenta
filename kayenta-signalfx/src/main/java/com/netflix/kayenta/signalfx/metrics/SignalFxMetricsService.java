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
import com.netflix.kayenta.canary.CanaryConfig;
import com.netflix.kayenta.canary.CanaryMetricConfig;
import com.netflix.kayenta.canary.CanaryScope;
import com.netflix.kayenta.canary.providers.metrics.QueryPair;
import com.netflix.kayenta.canary.providers.metrics.SignalFxCanaryMetricSetQueryConfig;
import com.netflix.kayenta.metrics.MetricSet;
import com.netflix.kayenta.metrics.MetricsService;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.signalfx.security.SignalFxNamedAccountCredentials;
import com.netflix.kayenta.signalfx.service.ErrorResponse;
import com.netflix.kayenta.signalfx.service.SignalFxDataPoint;
import com.netflix.kayenta.signalfx.service.SignalFxRemoteService;
import com.netflix.kayenta.signalfx.service.SignalFxMetricQueryRequestError;
import com.netflix.kayenta.signalfx.service.SignalFxTimeSeriesQueryResult;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import retrofit.RetrofitError;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Builder
@Slf4j
public class SignalFxMetricsService implements MetricsService {

    // Acceptable values are 1000 (1s), 60000 (1m), 300000 (5m), and 3600000 (1h).
    private static final List<Long> ACCEPTABLE_RESOLUTION_VALUES = ImmutableList.of(
            Duration.ofSeconds(1).toMillis(),
            Duration.ofMinutes(1).toMillis(),
            Duration.ofMinutes(5).toMillis(),
            Duration.ofHours(1).toMillis()
    );

    @NotNull
    @Singular
    @Getter
    private List<String> accountNames;

    @Autowired
    private final AccountCredentialsRepository accountCredentialsRepository;

    @Override
    public String getType() {
        return "signalfx";
    }

    @Override
    public boolean servicesAccount(String accountName) {
        return accountNames.contains(accountName);
    }

    @Override
    public List<MetricSet> queryMetrics(String accountName,
                                        CanaryConfig canaryConfig,
                                        CanaryMetricConfig canaryMetricConfig,
                                        CanaryScope canaryScope) {

        SignalFxNamedAccountCredentials accountCredentials =
                (SignalFxNamedAccountCredentials) accountCredentialsRepository.getOne(accountName)
                .orElseThrow(() -> new IllegalArgumentException("Unable to resolve account " + accountName + "."));

        String accessToken = accountCredentials.getCredentials().getAccessToken();
        SignalFxRemoteService remoteService = accountCredentials.getSignalFxRemoteService();
        SignalFxCanaryMetricSetQueryConfig queryConfig = (SignalFxCanaryMetricSetQueryConfig) canaryMetricConfig.getQuery();

        // Build query and fetch needed information for SignalFx Request
        String query = buildQuery(queryConfig.getMetricName(), queryConfig.getQueryPairs(), canaryScope.getScope());
        long startEpochMilli = canaryScope.getStart().toEpochMilli();
        long endEpochMilli = canaryScope.getEnd().toEpochMilli();
        long canaryStepLengthInSeconds = canaryScope.getStep();

        // Determine and validate the data resolution to use for the query
        long resolutionMilli = Optional.ofNullable(queryConfig.getResolutionMillis())
                .orElseGet(() -> getResolution(canaryStepLengthInSeconds));
        validateResolution(resolutionMilli);

        // Fetch the data from SignalFx
        SignalFxTimeSeriesQueryResult queryResult;
        try {
            queryResult = remoteService.getTimeSeries(
                    accessToken,
                    query,
                    startEpochMilli,
                    endEpochMilli,
                    resolutionMilli
            );
        } catch (RetrofitError e) {
            ErrorResponse errorResponse = (ErrorResponse) e.getBodyAs(ErrorResponse.class);
            throw new SignalFxMetricQueryRequestError(errorResponse, accountCredentials.getEndpoint().getBaseUrl(),
                    query, startEpochMilli, endEpochMilli, resolutionMilli, accountName);
        }

        // Transform the data to what Kayenta wants
        MetricSet metricSet = reduceAndConvertTimeSeriesData(
                queryResult.getTimeSeriesData(),
                canaryMetricConfig.getName(),
                queryConfig,
                query,
                startEpochMilli,
                endEpochMilli,
                resolutionMilli,
                canaryStepLengthInSeconds
        );

        return Collections.singletonList(metricSet);
    }

    /**
     * SignalFX only accepts certain values for the resolution query param, this function validates
     * that we won't send an bad value.
     *
     * @param resolution The resolution that was defined in the canary config or automatically determined.
     */
    protected void validateResolution(long resolution) {
        if (!ACCEPTABLE_RESOLUTION_VALUES.contains(resolution)) {
            throw new IllegalArgumentException(String.format("The SignalFx time series resolution of '%s' was invalid. " +
                    "Acceptable values are 1000 (1s), 60000 (1m), 300000 (5m), and 3600000 (1h).", resolution));
        }
    }

    /**
     * Acceptable values are 1000 (1s), 60000 (1m), 300000 (5m), and 3600000 (1h).
     * Bucket the resolution to an acceptable value that is close to the provided step.
     *
     * This function is a best effort guess of what should work as defaults.
     *
     * @return the resolution to use for the time series window api call.
     */
    protected long getResolution(long canaryStepLengthInSeconds) {

        long canaryDurationMillis = Duration.ofSeconds(canaryStepLengthInSeconds).toMillis();

        if (canaryDurationMillis < Duration.ofMinutes(30).toMillis()) {
            return Duration.ofSeconds(1).toMillis();
        } else if (canaryDurationMillis < Duration.ofDays(1).toMillis()) {
            return Duration.ofMinutes(1).toMillis();
        } else if (canaryDurationMillis < Duration.ofDays(7).toMillis()) {
            return Duration.ofMinutes(5).toMillis();
        } else {
            return Duration.ofHours(1).toMillis();
        }
    }

    /**
     * Builds the query needed to call the /v1/timeseries endpoint to fetch data from SignalFx.
     *
     * @param metricName The metric name.
     * @param queryPairs A list of key value pairs that represent dimensions, properties or tags.
     * @param scopeQuerySegment The scope defined in the canary execution request.
     *                          EX: 'auto_scaling_group_name:"my-service-v1 AND env:"production"'
     * @return The properly escaped and built query for fetching SignalFx time series data.
     */
    protected String buildQuery(String metricName,
                                List<QueryPair> queryPairs,
                                String scopeQuerySegment) {

        SimpleQueryBuilder query = SimpleQueryBuilder.create()
                .withMetricName(metricName)
                .withQuerySegment(scopeQuerySegment);

        queryPairs.forEach(queryPair ->
                query.withQueryPair(queryPair.getKey(), queryPair.getValue(), queryPair.isEscapeValue()));

        return query.build();
    }

    /**
     * Takes the data retrieved from /v1/timeseries which has multiple collections of data,
     * typically one collection per host in a cluster, and reduces the data and transforms it to a format that
     * Kayenta can deal with.
     *
     * @param timeSeriesData The data from a SignalFx /v1/timeseries query
     * @param metricName The metric that was queried for
     * @param queryConfig The config for the query
     * @param query The query that was generated
     * @param startEpochMilli The canary scope start time
     * @param endEpochMilli The canary scope end time
     * @param resolutionMilli The determined resolution of the data
     * @param canaryStepLengthInSeconds The canary step time
     * @return The reduced and transformed MetricSet that can be used for judgement.
     */
    protected MetricSet reduceAndConvertTimeSeriesData(List<List<SignalFxDataPoint>> timeSeriesData,
                                                       String metricName,
                                                       SignalFxCanaryMetricSetQueryConfig queryConfig,
                                                       String query,
                                                       long startEpochMilli,
                                                       long endEpochMilli,
                                                       long resolutionMilli,
                                                       long canaryStepLengthInSeconds) {

        // Reduce the N sets of data to a single map of epoch timestamp to value, using the supplied reducer method.
        // This assumes that the timestamps are the same across sets and doesn't need to be normalized.
        Map<Long, Double> aggregatedData = queryConfig.getTimeSeriesReducer().reduce(timeSeriesData);

        // Take the reduced data and populate an array of doubles, filling missing data with NaN's
        Double[] data = transformAndFillData(startEpochMilli, endEpochMilli, resolutionMilli, aggregatedData);

        // Return a Metric set of the reduced and transformed data
        return MetricSet.builder()
                .name(metricName)
                .startTimeMillis(startEpochMilli)
                .startTimeIso(Instant.ofEpochMilli(startEpochMilli).toString())
                .endTimeMillis(endEpochMilli)
                .endTimeIso(Instant.ofEpochMilli(endEpochMilli).toString())
                .stepMillis(Duration.ofSeconds(canaryStepLengthInSeconds).toMillis())
                .values(Arrays.asList(data))
                .tags(queryConfig.getQueryPairs().stream().collect(Collectors.toMap(QueryPair::getKey, QueryPair::getValue)))
                .attribute("query", query)
                .build();
    }

    /**
     * Transforms the aggregated SignalFx data to an array of Doubles, filling missing data with NaN.
     *
     * @param startEpochMilli The canary scope start time
     * @param endEpochMilli The canary scope end time
     * @param resolutionMilli The determined resolution of the data
     * @param aggregatedData The aggregated SignalFx data
     * @return The aggregated data transformed into an array of doubles with missing data filled with Nan.
     */
    protected Double[] transformAndFillData(long startEpochMilli,
                                            long endEpochMilli,
                                            long resolutionMilli,
                                            Map<Long, Double> aggregatedData) {

        // This assumes that start and end are inclusive.
        int arraySize = new Double(Math.ceil((double) (endEpochMilli - startEpochMilli) / resolutionMilli + 1)).intValue();

        if (aggregatedData.size() > arraySize) {
            throw new RuntimeException("There seems to be more data than expected, maybe the timestamps do need to be normalized before being reduced");
        }

        Double[] data = new Double[arraySize];
        Arrays.fill(data, Double.NaN);
        aggregatedData.forEach((epochTsMilli, value) -> {
            int idx = calculateIndex(startEpochMilli, resolutionMilli, epochTsMilli);
            if (idx < 0 || idx > arraySize - 1) {
                log.error("SignalFx returned a ts outside the expected range. data point ts: {}, expected range: {} - {}",
                        epochTsMilli, startEpochMilli, endEpochMilli);
                return;
            }
            data[idx] = value;
        });
        return data;
    }

    /**
     * Calculates the index where the given timestamp should go.
     *
     * @param startEpochMilli The canary scope start time
     * @param resolutionMilli The determined resolution of the data
     * @param tsEpochMilli The timestamp for the current datapoint that needs to be placed in the data array.
     * @return The index where the given timestamp should go.
     */
    protected int calculateIndex(long startEpochMilli, long resolutionMilli, long tsEpochMilli) {
        return new Double(Math.floor((double) tsEpochMilli / resolutionMilli)
                - Math.floor((double) startEpochMilli / resolutionMilli)).intValue();
    }
}
