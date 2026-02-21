# 📝 Text Editor - Android App

JSON, XML, 텍스트 파일을 위한 신택스 하이라이팅 에디터

## ✨ 기능

- **신택스 하이라이팅** — JSON, XML 파일 형식 자동 감지 및 컬러 하이라이팅
- **JSON 유효성 검사** — 파일 열 때 자동 검사 및 오류 표시
- **JSON 포맷팅** — 메뉴 → JSON 포맷팅으로 예쁘게 정렬
- **XML 유효성 검사** — XML 파싱 오류 자동 감지
- **줄 번호 표시** — 항상 표시
- **다크 모드 / 라이트 모드** 전환
- **글자 크기 조절** — 10~30sp
- **줄 바꿈 토글** — 가로 스크롤 모드 지원
- **파일 열기/저장** — Android Storage Access Framework 사용
- **외부 파일 연동** — 파일 앱에서 직접 열기 지원 (text/*, JSON, XML)

## 📱 지원 파일 형식

| 형식 | 확장자 | 하이라이팅 | 유효성 검사 | 포맷팅 |
|------|--------|-----------|------------|--------|
| 일반 텍스트 | .txt, .md, .csv, ... | ✗ | ✗ | ✗ |
| JSON | .json | ✅ | ✅ | ✅ |
| XML / HTML | .xml, .html, .htm | ✅ | ✅ | ✗ |

## 🚀 APK 빌드 방법

### 방법 1: GitHub Actions (추천)

1. 이 저장소를 GitHub에 업로드
2. `Actions` 탭 클릭
3. `Build APK` 워크플로우 실행
4. 빌드 완료 후 `Artifacts`에서 APK 다운로드

### 방법 2: Android Studio

1. [Android Studio](https://developer.android.com/studio) 설치
2. 프로젝트 폴더 열기
3. `Build` → `Build Bundle(s)/APK(s)` → `Build APK(s)`
4. `app/build/outputs/apk/debug/` 폴더에서 APK 확인

## 📋 요구사항

- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 34 (Android 14)
- **Java**: 8 이상

## 📂 프로젝트 구조

```
TextEditor/
├── app/src/main/
│   ├── java/com/texteditor/app/
│   │   ├── MainActivity.java       # 메인 액티비티
│   │   ├── SyntaxHighlighter.java  # 신택스 하이라이팅 엔진
│   │   └── FileValidator.java      # JSON/XML 유효성 검사
│   ├── res/
│   │   ├── layout/activity_main.xml
│   │   ├── menu/main_menu.xml
│   │   └── values/
│   └── AndroidManifest.xml
└── .github/workflows/build.yml    # GitHub Actions APK 빌드
```
# TextEditor
# TextEditor
# TextEditor
