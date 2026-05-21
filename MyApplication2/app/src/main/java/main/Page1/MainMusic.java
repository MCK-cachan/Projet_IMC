package main.Page1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication2.R;
import java.util.ArrayList;
import java.util.List;
import main.animation.animationButton;
import main.appTool.toolbar;
import main.appTool.toolbar_music;
import main.music.PlaylistAdd;
import main.music.manage;
import main.music.searchGestion;
import main.DBmusic.DetectedChangeDB;

public class MainMusic extends AppCompatActivity {

    private List<TextView> chips = new ArrayList<>();
    private View searchBarContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_page1);

        // 1. Initialisation des outils et barres
        toolbar.init(this);
        toolbar_music.init(this);
        
        // Initialisation de l'observation des changements de la base de données
        DetectedChangeDB.observe(this);
        
        // Initialisation de la recherche
        EditText searchInput = findViewById(R.id.search_input);
        if (searchInput != null) {
            searchGestion.initSearch(this, searchInput);
        }

        // Récupération de la barre de recherche
        searchBarContainer = findViewById(R.id.search_bar_container);

        // 2. Initialisation des boutons Chips
        initChips(this);

        // 3. Chargement initial de la page Home
        main.music.manage.loadAllModulesHome(this);

        View btnMusic = findViewById(R.id.btn_music_glossy);
        if (btnMusic != null) {
            btnMusic.setOnClickListener(v -> NavigationHelper.ouvrirMusique(this));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        PlaylistAdd.handleImageResult(requestCode, resultCode, data);
    }

    public void initChips(Activity activity) {
        chips.clear();
        chips.add(findViewById(R.id.chip_home));
        chips.add(findViewById(R.id.chip_podcast));
        chips.add(findViewById(R.id.chip_playlist));
        chips.add(findViewById(R.id.chip_speaker));

        for (TextView chip : chips) {
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    animationButton.applyBouncingEffect(v);
                    selectChip((TextView) v);
                    handleChipAction(v.getId());
                });
            }
        }

        if (!chips.isEmpty()) {
            selectChip(chips.get(0));
            updateSearchBarVisibility(R.id.chip_home);
        }
    }

    private void selectChip(TextView selectedChip) {
        for (TextView chip : chips) {
            if (chip == null) continue;
            if (chip == selectedChip) {
                chip.setBackgroundResource(R.drawable.glow_background);
                chip.setTextColor(Color.WHITE);
                chip.setElevation(15f);
            } else {
                chip.setBackgroundResource(R.drawable.button_capsule);
                chip.setTextColor(Color.GRAY);
                chip.setElevation(2f);
            }
        }
    }

    private void handleChipAction(int viewId) {
        updateSearchBarVisibility(viewId);
        
        // On vide la recherche lors d'un changement d'onglet
        EditText searchInput = findViewById(R.id.search_input);
        if (searchInput != null) searchInput.setText("");

        if (viewId == R.id.chip_home) {
            main.music.manage.loadAllModulesHome(this);
        } else if (viewId == R.id.chip_podcast) {
            main.music.manage.loadAllModulesPodcast(this);
        } else if (viewId == R.id.chip_playlist) {
            main.music.manage.loadAllModulesPlaylist(this);
        } else if (viewId == R.id.chip_speaker) {
            main.music.manage.loadAllModulesSpeaker(this);
        }
    }

    private void updateSearchBarVisibility(int viewId) {
        if (searchBarContainer != null) {
            if (viewId == R.id.chip_home || viewId == R.id.chip_podcast) {
                searchBarContainer.setVisibility(View.VISIBLE);
            } else {
                searchBarContainer.setVisibility(View.GONE);
            }
        }
    }
}
