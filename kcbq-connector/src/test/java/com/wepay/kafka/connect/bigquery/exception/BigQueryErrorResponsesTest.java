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

package com.wepay.kafka.connect.bigquery.exception;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.bigquery.BigQueryException;
import org.junit.jupiter.api.Test;

public class BigQueryErrorResponsesTest {

  @Test
  public void testIsAuthenticationError() {
    BigQueryException error = new BigQueryException(0, "......401.....Unauthorized error.....");
    assertTrue(BigQueryErrorResponses.isAuthenticationError(error));

    error = new BigQueryException(0, "......401.....Unauthorized error...invalid_grant..");
    assertTrue(BigQueryErrorResponses.isAuthenticationError(error));

    error = new BigQueryException(0, "......400........invalid_grant..");
    assertTrue(BigQueryErrorResponses.isAuthenticationError(error));

    error = new BigQueryException(0, "......400.....invalid_request..");
    assertTrue(BigQueryErrorResponses.isAuthenticationError(error));

    error = new BigQueryException(0, "......400.....invalid_client..");
    assertTrue(BigQueryErrorResponses.isAuthenticationError(error));

    error = new BigQueryException(0, "......400.....unauthorized_client..");
    assertTrue(BigQueryErrorResponses.isAuthenticationError(error));

    error = new BigQueryException(0, "......400.....unsupported_grant_type..");
    assertTrue(BigQueryErrorResponses.isAuthenticationError(error));

    error = new BigQueryException(0, "......403..Access denied error.....");
    assertFalse(BigQueryErrorResponses.isAuthenticationError(error));

    error = new BigQueryException(0, "......500...Internal Server Error...");
    assertFalse(BigQueryErrorResponses.isAuthenticationError(error));
  }
}
