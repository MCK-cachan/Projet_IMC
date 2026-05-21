package main;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.database.CursorWindow; // Import nécessaire
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.os.LocaleListCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication2.R;
import com.tonpackage.database.AppDatabase;
import com.tonpackage.database.Artiste;
import com.tonpackage.database.Playlist;

import java.lang.reflect.Field;
import java.util.List;

import main.Page1.NavigationHelper;
import main.animation.animations;
import main.appTool.toolbar;
import main.music.PlaylistAdd;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Appliquer les préférences (Thème et Langue) AVANT le super.onCreate
        toolbar.applySavedSettings(this);
        
        super.onCreate(savedInstanceState);

        // 1. AUGMENTER LA TAILLE DU CURSOR (Pour tes byte[] de musique/images)
        try {
            Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize");
            field.setAccessible(true);
            field.set(null, 100 * 1024 * 1024); // 100 Mo
        } catch (Exception e) {
            Log.e("DB_ERROR", "Erreur CursorWindow", e);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
        setContentView(R.layout.main_page1);

        View root = findViewById(android.R.id.content);
        View child = ((ViewGroup) root).getChildAt(0);

        child.setY(0);
        // 2. INITIALISATION DES OUTILS
        toolbar.init(this);
        animations.init(this);

        // 3. CHARGEMENT DES MODULES (Appel Room à l'intérieur)
        try {
            main.music.manage.loadAllModulesHome(this);
        } catch (Exception e) {
            Log.e("DB_ERROR", "Crash pendant le chargement des modules", e);
        }

        Log.d("DEBUG_UI", "ActionBar = " + getSupportActionBar());

        new Thread(() -> {
            try {
                // 1. Test des Playlists (via musicDao)
                List<Playlist> check = AppDatabase.getInstance(this).musicDao().getAllPlaylists();
                Log.d("MainActivity", "Nombre de playlists : " + check.size());

                // 2. TEST DE LA TABLE ARTISTE (via musicDao aussi !)
                List<Artiste> artistes = AppDatabase.getInstance(this).musicDao().getAllArtistes();

                Log.i("MainActivity"," Nombre d'artistes : " + artistes.size());
                int nbArtistes = artistes.size();

                Log.d("DB_TEST", "Succès ! Table Artiste accessible. Nombre d'artistes : " + nbArtistes);

            } catch (Exception e) {
                Log.e("DB_ERROR", "Erreur d'accès à la table Artiste : " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
        
        // Initialisation du bouton Musique
        FrameLayout btnMusic = findViewById(R.id.btn_music_glossy);
        if (btnMusic != null) {
            btnMusic.setOnClickListener(v -> goToMusic(v));
        }
        FrameLayout btnFood = findViewById(R.id.btn_food);
        if (btnFood != null) {
            //mettre l'activiter de food
            btnFood.setOnClickListener( v -> Toast.makeText(this, "c ok t bath", Toast.LENGTH_SHORT).show());

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PlaylistAdd.handleImageResult(requestCode, resultCode, data);
    }

    public void goToMusic(View view) {
        NavigationHelper.ouvrirMusique(this);
    }

}