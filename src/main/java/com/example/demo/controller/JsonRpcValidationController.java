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

@RestController
public class JsonRpcValidationController {

    @Autowired
    private JsonValidationService validationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/api/json-rpc")
    public ResponseEntity<String> handleJsonRpc(@RequestBody String jsonRequest) {
        try {
            
            JSONRPC2Request jsonrpc2Request = JSONRPC2Request.parse(jsonRequest);
            JSONRPC2Response jsonrpc2Response = processRequest(jsonrpc2Request);
            return ResponseEntity.ok(jsonrpc2Response.toJSONString());
            
        } catch (JSONRPC2ParseException e) {
            JSONRPC2Response errorResponse = new JSONRPC2Response(
                new JSONRPC2Error(JSONRPC2Error.PARSE_ERROR.getCode(), "Parse error"), 
                null
            );
            return ResponseEntity.ok(errorResponse.toJSONString());
            
        } catch (Exception e) {
            JSONRPC2Response errorResponse = new JSONRPC2Response(
                new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), "Internal error"), 
                null
            );
            return ResponseEntity.ok(errorResponse.toJSONString());
        }
    }

    private JSONRPC2Response processRequest(JSONRPC2Request request) {
        try {
            if ("validate".equals(request.getMethod())) {
                return handleValidateMethod(request);
            }
            return new JSONRPC2Response(
                new JSONRPC2Error(JSONRPC2Error.METHOD_NOT_FOUND.getCode(), "Method not found"), 
                request.getID()
            );
            
        } catch (JSONRPC2Error e) {
            return new JSONRPC2Response(e, request.getID());
        } catch (Exception e) {
            return new JSONRPC2Response(
                new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), "Internal error"), 
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
}