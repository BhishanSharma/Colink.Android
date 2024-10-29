package com.example.colink.change;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.colink.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class name extends AppCompatActivity {
    EditText new_name;
    Button submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_name);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        new_name = findViewById(R.id.new_name);
        submit = findViewById(R.id.submit_button);

        submit.setOnClickListener(v -> {
            String newName = new_name.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateUserName(currentUser, newName);
                finish();
            } else {
                Toast.makeText(name.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserName(FirebaseUser user, String newName) {
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(name.this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(name.this, "Failed to update name", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}