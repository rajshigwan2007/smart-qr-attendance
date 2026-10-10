package com.example.smartqrattendance;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StudentDashboardActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvWelcome;
    private TextView tvClass;
    private TextView tvAttendanceRate;
    private TextView tvGrade;
    private TextView tvStatus;
    private TextView tvAvatar;

    private LinearLayout todayClassesContainer;

    private CardView cardScan;
    private CardView cardProfile;
    private CardView cardClass;
    private CardView cardVerification;
    private CardView cardSubjectAttendance;

    private TextView cardHistory;

    private String selectedClass = "";

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
        setContentView(R.layout.activity_student_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        tvWelcome = findViewById(R.id.tvStudentWelcome);
        tvClass = findViewById(R.id.tvStudentClass);
        tvAttendanceRate = findViewById(R.id.tvStudentAttendanceRate);
        tvGrade = findViewById(R.id.tvStudentGrade);
        tvStatus = findViewById(R.id.tvStudentStatus);
        tvAvatar = findViewById(R.id.tvStudentAvatar);

        todayClassesContainer =
                findViewById(R.id.todayClassesContainer);

        cardScan =
                findViewById(R.id.cardStudentScan);

        cardHistory =
                findViewById(R.id.cardStudentHistory);

        cardVerification =
                findViewById(R.id.cardStudentVerification);

        cardProfile =
                findViewById(R.id.cardStudentProfile);

        cardClass =
                findViewById(R.id.cardStudentClass);

        cardSubjectAttendance =
                findViewById(R.id.cardStudentSubjectAttendance);

        // Scan
        cardScan.setOnClickListener(v -> openScanner());

        // History
        cardHistory.setOnClickListener(v -> openHistory());

        // Profile
        cardProfile.setOnClickListener(v -> openProfile());

        // Class
        cardClass.setOnClickListener(v -> chooseClass());

        tvClass.setOnClickListener(v -> chooseClass());

        // Verification
        cardVerification.setOnClickListener(v -> showVerification());

        // Subject Attendance
        cardSubjectAttendance.setOnClickListener(
                v -> openSubjectAttendance()
        );

        // Bottom navigation
        findViewById(R.id.navStudentHome)
                .setOnClickListener(v -> scrollTop());

        findViewById(R.id.navStudentHistory)
                .setOnClickListener(v -> openHistory());

        findViewById(R.id.navStudentScan)
                .setOnClickListener(v -> openScanner());

        findViewById(R.id.navStudentProfile)
                .setOnClickListener(v -> openProfile());

        loadProfile();
    }

    private void loadProfile() {

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {

                    String name = safe(doc.getString("name"));

                    if ("-".equals(name)) {
                        name = "Student";
                    }

                    selectedClass =
                            safe(doc.getString("className"));

                    if ("-".equals(selectedClass)) {
                        selectedClass = "";
                    }

                    tvWelcome.setText(
                            "Good morning, " + name
                    );

                    tvClass.setText(
                            selectedClass.isEmpty()
                                    ? "Select class"
                                    : selectedClass
                    );

                    tvAvatar.setText(initials(name));

                    tvStatus.setText(
                            selectedClass.isEmpty()
                                    ? "Select your class to get started"
                                    : "GPS & device verification ready"
                    );

                    loadAttendanceHealth();
                    loadTodayClasses();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not load student profile.",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void loadAttendanceHealth() {

        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid =
                auth.getCurrentUser().getUid();

        db.collectionGroup("attendance")
                .whereEqualTo("studentId", uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    int total = snapshot.size();
                    int present = 0;

                    for (DocumentSnapshot d :
                            snapshot.getDocuments()) {

                        String status =
                                safe(d.getString("status"));

                        if ("Present".equalsIgnoreCase(status)) {
                            present++;
                        }
                    }

                    int rate;

                    if (total == 0) {
                        rate = 0;
                    } else {
                        rate = Math.round(
                                present * 100f / total
                        );
                    }

                    tvAttendanceRate.setText(
                            rate + "%"
                    );

                    if (rate >= 90) {
                        tvGrade.setText("A");
                    } else if (rate >= 80) {
                        tvGrade.setText("B");
                    } else if (rate >= 70) {
                        tvGrade.setText("C");
                    } else if (rate > 0) {
                        tvGrade.setText("D");
                    } else {
                        tvGrade.setText("-");
                    }
                })
                .addOnFailureListener(e -> {

                    tvAttendanceRate.setText("0%");
                    tvGrade.setText("-");
                });
    }

    private void loadTodayClasses() {

        todayClassesContainer.removeAllViews();

        if (selectedClass.isEmpty()) {

            addClassRow(
                    "No class selected",
                    "Choose your class above",
                    "Setup"
            );

            return;
        }

        db.collectionGroup("attendanceSessions")
                .whereEqualTo(
                        "className",
                        selectedClass
                )
                .whereEqualTo(
                        "active",
                        true
                )
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (snapshot.isEmpty()) {

                        addClassRow(
                                "No live class",
                                "No attendance session is active right now",
                                "—"
                        );

                        return;
                    }

                    for (DocumentSnapshot session :
                            snapshot.getDocuments()) {

                        String subject =
                                safe(
                                        session.getString(
                                                "subjectName"
                                        )
                                );

                        Long started =
                                session.getLong("startedAt");

                        String time;

                        if (started == null) {

                            time = "Live now";

                        } else {

                            time =
                                    new SimpleDateFormat(
                                            "hh:mm a",
                                            Locale.getDefault()
                                    ).format(
                                            new Date(started)
                                    );
                        }

                        addClassRow(
                                subject,
                                time + " • Live attendance",
                                "LIVE"
                        );
                    }
                })
                .addOnFailureListener(e ->
                        addClassRow(
                                "Today's classes",
                                "Open Scan QR when your teacher starts attendance",
                                "Ready"
                        )
                );
    }

    private void addClassRow(
            String subject,
            String details,
            String badge
    ) {

        CardView card =
                new CardView(this);

        card.setRadius(dp(18));
        card.setCardElevation(dp(1));
        card.setCardBackgroundColor(
                0xFFFFFFFF
        );

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                Gravity.CENTER_VERTICAL
        );

        row.setPadding(
                dp(16),
                dp(14),
                dp(14),
                dp(14)
        );

        TextView icon =
                new TextView(this);

        icon.setText("▣");
        icon.setTextSize(25);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(0xFF2457D6);
        icon.setBackgroundColor(0xFFEAF0FF);

        row.addView(
                icon,
                new LinearLayout.LayoutParams(
                        dp(54),
                        dp(54)
                )
        );

        LinearLayout text =
                new LinearLayout(this);

        text.setOrientation(
                LinearLayout.VERTICAL
        );

        text.setPadding(
                dp(14),
                0,
                dp(8),
                0
        );

        TextView title =
                new TextView(this);

        title.setText(subject);
        title.setTextSize(16);
        title.setTextColor(0xFF111A2E);
        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        TextView sub =
                new TextView(this);

        sub.setText(details);
        sub.setTextSize(13);
        sub.setTextColor(0xFF667085);
        sub.setPadding(
                0,
                dp(4),
                0,
                0
        );

        text.addView(title);
        text.addView(sub);

        row.addView(
                text,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        TextView status =
                new TextView(this);

        status.setText(badge);
        status.setTextSize(12);
        status.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        status.setTextColor(0xFF16805C);
        status.setGravity(Gravity.CENTER);

        status.setPadding(
                dp(10),
                dp(7),
                dp(10),
                dp(7)
        );

        row.addView(
                status,
                new LinearLayout.LayoutParams(
                        -2,
                        -2
                )
        );

        card.addView(row);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        p.setMargins(
                0,
                0,
                0,
                dp(12)
        );

        todayClassesContainer.addView(
                card,
                p
        );
    }

    private void openScanner() {

        startActivity(
                new Intent(
                        this,
                        StudentQRScannerActivity.class
                )
        );
    }

    private void openHistory() {

        startActivity(
                new Intent(
                        this,
                        StudentAttendanceHistoryActivity.class
                )
        );
    }

    private void openProfile() {

        startActivity(
                new Intent(
                        this,
                        StudentProfileActivity.class
                )
        );
    }

    private void openSubjectAttendance() {

        startActivity(
                new Intent(
                        this,
                        StudentSubjectAttendanceActivity.class
                )
        );
    }

    private void chooseClass() {

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Select your class"
                        )
                        .setSingleChoiceItems(
                                classList,
                                indexOfClass(
                                        selectedClass
                                ),
                                null
                        )
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Save",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                int selected =
                        dialog.getListView()
                                .getCheckedItemPosition();

                if (selected < 0) {
                    return;
                }

                selectedClass =
                        classList[selected];

                Map<String, Object> update =
                        new HashMap<>();

                update.put(
                        "className",
                        selectedClass
                );

                if (auth.getCurrentUser() == null) {
                    return;
                }

                db.collection("users")
                        .document(
                                auth.getCurrentUser().getUid()
                        )
                        .set(
                                update,
                                SetOptions.merge()
                        )
                        .addOnSuccessListener(x -> {

                            tvClass.setText(
                                    selectedClass
                            );

                            tvStatus.setText(
                                    "GPS & device verification ready"
                            );

                            loadAttendanceHealth();
                            loadTodayClasses();

                            Toast.makeText(
                                    this,
                                    "Class saved.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            dialog.dismiss();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(
                                        this,
                                        "Could not save class.",
                                        Toast.LENGTH_LONG
                                ).show()
                        );
            });
        });

        dialog.show();
    }

    private void showVerification() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "Verification Status"
                )
                .setMessage(
                        "QR verification: Ready\n\n" +
                                "GPS verification: Ready\n\n" +
                                "Device verification: Ready"
                )
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }

    private void scrollTop() {

        View scroll =
                findViewById(
                        R.id.studentScroll
                );

        if (scroll instanceof android.widget.ScrollView) {

            ((android.widget.ScrollView) scroll)
                    .smoothScrollTo(0, 0);
        }
    }

    private int indexOfClass(String value) {

        for (int i = 0;
             i < classList.length;
             i++) {

            if (classList[i].equals(value)) {
                return i;
            }
        }

        return 0;
    }

    private String initials(String name) {

        if (name == null ||
                name.trim().isEmpty()) {

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

    private int dp(int value) {

        return Math.round(
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    private String safe(String value) {

        return value == null ||
                value.trim().isEmpty()
                ? "-"
                : value;
    }
}