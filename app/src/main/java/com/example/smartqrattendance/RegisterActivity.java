package com.example.smartqrattendance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private Spinner spinnerRole;
    private EditText etName;
    private EditText etRegisterEmail;
    private EditText etRegisterPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private TextView tvBackToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        // =========================
        // FIREBASE
        // =========================

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // =========================
        // XML VIEWS
        // =========================

        spinnerRole = findViewById(R.id.spinnerRole);
        etName = findViewById(R.id.etName);
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        // =========================
        // ROLE SPINNER
        // =========================

        String[] roles = {
                "Select Account Type",
                "Student",
                "Teacher"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                roles
        );

        adapter.setDropDownViewResource(
                R.layout.spinner_item
        );

        spinnerRole.setAdapter(adapter);

        // =========================
        // CREATE ACCOUNT
        // =========================

        btnRegister.setOnClickListener(v -> registerUser());

        // =========================
        // BACK TO LOGIN
        // =========================

        tvBackToLogin.setOnClickListener(v -> {

            Intent intent = new Intent(
                    RegisterActivity.this,
                    MainActivity.class
            );

            intent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
        });
    }

    // =========================================================
    // REGISTER USER
    // =========================================================

    private void registerUser() {

        // =========================
        // GET VALUES
        // =========================

        String role = spinnerRole
                .getSelectedItem()
                .toString()
                .trim();

        String name = etName
                .getText()
                .toString()
                .trim();

        String email = etRegisterEmail
                .getText()
                .toString()
                .trim();

        String password = etRegisterPassword
                .getText()
                .toString()
                .trim();

        String confirmPassword = etConfirmPassword
                .getText()
                .toString()
                .trim();

        // =========================
        // ROLE VALIDATION
        // =========================

        if (role.equals("Select Account Type")) {

            Toast.makeText(
                    this,
                    "Please select Student or Teacher",
                    Toast.LENGTH_SHORT
            ).show();

            spinnerRole.requestFocus();
            return;
        }

        // =========================
        // NAME VALIDATION
        // =========================

        if (name.isEmpty()) {

            etName.setError(
                    "Enter your full name"
            );

            etName.requestFocus();
            return;
        }

        // =========================
        // EMAIL VALIDATION
        // =========================

        if (email.isEmpty()) {

            etRegisterEmail.setError(
                    "Enter your email"
            );

            etRegisterEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            etRegisterEmail.setError(
                    "Enter a valid email"
            );

            etRegisterEmail.requestFocus();
            return;
        }

        // =========================
        // PASSWORD VALIDATION
        // =========================

        if (password.isEmpty()) {

            etRegisterPassword.setError(
                    "Enter password"
            );

            etRegisterPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {

            etRegisterPassword.setError(
                    "Password must be at least 6 characters"
            );

            etRegisterPassword.requestFocus();
            return;
        }

        // =========================
        // CONFIRM PASSWORD
        // =========================

        if (confirmPassword.isEmpty()) {

            etConfirmPassword.setError(
                    "Confirm your password"
            );

            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {

            etConfirmPassword.setError(
                    "Passwords do not match"
            );

            etConfirmPassword.requestFocus();
            return;
        }

        // =========================
        // DISABLE BUTTON
        // =========================

        btnRegister.setEnabled(false);
        btnRegister.setText("CREATING...");

        // =========================
        // FIREBASE AUTH
        // =========================

        mAuth.createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(
                        this,
                        task -> {

                            // =========================
                            // AUTH FAILED
                            // =========================

                            if (!task.isSuccessful()) {

                                btnRegister.setEnabled(true);
                                btnRegister.setText(
                                        "CREATE ACCOUNT"
                                );

                                String message =
                                        "Registration failed.";

                                if (task.getException() != null) {

                                    message =
                                            getFirebaseErrorMessage(
                                                    task.getException()
                                            );
                                }

                                Toast.makeText(
                                        RegisterActivity.this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            // =========================
                            // GET FIREBASE USER
                            // =========================

                            FirebaseUser user =
                                    mAuth.getCurrentUser();

                            if (user == null) {

                                btnRegister.setEnabled(true);
                                btnRegister.setText(
                                        "CREATE ACCOUNT"
                                );

                                Toast.makeText(
                                        RegisterActivity.this,
                                        "Account creation failed.",
                                        Toast.LENGTH_LONG
                                ).show();

                                return;
                            }

                            String uid = user.getUid();

                            // =========================
                            // CREATE USER DATA
                            // =========================

                            Map<String, Object> userData =
                                    new HashMap<>();

                            userData.put(
                                    "name",
                                    name
                            );

                            userData.put(
                                    "email",
                                    email
                            );

                            userData.put(
                                    "role",
                                    role
                            );

                            userData.put(
                                    "uid",
                                    uid
                            );

                            // =========================
                            // SAVE TO FIRESTORE
                            //
                            // users/{uid}
                            // =========================

                            db.collection("users")
                                    .document(uid)
                                    .set(userData)
                                    .addOnSuccessListener(
                                            unused -> {

                                                // =========================
                                                // FIRESTORE SUCCESS
                                                // =========================

                                                Toast.makeText(
                                                        RegisterActivity.this,
                                                        "Account created successfully!",
                                                        Toast.LENGTH_LONG
                                                ).show();

                                                // Logout
                                                // after Firestore save
                                                mAuth.signOut();

                                                // =========================
                                                // GO TO LOGIN
                                                // =========================

                                                Intent intent =
                                                        new Intent(
                                                                RegisterActivity.this,
                                                                MainActivity.class
                                                        );

                                                intent.setFlags(
                                                        Intent.FLAG_ACTIVITY_NEW_TASK
                                                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                );

                                                startActivity(intent);
                                                finish();
                                            }
                                    )
                                    .addOnFailureListener(
                                            e -> {

                                                // =========================
                                                // FIRESTORE FAILED
                                                // =========================

                                                btnRegister.setEnabled(
                                                        true
                                                );

                                                btnRegister.setText(
                                                        "CREATE ACCOUNT"
                                                );

                                                Toast.makeText(
                                                        RegisterActivity.this,
                                                        "Account created, but profile save failed:\n"
                                                                + e.getMessage(),
                                                        Toast.LENGTH_LONG
                                                ).show();
                                            }
                                    );
                        }
                );
    }

    // =========================================================
    // FIREBASE ERROR MESSAGE
    // =========================================================

    private String getFirebaseErrorMessage(
            Exception exception
    ) {

        String error =
                exception.getMessage();

        if (error == null) {
            return "Registration failed.";
        }

        if (error.contains(
                "The email address is already in use"
        )) {

            return "This email is already registered.";
        }

        if (error.contains(
                "The email address is badly formatted"
        )) {

            return "Invalid email address.";
        }

        if (error.contains(
                "The given password is invalid"
        )) {

            return "Password is too weak.";
        }

        if (error.contains(
                "A network error"
        )) {

            return "Network error. Check your internet.";
        }

        return error;
    }
}