package com.example.smartqrattendance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StudentQRScannerActivity extends AppCompatActivity {

    private Button btnScanQR;
    private TextView tvScanStatus;
    private TextView tvStudentDetails;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String>
            cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            startQRScanner();
                        } else {
                            showError(
                                    "Camera permission is required to scan QR."
                            );
                        }
                    }
            );

    private final ActivityResultLauncher<ScanOptions>
            qrScannerLauncher =
            registerForActivityResult(
                    new ScanContract(),
                    result -> {

                        if (result.getContents() == null) {

                            tvScanStatus.setText(
                                    "Scan cancelled. Tap Scan QR to try again."
                            );

                            return;
                        }

                        handleQRResult(
                                result.getContents()
                        );
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_student_qr_scanner
        );

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnScanQR =
                findViewById(R.id.btnScanQR);

        tvScanStatus =
                findViewById(R.id.tvScanStatus);

        tvStudentDetails =
                findViewById(R.id.tvStudentDetails);

        tvStudentDetails.setVisibility(View.GONE);

        tvScanStatus.setText(
                "Ready to scan the teacher's live QR."
        );

        btnScanQR.setOnClickListener(
                v -> checkCameraPermissionAndScan()
        );
    }

    private void checkCameraPermissionAndScan() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            startQRScanner();

        } else {

            cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
            );
        }
    }

    private void startQRScanner() {

        ScanOptions options =
                new ScanOptions();

        options.setDesiredBarcodeFormats(
                ScanOptions.QR_CODE
        );

        options.setPrompt(
                "Scan the teacher's current attendance QR"
        );

        options.setBeepEnabled(true);

        options.setOrientationLocked(false);

        qrScannerLauncher.launch(options);
    }

    private void handleQRResult(String qrData) {

        tvScanStatus.setText("Checking QR...");
        tvStudentDetails.setVisibility(View.GONE);

        if (qrData == null
                || !qrData.startsWith("SMART_QR_ATTENDANCE")) {

            showError(
                    "This is not a valid Smart QR."
            );

            return;
        }

        String teacherId =
                getValueFromQR(
                        qrData,
                        "teacherId"
                );

        String sessionId =
                getValueFromQR(
                        qrData,
                        "sessionId"
                );

        String token =
                getValueFromQR(
                        qrData,
                        "token"
                );

        String expiresText =
                getValueFromQR(
                        qrData,
                        "expiresAt"
                );

        if (isEmpty(teacherId)
                || isEmpty(sessionId)
                || isEmpty(token)
                || isEmpty(expiresText)) {

            showError(
                    "QR data is incomplete."
            );

            return;
        }

        long qrExpiresAt;

        try {

            qrExpiresAt =
                    Long.parseLong(expiresText);

        } catch (Exception e) {

            showError(
                    "Invalid QR expiry."
            );

            return;
        }

        long now =
                System.currentTimeMillis();

        if (now >= qrExpiresAt) {

            showError(
                    "QR expired. Scan the latest QR."
            );

            return;
        }

        if (mAuth.getCurrentUser() == null) {

            showError(
                    "Student is not logged in."
            );

            return;
        }

        String studentId =
                mAuth.getCurrentUser().getUid();

        verifySessionAndMarkAttendance(
                teacherId,
                sessionId,
                studentId,
                token,
                qrExpiresAt
        );
    }

    private void verifySessionAndMarkAttendance(
            String teacherId,
            String sessionId,
            String studentId,
            String scannedToken,
            long qrExpiresAt
    ) {

        db.collection("users")
                .document(teacherId)
                .collection("attendanceSessions")
                .document(sessionId)
                .get()
                .addOnSuccessListener(session -> {

                    if (!session.exists()) {

                        showError(
                                "Attendance session not found."
                        );

                        return;
                    }

                    Boolean active =
                            session.getBoolean("active");

                    String currentToken =
                            session.getString("currentToken");

                    Long tokenExpiresAt =
                            session.getLong("tokenExpiresAt");

                    Long endsAt =
                            session.getLong("endsAt");

                    long now =
                            System.currentTimeMillis();

                    if (!Boolean.TRUE.equals(active)) {

                        showError(
                                "Attendance session is closed."
                        );

                        return;
                    }

                    if (currentToken == null
                            || !currentToken.equals(scannedToken)) {

                        showError(
                                "Old QR detected. Scan the latest QR."
                        );

                        return;
                    }

                    if (tokenExpiresAt == null
                            || now >= tokenExpiresAt
                            || now >= qrExpiresAt) {

                        showError(
                                "QR expired. Scan the new QR."
                        );

                        return;
                    }

                    if (endsAt == null
                            || now >= endsAt) {

                        showError(
                                "Attendance session has ended."
                        );

                        return;
                    }

                    String className =
                            safe(
                                    session.getString(
                                            "className"
                                    )
                            );

                    String subjectName =
                            safe(
                                    session.getString(
                                            "subjectName"
                                    )
                            );

                    loadStudentProfileAndMark(
                            teacherId,
                            sessionId,
                            studentId,
                            className,
                            subjectName
                    );

                })
                .addOnFailureListener(e ->
                        showError(
                                "Could not verify live session. Check Firebase Rules."
                        )
                );
    }

    private void loadStudentProfileAndMark(
            String teacherId,
            String sessionId,
            String studentId,
            String className,
            String subjectName
    ) {

        db.collection("users")
                .document(studentId)
                .get()
                .addOnSuccessListener(profile -> {

                    if (!profile.exists()) {

                        showError(
                                "Student profile not found."
                        );

                        return;
                    }

                    String studentClass =
                            safe(
                                    profile.getString(
                                            "className"
                                    )
                            );

                    if (!studentClass.isEmpty()
                            && !className.isEmpty()
                            && !studentClass.equalsIgnoreCase(
                            className
                    )) {

                        showError(
                                "This QR is for "
                                        + className
                                        + ", not your class."
                        );

                        return;
                    }

                    markAttendance(
                            teacherId,
                            sessionId,
                            studentId,
                            className,
                            subjectName,
                            profile
                    );

                })
                .addOnFailureListener(e ->
                        showError(
                                "Could not load your student profile."
                        )
                );
    }

    private void markAttendance(
            String teacherId,
            String sessionId,
            String studentId,
            String className,
            String subjectName,
            DocumentSnapshot profile
    ) {

        db.collection("users")
                .document(teacherId)
                .collection("attendanceSessions")
                .document(sessionId)
                .collection("attendance")
                .document(studentId)
                .get()
                .addOnSuccessListener(existing -> {

                    if (existing.exists()) {

                        tvScanStatus.setText(
                                "Attendance already marked ✓"
                        );

                        saveExistingRecordToStudentHistory(
                                studentId,
                                sessionId,
                                existing
                        );

                        showStudentDetails(existing);

                        return;
                    }

                    String studentName =
                            safe(
                                    profile.getString("name")
                            );

                    if (studentName.isEmpty()) {
                        studentName = "Student";
                    }

                    String email =
                            safe(
                                    profile.getString("email")
                            );

                    String rollNo =
                            safe(
                                    profile.getString("rollNo")
                            );

                    long markedAt =
                            System.currentTimeMillis();

                    Map<String, Object> attendance =
                            new HashMap<>();

                    attendance.put(
                            "studentId",
                            studentId
                    );

                    attendance.put(
                            "studentName",
                            studentName
                    );

                    attendance.put(
                            "studentEmail",
                            email
                    );

                    attendance.put(
                            "rollNo",
                            rollNo
                    );

                    attendance.put(
                            "teacherId",
                            teacherId
                    );

                    attendance.put(
                            "sessionId",
                            sessionId
                    );

                    attendance.put(
                            "className",
                            className
                    );

                    attendance.put(
                            "subjectName",
                            subjectName
                    );

                    attendance.put(
                            "markedAt",
                            markedAt
                    );

                    attendance.put(
                            "status",
                            "Present"
                    );

                    /*
                     * FIRST:
                     * Save teacher-side attendance.
                     */
                    db.collection("users")
                            .document(teacherId)
                            .collection("attendanceSessions")
                            .document(sessionId)
                            .collection("attendance")
                            .document(studentId)
                            .set(
                                    attendance,
                                    SetOptions.merge()
                            )
                            .addOnSuccessListener(v -> {

                                /*
                                 * SECOND:
                                 * Save same record to student's history.
                                 */
                                saveStudentHistory(
                                        studentId,
                                        sessionId,
                                        attendance
                                );

                            })
                            .addOnFailureListener(e ->
                                    showError(
                                            "Could not mark attendance. Check Firebase Rules."
                                    )
                            );

                })
                .addOnFailureListener(e ->
                        showError(
                                "Could not check existing attendance."
                        )
                );
    }

    private void saveStudentHistory(
            String studentId,
            String sessionId,
            Map<String, Object> attendance
    ) {

        db.collection("users")
                .document(studentId)
                .collection("attendanceHistory")
                .document(sessionId)
                .set(
                        attendance,
                        SetOptions.merge()
                )
                .addOnSuccessListener(v -> {

                    tvScanStatus.setText(
                            "ATTENDANCE MARKED ✓"
                    );

                    showStudentDetails(
                            safe(
                                    (String) attendance.get(
                                            "studentName"
                                    )
                            ),
                            safe(
                                    (String) attendance.get(
                                            "studentEmail"
                                    )
                            ),
                            safe(
                                    (String) attendance.get(
                                            "rollNo"
                                    )
                            ),
                            safe(
                                    (String) attendance.get(
                                            "className"
                                    )
                            ),
                            safe(
                                    (String) attendance.get(
                                            "subjectName"
                                    )
                            ),
                            attendance.get("markedAt") instanceof Long
                                    ? (Long) attendance.get("markedAt")
                                    : System.currentTimeMillis(),
                            "Present"
                    );

                    Toast.makeText(
                            this,
                            "Attendance marked successfully.",
                            Toast.LENGTH_LONG
                    ).show();

                })
                .addOnFailureListener(e -> {

                    /*
                     * Teacher attendance is already saved.
                     * If this happens, RULES are blocking
                     * student's attendanceHistory write.
                     */
                    tvScanStatus.setText(
                            "Attendance marked, but history could not be saved."
                    );

                    Toast.makeText(
                            this,
                            "Attendance marked. History permission denied.",
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void saveExistingRecordToStudentHistory(
            String studentId,
            String sessionId,
            DocumentSnapshot existing
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "studentId",
                safe(existing.getString("studentId"))
        );

        data.put(
                "studentName",
                safe(existing.getString("studentName"))
        );

        data.put(
                "studentEmail",
                safe(existing.getString("studentEmail"))
        );

        data.put(
                "rollNo",
                safe(existing.getString("rollNo"))
        );

        data.put(
                "teacherId",
                safe(existing.getString("teacherId"))
        );

        data.put(
                "sessionId",
                safe(existing.getString("sessionId"))
        );

        data.put(
                "className",
                safe(existing.getString("className"))
        );

        data.put(
                "subjectName",
                safe(existing.getString("subjectName"))
        );

        data.put(
                "status",
                safe(existing.getString("status"))
        );

        Long markedAt =
                existing.getLong("markedAt");

        data.put(
                "markedAt",
                markedAt == null
                        ? System.currentTimeMillis()
                        : markedAt
        );

        db.collection("users")
                .document(studentId)
                .collection("attendanceHistory")
                .document(sessionId)
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    private void showStudentDetails(
            DocumentSnapshot attendance
    ) {

        Long markedAt =
                attendance.getLong("markedAt");

        showStudentDetails(
                attendance.getString("studentName"),
                attendance.getString("studentEmail"),
                attendance.getString("rollNo"),
                attendance.getString("className"),
                attendance.getString("subjectName"),
                markedAt == null ? 0 : markedAt,
                attendance.getString("status")
        );
    }

    private void showStudentDetails(
            String name,
            String email,
            String rollNo,
            String className,
            String subjectName,
            long markedAt,
            String status
    ) {

        String time =
                markedAt > 0
                        ? new SimpleDateFormat(
                        "dd MMM yyyy, hh:mm:ss a",
                        Locale.getDefault()
                ).format(
                        new Date(markedAt)
                )
                        : "-";

        tvStudentDetails.setText(
                "Attendance confirmed\n\n"
                        + "Name: " + safe(name) + "\n"
                        + "Roll No: " + safe(rollNo) + "\n"
                        + "Email: " + safe(email) + "\n"
                        + "Class: " + safe(className) + "\n"
                        + "Subject: " + safe(subjectName) + "\n"
                        + "Time: " + time + "\n"
                        + "Status: " + safe(status)
        );

        tvStudentDetails.setVisibility(
                View.VISIBLE
        );
    }

    private String getValueFromQR(
            String qrData,
            String key
    ) {

        String marker =
                "|" + key + "=";

        int start =
                qrData.indexOf(marker);

        if (start == -1) {
            return null;
        }

        start += marker.length();

        int end =
                qrData.indexOf("|", start);

        return end == -1
                ? qrData.substring(start)
                : qrData.substring(start, end);
    }

    private boolean isEmpty(String value) {

        return value == null
                || value.trim().isEmpty();
    }

    private String safe(String value) {

        return value == null
                ? ""
                : value.trim();
    }

    private void showError(String message) {

        tvScanStatus.setText(message);

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}