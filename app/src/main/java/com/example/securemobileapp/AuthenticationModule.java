package com.example.securemobileapp;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;

public class AuthenticationModule {

    private final FirebaseAuth firebaseAuth;

    public AuthenticationModule(){
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public Task<AuthResult> register(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> login(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

}
