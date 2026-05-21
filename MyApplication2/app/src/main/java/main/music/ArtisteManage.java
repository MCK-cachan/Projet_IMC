package main.music;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.tonpackage.database.Artiste;
import com.tonpackage.database.Musique;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import main.appTool.toolbar_music;

public class ArtisteManage {

    public static void openArtisteDetail(Activity activity, Artiste artiste) {
        LinearLayout dynamicContainer = activity.findViewById(R.id.dynamic_modules_container);
        if (dynamicContainer == null) return;

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
        View detailView = LayoutInflater.from(activity)
                .inflate(R.layout.music_playlist_manage, dynamicContainer, false);

        TextView  titleView = detailView.findViewById(R.id.playlist_title);
        ImageView bigImage  = detailView.findViewById(R.id.playlist_big_image);
        ImageView btnBack   = detailView.findViewById(R.id.btn_back);
        ImageView btnPlay   = detailView.findViewById(R.id.btn_play_playlist);
        RecyclerView rvSongs = detailView.findViewById(R.id.rv_playlist_songs);

        if (titleView != null) titleView.setText(artiste.Pseudo);
        if (bigImage  != null) loadImage(bigImage, artiste.imagePath);

        final List<Musique> artistSongs = new ArrayList<>();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                main.animation.animationButton.applyBouncingEffect(v);
                if (activity instanceof AppCompatActivity) {
                    ((AppCompatActivity) activity).onBackPressed();
                } else {
                    manage.loadAllModulesHome(activity);
                }
            });
        }

        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> {
                main.animation.animationButton.applyBouncingEffect(v);
                if (!artistSongs.isEmpty()) {
                    musicFile.createQueue(artistSongs);
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
        }

        if (rvSongs != null) {
            rvSongs.setLayoutManager(new LinearLayoutManager(activity));
            SongListAdapter adapter = new SongListAdapter();
            rvSongs.setAdapter(adapter);
            loadSongsFromDb(activity, artiste.idArtiste, adapter, artistSongs);
        }

        dynamicContainer.addView(detailView);
    }

    private static void loadSongsFromDb(Activity activity, int artisteId, SongListAdapter adapter, List<Musique> artistSongs) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Musique> songs = AppDatabase.getInstance(activity)
                    .musicDao()
                    .getMusicsByArtiste(artisteId);
            new Handler(Looper.getMainLooper()).post(() -> {
                adapter.setSongs(songs);
                artistSongs.clear();
                artistSongs.addAll(songs);
            });
        });
    }

    private static void loadImage(ImageView img, String raw) {
        if (raw == null || raw.isEmpty()) {
            img.setImageResource(R.drawable.music_bloc_like);
            return;
        }
        String path = raw.replace("\\", "/");
        if (path.contains("assets/")) path = path.substring(path.indexOf("assets/") + 7);

        try (InputStream is = img.getContext().getAssets().open(path)) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp != null) img.setImageBitmap(bmp);
        } catch (IOException e) {
            img.setImageResource(R.drawable.music_bloc_like);
        }
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
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.music_song_line, parent, false);
            return new SongHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SongHolder holder, int position) {
            Musique m = songList.get(position);
            if (holder.title  != null) holder.title.setText(m.musicTitle);
            if (holder.artist != null) holder.artist.setText(m.artisteName);
            if (holder.img    != null) loadImage(holder.img, m.imagePath);

            holder.itemView.setOnClickListener(v -> {
                main.animation.animationButton.applyBouncingEffect(v);
                
                musicFile.createQueue(songList);
                
                int newPos = 0;
                List<Musique> currentQueue = musicFile.getQueue();
                for (int i = 0; i < currentQueue.size(); i++) {
                    Musique qm = currentQueue.get(i);
                    if (qm.musicTitle.equals(m.musicTitle) && qm.artisteName.equals(m.artisteName)) {
                        newPos = i;
                        break;
                    }
                }
                
                musicFile.setCurrentPosition(newPos);
                
                Musique current = musicFile.getCurrentMusic();
                if (current != null) {
                    if (current.idMusic != null) {
                        toolbar_music.setMusicInfoById(current.idMusic);
                    } else {
                        toolbar_music.updateUI(current);
                    }
                }
            });

            // --- APPEL AU POPUPMENU CENTRALISÉ ---
            if (holder.options != null) {
                holder.options.setOnClickListener(v -> {
                    main.animation.animationButton.applyBouncingEffect(v);

                    // On ne met PAS l'action DELETE ici
                    List<String> actions = Arrays.asList(
                            popupMenu.ACTION_QUEUE,
                            popupMenu.ACTION_ADD_PLAYLIST,
                            popupMenu.ACTION_SHARE
                    );

                    // Le callback de suppression est à null car non utilisé ici
                    popupMenu.showCustomMenu(v, m, actions, null);
                });
            }
        }

        @Override
        public int getItemCount() { return songList.size(); }

        static class SongHolder extends RecyclerView.ViewHolder {
            ImageView img, options;
            TextView  title, artist;

            SongHolder(@NonNull View v) {
                super(v);
                img     = v.findViewById(R.id.song_img);
                options = v.findViewById(R.id.song_options);
                title   = v.findViewById(R.id.song_title);
                artist  = v.findViewById(R.id.song_artist);
            }
        }
    }
}