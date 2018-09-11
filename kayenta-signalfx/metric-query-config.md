Example metric configuration for a canary config, in yaml for readability.

See [The integration test canary-config json](src/integration-test/resources/integration-test-canary-config.json) for a real example.

```yaml
name: Error Rate for /v1/some-endpoint
query:
  metricName: kayenta.integration-test.internal-server-errors
  queryPairs: # Can be dimensions, properties, or tags (Use tag as key for tags).
  - key: uri
    value: /v1/some-endpoint
  - key: status_code
    value: "5*"
    # Don't escape because * is a wild card, 
    # We want to capture all 5xx errors
    escapeValue: false # [Optional] Defaults to true, only include for special cases.
  # Reduce the N time series across each instance in a cluster to a single series
  # Supported options [ sum | mean | min | max ]
  timeSeriesReducer: sum # [Optional] Defaults to avg
  serviceType: signalfx
  type: signalfx
analysisConfigurations:
  canary:
    direction: increase
    # Fail the canary if server errors increase.
    critical: true
groups:
- Integration Test Group
scopeName: default
```

Given the above metric definition and the following canary scope `auto_scailing_group_name:"my_app_v1" AND environment:"production"`

The following query would be generated and sent to to `/v1/timeserieswindow` in the SignalFx API.

Query:
```
sf_metric:"kayenta.integration-test.internal-server-errors"
AND uri="\\/v1\\/some\\-endpoint"
AND status_code="5*"
AND auto_scailing_group_name:"my_app_v1" 
AND environment:"production"
```

This would return N sets of time series data, n = number of instances reporting this data (assuming you are reporting data with a unique dimension per host generating a ts per host per metric).

EX:
```json
{
  "data": {
    "ts-id-1": [
      [
        1536785994000,
        1
      ]
    ],
    "ts-id-2": [
      [
        1536785994000,
        0
      ]
    ],
    "ts-id-3": [
      [
        1536785994000,
        0
      ]
    ]
  }
}
```

This response is an example of 3 time series data sets being returned, and would be reduced to a single data set by applying the reduction technique defined in the query (ex: sum, adding all the values with the same epoch time stamp).

ex:

```json
{
  "1536785994000": 1
}
```

This reduced data set then gets converted to a MetricSet and is ready to be used in a judgement.
