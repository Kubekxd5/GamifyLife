package com.example.gamifylife;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gamifylife.MainActivity;
import com.example.gamifylife.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Random;

public class LoginActivity extends BaseActivity {

    EditText editTextEmail;
    EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewGoToRegister, textViewForgotPassword;
    ProgressBar progressBar;
    FirebaseAuth mAuth;
    EditText editTextCaptchaAnswer;
    TextView textViewCaptchaQuestion;
    int captchaNum1;
    int captchaNum2;
    int captchaExpectedAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Make sure this matches your XML file name

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextLoginEmail);
        editTextPassword = findViewById(R.id.editTextLoginPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewGoToRegister = findViewById(R.id.textViewGoToRegister);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        progressBar = findViewById(R.id.progressBarLogin);

        editTextCaptchaAnswer = findViewById(R.id.editTextCaptchaAnswer); // Add to your XML
        textViewCaptchaQuestion = findViewById(R.id.textViewCaptchaQuestion); // Add to your XML
        generateNewCaptcha();

        textViewGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            // Optional: finish LoginActivity
        });

        buttonLogin.setOnClickListener(v -> {
            if (isCaptchaValid()) {
                loginUser();
            } else {
                Toast.makeText(LoginActivity.this, "Invalid CAPTCHA", Toast.LENGTH_SHORT).show();
                generateNewCaptcha(); // Generate a new one
            }
        });

        buttonLogin.setOnClickListener(v -> loginUser());

        textViewForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // User is already signed in, go to MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish(); // Finish LoginActivity so user can't go back to it
        }
    }

    private void generateNewCaptcha() {
        Random random = new Random();
        captchaNum1 = random.nextInt(10); // 0-9
        captchaNum2 = random.nextInt(10); // 0-9
        captchaExpectedAnswer = captchaNum1 + captchaNum2;
        textViewCaptchaQuestion.setText("What is " + captchaNum1 + " + " + captchaNum2 + "?");
        editTextCaptchaAnswer.setText(""); // Clear previous answer
    }

    boolean isCaptchaValid() {
        String answerStr = editTextCaptchaAnswer.getText().toString().trim();
        if (TextUtils.isEmpty(answerStr)) {
            return false;
        }
        try {
            int userAnswer = Integer.parseInt(answerStr);
            return userAnswer == captchaExpectedAnswer;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required.");
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email.");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required.");
            editTextPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(LoginActivity.this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                        editTextEmail.setError(getString(R.string.login_email_required));
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish(); // Finish LoginActivity
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_forgot_password, null); // Create this layout
        builder.setView(dialogView);

        final EditText emailEditText = dialogView.findViewById(R.id.editTextForgotPasswordEmail);
        final Button resetButton = dialogView.findViewById(R.id.buttonSendResetEmail);
        final ProgressBar progressDialog = dialogView.findViewById(R.id.progressBarForgotPassword);


        final AlertDialog dialog = builder.create();

        resetButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.setError("Enter a valid email");
                return;
            }
            progressDialog.setVisibility(View.VISIBLE);
            resetButton.setEnabled(false);

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        progressDialog.setVisibility(View.GONE);
                        resetButton.setEnabled(true);
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Password reset email sent.", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(LoginActivity.this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
        dialog.show();
    }
}