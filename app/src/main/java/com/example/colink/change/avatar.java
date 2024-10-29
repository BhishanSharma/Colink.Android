package com.example.colink.change;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.colink.HomeActivity;
import com.example.colink.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class avatar extends AppCompatActivity {

    private ImageView[] imageViews;
    private ImageView selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar2);

        selected = findViewById(R.id.selected_avatar);
        Button save = findViewById(R.id.save_button);

        loadSavedAvatarImage();

        int[] imageViewIds = {
                R.id.avatar_1,
                R.id.avatar_2,
                R.id.avatar_3,
                R.id.avatar_4,
                R.id.avatar_5,
                R.id.avatar_6,
                R.id.avatar_7,
                R.id.avatar_8,
                R.id.avatar_9
        };

        imageViews = new ImageView[imageViewIds.length];
        for (int i = 0; i < imageViews.length; i++) {
            imageViews[i] = findViewById(imageViewIds[i]);
            int finalI = i;
            imageViews[i].setOnClickListener(
                    v -> {
                        Drawable drawable = imageViews[finalI].getDrawable();
                        selected.setImageDrawable(drawable);
                        Bitmap bitmap = drawableToBitmap(drawable);
                        saveBitmapToFile(bitmap, "selected_avatar.png");
                    });
        }

        save.setOnClickListener(v -> finish());
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bitmap =
                Bitmap.createBitmap(
                        drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void saveBitmapToFile(Bitmap bitmap, String fileName) {
        File directory = new File(Environment.getExternalStorageDirectory(), "CoLink");
        if (!directory.exists() && !directory.mkdirs()) {
            Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(directory, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            Toast.makeText(this, "Image saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedAvatarImage() {
        String filePath =
                Environment.getExternalStorageDirectory().getPath() + "/CoLink/selected_avatar.png";
        File avatarFile = new File(filePath);

        if (avatarFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            selected.setImageBitmap(bitmap);
        } else {
            Toast.makeText(getApplicationContext(), "No avatar image found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        super.onBackPressed();
    }
}