package main.music;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication2.R;
import com.tonpackage.database.Artiste;
import com.tonpackage.database.Musique;
import com.tonpackage.database.Playlist;

import java.io.File;
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

public class MusicModule {

    private final View moduleView;
    private final BoxAdapter boxAdapter;

    public MusicModule(Activity activity, LinearLayout container, String title) {
        this(activity, container, title, true);
    }

    public MusicModule(Activity activity, LinearLayout container, String title, boolean showText) {
        moduleView = LayoutInflater.from(activity)
                .inflate(R.layout.music_home_module, container, false);

        TextView titleView = moduleView.findViewById(R.id.module_title);
        if (titleView != null) titleView.setText(title);

        boxAdapter = new BoxAdapter(showText);
        ViewPager2 pager = moduleView.findViewById(R.id.viewPagerGrid);
        if (pager != null) {
            pager.setAdapter(boxAdapter);
            pager.setClipChildren(false);
            pager.setClipToPadding(false);
            if (pager.getChildAt(0) instanceof ViewGroup) {
                ((ViewGroup) pager.getChildAt(0)).setClipChildren(false);
            }
        }

        container.addView(moduleView);
    }

    public void addBoxes(List<?> items) {
        if (items == null || items.isEmpty()) return;
        for (Object item : items) boxAdapter.addItem(item);

        int total = boxAdapter.getTotalItems();
        int pageCount = (int) Math.ceil(total / 9.0);
        boxAdapter.setPageCount(pageCount);
        boxAdapter.notifyDataSetChanged();

        ViewPager2 pager = moduleView.findViewById(R.id.viewPagerGrid);
        LinearLayout dots = moduleView.findViewById(R.id.dots_indicator);
        if (pager != null && dots != null) {
            module.setupLogic((Activity) moduleView.getContext(), pager, dots, pageCount);
        }
    }

    private static class BoxAdapter extends RecyclerView.Adapter<BoxAdapter.PageHolder> {
        private final List<Object> allItems = new ArrayList<>();
        private int pageCount = 1;
        private final boolean showText;

        BoxAdapter(boolean showText) {
            this.showText = showText;
        }

        void addItem(Object item) { allItems.add(item); }
        void setPageCount(int count) { pageCount = Math.max(count, 1); }
        int getTotalItems() { return allItems.size(); }

        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            RecyclerView grid = new RecyclerView(parent.getContext());
            grid.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            float dp = parent.getContext().getResources().getDisplayMetrics().density;
            grid.setPadding((int)(8*dp), (int)(8*dp), (int)(8*dp), (int)(8*dp));
            grid.setClipToPadding(false);
            grid.setClipChildren(false);

            grid.setLayoutManager(new GridLayoutManager(parent.getContext(), 3) {
                @Override public boolean canScrollVertically() { return false; }
                @Override public boolean canScrollHorizontally() { return false; }
            });
            PageItemAdapter inner = new PageItemAdapter(new ArrayList<>(), showText);
            grid.setAdapter(inner);
            return new PageHolder(grid, inner);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            int start = position * 9;
            int end = Math.min(start + 9, allItems.size());
            List<Object> page = start < allItems.size()
                    ? new ArrayList<>(allItems.subList(start, end))
                    : new ArrayList<>();
            holder.inner.setItems(page);
        }

        @Override
        public int getItemCount() { return pageCount; }

