package com.example.smartqrattendance;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class StudentSubjectAttendanceActivity
        extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_student_subject_attendance
        );

        auth =
                FirebaseAuth.getInstance();

        db =
                FirebaseFirestore.getInstance();

        container =
                findViewById(
                        R.id.subjectAttendanceContainer
                );

        loadAttendance();
    }

    private void loadAttendance() {

        if (auth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "Student is not logged in.",
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }

        String studentId =
                auth.getCurrentUser().getUid();

        db.collection("users")
                .document(studentId)
                .collection("attendanceHistory")
                .get()
                .addOnSuccessListener(snapshot -> {

                    container.removeAllViews();

                    if (snapshot.isEmpty()) {

                        addEmptyMessage();

                        return;
                    }

                    Map<String, int[]> subjectData =
                            new HashMap<>();

                    for (
                            DocumentSnapshot doc :
                            snapshot.getDocuments()
                    ) {

                        String subject =
                                safe(
                                        doc.getString(
                                                "subjectName"
                                        )
                                );

                        if (subject.equals("-")) {
                            subject = "Other";
                        }

                        int[] data =
                                subjectData.get(subject);

                        if (data == null) {

                            data =
                                    new int[]{0, 0};

                            subjectData.put(
                                    subject,
                                    data
                            );
                        }

                        /*
                         * Each attendanceHistory document
                         * represents one class/session.
                         */
                        data[0]++;

                        String status =
                                safe(
                                        doc.getString(
                                                "status"
                                        )
                                );

                        if ("Present".equalsIgnoreCase(
                                status
                        )) {

                            data[1]++;
                        }
                    }

                    for (
                            Map.Entry<String, int[]> entry :
                            subjectData.entrySet()
                    ) {

                        String subject =
                                entry.getKey();

                        int total =
                                entry.getValue()[0];

                        int present =
                                entry.getValue()[1];

                        int percentage =
                                total == 0
                                        ? 0
                                        : Math.round(
                                        present * 100f / total
                                );

                        addSubjectCard(
                                subject,
                                present,
                                total,
                                percentage
                        );
                    }

                })
                .addOnFailureListener(e -> {

                    container.removeAllViews();

                    addEmptyMessage();

                    Toast.makeText(
                            this,
                            "Could not load subject attendance. Check Firebase Rules.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void addSubjectCard(
            String subject,
            int present,
            int total,
            int percentage
    ) {

        CardView card =
                new CardView(this);

        card.setRadius(dp(18));

        card.setCardElevation(dp(3));

        card.setCardBackgroundColor(
                0xFFFFFFFF
        );

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(20),
                dp(18),
                dp(20),
                dp(18)
        );

        TextView title =
                new TextView(this);

        title.setText(subject);

        title.setTextColor(
                0xFF111A2E
        );

        title.setTextSize(18);

        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        TextView detail =
                new TextView(this);

        detail.setText(
                present
                        + " Present / "
                        + total
                        + " Classes"
        );

        detail.setTextColor(
                0xFF667085
        );

        detail.setTextSize(14);

        detail.setPadding(
                0,
                dp(6),
                0,
                0
        );

        TextView percentageText =
                new TextView(this);

        percentageText.setText(
                percentage + "%"
        );

        percentageText.setTextColor(
                percentage >= 75
                        ? 0xFF16805C
                        : 0xFFD92D20
        );

        percentageText.setTextSize(25);

        percentageText.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        percentageText.setGravity(
                Gravity.END
        );

        layout.addView(title);
        layout.addView(detail);
        layout.addView(percentageText);

        card.addView(layout);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.setMargins(
                0,
                0,
                0,
                dp(12)
        );

        container.addView(
                card,
                params
        );
    }

    private void addEmptyMessage() {

        TextView text =
                new TextView(this);

        text.setText(
                "No subject attendance records found yet."
        );

        text.setTextColor(
                0xFF667085
        );

        text.setTextSize(15);

        text.setGravity(
                Gravity.CENTER
        );

        text.setPadding(
                dp(20),
                dp(60),
                dp(20),
                dp(60)
        );

        container.addView(text);
    }

    private String safe(String value) {

        return value == null
                || value.trim().isEmpty()
                ? "-"
                : value.trim();
    }

    private int dp(int value) {

        return Math.round(
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}