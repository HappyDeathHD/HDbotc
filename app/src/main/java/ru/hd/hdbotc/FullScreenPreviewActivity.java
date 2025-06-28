package ru.hd.hdbotc;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FullScreenPreviewActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String EXTRA_IMAGE_PATH = "image_path";

    public static void start(AppCompatActivity activity, Bitmap image) {
        File tempFile = saveBitmapToTempFile(activity, image);
        if (tempFile == null) {
            Toast.makeText(activity, "Не удалось сохранить изображение", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(activity, FullScreenPreviewActivity.class);
        intent.putExtra(EXTRA_IMAGE_PATH, tempFile.getAbsolutePath());
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_preview);

        ImageView fullScreenImage = findViewById(R.id.fullScreenImage);
        Button saveButton = findViewById(R.id.saveButton);

        String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        if (imagePath != null) {
            Bitmap image = BitmapFactory.decodeFile(imagePath);
            if (image != null) {
                fullScreenImage.setImageBitmap(image);

                saveButton.setOnClickListener(v -> saveImageToDownloads(image));
            }
        }
    }

    private void saveImageToDownloads(Bitmap bitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".png";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Используем MediaStore для Android 10+ (API 29+)
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            ContentResolver resolver = getContentResolver();
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                }
                Toast.makeText(this, "Изображение сохранено в загрузках", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Не удалось сохранить изображение", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Используем классический подход для устройств с API < 29
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File imageFile = new File(downloadsDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();

                // Уведомляем систему о новом файле
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(imageFile);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);

                Toast.makeText(this, "Изображение сохранено в загрузках", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Не удалось сохранить изображение", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static File saveBitmapToTempFile(Context context, Bitmap bitmap) {
        try {
            File tempFile = File.createTempFile("preview_", ".png", context.getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}