package com.example.smartqrattendance;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvAvatar;
    private TextView tvName;
    private TextView tvRollNo;
    private TextView tvEmail;
    private TextView tvClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_student_profile
        );

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar =
                findViewById(R.id.profileToolbar);

        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar()
                    .setDisplayHomeAsUpEnabled(true);

            getSupportActionBar()
                    .setTitle("My Profile");
        }

        tvAvatar =
                findViewById(R.id.tvProfileAvatar);

        tvName =
                findViewById(R.id.tvProfileName);

        tvRollNo =
                findViewById(R.id.tvProfileRollNo);

        tvEmail =
                findViewById(R.id.tvProfileEmail);

        tvClass =
                findViewById(R.id.tvProfileClass);

        loadProfile();
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
                            safe(doc.getString("name"));

                    String rollNo =
                            safe(doc.getString("rollNo"));

                    String email =
                            safe(doc.getString("email"));

                    String className =
                            safe(doc.getString("className"));

                    tvName.setText(name);
                    tvRollNo.setText(rollNo);
                    tvEmail.setText(email);
                    tvClass.setText(className);

                    tvAvatar.setText(
                            initials(name)
                    );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not load profile.",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(
                R.menu.student_profile_menu,
                menu
        );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(
            MenuItem item
    ) {

        int id = item.getItemId();

        if (id == android.R.id.home) {

            finish();
            return true;
        }

        if (id == R.id.action_edit_profile) {

            openEditProfile();
            return true;
        }

        if (id == R.id.action_logout) {

            logout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openEditProfile() {

        startActivity(
                new Intent(
                        this,
                        StudentEditProfileActivity.class
                )
        );
    }

    private void logout() {

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage(
                        "Are you sure you want to logout?"
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Logout",
                        (dialog, which) -> {

                            auth.signOut();

                            Intent intent =
                                    new Intent(
                                            this,
                                            MainActivity.class
                                    );

                            intent.setFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                            );

                            startActivity(intent);
                            finish();
                        }
                )
                .show();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (auth != null &&
                auth.getCurrentUser() != null) {

            loadProfile();
        }
    }

    private String initials(String name) {

        if (name == null ||
                name.trim().isEmpty() ||
                name.equals("-")) {

            return "S";
        }

        String[] parts =
                name.trim().split("\\s+");

        if (parts.length == 1) {

            return parts[0]
                    .substring(0, 1)
                    .toUpperCase();
        }

        return (
                parts[0].substring(0, 1) +
                        parts[parts.length - 1]
                                .substring(0, 1)
        ).toUpperCase();
    }

    private String safe(String value) {

        return value == null ||
                value.trim().isEmpty()
                ? "-"
                : value;
    }
}