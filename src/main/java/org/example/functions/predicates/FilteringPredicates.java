package org.example.functions.predicates;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public enum FilteringPredicates {
    include {
        public Predicate<JsonNode> filter(final String field, final Object value) {
            return filteringValue -> filteringValue.get(field).equals(value);
        }
    },
    exclude {
        public Predicate<JsonNode> filter(final String field, final Object value) {
            return filteringValue -> !filteringValue.get(field).equals(value);
        }
    };

    private static final Map<String, FilteringPredicates> nameToValueMap =
        new HashMap<>();

    static {
        for (final FilteringPredicates value : EnumSet.allOf(FilteringPredicates.class)) {
            nameToValueMap.put(value.name(), value);
        }
    }

    public static FilteringPredicates forName(final String name) {
        return nameToValueMap.get(name);
    }

    public abstract Predicate<JsonNode> filter(final String field, final Object value);
}
