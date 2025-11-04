package com.example.demo.dto;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.springframework.http.ResponseEntity;

/*
 * Class for handle errors, codes and messages
 */
public class JsonRpcErrorHandler {
    
    public static final int PARSE_JSON_ERROR = -32700;
    public static final int PARSE_SCHEMA_ERROR = -32701;
    public static final int ID_ALREADY_IN_USE = -32800;
    public static final int ID_DOESNT_EXISTS = -32801;
    public static final int SCHEMA_NOT_EXISTS = -32802;
    
    public static ResponseEntity<String> parseError(Object requestId) {
        return createErrorResponse(PARSE_JSON_ERROR, "Parse json error", requestId);
    }
    
    public static ResponseEntity<String> internalError(Object requestId) {
        return createErrorResponse(JSONRPC2Error.INTERNAL_ERROR.getCode(), "Internal error", requestId);
    }
    
    public static ResponseEntity<String> invalidRequest(Object requestId, String message) {
        return createErrorResponse(JSONRPC2Error.INVALID_REQUEST.getCode(), message, requestId);
    }
    
    public static ResponseEntity<String> methodNotFound(Object requestId) {
        return createErrorResponse(JSONRPC2Error.METHOD_NOT_FOUND.getCode(), "Method not found", requestId);
    }
    
    public static ResponseEntity<String> invalidParams(Object requestId, String message) {
        return createErrorResponse(JSONRPC2Error.INVALID_PARAMS.getCode(), message, requestId);
    }
    
    public static ResponseEntity<String> parseSchemaError(Object requestId, String message) {
        return createErrorResponse(PARSE_SCHEMA_ERROR, "Parse schema error: " + message, requestId);
    }
    
    public static ResponseEntity<String> idDoesntExist(Object requestId, String message) {
        return createErrorResponse(ID_DOESNT_EXISTS, message, requestId);
    }
    
    public static ResponseEntity<String> schemaNotExists(Object requestId, String message) {
        return createErrorResponse(SCHEMA_NOT_EXISTS, message, requestId);
    }
    
    public static JSONRPC2Response createJsonRpcErrorResponse(int code, String message, Object requestId) {
        return new JSONRPC2Response(new JSONRPC2Error(code, message), requestId);
    }
    
    private static ResponseEntity<String> createErrorResponse(int code, String message, Object requestId) {
        JSONRPC2Response errorResponse = new JSONRPC2Response(new JSONRPC2Error(code, message), requestId);
        return ResponseEntity.ok(errorResponse.toJSONString());
    }
}