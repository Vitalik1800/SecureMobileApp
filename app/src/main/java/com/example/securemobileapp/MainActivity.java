package com.example.securemobileapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.securemobileapp.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {

    SecurityService securityService;
    boolean isBound = false;
    ActivityMainBinding binding;
    private static final String TAG = "BackupData";

    private static final String CHANNEL_ID = "com.example.securemobileapp.channel";
    private SecretKey secretKey;
    FirebaseAuth auth;
    FirebaseFirestore firebaseFirestore;
    String userId;
    ThreatDetentionModule threatDetentionModule;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            SecurityService.LocalBinder binder = (SecurityService.LocalBinder) iBinder;
            securityService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser().getUid();
        threatDetentionModule = new ThreatDetentionModule();
        DocumentReference documentReference = firebaseFirestore.collection("Users").document(userId);
        documentReference.addSnapshotListener(this, (value, error) -> binding.tvUserEmail.setText(value.getString("Email")));
        try {
            secretKey = EncryptionUtils.createSecretKey();
        } catch (Exception e) {
            Toast.makeText(this, "Не вдалося створити ключ для шифрування", Toast.LENGTH_SHORT).show();
        }

        createNotificationChannel();

        Intent intent = new Intent(this, SecurityService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

        binding.btnCheckMalware.setOnClickListener(v -> {
            if(isBound){
                List<String> maliciousApps = threatDetentionModule.checkForThreats(this);
                if(!maliciousApps.isEmpty()){
                    try {
                        // Encrypt the list of malicious apps
                        String encryptedApps = EncryptionUtils.encrypt(String.join(",", maliciousApps), secretKey);
                        securityService.blockMaliciousApps(maliciousApps);

                        // Display the unencrypted app names and encrypted data
                        binding.tvResult.setText("Знайдено шкідливі програми: " + String.join(", ", maliciousApps) + "\nШифрований список: " + encryptedApps);

                        // Save the list of malicious apps to a file
                        saveMaliciousAppsToFile(maliciousApps);

                        backupData();
                        sendNotification();
                    } catch (Exception e) {
                        binding.tvResult.setText("Помилка шифрування");
                    }
                } else{
                    binding.tvResult.setText("Шкідливих програм не знайдено");
                }
            } else{
                Toast.makeText(this, "Служба не підключена", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isBound){
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = "Сповіщення безпеки";
            String description = "Сповіщення про безпеку";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationChannel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if(notificationManager != null){
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    @SuppressLint("NotificationPermission")
    private void sendNotification() {
        try {
            String encryptedMessage = EncryptionUtils.encrypt("Знайдено шкідливу програму на пристрої!", secretKey);
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            @SuppressLint({"NewApi", "LocalSuppress"}) Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Шкідлива програма виявлена")
                    .setContentText(encryptedMessage)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .build();

            if (notificationManager != null) {
                notificationManager.notify(1, notification);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Помилка шифрування повідомлення", Toast.LENGTH_SHORT).show();
        }
    }

    private void backupData() {
        // Check if external storage is available
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File sourceFile = new File(getFilesDir(), "data_to_backup.txt"); // Your file to back up

            // Check if the source file exists, if not, create it
            if (!sourceFile.exists()) {
                try {
                    // Create a new file and write some initial data if the file doesn't exist
                    if (sourceFile.createNewFile()) {
                        Log.d(TAG, "File created: " + sourceFile.getAbsolutePath());

                        // Example data to write to the file
                        String initialData = "This is the initial data for backup.";

                        // Write initial data to the file
                        try (FileOutputStream fos = new FileOutputStream(sourceFile)) {
                            fos.write(initialData.getBytes());
                            Log.d(TAG, "Initial data written to file.");
                        } catch (IOException e) {
                            Toast.makeText(this, "Помилка при запису в файл", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error writing initial data to file", e);
                        }
                    } else {
                        Log.d(TAG, "File already exists: " + sourceFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    Toast.makeText(this, "Помилка при створенні файлу", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error creating file", e);
                }
            }

            File destinationFile = new File(Environment.getExternalStorageDirectory(), "backup_data.txt");

            try (FileInputStream fis = new FileInputStream(sourceFile);
                 FileOutputStream fos = new FileOutputStream(destinationFile)) {

                // Write user data to the backup file
                String userData = "User Email: " + binding.tvUserEmail.getText().toString() + "\n";
                fos.write(userData.getBytes()); // Write user email to the backup file
                Log.d(TAG, "User email written to backup: " + userData);

                // Read from the file and write to the backup
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }

                Toast.makeText(this, "Дані успішно збережено в резервну копію", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Backup successful: " + destinationFile.getAbsolutePath());

            } catch (IOException e) {
                Toast.makeText(this, "Помилка при створенні резервної копії", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error during backup", e);
            }
        } else {
            Toast.makeText(this, "Зовнішнє сховище недоступне", Toast.LENGTH_SHORT).show();
        }
    }


    private void saveMaliciousAppsToFile(List<String> maliciousApps) {
        // Write the list of malicious apps to a file
        File maliciousAppsFile = new File(Environment.getExternalStorageDirectory(), "malicious_apps.txt");

        try (FileOutputStream fos = new FileOutputStream(maliciousAppsFile, true)) {
            // Append the malicious apps to the file
            for (String app : maliciousApps) {
                fos.write((app + "\n").getBytes());
            }
            Log.d(TAG, "Malicious apps saved to file: " + maliciousAppsFile.getAbsolutePath());
        } catch (IOException e) {
            Toast.makeText(this, "Помилка при збереженні шкідливих програм", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Error saving malicious apps to file", e);
        }
    }
}
