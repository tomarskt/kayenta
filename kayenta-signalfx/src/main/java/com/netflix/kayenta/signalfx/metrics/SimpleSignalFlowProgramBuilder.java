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

import com.netflix.kayenta.canary.providers.metrics.QueryPair;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Builds simple signal flow programs.
 */
public class SimpleSignalFlowProgramBuilder {

    private final String metricName;
    private final String aggregationMethod;

    private List<QueryPair> queryPairs;
    private List<String> filterSegments;
    private List<String> scopeKeys;

    private SimpleSignalFlowProgramBuilder(String metricName, String aggregationMethod) {
        this.metricName = metricName;
        this.aggregationMethod = aggregationMethod;
        queryPairs = new LinkedList<>();
        filterSegments = new LinkedList<>();
        scopeKeys = new LinkedList<>();
    }

    public static SimpleSignalFlowProgramBuilder create(String metricName,
                                                        String transformationMethod) {

        return new SimpleSignalFlowProgramBuilder(metricName, transformationMethod);
    }

    public SimpleSignalFlowProgramBuilder withQueryPair(QueryPair queryPair) {
        queryPairs.add(queryPair);
        return this;
    }

    public SimpleSignalFlowProgramBuilder withQueryPairs(Collection<QueryPair> queryPairs) {
        this.queryPairs.addAll(queryPairs);
        return this;
    }

    public SimpleSignalFlowProgramBuilder withScope(String scope) {

        scopeKeys.addAll(parseScopeKeysFromScope(scope));

        filterSegments.add(scope);
        return this;
    }

    private Collection<? extends String> parseScopeKeysFromScope(String scope) {
        List<String> scopeKeys = new LinkedList<>();

        Pattern pattern = Pattern.compile("filter\\('(.*?)'.*?'\\)");
        Matcher matcher = pattern.matcher(scope);
        while (matcher.find()) {
            scopeKeys.add(matcher.group(1));
        }

        return scopeKeys;
    }

    public String build() {

        StringBuilder program = new StringBuilder("data('").append(metricName).append("', filter=");


        List<String> filters = new LinkedList<>();

        if (queryPairs.size() > 1) {
            filters.add(queryPairs.stream()
                    .map(qp -> String.format("filter('%s', '%s')", qp.getKey(), qp.getValue()))
                    .collect(Collectors.joining(" and ")));
        }
        filters.addAll(filterSegments);

        program.append(String.join(" and ", filters)).append(")");

        program.append('.').append(aggregationMethod)
                .append("(by=['")
                .append(String.join("', '", scopeKeys))
                .append("'])");

        program.append(".publish()");
        return program.toString();
    }
}
