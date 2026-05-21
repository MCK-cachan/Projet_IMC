package main.music;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication2.R;
import com.tonpackage.database.AppDatabase;
import com.tonpackage.database.Artiste;
import com.tonpackage.database.Playlist;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class PlaylistPage {

    public static void loadPlaylistAndPodcasts(Activity activity) {
        LinearLayout container = activity.findViewById(R.id.dynamic_modules_container);
        if (container == null) return;
        container.removeAllViews();

        Context context = activity;

        // --- BOUTON AJOUTER PLAYLIST (STYLE CAPSULE) ---
        Button btnAdd = new Button(context);
        btnAdd.setText("+ Créer une playlist");
        btnAdd.setBackgroundResource(R.drawable.button_capsule);
        btnAdd.setTextColor(Color.WHITE);
        btnAdd.setAllCaps(false);
        btnAdd.setPadding(40, 0, 40, 0);

        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (int) (45 * context.getResources().getDisplayMetrics().density));
        lpBtn.setMargins(30, 20, 30, 10);
        btnAdd.setLayoutParams(lpBtn);

        btnAdd.setOnClickListener(v -> {
            main.animation.animationButton.applyBouncingEffect(v);
            PlaylistAdd.showAddPlaylistDialog(activity, () -> {
                loadPlaylistAndPodcasts(activity);
            });
        });
        container.addView(btnAdd);

        addSectionTitle(container, "Mes Playlists");
        RecyclerView rvPlaylists = createRecyclerView(context);
        container.addView(rvPlaylists);
        LineAdapter playlistAdapter = new LineAdapter(new ArrayList<>(), true);
        rvPlaylists.setAdapter(playlistAdapter);

        addSectionTitle(container, "Artistes");
        RecyclerView rvArtistes = createRecyclerView(context);
        container.addView(rvArtistes);
        LineAdapter artisteAdapter = new LineAdapter(new ArrayList<>(), false);
        rvArtistes.setAdapter(artisteAdapter);

        addSectionTitle(container, "Podcasteurs");
        RecyclerView rvPodcasts = createRecyclerView(context);
        container.addView(rvPodcasts);
        LineAdapter podcastAdapter = new LineAdapter(new ArrayList<>(), false);
        rvPodcasts.setAdapter(podcastAdapter);

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(activity);
            List<Playlist> playlists = db.musicDao().getAllPlaylists();
            List<Artiste> artistes = db.musicDao().getMusicians(); 
            List<Artiste> podcasters = db.musicDao().getPodcasters();

            new Handler(Looper.getMainLooper()).post(() -> {
                playlistAdapter.setItems(playlists);
                artisteAdapter.setItems(artistes);
                podcastAdapter.setItems(podcasters);
            });
        });
    }

    private static void addSectionTitle(LinearLayout container, String title) {
        TextView textView = new TextView(container.getContext());
        textView.setText(title);
        textView.setTextSize(20);
        textView.setPadding(30, 40, 30, 10);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        container.addView(textView);
    }

    private static RecyclerView createRecyclerView(Context context) {
        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setNestedScrollingEnabled(false);
        return recyclerView;
    }

    private static class LineAdapter extends RecyclerView.Adapter<LineAdapter.LineHolder> {
        private final List<Object> items = new ArrayList<>();
        private final boolean isPlaylistSection;

        LineAdapter(List<Object> items, boolean isPlaylistSection) {
            this.items.addAll(items);
            this.isPlaylistSection = isPlaylistSection;
        }

        void setItems(List<?> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public LineHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_song_line, parent, false);
            return new LineHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull LineHolder holder, int position) {
            Object item = items.get(position);
            String name = "";
            String sub = "";
            String path = "";
            Integer color = null;

            if (item instanceof Playlist) {
                Playlist p = (Playlist) item;
                name = p.Name;
                sub = "Playlist";
                path = p.imagePath;
                color = p.selectedColor;
            } else if (item instanceof Artiste) {
                Artiste a = (Artiste) item;
                name = a.Pseudo;
                sub = a.isPodcast ? "Podcast" : "Artiste";
                path = a.imagePath;
            }

            holder.title.setText(name);
            holder.subtitle.setText(sub);

            loadImg(holder.img, path, color);

            holder.itemView.setOnClickListener(v -> {
                if (item instanceof Playlist) {
                    PlaylistManage.openPlaylistDetail((Activity) v.getContext(), (Playlist) item);
                } else if (item instanceof Artiste) {
                    ArtisteManage.openArtisteDetail((Activity) v.getContext(), (Artiste) item);
                }
            });

            holder.options.setOnClickListener(v -> {
                main.animation.animationButton.applyBouncingEffect(v);
                if (item instanceof Playlist) {
                    Playlist p = (Playlist) item;
                    List<String> actions = new ArrayList<>(Arrays.asList(
                            popupMenu.ACTION_PLAY,
                            popupMenu.ACTION_SHARE
                    ));
                    if (!"Like".equalsIgnoreCase(p.Name)) {
                        actions.add(popupMenu.ACTION_DELETE);
                    }
                    popupMenu.showPlaylistMenu(v, p, actions, () -> {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            items.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    });
                } else if (item instanceof Artiste) {
                    Artiste a = (Artiste) item;
                    List<String> actions = new ArrayList<>(Arrays.asList(
                            popupMenu.ACTION_PLAY,
                            popupMenu.ACTION_SHARE,
                            popupMenu.ACTION_DELETE
                    ));
                    popupMenu.showArtisteMenu(v, a, actions, () -> {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            items.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    });
                }
            });
        }

        private void loadImg(ImageView img, String path, Integer color) {
            img.clearColorFilter();
            if (path != null && !path.isEmpty()) {
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

                String assetPath = normalizeAssetPath(path);
                try (InputStream is = img.getContext().getAssets().open(assetPath)) {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    img.setImageBitmap(bmp);
                    return;
                } catch (IOException ignored) {}
            }
            img.setImageResource(R.drawable.music_bloc_like);
        }

        @Override
        public int getItemCount() { return items.size(); }

        private String normalizeAssetPath(String raw) {
            if (raw == null) return "";
            String path = raw.replace("\\", "/");
            int idx = path.indexOf("assets/");
            return (idx >= 0) ? path.substring(idx + 7) : path;
        }

        static class LineHolder extends RecyclerView.ViewHolder {
            ImageView img, options;
            TextView title, subtitle;
            LineHolder(View v) {
                super(v);
                img = v.findViewById(R.id.song_img);
                options = v.findViewById(R.id.song_options);
                title = v.findViewById(R.id.song_title);
                subtitle = v.findViewById(R.id.song_artist);
            }
        }
    }
}
