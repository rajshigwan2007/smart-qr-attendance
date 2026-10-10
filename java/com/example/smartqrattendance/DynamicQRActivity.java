package com.example.smartqrattendance;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DynamicQRActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ImageView ivQRCode;
    private TextView tvClass, tvSubject, tvTimer, tvQRStatus, tvTokenTimer;
    private TextView btnSessionMenu;
    private Button btnCloseSession;

    private String selectedClass = "";
    private String selectedSubject = "";
    private String sessionId = "";

    private long sessionDurationMillis = 15 * 60_000L;
    private long sessionEndTime = 0L;

    private boolean sessionActive = false;
    private boolean closingSession = false;
    private boolean sessionCreated = false;
    private boolean tokenWriteInProgress = false;

    private CountDownTimer countDownTimer;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private Runnable qrRotationRunnable;
    private Runnable tokenTimerRunnable;

    private static final long QR_CHANGE_INTERVAL = 10_000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dynamic_qr);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivQRCode = findViewById(R.id.ivQRCode);
        tvClass = findViewById(R.id.tvQRClass);
        tvSubject = findViewById(R.id.tvQRSubject);
        tvTimer = findViewById(R.id.tvQRTimer);
        tvQRStatus = findViewById(R.id.tvQRStatus);
        tvTokenTimer = findViewById(R.id.tvQRTokenTimer);
        btnSessionMenu = findViewById(R.id.btnSessionMenu);
        btnCloseSession = findViewById(R.id.btnCloseSession);

        selectedClass = safe(
                getIntent().getStringExtra("selectedClass")
        );

        selectedSubject = safe(
                getIntent().getStringExtra("selectedSubject")
        );

        long minutes = getIntent().getLongExtra(
                "durationMinutes",
                15
        );

        if (minutes <= 0) {
            minutes = 15;
        }

        sessionDurationMillis = minutes * 60_000L;

        tvClass.setText(selectedClass);
        tvSubject.setText(selectedSubject);

        tvTimer.setText(
                formatDuration(sessionDurationMillis)
        );

        tvQRStatus.setText("Creating live session...");
        tvTokenTimer.setText("Please wait...");

        ivQRCode.setVisibility(View.INVISIBLE);

        btnCloseSession.setEnabled(false);

        btnCloseSession.setOnClickListener(
                v -> confirmCloseSession()
        );

        btnSessionMenu.setOnClickListener(
                v -> showSessionMenu()
        );

        startSession();
    }

    private void startSession() {

        if (mAuth.getCurrentUser() == null) {
            showError("Teacher is not logged in.");
            finish();
            return;
        }

        if (selectedClass.isEmpty()) {
            showError("Class is required.");
            finish();
            return;
        }

        if (selectedSubject.isEmpty()) {
            showError("Subject is required.");
            finish();
            return;
        }

        if (sessionCreated) {
            return;
        }

        sessionCreated = true;

        String teacherId = mAuth.getCurrentUser().getUid();

        sessionId = UUID.randomUUID().toString();

        long startedAt = System.currentTimeMillis();

        sessionEndTime =
                startedAt + sessionDurationMillis;

        createInitialToken(
                teacherId,
                startedAt
        );
    }

    private void createInitialToken(
            String teacherId,
            long startedAt
    ) {

        String token = UUID.randomUUID().toString();

        long tokenExpiresAt = Math.min(
                startedAt + QR_CHANGE_INTERVAL,
                sessionEndTime
        );

        Map<String, Object> data = new HashMap<>();

        data.put("teacherId", teacherId);
        data.put("className", selectedClass);
        data.put("subjectName", selectedSubject);

        data.put("sessionId", sessionId);

        data.put("active", true);

        data.put("startedAt", startedAt);
        data.put("endsAt", sessionEndTime);

        data.put(
                "durationMinutes",
                sessionDurationMillis / 60_000L
        );

        data.put("currentToken", token);
        data.put("tokenExpiresAt", tokenExpiresAt);

        db.collection("users")
                .document(teacherId)
                .collection("attendanceSessions")
                .document(sessionId)
                .set(data)
                .addOnSuccessListener(v -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    sessionActive = true;

                    btnCloseSession.setEnabled(true);

                    showQR(
                            token,
                            tokenExpiresAt
                    );

                    startCountdown();

                    startQRRotation();

                })
                .addOnFailureListener(e -> {

                    sessionCreated = false;

                    sessionActive = false;

                    ivQRCode.setVisibility(View.INVISIBLE);

                    tvQRStatus.setText(
                            "SESSION CREATION FAILED"
                    );

                    tvTokenTimer.setText(
                            "Could not connect to Firebase"
                    );

                    showError(
                            "Could not create attendance session."
                    );
                });
    }

    private void showQR(
            String token,
            long tokenExpiresAt
    ) {

        String teacherId =
                mAuth.getCurrentUser().getUid();

        String qrData =
                buildQrData(
                        teacherId,
                        sessionId,
                        token,
                        tokenExpiresAt
                );

        try {

            Bitmap bitmap =
                    generateQRCode(
                            qrData,
                            760,
                            760
                    );

            ivQRCode.setImageBitmap(bitmap);

            ivQRCode.setVisibility(View.VISIBLE);

            tvQRStatus.setText(
                    "LIVE • SCAN THIS QR"
            );

            startTokenTimer(tokenExpiresAt);

        } catch (WriterException e) {

            showError(
                    "QR generation failed."
            );
        }
    }

    private String buildQrData(
            String teacherId,
            String sessionId,
            String token,
            long expiresAt
    ) {

        return "SMART_QR_ATTENDANCE"
                + "|teacherId=" + teacherId
                + "|sessionId=" + sessionId
                + "|token=" + token
                + "|expiresAt=" + expiresAt;
    }

    private void generateAndPublishQR() {

        if (!sessionActive
                || closingSession
                || tokenWriteInProgress) {

            return;
        }

        long now =
                System.currentTimeMillis();

        if (now >= sessionEndTime) {

            closeSessionAutomatically();
            return;
        }

        String teacherId =
                mAuth.getCurrentUser().getUid();

        String token =
                UUID.randomUUID().toString();

        long tokenExpiresAt =
                Math.min(
                        now + QR_CHANGE_INTERVAL,
                        sessionEndTime
                );

        tokenWriteInProgress = true;

        Map<String, Object> update =
                new HashMap<>();

        update.put(
                "currentToken",
                token
        );

        update.put(
                "tokenExpiresAt",
                tokenExpiresAt
        );

        update.put(
                "active",
                true
        );

        db.collection("users")
                .document(teacherId)
                .collection("attendanceSessions")
                .document(sessionId)
                .set(update, SetOptions.merge())

                .addOnSuccessListener(v -> {

                    tokenWriteInProgress = false;

                    if (!sessionActive
                            || closingSession) {

                        return;
                    }

                    // IMPORTANT:
                    // Firebase update successful झाल्यावरच
                    // नवीन QR display करतो.
                    showQR(
                            token,
                            tokenExpiresAt
                    );

                })

                .addOnFailureListener(e -> {

                    tokenWriteInProgress = false;

                    tvQRStatus.setText(
                            "QR SYNC FAILED — RETRYING"
                    );

                    handler.postDelayed(
                            this::generateAndPublishQR,
                            1500
                    );
                });
    }

    private Bitmap generateQRCode(
            String data,
            int width,
            int height
    ) throws WriterException {

        BitMatrix matrix =
                new MultiFormatWriter().encode(
                        data,
                        BarcodeFormat.QR_CODE,
                        width,
                        height
                );

        Bitmap bitmap =
                Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                );

        for (int x = 0; x < width; x++) {

            for (int y = 0; y < height; y++) {

                bitmap.setPixel(
                        x,
                        y,
                        matrix.get(x, y)
                                ? Color.BLACK
                                : Color.WHITE
                );
            }
        }

        return bitmap;
    }

    private void startQRRotation() {

        stopQRRotation();

        qrRotationRunnable =
                new Runnable() {

                    @Override
                    public void run() {

                        if (!sessionActive
                                || closingSession) {

                            return;
                        }

                        if (System.currentTimeMillis()
                                >= sessionEndTime) {

                            closeSessionAutomatically();
                            return;
                        }

                        generateAndPublishQR();

                        handler.postDelayed(
                                this,
                                QR_CHANGE_INTERVAL
                        );
                    }
                };

        handler.postDelayed(
                qrRotationRunnable,
                QR_CHANGE_INTERVAL
        );
    }

    private void startCountdown() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer =
                new CountDownTimer(
                        Math.max(
                                0,
                                sessionEndTime
                                        - System.currentTimeMillis()
                        ),
                        1000
                ) {

                    @Override
                    public void onTick(long left) {

                        tvTimer.setText(
                                formatDuration(left)
                        );
                    }

                    @Override
                    public void onFinish() {

                        tvTimer.setText("00:00");

                        closeSessionAutomatically();
                    }
                };

        countDownTimer.start();
    }

    private void startTokenTimer(
            long expiresAt
    ) {

        if (tokenTimerRunnable != null) {

            handler.removeCallbacks(
                    tokenTimerRunnable
            );
        }

        tokenTimerRunnable =
                new Runnable() {

                    @Override
                    public void run() {

                        if (!sessionActive
                                || closingSession) {

                            return;
                        }

                        long left =
                                expiresAt
                                        - System.currentTimeMillis();

                        if (left <= 0) {

                            tvTokenTimer.setText(
                                    "Refreshing QR..."
                            );

                            return;
                        }

                        tvTokenTimer.setText(
                                "Current QR expires in "
                                        + ((left + 999) / 1000)
                                        + " sec"
                        );

                        handler.postDelayed(
                                this,
                                250
                        );
                    }
                };

        handler.post(
                tokenTimerRunnable
        );
    }

    private String formatDuration(
            long millis
    ) {

        long total =
                Math.max(0, millis) / 1000;

        return String.format(
                "%02d:%02d",
                total / 60,
                total % 60
        );
    }

    private void confirmCloseSession() {

        if (!sessionActive
                || closingSession) {

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "Close attendance session"
                )
                .setMessage(
                        "Students will no longer be able to mark attendance."
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Close session",
                        (d, w) ->
                                closeSessionInFirestore(
                                        "Session closed by teacher."
                                )
                )
                .show();
    }

    private void closeSessionAutomatically() {

        if (!sessionActive
                || closingSession) {

            return;
        }

        closeSessionInFirestore(
                "Session ended automatically."
        );
    }

    private void closeSessionInFirestore(
            String reason
    ) {

        if (!sessionActive
                || closingSession) {

            return;
        }

        closingSession = true;
        sessionActive = false;

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        stopQRRotation();

        if (tokenTimerRunnable != null) {

            handler.removeCallbacks(
                    tokenTimerRunnable
            );
        }

        ivQRCode.setVisibility(
                View.INVISIBLE
        );

        tvQRStatus.setText(
                "SESSION CLOSED"
        );

        tvTokenTimer.setText(
                "QR disabled"
        );

        tvTimer.setText("00:00");

        btnCloseSession.setEnabled(false);

        if (mAuth.getCurrentUser() == null) {

            finish();
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put("active", false);
        data.put(
                "closedAt",
                System.currentTimeMillis()
        );
        data.put(
                "closeReason",
                reason
        );
        data.put(
                "currentToken",
                ""
        );
        data.put(
                "tokenExpiresAt",
                0L
        );

        db.collection("users")
                .document(
                        mAuth.getCurrentUser().getUid()
                )
                .collection("attendanceSessions")
                .document(sessionId)
                .set(
                        data,
                        SetOptions.merge()
                )
                .addOnCompleteListener(task -> {

                    Toast.makeText(
                            this,
                            reason,
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                });
    }

    private void showSessionMenu() {

        android.widget.PopupMenu menu =
                new android.widget.PopupMenu(
                        this,
                        btnSessionMenu
                );

        menu.getMenu().add("Attendance");
        menu.getMenu().add("Students");
        menu.getMenu().add("History");
        menu.getMenu().add("Reports");

        menu.setOnMenuItemClickListener(
                item -> {

                    Intent intent = null;

                    switch (
                            item.getTitle().toString()
                    ) {

                        case "Attendance":
                            intent =
                                    new Intent(
                                            this,
                                            TeacherAttendanceActivity.class
                                    );
                            break;

                        case "Students":
                            intent =
                                    new Intent(
                                            this,
                                            TeacherStudentsActivity.class
                                    );
                            break;

                        case "History":
                            intent =
                                    new Intent(
                                            this,
                                            TeacherHistoryActivity.class
                                    );
                            break;

                        case "Reports":
                            intent =
                                    new Intent(
                                            this,
                                            TeacherReportsActivity.class
                                    );
                            break;
                    }

                    if (intent != null) {

                        intent.putExtra(
                                "selectedClass",
                                selectedClass
                        );

                        intent.putExtra(
                                "selectedSubject",
                                selectedSubject
                        );

                        startActivity(intent);

                        return true;
                    }

                    return false;
                }
        );

        menu.show();
    }

    private void stopQRRotation() {

        if (qrRotationRunnable != null) {

            handler.removeCallbacks(
                    qrRotationRunnable
            );

            qrRotationRunnable = null;
        }
    }

    @Override
    public void onBackPressed() {

        if (sessionActive
                && !closingSession) {

            confirmCloseSession();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        stopQRRotation();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (tokenTimerRunnable != null) {

            handler.removeCallbacks(
                    tokenTimerRunnable
            );
        }

        handler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    private String safe(String value) {

        return value == null
                ? ""
                : value.trim();
    }

    private void showError(String message) {

        tvQRStatus.setText(message);

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}