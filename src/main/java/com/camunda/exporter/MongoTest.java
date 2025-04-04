package com.camunda.exporter;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Arrays;

public class MongoTest {

    public static void main(String[] args) {
        String username = "admin";
        String password = "secret";
        String databaseName = "zeebe";
        String collectionName = "col_user";
        String host = "localhost";
        int port = 27017;

        // Create credentials
        MongoCredential credential = MongoCredential.createCredential(username, "admin", password.toCharArray());

        // Configure client settings
        MongoClientSettings settings = MongoClientSettings.builder()
                .credential(credential)
                .applyToClusterSettings(builder ->
                        builder.hosts(Arrays.asList(new ServerAddress(host, port))))
                .build();

        // Create a new client and connect to the server
        try (MongoClient mongoClient = MongoClients.create(settings)) {
            // Get the database
            MongoDatabase database = mongoClient.getDatabase(databaseName);

            // Get the collection
            MongoCollection<Document> collection = database.getCollection(collectionName);

            // Create a document to insert
            Document document = new Document("name", "John Doe")
                    .append("age", 30)
                    .append("city", "New York");

            // Insert the document into the collection
            collection.insertOne(document);

            System.out.println("Document inserted successfully.");

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

}
