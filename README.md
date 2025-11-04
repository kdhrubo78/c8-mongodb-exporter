# Introduction

This repo contains a sample Camunda 8.8 Exporter for MongoDB

# Getting started with Camunda

Follow the instructions here - https://docs.camunda.io/docs/guides/getting-started-example/
to download and run Camunda 8.8 on your machine. 

# Build and run

- mvn package


# References

- https://docs.camunda.io/docs/self-managed/concepts/exporters/
- https://camunda.com/blog/2025/04/creating-testing-custom-exporters-camunda-8-run/


## Filter - User Task Only Exporter

```java
package com.example.exporter;

import io.camunda.zeebe.exporter.api.Controller;
import io.camunda.zeebe.exporter.api.Exporter;
import io.camunda.zeebe.exporter.api.context.Context;
import io.camunda.zeebe.exporter.api.context.RecordFilter;
import io.camunda.zeebe.protocol.record.Intent;
import io.camunda.zeebe.protocol.record.Record;
import io.camunda.zeebe.protocol.record.RecordType;
import io.camunda.zeebe.protocol.record.ValueType;
import io.camunda.zeebe.protocol.record.intent.UserTaskIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserTaskOnlyExporter implements Exporter {

  private static final Logger LOG = LoggerFactory.getLogger(UserTaskOnlyExporter.class);

  private Controller controller;

  // Filter: only EVENT + USER_TASK; accept all user task intents by default
  static final class UserTaskFilter implements RecordFilter {
    @Override
    public boolean acceptType(final RecordType recordType) {
      return recordType == RecordType.EVENT;
    }

    @Override
    public boolean acceptValue(final ValueType valueType) {
      return valueType == ValueType.USER_TASK;
    }

    @Override
    public boolean acceptIntent(final Intent intent) {
      // Limit to user task lifecycle events; widen/narrow as needed
      return intent instanceof UserTaskIntent;
    }
  }

  @Override
  public void configure(final Context context) {
    // Install the filter so the broker only forwards matching records
    context.setFilter(new UserTaskFilter());
  }

  @Override
  public void open(final Controller controller) {
    this.controller = controller;
  }

  @Override
  public void close() {
    // no-op
  }

  @Override
  public void export(final Record<?> record) {
    try {
      // Handle the user task record here
      // Example: log minimal envelope; cast value when you need typed fields
      LOG.info(
          "UserTask event: intent={} key={} position={} ts={}",
          record.getIntent(),
          record.getKey(),
          record.getPosition(),
          record.getTimestamp());

      // IMPORTANT: acknowledge last exported position to enable compaction
      controller.updateLastExportedRecordPosition(record.getPosition());
    } catch (final Exception e) {
      // Let the broker retry; keep operations idempotent
      LOG.error("Failed to export record at position {}", record.getPosition(), e);
      throw e;
    }
  }
}
```
