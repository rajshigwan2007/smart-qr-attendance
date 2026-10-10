package com.example.smartqrattendance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeacherReportsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView tvTitle;
    private TextView tvSessions;
    private TextView tvRecords;
    private TextView tvEmpty;

    private ListView listReports;

    private String selectedClass = "";
    private String selectedSubject = "";

    private final List<ReportItem> reportList = new ArrayList<>();
    private ReportAdapter adapter;

    private int totalSessions = 0;
    private int totalPresentRecords = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_teacher_reports);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        selectedClass =
                getIntent().getStringExtra("selectedClass");

        selectedSubject =
                getIntent().getStringExtra("selectedSubject");

        if (selectedClass == null) selectedClass = "";
        if (selectedSubject == null) selectedSubject = "";

        findViews();

        adapter = new ReportAdapter();
        listReports.setAdapter(adapter);

        loadReports();
    }

    private void findViews() {

        tvTitle = findViewById(R.id.tvReportsTitle);
        tvSessions = findViewById(R.id.tvReportsSessions);
        tvRecords = findViewById(R.id.tvReportsRecords);
        tvEmpty = findViewById(R.id.tvReportsEmpty);
        listReports = findViewById(R.id.listReports);

        tvTitle.setText("Attendance Reports");
    }

    private void loadReports() {

        if (mAuth.getCurrentUser() == null) {
            showEmpty("Teacher is not logged in.");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("attendanceSessions")
                .get()
                .addOnSuccessListener(snapshot -> {

                    reportList.clear();

                    totalSessions = 0;
                    totalPresentRecords = 0;

                    for (DocumentSnapshot session :
                            snapshot.getDocuments()) {

                        String className =
                                session.getString("className");

                        String subjectName =
                                session.getString("subjectName");

                        Long startedAt =
                                session.getLong("startedAt");

                        if (className == null) className = "";
                        if (subjectName == null) subjectName = "";

                        if (!selectedClass.isEmpty()
                                && !selectedClass.equals(className)) {
                            continue;
                        }

                        if (!selectedSubject.isEmpty()
                                && !selectedSubject.equals(subjectName)) {
                            continue;
                        }

                        totalSessions++;

                        String dateText = "Unknown date";

                        if (startedAt != null) {
                            dateText =
                                    new SimpleDateFormat(
                                            "dd MMM yyyy, hh:mm a",
                                            Locale.getDefault()
                                    ).format(
                                            new Date(startedAt)
                                    );
                        }

                        final String finalDate = dateText;
                        final String finalClass = className;
                        final String finalSubject = subjectName;

                        session.getReference()
                                .collection("attendance")
                                .get()
                                .addOnSuccessListener(attendance -> {

                                    int count = attendance.size();

                                    totalPresentRecords += count;

                                    reportList.add(
                                            new ReportItem(
                                                    finalClass,
                                                    finalSubject,
                                                    finalDate,
                                                    count
                                            )
                                    );

                                    updateUI();
                                })
                                .addOnFailureListener(e -> {

                                    reportList.add(
                                            new ReportItem(
                                                    finalClass,
                                                    finalSubject,
                                                    finalDate,
                                                    0
                                            )
                                    );

                                    updateUI();
                                });
                    }

                    updateSummary();

                    if (totalSessions == 0) {
                        showEmpty("No report data found.");
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Could not load reports.",
                            Toast.LENGTH_SHORT
                    ).show();

                    showEmpty("Could not load reports.");
                });
    }

    private void updateUI() {

        updateSummary();

        if (reportList.isEmpty()) {
            return;
        }

        tvEmpty.setVisibility(View.GONE);
        listReports.setVisibility(View.VISIBLE);

        adapter.notifyDataSetChanged();
    }

    private void updateSummary() {

        tvSessions.setText(
                String.valueOf(totalSessions)
        );

        tvRecords.setText(
                String.valueOf(totalPresentRecords)
        );
    }

    private void showEmpty(String message) {

        listReports.setVisibility(View.GONE);

        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);

        updateSummary();
    }

    private class ReportItem {

        String className;
        String subjectName;
        String date;
        int present;

        ReportItem(
                String className,
                String subjectName,
                String date,
                int present) {

            this.className = className;
            this.subjectName = subjectName;
            this.date = date;
            this.present = present;
        }
    }

    private class ReportAdapter extends android.widget.BaseAdapter {

        @Override
        public int getCount() {
            return reportList.size();
        }

        @Override
        public Object getItem(int position) {
            return reportList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(
                int position,
                View convertView,
                android.view.ViewGroup parent) {

            if (convertView == null) {

                convertView = LayoutInflater.from(
                        TeacherReportsActivity.this
                ).inflate(
                        R.layout.item_teacher_report,
                        parent,
                        false
                );
            }

            TextView title =
                    convertView.findViewById(R.id.tvReportTitle);

            TextView date =
                    convertView.findViewById(R.id.tvReportDate);

            TextView present =
                    convertView.findViewById(R.id.tvReportPresent);

            ReportItem item = reportList.get(position);

            title.setText(
                    item.className + "  •  " + item.subjectName
            );

            date.setText(item.date);

            present.setText(
                    "Present: " + item.present
            );

            return convertView;
        }
    }
}