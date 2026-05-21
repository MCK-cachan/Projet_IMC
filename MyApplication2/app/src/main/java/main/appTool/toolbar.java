package main.appTool;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.example.myapplication2.R;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executors;

import main.animation.animations;
import main.login.LoginActivity;
import main.serveur;

public class toolbar {

    public static final int PICK_IMAGE_PROFILE = 1002;
    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_LANG = "app_lang";

    public static void init(Activity activity) {
        animations.init(activity);

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences loginPrefs = activity.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);

        // --- GESTION DE L'ICÔNE DE PROFIL ---
        ImageView profileImg = activity.findViewById(R.id.img_profile_simple);
        if (profileImg != null) {
            String savedPhoto = loginPrefs.getString(LoginActivity.KEY_USER_PHOTO, "");

            if (savedPhoto != null && !savedPhoto.isEmpty() && !savedPhoto.equals("default.png")) {
                loadProfileImage(profileImg, savedPhoto);
            } else {
                // Image par défaut
                profileImg.setImageResource(R.drawable.ic_face);
                TypedValue typedValue = new TypedValue();
                activity.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
                profileImg.setColorFilter(typedValue.data);
            }

            // Menu Pop-up au clic
            profileImg.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(activity, profileImg);
                popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_change_profile_picture) {
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        activity.startActivityForResult(intent, PICK_IMAGE_PROFILE);
                        return true;
                    } else if (id == R.id.menu_change_password) {
                        showPasswordEditDialog(activity);
                        return true;
                    } else if (id == R.id.menu_change_name) {
                        showNameEditDialog(activity);
                        return true;
                    } else if (id == R.id.menu_change_email) {
                        showEmailEditDialog(activity);
                        return true;
                    } else if (id == R.id.menu_logout) {
                        logout(activity);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        // --- GESTION DU THÈME ---
        ImageView btnTheme = activity.findViewById(R.id.btn_switch_theme);
        if (btnTheme != null) {
            int currentMode = AppCompatDelegate.getDefaultNightMode();
            btnTheme.setImageResource(currentMode == AppCompatDelegate.MODE_NIGHT_YES ? R.drawable.ic_night : R.drawable.ic_day);

            // On force l'icône en blanc (notamment ic_day en mode jour)
            btnTheme.setColorFilter(Color.WHITE);

            btnTheme.setOnClickListener(v -> {
                animations.triggerThemeChanged();
                int newMode = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
                              ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
                prefs.edit().putInt(KEY_THEME, newMode).apply();
                AppCompatDelegate.setDefaultNightMode(newMode);
            });
        }

        // --- GESTION DE LA LANGUE ---
        ImageView btnLanguage = activity.findViewById(R.id.btn_switch_language);
        if (btnLanguage != null) {
            String currentLang = activity.getResources().getConfiguration().getLocales().get(0).getLanguage();
            btnLanguage.setImageResource(currentLang.equals("fr") ? R.drawable.ic_flag_fr : R.drawable.ic_flag_en);
            btnLanguage.setOnClickListener(v -> {
                String lang = activity.getResources().getConfiguration().getLocales().get(0).getLanguage();
                String nextLang = lang.equals("fr") ? "en" : "fr";
                prefs.edit().putString(KEY_LANG, nextLang).apply();
                LocaleListCompat appLocales = LocaleListCompat.forLanguageTags(nextLang);
                AppCompatDelegate.setApplicationLocales(appLocales);
            });
        }
    }

    private static void loadProfileImage(ImageView img, String photoData) {
        try {
            img.setImageTintList(null);
            img.setColorFilter(null);

            if (photoData.startsWith("/uploads/")) {
                String imageUrl = serveur.BASE_URL + photoData.replaceFirst("^/", "");
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        java.net.URL url = new java.net.URL(imageUrl);
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.connect();
                        Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream());
                        new Handler(Looper.getMainLooper()).post(() -> img.setImageBitmap(bitmap));
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() -> img.setImageResource(R.drawable.ic_face));
                    }
                });
            }

        } catch (Exception e) {
            img.setImageResource(R.drawable.ic_face);
        }
    }

    public static void updateProfileOnServer(Activity activity, Uri imageUri) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream inputStream = activity.getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

                serveur.updateImage(activity, base64Image, () -> {
                    SharedPreferences loginPrefs = activity.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
                    loginPrefs.edit().putString(LoginActivity.KEY_USER_PHOTO, base64Image).apply();

                    ImageView profileImg = activity.findViewById(R.id.img_profile_simple);
                    if (profileImg != null) {
                        profileImg.setImageURI(null);
                        profileImg.setImageURI(imageUri);
                        profileImg.setImageTintList(null);
                        profileImg.setColorFilter(null);
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private static void showNameEditDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Changer Nom et Prénom");
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_name, null);
        EditText editNom = view.findViewById(R.id.edit_new_nom);
        EditText editPrenom = view.findViewById(R.id.edit_new_prenom);
        builder.setView(view);
        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            String nom = editNom.getText().toString().trim();
            String prenom = editPrenom.getText().toString().trim();
            if (!nom.isEmpty() || !prenom.isEmpty()) serveur.updateName(activity, nom, prenom);
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private static void showEmailEditDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Changer l'adresse mail");
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_email, null);
        EditText editEmail = view.findViewById(R.id.edit_new_email);
        builder.setView(view);
        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            String email = editEmail.getText().toString().trim();
            if (!email.isEmpty()) serveur.updateEmail(activity, email);
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private static void showPasswordEditDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Changer le mot de passe");
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_edit_password, null);
        EditText editPassword = view.findViewById(R.id.edit_new_password);
        builder.setView(view);
        builder.setPositiveButton("Enregistrer", (dialog, which) -> {
            String password = editPassword.getText().toString().trim();
            if (!password.isEmpty()) serveur.updatePassword(activity, password);
        });
        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private static void logout(Activity activity) {
        SharedPreferences loginPrefs = activity.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        loginPrefs.edit().clear().apply();
        Toast.makeText(activity, "Déconnexion réussie", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void applySavedSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int savedTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (AppCompatDelegate.getDefaultNightMode() != savedTheme) AppCompatDelegate.setDefaultNightMode(savedTheme);
        String savedLang = prefs.getString(KEY_LANG, null);
        if (savedLang != null) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLang));
    }
}
