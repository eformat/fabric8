/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Helper methods for extracting values form a Properties object
 */
public class PropertiesHelper {

    public static Long getLong(Properties properties, String key, Long defaultValue) {
        Object value = properties.get(key);
        if (value instanceof String) {
            return Long.parseLong(value.toString());
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            Number number = (Number)value;
            return number.longValue();
        }
        return defaultValue;
    }

    public static long getLongValue(Properties properties, String key, long defaultValue) {
        return getLong(properties, key, defaultValue);
    }

    public static long getLongValue(Map<String, String> map, String key, long defaultValue) {
        Properties properties = new Properties();
        properties.putAll(map);
        return getLong(properties, key, defaultValue);
    }

    /**
     * Returns the map of entries in the properties object which have keys starting with the given prefix, removing the prefix
     * from the returned map.   Keys are made lowercase as required by Kubernetes.
     */
    public static Map<String, String> findPropertiesWithPrefix(Properties properties, String prefix) {
        Map<String, String> answer = new HashMap<>();
        Set<Map.Entry<Object, Object>> entries = properties.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            Object value = entry.getValue();
            Object key = entry.getKey();
            if (key instanceof String && value != null) {
                String keyText = key.toString();
                if (keyText.startsWith(prefix)) {
                    String newKey = keyText.substring(prefix.length()).toLowerCase();
                    answer.put(newKey, value.toString());
                }
            }
        }
        return answer;
    }
}
