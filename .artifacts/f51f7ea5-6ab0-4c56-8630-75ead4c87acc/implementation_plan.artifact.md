# Gemini API Integration & Adaptive Multi-pane Layout

Implement Gemini API integration to answer OCR-extracted questions and display them using an adaptive layout with Navigation 3.

## User Review Required

> [!IMPORTANT]
> The Gemini API key must be provided in the `local.properties` file as `GEMINI_API_KEY=your_api_key_here`.

## Proposed Changes

### Gemini API Integration

#### [NEW] [GeminiService.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplicationanswering/app/src/main/java/com/example/myapplication_answering/api/GeminiService.kt)
- Create a service to interact with the Google AI SDK.
- Use `generative-ai` library to prompt Gemini with the extracted question and options.

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplicationanswering/app/src/main/java/com/example/myapplication_answering/viewmodel/MainViewModel.kt)
- Integrate `GeminiService`.
- Update `UiState` to include `geminiAnswer`.
- Update `processImage` to trigger Gemini answer generation after OCR success.

### Navigation 3 & Adaptive Layout

#### [NEW] [Destinations.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplicationanswering/app/src/main/java/com/example/myapplication_answering/ui/navigation/Destinations.kt)
- Define `@Serializable` destinations for Navigation 3.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplicationanswering/app/src/main/java/com/example/myapplication_answering/MainActivity.kt)
- Setup Navigation 3 `NavDisplay`.
- Use `AdaptiveNavDisplay` for adaptive behavior.

#### [MODIFY] [MainScreen.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplicationanswering/app/src/main/java/com/example/myapplication_answering/ui/screens/MainScreen.kt)
- Refactor to use `ListDetailPaneScaffold` (or `SupportingPaneScaffold`).
- Separate OCR result view and Gemini answer view.
- Handle different screen sizes.

### Build Configuration

#### [MODIFY] [build.gradle.kts](file:///C:/Users/User/AndroidStudioProjects/MyApplicationanswering/app/build.gradle.kts)
- Add BuildConfig support to access the API key from `local.properties`.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew assembleDebug`.
- Unit tests for `GeminiService` (mocking the SDK if possible).

### Manual Verification
- Verify that selecting an image triggers OCR and then Gemini.
- Verify that on tablets, the question and answer are side-by-side.
- Verify that on phones, they are shown sequentially or in a way that fits the screen.
