# Introduction

This repo contains a sample Camunda 8.8 Exporter for MongoDB

# Getting started with Camunda

Follow the instructions here - https://docs.camunda.io/docs/guides/getting-started-example/
to download and run Camunda 8.8 on your machine. 

# Build and run

- mvn package


## Start MongoDB

- docker compose up 


# References

- Exporters - https://docs.camunda.io/docs/self-managed/concepts/exporters/
- Blog - https://camunda.com/blog/2025/04/creating-testing-custom-exporters-camunda-8-run/
- Install on K8S - https://docs.camunda.io/docs/self-managed/components/orchestration-cluster/zeebe/exporters/install-zeebe-exporters/

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


![Zeebe Exporter](/zeebe-exporterV2.svg "Zeebe Exporter")



- Only partition leaders export; followers never export. 
- Export is at‑least‑once and resumes from snapshot position.
- Leader: exports committed records via ExporterDirector; at‑least‑once delivery (resume from snapshot position)
- Followers: replica only; waits for leader to mark segments deletable.
- Follower: no exporting; retains replica and deletes segments only after leader marks safe

# Why you need idempotency ?
Exporting is at‑least‑once. 
After a leader failover, the new leader resumes from the last snapshot position, so some records may be exported again. 
Your sink must safely handle duplicates.  


A stable unique event key is the combination of `partitionId + position`; 
use this as the canonical identifier in your sink to deduplicate.  


e.g

```sql

CREATE TABLE zeebe_records (
  partition_id INT NOT NULL,
  position BIGINT NOT NULL,
  intent VARCHAR(64) NOT NULL,
  value_type VARCHAR(64) NOT NULL,
  ts TIMESTAMP NOT NULL,
  payload JSONB NOT NULL,
  PRIMARY KEY (partition_id, position)
);

```