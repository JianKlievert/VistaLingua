package com.jethers.mobcompfinalproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import android.content.Intent;
import com.jethers.mobcompfinalproject.translation.TranslationService;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceTranslationActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final String TAG = "VoiceTranslationActivity";
    private static final int PERMISSION_REQUEST_CODE = 123;
    
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private FloatingActionButton recordButton;
    private TextView statusText;
    private EditText recognizedText;
    private TextView translatedText;
    private MaterialButton translateButton;
    private MaterialButton speakRecognizedText;
    private MaterialButton speakTranslatedText;
    private Spinner sourceLanguageSpinner;
    private Spinner targetLanguageSpinner;
    private boolean hasRecordPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_translation);

        // Initialize views
        recordButton = findViewById(R.id.recordButton);
        statusText = findViewById(R.id.statusText);
        recognizedText = findViewById(R.id.recognizedText);
        translatedText = findViewById(R.id.translatedText);
        translateButton = findViewById(R.id.translateButton);
        speakRecognizedText = findViewById(R.id.speakRecognizedText);
        speakTranslatedText = findViewById(R.id.speakTranslatedText);
        sourceLanguageSpinner = findViewById(R.id.sourceLanguageSpinner);
        targetLanguageSpinner = findViewById(R.id.targetLanguageSpinner);

        // Check for record audio permission
        checkPermission();

        // Initialize text-to-speech
        textToSpeech = new TextToSpeech(this, this);

        // Set up language spinners
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            TranslationService.getSupportedLanguages()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        sourceLanguageSpinner.setAdapter(adapter);
        targetLanguageSpinner.setAdapter(adapter);

        // Set default selections
        sourceLanguageSpinner.setSelection(0); // English
        targetLanguageSpinner.setSelection(1); // Spanish

        // Initialize speech recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    statusText.setText("Listening...");
                }

                @Override
                public void onBeginningOfSpeech() {
                    recognizedText.setText("");
                    statusText.setText("Listening...");
                }

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    statusText.setText("Press and hold to record");
                }

                @Override
                public void onError(int error) {
                    String message;
                    switch (error) {
                        case SpeechRecognizer.ERROR_AUDIO:
                            message = "Audio recording error";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            message = "Client side error";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            message = "Insufficient permissions";
                            checkPermission();
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            message = "Network error";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            message = "Network timeout";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            message = "No match found";
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            message = "RecognitionService busy";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                            message = "Server error";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            message = "No speech input";
                            break;
                        default:
                            message = "Error occurred. Please try again.";
                            break;
                    }
                    statusText.setText("Error: " + message);
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String recognizedSpeech = matches.get(0);
                        recognizedText.setText(recognizedSpeech);
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });

            // Set up record button touch listener
            recordButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (hasRecordPermission) {
                        startListening();
                    } else {
                        checkPermission();
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    stopListening();
                }
                return false;
            });
        } else {
            Toast.makeText(this, "Speech recognition not available on this device", 
                    Toast.LENGTH_SHORT).show();
            recordButton.setEnabled(false);
        }

        // Set up translate button
        translateButton.setOnClickListener(v -> performTranslation());
        
        // Set up speak buttons
        speakRecognizedText.setOnClickListener(v -> speakRecognizedText());
        speakTranslatedText.setOnClickListener(v -> speakTranslatedText());
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        } else {
            hasRecordPermission = true;
        }
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        
        // Always use English for voice recognition since we're translating from English
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error occurred. Please try again.",
                    Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error starting speech recognition", e);
        }
    }

    private void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasRecordPermission = true;
                Toast.makeText(this, "Permission granted. You can now use voice recognition.", 
                    Toast.LENGTH_SHORT).show();
            } else {
                hasRecordPermission = false;
                Toast.makeText(this, "Permission denied. Voice recognition unavailable.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void performTranslation() {
        String textToTranslate = recognizedText.getText().toString().trim();
        if (textToTranslate.isEmpty() || textToTranslate.equals(getString(R.string.recognized_text_hint))) {
            Toast.makeText(this, "Please speak something first", Toast.LENGTH_SHORT).show();
            return;
        }

        String sourceLanguage = sourceLanguageSpinner.getSelectedItem().toString();
        String targetLanguage = targetLanguageSpinner.getSelectedItem().toString();

        // Show a loading message
        translatedText.setText("Translating...");

        TranslationService.translateText(textToTranslate, sourceLanguage, targetLanguage,
                new TranslationService.TranslationCallback() {
                    @Override
                    public void onTranslationComplete(String result) {
                        translatedText.setText(result);
                    }

                    @Override
                    public void onTranslationError(Exception e) {
                        translatedText.setText("");
                        Toast.makeText(VoiceTranslationActivity.this,
                                "Translation failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void speakRecognizedText() {
        String text = recognizedText.getText().toString();
        if (text.isEmpty() || text.equals(getString(R.string.recognized_text_hint))) {
            Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show();
            return;
        }

        String sourceLanguage = sourceLanguageSpinner.getSelectedItem().toString();
        String langCode = TranslationService.getLanguageCode(sourceLanguage);
        if (langCode == null) {
            Toast.makeText(this, "Language not supported for speech", Toast.LENGTH_SHORT).show();
            return;
        }

        Locale locale;
        switch (langCode) {
            case "zh":
                locale = Locale.CHINESE;
                break;
            case "ja":
                locale = Locale.JAPANESE;
                break;
            case "ko":
                locale = Locale.KOREAN;
                break;
            case "de":
                locale = Locale.GERMAN;
                break;
            case "fr":
                locale = Locale.FRENCH;
                break;
            case "it":
                locale = Locale.ITALIAN;
                break;
            default:
                locale = new Locale(langCode);
        }

        int result = textToSpeech.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Language not supported for speech", Toast.LENGTH_SHORT).show();
            return;
        }

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void speakTranslatedText() {
        String text = translatedText.getText().toString();
        if (text.isEmpty() || text.equals(getString(R.string.translated_text_hint))) {
            Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetLanguage = targetLanguageSpinner.getSelectedItem().toString();
        String langCode = TranslationService.getLanguageCode(targetLanguage);
        if (langCode == null) {
            Toast.makeText(this, "Language not supported for speech", Toast.LENGTH_SHORT).show();
            return;
        }

        Locale locale;
        switch (langCode) {
            case "zh":
                locale = Locale.CHINESE;
                break;
            case "ja":
                locale = Locale.JAPANESE;
                break;
            case "ko":
                locale = Locale.KOREAN;
                break;
            case "de":
                locale = Locale.GERMAN;
                break;
            case "fr":
                locale = Locale.FRENCH;
                break;
            case "it":
                locale = Locale.ITALIAN;
                break;
            default:
                locale = new Locale(langCode);
        }

        int result = textToSpeech.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Language not supported for speech", Toast.LENGTH_SHORT).show();
            return;
        }

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TextToSpeech initialized successfully");
        } else {
            Log.e(TAG, "TextToSpeech initialization failed");
            Toast.makeText(this, "Text-to-speech failed to initialize", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
