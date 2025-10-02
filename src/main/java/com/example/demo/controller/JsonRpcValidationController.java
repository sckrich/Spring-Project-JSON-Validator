package com.example.demo.controller;

import com.example.demo.dto.JsonValidationResult;
import com.example.demo.service.JsonValidationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thetransactioncompany.jsonrpc2.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.LinkedHashMap;

@RestController
public class JsonRpcValidationController {

    @Autowired
    private JsonValidationService validationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
            JSONRPC2Response errorResponse = new JSONRPC2Response(
                new JSONRPC2Error(JSONRPC2Error.PARSE_ERROR.getCode(), "Parse error"), 
                requestId
            );
            return ResponseEntity.ok(errorResponse.toJSONString());
            
        } catch (Exception e) {
            JSONRPC2Response errorResponse = new JSONRPC2Response(
                new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), "Internal error"), 
                requestId
            );
            return ResponseEntity.ok(errorResponse.toJSONString());
        }
    }

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

    private JSONRPC2Response processRequest(JSONRPC2Request request) {
        try {
            switch (request.getMethod()) {
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
                    return new JSONRPC2Response(
                        new JSONRPC2Error(JSONRPC2Error.METHOD_NOT_FOUND.getCode(), "Method not found"), 
                        request.getID()
                    );
            }
            
        } catch (JSONRPC2Error e) {
            return new JSONRPC2Response(e, request.getID());
        } catch (Exception e) {
            return new JSONRPC2Response(
                new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), "Internal error: " + e.getMessage()), 
                request.getID()
            );
        }
    }
    
    private JSONRPC2Response handleValidateMethod(JSONRPC2Request request) throws JSONRPC2Error {
        Map<String, Object> params = request.getNamedParams();
        if (params == null) {
            throw new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing parameters");
        }
        Object schemaObj = params.get("schema");
        Object jsonObj = params.get("json");
        
        if (schemaObj == null || jsonObj == null) {
            throw new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing schema or json parameters");
        }
        
        try {
            JsonNode schemaNode = objectMapper.valueToTree(schemaObj);
            JsonNode jsonNode = objectMapper.valueToTree(jsonObj);
        
            JsonValidationResult result = validationService.validateJson(schemaNode, jsonNode);
            
            return new JSONRPC2Response(result, request.getID());
            
        } catch (Exception e) {
            throw new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), "Validation error: " + e.getMessage());
        }
    }

    private JSONRPC2Response handleValidateByIdMethod(JSONRPC2Request request) throws JSONRPC2Error {
        Map<String, Object> params = request.getNamedParams();
        if (params == null) {
            throw new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing parameters");
        }
        
        Object schemaIdObj = params.get("schemaId");
        Object jsonObj = params.get("json");
        
        if (schemaIdObj == null || jsonObj == null) {
            throw new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing schemaId or json parameters");
        }
        
        try {
            Long schemaId;
            if (schemaIdObj instanceof Number) {
                schemaId = ((Number) schemaIdObj).longValue();
            } else {
                schemaId = Long.parseLong(schemaIdObj.toString());
            }
            
            JsonNode jsonNode = objectMapper.valueToTree(jsonObj);
        
            JsonValidationResult result = validationService.validateJsonById(schemaId, jsonNode);
            
            return new JSONRPC2Response(result, request.getID());
            
        } catch (NumberFormatException e) {
            throw new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), "Invalid schema ID format. Must be a number.");
        } catch (Exception e) {
            throw new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), "Validation error: " + e.getMessage());
        }
    }

    private JSONRPC2Response handleSaveSchemaMethod(JSONRPC2Request request) throws JSONRPC2Error {
        Map<String, Object> params = request.getNamedParams();
        if (params == null) {
            throw new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing parameters");
        }
        
        Object nameObj = params.get("name");
        Object schemaObj = params.get("schema");
        
        if (nameObj == null || schemaObj == null) {
            throw new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing name or schema parameters");
        }
        
        try {
            String name = nameObj.toString();
            JsonNode schemaNode = objectMapper.valueToTree(schemaObj);
            Long schemaId = validationService.saveSchema(name, schemaNode);
            
            return new JSONRPC2Response(
                Map.of("schemaId", schemaId),
                request.getID()
            );
            
        } catch (Exception e) {
            throw new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), "Schema save error: " + e.getMessage());
        }
    }

    private JSONRPC2Response handleGetAllSchemasMethod(JSONRPC2Request request) {
        Map<String, Object> allSchemas = validationService.getAllSchemas();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSchemas", allSchemas.size());
        result.put("schemas", allSchemas);
        
        return new JSONRPC2Response(result, request.getID());
    }

    private JSONRPC2Response handleGetSchemaMethod(JSONRPC2Request request) throws JSONRPC2Error {
        Map<String, Object> params = request.getNamedParams();
        if (params == null) {
            throw new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing parameters");
        }
        
        Object schemaIdObj = params.get("schemaId");
        if (schemaIdObj == null) {
            throw new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), "Missing schemaId parameter");
        }
        
        try {
            Long schemaId;
            if (schemaIdObj instanceof Number) {
                schemaId = ((Number) schemaIdObj).longValue();
            } else {
                schemaId = Long.parseLong(schemaIdObj.toString());
            }
            
            Object schema = validationService.getSchema(schemaId);
            if (schema == null) {
                throw new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), "Schema with ID " + schemaId + " not found");
            }
            
            return new JSONRPC2Response(schema, request.getID());
            
        } catch (NumberFormatException e) {
            throw new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), "Invalid schema ID format. Must be a number.");
        }
    }

    private JSONRPC2Response handleGetAllSchemasMetadataMethod(JSONRPC2Request request) {
        Map<String, Object> allSchemasMetadata = validationService.getAllSchemasMetadata();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSchemas", allSchemasMetadata.size());
        result.put("schemas", allSchemasMetadata);
        
        return new JSONRPC2Response(result, request.getID());
    }
}