package com.texteditor.app;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {

    public enum FileType {
        TEXT, JSON, XML, UNKNOWN
    }

    // 다크모드 색상
    private static final int COLOR_JSON_KEY_DARK       = 0xFF9CDCFE; // 하늘색 - 키
    private static final int COLOR_JSON_STRING_DARK    = 0xFFCE9178; // 주황색 - 문자열
    private static final int COLOR_JSON_NUMBER_DARK    = 0xFFB5CEA8; // 연초록 - 숫자
    private static final int COLOR_JSON_BOOL_NULL_DARK = 0xFF569CD6; // 파란색 - true/false/null
    private static final int COLOR_JSON_BRACKET_DARK   = 0xFFFFD700; // 노란색 - 괄호
    private static final int COLOR_XML_TAG_DARK        = 0xFF569CD6; // 파란색 - 태그
    private static final int COLOR_XML_ATTR_DARK       = 0xFF9CDCFE; // 하늘색 - 속성명
    private static final int COLOR_XML_VALUE_DARK      = 0xFFCE9178; // 주황색 - 속성값
    private static final int COLOR_XML_COMMENT_DARK    = 0xFF6A9955; // 초록색 - 주석
    private static final int COLOR_XML_CDATA_DARK      = 0xFF808080; // 회색 - CDATA

    // 라이트모드 색상
    private static final int COLOR_JSON_KEY_LIGHT       = 0xFF0451A5;
    private static final int COLOR_JSON_STRING_LIGHT    = 0xFFA31515;
    private static final int COLOR_JSON_NUMBER_LIGHT    = 0xFF098658;
    private static final int COLOR_JSON_BOOL_NULL_LIGHT = 0xFF0000FF;
    private static final int COLOR_JSON_BRACKET_LIGHT   = 0xFF795E26;
    private static final int COLOR_XML_TAG_LIGHT        = 0xFF800000;
    private static final int COLOR_XML_ATTR_LIGHT       = 0xFFFF0000;
    private static final int COLOR_XML_VALUE_LIGHT      = 0xFF0000FF;
    private static final int COLOR_XML_COMMENT_LIGHT    = 0xFF008000;
    private static final int COLOR_XML_CDATA_LIGHT      = 0xFF808080;

    private boolean isDarkMode;
    private FileType fileType = FileType.TEXT;
    private boolean isHighlighting = false;

    public SyntaxHighlighter(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.isDarkMode = darkMode;
    }

    public void setFileType(FileType fileType) {
        this.fileType = fileType;
    }

    public FileType getFileType() {
        return fileType;
    }

    public static FileType detectFileType(String filename) {
        if (filename == null) return FileType.TEXT;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".json")) return FileType.JSON;
        if (lower.endsWith(".xml") || lower.endsWith(".html") || lower.endsWith(".htm")) return FileType.XML;
        return FileType.TEXT;
    }

    public void attachToEditText(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isHighlighting) return;
                if (fileType == FileType.TEXT) return;
                isHighlighting = true;
                highlight(s);
                isHighlighting = false;
            }
        });
    }

    public void highlight(Editable s) {
        if (fileType == FileType.JSON) {
            highlightJson(s);
        } else if (fileType == FileType.XML) {
            highlightXml(s);
        }
    }

    private void clearSpans(Editable s) {
        ForegroundColorSpan[] spans = s.getSpans(0, s.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            s.removeSpan(span);
        }
    }

    private void applyColor(Editable s, int start, int end, int color) {
        s.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void highlightJson(Editable s) {
        clearSpans(s);
        String text = s.toString();

        // JSON 키 (따옴표 안 문자열 뒤에 콜론이 오는 경우)
        Pattern keyPattern = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:");
        Matcher m = keyPattern.matcher(text);
        while (m.find()) {
            applyColor(s, m.start(), m.end() - 1, isDarkMode ? COLOR_JSON_KEY_DARK : COLOR_JSON_KEY_LIGHT);
        }

        // JSON 문자열 값 (키가 아닌 문자열)
        Pattern strPattern = Pattern.compile(":\\s*(\"(?:[^\"\\\\]|\\\\.)*\")");
        m = strPattern.matcher(text);
        while (m.find()) {
            applyColor(s, m.start(1), m.end(1), isDarkMode ? COLOR_JSON_STRING_DARK : COLOR_JSON_STRING_LIGHT);
        }

        // 배열/단독 문자열
        Pattern arrStrPattern = Pattern.compile("(?<=[\\[,\\s])(\"(?:[^\"\\\\]|\\\\.)*\")(?=[\\],\\s])");
        m = arrStrPattern.matcher(text);
        while (m.find()) {
            applyColor(s, m.start(1), m.end(1), isDarkMode ? COLOR_JSON_STRING_DARK : COLOR_JSON_STRING_LIGHT);
        }

        // 숫자
        Pattern numPattern = Pattern.compile(":\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)");
        m = numPattern.matcher(text);
        while (m.find()) {
            applyColor(s, m.start(1), m.end(1), isDarkMode ? COLOR_JSON_NUMBER_DARK : COLOR_JSON_NUMBER_LIGHT);
        }

        // true, false, null
        Pattern boolPattern = Pattern.compile("\\b(true|false|null)\\b");
        m = boolPattern.matcher(text);
        while (m.find()) {
            applyColor(s, m.start(), m.end(), isDarkMode ? COLOR_JSON_BOOL_NULL_DARK : COLOR_JSON_BOOL_NULL_LIGHT);
        }

        // 괄호
        Pattern bracketPattern = Pattern.compile("[{}\\[\\]]");
        m = bracketPattern.matcher(text);
        while (m.find()) {
            applyColor(s, m.start(), m.end(), isDarkMode ? COLOR_JSON_BRACKET_DARK : COLOR_JSON_BRACKET_LIGHT);
        }
    }

    private void highlightXml(Editable s) {
        clearSpans(s);
        String text = s.toString();

        // 주석
        Pattern commentPattern = Pattern.compile("<!--[\\s\\S]*?-->");
        Matcher m = commentPattern.matcher(text);
        while (m.find()) {
            applyColor(s, m.start(), m.end(), isDarkMode ? COLOR_XML_COMMENT_DARK : COLOR_XML_COMMENT_LIGHT);
        }

        // CDATA
        Pattern cdataPattern = Pattern.compile("<!\\[CDATA\\[[\\s\\S]*?\\]\\]>");
        m = cdataPattern.matcher(text);
        while (m.find()) {
            applyColor(s, m.start(), m.end(), isDarkMode ? COLOR_XML_CDATA_DARK : COLOR_XML_CDATA_LIGHT);
        }

        // 태그명 (<tag, </tag, />)
        Pattern tagPattern = Pattern.compile("</?([a-zA-Z][a-zA-Z0-9_:.-]*)");
        m = tagPattern.matcher(text);
        while (m.find()) {
            applyColor(s, m.start(), m.end(), isDarkMode ? COLOR_XML_TAG_DARK : COLOR_XML_TAG_LIGHT);
        }

        // 속성명
        Pattern attrPattern = Pattern.compile("\\s([a-zA-Z][a-zA-Z0-9_:.-]*)\\s*=");
        m = attrPattern.matcher(text);
        while (m.find()) {
            applyColor(s, m.start(1), m.end(1), isDarkMode ? COLOR_XML_ATTR_DARK : COLOR_XML_ATTR_LIGHT);
        }

        // 속성값
        Pattern valuePattern = Pattern.compile("=\"([^\"]*)\"");
        m = valuePattern.matcher(text);
        while (m.find()) {
            applyColor(s, m.start(), m.end(), isDarkMode ? COLOR_XML_VALUE_DARK : COLOR_XML_VALUE_LIGHT);
        }
    }
}
