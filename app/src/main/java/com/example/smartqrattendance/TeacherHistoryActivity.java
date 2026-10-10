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

public class TeacherHistoryActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ListView listHistory;
    private TextView tvTitle, tvEmpty, tvHistoryCount;

    private String selectedClass = "";
    private String selectedSubject = "";

    private final List<HistoryItem> historyList = new ArrayList<>();
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_history);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        selectedClass =
                getIntent().getStringExtra("selectedClass");

        selectedSubject =
                getIntent().getStringExtra("selectedSubject");

        if (selectedClass == null) selectedClass = "";
        if (selectedSubject == null) selectedSubject = "";

        findViews();

        adapter = new HistoryAdapter();
        listHistory.setAdapter(adapter);

        loadHistory();
    }

    private void findViews() {

        tvTitle = findViewById(R.id.tvHistoryTitle);
        tvEmpty = findViewById(R.id.tvHistoryEmpty);
        tvHistoryCount = findViewById(R.id.tvHistoryCount);
        listHistory = findViewById(R.id.listHistory);

        tvTitle.setText("Attendance History");
    }

    private void loadHistory() {

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

                    historyList.clear();

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

                        Boolean active =
                                session.getBoolean("active");

                        String status =
                                Boolean.TRUE.equals(active)
                                        ? "Active"
                                        : "Completed";

                        historyList.add(
                                new HistoryItem(
                                        className,
                                        subjectName,
                                        dateText,
                                        status
                                )
                        );
                    }

                    tvHistoryCount.setText(
                            historyList.size() + " Sessions"
                    );

                    if (historyList.isEmpty()) {
                        showEmpty("No attendance history found.");
                        return;
                    }

                    tvEmpty.setVisibility(View.GONE);
                    listHistory.setVisibility(View.VISIBLE);

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Could not load attendance history.",
                            Toast.LENGTH_SHORT
                    ).show();

                    showEmpty("Could not load attendance history.");
                });
    }

    private void showEmpty(String message) {

        listHistory.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
        tvHistoryCount.setText("0 Sessions");
    }

    private class HistoryItem {

        String className;
        String subjectName;
        String date;
        String status;

        HistoryItem(
                String className,
                String subjectName,
                String date,
                String status) {

            this.className = className;
            this.subjectName = subjectName;
            this.date = date;
            this.status = status;
        }
    }

    private class HistoryAdapter extends android.widget.BaseAdapter {

        @Override
        public int getCount() {
            return historyList.size();
        }

        @Override
        public Object getItem(int position) {
            return historyList.get(position);
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
                        TeacherHistoryActivity.this
                ).inflate(
                        R.layout.item_teacher_history,
                        parent,
                        false
                );
            }

            TextView title =
                    convertView.findViewById(R.id.tvHistoryItemTitle);

            TextView date =
                    convertView.findViewById(R.id.tvHistoryItemDate);

            TextView status =
                    convertView.findViewById(R.id.tvHistoryItemStatus);

            HistoryItem item = historyList.get(position);

            title.setText(
                    item.className + "  •  " + item.subjectName
            );

            date.setText(item.date);
            status.setText(item.status);

            return convertView;
        }
    }
}