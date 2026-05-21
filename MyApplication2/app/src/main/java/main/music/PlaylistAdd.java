package main.music;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.myapplication2.R;
import com.tonpackage.database.AppDatabase;
import com.tonpackage.database.Playlist;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class PlaylistAdd {

    private static final int PICK_IMAGE_REQUEST = 1001;
    private static String currentImagePath = "baseline_laptop_24"; 
    private static int selectedColor = 0xFF607D8B;
    private static ImageView previewImage;
    private static Uri selectedUri;
    private static String selectedLogoName = "baseline_laptop_24";

    public static void showAddPlaylistDialog(Activity activity, Runnable callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_add_playlist, null);
        builder.setView(view);

        EditText editName = view.findViewById(R.id.edit_playlist_name);
        previewImage = view.findViewById(R.id.img_preview);
        GridLayout gridIcons = view.findViewById(R.id.grid_icons);
        GridLayout gridColors = view.findViewById(R.id.grid_colors);
        View btnChooseImg = view.findViewById(R.id.btn_choose_image);
        View btnCreate = view.findViewById(R.id.btn_create_playlist);
        ImageView btnClose = view.findViewById(R.id.btn_close_dialog);

        AlertDialog dialog = builder.create();

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        setupLogos(activity, gridIcons);
        setupColors(gridColors);

        // Initialisation de l'état pour un nouveau dialogue
        selectedUri = null;
        selectedLogoName = "baseline_laptop_24";
        currentImagePath = "baseline_laptop_24";
        updatePreview(activity);

        btnChooseImg.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            activity.startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        btnCreate.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(activity, "Veuillez entrer un nom", Toast.LENGTH_SHORT).show();
                return;
            }
            saveFinal(activity, name, callback);
            dialog.dismiss();
        });

        dialog.show();
    }

    private static void updatePreview(Context context) {
        if (previewImage == null) return;
        
        // Reset style
        previewImage.setBackground(null);
        previewImage.setPadding(0, 0, 0, 0);
        previewImage.clearColorFilter();

        if (selectedUri != null) {
            previewImage.setImageURI(selectedUri);
            previewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            int resId = context.getResources().getIdentifier(selectedLogoName, "drawable", context.getPackageName());
            if (resId != 0) {
                previewImage.setImageResource(resId);
                previewImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
                
                // On applique le style "Box" comme dans les modules
                int lightColor = adjustAlpha(selectedColor, 0.2f);
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(lightColor);
                gd.setCornerRadius(32);
                previewImage.setBackground(gd);
                
                // Réduction drastique du padding pour agrandir le logo interne (de 25dp à 5dp)
                int p = (int)(5 * context.getResources().getDisplayMetrics().density);
                previewImage.setPadding(p, p, p, p);
                
                previewImage.setColorFilter(selectedColor, PorterDuff.Mode.SRC_IN);
            }
        }
    }

    public static void handleImageResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedUri = data.getData();
            selectedLogoName = null;
            if (previewImage != null) {
                previewImage.setImageURI(selectedUri);
                previewImage.clearColorFilter();
                previewImage.setBackground(null);
                previewImage.setPadding(0, 0, 0, 0);
                previewImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        }
    }

    private static void setupLogos(Context context, GridLayout grid) {
        grid.removeAllViews();
        
        List<String> logos = new ArrayList<>();
        Field[] fields = R.drawable.class.getFields();
        for (Field field : fields) {
            try {
                String name = field.getName();
                if (name.startsWith("baseline_")) {
                    logos.add(name);
                }
            } catch (Exception ignored) {}
        }

        if (logos.isEmpty()) {
            logos.add("baseline_laptop_24");
            logos.add("baseline_language_24");
        }

        float density = context.getResources().getDisplayMetrics().density;
        int iconSize = (int) (60 * density); // Utilisation de DP pour la taille des icônes de la grille
        int padding = (int) (10 * density);

        for (String logoName : logos) {
            int resId = context.getResources().getIdentifier(logoName, "drawable", context.getPackageName());
            if (resId != 0) {
                ImageView img = new ImageView(context);
                img.setImageResource(resId);
                img.setColorFilter(selectedColor, PorterDuff.Mode.SRC_IN);
                
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = iconSize;
                lp.height = iconSize;
                lp.setMargins((int)(5*density), (int)(5*density), (int)(5*density), (int)(5*density));
                img.setLayoutParams(lp);
                img.setPadding(padding, padding, padding, padding);
                img.setAlpha(0.6f);

                img.setOnClickListener(v -> {
                    selectedUri = null;
                    selectedLogoName = logoName;
                    currentImagePath = logoName;
                    updatePreview(context);
                    highlightSelection(grid, v);
                });
                grid.addView(img);
            }
        }
    }

    private static void setupColors(GridLayout grid) {
        grid.removeAllViews();
        int[] colors = {0xFF607D8B, 0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 
                        0xFF2196F3, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFFFC107};

        float density = grid.getContext().getResources().getDisplayMetrics().density;
        int colorSize = (int) (40 * density);

        for (int color : colors) {
            View v = new View(grid.getContext());
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = colorSize;
            lp.height = colorSize;
            lp.setMargins((int)(5*density), (int)(5*density), (int)(5*density), (int)(5*density));
            v.setLayoutParams(lp);
            v.setBackgroundColor(color);
            
            v.setOnClickListener(view -> {
                selectedColor = color;
                refreshLogosColor(grid.getRootView());
                updatePreview(grid.getContext());
                highlightSelection(grid, view);
            });
            grid.addView(v);
        }
    }

    private static void refreshLogosColor(View root) {
        GridLayout gridIcons = root.findViewById(R.id.grid_icons);
        if (gridIcons != null) {
            for (int i = 0; i < gridIcons.getChildCount(); i++) {
                View child = gridIcons.getChildAt(i);
                if (child instanceof ImageView) {
                    ((ImageView) child).setColorFilter(selectedColor, PorterDuff.Mode.SRC_IN);
                }
            }
        }
    }

    private static void highlightSelection(GridLayout grid, View selectedView) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            child.setAlpha(child == selectedView ? 1.0f : 0.6f);
        }
    }

    private static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private static void saveFinal(Context context, String name, Runnable callback) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String finalPath;
            Integer finalColor = null;

            if (selectedUri != null) {
                File dir = new File(context.getFilesDir(), "playlist_images");
                if (!dir.exists()) dir.mkdirs();
                String fileName = "playlist_" + System.currentTimeMillis() + ".jpg";
                File file = new File(dir, fileName);
                try (InputStream is = context.getContentResolver().openInputStream(selectedUri);
                     FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = is.read(buffer)) != -1) fos.write(buffer, 0, read);
                    finalPath = file.getAbsolutePath();
                } catch (IOException e) {
                    e.printStackTrace();
                    finalPath = "baseline_laptop_24";
                }
            } else {
                finalPath = "logo:" + currentImagePath;
                finalColor = selectedColor;
            }

            Playlist p = new Playlist();
            p.Name = name;
            p.imagePath = finalPath;
            p.selectedColor = finalColor;

            AppDatabase.getInstance(context).musicDao().insertPlaylist(p);

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, context.getString(R.string.playlist_created), Toast.LENGTH_SHORT).show();
                if (callback != null) callback.run();
            });
        });
    }
}
