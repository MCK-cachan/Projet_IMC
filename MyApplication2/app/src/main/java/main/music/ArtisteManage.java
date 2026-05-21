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
import com.tonpackage.database.Artiste;
import com.tonpackage.database.Musique;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import main.appTool.toolbar_music;
import main.serveur;

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

        SongListAdapter adapter = new SongListAdapter();
        if (rvSongs != null) {
            rvSongs.setLayoutManager(new LinearLayoutManager(activity));
            rvSongs.setAdapter(adapter);
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

        loadData(activity, artiste, adapter, artistSongs);

        dynamicContainer.addView(detailView);
    }

    private static void loadData(Activity activity, Artiste artiste, SongListAdapter adapter, List<Musique> artistSongs) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Musique> songs = AppDatabase.getInstance(activity)
                    .musicDao()
                    .getMusicsByArtiste(artiste.idArtiste);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (songs != null && !songs.isEmpty()) {
                    adapter.setSongs(songs);
                    artistSongs.clear();
                    artistSongs.addAll(songs);
                } else {
                    // Tenter de charger depuis le serveur
                    serveur.getArtistTracks(activity, artiste.idArtiste, new serveur.SearchCallback() {
                        @Override
                        public void onResult(List<?> results) {
                            List<Musique> serverSongs = (List<Musique>) results;
                            adapter.setSongs(serverSongs);
                            artistSongs.clear();
                            artistSongs.addAll(serverSongs);
                        }

                        @Override
                        public void onError(String message) {
                            // Erreur ou pas de résultats
                        }
                    });
                }
            });
        });
    }

    private static void loadImage(ImageView img, String path) {
        if (img == null) return;
        img.clearColorFilter();
        img.setTag(path);

        if (path == null || path.isEmpty()) {
            img.setImageResource(R.drawable.music_bloc_like);
            return;
        }

        if (path.startsWith("http")) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    URL url = new URL(path);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap myBitmap = BitmapFactory.decodeStream(input);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (path.equals(img.getTag())) {
                            img.setImageBitmap(myBitmap);
                        }
                    });
                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() -> img.setImageResource(R.drawable.music_bloc_like));
                }
            });
            return;
        }

        if (path.startsWith("logo:")) {
            String logoName = path.substring(5);
            int resId = img.getContext().getResources().getIdentifier(logoName, "drawable", img.getContext().getPackageName());
            if (resId != 0) {
                img.setImageResource(resId);
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

            if (holder.options != null) {
                holder.options.setOnClickListener(v -> {
                    main.animation.animationButton.applyBouncingEffect(v);
                    List<String> actions = Arrays.asList(
                            popupMenu.ACTION_QUEUE,
                            popupMenu.ACTION_ADD_PLAYLIST,
                            popupMenu.ACTION_SHARE
                    );
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
