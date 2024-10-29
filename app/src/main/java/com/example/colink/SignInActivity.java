package com.example.colink;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.colink.share.NetworkChangeReceiver;
import com.example.colink.share.NetworkUtil;
import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.io.File;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;

    private EditText emailText;
    private EditText passwordText;

    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        initFirebase();
        initViews();

        // Initial network connectivity check
        if (NetworkUtil.isConnectedToInternet(this)) {
            setClickListeners();
        } else {
            waitForNetworkConnection();
        }

        setupGoogleSignIn();
    }

    private void initFirebase() {
        mAuth = FirebaseAuth.getInstance();
    }

    private void initViews() {
        emailText = findViewById(R.id.email_field);
        passwordText = findViewById(R.id.password_field);
    }

    private void setClickListeners() {
        Button logInButton = findViewById(R.id.submit_button);
        ImageView googleLogin = findViewById(R.id.google_login);
        Button signUpPage = findViewById(R.id.sign_up_page);

        logInButton.setOnClickListener(v -> loginUser());
        googleLogin.setOnClickListener(v -> signInWithGoogle());
        signUpPage.setOnClickListener(v -> navigateToSignUpPage());
    }

    private void setupGoogleSignIn() {
        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();
    }

    private void loginUser() {
        String email = emailText.getText().toString().trim();
        String password = passwordText.getText().toString().trim();

        if (isInputValid(email, password)) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            onLoginSuccess();
                        } else {
                            showToast("Authentication failed.");
                        }
                    });
        }
    }

    private boolean isInputValid(String email, String password) {
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

    private void onLoginSuccess() {
        showToast("Login successful");
        if (checkAvatar()) {
            navigateToHome();
        } else {
            navigateToAvatar();
        }
        finish();
    }

    private void signInWithGoogle() {
        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> {
                    try {
                        startIntentSenderForResult(result.getPendingIntent().getIntentSender(), RC_SIGN_IN, null, 0, 0, 0);
                    } catch (Exception e) {
                        Log.e(TAG, "Google Sign-In failed", e);
                    }
                })
                .addOnFailureListener(this, e -> Log.e(TAG, "Google Sign-In failed", e));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken);
                }
            } catch (ApiException e) {
                Log.e(TAG, "Google Sign-In failed", e);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        onGoogleSignInSuccess();
                    } else {
                        Log.e(TAG, "Google Sign-In failed", task.getException());
                        showToast("Authentication failed.");
                    }
                });
    }

    private void onGoogleSignInSuccess() {
        showToast("Google Sign-In successful");
        if (checkAvatar()) {
            navigateToHome();
        } else {
            navigateToAvatar();
        }
        finish();
    }

    private boolean checkAvatar() {
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CoLink/selected_avatar.png";
        File avatarFile = new File(filePath);
        return avatarFile.exists();
    }

    private void navigateToSignUpPage() {
        Intent signUpPage = new Intent(SignInActivity.this, SignUpActivity.class);
        startActivity(signUpPage);
    }

    private void navigateToHome() {
        startActivity(new Intent(SignInActivity.this, HomeActivity.class));
    }

    private void navigateToAvatar() {
        startActivity(new Intent(SignInActivity.this, AvatarActivity.class));
    }

    private void showToast(String message) {
        Toast.makeText(SignInActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void waitForNetworkConnection() {
        networkChangeReceiver = new NetworkChangeReceiver(isConnected -> {
            if (isConnected) {
                // Perform actions that require network connectivity
                setClickListeners();
            } else {
                Toast.makeText(SignInActivity.this, "Waiting for network connection...", Toast.LENGTH_SHORT).show();
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
}
