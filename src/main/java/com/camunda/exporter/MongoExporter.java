package com.camunda.exporter;

import io.camunda.zeebe.exporter.api.Exporter;
import io.camunda.zeebe.exporter.api.context.Context;
import io.camunda.zeebe.exporter.api.context.Controller;
import io.camunda.zeebe.protocol.record.Record;

import io.camunda.zeebe.protocol.record.ValueType;
import org.slf4j.Logger;

public class MongoExporter implements Exporter {

    private Logger log;
    private MongoExporterConfiguration configuration;
    private Controller controller;
    private ZeebeMongoClient client;

    private long lastPosition = -1;
    private boolean colsCreated;

    @Override
    public void configure(Context context) {


        log = context.getLogger();

        configuration =
                context.getConfiguration().instantiate(MongoExporterConfiguration.class);
        log.debug("Exporter configured with {}", configuration);


    }


    @Override
    public void open(Controller controller) {
        log.info("Opening MongoDB Exporter");
        this.controller = controller;
        this.client = new ZeebeMongoClient(log, configuration);
    }

    @Override
    public void export(Record<?> record) {

        if (!colsCreated) {
            createCols();
        }

        if(! record.getValue().toString().contains("worker")) {
            log.info("Record : "+ record.getValue().toString());
        }

        client.insert(record);
        lastPosition = record.getPosition();

        if (client.shouldFlush()) {
            log.info("**** Flushing Records to MongoDB");
            flush();
        }
    }

    private void flush() {
        client.flush();
        controller.updateLastExportedRecordPosition(lastPosition);
    }

    private void createValueCol(final ValueType valueType) {
        if (!client.createCollection(valueType)) {
            log.warn("Put index template for value type {} was not acknowledged", valueType);
        }
    }

    private void createCols() {
        final MongoExporterConfiguration.ColConfiguration col = configuration.col;

        if (col.createCollections) {
            if (col.deployment) {
                createValueCol(ValueType.DEPLOYMENT);
            }
            if (col.error) {
                createValueCol(ValueType.ERROR);
            }
            if (col.incident) {
                createValueCol(ValueType.INCIDENT);
            }
            if (col.job) {
                createValueCol(ValueType.JOB);
            }
            if (col.jobBatch) {
                createValueCol(ValueType.JOB_BATCH);
            }
            if (col.message) {
                createValueCol(ValueType.MESSAGE);
            }
            if (col.messageSubscription) {
                createValueCol(ValueType.MESSAGE_SUBSCRIPTION);
            }
            if (col.variable) {
                createValueCol(ValueType.VARIABLE);
            }
            if (col.variableDocument) {
                createValueCol(ValueType.VARIABLE_DOCUMENT);
            }
            if (col.workflowInstance) {
                createValueCol(ValueType.PROCESS_INSTANCE);
            }
            if (col.workflowInstanceCreation) {
                createValueCol(ValueType.PROCESS_INSTANCE_CREATION);
            }
            if (col.workflowInstanceSubscription) {
                createValueCol(ValueType.PROCESS_MESSAGE_SUBSCRIPTION);
            }
        }

        colsCreated = true;
    }
}
