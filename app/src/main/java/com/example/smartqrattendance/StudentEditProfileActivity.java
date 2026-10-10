package com.example.smartqrattendance;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class StudentEditProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText etName;
    private EditText etRollNo;
    private Spinner spinnerClass;
    private Button btnSave;

    private final String[] classList = {
            "FY BSc CS",
            "SY BSc CS",
            "TY BSc CS",
            "FY BCA",
            "SY BCA",
            "TY BCA",
            "FY BSc IT",
            "SY BSc IT",
            "TY BSc IT"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_student_edit_profile
        );

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etName =
                findViewById(R.id.etEditStudentName);

        etRollNo =
                findViewById(R.id.etEditStudentRollNo);

        spinnerClass =
                findViewById(R.id.spinnerEditStudentClass);

        btnSave =
                findViewById(R.id.btnSaveStudentProfile);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        classList
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerClass.setAdapter(adapter);

        loadProfile();

        btnSave.setOnClickListener(
                v -> saveProfile()
        );
    }

    private void loadProfile() {

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        String uid =
                auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    String name =
                            doc.getString("name");

                    String rollNo =
                            doc.getString("rollNo");

                    String className =
                            doc.getString("className");

                    if (name != null) {
                        etName.setText(name);
                    }

                    if (rollNo != null) {
                        etRollNo.setText(rollNo);
                    }

                    if (className != null) {

                        for (int i = 0;
                             i < classList.length;
                             i++) {

                            if (classList[i]
                                    .equals(className)) {

                                spinnerClass
                                        .setSelection(i);

                                break;
                            }
                        }
                    }
                });
    }

    private void saveProfile() {

        String name =
                etName.getText()
                        .toString()
                        .trim();

        String rollNo =
                etRollNo.getText()
                        .toString()
                        .trim();

        String className =
                spinnerClass
                        .getSelectedItem()
                        .toString();

        if (name.isEmpty()) {

            etName.setError(
                    "Enter your name"
            );

            etName.requestFocus();
            return;
        }

        if (rollNo.isEmpty()) {

            etRollNo.setError(
                    "Enter your roll number"
            );

            etRollNo.requestFocus();
            return;
        }

        if (auth.getCurrentUser() == null) {
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("SAVING...");

        Map<String, Object> update =
                new HashMap<>();

        update.put("name", name);
        update.put("rollNo", rollNo);
        update.put("className", className);

        String uid =
                auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .set(
                        update,
                        SetOptions.merge()
                )
                .addOnSuccessListener(v -> {

                    Toast.makeText(
                            this,
                            "Profile updated successfully.",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    btnSave.setEnabled(true);
                    btnSave.setText("SAVE CHANGES");

                    Toast.makeText(
                            this,
                            "Could not update profile.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}