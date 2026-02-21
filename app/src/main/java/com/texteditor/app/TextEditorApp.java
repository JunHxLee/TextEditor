package com.texteditor.app;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class TextEditorApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Activity가 생성되기 전에 야간 모드를 설정해야
        // attachBaseContext2()가 올바른 모드로 컨텍스트를 래핑하여
        // Activity recreate() 없이 앱이 바로 정상 실행됨
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
            isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}
