package com.example.smartqrattendance;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class TeacherAttendanceActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout attendanceContainer;

    private String selectedClass = "";
    private String selectedSubject = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_attendance);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        attendanceContainer =
                findViewById(R.id.attendanceContainer);

        selectedClass =
                getIntent().getStringExtra("selectedClass");

        selectedSubject =
                getIntent().getStringExtra("selectedSubject");

        if (selectedClass == null) selectedClass = "";
        if (selectedSubject == null) selectedSubject = "";

        loadAttendance();
    }

    private void loadAttendance() {

        if (mAuth.getCurrentUser() == null) {
            addMessage("Teacher is not logged in.");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("attendanceSessions")
                .whereEqualTo("className", selectedClass)
                .whereEqualTo("subjectName", selectedSubject)
                .get()
                .addOnSuccessListener(sessions -> {

                    attendanceContainer.removeAllViews();

                    if (sessions.isEmpty()) {
                        addMessage("No attendance sessions found.");
                        return;
                    }

                    for (DocumentSnapshot session :
                            sessions.getDocuments()) {

                        String sessionId = session.getId();

                        addSessionHeader(
                                selectedClass,
                                selectedSubject,
                                sessionId
                        );

                        session.getReference()
                                .collection("attendance")
                                .get()
                                .addOnSuccessListener(attendance -> {

                                    if (attendance.isEmpty()) {
                                        addSessionEmpty();
                                        return;
                                    }

                                    for (DocumentSnapshot student :
                                            attendance.getDocuments()) {

                                        String name =
                                                student.getString(
                                                        "studentName"
                                                );

                                        if (name == null ||
                                                name.trim().isEmpty()) {
                                            name = "Unknown Student";
                                        }

                                        addStudent(name);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Could not load attendance.",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void addSessionHeader(
            String className,
            String subjectName,
            String sessionId) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 18, 20, 18);
        card.setBackgroundResource(R.drawable.bg_info_card);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText(
                className + "  •  " + subjectName
        );
        title.setTextSize(17);
        title.setTextColor(Color.rgb(23, 43, 77));
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView session = new TextView(this);
        session.setText(
                "Session ID: " + sessionId
        );
        session.setTextSize(12);
        session.setTextColor(Color.rgb(102, 112, 133));
        session.setPadding(0, 6, 0, 0);

        card.addView(title);
        card.addView(session);

        attendanceContainer.addView(card);
    }

    private void addStudent(String studentName) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(18, 15, 18, 15);
        row.setBackgroundResource(R.drawable.bg_info_card);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        params.setMargins(0, 0, 0, 8);
        row.setLayoutParams(params);

        TextView check = new TextView(this);
        check.setText("✓");
        check.setTextSize(20);
        check.setTextColor(Color.rgb(22, 128, 92));

        TextView name = new TextView(this);
        name.setText(studentName);
        name.setTextSize(15);
        name.setTextColor(Color.rgb(23, 43, 77));
        name.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                );

        nameParams.setMargins(14, 0, 0, 0);

        row.addView(check);
        row.addView(name, nameParams);

        TextView present = new TextView(this);
        present.setText("PRESENT");
        present.setTextSize(11);
        present.setTypeface(null, android.graphics.Typeface.BOLD);
        present.setTextColor(Color.rgb(22, 128, 92));

        row.addView(present);

        attendanceContainer.addView(row);
    }

    private void addSessionEmpty() {

        TextView text = new TextView(this);

        text.setText("No students marked attendance.");
        text.setTextSize(13);
        text.setTextColor(Color.rgb(102, 112, 133));
        text.setPadding(10, 5, 10, 18);

        attendanceContainer.addView(text);
    }

    private void addMessage(String message) {

        TextView text = new TextView(this);

        text.setText(message);
        text.setTextSize(15);
        text.setTextColor(Color.rgb(102, 112, 133));
        text.setGravity(Gravity.CENTER);
        text.setPadding(20, 50, 20, 50);

        attendanceContainer.addView(text);
    }
}