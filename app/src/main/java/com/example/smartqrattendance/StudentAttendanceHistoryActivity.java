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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudentAttendanceHistoryActivity
        extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvTitle;
    private TextView tvMessage;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_student_attendance_history
        );

        auth =
                FirebaseAuth.getInstance();

        db =
                FirebaseFirestore.getInstance();

        tvTitle =
                findViewById(R.id.tvTitle);

        tvMessage =
                findViewById(R.id.tvMessage);

        container =
                findViewById(
                        R.id.attendanceHistoryContainer
                );

        tvTitle.setText(
                "Attendance History"
        );

        tvMessage.setText(
                "Loading attendance..."
        );

        loadAttendanceHistory();
    }

    private void loadAttendanceHistory() {

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

                        tvMessage.setText(
                                "No attendance records yet."
                        );

                        addEmptyMessage();

                        return;
                    }

                    tvMessage.setText(
                            snapshot.size()
                                    + " attendance record"
                                    + (
                                    snapshot.size() == 1
                                            ? ""
                                            : "s"
                            )
                    );

                    for (
                            DocumentSnapshot doc :
                            snapshot.getDocuments()
                    ) {

                        addAttendanceCard(doc);
                    }

                })
                .addOnFailureListener(e -> {

                    container.removeAllViews();

                    tvMessage.setText(
                            "Unable to load attendance."
                    );

                    addEmptyMessage();

                    Toast.makeText(
                            this,
                            "Could not load attendance history. Check Firebase Rules.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void addAttendanceCard(
            DocumentSnapshot doc
    ) {

        String subject =
                safe(
                        doc.getString(
                                "subjectName"
                        )
                );

        String className =
                safe(
                        doc.getString(
                                "className"
                        )
                );

        String status =
                safe(
                        doc.getString(
                                "status"
                        )
                );

        String rollNo =
                safe(
                        doc.getString(
                                "rollNo"
                        )
                );

        String teacherId =
                safe(
                        doc.getString(
                                "teacherId"
                        )
                );

        Long markedAt =
                doc.getLong("markedAt");

        String time = "-";

        if (markedAt != null
                && markedAt > 0) {

            time =
                    new SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            Locale.getDefault()
                    ).format(
                            new Date(markedAt)
                    );
        }

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
                dp(18),
                dp(18),
                dp(18),
                dp(18)
        );

        TextView title =
                new TextView(this);

        title.setText(
                subject.equals("-")
                        ? "Attendance"
                        : subject
        );

        title.setTextColor(
                0xFF111827
        );

        title.setTextSize(18);

        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        TextView details =
                new TextView(this);

        details.setText(
                "Class: "
                        + className
                        + "\nRoll No: "
                        + rollNo
                        + "\nDate & Time: "
                        + time
        );

        details.setTextColor(
                0xFF667085
        );

        details.setTextSize(14);

        details.setPadding(
                0,
                dp(8),
                0,
                dp(8)
        );

        TextView statusText =
                new TextView(this);

        statusText.setText(
                "● " + status
        );

        statusText.setTextSize(14);

        statusText.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        statusText.setTextColor(
                "Present".equalsIgnoreCase(status)
                        ? 0xFF16805C
                        : 0xFFD92D20
        );

        layout.addView(title);
        layout.addView(details);
        layout.addView(statusText);

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
                "No attendance records found yet."
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