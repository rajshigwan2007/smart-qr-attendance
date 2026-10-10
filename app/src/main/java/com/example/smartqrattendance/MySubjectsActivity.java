package com.example.smartqrattendance;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class MySubjectsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout subjectsContainer;
    private TextView tvEmptySubjects;
    private TextView tvSubjectCount;

    private ListenerRegistration subjectsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_subjects);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        subjectsContainer =
                findViewById(R.id.subjectsContainer);

        tvEmptySubjects =
                findViewById(R.id.tvEmptySubjects);

        tvSubjectCount =
                findViewById(R.id.tvSubjectCount);

        TextView tvBack =
                findViewById(R.id.tvSubjectsBack);

        TextView btnAddSubject =
                findViewById(R.id.btnAddSubject);

        tvBack.setOnClickListener(
                v -> finish()
        );

        btnAddSubject.setOnClickListener(
                v -> showAddSubjectDialog()
        );

        listenForSubjects();
    }

    // =========================================================
    // REAL-TIME LOAD SUBJECTS
    // =========================================================

    private void listenForSubjects() {

        if (mAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "User is not logged in.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        subjectsListener =
                db.collection("users")
                        .document(uid)
                        .collection("subjects")
                        .addSnapshotListener(
                                (querySnapshot, error) -> {

                                    if (error != null) {

                                        Toast.makeText(
                                                this,
                                                "Unable to load subjects: "
                                                        + error.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    if (querySnapshot == null) {
                                        return;
                                    }

                                    subjectsContainer
                                            .removeAllViews();

                                    int count =
                                            querySnapshot.size();

                                    tvSubjectCount.setText(
                                            count + " Subjects"
                                    );

                                    if (count == 0) {

                                        tvEmptySubjects
                                                .setVisibility(
                                                        View.VISIBLE
                                                );

                                        return;
                                    }

                                    tvEmptySubjects
                                            .setVisibility(
                                                    View.GONE
                                            );

                                    for (DocumentSnapshot document :
                                            querySnapshot.getDocuments()) {

                                        String name =
                                                document.getString(
                                                        "name"
                                                );

                                        String code =
                                                document.getString(
                                                        "code"
                                                );

                                        if (name == null ||
                                                name.trim().isEmpty()) {

                                            name =
                                                    "Unnamed Subject";
                                        }

                                        if (code == null) {
                                            code = "";
                                        }

                                        addSubjectCard(
                                                document.getId(),
                                                name,
                                                code
                                        );
                                    }
                                }
                        );
    }

    // =========================================================
    // ADD SUBJECT CARD
    // =========================================================

    private void addSubjectCard(
            String documentId,
            String subjectName,
            String subjectCode
    ) {

        CardView card =
                new CardView(this);

        card.setRadius(20f);
        card.setCardElevation(4f);
        card.setUseCompatPadding(true);

        card.setCardBackgroundColor(
                android.graphics.Color.WHITE
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
                20,
                18,
                18,
                18
        );

        // Subject initial
        TextView icon =
                new TextView(this);

        String firstLetter =
                subjectName.substring(0, 1)
                        .toUpperCase();

        icon.setText(firstLetter);

        icon.setTextColor(
                android.graphics.Color.WHITE
        );

        icon.setTextSize(20);

        icon.setGravity(
                Gravity.CENTER
        );

        icon.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        icon.setBackgroundColor(
                android.graphics.Color.rgb(
                        25,
                        118,
                        210
                )
        );

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        58,
                        58
                );

        row.addView(
                icon,
                iconParams
        );

        // Subject details
        LinearLayout details =
                new LinearLayout(this);

        details.setOrientation(
                LinearLayout.VERTICAL
        );

        LinearLayout.LayoutParams detailsParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        detailsParams.setMargins(
                18,
                0,
                10,
                0
        );

        TextView name =
                new TextView(this);

        name.setText(subjectName);

        name.setTextColor(
                android.graphics.Color.rgb(
                        23,
                        43,
                        77
                )
        );

        name.setTextSize(18);

        name.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        details.addView(name);

        TextView code =
                new TextView(this);

        if (!TextUtils.isEmpty(subjectCode)) {

            code.setText(
                    "Code: " + subjectCode
            );

        } else {

            code.setText("Subject");
        }

        code.setTextColor(
                android.graphics.Color.rgb(
                        102,
                        112,
                        133
                )
        );

        code.setTextSize(13);

        LinearLayout.LayoutParams codeParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        codeParams.setMargins(
                0,
                5,
                0,
                0
        );

        details.addView(
                code,
                codeParams
        );

        row.addView(
                details,
                detailsParams
        );

        // Delete
        TextView delete =
                new TextView(this);

        delete.setText("Delete");

        delete.setTextColor(
                android.graphics.Color.rgb(
                        214,
                        69,
                        69
                )
        );

        delete.setTextSize(12);

        delete.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        delete.setPadding(
                8,
                12,
                8,
                12
        );

        delete.setOnClickListener(
                v -> confirmDelete(
                        documentId,
                        subjectName
                )
        );

        row.addView(delete);

        card.addView(row);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                12
        );

        subjectsContainer.addView(
                card,
                cardParams
        );
    }

    // =========================================================
    // ADD SUBJECT DIALOG
    // =========================================================

    private void showAddSubjectDialog() {

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setPadding(
                35,
                10,
                35,
                0
        );

        EditText etName =
                new EditText(this);

        etName.setHint("Subject name");
        etName.setSingleLine(true);

        container.addView(etName);

        EditText etCode =
                new EditText(this);

        etCode.setHint(
                "Subject code (optional)"
        );

        etCode.setSingleLine(true);

        container.addView(etCode);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Add New Subject")
                        .setView(container)
                        .setNegativeButton(
                                "CANCEL",
                                null
                        )
                        .setPositiveButton(
                                "ADD",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                String name =
                        etName.getText()
                                .toString()
                                .trim();

                String code =
                        etCode.getText()
                                .toString()
                                .trim();

                if (TextUtils.isEmpty(name)) {

                    etName.setError(
                            "Enter subject name"
                    );

                    return;
                }

                addSubjectToFirestore(
                        name,
                        code
                );

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    // =========================================================
    // ADD SUBJECT TO FIRESTORE
    // =========================================================

    private void addSubjectToFirestore(
            String name,
            String code
    ) {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        Map<String, Object> subjectData =
                new HashMap<>();

        subjectData.put(
                "name",
                name
        );

        subjectData.put(
                "code",
                code
        );

        subjectData.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(uid)
                .collection("subjects")
                .add(subjectData)
                .addOnSuccessListener(
                        documentReference -> {

                            Toast.makeText(
                                    this,
                                    "Subject added successfully.",
                                    Toast.LENGTH_SHORT
                            ).show();

                            // NO loadSubjects() HERE.
                            // SnapshotListener automatically updates UI.
                        }
                )
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Failed to add subject: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // DELETE CONFIRMATION
    // =========================================================

    private void confirmDelete(
            String documentId,
            String subjectName
    ) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Subject")
                .setMessage(
                        "Delete \"" +
                                subjectName +
                                "\"?"
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .setPositiveButton(
                        "DELETE",
                        (dialog, which) ->
                                deleteSubject(
                                        documentId
                                )
                )
                .show();
    }

    // =========================================================
    // DELETE SUBJECT
    // =========================================================

    private void deleteSubject(
            String documentId
    ) {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("subjects")
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Subject deleted.",
                            Toast.LENGTH_SHORT
                    ).show();

                    // NO loadSubjects() HERE.
                    // SnapshotListener automatically updates UI.
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Delete failed: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // STOP FIRESTORE LISTENER
    // =========================================================

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (subjectsListener != null) {
            subjectsListener.remove();
            subjectsListener = null;
        }
    }
}
