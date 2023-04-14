/*
 * Copyright 2015 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openid.appauth;

import static net.openid.appauth.Preconditions.checkNotNull;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for JSON object manipulation, avoiding unnecessary checked exceptions.
 */
final class JsonUtil {

    private JsonUtil() {
        throw new IllegalStateException("This type is not intended to be instantiated");
    }

    public static void put(
            @NonNull JSONObject json,
            @NonNull String field,
            @NonNull int value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        checkNotNull(value, "value must not be null");

        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract, ex");
        }
    }

    public static void put(
            @NonNull JSONObject json,
            @NonNull String field,
            @NonNull String value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        checkNotNull(value, "value must not be null");
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void put(
            @NonNull JSONObject json,
            @NonNull String field,
            @NonNull JSONArray value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        checkNotNull(value, "value must not be null");
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void put(
            @NonNull JSONObject json,
            @NonNull String field,
            @NonNull JSONObject value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        checkNotNull(value, "value must not be null");
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void putIfNotNull(
            @NonNull JSONObject json,
            @NonNull String field,
            @Nullable String value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (value == null) {
            return;
        }
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void putIfNotNull(
            @NonNull JSONObject json,
            @NonNull String field,
            @Nullable Uri value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (value == null) {
            return;
        }
        try {
            json.put(field, value.toString());
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void putIfNotNull(
            @NonNull JSONObject json,
            @NonNull String field,
            @Nullable Long value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (value == null) {
            return;
        }
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    public static void putIfNotNull(
            @NonNull JSONObject json,
            @NonNull String field,
            @Nullable JSONObject value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (value == null) {
            return;
        }
        try {
            json.put(field, value);
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    @VisibleForTesting
    static void putIfNotNull(
            @NonNull JSONObject json,
            @NonNull String field,
            @Nullable Object value) {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (value == null) {
            return;
        }
        try {
            if (value instanceof Collection) {
                json.put(field, new JSONArray((Collection) value));
            } else if (value instanceof Map) {
                Map<String, Object> map = (Map<String, Object>)value;
                JSONObject valueObj = new JSONObject();
                for (String key : map.keySet()) {
                    JsonUtil.putIfNotNull(valueObj, key, map.get(key));
                }
                json.put(field, valueObj);
            } else {
                json.put(field, value);
            }
        } catch (JSONException ex) {
            throw new IllegalStateException("JSONException thrown in violation of contract", ex);
        }
    }

    @NonNull
    public static String getString(
            @NonNull JSONObject json,
            @NonNull String field)
            throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            throw new JSONException("field \"" + field + "\" not found in json object");
        }

        String value = json.getString(field);
        if (value == null) {
            throw new JSONException("field \"" + field + "\" is mapped to a null value");
        }
        return value;
    }

    public static String getStringIfDefined(
            @NonNull JSONObject json,
            @NonNull String field) throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            return null;
        }

        String value = json.getString(field);
        if (value == null) {
            throw new JSONException("field \"" + field + "\" is mapped to a null value");
        }
        return value;
    }

    public static List<String> getStringListIfDefined(@NonNull JSONObject json,
                                                      @NonNull String field) throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            return null;
        }

        JSONArray array = json.getJSONArray(field);
        if (array == null) {
            throw new JSONException("field \"" + field + "\" is mapped to a null value");
        }
        return toStringList(array);
    }

    public static Uri getUri(
            @NonNull JSONObject json,
            @NonNull String field)
            throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");

        String value = json.getString(field);
        if (value == null) {
            throw new JSONException("field \"" + field + "\" is mapped to a null value");
        }
        return Uri.parse(value);
    }

    @Nullable
    public static Uri getUriIfDefined(
            @NonNull JSONObject json,
            @NonNull String field)
            throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            return null;
        }

        String value = json.getString(field);
        if (value == null) {
            throw new JSONException("field \"" + field + "\" is mapped to a null value");
        }

        return Uri.parse(value);
    }

    @Nullable
    public static Long getLongIfDefined(
            @NonNull JSONObject json,
            @NonNull String field)
            throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field) || json.isNull(field)) {
            return null;
        }

        try {
            return json.getLong(field);
        } catch (JSONException e) {
            return null;
        }
    }

    @NonNull
    public static List<String> getStringList(
            @NonNull JSONObject json,
            @NonNull String field) throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            throw new JSONException("field \"" + field + "\" not found in json object");
        }

        JSONArray array = json.getJSONArray(field);
        return toStringList(array);
    }

    @NonNull
    public static List<Uri> getUriList(
            @NonNull JSONObject json,
            @NonNull String field) throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            throw new JSONException("field \"" + field + "\" not found in json object");
        }

        JSONArray array = json.getJSONArray(field);
        return toUriList(array);
    }

    @NonNull
    public static Map<String, String> getStringMap(JSONObject json, String field)
            throws JSONException {
        LinkedHashMap<String, String> stringMap = new LinkedHashMap<>();
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            return stringMap;
        }

        JSONObject mapJson = json.getJSONObject(field);
        Iterator<String> mapKeys = mapJson.keys();
        while (mapKeys.hasNext()) {
            String key = mapKeys.next();
            String value = checkNotNull(mapJson.getString(key),
                    "additional parameter values must not be null");
            stringMap.put(key, value);
        }
        return stringMap;
    }

    public static JSONObject getJsonObjectIfDefined(@NonNull JSONObject json,
            @NonNull String field) throws JSONException {
        checkNotNull(json, "json must not be null");
        checkNotNull(field, "field must not be null");
        if (!json.has(field)) {
            return null;
        }

        JSONObject value = json.optJSONObject(field);
        if (value == null) {
            throw new JSONException("field \"" + field + "\" is mapped to a null value");
        }
        return value;
    }

    @NonNull
    public static List<String> toStringList(@Nullable JSONArray jsonArray)
            throws JSONException {
        List<String> arrayList = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                arrayList.add(checkNotNull(jsonArray.get(i)).toString());
            }
        }
        return arrayList;
    }

    @NonNull
    public static Map<String, Object> toMap(@NonNull  JSONObject json) throws JSONException {
        checkNotNull(json, "json must not be null");

        Map<String, Object> map = new HashMap<>();
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value);
        }
        return map;
    }

    @NonNull
    public static List<Object> toList(@NonNull JSONArray jsonArray) throws JSONException {
        checkNotNull(jsonArray, "jsonArray must not be null");

        List<Object> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONArray) {
                value = toList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    @NonNull
    public static List<Uri> toUriList(@Nullable JSONArray jsonArray)
            throws JSONException {
        List<Uri> arrayList = new ArrayList<>();
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                arrayList.add(Uri.parse(checkNotNull(jsonArray.get(i)).toString()));
            }
        }
        return arrayList;
    }

    @NonNull
    public static JSONArray toJsonArray(@NonNull Iterable<?> objects) {
        checkNotNull(objects, "objects cannot be null");
        JSONArray jsonArray = new JSONArray();
        for (Object obj : objects) {
            jsonArray.put(obj.toString());
        }
        return jsonArray;
    }

    @NonNull
    public static JSONObject mapToJsonObject(@NonNull Map<String, String> map) {
        checkNotNull(map);
        JSONObject json = new JSONObject();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            checkNotNull(entry.getKey(), "map entries must not have null keys");
            checkNotNull(entry.getValue(), "map entries must not have null values");
            JsonUtil.put(json, entry.getKey(), entry.getValue());
        }
        return json;
    }

    public static <T> T get(JSONObject json, Field<T> field) {
        try {
            if (!json.has(field.key)) {
                return field.defaultValue;
            }
            return field.convert(json.getString(field.key));
        } catch (JSONException e) {
            // all appropriate steps are taken above to avoid a JSONException. If it is still
            // thrown, indicating an implementation change, throw an exception
            throw new IllegalStateException("unexpected JSONException", e);
        }
    }

    public static <T> List<T> get(JSONObject json, ListField<T> field) {
        try {
            if (!json.has(field.key)) {
                return field.defaultValue;
            }
            Object value = json.get(field.key);
            if (!(value instanceof JSONArray)) {
                throw new IllegalStateException(field.key
                        + " does not contain the expected JSON array");
            }
            JSONArray arrayValue = (JSONArray) value;
            ArrayList<T> values = new ArrayList<>();
            for (int i = 0; i < arrayValue.length(); i++) {
                values.add(field.convert(arrayValue.getString(i)));
            }
            return values;
        } catch (JSONException e) {
            // all appropriate steps are taken above to avoid a JSONException. If it is still
            // thrown, indicating an implementation change, throw an excpetion
            throw new IllegalStateException("unexpected JSONException", e);
        }
    }

    abstract static class Field<T> {
        /**
         * The metadata key within the discovery document.
         */
        public final String key;

        /**
         * The default value for this metadata entry, as defined by the OpenID Connect
         * specification.
         */
        public final T defaultValue;

        /**
         * Creates a metadata value abstraction with the given key and default value.
         */
        Field(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        /**
         * Converts the string representation of the value to the correct type.
         */
        abstract T convert(String value);
    }

    static final class UriField extends Field<Uri> {
        /**
         * Creates a metadata value abstraction with the given key and default URI value.
         */
        UriField(String key, Uri defaultValue) {
            super(key, defaultValue);
        }

        /**
         * Creates a metadata abstraction with the given key and a null URI default value.
         */
        UriField(String key) {
            this(key, null);
        }

        @Override
        Uri convert(String value) {
            return Uri.parse(value);
        }
    }

    static final class StringField extends Field<String> {
        /**
         * Creates a metadata abstraction with the given key and string default value.
         */
        StringField(String key, String defaultValue) {
            super(key, defaultValue);
        }

        /**
         * Creates a metadata abstraction with the given key and a null string default value.
         */
        StringField(String key) {
            this(key, null);
        }

        @Override
        String convert(String value) {
            return value;
        }
    }

    static final class BooleanField extends Field<Boolean> {

        /**
         * Creates a metadata abstraction with the given key and default boolean value.
         */
        BooleanField(String key, boolean defaultValue) {
            super(key, defaultValue);
        }

        @Override
        Boolean convert(String value) {
            return Boolean.parseBoolean(value);
        }
    }

    abstract static class ListField<T> {
        public final String key;
        public final List<T> defaultValue;

        ListField(String key, List<T> defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        abstract T convert(String value);
    }

    static final class StringListField extends ListField<String> {

        StringListField(String key) {
            super(key, null);
        }

        StringListField(String key, List<String> defaultValue) {
            super(key, defaultValue);
        }

        @Override
        String convert(String value) {
            return value;
        }
    }
}
