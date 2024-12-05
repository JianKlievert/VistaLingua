package com.jethers.mobcompfinalproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class MenuActivity extends AppCompatActivity {
    private MaterialButton textTranslationButton;
    private MaterialButton voiceTranslationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // Initialize buttons
        textTranslationButton = findViewById(R.id.textTranslationButton);
        voiceTranslationButton = findViewById(R.id.voiceTranslationButton);

        // Set click listeners
        textTranslationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start TextTranslationActivity
                Intent intent = new Intent(MenuActivity.this, TextTranslationActivity.class);
                startActivity(intent);
            }
        });

        voiceTranslationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start VoiceTranslationActivity
                Intent intent = new Intent(MenuActivity.this, VoiceTranslationActivity.class);
                startActivity(intent);
            }
        });
    }
}
