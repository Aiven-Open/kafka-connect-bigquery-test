/*
 * Copyright 2020 Confluent, Inc.
 *
 * This software contains code derived from the WePay BigQuery Kafka Connector, Copyright WePay, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.wepay.kafka.connect.bigquery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import com.wepay.kafka.connect.bigquery.api.SchemaRetriever;
import com.wepay.kafka.connect.bigquery.config.BigQuerySinkTaskConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BigQuerySinkConnectorTest {
  private static SinkPropertiesFactory propertiesFactory;

  @BeforeAll
  public static void initializePropertiesFactory() {
    propertiesFactory = new SinkPropertiesFactory();
  }

  @Test
  public void testTaskClass() {
    assertEquals(BigQuerySinkTask.class, new BigQuerySinkConnector().taskClass());
  }

  @Test
  public void testTaskConfigs() {
    Map<String, String> properties = propertiesFactory.getProperties();

    BigQuerySinkConnector testConnector = new BigQuerySinkConnector();

    testConnector.start(properties);

    for (int i : new int[]{1, 2, 10, 100}) {
      Map<String, String> expectedProperties = new HashMap<>(properties);
      List<Map<String, String>> taskConfigs = testConnector.taskConfigs(i);
      assertEquals(i, taskConfigs.size());
      for (int j = 0; j < i; j++) {
        expectedProperties.put(BigQuerySinkTaskConfig.TASK_ID_CONFIG, Integer.toString(j));
        assertEquals(
            expectedProperties,
            taskConfigs.get(j),
            "Connector properties should match task configs"
        );
        assertNotSame(
            properties,
            taskConfigs.get(j),
            "Properties should not be referentially equal to task config"
        );
        // A little overboard, sure, but since it's only in the ballpark of 10,000 iterations this
        // should be fine
        for (int k = j + 1; k < i; k++) {
          assertNotSame(
              taskConfigs.get(j),
              taskConfigs.get(k),
              "Task configs should not be referentially equal to each other"
          );
        }
      }
    }
  }

  @Test
  public void testConfig() {
    assertNotNull(new BigQuerySinkConnector().config());
  }

  @Test
  public void testVersion() {
    assertNotNull(new BigQuerySinkConnector().version());
  }

  // Doesn't do anything at the moment, but having this here will encourage tests to be written if
  // the stop() method ever does anything significant
  @Test
  public void testStop() {
    new BigQuerySinkConnector().stop();
  }

  // Would just use Mockito, but can't provide the name of an anonymous class to the config file
  public static class MockSchemaRetriever implements SchemaRetriever {
    @Override
    public void configure(Map<String, String> properties) {
      // Shouldn't be called
    }

    @Override
    public Schema retrieveKeySchema(SinkRecord record) {
      return null;
    }

    @Override
    public Schema retrieveValueSchema(SinkRecord record) {
      return null;
    }
  }
}
