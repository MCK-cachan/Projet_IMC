package main.music;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.tonpackage.database.AppDatabase;
import com.tonpackage.database.Playlist;
import java.io.FileOutputStream;
import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import com.example.myapplication2.R;
public class DBmManager {
    public static void initDb(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);

        // On vérifie en arrière-plan
        Executors.newSingleThreadExecutor().execute(() -> {
            if (db.musicDao().getAllPlaylists().isEmpty()) {
                Playlist p = new Playlist();
                p.Name = "Ma Playlist";
                // On enregistre l'image par défaut sur le disque et on récupère son nom
                p.imagePath = saveDrawableToDisk(context, R.drawable.music_bloc_like, "default_cover.jpg");
                db.musicDao().insertPlaylist(p);
            }
        });
    }

    private static String saveDrawableToDisk(Context context, int resId, String fileName) {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
            File file = new File(context.getFilesDir(), fileName);
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();
            return fileName; // On retourne juste le nom
        } catch (Exception e) {
            return null;
        }
    }
}