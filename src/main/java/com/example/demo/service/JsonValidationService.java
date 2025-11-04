package com.example.demo.service;

import com.example.demo.SchemaModel.SchemaModel;
import com.example.demo.dto.JsonValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Validates JSON data against JSON Schema using networknt library
 * Provides schema storage and management capabilities
 */
@Service
public class JsonValidationService {

    private static final Logger logger = LoggerFactory.getLogger(JsonValidationService.class);
    
    private final ConcurrentHashMap<Integer, SchemaModel> schemaStorage = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validates JSON data against provided JSON schema
     * @param schemaNode JSON schema as JsonNode
     * @param dataNode JSON data to validate as JsonNode
     * @return validation result with status and error messages
     */
    public JsonValidationResult validateJson(JsonNode schemaNode, JsonNode dataNode) {
        logger.info("Starting JSON validation with provided schema");
        logger.debug("Schema: {}, Data: {}", schemaNode, dataNode);
        
        try {
            JsonSchema validatorSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaNode);
            Set<ValidationMessage> errors = validatorSchema.validate(dataNode);

            if (!errors.isEmpty()) {
                logger.warn("JSON validation failed with {} errors", errors.size());
                List<String> errorMessages = new ArrayList<>();
                for (ValidationMessage error : errors) {
                    String errorMsg = error.getMessage() + " (path: " + error.getInstanceLocation() + ")";
                    errorMessages.add(errorMsg);
                    logger.debug("Validation error: {}", errorMsg);
                }
                logger.info("JSON validation completed - INVALID");
                return new JsonValidationResult(false, errorMessages);
            }

            logger.info("JSON validation completed - VALID");
            return new JsonValidationResult(true);

        } catch (Exception e) {
            logger.error("Exception during JSON validation: {}", e.getMessage(), e);
            List<String> errorMessages = new ArrayList<>();
            errorMessages.add("Parse schema error: " + e.getMessage());
            return new JsonValidationResult(false, errorMessages);
        }
    }
    /**
     * Updates existing schema by ID with new schema content
     * @param schemaId ID of the schema to update
     * @param schemaNode new JSON schema content as JsonNode
     * @return true if schema was updated, false if schema with given ID doesn't exist
     */
    public boolean updateSchema(int schemaId, JsonNode schemaNode) {
        logger.info("Updating schema with ID: {}", schemaId);
        
        SchemaModel schemaModel = schemaStorage.get(schemaId);
        if (schemaModel == null) {
            logger.warn("Attempt to update non-existent schema with ID: {}", schemaId);
            return false;
        }
        
        schemaModel.setSchema(schemaNode);
        logger.info("Schema with ID {} updated successfully", schemaId);
        logger.debug("Updated schema content: {}", schemaNode);
        return true;
    }
    
    /**
     * Saves a new JSON schema to storage with optional custom ID
     * @param name human-readable schema name
     * @param schemaNode JSON schema content as JsonNode
     * @param customId custom ID for schema (if null, will be auto-generated)
     * @return generated schema ID
     */
    public Integer saveSchema(String name, JsonNode schemaNode, Integer customId) {
        logger.info("Saving new schema with name: '{}' and custom ID: {}", name, customId);
        
        Integer schemaId;
        if (customId != null) {
            if (schemaStorage.containsKey(customId)) {
                logger.warn("Attempt to save schema with existing ID: {}", customId);
                throw new IllegalArgumentException("ID " + customId + " already in use");
            }
            schemaId = customId;
            if (customId >= idCounter.get()) {
                idCounter.set(customId + 1);
            }
        } else {
            schemaId = idCounter.getAndIncrement();
        }
        
        SchemaModel schemaModel = new SchemaModel(schemaId, name, schemaNode);
        schemaStorage.put(schemaId, schemaModel);
        
        logger.info("Schema saved successfully with ID: {}", schemaId);
        logger.debug("Schema content: {}", schemaNode);
        return schemaId;
    }

    /**
     * Validates JSON data against schema stored by ID
     * @param schemaId ID of the stored schema
     * @param dataNode JSON data to validate as JsonNode
     * @return validation result with status and error messages
     */
    public JsonValidationResult validateJsonById(int schemaId, JsonNode dataNode) {
        logger.info("Starting JSON validation with schema ID: {}", schemaId);
        logger.debug("Data to validate: {}", dataNode);
        
        try {
            SchemaModel schemaModel = schemaStorage.get(schemaId);
            if (schemaModel == null) {
                logger.warn("Schema with ID {} not found", schemaId);
                List<String> errorMessages = new ArrayList<>();
                errorMessages.add("SCHEMA_NOT_FOUND: Schema with ID " + schemaId + " not found");
                return new JsonValidationResult(false, errorMessages);
            }

            logger.debug("Retrieved schema: {}", schemaModel.getName());
            JsonNode schemaNode = schemaModel.getSchema();
            JsonSchema validatorSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaNode);
            Set<ValidationMessage> errors = validatorSchema.validate(dataNode);

            if (!errors.isEmpty()) {
                logger.warn("JSON validation against schema ID {} failed with {} errors", schemaId, errors.size());
                List<String> errorMessages = new ArrayList<>();
                for (ValidationMessage error : errors) {
                    String errorMsg = error.getMessage() + " (path: " + error.getInstanceLocation() + ")";
                    errorMessages.add(errorMsg);
                    logger.debug("Validation error: {}", errorMsg);
                }
                logger.info("JSON validation completed - INVALID");
                return new JsonValidationResult(false, errorMessages);
            }

            logger.info("JSON validation against schema ID {} completed - VALID", schemaId);
            return new JsonValidationResult(true);

        } catch (Exception e) {
            logger.error("Exception during JSON validation with schema ID {}: {}", schemaId, e.getMessage(), e);
            List<String> errorMessages = new ArrayList<>();
            errorMessages.add("Validation error: " + e.getMessage());
            return new JsonValidationResult(false, errorMessages);
        }
    }

    /**
     * Checks if schema with given ID exists in storage
     * @param schemaId schema ID to check
     * @return true if schema exists, false otherwise
     */
    public boolean schemaExists(Integer schemaId) {
        boolean exists = schemaStorage.containsKey(schemaId);
        logger.debug("Checked existence of schema ID {}: {}", schemaId, exists);
        return exists;
    }

    /**
     * Deletes schema from storage by ID
     * @param schemaId ID of schema to delete
     * @return true if schema was deleted, false if not found
     */
    public boolean deleteSchema(Integer schemaId) {
        SchemaModel removed = schemaStorage.remove(schemaId);
        if (removed != null) {
            logger.info("Schema with ID {} deleted successfully", schemaId);
            return true;
        } else {
            logger.warn("Attempt to delete non-existent schema with ID: {}", schemaId);
            return false;
        }
    }

    /**
     * Retrieves all stored schemas with full content
     * @return map of schema IDs to complete schema information
     */
    public Map<String, Object> getAllSchemas() {
        logger.info("Retrieving all schemas, total count: {}", schemaStorage.size());
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        for (Map.Entry<Integer, SchemaModel> entry : schemaStorage.entrySet()) {
            Integer schemaId = entry.getKey();
            SchemaModel schemaModel = entry.getValue();
            
            Map<String, Object> schemaInfo = new LinkedHashMap<>();
            schemaInfo.put("id", schemaModel.getId());
            schemaInfo.put("name", schemaModel.getName());
            schemaInfo.put("uploadDate", schemaModel.getUploadDate());
            schemaInfo.put("schema", objectMapper.convertValue(schemaModel.getSchema(), Object.class));
            
            result.put(schemaId.toString(), schemaInfo);
        }
        
        logger.debug("Retrieved {} schemas", result.size());
        return result;
    }

    /**
     * Retrieves specific schema by ID with full content
     * @param schemaId ID of schema to retrieve
     * @return schema information or null if not found
     */
    public Object getSchema(Integer schemaId) {
        logger.info("Retrieving schema with ID: {}", schemaId);
        
        SchemaModel schemaModel = schemaStorage.get(schemaId);
        if (schemaModel == null) {
            logger.warn("Schema with ID {} not found", schemaId);
            return null;
        }
        
        Map<String, Object> schemaInfo = new LinkedHashMap<>();
        schemaInfo.put("id", schemaModel.getId());
        schemaInfo.put("name", schemaModel.getName());
        schemaInfo.put("uploadDate", schemaModel.getUploadDate());
        schemaInfo.put("schema", objectMapper.convertValue(schemaModel.getSchema(), Object.class));
        
        logger.debug("Retrieved schema: {}", schemaModel.getName());
        return schemaInfo;
    }

    /**
     * Retrieves metadata for all schemas without full schema content
     * @return map of schema IDs to schema metadata (id, name, uploadDate)
     */
    public Map<String, Object> getAllSchemasMetadata() {
        logger.info("Retrieving metadata for all schemas, total: {}", schemaStorage.size());
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        for (Map.Entry<Integer, SchemaModel> entry : schemaStorage.entrySet()) {
            Integer schemaId = entry.getKey();
            SchemaModel schemaModel = entry.getValue();
            
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("id", schemaModel.getId());
            metadata.put("name", schemaModel.getName());
            metadata.put("uploadDate", schemaModel.getUploadDate());
            
            result.put(schemaId.toString(), metadata);
        }
        
        logger.debug("Retrieved metadata for {} schemas", result.size());
        return result;
    }

    /**
     * Gets total number of stored schemas
     * @return count of stored schemas
     */
    public int getSchemaCount() {
        int count = schemaStorage.size();
        logger.debug("Current schema count: {}", count);
        return count;
    }
}