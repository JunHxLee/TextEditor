package com.texteditor.app;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.texteditor.app.databinding.ActivityMainBinding;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SyntaxHighlighter highlighter;
    private SharedPreferences prefs;

    private Uri currentFileUri = null;
    private String currentFileName = "새 파일";
    private boolean isModified = false;
    private int currentFontSize = 14;
    private boolean isDarkMode = false;
    private boolean isWordWrap = true;

    // 찾기/바꾸기 상태
    private List<Integer> findResults = new ArrayList<>();
    private int findCurrentIndex = -1;
    private boolean isFindReplaceMode = false; // false=찾기만, true=찾기+바꾸기

    private static final int FONT_SIZE_MIN = 10;
    private static final int FONT_SIZE_MAX = 30;
    private static final String PREF_FONT_SIZE = "font_size";
    private static final String PREF_DARK_MODE = "dark_mode";
    private static final String PREF_WORD_WRAP = "word_wrap";

    private final ActivityResultLauncher<Intent> openFileLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) openFile(uri);
            }
        });

    private final ActivityResultLauncher<Intent> saveFileLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    currentFileUri = uri;
                    saveToUri(uri);
                }
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // setDefaultNightMode은 super.onCreate() 이전에 호출해야
        // Activity recreate() 루프 없이 올바른 테마가 즉시 적용됨
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        loadSettings();
        AppCompatDelegate.setDefaultNightMode(
            isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        setupEditor();
        setupFindReplace();
        applySettings();

        // 외부에서 파일 열기 인텐트 처리
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            openFile(intent.getData());
        }

        updateTitle();
    }

    private void loadSettings() {
        currentFontSize = prefs.getInt(PREF_FONT_SIZE, 14);
        isDarkMode = prefs.getBoolean(PREF_DARK_MODE, false);
        isWordWrap = prefs.getBoolean(PREF_WORD_WRAP, true);
    }

    private void saveSettings() {
        prefs.edit()
            .putInt(PREF_FONT_SIZE, currentFontSize)
            .putBoolean(PREF_DARK_MODE, isDarkMode)
            .putBoolean(PREF_WORD_WRAP, isWordWrap)
            .apply();
    }

    private void setupEditor() {
        highlighter = new SyntaxHighlighter(isDarkMode);
        highlighter.attachToEditText(binding.editText);

        binding.editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateLineNumbers();
                updateStatusBar();
                if (!isModified) {
                    isModified = true;
                    updateTitle();
                }
                // 찾기 바가 열려있으면 결과 갱신
                if (binding.findReplaceBar.getVisibility() == View.VISIBLE) {
                    performFind(binding.findInput.getText().toString(), false);
                }
            }
        });

        // 커서 위치 변경 감지
        binding.editText.setOnClickListener(v -> updateStatusBar());

        // 스크롤 동기화
        binding.scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            binding.lineNumberScrollView.scrollTo(0, scrollY);
        });

        binding.editText.setTypeface(Typeface.MONOSPACE);
    }

    private void setupFindReplace() {
        // 찾기 입력창
        binding.findInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                performFind(s.toString(), false);
            }
        });

        binding.findInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                findNext();
                return true;
            }
            return false;
        });

        binding.findNextBtn.setOnClickListener(v -> findNext());
        binding.findPrevBtn.setOnClickListener(v -> findPrev());
        binding.findCloseBtn.setOnClickListener(v -> closeFindBar());

        binding.replaceBtn.setOnClickListener(v -> replaceCurrentMatch());
        binding.replaceAllBtn.setOnClickListener(v -> replaceAllMatches());
    }

    private void applySettings() {
        highlighter.setDarkMode(isDarkMode);

        binding.editText.setTextSize(currentFontSize);
        binding.lineNumbers.setTextSize(currentFontSize);

        if (isWordWrap) {
            binding.editText.setHorizontallyScrolling(false);
        } else {
            binding.editText.setHorizontallyScrolling(true);
        }

        updateLineNumbers();
    }

    private void updateLineNumbers() {
        String text = binding.editText.getText().toString();
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines.length; i++) {
            sb.append(i);
            if (i < lines.length) sb.append("\n");
        }
        binding.lineNumbers.setText(sb.toString());
    }

    private void updateTitle() {
        String title = currentFileName + (isModified ? " *" : "");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }

        SyntaxHighlighter.FileType ft = highlighter.getFileType();
        String badge;
        switch (ft) {
            case JSON: badge = "JSON"; break;
            case XML:  badge = "XML";  break;
            default:   badge = "TXT";  break;
        }
        binding.fileTypeBadge.setText(badge);

        updateStatusBar();
    }

    private void updateStatusBar() {
        String text = binding.editText.getText().toString();
        int totalLines = text.isEmpty() ? 1 : text.split("\n", -1).length;
        int chars = text.length();

        // 커서 위치 계산
        int cursorPos = binding.editText.getSelectionStart();
        int line = 1;
        int col = 1;
        if (cursorPos >= 0 && cursorPos <= text.length()) {
            String before = text.substring(0, cursorPos);
            String[] beforeLines = before.split("\n", -1);
            line = beforeLines.length;
            col = beforeLines[beforeLines.length - 1].length() + 1;
        }

        binding.statusBar.setText("줄: " + line + "/" + totalLines + "  열: " + col + "  문자: " + chars);
    }

    // ===== 찾기/바꾸기 =====

    private void openFindBar(boolean withReplace) {
        isFindReplaceMode = withReplace;
        binding.findReplaceBar.setVisibility(View.VISIBLE);
        binding.replaceRow.setVisibility(withReplace ? View.VISIBLE : View.GONE);
        binding.findInput.requestFocus();
        String query = binding.findInput.getText().toString();
        if (!query.isEmpty()) {
            performFind(query, false);
        }
    }

    private void closeFindBar() {
        binding.findReplaceBar.setVisibility(View.GONE);
        clearFindHighlights();
        findResults.clear();
        findCurrentIndex = -1;
        binding.findMatchCount.setText("");
    }

    private void performFind(String query, boolean scrollToCurrent) {
        clearFindHighlights();
        findResults.clear();
        findCurrentIndex = -1;

        if (query.isEmpty()) {
            binding.findMatchCount.setText("");
            return;
        }

        String text = binding.editText.getText().toString();
        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();

        int index = 0;
        while ((index = lowerText.indexOf(lowerQuery, index)) != -1) {
            findResults.add(index);
            index += lowerQuery.length();
        }

        if (!findResults.isEmpty()) {
            findCurrentIndex = 0;
            highlightMatches(query.length());
            if (scrollToCurrent) scrollToMatch(findCurrentIndex);
        }

        updateFindCount();
    }

    private void highlightMatches(int queryLen) {
        Editable editable = binding.editText.getText();
        // 기존 하이라이트 제거
        BackgroundColorSpan[] spans = editable.getSpans(0, editable.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan span : spans) editable.removeSpan(span);

        for (int i = 0; i < findResults.size(); i++) {
            int start = findResults.get(i);
            int end = start + queryLen;
            int color = (i == findCurrentIndex) ? 0xFFFF9800 : 0x44FFEB3B; // 현재=주황, 나머지=노랑
            editable.setSpan(new BackgroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void clearFindHighlights() {
        Editable editable = binding.editText.getText();
        BackgroundColorSpan[] spans = editable.getSpans(0, editable.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan span : spans) editable.removeSpan(span);
    }

    private void findNext() {
        if (findResults.isEmpty()) return;
        findCurrentIndex = (findCurrentIndex + 1) % findResults.size();
        int queryLen = binding.findInput.getText().length();
        highlightMatches(queryLen);
        scrollToMatch(findCurrentIndex);
        updateFindCount();
    }

    private void findPrev() {
        if (findResults.isEmpty()) return;
        findCurrentIndex = (findCurrentIndex - 1 + findResults.size()) % findResults.size();
        int queryLen = binding.findInput.getText().length();
        highlightMatches(queryLen);
        scrollToMatch(findCurrentIndex);
        updateFindCount();
    }

    private void scrollToMatch(int idx) {
        if (idx < 0 || idx >= findResults.size()) return;
        int pos = findResults.get(idx);
        binding.editText.setSelection(pos);
        // EditText의 레이아웃을 통해 y좌표 계산 후 스크롤
        binding.editText.post(() -> {
            android.text.Layout layout = binding.editText.getLayout();
            if (layout != null) {
                int line = layout.getLineForOffset(pos);
                int y = layout.getLineTop(line);
                binding.scrollView.smoothScrollTo(0, y);
            }
        });
    }

    private void updateFindCount() {
        if (findResults.isEmpty()) {
            binding.findMatchCount.setText("없음");
        } else {
            binding.findMatchCount.setText((findCurrentIndex + 1) + "/" + findResults.size());
        }
    }

    private void replaceCurrentMatch() {
        if (findCurrentIndex < 0 || findCurrentIndex >= findResults.size()) return;
        String query = binding.findInput.getText().toString();
        String replacement = binding.replaceInput.getText().toString();
        if (query.isEmpty()) return;

        int start = findResults.get(findCurrentIndex);
        int end = start + query.length();
        String text = binding.editText.getText().toString();

        if (end > text.length()) return;

        // 대소문자 무시 비교
        if (!text.substring(start, end).equalsIgnoreCase(query)) return;

        binding.editText.getText().replace(start, end, replacement);
        performFind(query, true);
    }

    private void replaceAllMatches() {
        String query = binding.findInput.getText().toString();
        String replacement = binding.replaceInput.getText().toString();
        if (query.isEmpty()) return;

        String text = binding.editText.getText().toString();
        String newText = text.replaceAll("(?i)" + java.util.regex.Pattern.quote(query),
                                         java.util.regex.Matcher.quoteReplacement(replacement));
        int count = findResults.size();
        binding.editText.setText(newText);
        Toast.makeText(this, count + "개 바꿈 완료", Toast.LENGTH_SHORT).show();
        performFind(query, false);
    }

    // ===== 파일 작업 =====

    private void newFile() {
        if (isModified) {
            showSaveDialog(() -> clearEditor());
        } else {
            clearEditor();
        }
    }

    private void clearEditor() {
        binding.editText.setText("");
        currentFileUri = null;
        currentFileName = "새 파일";
        isModified = false;
        highlighter.setFileType(SyntaxHighlighter.FileType.TEXT);
        updateTitle();
    }

    private void openFileChooser() {
        if (isModified) {
            showSaveDialog(() -> launchFileChooser());
        } else {
            launchFileChooser();
        }
    }

    private void launchFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"text/plain", "application/json", "text/xml",
                              "application/xml", "text/html", "text/csv", "text/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        openFileLauncher.launch(intent);
    }

    private void openFile(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return;
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();

            currentFileUri = uri;
            currentFileName = getFileName(uri);

            SyntaxHighlighter.FileType ft = SyntaxHighlighter.detectFileType(currentFileName);
            highlighter.setFileType(ft);

            isModified = false;
            binding.editText.setText(sb.toString());
            isModified = false;

            Editable editable = binding.editText.getText();
            if (editable != null) highlighter.highlight(editable);

            if (ft == SyntaxHighlighter.FileType.JSON) {
                FileValidator.ValidationResult r = FileValidator.validateJson(sb.toString());
                binding.validationBar.setVisibility(View.VISIBLE);
                binding.validationBar.setText(r.message);
                binding.validationBar.setTextColor(r.isValid ? 0xFF4CAF50 : 0xFFF44336);
            } else if (ft == SyntaxHighlighter.FileType.XML) {
                FileValidator.ValidationResult r = FileValidator.validateXml(sb.toString());
                binding.validationBar.setVisibility(View.VISIBLE);
                binding.validationBar.setText(r.message);
                binding.validationBar.setTextColor(r.isValid ? 0xFF4CAF50 : 0xFFF44336);
            } else {
                binding.validationBar.setVisibility(View.GONE);
            }

            updateTitle();
            Toast.makeText(this, currentFileName + " 열림", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "파일 열기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveFile() {
        if (currentFileUri != null) {
            saveToUri(currentFileUri);
        } else {
            saveFileAs();
        }
    }

    private void saveFileAs() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, currentFileName.equals("새 파일") ? "untitled.txt" : currentFileName);
        saveFileLauncher.launch(intent);
    }

    private void saveToUri(Uri uri) {
        try {
            OutputStream os = getContentResolver().openOutputStream(uri, "wt");
            if (os == null) return;
            os.write(binding.editText.getText().toString().getBytes());
            os.close();
            isModified = false;
            currentFileName = getFileName(uri);
            updateTitle();
            Toast.makeText(this, "저장 완료", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * SAF URI에서 실제 파일명을 추출한다.
     * ContentResolver를 통해 OpenableColumns.DISPLAY_NAME을 조회하고,
     * 실패 시 URI 마지막 경로 세그먼트에서 추출한다.
     */
    private String getFileName(Uri uri) {
        // SAF URI의 경우 ContentResolver로 파일명 조회
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    String name = cursor.getString(nameIndex);
                    if (name != null && !name.isEmpty()) return name;
                }
            }
        } catch (Exception ignored) {}

        // 폴백: 마지막 경로 세그먼트 파싱
        String path = uri.getLastPathSegment();
        if (path == null) return "unknown";
        // SAF는 "primary:Downloads/file.json" 형태일 수 있음
        int slash = path.lastIndexOf('/');
        if (slash >= 0) path = path.substring(slash + 1);
        int colon = path.lastIndexOf(':');
        if (colon >= 0) path = path.substring(colon + 1);
        return path.isEmpty() ? "unknown" : path;
    }

    // ===== 다이얼로그 =====

    private void showSaveDialog(Runnable afterAction) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("저장하지 않은 변경사항")
            .setMessage("변경사항을 저장하시겠습니까?")
            .setPositiveButton("저장", (d, w) -> { saveFile(); afterAction.run(); })
            .setNegativeButton("저장 안함", (d, w) -> afterAction.run())
            .setNeutralButton("취소", null)
            .show();
    }

    private void showFontSizeDialog() {
        String[] sizes = {"10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30"};
        int currentIndex = 0;
        for (int i = 0; i < sizes.length; i++) {
            if (Integer.parseInt(sizes[i]) == currentFontSize) {
                currentIndex = i;
                break;
            }
        }
        new MaterialAlertDialogBuilder(this)
            .setTitle("글자 크기")
            .setSingleChoiceItems(sizes, currentIndex, (d, which) -> {
                currentFontSize = Integer.parseInt(sizes[which]);
                binding.editText.setTextSize(currentFontSize);
                binding.lineNumbers.setTextSize(currentFontSize);
                saveSettings();
                d.dismiss();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    private void showFormatDialog() {
        SyntaxHighlighter.FileType ft = highlighter.getFileType();
        if (ft == SyntaxHighlighter.FileType.JSON) {
            try {
                String formatted = FileValidator.formatJson(binding.editText.getText().toString());
                binding.editText.setText(formatted);
                Toast.makeText(this, "JSON 포맷팅 완료", Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                Toast.makeText(this, "포맷팅 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "JSON 파일만 포맷팅을 지원합니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFileTypeDialog() {
        String[] types = {"일반 텍스트", "JSON", "XML"};
        SyntaxHighlighter.FileType current = highlighter.getFileType();
        int idx = current == SyntaxHighlighter.FileType.JSON ? 1 :
                  current == SyntaxHighlighter.FileType.XML  ? 2 : 0;
        new MaterialAlertDialogBuilder(this)
            .setTitle("파일 형식 변경")
            .setSingleChoiceItems(types, idx, (d, which) -> {
                SyntaxHighlighter.FileType ft = which == 1 ? SyntaxHighlighter.FileType.JSON :
                                                which == 2 ? SyntaxHighlighter.FileType.XML :
                                                             SyntaxHighlighter.FileType.TEXT;
                highlighter.setFileType(ft);
                Editable editable = binding.editText.getText();
                if (editable != null) highlighter.highlight(editable);
                updateTitle();
                d.dismiss();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    // ===== 메뉴 =====

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_new)          { newFile(); return true; }
        if (id == R.id.menu_open)         { openFileChooser(); return true; }
        if (id == R.id.menu_save)         { saveFile(); return true; }
        if (id == R.id.menu_save_as)      { saveFileAs(); return true; }
        if (id == R.id.menu_find)         { openFindBar(false); return true; }
        if (id == R.id.menu_find_replace) { openFindBar(true); return true; }
        if (id == R.id.menu_undo)         { binding.editText.onKeyDown(KeyEvent.KEYCODE_Z,
                                                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z)); return true; }
        if (id == R.id.menu_redo)         { binding.editText.onKeyDown(KeyEvent.KEYCODE_Z,
                                                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_Z)); return true; }
        if (id == R.id.menu_format)       { showFormatDialog(); return true; }
        if (id == R.id.menu_font_size)    { showFontSizeDialog(); return true; }
        if (id == R.id.menu_word_wrap)    {
            isWordWrap = !isWordWrap;
            item.setChecked(isWordWrap);
            applySettings();
            saveSettings();
            return true;
        }
        if (id == R.id.menu_dark_mode)    {
            isDarkMode = !isDarkMode;
            saveSettings();
            AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            return true;
        }
        if (id == R.id.menu_file_type)    { showFileTypeDialog(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem wordWrap = menu.findItem(R.id.menu_word_wrap);
        MenuItem darkMode = menu.findItem(R.id.menu_dark_mode);
        if (wordWrap != null) wordWrap.setChecked(isWordWrap);
        if (darkMode != null) darkMode.setChecked(isDarkMode);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        // 찾기 바가 열려있으면 먼저 닫기
        if (binding.findReplaceBar.getVisibility() == View.VISIBLE) {
            closeFindBar();
            return;
        }
        if (isModified) {
            showSaveDialog(() -> finish());
        } else {
            super.onBackPressed();
        }
    }
}
