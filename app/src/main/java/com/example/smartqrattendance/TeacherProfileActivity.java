package com.example.smartqrattendance;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TeacherProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileRole;
    private TextView tvProfileInitial;
    private TextView tvBack;

    private EditText etEditName;

    private Button btnEditProfile;
    private Button btnSaveProfile;
    private Button btnCancelEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_teacher_profile);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvProfileInitial = findViewById(R.id.tvProfileInitial);
        tvBack = findViewById(R.id.tvBack);

        etEditName = findViewById(R.id.etEditName);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);

        // Load profile
        loadProfile();

        // Back
        tvBack.setOnClickListener(v -> finish());

        // Edit
        btnEditProfile.setOnClickListener(v -> startEditing());

        // Save
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Cancel
        btnCancelEdit.setOnClickListener(v -> cancelEditing());
    }

    // =========================================================
    // LOAD PROFILE
    // =========================================================

    private void loadProfile() {

        if (mAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "User is not logged in.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        String email =
                mAuth.getCurrentUser().getEmail();

        // Email from Firebase Authentication
        if (email != null && !email.trim().isEmpty()) {

            tvProfileEmail.setText(email);

        } else {

            tvProfileEmail.setText(
                    "Email not available"
            );
        }

        // Role
        tvProfileRole.setText("Teacher");

        // Name from Firestore
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        String name =
                                documentSnapshot.getString("name");

                        if (name != null
                                && !name.trim().isEmpty()) {

                            showName(name.trim());

                        } else {

                            showName("Teacher");
                        }

                    } else {

                        showName("Teacher");
                    }
                })
                .addOnFailureListener(e -> {

                    showName("Teacher");

                    Toast.makeText(
                            this,
                            "Could not load profile.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =========================================================
    // SHOW NAME
    // =========================================================

    private void showName(String name) {

        tvProfileName.setText(name);

        if (name.length() > 0) {

            String firstLetter =
                    name.substring(0, 1).toUpperCase();

            tvProfileInitial.setText(firstLetter);

        } else {

            tvProfileInitial.setText("T");
        }
    }

    // =========================================================
    // START EDITING
    // =========================================================

    private void startEditing() {

        String currentName =
                tvProfileName.getText().toString();

        etEditName.setText(currentName);

        // Hide normal name
        tvProfileName.setVisibility(View.GONE);

        // Show edit box
        etEditName.setVisibility(View.VISIBLE);

        // Buttons
        btnEditProfile.setVisibility(View.GONE);

        btnSaveProfile.setVisibility(View.VISIBLE);

        btnCancelEdit.setVisibility(View.VISIBLE);

        etEditName.requestFocus();
    }

    // =========================================================
    // CANCEL EDITING
    // =========================================================

    private void cancelEditing() {

        etEditName.setText("");

        etEditName.setVisibility(View.GONE);

        tvProfileName.setVisibility(View.VISIBLE);

        btnEditProfile.setVisibility(View.VISIBLE);

        btnSaveProfile.setVisibility(View.GONE);

        btnCancelEdit.setVisibility(View.GONE);
    }

    // =========================================================
    // SAVE PROFILE
    // =========================================================

    private void saveProfile() {

        if (mAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "User is not logged in.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String newName =
                etEditName.getText().toString().trim();

        // Empty name
        if (TextUtils.isEmpty(newName)) {

            etEditName.setError(
                    "Enter your name"
            );

            etEditName.requestFocus();

            return;
        }

        // Minimum name length
        if (newName.length() < 2) {

            etEditName.setError(
                    "Enter a valid name"
            );

            etEditName.requestFocus();

            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        btnSaveProfile.setEnabled(false);

        Map<String, Object> updates =
                new HashMap<>();

        updates.put(
                "name",
                newName
        );

        updates.put(
                "role",
                "Teacher"
        );

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(unused -> {

                    // Update UI
                    showName(newName);

                    // Exit edit mode
                    etEditName.setText("");

                    etEditName.setVisibility(
                            View.GONE
                    );

                    tvProfileName.setVisibility(
                            View.VISIBLE
                    );

                    btnEditProfile.setVisibility(
                            View.VISIBLE
                    );

                    btnSaveProfile.setVisibility(
                            View.GONE
                    );

                    btnCancelEdit.setVisibility(
                            View.GONE
                    );

                    btnSaveProfile.setEnabled(
                            true
                    );

                    Toast.makeText(
                            this,
                            "Profile updated successfully.",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    btnSaveProfile.setEnabled(true);

                    String error =
                            e.getMessage();

                    if (error == null
                            || error.trim().isEmpty()) {

                        error =
                                "Unable to update profile.";
                    }

                    Toast.makeText(
                            this,
                            "Update failed: " + error,
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}