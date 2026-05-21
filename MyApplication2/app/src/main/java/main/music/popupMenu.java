package main.music;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;

import com.example.myapplication2.R;
import com.tonpackage.database.AppDatabase;
import com.tonpackage.database.Artiste;
import com.tonpackage.database.Musique;
import com.tonpackage.database.Playlist;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;

import main.appTool.toolbar_music;
import main.serveur;

public class popupMenu {

    public static final String ACTION_PLAY = "PLAY";
    public static final String ACTION_QUEUE = "QUEUE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_ADD_PLAYLIST = "ADD_PLAYLIST";
    public static final String ACTION_SHARE = "SHARE";

    public static void showCustomMenu(View view, Musique musique, List<String> allowedActions, Runnable onDeleteCallback) {
        showMenuInternal(view, musique, null, null, allowedActions, onDeleteCallback);
    }

    public static void showPlaylistMenu(View view, Playlist playlist, List<String> allowedActions, Runnable onDeleteCallback) {
        showMenuInternal(view, null, playlist, null, allowedActions, onDeleteCallback);
    }

    public static void showArtisteMenu(View view, Artiste artiste, List<String> allowedActions, Runnable onDeleteCallback) {
        showMenuInternal(view, null, null, artiste, allowedActions, onDeleteCallback);
    }

