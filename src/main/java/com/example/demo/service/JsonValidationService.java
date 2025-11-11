package com.example.demo.service;

import com.example.demo.SchemaModel.SchemaModel;
import com.example.demo.dto.JsonValidationResult;
import com.example.demo.DBEntity.SchemaEntity;
import com.example.demo.Repository.SchemaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;

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
    
    @Autowired(required = false)
    private SchemaRepository schemaRepository;

    /**
     * Load all schemas from DataBase after starting application
     */
    @PostConstruct
    @Transactional
    public void loadAllSchemasFromDatabase() {
        if (schemaRepository == null) {
            logger.info("Database repository not available - using in-memory storage only");
            return;
        }
        
        try {
            logger.info("Loading schemas from Oracle database...");
            List<SchemaEntity> allSchemas = schemaRepository.findAllOrderedById();
            schemaStorage.clear();
            
            for (SchemaEntity entity : allSchemas) {
                JsonNode schemaNode = objectMapper.readTree(entity.getJsonSchema());
                SchemaModel schemaModel = new SchemaModel(
                    entity.getSchemaId(), 
                    entity.getSchemaName(), 
                    schemaNode
                );
                schemaStorage.put(entity.getSchemaId(), schemaModel);
            }
            
            Optional<Integer> maxId = schemaRepository.findMaxSchemaId();
            if (maxId.isPresent()) {
                idCounter.set(maxId.get() + 1);
                logger.info("Loaded {} schemas from database. Next ID: {}", schemaStorage.size(), idCounter.get());
            } else {
                logger.info("No schemas found in database, starting with ID: 1");
            }
            
        } catch (Exception e) {
            logger.error("Error loading schemas from database: {}", e.getMessage(), e);
            logger.info("Continuing with in-memory storage only");
        }
    }

    /**
     * Validates JSON data against provided JSON schema
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
     */
    @Transactional
    public boolean updateSchema(int schemaId, JsonNode schemaNode) {
        logger.info("Updating schema with ID: {}", schemaId);
        
        SchemaModel schemaModel = schemaStorage.get(schemaId);
        if (schemaModel == null) {
            logger.warn("Attempt to update non-existent schema with ID: {}", schemaId);
            return false;
        }
        
        try {
            if (schemaRepository != null) {
                Optional<SchemaEntity> existingEntity = schemaRepository.findBySchemaId(schemaId);
                if (existingEntity.isPresent()) {
                    SchemaEntity entity = existingEntity.get();
                    entity.setJsonSchema(schemaNode.toString());
                    entity.setSchemaName(schemaModel.getName());
                    schemaRepository.save(entity);
                    logger.debug("Schema updated in database: {}", schemaId);
                }
            }
            
            schemaModel.setSchema(schemaNode);
            schemaStorage.put(schemaId, schemaModel);
            
            logger.info("Schema with ID {} updated successfully", schemaId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error updating schema in database: {}", e.getMessage(), e);
            schemaModel.setSchema(schemaNode);
            schemaStorage.put(schemaId, schemaModel);
            logger.info("Schema updated in in-memory storage only with ID: {}", schemaId);
            return true;
        }
    }
    
    /**
     * Saves a new JSON schema to storage with optional custom ID
     */
    @Transactional
    public Integer saveSchema(String name, JsonNode schemaNode, Integer customId) {
        logger.info("Saving new schema with name: '{}' and custom ID: {}", name, customId);
        
        Integer schemaId;
        if (customId != null) {
            boolean existsInMemory = schemaStorage.containsKey(customId);
            boolean existsInDb = schemaRepository != null && schemaRepository.existsBySchemaId(customId);
            
            if (existsInMemory || existsInDb) {
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
        
        try {
            if (schemaRepository != null) {
                SchemaEntity entity = new SchemaEntity();
                entity.setSchemaId(schemaId);
                entity.setSchemaName(name);
                entity.setJsonSchema(schemaNode.toString());
                entity.setDescription("Schema: " + name);
                
                schemaRepository.save(entity);
                logger.debug("Schema saved to database with ID: {}", schemaId);
            }
            
            SchemaModel schemaModel = new SchemaModel(schemaId, name, schemaNode);
            schemaStorage.put(schemaId, schemaModel);
            
            logger.info("Schema saved successfully with ID: {}", schemaId);
            return schemaId;
            
        } catch (Exception e) {
            logger.error("Error saving schema to database: {}", e.getMessage(), e);
            SchemaModel schemaModel = new SchemaModel(schemaId, name, schemaNode);
            schemaStorage.put(schemaId, schemaModel);
            logger.info("Schema saved to in-memory storage only with ID: {}", schemaId);
            return schemaId;
        }
    }

    /**
     * Validates JSON data against schema stored by ID
     */
    public JsonValidationResult validateJsonById(int schemaId, JsonNode dataNode) {
        logger.info("Starting JSON validation with schema ID: {}", schemaId);
        logger.debug("Data to validate: {}", dataNode);
        
        try {
            SchemaModel schemaModel = schemaStorage.get(schemaId);
            if (schemaModel == null) {
                logger.warn("Schema with ID {} not found in cache, checking database...", schemaId);
                
                if (schemaRepository != null) {
                    Optional<SchemaEntity> entity = schemaRepository.findBySchemaId(schemaId);
                    if (entity.isPresent()) {
                        JsonNode schemaNode = objectMapper.readTree(entity.get().getJsonSchema());
                        schemaModel = new SchemaModel(schemaId, entity.get().getSchemaName(), schemaNode);
                        schemaStorage.put(schemaId, schemaModel); 
                        logger.info("Loaded schema from database: {}", schemaId);
                    }
                }
                
                if (schemaModel == null) {
                    logger.warn("Schema with ID {} not found", schemaId);
                    List<String> errorMessages = new ArrayList<>();
                    errorMessages.add("SCHEMA_NOT_FOUND: Schema with ID " + schemaId + " not found");
                    return new JsonValidationResult(false, errorMessages);
                }
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
     */
    public boolean schemaExists(Integer schemaId) {
        boolean exists = schemaStorage.containsKey(schemaId);
        if (!exists && schemaRepository != null) {
            exists = schemaRepository.existsBySchemaId(schemaId);
        }
        logger.debug("Checked existence of schema ID {}: {}", schemaId, exists);
        return exists;
    }

    /**
     * Deletes schema from storage by ID
     */
    @Transactional
    public boolean deleteSchema(Integer schemaId) {
        try {
            if (schemaRepository != null && schemaRepository.existsBySchemaId(schemaId)) {
                schemaRepository.deleteById(schemaId);
                logger.debug("Schema deleted from database: {}", schemaId);
            }
            
            SchemaModel removed = schemaStorage.remove(schemaId);
            if (removed != null) {
                logger.info("Schema with ID {} deleted successfully", schemaId);
                return true;
            } else {
                logger.warn("Attempt to delete non-existent schema with ID: {}", schemaId);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error deleting schema from database: {}", e.getMessage(), e);
            SchemaModel removed = schemaStorage.remove(schemaId);
            if (removed != null) {
                logger.info("Schema deleted from in-memory storage only with ID: {}", schemaId);
                return true;
            }
            return false;
        }
    }

  /**
 * Retrieves all stored schemas with full content
 */
public Map<String, Object> getAllSchemas() {
    logger.info("Retrieving all schemas, total count: {}", schemaStorage.size());
    
    Map<String, Object> result = new LinkedHashMap<>();
    List<Map<String, Object>> schemasList = new ArrayList<>();
    
    for (Map.Entry<Integer, SchemaModel> entry : schemaStorage.entrySet()) {
        Integer schemaId = entry.getKey();
        SchemaModel schemaModel = entry.getValue();
        
        Map<String, Object> schemaInfo = new LinkedHashMap<>();
        schemaInfo.put("id", schemaId); 
        schemaInfo.put("name", schemaModel.getName());
        schemaInfo.put("uploadDate", schemaModel.getUploadDate());
        schemaInfo.put("schema", objectMapper.convertValue(schemaModel.getSchema(), Object.class));
        
        schemasList.add(schemaInfo);
    }
    
    result.put("totalSchemas", schemasList.size());
    result.put("schemas", schemasList); 
    
    logger.debug("Retrieved {} schemas", schemasList.size());
    return result;
}
    /**
     * Retrieves specific schema by ID with full content
     */
    public Object getSchema(Integer schemaId) {
        logger.info("Retrieving schema with ID: {}", schemaId);
        
        SchemaModel schemaModel = schemaStorage.get(schemaId);
        if (schemaModel == null) {
            if (schemaRepository != null) {
                logger.debug("Schema not found in cache, checking database...");
                Optional<SchemaEntity> entity = schemaRepository.findBySchemaId(schemaId);
                if (entity.isPresent()) {
                    try {
                        JsonNode schemaNode = objectMapper.readTree(entity.get().getJsonSchema());
                        schemaModel = new SchemaModel(schemaId, entity.get().getSchemaName(), schemaNode);
                        schemaStorage.put(schemaId, schemaModel); 
                        logger.info("Loaded schema from database: {}", schemaId);
                    } catch (Exception e) {
                        logger.error("Error loading schema from database: {}", e.getMessage(), e);
                        return null;
                    }
                }
            }
            
            if (schemaModel == null) {
                logger.warn("Schema with ID {} not found", schemaId);
                return null;
            }
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
 */
public Map<String, Object> getAllSchemasMetadata() {
    logger.info("Retrieving metadata for all schemas");
    
    Map<String, Object> result = new LinkedHashMap<>();
    List<Map<String, Object>> schemasList = new ArrayList<>();
    
    try {
        if (schemaRepository != null) {
            List<SchemaEntity> allSchemas = schemaRepository.findAllOrderedById();
            
            for (SchemaEntity entity : allSchemas) {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("id", entity.getSchemaId()); 
                metadata.put("name", entity.getSchemaName());
                metadata.put("description", entity.getDescription());
                metadata.put("chgDt", entity.getChgDt());
                
                schemasList.add(metadata);
            }
            
            logger.debug("Retrieved metadata for {} schemas from database", schemasList.size());
        } else {
            logger.debug("Database not available, using in-memory storage");
        }
        
    } catch (Exception e) {
        logger.error("Error loading metadata from database, using cache: {}", e.getMessage());
    }
    
    if (schemasList.isEmpty()) {
        for (Map.Entry<Integer, SchemaModel> entry : schemaStorage.entrySet()) {
            Integer schemaId = entry.getKey();
            SchemaModel schemaModel = entry.getValue();
            
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("id", schemaId); 
            metadata.put("name", schemaModel.getName());
            metadata.put("uploadDate", schemaModel.getUploadDate());
            
            schemasList.add(metadata);
        }
        logger.debug("Retrieved metadata for {} schemas from cache", schemasList.size());
    }
    
    result.put("totalSchemas", schemasList.size());
    result.put("schemas", schemasList); 
    
    return result;
}

    /**
     * Gets total number of stored schemas
     */
    public int getSchemaCount() {
        int count = schemaStorage.size();
        logger.debug("Current schema count: {}", count);
        return count;
    }

    /**
     * Check if database is available
     */
    public boolean isDatabaseAvailable() {
        return schemaRepository != null;
    }
}