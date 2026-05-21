package main.appTool;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.example.myapplication2.R;

import main.animation.animations;
import main.login.LoginActivity;

public class toolbar {

    private static final String PREFS_NAME = "app_settings";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_LANG = "app_lang";

    public static void init(Activity activity) {
        animations.init(activity);

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // --- GESTION DE L'ICÔNE DE PROFIL SIMPLE ---
        ImageView profileImg = activity.findViewById(R.id.img_profile_simple);
        if (profileImg != null) {
            profileImg.setImageResource(R.drawable.ic_face);
            
            // Appliquer la couleur du background à l'icône
            TypedValue typedValue = new TypedValue();
            activity.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true);
            profileImg.setColorFilter(typedValue.data);

            // Menu Pop-up au clic sur la photo de profil
            profileImg.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(activity, profileImg);
                popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_change_profile_picture) {
                        Toast.makeText(activity, "Changer la photo de profil", Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (id == R.id.menu_settings) {
                        Toast.makeText(activity, "Paramètres", Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (id == R.id.menu_change_password) {
                        Toast.makeText(activity, "Changer le mot de passe", Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (id == R.id.menu_change_name) {
                        Toast.makeText(activity, "Changer le nom et prénom", Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (id == R.id.menu_change_email) {
                        Toast.makeText(activity, "Changer l'adresse mail", Toast.LENGTH_SHORT).show();
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
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                btnTheme.setImageResource(R.drawable.ic_night); 
            } else {
                btnTheme.setImageResource(R.drawable.ic_day);
            }

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

    private static void logout(Activity activity) {
        // Supprimer les données de connexion
        SharedPreferences loginPrefs = activity.getSharedPreferences(LoginActivity.PREF_NAME, Context.MODE_PRIVATE);
        loginPrefs.edit().clear().apply();

        Toast.makeText(activity, "Déconnexion réussie", Toast.LENGTH_SHORT).show();

        // Retourner à l'écran de connexion
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void applySavedSettings(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int savedTheme = prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (AppCompatDelegate.getDefaultNightMode() != savedTheme) {
            AppCompatDelegate.setDefaultNightMode(savedTheme);
        }
        String savedLang = prefs.getString(KEY_LANG, null);
        if (savedLang != null) {
            LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
            if (currentLocales.isEmpty() || !currentLocales.toLanguageTags().equals(savedLang)) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLang));
            }
        }
    }
}
