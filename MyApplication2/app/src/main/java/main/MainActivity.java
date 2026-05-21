package main;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.database.CursorWindow; 
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.example.myapplication2.R;
import com.tonpackage.database.AppDatabase;
import com.tonpackage.database.Artiste;
import com.tonpackage.database.Playlist;

import java.lang.reflect.Field;
import java.util.List;

import main.Page1.NavigationHelper;
import main.animation.animations;
import main.appTool.toolbar;
import main.appTool.toolbar_music;
import main.music.PlaylistAdd;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        toolbar.applySavedSettings(this);
        super.onCreate(savedInstanceState);

        try {
            Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize");
            field.setAccessible(true);
            field.set(null, 100 * 1024 * 1024);
        } catch (Exception e) {
            Log.e("DB_ERROR", "Erreur CursorWindow", e);
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
        
        setContentView(R.layout.main_page1);
        toolbar.init(this);
        toolbar_music.init(this); // Initialise la toolbar musique sur le menu principal
        animations.init(this);

        updateLastIMC();
        
        findViewById(R.id.btn_music_glossy).setOnClickListener(v -> NavigationHelper.ouvrirMusique(this));
        findViewById(R.id.btn_food).setOnClickListener(v -> NavigationHelper.ouvrirIMC(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLastIMC();
        // Rafraîchir l'état de la toolbar si une musique tourne
        toolbar_music.init(this);
    }

    private void updateLastIMC() {
        TextView txtLastIMC = findViewById(R.id.txt_last_imc);
        if (txtLastIMC != null) {
            SharedPreferences prefs = getSharedPreferences("IMC_prefs", MODE_PRIVATE);
            float lastIMC = prefs.getFloat("IMCdata", -1f);
            if (lastIMC > 0) {
                txtLastIMC.setText("Dernier IMC : " + String.format("%.2f", lastIMC));
                txtLastIMC.setVisibility(View.VISIBLE);
            } else {
                txtLastIMC.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == toolbar.PICK_IMAGE_PROFILE && resultCode == RESULT_OK && data != null) {
            toolbar.updateProfileOnServer(this, data.getData());
        } else {
            PlaylistAdd.handleImageResult(requestCode, resultCode, data);
        }
    }
}
