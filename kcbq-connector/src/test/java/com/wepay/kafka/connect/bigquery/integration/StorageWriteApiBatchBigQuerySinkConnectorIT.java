package com.wepay.kafka.connect.bigquery.integration;

import com.wepay.kafka.connect.bigquery.config.BigQuerySinkConfig;
import java.util.Map;
import org.junit.jupiter.api.Tag;

@Tag("integration")
public class StorageWriteApiBatchBigQuerySinkConnectorIT extends StorageWriteApiBigQuerySinkConnectorIT {

  @Override
  protected Map<String, String> configs(String topic) {
    Map<String, String> result = super.configs(topic);
    result.put(BigQuerySinkConfig.ENABLE_BATCH_MODE_CONFIG, "true");
    result.put(BigQuerySinkConfig.COMMIT_INTERVAL_SEC_CONFIG, "15");
    return result;
  }

  @Override
  protected String topic(String basename) {
    return super.topic(basename + "-batch-mode");
  }

}
