package com.example.demo.service;

import com.example.demo.SchemaModel.SchemaModel;
import com.example.demo.dto.JsonValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class JsonValidationService {

    private final ConcurrentHashMap<Long, SchemaModel> schemaStorage = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonValidationResult validateJson(JsonNode schemaNode, JsonNode dataNode) {
        try {
            JsonSchema validatorSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaNode);
            Set<ValidationMessage> errors = validatorSchema.validate(dataNode);

            if (!errors.isEmpty()) {
                List<String> errorMessages = new ArrayList<>();
                for (ValidationMessage error : errors) {
                    errorMessages.add(error.getMessage() + " (path: " + error.getInstanceLocation() + ")");
                }
                return new JsonValidationResult(false, errorMessages);
            }

            return new JsonValidationResult(true);

        } catch (Exception e) {
            List<String> errorMessages = new ArrayList<>();
            errorMessages.add("Validation error: " + e.getMessage());
            return new JsonValidationResult(false, errorMessages);
        }
    }

    public Long saveSchema(String name, JsonNode schemaNode) {
        Long schemaId = idCounter.getAndIncrement();
        SchemaModel schemaModel = new SchemaModel(schemaId, name, schemaNode);
        schemaStorage.put(schemaId, schemaModel);
        return schemaId;
    }

    public JsonValidationResult validateJsonById(Long schemaId, JsonNode dataNode) {
        try {
            SchemaModel schemaModel = schemaStorage.get(schemaId);
            if (schemaModel == null) {
                List<String> errorMessages = new ArrayList<>();
                errorMessages.add("Schema with ID " + schemaId + " not found");
                return new JsonValidationResult(false, errorMessages);
            }

            JsonNode schemaNode = schemaModel.getSchema();
            JsonSchema validatorSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaNode);
            Set<ValidationMessage> errors = validatorSchema.validate(dataNode);

            if (!errors.isEmpty()) {
                List<String> errorMessages = new ArrayList<>();
                for (ValidationMessage error : errors) {
                    errorMessages.add(error.getMessage() + " (path: " + error.getInstanceLocation() + ")");
                }
                return new JsonValidationResult(false, errorMessages);
            }

            return new JsonValidationResult(true);

        } catch (Exception e) {
            List<String> errorMessages = new ArrayList<>();
            errorMessages.add("Validation error: " + e.getMessage());
            return new JsonValidationResult(false, errorMessages);
        }
    }

    public boolean schemaExists(Long schemaId) {
        return schemaStorage.containsKey(schemaId);
    }

    public boolean deleteSchema(Long schemaId) {
        return schemaStorage.remove(schemaId) != null;
    }

    public Map<String, Object> getAllSchemas() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        for (Map.Entry<Long, SchemaModel> entry : schemaStorage.entrySet()) {
            Long schemaId = entry.getKey();
            SchemaModel schemaModel = entry.getValue();
            
            Map<String, Object> schemaInfo = new LinkedHashMap<>();
            schemaInfo.put("id", schemaModel.getId());
            schemaInfo.put("name", schemaModel.getName());
            schemaInfo.put("uploadDate", schemaModel.getUploadDate());
            schemaInfo.put("schema", objectMapper.convertValue(schemaModel.getSchema(), Object.class));
            
            result.put(schemaId.toString(), schemaInfo);
        }
        
        return result;
    }

    public Object getSchema(Long schemaId) {
        SchemaModel schemaModel = schemaStorage.get(schemaId);
        if (schemaModel == null) {
            return null;
        }
        
        Map<String, Object> schemaInfo = new LinkedHashMap<>();
        schemaInfo.put("id", schemaModel.getId());
        schemaInfo.put("name", schemaModel.getName());
        schemaInfo.put("uploadDate", schemaModel.getUploadDate());
        schemaInfo.put("schema", objectMapper.convertValue(schemaModel.getSchema(), Object.class));
        
        return schemaInfo;
    }

    public Map<String, Object> getAllSchemasMetadata() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        for (Map.Entry<Long, SchemaModel> entry : schemaStorage.entrySet()) {
            Long schemaId = entry.getKey();
            SchemaModel schemaModel = entry.getValue();
            
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("id", schemaModel.getId());
            metadata.put("name", schemaModel.getName());
            metadata.put("uploadDate", schemaModel.getUploadDate());
            
            result.put(schemaId.toString(), metadata);
        }
        
        return result;
    }

    public int getSchemaCount() {
        return schemaStorage.size();
    }
}