package com.example.smartqrattendance;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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

public class NotificationsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout notificationsContainer;
    private TextView tvEmptyNotifications;
    private TextView tvNotificationCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        notificationsContainer =
                findViewById(R.id.notificationsContainer);

        tvEmptyNotifications =
                findViewById(R.id.tvEmptyNotifications);

        tvNotificationCount =
                findViewById(R.id.tvNotificationCount);

        TextView btnBack =
                findViewById(R.id.tvNotificationsBack);

        TextView btnMarkAll =
                findViewById(R.id.btnMarkAllRead);

        btnBack.setOnClickListener(v -> finish());

        btnMarkAll.setOnClickListener(
                v -> markAllAsRead()
        );

        loadNotifications();
    }

    private void loadNotifications() {

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .orderBy("createdAt",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    notificationsContainer.removeAllViews();

                    int count = querySnapshot.size();

                    tvNotificationCount.setText(
                            count + " Notifications"
                    );

                    if (count == 0) {

                        tvEmptyNotifications.setVisibility(
                                View.VISIBLE
                        );

                        return;
                    }

                    tvEmptyNotifications.setVisibility(
                            View.GONE
                    );

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        String title =
                                document.getString("title");

                        String message =
                                document.getString("message");

                        Boolean read =
                                document.getBoolean("read");

                        if (title == null) {
                            title = "Notification";
                        }

                        if (message == null) {
                            message = "";
                        }

                        addNotificationCard(
                                document.getId(),
                                title,
                                message,
                                read != null && read
                        );
                    }
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Unable to load notifications.",
                            Toast.LENGTH_LONG
                    ).show();

                    tvEmptyNotifications.setVisibility(
                            View.VISIBLE
                    );
                });
    }

    private void addNotificationCard(
            String documentId,
            String title,
            String message,
            boolean isRead
    ) {

        CardView card = new CardView(this);

        card.setRadius(18f);
        card.setCardElevation(3f);
        card.setUseCompatPadding(true);
        card.setCardBackgroundColor(
                android.graphics.Color.WHITE
        );

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        layout.setPadding(
                18,
                18,
                18,
                18
        );

        TextView icon =
                new TextView(this);

        icon.setText("•");
        icon.setTextSize(28);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(
                isRead
                        ? android.graphics.Color.GRAY
                        : android.graphics.Color.rgb(
                        25,
                        118,
                        210
                )
        );

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(
                        35,
                        60
                );

        layout.addView(icon, iconParams);

        LinearLayout details =
                new LinearLayout(this);

        details.setOrientation(
                LinearLayout.VERTICAL
        );

        TextView tvTitle =
                new TextView(this);

        tvTitle.setText(title);
        tvTitle.setTextSize(16);
        tvTitle.setTextColor(
                android.graphics.Color.rgb(
                        23,
                        43,
                        77
                )
        );

        tvTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        details.addView(tvTitle);

        TextView tvMessage =
                new TextView(this);

        tvMessage.setText(message);
        tvMessage.setTextSize(13);
        tvMessage.setTextColor(
                android.graphics.Color.rgb(
                        102,
                        112,
                        133
                )
        );

        LinearLayout.LayoutParams messageParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        messageParams.setMargins(
                0,
                5,
                0,
                0
        );

        details.addView(
                tvMessage,
                messageParams
        );

        layout.addView(
                details,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        card.addView(layout);

        card.setOnClickListener(v ->
                markNotificationRead(
                        documentId
                )
        );

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

        notificationsContainer.addView(
                card,
                cardParams
        );
    }

    private void markNotificationRead(
            String documentId
    ) {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .document(documentId)
                .update("read", true)
                .addOnSuccessListener(unused ->
                        loadNotifications()
                );
    }

    private void markAllAsRead() {

        if (mAuth.getCurrentUser() == null) {
            return;
        }

        String uid =
                mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch =
                            db.batch();

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        batch.update(
                                document.getReference(),
                                "read",
                                true
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        this,
                                        "All notifications marked as read.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadNotifications();
                            });
                });
    }
}