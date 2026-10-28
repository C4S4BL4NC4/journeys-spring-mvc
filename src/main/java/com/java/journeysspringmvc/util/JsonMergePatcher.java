/*
 * Applies a JSON Merge Patch (RFC 7396) to a DTO, for PATCH endpoints.
 *
 * The client sends only the fields to change, e.g. {"price": 999, "discount": null}:
 *   - fields present in the patch replace the current value (nested objects merge the same way)
 *   - fields set to null are cleared
 *   - fields left out keep their current value
 * The merged result is then run through Bean Validation, so a PATCH obeys exactly the same
 * rules as a full PUT. Problems become a 400 (BadRequestException) listing what's wrong.
 */
package com.java.journeysspringmvc.util;

import com.java.journeysspringmvc.exception.BadRequestException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JsonMergePatcher {

    private final JsonMapper jsonMapper;
    private final Validator validator;

    public <T> T apply(T current, JsonNode patch, Class<T> type) {
        if (patch == null || !patch.isObject()) {
            throw new BadRequestException("A merge patch must be a JSON object");
        }

        T patched;
        try {
            patched = jsonMapper.treeToValue(merge(jsonMapper.valueToTree(current), patch), type);
        } catch (JacksonException ex) {
            throw new BadRequestException("Invalid patch: " + ex.getOriginalMessage());
        }

        String violations = validator.validate(patched).stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        if (!violations.isEmpty()) {
            throw new BadRequestException(violations);
        }
        return patched;
    }

    private static JsonNode merge(JsonNode target, JsonNode patch) {
        if (!patch.isObject() || target == null || !target.isObject()) {
            return patch;
        }
        ObjectNode result = ((ObjectNode) target).deepCopy();
        for (Map.Entry<String, JsonNode> field : patch.properties()) {
            if (field.getValue().isNull()) {
                result.remove(field.getKey());
            } else {
                result.set(field.getKey(), merge(result.get(field.getKey()), field.getValue()));
            }
        }
        return result;
    }
}
