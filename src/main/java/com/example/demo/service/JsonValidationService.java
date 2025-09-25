package com.example.demo.service;

import com.example.demo.dto.JsonValidationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class JsonValidationService {

    public JsonValidationResult validateJson(JsonNode schemaNode, JsonNode dataNode) {
        try {
            JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaNode);
            Set<ValidationMessage> errors = schema.validate(dataNode);

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
}