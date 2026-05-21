package main.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.R;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

import main.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editEmail, editPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private SharedPreferences prefs;

    // Constantes pour SharedPreferences
    public static final String PREF_NAME = "secure_user_prefs";
    public static final String KEY_IS_LOGGED = "is_logged_in";
    public static final String KEY_USER_EMAIL = "user_email";
    public static final String KEY_USER_NOM = "user_nom";
    public static final String KEY_USER_PRENOM = "user_prenom";
    public static final String KEY_USER_PHOTO = "user_photo";
    public static final String KEY_USER_TOKEN = "user_token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Vérification automatique au lancement
        prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_IS_LOGGED, false)) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.login_progress);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cas spécial Admin
        if (email.equals("admin") && password.equals("admin")) {
            saveUserAndGo("Admin", "Système", "admin@app.com", "android.resource://com.example.myapplication2/drawable/ic_menu", "token_admin_local");
            return;
        }

        performHttpPost(email, password);
    }

    private void performHttpPost(String email, String password) {
        btnLogin.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            String resultNom = "", resultPrenom = "", resultPhoto = "", resultToken = "";
            boolean success = false;

            try {
                // URL du serveur
                URL url = new URL("http://servfatout.duckdns.org:8081/user/login.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                // Corps de la requête
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("mail", email);
                jsonParam.put("password", password);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonParam.toString().getBytes("utf-8"));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) { // Correction ici : 200 au lieu de 800
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) response.append(line);
                    
                    JSONObject res = new JSONObject(response.toString());
                    
                    // Extraction du token (au premier niveau)
                    resultToken = res.optString("token", "");

                    // Extraction des infos utilisateur (dans l'objet "user")
                    if (res.has("user")) {
                        JSONObject userObj = res.getJSONObject("user");
                        resultNom = userObj.optString("nom", "Utilisateur");
                        resultPrenom = userObj.optString("prenom", "");
                        resultPhoto = userObj.optString("image", "");
                        success = true;
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            final boolean isOk = success;
            final String fNom = resultNom;
            final String fPrenom = resultPrenom;
            final String fPhoto = resultPhoto;
            final String fToken = resultToken;

            new Handler(Looper.getMainLooper()).post(() -> {
                btnLogin.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                if (isOk) {
                    saveUserAndGo(fNom, fPrenom, email, fPhoto, fToken);
                } else {
                    Toast.makeText(this, "Identifiants incorrects ou erreur serveur", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void saveUserAndGo(String nom, String prenom, String email, String photo, String token) {
        prefs.edit()
                .putBoolean(KEY_IS_LOGGED, true)
                .putString(KEY_USER_NOM, nom)
                .putString(KEY_USER_PRENOM, prenom)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_PHOTO, photo)
                .putString(KEY_USER_TOKEN, token)
                .apply();
        goToMain();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
