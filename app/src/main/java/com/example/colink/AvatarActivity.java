package com.example.colink;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AvatarActivity extends AppCompatActivity {

    private ImageView[] imageViews;
    private ImageView selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar);

        selected = findViewById(R.id.selected_avatar);
        Button gs = findViewById(R.id.gs_button);

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

        gs.setOnClickListener(
                v -> {
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    finish();
                });
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
}
