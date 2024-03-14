/*
 * Copyright 2024 Copyright 2022 Aiven Oy and
 * bigquery-connector-for-apache-kafka project contributors
 *
 * This software contains code derived from the Confluent BigQuery
 * Kafka Connector, Copyright Confluent, Inc, which in turn
 * contains code derived from the WePay BigQuery Kafka Connector,
 * Copyright WePay, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.wepay.kafka.connect.bigquery.retrieve;

import com.wepay.kafka.connect.bigquery.api.SchemaRetriever;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.sink.SinkRecord;

/**
 * Fetches the key Schema and value Schema from a Sink Record
 */
public class IdentitySchemaRetriever implements SchemaRetriever {

  @Override
  public void configure(Map<String, String> properties) {
  }

  @Override
  public Schema retrieveKeySchema(SinkRecord record) {
    return record.keySchema();
  }

  @Override
  public Schema retrieveValueSchema(SinkRecord record) {
    return record.valueSchema();
  }
}
