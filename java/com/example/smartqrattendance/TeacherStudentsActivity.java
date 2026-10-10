package com.example.smartqrattendance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TeacherStudentsActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private ListView listStudents;
    private TextView tvTitle, tvEmpty, tvStudentCount;

    private String selectedClass = "";

    private final List<StudentItem> studentList = new ArrayList<>();
    private StudentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_students);

        db = FirebaseFirestore.getInstance();

        selectedClass = getIntent().getStringExtra("selectedClass");

        if (selectedClass == null) {
            selectedClass = "";
        }

        findViews();

        adapter = new StudentAdapter();
        listStudents.setAdapter(adapter);

        loadStudents();
    }

    private void findViews() {

        tvTitle = findViewById(R.id.tvStudentsTitle);
        tvEmpty = findViewById(R.id.tvStudentsEmpty);
        tvStudentCount = findViewById(R.id.tvStudentCount);
        listStudents = findViewById(R.id.listStudents);

        if (selectedClass.isEmpty()) {
            tvTitle.setText("Manage Roster");
        } else {
            tvTitle.setText("Manage Roster");
        }
    }

    private void loadStudents() {

        if (selectedClass.isEmpty()) {
            showEmpty("Please select a class first.");
            return;
        }

        db.collection("users")
                .whereEqualTo("role", "student")
                .whereEqualTo("className", selectedClass)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    studentList.clear();

                    for (QueryDocumentSnapshot document : querySnapshot) {

                        String name = document.getString("name");
                        String email = document.getString("email");
                        String rollNo = document.getString("rollNo");

                        if (name == null || name.trim().isEmpty()) {
                            name = "Unknown Student";
                        }

                        if (email == null) {
                            email = "";
                        }

                        if (rollNo == null || rollNo.trim().isEmpty()) {
                            rollNo = "-";
                        }

                        studentList.add(
                                new StudentItem(
                                        name,
                                        email,
                                        rollNo
                                )
                        );
                    }

                    tvStudentCount.setText(
                            studentList.size() + " Students"
                    );

                    if (studentList.isEmpty()) {
                        showEmpty(
                                "No students found for " + selectedClass
                        );
                        return;
                    }

                    tvEmpty.setVisibility(View.GONE);
                    listStudents.setVisibility(View.VISIBLE);

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Could not load students.",
                            Toast.LENGTH_SHORT
                    ).show();

                    showEmpty("Could not load students.");
                });
    }

    private void showEmpty(String message) {

        listStudents.setVisibility(View.GONE);

        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);

        tvStudentCount.setText("0 Students");
    }

    private class StudentItem {
        String name;
        String email;
        String rollNo;

        StudentItem(String name, String email, String rollNo) {
            this.name = name;
            this.email = email;
            this.rollNo = rollNo;
        }
    }

    private class StudentAdapter extends android.widget.BaseAdapter {

        @Override
        public int getCount() {
            return studentList.size();
        }

        @Override
        public Object getItem(int position) {
            return studentList.get(position);
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
                        TeacherStudentsActivity.this
                ).inflate(
                        R.layout.item_teacher_student,
                        parent,
                        false
                );
            }

            TextView tvName =
                    convertView.findViewById(R.id.tvStudentName);

            TextView tvRoll =
                    convertView.findViewById(R.id.tvStudentRoll);

            TextView tvEmail =
                    convertView.findViewById(R.id.tvStudentEmail);

            StudentItem student = studentList.get(position);

            tvName.setText(student.name);
            tvRoll.setText("Roll No: " + student.rollNo);

            if (student.email.isEmpty()) {
                tvEmail.setText("Email not available");
            } else {
                tvEmail.setText(student.email);
            }

            return convertView;
        }
    }
}