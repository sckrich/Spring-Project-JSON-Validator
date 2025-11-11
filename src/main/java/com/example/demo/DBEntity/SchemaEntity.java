package com.example.demo.DBEntity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ODS_HUB_SCHEMAS")
public class SchemaEntity {
    
    @Id
    @Column(name = "SCHEMA_ID")
    private Integer schemaId;
    
    @Column(name = "SCHEMA_NAME")
    private String schemaName;
    
    @Column(name = "JSON_SCHEMA")
    @Lob
    private String jsonSchema;
    
    @Column(name = "DESCRIPTION")
    private String description;
    
    @Column(name = "CHG_DT")
    @UpdateTimestamp 
    private LocalDateTime chgDt;
    
    public SchemaEntity() {}
    
    public SchemaEntity(Integer schemaId, String schemaName, String jsonSchema, String description) {
        this.schemaId = schemaId;
        this.schemaName = schemaName;
        this.jsonSchema = jsonSchema;
        this.description = description;
    }
    
    public Integer getSchemaId() { return schemaId; }
    public void setSchemaId(Integer schemaId) { this.schemaId = schemaId; }
    
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    
    public String getJsonSchema() { return jsonSchema; }
    public void setJsonSchema(String jsonSchema) { this.jsonSchema = jsonSchema; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getChgDt() { return chgDt; }
    public void setChgDt(LocalDateTime chgDt) { this.chgDt = chgDt; }
}