package com.jethers.mobcompfinalproject.translation;

import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class TranslationService {
    private static final String TAG = "TranslationService";
    private static final String API_URL = "https://api.mymemory.translated.net/get";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    private static final Map<String, String> LANGUAGE_CODES = new HashMap<String, String>() {{
        put("English", "en");
        put("Spanish", "es");
        put("French", "fr");
        put("German", "de");
        put("Italian", "it");
        put("Portuguese", "pt");
        put("Russian", "ru");
        put("Chinese", "zh");
        put("Japanese", "ja");
        put("Korean", "ko");
    }};

    private static final Map<String, String> LANGUAGE_NAMES = new HashMap<String, String>() {{
        put("en", "English");
        put("es", "Spanish");
        put("fr", "French");
        put("de", "German");
        put("it", "Italian");
        put("pt", "Portuguese");
        put("ru", "Russian");
        put("zh", "Chinese");
        put("ja", "Japanese");
        put("ko", "Korean");
    }};

    public interface TranslationCallback {
        void onTranslationComplete(String translatedText);
        void onTranslationError(Exception e);
    }

    private static String buildTranslationUrl(String text, String sourceLang, String targetLang) {
        try {
            // Clean and encode the text
            String cleanedText = text.trim()
                                   .replaceAll("\\s+", " ")  // Replace multiple spaces with single space
                                   .replaceAll("[\\p{Punct}&&[^']]", ""); // Remove punctuation except apostrophes
            
            String encodedText = URLEncoder.encode(cleanedText, "UTF-8");
            
            return String.format(
                "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s&de=example@email.com",
                encodedText, sourceLang, targetLang
            );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void translateText(String text, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
        new AsyncTask<Void, Void, String>() {
            private Exception exception;

            @Override
            protected String doInBackground(Void... params) {
                try {
                    String sourceLangCode = getLanguageCode(sourceLanguage);
                    String targetLangCode = getLanguageCode(targetLanguage);

                    if (sourceLangCode == null || targetLangCode == null) {
                        throw new Exception("Invalid language code");
                    }

                    String url = buildTranslationUrl(text, sourceLangCode, targetLangCode);
                    if (url == null) {
                        throw new Exception("Failed to build URL");
                    }

                    Request request = new Request.Builder()
                            .url(url)
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected response " + response);
                        }

                        String responseData = response.body().string();
                        JsonObject jsonResponse = gson.fromJson(responseData, JsonObject.class);
                        
                        if (jsonResponse.has("responseData") && 
                            jsonResponse.getAsJsonObject("responseData").has("translatedText")) {
                            return jsonResponse.getAsJsonObject("responseData")
                                    .get("translatedText").getAsString();
                        } else {
                            throw new Exception("Invalid response format");
                        }
                    }
                } catch (Exception e) {
                    this.exception = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (exception != null) {
                    callback.onTranslationError(exception);
                } else if (result != null) {
                    callback.onTranslationComplete(result);
                } else {
                    callback.onTranslationError(new Exception("Translation failed"));
                }
            }
        }.execute();
    }

    public static String getLanguageCode(String language) {
        return LANGUAGE_CODES.get(language);
    }

    public static String getLanguageName(String code) {
        return LANGUAGE_NAMES.get(code);
    }

    public static String[] getSupportedLanguages() {
        return LANGUAGE_CODES.keySet().toArray(new String[0]);
    }
}