    private static void showMenuInternal(View view, Musique musique, Playlist playlist, Artiste artiste, List<String> allowedActions, Runnable onDeleteCallback) {
        Context context = view.getContext();
        Context wrapper = new ContextThemeWrapper(context, R.style.PopupMenuStyle);
        PopupMenu popup = new PopupMenu(wrapper, view);

        for (String action : allowedActions) {
            switch (action) {
                case ACTION_PLAY:
                    popup.getMenu().add(0, 0, 0, "Lire maintenant").setIcon(android.R.drawable.ic_media_play);
                    break;
                case ACTION_QUEUE:
                    popup.getMenu().add(0, 1, 1, "Lire en file d'attente").setIcon(R.drawable.ic_add_file);
                    break;
                case ACTION_DELETE:
                    String label = "Supprimer";
                    if (playlist != null) label = "Supprimer la playlist";
                    else if (artiste != null) label = "Se désabonner";
                    popup.getMenu().add(0, 2, 2, label).setIcon(R.drawable.ic_playlist_remove);
                    break;
                case ACTION_ADD_PLAYLIST:
                    popup.getMenu().add(0, 3, 3, "Ajouter à une playlist").setIcon(R.drawable.ic_playlist_add);
                    break;
                case ACTION_SHARE:
                    popup.getMenu().add(0, 4, 4, "Partager").setIcon(R.drawable.ic_share);
                    break;
            }
        }

        applyIconsStyle(wrapper, popup);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 0) {
                if (musique != null) {
                    musicFile.addMusic(musique);
                    if (musique.idMusic != null) toolbar_music.setMusicInfoById(musique.idMusic);
                    else toolbar_music.updateUI(musique);
                } else if (playlist != null) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        List<Musique> songs = AppDatabase.getInstance(context).musicDao().getMusicsByPlaylist(playlist.IdPlaylist);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!songs.isEmpty()) {
                                musicFile.createQueue(songs);
                                Musique first = songs.get(0);
                                if (first.idMusic != null) toolbar_music.setMusicInfoById(first.idMusic);
                                else toolbar_music.updateUI(first);
                            } else {
                                Toast.makeText(context, "Playlist vide", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
            } else if (id == 1) {
                if (musique != null) {
                    musicFile.addQueue(musique);
                    Toast.makeText(context, "Ajouté à la file", Toast.LENGTH_SHORT).show();
                } else if (playlist != null) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        List<Musique> songs = AppDatabase.getInstance(context).musicDao().getMusicsByPlaylist(playlist.IdPlaylist);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!songs.isEmpty()) {
                                for (Musique s : songs) musicFile.addQueue(s);
                                Toast.makeText(context, songs.size() + " morceaux ajoutés à la file", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "Playlist vide", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                }
            } else if (id == 2) {
                if (playlist != null) deletePlaylistFromDb(context, playlist, onDeleteCallback);
                else if (musique != null) deleteMusicFromDb(context, musique, onDeleteCallback);
                else if (onDeleteCallback != null) onDeleteCallback.run();
            } else if (id == 3 && musique != null) {
                showPlaylistSelection(context, musique);
            } else if (id == 4) {
                if (playlist != null) sharePlaylist(context, playlist);
                else if (musique != null) shareSong(context, musique);
                else if (artiste != null) shareArtiste(context, artiste);
            }
            return true;
        });

        popup.show();
    }

    public static void showPlaylistSelection(Context context, Musique musique) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Playlist> lists = AppDatabase.getInstance(context).musicDao().getAllPlaylists();
            
            new Handler(Looper.getMainLooper()).post(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Choisir une playlist");

                // Adaptateur personnalisé pour afficher Image + Texte
                ArrayAdapter<Playlist> adapter = new ArrayAdapter<Playlist>(context, android.R.layout.select_dialog_item, lists) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView text = view.findViewById(android.R.id.text1);
                        Playlist p = getItem(position);
                        
                        text.setText(p.Name);
                        text.setCompoundDrawablePadding(32);
                        
                        Drawable icon = getPlaylistIcon(context, p);
                        if (icon != null) {
                            int size = (int) (40 * context.getResources().getDisplayMetrics().density);
                            icon.setBounds(0, 0, size, size);
                            text.setCompoundDrawables(icon, null, null, null);
                        }
                        
                        return view;
                    }
                };

                builder.setAdapter(adapter, (dialog, which) -> {
                    checkAndSave(context, musique, lists.get(which).IdPlaylist);
                });
                builder.show();
            });
        });
    }

    private static Drawable getPlaylistIcon(Context context, Playlist p) {
        if (p.imagePath == null) return null;

        if (p.imagePath.startsWith("logo:")) {
            String logoName = p.imagePath.replace("logo:", "");
            int resId = context.getResources().getIdentifier(logoName, "drawable", context.getPackageName());
            if (resId != 0) {
                Drawable drawable = context.getDrawable(resId).mutate();
                if (p.selectedColor != null) {
                    drawable.setColorFilter(p.selectedColor, PorterDuff.Mode.SRC_IN);
                }
                return drawable;
            }
        } else {
            File file = new File(p.imagePath);
            if (file.exists()) {
                return Drawable.createFromPath(p.imagePath);
            }
        }
        return null;
    }

    private static void deletePlaylistFromDb(Context context, Playlist playlist, Runnable callback) {
        new AlertDialog.Builder(context)
                .setTitle("Supprimer la playlist")
                .setMessage("Voulez-vous vraiment supprimer la playlist '" + playlist.Name + "' ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppDatabase.getInstance(context).musicDao().deletePlaylist(playlist.IdPlaylist);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(context, "Playlist supprimée", Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.run();
                        });
                    });
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private static void deleteMusicFromDb(Context context, Musique musique, Runnable callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            if (musique.idMusic != null) {
                AppDatabase.getInstance(context).musicDao().deleteMusic(musique.idMusic);
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "Musique supprimée", Toast.LENGTH_SHORT).show();
                if (callback != null) callback.run();
            });
        });
    }

    private static void applyIconsStyle(Context context, PopupMenu popup) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
        int iconColor = ta.getColor(0, android.graphics.Color.BLACK);
        ta.recycle();

        for (int i = 0; i < popup.getMenu().size(); i++) {
            Drawable icon = popup.getMenu().getItem(i).getIcon();
            if (icon != null) icon.setTint(iconColor);
        }

        try {
            java.lang.reflect.Field field = popup.getClass().getDeclaredField("mPopup");
            field.setAccessible(true);
            Object menuHelper = field.get(popup);
            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", boolean.class).invoke(menuHelper, true);
        } catch (Exception ignored) {}
    }

    private static void shareSong(Context context, Musique m) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, "Écoute ce morceau : " + m.musicTitle + " de " + m.artisteName);
        context.startActivity(Intent.createChooser(intent, "Partager via"));
    }

    private static void sharePlaylist(Context context, Playlist p) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Musique> songs = AppDatabase.getInstance(context).musicDao().getMusicsByPlaylist(p.IdPlaylist);
            StringBuilder sb = new StringBuilder("Check ma playlist '" + p.Name + "' :\n");
            for (Musique s : songs) {
                sb.append("- ").append(s.musicTitle).append(" (").append(s.artisteName).append(")\n");
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
                context.startActivity(Intent.createChooser(intent, "Partager la playlist via"));
            });
        });
    }

    private static void shareArtiste(Context context, Artiste a) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        String type = a.isPodcast ? "ce podcasteur" : "cet artiste";
        intent.putExtra(Intent.EXTRA_TEXT, "Écoute " + type + " : " + a.Pseudo);
        context.startActivity(Intent.createChooser(intent, "Partager via"));
    }

    private static void checkAndSave(Context context, Musique m, Long plId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            if (db.musicDao().checkIfMusicInPlaylist(m.musicTitle, plId)) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "Déjà dans la playlist !", Toast.LENGTH_SHORT).show());
            } else {
                // Signalement au serveur
                serveur.addToForever(context, m.idMusic);

                Musique copy = new Musique();
                copy.musicTitle = m.musicTitle;
                copy.artisteName = m.artisteName;
                copy.idPlaylist = plId;
                copy.idArtiste = m.idArtiste;

                // 1. Download Image
                if (m.imagePath != null && m.imagePath.startsWith("http")) {
                    String fileName = "img_" + System.currentTimeMillis() + ".jpg";
                    String localPath = downloadFile(context, m.imagePath, "musics_images", fileName);
                    copy.imagePath = (localPath != null) ? localPath : m.imagePath;
                } else {
                    copy.imagePath = m.imagePath;
                }

                // 2. Download Audio
                if (m.audioPath != null && m.audioPath.startsWith("http")) {
                    String fileName = "audio_" + System.currentTimeMillis() + ".mp3";
                    String localPath = downloadFile(context, m.audioPath, "musics_audio", fileName);
                    copy.audioPath = (localPath != null) ? localPath : m.audioPath;
                } else {
                    copy.audioPath = m.audioPath;
                }

                db.musicDao().insertMusic(copy);
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "music ajouter", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private static String downloadFile(Context context, String urlString, String dirName, String fileName) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.connect();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            File dir = new File(context.getFilesDir(), dirName);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);

            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
