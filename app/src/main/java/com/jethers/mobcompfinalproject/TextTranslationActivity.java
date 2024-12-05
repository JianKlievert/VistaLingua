package com.jethers.mobcompfinalproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.method.ScrollingMovementMethod;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.jethers.mobcompfinalproject.translation.TranslationService;

import java.io.IOException;
import java.util.Locale;

public class TextTranslationActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final String TAG = "TextTranslationActivity";
    private static final int CAMERA_PERMISSION_CODE = 100;

    private ImageView imagePreview;
    private EditText extractedText;
    private TextView translatedText;
    private MaterialButton takePictureButton;
    private MaterialButton uploadImageButton;
    private MaterialButton translateButton;
    private MaterialButton speakExtractedText;
    private MaterialButton speakTranslatedText;
    private Spinner sourceLanguageSpinner;
    private Spinner targetLanguageSpinner;

    private Bitmap currentImageBitmap;
    private TextRecognizer textRecognizer;
    private TextToSpeech textToSpeech;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_translation);

        // Initialize views
        imagePreview = findViewById(R.id.imagePreview);
        extractedText = findViewById(R.id.extractedText);
        translatedText = findViewById(R.id.translatedText);
        takePictureButton = findViewById(R.id.takePictureButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        translateButton = findViewById(R.id.translateButton);
        speakExtractedText = findViewById(R.id.speakExtractedText);
        speakTranslatedText = findViewById(R.id.speakTranslatedText);
        sourceLanguageSpinner = findViewById(R.id.sourceLanguageSpinner);
        targetLanguageSpinner = findViewById(R.id.targetLanguageSpinner);

        // Make EditTexts scrollable
        extractedText.setMovementMethod(new ScrollingMovementMethod());
        translatedText.setMovementMethod(new ScrollingMovementMethod());

        // Initialize ML Kit text recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Initialize text-to-speech
        textToSpeech = new TextToSpeech(this, this);

        // Set up language spinners
        ArrayAdapter<String> languageAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            TranslationService.getSupportedLanguages()
        );
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceLanguageSpinner.setAdapter(languageAdapter);
        targetLanguageSpinner.setAdapter(languageAdapter);

        // Set default selections
        sourceLanguageSpinner.setSelection(0); // English
        targetLanguageSpinner.setSelection(1); // Spanish

        // Initialize activity result launchers
        initializeActivityResultLaunchers();

        // Set click listeners
        takePictureButton.setOnClickListener(v -> checkCameraPermission());
        uploadImageButton.setOnClickListener(v -> openGallery());
        translateButton.setOnClickListener(v -> translateText());

        // Set click listeners for speak buttons
        speakExtractedText.setOnClickListener(v -> speakText(extractedText.getText().toString(), 
            sourceLanguageSpinner.getSelectedItem().toString()));
        speakTranslatedText.setOnClickListener(v -> speakText(translatedText.getText().toString(), 
            targetLanguageSpinner.getSelectedItem().toString()));
    }

    private void initializeActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        currentImageBitmap = (Bitmap) extras.get("data");
                        imagePreview.setImageBitmap(currentImageBitmap);
                        extractTextFromImage();
                    }
                }
            }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        loadImageFromGallery(selectedImage);
                    }
                }
        );

        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, R.string.error_camera_permission, Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        galleryLauncher.launch(galleryIntent);
    }

    private void loadImageFromGallery(Uri selectedImage) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), selectedImage);
                currentImageBitmap = ImageDecoder.decodeBitmap(source);
            } else {
                currentImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
            }
            imagePreview.setImageBitmap(currentImageBitmap);
            extractTextFromImage();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error loading image", e);
        }
    }

    private void extractTextFromImage() {
        if (currentImageBitmap == null) {
            Log.e(TAG, "No image to extract text from");
            return;
        }

        InputImage image = InputImage.fromBitmap(currentImageBitmap, 0);
        textRecognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String recognizedText = visionText.getText();
                    Log.d(TAG, "Extracted text: " + recognizedText);
                    if (recognizedText.isEmpty()) {
                        extractedText.setText(R.string.error_no_text_found);
                    } else {
                        extractedText.setText(recognizedText);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Text recognition failed", e);
                    extractedText.setText(R.string.error_extract_text);
                    Toast.makeText(this, R.string.error_extract_text, Toast.LENGTH_SHORT).show();
                });
    }

    private void translateText() {
        String sourceText = extractedText.getText().toString();
        Log.d(TAG, "Source text for translation: " + sourceText);
        
        if (sourceText.isEmpty() || sourceText.equals(getString(R.string.error_no_text_found)) || 
            sourceText.equals(getString(R.string.error_extract_text))) {
            Toast.makeText(this, R.string.error_no_text_to_translate, Toast.LENGTH_SHORT).show();
            return;
        }

        String sourceLanguage = sourceLanguageSpinner.getSelectedItem().toString();
        String targetLanguage = targetLanguageSpinner.getSelectedItem().toString();
        Log.d(TAG, "Source language: " + sourceLanguage);
        Log.d(TAG, "Target language: " + targetLanguage);
        
        // Show loading state
        translatedText.setText(R.string.translating);
        
        TranslationService.translateText(sourceText, sourceLanguage, targetLanguage, new TranslationService.TranslationCallback() {
            @Override
            public void onTranslationComplete(String result) {
                Log.d(TAG, "Translation successful: " + result);
                runOnUiThread(() -> {
                    translatedText.setText(result);
                });
            }

            @Override
            public void onTranslationError(Exception e) {
                Log.e(TAG, "Translation error", e);
                runOnUiThread(() -> {
                    Toast.makeText(TextTranslationActivity.this, 
                        getString(R.string.error_translation) + ": " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    translatedText.setText("");
                });
            }
        });
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

    private void speakText(String text, String language) {
        if (text == null || text.isEmpty() || text.equals(getString(R.string.extracted_text_hint)) || 
            text.equals(getString(R.string.translated_text_hint))) {
            Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show();
            return;
        }

        String langCode = TranslationService.getLanguageCode(language);
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
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
