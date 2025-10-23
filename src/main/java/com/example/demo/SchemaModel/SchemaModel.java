package com.example.demo.SchemaModel;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SchemaModel {
    private Integer id;
    private String name;
    private String uploadDate; 
    private JsonNode schema;
    
    public SchemaModel() {}
    
    /**
     * Creates a new SchemaModel with auto-generated upload date
     * @param id unique identifier for the schema
     * @param name human-readable name of the schema
     * @param schema JSON schema content as JsonNode
     */
    public SchemaModel(Integer id, String name, JsonNode schema) {
        this.id = id;
        this.name = name;
        this.schema = schema;
        this.uploadDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }
    
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getUploadDate() { return uploadDate; }
    public void setUploadDate(String uploadDate) { this.uploadDate = uploadDate; }
    
    public JsonNode getSchema() { return schema; }
    public void setSchema(JsonNode schema) { this.schema = schema; }
}