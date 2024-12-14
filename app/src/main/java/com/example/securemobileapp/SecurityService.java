package com.example.securemobileapp;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class SecurityService extends Service {

    private final IBinder binder = new LocalBinder();
    FirebaseAuth firebaseAuth;

    @Override
    public void onCreate() {
        super.onCreate();
        firebaseAuth = FirebaseAuth.getInstance();
        PackageManager packageManager = getPackageManager();
    }

    public class LocalBinder extends Binder{
        public SecurityService getService(){
            return SecurityService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void blockMaliciousApps(List<String> maliciousApps){
        for(String packageName : maliciousApps){
            try{
                Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
                intent.setData(Uri.parse("package:" + packageName));
                intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }catch (Exception e) {
                // Обробка помилок при спробі видалити або заблокувати додаток
                Toast.makeText(this, e.getMessage() + packageName, Toast.LENGTH_SHORT).show();
            }
        }
    }
}