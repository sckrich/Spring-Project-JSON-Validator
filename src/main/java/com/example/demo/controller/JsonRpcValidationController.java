package com.example.demo.controller;

import com.example.demo.dto.JsonValidationResult;
import com.example.demo.dto.JsonRpcErrorHandler;
import com.example.demo.service.JsonValidationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thetransactioncompany.jsonrpc2.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
public class JsonRpcValidationController {

    @Autowired
    private JsonValidationService validationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * If DB_URL exists - load schemas from DB
     */
    @PostConstruct
    public void init() {
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl != null && !dbUrl.trim().isEmpty()) {
            loadSchemasFromDatabase();
        }
    }

    /**
     * Function for loading schemas from Data Base
     */
    private void loadSchemasFromDatabase() {
        System.out.println("DB_URL найден, loading schemas from DataBase...");
    }

    /**
     * Handles incoming JSON-RPC requests for JSON validation
     * Supported methods: validate, validateById, saveSchema, getAllSchemas, getSchema, getAllSchemasMetadata
     */
    @PostMapping("/api/validation")
    public ResponseEntity<String> handleJsonRpc(@RequestBody String jsonRequest) {
        Object requestId = null;
        
        try {
            JSONRPC2Request jsonrpc2Request = JSONRPC2Request.parse(jsonRequest);
            requestId = jsonrpc2Request.getID();
            
            JSONRPC2Response jsonrpc2Response = processRequest(jsonrpc2Request);
            return ResponseEntity.ok(jsonrpc2Response.toJSONString());
            
        } catch (JSONRPC2ParseException e) {
            requestId = extractRequestIdFromRawJson(jsonRequest);
            return JsonRpcErrorHandler.parseError(requestId);
            
        } catch (Exception e) {
            requestId = extractRequestIdFromRawJson(jsonRequest);
            return JsonRpcErrorHandler.internalError(requestId);
        }
    }

    /**
     * Extracts request ID from raw JSON
     */
    private Object extractRequestIdFromRawJson(String jsonRequest) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonRequest);
            JsonNode idNode = rootNode.get("id");
            if (idNode != null) {
                if (idNode.isTextual()) {
                    return idNode.asText();
                } else if (idNode.isNumber()) {
                    return idNode.asLong();
                } else if (idNode.isBoolean()) {
                    return idNode.asBoolean();
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Main request processing method that routes requests to appropriate handler methods
     */
    private JSONRPC2Response processRequest(JSONRPC2Request request) {
        try {
            if (request.getMethod() == null || request.getMethod().trim().isEmpty()) {
                return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                    JSONRPC2Error.INVALID_REQUEST.getCode(), "Invalid Request", request.getID());
            }

            if (request.getID() == null) {
                return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                    JSONRPC2Error.INVALID_REQUEST.getCode(), "Missing id field", null);
            }

            String method = request.getMethod();
            boolean requiresParams = !method.equals("getAllSchemas") && !method.equals("getAllSchemasMetadata");

            if (requiresParams && request.getNamedParams() == null) {
                return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                    JSONRPC2Error.INVALID_PARAMS.getCode(), "Invalid params", request.getID());
            }

            switch (method) {
                case "validate":
                    return handleValidateMethod(request);
                case "validateById":
                    return handleValidateByIdMethod(request);
                case "saveSchema":
                    return handleSaveSchemaMethod(request);
                case "getAllSchemas":
                    return handleGetAllSchemasMethod(request);
                case "getSchema":
                    return handleGetSchemaMethod(request);
                case "getAllSchemasMetadata": 
                    return handleGetAllSchemasMetadataMethod(request);
                default:
                    return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                        JSONRPC2Error.METHOD_NOT_FOUND.getCode(), "Method not found", request.getID());
            }
            
        } catch (Exception e) {
            return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                JSONRPC2Error.INTERNAL_ERROR.getCode(), "Internal error: " + e.getMessage(), request.getID());
        }
    }

    /**
     * Handles the 'validate' method - validates JSON against provided schema
     * Required parameters: schema, json
     */
    private JSONRPC2Response handleValidateMethod(JSONRPC2Request request) {
        Map<String, Object> params = request.getNamedParams();
        Object schemaObj = params.get("schema");
        Object jsonObj = params.get("json");
        
        if (schemaObj == null || jsonObj == null) {
            return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing schema or json parameters", request.getID());
        }
        
        try {
            JsonNode schemaNode = objectMapper.valueToTree(schemaObj);
            JsonNode jsonNode = objectMapper.valueToTree(jsonObj);
        
            JsonValidationResult result = validationService.validateJson(schemaNode, jsonNode);
            
            return new JSONRPC2Response(result, request.getID());
            
        } catch (Exception e) {
            return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                JsonRpcErrorHandler.PARSE_SCHEMA_ERROR, "Parse schema error: " + e.getMessage(), request.getID());
        }
    }

    /**
     * Handles the 'validateById' method - validates JSON against schema stored by ID
     * Required parameters: schemaId, json
     */
    private JSONRPC2Response handleValidateByIdMethod(JSONRPC2Request request) {
        Map<String, Object> params = request.getNamedParams();
        Object schemaIdObj = params.get("schemaId");
        Object jsonObj = params.get("json");
        
        if (schemaIdObj == null || jsonObj == null) {
            return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing schemaId or json parameters", request.getID());
        }
        
        try {
            int schemaId;
            if (schemaIdObj instanceof Number) {
                schemaId = ((Number) schemaIdObj).intValue();
            } else {
                schemaId = Integer.parseInt(schemaIdObj.toString());
            }
            
            JsonNode jsonNode = objectMapper.valueToTree(jsonObj);
        
            JsonValidationResult result = validationService.validateJsonById(schemaId, jsonNode);
            
            return new JSONRPC2Response(result, request.getID());
            
        } catch (NumberFormatException e) {
            return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                JSONRPC2Error.INVALID_PARAMS.getCode(), "Invalid schema ID format. Must be a number.", request.getID());
        } catch (Exception e) {
            return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                JSONRPC2Error.INTERNAL_ERROR.getCode(), "Validation error: " + e.getMessage(), request.getID());
        }
    }

    /**
     * Handles the 'saveSchema' method - saves a new schema with name
     * Required parameters: name, schema
     */
    private JSONRPC2Response handleSaveSchemaMethod(JSONRPC2Request request) {
        Map<String, Object> params = request.getNamedParams();
        Object nameObj = params.get("name");
        Object schemaObj = params.get("schema");
        
        if (nameObj == null || schemaObj == null) {
            return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing name or schema parameters", request.getID());
        }
        
        try {
            String name = nameObj.toString();
            JsonNode schemaNode = objectMapper.valueToTree(schemaObj);
            Integer schemaId = validationService.saveSchema(name, schemaNode);
            
            return new JSONRPC2Response(
                Map.of("schemaId", schemaId),
                request.getID()
            );
            
        } catch (Exception e) {
            return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                JSONRPC2Error.INTERNAL_ERROR.getCode(), "Schema save error: " + e.getMessage(), request.getID());
        }
    }

    /**
     * Handles the 'getAllSchemas' method - retrieves all stored schemas with full content
     */
    private JSONRPC2Response handleGetAllSchemasMethod(JSONRPC2Request request) {
        Map<String, Object> allSchemas = validationService.getAllSchemas();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSchemas", allSchemas.size());
        result.put("schemas", allSchemas);
        
        return new JSONRPC2Response(result, request.getID());
    }

    /**
     * Handles the 'getSchema' method - retrieves specific schema by ID
     * Required parameters: schemaId
     */
    private JSONRPC2Response handleGetSchemaMethod(JSONRPC2Request request) {
        Map<String, Object> params = request.getNamedParams();
        Object schemaIdObj = params.get("schemaId");
        if (schemaIdObj == null) {
            return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing schemaId parameter", request.getID());
        }
        
        try {
            Integer schemaId;
            if (schemaIdObj instanceof Number) {
                schemaId = ((Number) schemaIdObj).intValue();
            } else {
                schemaId = Integer.parseInt(schemaIdObj.toString());
            }
            
            Object schema = validationService.getSchema(schemaId);
            if (schema == null) {
                return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                    JsonRpcErrorHandler.ID_DOESNT_EXISTS, "Schema with ID " + schemaId + " not found", request.getID());
            }
            
            return new JSONRPC2Response(schema, request.getID());
            
        } catch (NumberFormatException e) {
            return JsonRpcErrorHandler.createJsonRpcErrorResponse(
                JSONRPC2Error.INVALID_PARAMS.getCode(), "Invalid schema ID format. Must be a number.", request.getID());
        }
    }

    /**
     * Handles the 'getAllSchemasMetadata' method - retrieves schemas metadata without full content
     */
    private JSONRPC2Response handleGetAllSchemasMetadataMethod(JSONRPC2Request request) {
        Map<String, Object> allSchemasMetadata = validationService.getAllSchemasMetadata();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSchemas", allSchemasMetadata.size());
        result.put("schemas", allSchemasMetadata);
        
        return new JSONRPC2Response(result, request.getID());
    }
}