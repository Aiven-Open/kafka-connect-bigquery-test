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

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.storage.Storage;
import com.wepay.kafka.connect.bigquery.api.SchemaRetriever;
import com.wepay.kafka.connect.bigquery.config.BigQuerySinkConfig;
import com.wepay.kafka.connect.bigquery.exception.BigQueryConnectException;
import com.wepay.kafka.connect.bigquery.exception.BigQueryStorageWriteApiConnectException;
import com.wepay.kafka.connect.bigquery.utils.MockTime;
import com.wepay.kafka.connect.bigquery.utils.Time;
import com.wepay.kafka.connect.bigquery.write.storage.StorageApiBatchModeHandler;
import com.wepay.kafka.connect.bigquery.write.storage.StorageWriteApiBatchApplicationStream;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTaskContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

public class BigQueryStorageApiBatchSinkTaskTest {
    private static final SinkTaskPropertiesFactory propertiesFactory = new SinkTaskPropertiesFactory();
    Map<String, String> properties;
    private final AtomicLong spoofedRecordOffset = new AtomicLong();
    private final StorageWriteApiBatchApplicationStream mockedStorageWriteApiBatchStream = mock(
            StorageWriteApiBatchApplicationStream.class, CALLS_REAL_METHODS);
    private final StorageApiBatchModeHandler mockedBatchHandler = mock(StorageApiBatchModeHandler.class);

    BigQuery bigQuery = mock(BigQuery.class);

    Storage storage = mock(Storage.class);
    SinkTaskContext sinkTaskContext = mock(SinkTaskContext.class);
    SchemaRetriever schemaRetriever = mock(SchemaRetriever.class);
    SchemaManager schemaManager = mock(SchemaManager.class);
    Map<TableId, Table> cache = new HashMap<>();
    Time time = new MockTime();
    BigQuerySinkTask testTask = new BigQuerySinkTask(
            bigQuery, schemaRetriever, storage, schemaManager, cache, mockedStorageWriteApiBatchStream, mockedBatchHandler, time);
    BigQueryStorageWriteApiConnectException exception = new BigQueryStorageWriteApiConnectException("error 12345");
    final String topic = "test-topic";

    Map<TopicPartition, OffsetAndMetadata> mockedOffset = mock(Map.class);

    @Before
    public void setUp()  {
        reset(mockedStorageWriteApiBatchStream);
        properties = propertiesFactory.getProperties();

        properties.put(BigQuerySinkConfig.TOPICS_CONFIG, topic);
        properties.put(BigQuerySinkConfig.USE_STORAGE_WRITE_API_CONFIG, "true");
        properties.put(BigQuerySinkConfig.ENABLE_BATCH_MODE_CONFIG, "true");
        properties.put(BigQuerySinkConfig.DEFAULT_DATASET_CONFIG, "scratch");
        spoofedRecordOffset.set(0);
        mockedOffset.put(new TopicPartition(topic, 0), new OffsetAndMetadata(0));

        doNothing().when(mockedStorageWriteApiBatchStream).initializeAndWriteRecords(any(), any(), eq("dummyStream"));
        doNothing().when(mockedStorageWriteApiBatchStream).shutdown();
        doNothing().when(mockedBatchHandler).refreshStreams();
        when(mockedBatchHandler.updateOffsetsOnStream(any(), any())).thenReturn("dummyStream");
        when(mockedBatchHandler.getCommitableOffsets()).thenReturn(mockedOffset);
        testTask.initialize(sinkTaskContext);
    }

    @After
    public void teardown() {
        testTask.stop();
    }

    @Test
    public void testPut() throws Exception {
        CountDownLatch writeThreadStarted = new CountDownLatch(1);
        doAnswer(invocationOnMock -> {
            writeThreadStarted.countDown();
            return null;
        }).when(mockedStorageWriteApiBatchStream).initializeAndWriteRecords(any(), any(), any());

        testTask.start(properties);
        testTask.put(Collections.singletonList(spoofSinkRecord()));

        assertTrue(
            "Task did not initialize expected write thread for record in time",
            writeThreadStarted.await(1, TimeUnit.SECONDS)
        );
    }

    @Test
    public void testStart() {
        ScheduledExecutorService loadExecutor = mock(ScheduledExecutorService.class);
        testTask.loadExecutor = loadExecutor;
        testTask.start(properties);

        // Ensure that the task has scheduled exactly one load task
        ArgumentCaptor<Runnable> loadExecutorTask = ArgumentCaptor.forClass(Runnable.class);
        verify(loadExecutor, times(1)).scheduleAtFixedRate(loadExecutorTask.capture(), anyLong(), anyLong(), any());

        // Run that load task
        loadExecutorTask.getValue().run();

        // Ensure that the load task created a new stream
        verify(mockedBatchHandler, times(1)).refreshStreams();
    }

    @Test(expected = BigQueryStorageWriteApiConnectException.class)
    public void testBatchLoadFailure() throws InterruptedException {
        ScheduledExecutorService loadExecutor = mock(ScheduledExecutorService.class);
        testTask.loadExecutor = loadExecutor;
        testTask.start(properties);

        // Ensure that the task has scheduled exactly one load task
        ArgumentCaptor<Runnable> loadExecutorTask = ArgumentCaptor.forClass(Runnable.class);
        verify(loadExecutor, times(1)).scheduleAtFixedRate(loadExecutorTask.capture(), anyLong(), anyLong(), any());

        // Run that load task
        doThrow(exception).when(mockedBatchHandler).refreshStreams();
        loadExecutorTask.getValue().run();

        // Ensure that the failure of the load task has caused the load executor to be shut down
        verify(loadExecutor).shutdown();
        when(loadExecutor.isTerminated()).thenReturn(true);

        // Ensure that the load task failure is propagated by put()
        testTask.put(Collections.emptyList());
    }

    @Test(expected = BigQueryConnectException.class)
    public void testSimplePutException() throws Exception {
        testTask.start(properties);
        doThrow(exception).when(mockedStorageWriteApiBatchStream).initializeAndWriteRecords(any(), any(), eq("dummyStream"));

        testTask.put(Collections.singletonList(spoofSinkRecord()));
        try {
            while (true) {
                Thread.sleep(100);
                testTask.put(Collections.emptyList());
            }
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof BigQueryStorageWriteApiConnectException);
            throw e;
        }
    }

    @Test
    public void testPrecommit() {
        testTask.start(properties);
        testTask.preCommit(Collections.emptyMap());
        verify(mockedBatchHandler, times(1)).getCommitableOffsets();
    }


    @Test(expected = BigQueryStorageWriteApiConnectException.class)
    public void testStop() {
        testTask.start(properties);
        testTask.stop();

        verify(mockedStorageWriteApiBatchStream, times(1)).shutdown();

        testTask.put(Collections.singletonList(spoofSinkRecord()));
    }

    private SinkRecord spoofSinkRecord() {
        Schema basicValueSchema = SchemaBuilder
                .struct()
                .field("sink_task_test_field", Schema.STRING_SCHEMA)
                .build();
        Struct basicValue = new Struct(basicValueSchema);
        basicValue.put("sink_task_test_field", "sink task test row");

        return new SinkRecord(topic, 0, null, null,
                basicValueSchema, basicValue, spoofedRecordOffset.getAndIncrement(), null, TimestampType.NO_TIMESTAMP_TYPE);
    }

}
