package main.music;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.R;
import main.serveur;

import java.util.ArrayList;
import java.util.List;

public class searchGestion {

    private static Handler searchHandler = new Handler(Looper.getMainLooper());
    private static Runnable searchRunnable;
    private static OnBackPressedCallback searchBackCallback;

    /**
     * Initialise la logique de recherche avec un système de délai (debounce).
     */
    public static void initSearch(Activity activity, EditText searchInput) {
        if (searchInput == null) return;

        // Gestion du bouton "Entrer" ou "Rechercher" du clavier
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                
                // Cacher le clavier
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }

                // Lancer la recherche immédiatement si elle n'a pas encore été déclenchée
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) {
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    displaySearchResults(activity, query, searchInput);
                }
                return true;
            }
            return false;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // On annule la recherche précédente si l'utilisateur tape encore
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                String query = s.toString().trim();

                if (query.isEmpty()) {
                    if (searchBackCallback != null) {
                        searchBackCallback.remove();
                        searchBackCallback = null;
                    }
                    manage.reloadCurrentSection(activity);
                } else {
                    // On attend 300ms avant de lancer la recherche
                    searchRunnable = () -> displaySearchResults(activity, query, searchInput);
                    searchHandler.postDelayed(searchRunnable, 300);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Remplace les modules actuels par les modules de recherche remplis par le serveur.
     */
    private static void displaySearchResults(Activity activity, String query, EditText searchInput) {
        LinearLayout container = activity.findViewById(R.id.dynamic_modules_container);
        if (container == null) return;

        // Gestion du bouton retour système
        if (activity instanceof AppCompatActivity) {
            AppCompatActivity host = (AppCompatActivity) activity;
            if (searchBackCallback != null) {
                searchBackCallback.remove();
            }
            searchBackCallback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    searchInput.setText(""); // Vider la recherche déclenche le retour à la section via le TextWatcher
                    this.remove();
                    searchBackCallback = null;
                }
            };
            host.getOnBackPressedDispatcher().addCallback(host, searchBackCallback);
        }

        // Vider le conteneur pour injecter les nouveaux modules de recherche
        container.removeAllViews();

        // 1. Création visuelle immédiate des modules
        MusicModule searchMusics = new MusicModule(activity, container, "Musiques trouvées", true);
        MusicModule searchArtists = new MusicModule(activity, container, "Artistes", true);
        MusicModule searchAlbums = new MusicModule(activity, container, "Albums", true);
        MusicModule searchPlaylists = new MusicModule(activity, container, "Playlists", true);

        // 2. Lancement des appels API en parallèle via serveur.java

        // Recherche des Musiques
        serveur.search(activity, query, "track", new serveur.SearchCallback() {
            @Override
            public void onResult(List<?> results) {
                searchMusics.addBoxes(results);
            }
            @Override
            public void onError(String message) {
                Log.e("SEARCH", "Erreur musiques: " + message);
            }
        });

        // Recherche des Artistes
        serveur.search(activity, query, "artist", new serveur.SearchCallback() {
            @Override
            public void onResult(List<?> results) {
                searchArtists.addBoxes(results);
            }
            @Override
            public void onError(String message) {
                Log.e("SEARCH", "Erreur artistes: " + message);
            }
        });

        // Recherche des Albums
        serveur.search(activity, query, "album", new serveur.SearchCallback() {
            @Override
            public void onResult(List<?> results) {
                searchAlbums.addBoxes(results);
            }
            @Override
            public void onError(String message) {
                Log.e("SEARCH", "Erreur albums: " + message);
            }
        });

        // Recherche des Playlists
        serveur.search(activity, query, "playlist", new serveur.SearchCallback() {
            @Override
            public void onResult(List<?> results) {
                searchPlaylists.addBoxes(results);
            }
            @Override
            public void onError(String message) {
                Log.e("SEARCH", "Erreur playlists: " + message);
            }
        });
    }
}
