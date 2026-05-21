package main.music;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.myapplication2.R;
import com.tonpackage.database.AppDatabase;
import com.tonpackage.database.Artiste;
import com.tonpackage.database.Musique;
import com.tonpackage.database.Playlist;
import java.util.List;
import java.util.concurrent.Executors;

public class manage {

    public static void loadAllModulesHome(Activity activity) {
        LinearLayout container = activity.findViewById(R.id.dynamic_modules_container);
        if (container == null) {
            Log.e("MUSIC_MANAGE", "Container dynamic_modules_container introuvable !");
            return;
        }
        container.removeAllViews();

        // Par défaut, showText est true, donc les noms seront affichés.
        MusicModule modulePlaylists = new MusicModule(activity, container, "Mes Playlists", true);
        MusicModule moduleMusics    = new MusicModule(activity, container, "Titres favoris", true);
        MusicModule moduleArtistes  = new MusicModule(activity, container, "Artistes", true);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(activity);
                List<Playlist> playlists = db.musicDao().getAllPlaylists();
                List<Musique> musics = db.musicDao().getAllMusicOnly();
                List<Artiste> artistes = db.musicDao().getMusicians();

                new Handler(Looper.getMainLooper()).post(() -> {
                    // Log pour debug
                    Log.d("MUSIC_MANAGE", "Playlists: " + playlists.size() + ", Musics: " + musics.size() + ", Artistes: " + artistes.size());
                    
                    if (playlists.isEmpty() && musics.isEmpty() && artistes.isEmpty()) {
                        Toast.makeText(activity, "Base de données vide !", Toast.LENGTH_SHORT).show();
                    }

                    modulePlaylists.addBoxes(playlists);
                    moduleMusics.addBoxes(musics);
                    moduleArtistes.addBoxes(artistes);
                });
            } catch (Exception e) {
                Log.e("MUSIC_MANAGE", "Erreur DB: ", e);
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(activity, "Erreur de chargement DB", Toast.LENGTH_LONG).show());
            }
        });
    }

    public static void loadAllModulesPodcast(Activity activity) {
        LinearLayout container = activity.findViewById(R.id.dynamic_modules_container);
        if (container == null) return;
        container.removeAllViews();
        new MusicModule(activity, container, "Podcasts récents", true);
    }

    public static void loadAllModulesPlaylist(Activity activity) {
        PlaylistPage.loadPlaylistAndPodcasts(activity);
    }

    public static void loadAllModulesSpeaker(Activity activity) {
        LinearLayout container = activity.findViewById(R.id.dynamic_modules_container);
        if (container == null) return;
        container.removeAllViews();
        new MusicModule(activity, container, "Appareils connectés", true);
    }
}
