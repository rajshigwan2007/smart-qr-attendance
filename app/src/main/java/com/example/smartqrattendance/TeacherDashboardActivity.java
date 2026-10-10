package com.example.smartqrattendance;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class TeacherDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvWelcome;
    private TextView tvTeacherAvatar;
    private TextView tvSelectedClass;
    private TextView tvSelectedSubject;
    private TextView tvLiveSessions;
    private TextView tvPresentToday;
    private TextView tvAvgRate;

    private CardView cardClassSubject;
    private CardView cardGenerateQR;
    private CardView cardAttendance;
    private CardView cardStudents;
    private CardView cardHistory;
    private CardView cardReports;

    private String selectedClass = "";
    private String selectedSubject = "";

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

    private final String[] subjectList = {
            "Java",
            "Python",
            "Android Development",
            "Database Management",
            "Web Development",
            "Computer Networks",
            "Operating Systems",
            "Software Engineering"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_teacher_dashboard
        );

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        findViews();
        setupClicks();
        loadTeacherProfile();
        loadSavedSelection();
    }

    private void findViews() {

        tvWelcome =
                findViewById(R.id.tvWelcome);

        tvTeacherAvatar =
                findViewById(R.id.tvTeacherAvatar);

        tvSelectedClass =
                findViewById(R.id.tvSelectedClass);

        tvSelectedSubject =
                findViewById(R.id.tvSelectedSubject);

        tvLiveSessions =
                findViewById(R.id.tvLiveSessions);

        tvPresentToday =
                findViewById(R.id.tvPresentToday);

        tvAvgRate =
                findViewById(R.id.tvAvgRate);

        cardClassSubject =
                findViewById(R.id.cardClassSubject);

        cardGenerateQR =
                findViewById(R.id.cardGenerateQR);

        cardAttendance =
                findViewById(R.id.cardAttendance);

        cardStudents =
                findViewById(R.id.cardStudents);

        cardHistory =
                findViewById(R.id.cardHistory);

        cardReports =
                findViewById(R.id.cardReports);

        updateSelectionUI();
    }

    private void setupClicks() {

        tvTeacherAvatar.setOnClickListener(
                v -> showTeacherMenu(v)
        );

        cardClassSubject.setOnClickListener(
                v -> showClassSubjectDialog()
        );

        cardGenerateQR.setOnClickListener(v -> {

            if (selectedClass.isEmpty()
                    || selectedSubject.isEmpty()) {

                Toast.makeText(
                        this,
                        "Select class and subject first.",
                        Toast.LENGTH_SHORT
                ).show();

                showClassSubjectDialog();
                return;
            }

            showSessionDurationDialog();
        });

        cardAttendance.setOnClickListener(
                v -> openAttendance()
        );

        cardStudents.setOnClickListener(
                v -> openStudents()
        );

        cardHistory.setOnClickListener(
                v -> openHistory()
        );

        cardReports.setOnClickListener(
                v -> openReports()
        );

        findViewById(R.id.navHome)
                .setOnClickListener(
                        v -> scrollToTop()
                );

        findViewById(R.id.navReports)
                .setOnClickListener(
                        v -> openReports()
                );

        findViewById(R.id.navStudents)
                .setOnClickListener(
                        v -> openStudents()
                );

        findViewById(R.id.navProfile)
                .setOnClickListener(
                        v -> openProfile()
                );
    }

    private void scrollToTop() {

        View scroll =
                findViewById(R.id.teacherScroll);

        if (scroll instanceof android.widget.ScrollView) {

            ((android.widget.ScrollView) scroll)
                    .smoothScrollTo(0, 0);
        }
    }

    private void showSessionDurationDialog() {

        String[] durations = {
                "10 Minutes",
                "15 Minutes",
                "20 Minutes"
        };

        new AlertDialog.Builder(this)
                .setTitle("Attendance session duration")
                .setItems(
                        durations,
                        (dialog, which) -> {

                            long minutes;

                            if (which == 0) {

                                minutes = 10;

                            } else if (which == 1) {

                                minutes = 15;

                            } else {

                                minutes = 20;
                            }

                            openDynamicQR(minutes);
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .show();
    }

    private void openDynamicQR(
            long durationMinutes
    ) {

        Intent intent =
                new Intent(
                        this,
                        DynamicQRActivity.class
                );

        intent.putExtra(
                "selectedClass",
                selectedClass
        );

        intent.putExtra(
                "selectedSubject",
                selectedSubject
        );

        intent.putExtra(
                "durationMinutes",
                durationMinutes
        );

        startActivity(intent);
    }

    private void showClassSubjectDialog() {

        View dialogView =
                getLayoutInflater().inflate(
                        R.layout.dialog_class_subject,
                        null
                );

        Spinner spinnerClass =
                dialogView.findViewById(
                        R.id.spinnerClass
                );

        Spinner spinnerSubject =
                dialogView.findViewById(
                        R.id.spinnerSubject
                );

        ArrayAdapter<String> classAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        classList
                );

        classAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerClass.setAdapter(
                classAdapter
        );

        ArrayAdapter<String> subjectAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        subjectList
                );

        subjectAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerSubject.setAdapter(
                subjectAdapter
        );

        selectSpinner(
                spinnerClass,
                classList,
                selectedClass
        );

        selectSpinner(
                spinnerSubject,
                subjectList,
                selectedSubject
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "Select class & subject"
                        )
                        .setView(dialogView)
                        .setNegativeButton(
                                "Cancel",
                                null
                        )
                        .setPositiveButton(
                                "Save",
                                null
                        )
                        .create();

        dialog.setOnShowListener(
                d -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(v -> {

                        String newClass =
                                String.valueOf(
                                        spinnerClass
                                                .getSelectedItem()
                                ).trim();

                        String newSubject =
                                String.valueOf(
                                        spinnerSubject
                                                .getSelectedItem()
                                ).trim();

                        if (newClass.isEmpty()
                                || newSubject.isEmpty()) {

                            return;
                        }

                        selectedClass =
                                newClass;

                        selectedSubject =
                                newSubject;

                        updateSelectionUI();

                        saveSelectionToFirestore();

                        loadDashboardStats();

                        dialog.dismiss();
                    });
                }
        );

        dialog.show();
    }

    private void selectSpinner(
            Spinner spinner,
            String[] values,
            String value
    ) {

        for (int i = 0; i < values.length; i++) {

            if (values[i].equals(value)) {

                spinner.setSelection(i);
                return;
            }
        }
    }

    private void updateSelectionUI() {

        tvSelectedClass.setText(
                selectedClass.isEmpty()
                        ? "Select class"
                        : selectedClass
        );

        tvSelectedSubject.setText(
                selectedSubject.isEmpty()
                        ? "Select subject"
                        : selectedSubject
        );
    }

    private void saveSelectionToFirestore() {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "selectedClass",
                selectedClass
        );

        data.put(
                "selectedSubject",
                selectedSubject
        );

        db.collection("users")
                .document(
                        mAuth.getCurrentUser().getUid()
                )
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    private void loadSavedSelection() {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        db.collection("users")
                .document(
                        mAuth.getCurrentUser().getUid()
                )
                .get()
                .addOnSuccessListener(doc -> {

                    String c =
                            doc.getString(
                                    "selectedClass"
                            );

                    String s =
                            doc.getString(
                                    "selectedSubject"
                            );

                    selectedClass =
                            c == null ? "" : c;

                    selectedSubject =
                            s == null ? "" : s;

                    updateSelectionUI();

                    loadDashboardStats();
                })
                .addOnFailureListener(e -> {

                    loadDashboardStats();
                });
    }

    private void loadTeacherProfile() {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        db.collection("users")
                .document(
                        mAuth.getCurrentUser().getUid()
                )
                .get()
                .addOnSuccessListener(doc -> {

                    String name =
                            doc.getString("name");

                    if (name == null
                            || name.trim().isEmpty()) {

                        name = "Teacher";

                    } else {

                        name = name.trim();
                    }

                    tvWelcome.setText(
                            "Hello, " + name
                    );

                    tvTeacherAvatar.setText(
                            initials(name)
                    );
                })
                .addOnFailureListener(e -> {

                    tvWelcome.setText(
                            "Hello, Teacher"
                    );

                    tvTeacherAvatar.setText(
                            "UA"
                    );
                });
    }

    private String initials(
            String name
    ) {

        String[] parts =
                name.trim().split("\\s+");

        if (parts.length == 1) {

            return parts[0]
                    .substring(0, 1)
                    .toUpperCase();
        }

        return (
                parts[0].substring(0, 1)
                        + parts[parts.length - 1]
                        .substring(0, 1)
        ).toUpperCase();
    }

    /*
     * ============================================================
     * DASHBOARD STATS
     * ============================================================
     *
     * tvLiveSessions now represents TOTAL sessions created
     * for the selected class + subject.
     *
     * Example:
     *
     * 6 sessions created
     * 0 active right now
     *
     * Counter = 6
     *
     * QR functionality is NOT changed.
     */
    private void loadDashboardStats() {

        if (mAuth.getCurrentUser() == null
                || selectedClass.isEmpty()
                || selectedSubject.isEmpty()) {

            tvLiveSessions.setText("0");
            tvPresentToday.setText("0");
            tvAvgRate.setText("0%");
            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("attendanceSessions")
                .whereEqualTo(
                        "className",
                        selectedClass
                )
                .whereEqualTo(
                        "subjectName",
                        selectedSubject
                )
                .get()
                .addOnSuccessListener(snapshot -> {

                    /*
                     * Count ALL sessions.
                     *
                     * We intentionally do NOT check:
                     *
                     * active == true
                     *
                     * because this counter is the
                     * overall session count.
                     */
                    int totalSessions =
                            snapshot.size();

                    tvLiveSessions.setText(
                            String.valueOf(
                                    totalSessions
                            )
                    );

                    loadPresentAndRate(
                            snapshot.getDocuments()
                    );
                })
                .addOnFailureListener(e -> {

                    tvLiveSessions.setText("0");
                    tvPresentToday.setText("0");
                    tvAvgRate.setText("0%");
                });
    }

    /*
     * ============================================================
     * PRESENT TODAY + OVERALL RATE
     * ============================================================
     */
    private void loadPresentAndRate(
            java.util.List<DocumentSnapshot> sessions
    ) {

        if (sessions == null
                || sessions.isEmpty()) {

            tvPresentToday.setText("0");
            tvAvgRate.setText("0%");
            return;
        }

        /*
         * Calculate today's starting timestamp.
         */
        java.util.Calendar calendar =
                java.util.Calendar.getInstance();

        calendar.set(
                java.util.Calendar.HOUR_OF_DAY,
                0
        );

        calendar.set(
                java.util.Calendar.MINUTE,
                0
        );

        calendar.set(
                java.util.Calendar.SECOND,
                0
        );

        calendar.set(
                java.util.Calendar.MILLISECOND,
                0
        );

        long startOfDay =
                calendar.getTimeInMillis();

        final int[] completed =
                {0};

        final int[] presentToday =
                {0};

        final int[] attendanceRecords =
                {0};

        for (DocumentSnapshot session :
                sessions) {

            session.getReference()
                    .collection("attendance")
                    .get()
                    .addOnSuccessListener(
                            attendance -> {

                                for (
                                        DocumentSnapshot a :
                                        attendance.getDocuments()
                                ) {

                                    /*
                                     * Every attendance record
                                     * contributes to overall rate.
                                     */
                                    attendanceRecords[0]++;

                                    /*
                                     * Only today's records
                                     * contribute to Present Today.
                                     */
                                    Long marked =
                                            a.getLong(
                                                    "markedAt"
                                            );

                                    if (marked != null
                                            && marked >= startOfDay) {

                                        presentToday[0]++;
                                    }
                                }

                                completed[0]++;

                                if (completed[0]
                                        == sessions.size()) {

                                    loadStudentCountAndRate(
                                            sessions.size(),
                                            attendanceRecords[0],
                                            presentToday[0]
                                    );
                                }
                            }
                    )
                    .addOnFailureListener(
                            e -> {

                                /*
                                 * Continue even if one
                                 * session cannot be read.
                                 */
                                completed[0]++;

                                if (completed[0]
                                        == sessions.size()) {

                                    loadStudentCountAndRate(
                                            sessions.size(),
                                            attendanceRecords[0],
                                            presentToday[0]
                                    );
                                }
                            }
                    );
        }
    }

    /*
     * ============================================================
     * OVERALL ATTENDANCE RATE
     * ============================================================
     *
     * Example:
     *
     * Students = 30
     * Sessions = 6
     *
     * Possible attendance =
     *
     * 30 x 6 = 180
     *
     * If attendance records = 150:
     *
     * 150 / 180 x 100
     *
     * = 83%
     */
    private void loadStudentCountAndRate(
            int sessionCount,
            int attendanceRecords,
            int presentToday
    ) {

        db.collection("users")
                .whereEqualTo(
                        "role",
                        "Student"
                )
                .whereEqualTo(
                        "className",
                        selectedClass
                )
                .get()
                .addOnSuccessListener(
                        students -> {

                            int studentCount =
                                    students.size();

                            /*
                             * Present Today remains
                             * today's attendance count.
                             */
                            tvPresentToday.setText(
                                    String.valueOf(
                                            presentToday
                                    )
                            );

                            /*
                             * Overall attendance percentage.
                             */
                            if (studentCount == 0
                                    || sessionCount == 0) {

                                tvAvgRate.setText(
                                        "0%"
                                );

                                return;
                            }

                            double possible =
                                    (double)
                                            studentCount
                                            * sessionCount;

                            int rate =
                                    (int)
                                            Math.round(
                                                    (
                                                            attendanceRecords
                                                                    * 100.0
                                                    )
                                                            / possible
                                            );

                            /*
                             * Keep percentage between
                             * 0 and 100.
                             */
                            rate =
                                    Math.min(
                                            100,
                                            Math.max(
                                                    0,
                                                    rate
                                            )
                                    );

                            tvAvgRate.setText(
                                    rate + "%"
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            /*
                             * Even if student count
                             * fails, show today's count.
                             */
                            tvPresentToday.setText(
                                    String.valueOf(
                                            presentToday
                                    )
                            );

                            tvAvgRate.setText(
                                    "0%"
                            );
                        }
                );
    }

    private void openAttendance() {

        Intent i =
                new Intent(
                        this,
                        TeacherAttendanceActivity.class
                );

        i.putExtra(
                "selectedClass",
                selectedClass
        );

        i.putExtra(
                "selectedSubject",
                selectedSubject
        );

        startActivity(i);
    }

    private void openStudents() {

        if (selectedClass.isEmpty()) {

            showClassSubjectDialog();
            return;
        }

        Intent i =
                new Intent(
                        this,
                        TeacherStudentsActivity.class
                );

        i.putExtra(
                "selectedClass",
                selectedClass
        );

        startActivity(i);
    }

    private void openHistory() {

        Intent i =
                new Intent(
                        this,
                        TeacherHistoryActivity.class
                );

        i.putExtra(
                "selectedClass",
                selectedClass
        );

        i.putExtra(
                "selectedSubject",
                selectedSubject
        );

        startActivity(i);
    }

    private void openReports() {

        Intent i =
                new Intent(
                        this,
                        TeacherReportsActivity.class
                );

        i.putExtra(
                "selectedClass",
                selectedClass
        );

        i.putExtra(
                "selectedSubject",
                selectedSubject
        );

        startActivity(i);
    }

    private void openProfile() {

        startActivity(
                new Intent(
                        this,
                        TeacherProfileActivity.class
                )
        );
    }

    private void showTeacherMenu(
            View anchor
    ) {

        PopupMenu menu =
                new PopupMenu(
                        this,
                        anchor
                );

        menu.getMenu().add(
                "My Profile"
        );

        menu.getMenu().add(
                "My Classes"
        );

        menu.getMenu().add(
                "My Subjects"
        );

        menu.getMenu().add(
                "Notifications"
        );

        menu.getMenu().add(
                "Settings"
        );

        menu.getMenu().add(
                "Logout"
        );

        menu.setOnMenuItemClickListener(
                this::handleMenuItem
        );

        menu.show();
    }

    private boolean handleMenuItem(
            MenuItem item
    ) {

        switch (
                item.getTitle().toString()
        ) {

            case "My Profile":

                openProfile();
                return true;

            case "My Classes":

                startActivity(
                        new Intent(
                                this,
                                MyClassesActivity.class
                        )
                );

                return true;

            case "My Subjects":

                startActivity(
                        new Intent(
                                this,
                                MySubjectsActivity.class
                        )
                );

                return true;

            case "Notifications":

                startActivity(
                        new Intent(
                                this,
                                NotificationsActivity.class
                        )
                );

                return true;

            case "Settings":

                startActivity(
                        new Intent(
                                this,
                                TeacherSettingsActivity.class
                        )
                );

                return true;

            case "Logout":

                confirmLogout();
                return true;

            default:

                return false;
        }
    }

    private void confirmLogout() {

        new AlertDialog.Builder(this)
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
                        (d, w) -> {

                            mAuth.signOut();

                            Intent intent =
                                    new Intent(
                                            this,
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
                .show();
    }
}