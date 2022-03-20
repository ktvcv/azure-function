package org.example.functions.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.HttpRequestMessage;
import org.example.functions.exceptions.FilterException;
import org.example.functions.predicates.FilteringPredicates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class FilterService {
    private final static ObjectMapper mapper = new ObjectMapper();

    public List<String> filter(HttpRequestMessage<Optional<String>> request) {
        final JsonNode jsonNode = parse(request);
        final List<String> errorLogs = validate(jsonNode);
        if (errorLogs.isEmpty()) {
            return filter(jsonNode.get("data"), jsonNode.get("condition"));
        } else throw new FilterException(errorLogs);
    }

    private List<String> filter(final JsonNode data, final JsonNode condition) {
        final Iterator<Map.Entry<String, JsonNode>> nodes = condition.fields();
        final Map<String, Map<String, Object>> conditions = getConditions(nodes);
        return getList(data, conditions);
    }

    private Map<String, Map<String, Object>> getConditions(Iterator<Map.Entry<String, JsonNode>> nodes) {
        final Map<String, Map<String, Object>> conditions = new HashMap<>();

        while (nodes.hasNext()) {
            final Map.Entry<String, JsonNode> entry = nodes.next();
            final Map<String, Object> values = new HashMap<>();
            for (final JsonNode value : entry.getValue()) {
                if (value.isValueNode()) {
                    values.put(value.textValue(), null);
                } else {
                    value.fields().forEachRemaining(e -> values.put(e.getKey(), e.getValue()));
                }
            }

            conditions.put(entry.getKey(), values);
        }

        return conditions;
    }

    private List<String> getList(final JsonNode data, final Map<String, Map<String, Object>> conditions) {
        final List<JsonNode> list = new ArrayList<>();
        final List<String> errorLogs = new ArrayList<>();

        if (data.isArray()) {
            for (final JsonNode objNode : data) {
                list.add(objNode);
            }
        } else {
            errorLogs.add("Data is not a list");
        }

        final Map<String, String> filterValues = new HashMap<>();

        conditions.keySet()
            .forEach(filteringCondition -> {
                if (FilteringPredicates.forName(filteringCondition) != null) {
                    conditions.get(filteringCondition).keySet().forEach(key -> {
                        if ((conditions.get(filteringCondition).get(key) != null)
                            && (list.stream().noneMatch(node -> node.get(key) == null))
                        ) {
                            filterValues.put(filteringCondition, key);
                        } else {
                            errorLogs.add("Not in every data list field for filtering exists");
                        }
                    });
                }
            });

        if (errorLogs.isEmpty()) {
            return filter(list, conditions, filterValues);
        } else {
            throw new FilterException(errorLogs);
        }
    }

    private List<String> filter(
        final List<JsonNode> list, final Map<String, Map<String, Object>> conditions,
        final Map<String, String> filterValues
    ) {
        final List<Predicate<JsonNode>> predicates = new ArrayList<>();
        for (final Map.Entry<String, String> filterValue : filterValues.entrySet()) {
            predicates.add(
                FilteringPredicates.forName(filterValue.getKey())
                .filter(filterValue.getValue(), conditions.get(filterValue.getKey())
                    .get(filterValue.getValue()))
            );
        }
            return list
                .stream()
                .filter(predicates.stream().reduce(availablePriceConfig -> true, Predicate::and))
                .map(JsonNode::toPrettyString)
                .collect(toList());

    }

    private List<String> validate(final JsonNode jsonNode) {
        final List<String> errorLogs = new ArrayList<>();
        if (jsonNode == null) {
            errorLogs.add("Not json type object received");
        } else {
            if (jsonNode.get("data") == null) {
                errorLogs.add("Data node is null");
            }
            if (jsonNode.get("condition") == null) {
                errorLogs.add("Condition node is null");
            }
        }

        return errorLogs;
    }

    private JsonNode parse(final HttpRequestMessage<Optional<String>> request) {
        JsonNode jsonNode = null;
        try {
            jsonNode = request.getBody().isPresent()
                ? mapper.readTree(request.getBody().get())
                : null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return jsonNode;
    }
}

