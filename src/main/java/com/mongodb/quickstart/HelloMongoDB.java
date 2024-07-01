package com.mongodb.quickstart;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.bson.Document;
import org.bson.json.JsonObject;
import org.bson.types.ObjectId;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class HelloMongoDB {

    public static void main(String[] args) throws FileNotFoundException {
        Random rand = new Random();
        String cnstr = "mongodb://batam:batam@localhost:27017/batam";
        MongoClient client = MongoClients.create(cnstr);
        MongoDatabase dbname = client.getDatabase("Sai");
        MongoCollection collection = dbname.getCollection("COVIDCASES");
        if(collection == null) {
            dbname.createCollection("COVIDCASES");
        }


//        String json = "[{CaseId:123,CaseName:hello},{Title:234,Description:hello1}]";
//        JsonObject sampleObject = new JsonObject(json);
        try {
            CSVParser parser = new CSVParser(new FileReader("C:\\Work\\sai\\java-quick-start\\src\\main\\resources\\all-states-history.csv"), CSVFormat.DEFAULT);
            List<CSVRecord> records = parser.getRecords();
            CSVRecord header = records.get(0);
            for(int i=1;i<records.size();i++){
                CSVRecord record = records.get(i);
                Iterator headerIterator = header.iterator();
                Iterator recordIterator = record.iterator();
                Document caseDoc = new Document("_id", new ObjectId());
                while(headerIterator.hasNext() && recordIterator.hasNext()) {
                    String columnName = headerIterator.next().toString();
                    caseDoc.append(columnName,recordIterator.next());
                }
                collection.insertOne(caseDoc);
            }
            aggregate(collection);

        }catch (Exception ex){
            ex.printStackTrace();
        }


        System.out.println("Hello MongoDB!");
    }

    public static void aggregate(MongoCollection<Document> collection){
        Document matchStage = new Document("$match", Filters.and(
                Filters.type("death", "string"),
                Filters.ne("death", "")
        ));

        Document addFieldsStage = new Document("$addFields", new Document("deaths", new Document("$toInt", "$death")));

        Document groupStage = new Document("$group", new Document("_id", "$state")
                .append("totalDeaths", new Document("$sum", "$deaths")));

        Document sortStage = new Document("$sort", Sorts.descending("totalDeaths"));

        Document limitStage = new Document("$limit", 5);

        // Construct the aggregation pipeline
        List<Document> pipeline = Arrays.asList(matchStage, addFieldsStage, groupStage, sortStage, limitStage);

        // Execute the aggregation pipeline
        collection.aggregate(pipeline).forEach((Document doc) -> {
            // Handle each result document here
            System.out.println(doc.toJson());
        });
    }
}
