package com.example.smartqrattendance;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private TextView tvStudentRole;
    private TextView tvTeacherRole;
    private TextView tvSecureLogin;
    private TextView tvRegister;
    private TextView tvForgotPassword;

    private android.widget.EditText etEmail;
    private android.widget.EditText etPassword;

    private AppCompatButton btnLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String selectedRole = "student";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // =====================================================
        // FIREBASE
        // =====================================================

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // =====================================================
        // CHECK ALREADY LOGGED-IN USER
        // =====================================================
        /*
         * Firebase Auth automatically keeps the login session.
         *
         * Therefore:
         *
         * App opened for first time
         *       -> Login screen
         *
         * User logged in
         *       -> Firebase remembers user
         *
         * App closed and opened again
         *       -> Directly open correct dashboard
         *
         * No need to login again.
         */

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {

            // User already logged in.
            // Find role and open correct dashboard.
            loadUserRole(currentUser.getUid());

            return;
        }

        // =====================================================
        // VIEWS
        // =====================================================

        tvStudentRole = findViewById(R.id.tvStudentRole);
        tvTeacherRole = findViewById(R.id.tvTeacherRole);

        tvSecureLogin = findViewById(R.id.tvSecureLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnLogin = findViewById(R.id.btnLogin);

        // =====================================================
        // DEFAULT ROLE = STUDENT
        // =====================================================

        selectStudent();

        // =====================================================
        // STUDENT
        // =====================================================

        tvStudentRole.setOnClickListener(v -> selectStudent());

        // =====================================================
        // TEACHER
        // =====================================================

        tvTeacherRole.setOnClickListener(v -> selectTeacher());

        // =====================================================
        // EMAIL / PASSWORD WATCHER
        // =====================================================

        TextWatcher watcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count) {

                updateLoginState();
                updateSecureText();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        etEmail.addTextChangedListener(watcher);
        etPassword.addTextChangedListener(watcher);

        updateLoginState();
        updateSecureText();

        // =====================================================
        // LOGIN
        // =====================================================

        btnLogin.setOnClickListener(v -> loginUser());

        // =====================================================
        // REGISTER
        // =====================================================

        tvRegister.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    RegisterActivity.class
            );

            startActivity(intent);
        });

        // =====================================================
        // FORGOT PASSWORD
        // =====================================================

        tvForgotPassword.setOnClickListener(v -> resetPassword());
    }

    // =========================================================
    // STUDENT SELECT
    // =========================================================

    private void selectStudent() {

        selectedRole = "student";

        tvStudentRole.setBackgroundResource(
                R.drawable.bg_selected_role
        );

        tvStudentRole.setTextColor(
                Color.parseColor("#2454D7")
        );

        tvStudentRole.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        tvTeacherRole.setBackgroundColor(
                Color.TRANSPARENT
        );

        tvTeacherRole.setTextColor(
                Color.parseColor("#98A2B3")
        );

        tvTeacherRole.setTypeface(
                null,
                android.graphics.Typeface.NORMAL
        );
    }

    // =========================================================
    // TEACHER SELECT
    // =========================================================

    private void selectTeacher() {

        selectedRole = "teacher";

        tvTeacherRole.setBackgroundResource(
                R.drawable.bg_selected_role
        );

        tvTeacherRole.setTextColor(
                Color.parseColor("#2454D7")
        );

        tvTeacherRole.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        tvStudentRole.setBackgroundColor(
                Color.TRANSPARENT
        );

        tvStudentRole.setTextColor(
                Color.parseColor("#98A2B3")
        );

        tvStudentRole.setTypeface(
                null,
                android.graphics.Typeface.NORMAL
        );
    }

    // =========================================================
    // SECURE LOGIN TEXT
    // =========================================================

    private void updateSecureText() {

        String email =
                etEmail.getText().toString().trim();

        String password =
                etPassword.getText().toString().trim();

        if (!email.isEmpty() && !password.isEmpty()) {

            tvSecureLogin.setTextColor(
                    Color.parseColor("#172B4D")
            );

            tvSecureLogin.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
            );

        } else {

            tvSecureLogin.setTextColor(
                    Color.parseColor("#98A2B3")
            );

            tvSecureLogin.setTypeface(
                    null,
                    android.graphics.Typeface.NORMAL
            );
        }
    }

    // =========================================================
    // LOGIN BUTTON STATE
    // =========================================================

    private void updateLoginState() {

        String email =
                etEmail.getText().toString().trim();

        String password =
                etPassword.getText().toString().trim();

        boolean valid =
                !email.isEmpty()
                        && Patterns.EMAIL_ADDRESS
                        .matcher(email)
                        .matches()
                        && !password.isEmpty();

        btnLogin.setEnabled(valid);

        if (valid) {

            btnLogin.setAlpha(1.0f);

        } else {

            btnLogin.setAlpha(0.45f);
        }
    }

    // =========================================================
    // LOGIN
    // =========================================================

    private void loginUser() {

        String email =
                etEmail.getText().toString().trim();

        String password =
                etPassword.getText().toString().trim();

        if (email.isEmpty()) {

            etEmail.setError(
                    "Enter your email"
            );

            etEmail.requestFocus();

            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etEmail.setError(
                    "Enter a valid email"
            );

            etEmail.requestFocus();

            return;
        }

        if (password.isEmpty()) {

            etPassword.setError(
                    "Enter your password"
            );

            etPassword.requestFocus();

            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing in...");
        btnLogin.setAlpha(0.65f);

        mAuth.signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {

                        btnLogin.setText("SIGN IN");

                        updateLoginState();

                        String error =
                                "Login failed.";

                        if (task.getException() != null) {

                            error =
                                    task.getException()
                                            .getMessage();
                        }

                        Toast.makeText(
                                MainActivity.this,
                                error,
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    FirebaseUser user =
                            mAuth.getCurrentUser();

                    if (user == null) {

                        btnLogin.setText("SIGN IN");

                        updateLoginState();

                        Toast.makeText(
                                this,
                                "User account not found.",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    loadUserRole(user.getUid());
                });
    }

    // =========================================================
    // FIRESTORE ROLE
    // =========================================================

    private void loadUserRole(String uid) {

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {

                        showError(
                                "User profile not found."
                        );

                        return;
                    }

                    String role =
                            document.getString("role");

                    if (role == null
                            || role.trim().isEmpty()) {

                        showError(
                                "User role is not configured."
                        );

                        return;
                    }

                    role = role.trim();

                    // =================================================
                    // STUDENT ACCOUNT
                    // =================================================

                    if (role.equalsIgnoreCase(
                            "student"
                    )) {

                        openStudentDashboard();

                    }

                    // =================================================
                    // TEACHER ACCOUNT
                    // =================================================

                    else if (role.equalsIgnoreCase(
                            "teacher"
                    )) {

                        openTeacherDashboard();

                    }

                    else {

                        showError(
                                "Invalid user role: "
                                        + role
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    showError(
                            "Could not load user profile."
                    );
                });
    }

    // =========================================================
    // STUDENT DASHBOARD
    // =========================================================

    private void openStudentDashboard() {

        /*
         * Do not show "login successful" toast here.
         *
         * This method is also called when Firebase finds
         * an already logged-in user.
         */

        Intent intent = new Intent(
                MainActivity.this,
                StudentDashboardActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // =========================================================
    // TEACHER DASHBOARD
    // =========================================================

    private void openTeacherDashboard() {

        /*
         * Do not show "login successful" toast here.
         *
         * This method is also called when Firebase finds
         * an already logged-in user.
         */

        Intent intent = new Intent(
                MainActivity.this,
                TeacherDashboardActivity.class
        );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // =========================================================
    // ERROR
    // =========================================================

    private void showError(String message) {

        if (btnLogin != null) {

            btnLogin.setText("SIGN IN");

            updateLoginState();
        }

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // =========================================================
    // FORGOT PASSWORD
    // =========================================================

    private void resetPassword() {

        String email =
                etEmail.getText().toString().trim();

        if (email.isEmpty()) {

            etEmail.setError(
                    "Enter your email first"
            );

            etEmail.requestFocus();

            return;
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etEmail.setError(
                    "Enter a valid email"
            );

            etEmail.requestFocus();

            return;
        }

        tvForgotPassword.setEnabled(false);
        tvForgotPassword.setAlpha(0.5f);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    tvForgotPassword.setEnabled(true);
                    tvForgotPassword.setAlpha(1.0f);

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                this,
                                "Password reset email sent. Check your inbox.",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        String message =
                                "Could not send reset email.";

                        if (task.getException() != null) {

                            message =
                                    task.getException()
                                            .getMessage();
                        }

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}