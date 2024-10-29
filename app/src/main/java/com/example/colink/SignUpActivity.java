package com.example.colink;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.colink.share.NetworkChangeReceiver;
import com.example.colink.share.NetworkUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText usernameText;
    private EditText emailText;
    private EditText passwordText;

    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        initFirebase();
        initViews();

        // Initial network connectivity check
        if (NetworkUtil.isConnectedToInternet(this)) {
            setSignUpButtonListener();
        } else {
            // if the network connects and then again disconnects
            unsetSignUpButtonListener();
            waitForNetworkConnection();
        }
    }

    private void unsetSignUpButtonListener() {
        Button signUpButton = findViewById(R.id.submit_button);
        signUpButton.setOnClickListener(null);
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void initViews() {
        usernameText = findViewById(R.id.username_field);
        emailText = findViewById(R.id.email_field);
        passwordText = findViewById(R.id.password_field);
    }

    private void setSignUpButtonListener() {
        Button signUpButton = findViewById(R.id.submit_button);
        signUpButton.setOnClickListener(v -> signUpUser());
    }

    private void signUpUser() {
        String name = usernameText.getText().toString().trim();
        String email = emailText.getText().toString().trim();
        String password = passwordText.getText().toString().trim();

        if (isInputValid(name, email, password)) {
            checkIfEmailExists(name, email, password);
        }
    }

    private boolean isInputValid(String name, String email, String password) {
        if (TextUtils.isEmpty(name)) {
            usernameText.setError("Name is required");
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            emailText.setError("Email is required");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordText.setError("Password is required");
            return false;
        }
        return true;
    }

    private void checkIfEmailExists(String name, String email, String password) {
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot.isEmpty()) {
                            createUserWithEmailAndPassword(name, email, password);
                        } else {
                            Toast.makeText(SignUpActivity.this, "Email already exists", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "Error checking email: ", task.getException());
                        Toast.makeText(SignUpActivity.this, "Error checking email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserWithEmailAndPassword(String name, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        sendEmailVerification();
                    } else {
                        Toast.makeText(SignUpActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendEmailVerification() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            saveUserData(user);
                            Toast.makeText(SignUpActivity.this, "Verification email sent to " + user.getEmail(), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
                            finish();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void saveUserData(FirebaseUser user) {
        String userId = user.getUid();
        String name = usernameText.getText().toString().trim();
        String email = emailText.getText().toString().trim();

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("email", email);

        db.collection("users").document(userId).set(userMap)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Toast.makeText(SignUpActivity.this, "Failed to store user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void waitForNetworkConnection() {
        networkChangeReceiver = new NetworkChangeReceiver(isConnected -> {
            if (isConnected) {
                setSignUpButtonListener();
            } else {
                Toast.makeText(SignUpActivity.this, "Waiting for network connection...", Toast.LENGTH_SHORT).show();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);
    }

    private void unregisterNetworkReceiver() {
        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);
            networkChangeReceiver = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterNetworkReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-register network receiver if needed
        if (networkChangeReceiver != null) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(networkChangeReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister network receiver to prevent leaks
        unregisterNetworkReceiver();
    }
}