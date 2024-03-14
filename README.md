# Kafka Connect BigQuery Connector

This is an implementation of a sink connector from [Apache Kafka] to [Google BigQuery], built on top 
of [Apache Kafka Connect].

## History

This connector was [originally developed by WePay](https://github.com/wepay/kafka-connect-bigquery).
In late 2020 the project moved to [Confluent](https://github.com/confluentinc/kafka-connect-bigquery),
with both companies taking on maintenance duties.
In 2024, Aiven created [its own fork](https://github.com/Aiven-Open/bigquery-connector-for-apache-kafka/)
based off the Confluent project in order to continue maintaining an open source, Apache 2-licensed
version of the connector.

## Configuration

### Sample

An example connector configuration, that reads records from Kafka with
JSON-encoded values and writes their values to BigQuery:

```json
{
  "connector.class": "com.wepay.kafka.connect.bigquery.BigQuerySinkConnector",
  "topics": "users, clicks, payments",
  "tasks.max": "3",
  "value.converter": "org.apache.kafka.connect.json.JsonConverter",

  "project": "kafka-ingest-testing",
  "defaultDataset": "kcbq-example",
  "keyfile": "/tmp/bigquery-credentials.json"
}
```

### Complete docs
See [here](docs/sink-connector-config-options.rst) for a list of the connector's
configuration properties.

## Download

Releases are available in the GitHub release tab.
<!-- TODO:
  Mention first Aiven-published release (which will be the first to
  include executable artifacts)
-->

  [Apache Kafka Connect]: https://kafka.apache.org/documentation.html#connect
  [Apache Kafka]: http://kafka.apache.org
  [Google BigQuery]: https://cloud.google.com/bigquery/
  [Kafka]: http://kafka.apache.org
