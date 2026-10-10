package com.example.smartqrattendance;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TeacherSettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private SwitchCompat switchNotifications;
    private SwitchCompat switchAttendanceReminder;
    private SwitchCompat switchSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        TextView btnBack =
                findViewById(R.id.tvSettingsBack);

        switchNotifications =
                findViewById(R.id.switchNotifications);

        switchAttendanceReminder =
                findViewById(R.id.switchAttendanceReminder);

        switchSound =
                findViewById(R.id.switchSound);

        btnBack.setOnClickListener(v -> finish());

        loadSettings();

        switchNotifications.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (buttonView.isPressed()) {
                        saveSetting(
                                "notificationsEnabled",
                                isChecked
                        );
                    }
                }
        );

        switchAttendanceReminder.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (buttonView.isPressed()) {
                        saveSetting(
                                "attendanceReminder",
                                isChecked
                        );
                    }
                }
        );

        switchSound.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    if (buttonView.isPressed()) {
                        saveSetting(
                                "soundEnabled",
                                isChecked
                        );
                    }
                }
        );
    }

    private void loadSettings() {

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("settings")
                .document("preferences")
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        return;
                    }

                    Boolean notifications =
                            documentSnapshot.getBoolean(
                                    "notificationsEnabled"
                            );

                    Boolean reminder =
                            documentSnapshot.getBoolean(
                                    "attendanceReminder"
                            );

                    Boolean sound =
                            documentSnapshot.getBoolean(
                                    "soundEnabled"
                            );

                    switchNotifications.setChecked(
                            notifications == null
                                    || notifications
                    );

                    switchAttendanceReminder.setChecked(
                            reminder != null
                                    && reminder
                    );

                    switchSound.setChecked(
                            sound == null
                                    || sound
                    );
                });
    }

    private void saveSetting(
            String key,
            boolean value
    ) {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        Map<String, Object> data =
                new HashMap<>();

        data.put(key, value);

        db.collection("users")
                .document(uid)
                .collection("settings")
                .document("preferences")
                .set(
                        data,
                        com.google.firebase.firestore.SetOptions.merge()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not save setting.",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }
}