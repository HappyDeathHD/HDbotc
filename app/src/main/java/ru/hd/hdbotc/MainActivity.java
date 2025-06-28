package ru.hd.hdbotc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import ru.hd.hdbotc.model.ScriptStyles;
import ru.hd.hdbotc.utils.generator.PngGenerator;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText jsonInput;
    private MaterialButton filePickerButton;
    private MaterialButton generateButton;
    private ImageView previewImage;
    private Spinner scriptStyleSpinner;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    try {
                        String jsonContent = readFileContent(fileUri);
                        jsonInput.setText(jsonContent);
                    } catch (IOException e) {
                        Toast.makeText(this, "Ошибка чтения файла", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        jsonInput = findViewById(R.id.jsonInput);
        filePickerButton = findViewById(R.id.filePickerButton);
        generateButton = findViewById(R.id.generateButton);
        previewImage = findViewById(R.id.previewImage);
        scriptStyleSpinner = findViewById(R.id.scriptStyleSpinner);
        setupFilePicker();
        setupSpinner();
        setupGenerateButton();
    }

    private void setupFilePicker() {
        filePickerButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/json");
            filePickerLauncher.launch(intent); // Запуск выбора файла
        });
    }

    private void setupGenerateButton() {
        generateButton.setOnClickListener(v -> {
            String jsonText = jsonInput.getText().toString();
            if (jsonText.isEmpty()) {
                Toast.makeText(this, "Введите или выберите JSON", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONArray jsonArray = new JSONArray(jsonText);
                Bitmap generatedImage = generateImageFromJson(jsonArray);
                previewImage.setImageBitmap(generatedImage);

                previewImage.setOnClickListener(view -> showFullScreenPreview(generatedImage));
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка обработки JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    private void setupSpinner() {
        String[] styles = new String[ScriptStyles.values().length];
        for (int i = 0; i < ScriptStyles.values().length; i++) {
            styles[i] = ScriptStyles.values()[i].getName();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                styles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scriptStyleSpinner.setAdapter(adapter);
    }

    private String readFileContent(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        reader.close();
        return stringBuilder.toString();
    }

    private Bitmap generateImageFromJson(JSONArray jsonArray) throws Exception {
        return PngGenerator.generatePng(this, jsonArray, ScriptStyles.values()[scriptStyleSpinner.getSelectedItemPosition()]);
    }

    private void showFullScreenPreview(Bitmap image) {
        FullScreenPreviewActivity.start(this, image);
    }
}