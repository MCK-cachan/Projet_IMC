package main.music;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication2.R;
import com.tonpackage.database.AppDatabase;
import com.tonpackage.database.Musique;
import com.tonpackage.database.Playlist;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import main.appTool.toolbar_music;

public class PlaylistManage {

    public static void openPlaylistDetail(Activity activity, Playlist playlist) {
        LinearLayout dynamicContainer = activity.findViewById(R.id.dynamic_modules_container);
        if (dynamicContainer == null) return;

        // Gestion du retour
        if (activity instanceof AppCompatActivity) {
            AppCompatActivity host = (AppCompatActivity) activity;
            OnBackPressedCallback callback = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    manage.loadAllModulesHome(activity);
                    this.remove();
                }
            };
            host.getOnBackPressedDispatcher().addCallback(host, callback);
        }

        dynamicContainer.removeAllViews();
        View detailView = LayoutInflater.from(activity).inflate(R.layout.music_playlist_manage, dynamicContainer, false);

        // UI
        TextView titleView = detailView.findViewById(R.id.playlist_title);
        ImageView bigImage = detailView.findViewById(R.id.playlist_big_image);
        ImageView btnBack = detailView.findViewById(R.id.btn_back);
        ImageView btnPlay = detailView.findViewById(R.id.btn_play_playlist);
        RecyclerView rvSongs = detailView.findViewById(R.id.rv_playlist_songs);

        titleView.setText(playlist.Name);
        loadPlaylistImage(bigImage, playlist.imagePath, playlist.selectedColor);

        // Liste locale pour stocker les musiques de la playlist
        final List<Musique> songsInPlaylist = new ArrayList<>();

        btnBack.setOnClickListener(v -> {
            main.animation.animationButton.applyBouncingEffect(v);
            if (activity instanceof AppCompatActivity) {
                ((AppCompatActivity) activity).onBackPressed();
            } else {
                manage.loadAllModulesHome(activity);
            }
        });

        btnPlay.setOnClickListener(v -> {
            main.animation.animationButton.applyBouncingEffect(v);
            if (!songsInPlaylist.isEmpty()) {
                // On remplace la file actuelle par celle de la playlist
                musicFile.createQueue(songsInPlaylist);
                
                // On lance la musique actuellement en tête de file
                Musique first = musicFile.getCurrentMusic();
                if (first != null) {
                    if (first.idMusic != null) {
                        toolbar_music.setMusicInfoById(first.idMusic);
                    } else {
                        toolbar_music.updateUI(first);
                    }
                }
            }
        });

        // Liste des musiques
        rvSongs.setLayoutManager(new LinearLayoutManager(activity));
        SongListAdapter songAdapter = new SongListAdapter();
        rvSongs.setAdapter(songAdapter);

        loadSongsFromDb(activity, playlist.IdPlaylist, songAdapter, songsInPlaylist);
        dynamicContainer.addView(detailView);
    }

    private static void loadPlaylistImage(ImageView img, String path, Integer color) {
        img.clearColorFilter();
        if (path == null || path.isEmpty()) {
            img.setImageResource(R.drawable.music_bloc_like);
            return;
        }

        if (path.startsWith("logo:")) {
            String logoName = path.substring(5);
            int resId = img.getContext().getResources().getIdentifier(logoName, "drawable", img.getContext().getPackageName());
            if (resId != 0) {
                img.setImageResource(resId);
                if (color != null) {
                    img.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }
                return;
            }
        }

        if (path.startsWith("/")) {
            Bitmap bmp = BitmapFactory.decodeFile(path);
            if (bmp != null) {
                img.setImageBitmap(bmp);
                return;
            }
        }

        try (InputStream is = img.getContext().getAssets().open(normalizePath(path))) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            img.setImageBitmap(bmp);
        } catch (IOException e) {
            img.setImageResource(R.drawable.music_bloc_like);
        }
    }

    private static String normalizePath(String raw) {
        if (raw == null) return "";
        String p = raw.replace("\\", "/");
        return p.contains("assets/") ? p.substring(p.indexOf("assets/") + 7) : p;
    }

    private static void loadSongsFromDb(Activity activity, int playlistId, SongListAdapter adapter, List<Musique> outList) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Musique> songs = AppDatabase.getInstance(activity).musicDao().getMusicsByPlaylist(playlistId);
            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.setSongs(songs);
                outList.clear();
                outList.addAll(songs);
            });
        });
    }

    private static class SongListAdapter extends RecyclerView.Adapter<SongListAdapter.SongHolder> {
        private final List<Musique> songList = new ArrayList<>();

        void setSongs(List<Musique> songs) {
            songList.clear();
            songList.addAll(songs);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SongHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_song_line, parent, false);
            return new SongHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SongHolder holder, int position) {
            Musique m = songList.get(position);
            holder.title.setText(m.musicTitle);
            holder.artist.setText(m.artisteName);
            loadPlaylistImage(holder.img, m.imagePath, null);

            holder.itemView.setOnClickListener(v -> {
                main.animation.animationButton.applyBouncingEffect(v);
                
                // On charge toute la playlist dans la file
                musicFile.createQueue(songList);
                
                // On cherche la musique cliquée dans la file (car elle peut avoir bougé si le shuffle est actif)
                int newPos = 0;
                List<Musique> currentQueue = musicFile.getQueue();
                for (int i = 0; i < currentQueue.size(); i++) {
                    Musique qm = currentQueue.get(i);
                    // Comparaison par titre et artiste (ou ID si dispo)
                    if (qm.musicTitle.equals(m.musicTitle) && qm.artisteName.equals(m.artisteName)) {
                        newPos = i;
                        break;
                    }
                }
                
                // On définit la position actuelle sur la musique cliquée
                musicFile.setCurrentPosition(newPos);
                
                // On met à jour l'UI et on lance la lecture
                Musique current = musicFile.getCurrentMusic();
                if (current != null) {
                    if (current.idMusic != null) {
                        toolbar_music.setMusicInfoById(current.idMusic);
                    } else {
                        toolbar_music.updateUI(current);
                    }
                }
            });

            holder.options.setOnClickListener(v -> {
                main.animation.animationButton.applyBouncingEffect(v);

                List<String> actions = Arrays.asList(
                        popupMenu.ACTION_QUEUE,
                        popupMenu.ACTION_DELETE,
                        popupMenu.ACTION_ADD_PLAYLIST,
                        popupMenu.ACTION_SHARE
                );

                popupMenu.showCustomMenu(v, m, actions, () -> {
                    int currentPos = holder.getAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION) {
                        songList.remove(currentPos);
                        notifyItemRemoved(currentPos);
                        notifyItemRangeChanged(currentPos, songList.size());
                    }
                });
            });
        }

        @Override
        public int getItemCount() { return songList.size(); }

        static class SongHolder extends RecyclerView.ViewHolder {
            ImageView img, options;
            TextView title, artist;
            SongHolder(View v) {
                super(v);
                img = v.findViewById(R.id.song_img);
                options = v.findViewById(R.id.song_options);
                title = v.findViewById(R.id.song_title);
                artist = v.findViewById(R.id.song_artist);
            }
        }
    }
}