        static class PageHolder extends RecyclerView.ViewHolder {
            final PageItemAdapter inner;
            PageHolder(@NonNull RecyclerView grid, PageItemAdapter inner) {
                super(grid);
                this.inner = inner;
            }
        }
    }

    private static class PageItemAdapter extends RecyclerView.Adapter<PageItemAdapter.BoxHolder> {
        private final List<Object> items;
        private final boolean showText;

        PageItemAdapter(List<Object> items, boolean showText) { 
            this.items = items; 
            this.showText = showText;
        }

        void setItems(List<Object> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BoxHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_box_item, parent, false);
            int screenWidth = parent.getContext().getResources().getDisplayMetrics().widthPixels;
            int size = screenWidth / 4; 
            
            ImageView img = v.findViewById(R.id.box_image);
            if (img != null) {
                img.getLayoutParams().width = size;
                img.getLayoutParams().height = size;
            }

            TextView txt = v.findViewById(R.id.box_text);
            if (txt != null) {
                txt.setVisibility(showText ? View.VISIBLE : View.GONE);
            }

            return new BoxHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BoxHolder holder, int position) {
            Object item = items.get(position);

            holder.itemView.setOnLongClickListener(v -> {
                if (item instanceof Musique) {
                    List<String> actions = Arrays.asList(
                            popupMenu.ACTION_PLAY,
                            popupMenu.ACTION_QUEUE,
                            popupMenu.ACTION_ADD_PLAYLIST,
                            popupMenu.ACTION_SHARE
                    );
                    popupMenu.showCustomMenu(v, (Musique) item, actions, null);
                    return true;
                } else if (item instanceof Playlist) {
                    List<String> actions = Arrays.asList(
                            popupMenu.ACTION_PLAY,
                            popupMenu.ACTION_SHARE
                    );
                    popupMenu.showPlaylistMenu(v, (Playlist) item, actions, null);
                    return true;
                }
                return false;
            });

            String path = "";
            String name = "";
            Integer color = null;

            if (item instanceof Playlist) {
                path = ((Playlist) item).imagePath;
                name = ((Playlist) item).Name;
                color = ((Playlist) item).selectedColor;
            } else if (item instanceof Musique) {
                path = ((Musique) item).imagePath;
                name = ((Musique) item).musicTitle;
            } else if (item instanceof Artiste) {
                path = ((Artiste) item).imagePath;
                name = ((Artiste) item).Pseudo;
            }

            if (holder.txtName != null) {
                holder.txtName.setText(name);
            }
            loadBoxImg(holder.img, path, color);

            holder.itemView.setOnClickListener(v -> {
                main.animation.animationButton.applyBouncingEffect(v);
                
                // Masquer le clavier si actif
                InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }

                if (item instanceof Playlist) {
                    Playlist p = (Playlist) item;
                    PlaylistManage.openPlaylistDetail((Activity) v.getContext(), p);
                } else if (item instanceof Artiste) {
                    ArtisteManage.openArtisteDetail((Activity) v.getContext(), (Artiste) item);
                } else if (item instanceof Musique) {
                    Musique m = (Musique) item;
                    if (m.idMusic != null && m.idMusic > 0) {
                        // Requete au serveur pour avoir les infos completes et lancer
                        serveur.getTrack(v.getContext(), m.idMusic, new serveur.SearchCallback() {
                            @Override
                            public void onResult(List<?> results) {
                                if (!results.isEmpty()) {
                                    Musique full = (Musique) results.get(0);
                                    musicFile.addMusic(full);
                                    toolbar_music.updateUI(full);
                                } else {
                                    // Fallback
                                    musicFile.addMusic(m);
                                    toolbar_music.updateUI(m);
                                }
                            }

                            @Override
                            public void onError(String message) {
                                //Toast.makeText(v.getContext(), "Erreur: " + message, Toast.LENGTH_SHORT).show();
                                // Fallback
                                musicFile.addMusic(m);
                                toolbar_music.updateUI(m);
                            }
                        });
                    } else {
                        musicFile.addMusic(m);
                        toolbar_music.updateUI(m);
                    }
                }
            });
        }

        private void loadBoxImg(ImageView img, String path, Integer color) {
            if (img == null) return;
            img.clearColorFilter();
            img.setBackground(null);
            img.setPadding(0, 0, 0, 0);
            img.setTag(path); // Pour éviter les problèmes de recyclage avec l'asynchrone

            if (path == null || path.isEmpty()) {
                img.setImageResource(R.drawable.music_bloc_like);
                return;
            }

            if (path.startsWith("http")) {
                img.setImageResource(R.drawable.music_bloc_like);
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        URL url = new URL(path);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap myBitmap = BitmapFactory.decodeStream(input);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (path.equals(img.getTag())) {
                                img.setImageBitmap(myBitmap);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                return;
            }

            if (path.startsWith("logo:")) {
                String logoName = path.substring(5);
                int resId = img.getContext().getResources().getIdentifier(logoName, "drawable", img.getContext().getPackageName());
                if (resId != 0) {
                    img.setImageResource(resId);
                    if (color != null) {
                        int lightColor = adjustAlpha(color, 0.2f);
                        GradientDrawable gd = new GradientDrawable();
                        gd.setColor(lightColor);
                        gd.setCornerRadius(20);
                        img.setBackground(gd);
                        int p = (int)(15 * img.getContext().getResources().getDisplayMetrics().density);
                        img.setPadding(p, p, p, p);
                        img.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    }
                    return;
                }
            }

            if (path.startsWith("/")) {
                img.setImageURI(Uri.fromFile(new File(path)));
                return;
            }

            if (path.startsWith("res/drawable/")) {
                String iconName = path.substring(13);
                int resId = img.getContext().getResources().getIdentifier(iconName, "drawable", img.getContext().getPackageName());
                if (resId != 0) {
                    img.setImageResource(resId);
                    return;
                }
            }

            String assetPath = path.replace("\\", "/");
            if (assetPath.contains("assets/")) {
                assetPath = assetPath.substring(assetPath.indexOf("assets/") + 7);
            }
            
            try (InputStream is = img.getContext().getAssets().open(assetPath)) {
                Bitmap bmp = BitmapFactory.decodeStream(is);
                img.setImageBitmap(bmp);
            } catch (IOException e) {
                img.setImageResource(R.drawable.music_bloc_like);
            }
        }

        private int adjustAlpha(int color, float factor) {
            int alpha = Math.round(Color.alpha(color) * factor);
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            return Color.argb(alpha, red, green, blue);
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class BoxHolder extends RecyclerView.ViewHolder {
            final ImageView img;
            final TextView txtName;
            BoxHolder(@NonNull View v) {
                super(v);
                this.img = v.findViewById(R.id.box_image);
                this.txtName = v.findViewById(R.id.box_text);
            }
        }
    }
}
