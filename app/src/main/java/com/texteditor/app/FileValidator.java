package com.texteditor.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

public class FileValidator {

    public static class ValidationResult {
        public final boolean isValid;
        public final String message;
        public ValidationResult(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }
    }

    public static ValidationResult validateJson(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ValidationResult(true, "빈 파일");
        }
        try {
            Object obj = new JSONTokener(text.trim()).nextValue();
            if (obj instanceof JSONObject || obj instanceof JSONArray) {
                return new ValidationResult(true, "유효한 JSON ✓");
            }
            return new ValidationResult(true, "유효한 JSON ✓");
        } catch (JSONException e) {
            return new ValidationResult(false, "JSON 오류: " + e.getMessage());
        }
    }

    public static ValidationResult validateXml(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ValidationResult(true, "빈 파일");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new InputSource(new StringReader(text)));
            return new ValidationResult(true, "유효한 XML ✓");
        } catch (Exception e) {
            return new ValidationResult(false, "XML 오류: " + e.getMessage());
        }
    }

    public static String formatJson(String text) throws JSONException {
        String trimmed = text.trim();
        Object obj = new JSONTokener(trimmed).nextValue();
        if (obj instanceof JSONObject) {
            return ((JSONObject) obj).toString(2);
        } else if (obj instanceof JSONArray) {
            return ((JSONArray) obj).toString(2);
        }
        return text;
    }
}
