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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MyClassesActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout classesContainer;
    private TextView tvEmptyClasses;
    private TextView tvClassCount;

    private ListenerRegistration classesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_classes);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        classesContainer = findViewById(R.id.classesContainer);
        tvEmptyClasses = findViewById(R.id.tvEmptyClasses);
        tvClassCount = findViewById(R.id.tvClassCount);

        TextView tvBack = findViewById(R.id.tvClassesBack);
        TextView btnAddClass = findViewById(R.id.btnAddClass);

        tvBack.setOnClickListener(v -> finish());

        btnAddClass.setOnClickListener(v -> showAddClassDialog());

        listenForClasses();
    }

    // =========================================================
    // REAL-TIME LOAD CLASSES
    // =========================================================

    private void listenForClasses() {

        if (mAuth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "User is not logged in.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        classesListener = db.collection("users")
                .document(uid)
                .collection("classes")
                .addSnapshotListener((querySnapshot, error) -> {

                    if (error != null) {

                        Toast.makeText(
                                this,
                                "Unable to load classes: "
                                        + error.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    if (querySnapshot == null) {
                        return;
                    }

                    classesContainer.removeAllViews();

                    int count = querySnapshot.size();

                    tvClassCount.setText(
                            count + " Classes"
                    );

                    if (count == 0) {

                        tvEmptyClasses.setVisibility(
                                View.VISIBLE
                        );

                        return;
                    }

                    tvEmptyClasses.setVisibility(
                            View.GONE
                    );

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        String name =
                                document.getString("name");

                        String section =
                                document.getString("section");

                        if (name == null ||
                                name.trim().isEmpty()) {

                            name = "Unnamed Class";
                        }

                        if (section == null) {
                            section = "";
                        }

                        addClassCard(
                                document.getId(),
                                name,
                                section
                        );
                    }
                });
    }

    // =========================================================
    // ADD CLASS CARD
    // =========================================================

    private void addClassCard(
            String documentId,
            String className,
            String section
    ) {

        CardView card = new CardView(this);

        card.setRadius(20f);
        card.setCardElevation(4f);
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(
                android.graphics.Color.WHITE
        );

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        layout.setGravity(
                Gravity.CENTER_VERTICAL
        );

        layout.setPadding(
                22,
                20,
                18,
                20
        );

        // Class initial
        TextView initial =
                new TextView(this);

        initial.setText(
                className.substring(0, 1).toUpperCase()
        );

        initial.setTextColor(
                android.graphics.Color.WHITE
        );

        initial.setTextSize(20);

        initial.setGravity(
                Gravity.CENTER
        );

        initial.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        initial.setBackgroundColor(
                android.graphics.Color.rgb(
                        25,
                        118,
                        210
                )
        );

        LinearLayout.LayoutParams initialParams =
                new LinearLayout.LayoutParams(
                        58,
                        58
                );

        layout.addView(
                initial,
                initialParams
        );

        // Details
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

        name.setText(className);

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

        TextView sectionText =
                new TextView(this);

        if (!TextUtils.isEmpty(section)) {

            sectionText.setText(
                    "Section: " + section
            );

        } else {

            sectionText.setText("Class");
        }

        sectionText.setTextColor(
                android.graphics.Color.rgb(
                        102,
                        112,
                        133
                )
        );

        sectionText.setTextSize(13);

        LinearLayout.LayoutParams sectionParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        sectionParams.setMargins(
                0,
                5,
                0,
                0
        );

        details.addView(
                sectionText,
                sectionParams
        );

        layout.addView(
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
                        className
                )
        );

        layout.addView(delete);

        card.addView(layout);

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

        classesContainer.addView(
                card,
                cardParams
        );
    }

    // =========================================================
    // ADD CLASS DIALOG
    // =========================================================

    private void showAddClassDialog() {

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

        EditText etClassName =
                new EditText(this);

        etClassName.setHint("Class name");
        etClassName.setSingleLine(true);

        container.addView(etClassName);

        EditText etSection =
                new EditText(this);

        etSection.setHint("Section (optional)");
        etSection.setSingleLine(true);

        container.addView(etSection);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("Add New Class")
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

                String className =
                        etClassName.getText()
                                .toString()
                                .trim();

                String section =
                        etSection.getText()
                                .toString()
                                .trim();

                if (TextUtils.isEmpty(className)) {

                    etClassName.setError(
                            "Enter class name"
                    );

                    return;
                }

                addClassToFirestore(
                        className,
                        section
                );

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    // =========================================================
    // ADD CLASS TO FIRESTORE
    // =========================================================

    private void addClassToFirestore(
            String className,
            String section
    ) {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        java.util.Map<String, Object> classData =
                new java.util.HashMap<>();

        classData.put(
                "name",
                className
        );

        classData.put(
                "section",
                section
        );

        classData.put(
                "createdAt",
                com.google.firebase.firestore.FieldValue
                        .serverTimestamp()
        );

        db.collection("users")
                .document(uid)
                .collection("classes")
                .add(classData)
                .addOnSuccessListener(documentReference -> {

                    Toast.makeText(
                            this,
                            "Class added successfully.",
                            Toast.LENGTH_SHORT
                    ).show();

                    // NO loadClasses() HERE.
                    // SnapshotListener automatically updates UI.
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Failed to add class: "
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
            String className
    ) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Class")
                .setMessage(
                        "Delete \"" +
                                className +
                                "\"?"
                )
                .setNegativeButton(
                        "CANCEL",
                        null
                )
                .setPositiveButton(
                        "DELETE",
                        (dialog, which) ->
                                deleteClass(documentId)
                )
                .show();
    }

    // =========================================================
    // DELETE CLASS
    // =========================================================

    private void deleteClass(
            String documentId
    ) {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("classes")
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Class deleted.",
                            Toast.LENGTH_SHORT
                    ).show();

                    // NO loadClasses() HERE.
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

        if (classesListener != null) {
            classesListener.remove();
            classesListener = null;
        }
    }
}
