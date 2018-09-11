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

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class QueryBuilderTest {

    @Test
    public void test_that_the_query_builder_build_query_as_expected() {
        String expected = "sf_metric:\"requests.count\" AND app:\"cms\" AND env:\"production\" AND version:\"1.0.0\" AND response_code:\"400\" AND uri:\"\\\\/v2\\\\/auth\\\\/iam\\\\-principal\"";
        String actual = SimpleQueryBuilder.create()
                .withMetricName("requests.count")
                .withQueryPair("app", "cms", true)
                .withQueryPair("response_code", "400", true)
                .withQueryPair("uri", "/v2/auth/iam-principal", true)
                .withQuerySegment("env:\"production\" AND version:\"1.0.0\"")
                .build();

        assertThat("The queries contain the same parts regardless of order",
                Arrays.asList(actual.split(" AND ")), containsInAnyOrder(expected.split(" AND ")));
    }

}
