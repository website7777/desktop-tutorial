package com.example.keyboard3;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.net.Uri; // ← ДОБАВЬТЕ ЭТУ СТРОЧКУ
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button enableButton = findViewById(R.id.enable_button);
        Button selectButton = findViewById(R.id.select_button);
        Button permissionsButton = findViewById(R.id.permissions_button);

        enableButton.setOnClickListener(v -> {
            // Открывает настройки клавиатуры
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            startActivity(intent);
        });

        selectButton.setOnClickListener(v -> {
            // Открывает выбор клавиатуры
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS);
            startActivity(intent);
        });

        permissionsButton.setOnClickListener(v -> {
            // Открывает настройки разрешений
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
    }
}