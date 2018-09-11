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


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * SignalFx query builder that can take key value pairs (dimensions, tags, properties) and a metric name and joins them with ANDs.
 * This builder is not capable of building advanced queries with NOTs, ORs or grouping.
 *
 * @see <a href="https://developers.signalfx.com/docs/metric-data-overview#section-searching-for-metrics-dimensions-or-properties">The SignalFx query overview</a> for more details.
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/2.1/query-dsl-query-string-query.html#_reserved_characters">Elasticsearch reserved charecters</a> for chars that must be escaped with '\\'
 */
public class SimpleQueryBuilder {

    private String metricName;
    private Map<String, String> queryPairs;
    private List<String> querySegments = new LinkedList<>();

    public SimpleQueryBuilder() {
        queryPairs = new HashMap<>();
    }

    public static SimpleQueryBuilder create() {
        return new SimpleQueryBuilder();
    }

    public SimpleQueryBuilder withMetricName(String metricName) {
        this.metricName = metricName;
        return this;
    }

    public SimpleQueryBuilder withQueryPair(String key, String value, boolean escapeValue) {
        queryPairs.put(key, escapeValue ? escape(value) : value);
        return this;
    }

    public SimpleQueryBuilder withQuerySegment(String querySegment) {
        querySegments.add(querySegment);
        return this;
    }

    /**
     * Returns a String where those characters that TextParser expects to be
     * escaped are escaped by a preceding <code>\</code>.
     */
    private String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')'
                    || c == ':' || c == '^' || c == '[' || c == ']' || c == '\"'
                    || c == '{' || c == '}' || c == '~' || c == '*' || c == '?'
                    || c == '|' || c == '&' || c == '/') {
                sb.append("\\\\");
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public String build() {
        List<String> querySegments = new LinkedList<>();

        // add the metric name to the query
        if (isNotBlank(metricName)) {
            querySegments.add(String.format("sf_metric:\"%s\"", metricName));
        }

        querySegments.addAll(queryPairs.entrySet().stream()
                .map(kvEntry -> String.format("%s:\"%s\"", kvEntry.getKey(), kvEntry.getValue()))
                .collect(Collectors.toList()));

        querySegments.addAll(this.querySegments);

        return String.join(" AND ", querySegments);
    }
}
