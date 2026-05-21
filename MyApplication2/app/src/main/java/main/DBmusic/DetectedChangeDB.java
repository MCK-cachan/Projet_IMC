package main.DBmusic;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;

import com.tonpackage.database.AppDatabase;
import com.tonpackage.database.Artiste;
import com.tonpackage.database.MusicDao;
import com.tonpackage.database.Musique;
import com.tonpackage.database.Playlist;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import main.serveur;

public class DetectedChangeDB {

    private static boolean isObserving = false;

    /**
     * Commence à observer les changements dans la base de données Room.
     * Si un changement est détecté dans les tables Playlist, Musique ou Artiste,
     * une synchronisation avec le serveur est lancée.
     */
    public static void observe(Context context) {
        if (isObserving) return;
        isObserving = true;

        AppDatabase db = AppDatabase.getInstance(context);
        // On observe les tables définies dans les entités Room
        String[] tables = {"Playlist", "Musique", "Artiste"};

        db.getInvalidationTracker().addObserver(new InvalidationTracker.Observer(tables) {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                // Déclenché dès qu'une modification (Insert/Update/Delete) survient
                sync(context);
            }
        });
    }

    private static void sync(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            MusicDao dao = db.musicDao();

            // Récupère toutes les données de la DB locale
            List<Playlist> playlists = dao.getAllPlaylists();
            List<Musique> musiques = dao.getAllMusics();
            List<Artiste> artistes = dao.getAllArtistes();

            // Envoi au serveur via la méthode syncDatabase de serveur.java
            serveur.syncDatabase(context, playlists, musiques, artistes, () -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(context, "Synchronisation réussie !", Toast.LENGTH_SHORT).show();
                });
            });
        });
    }
}
