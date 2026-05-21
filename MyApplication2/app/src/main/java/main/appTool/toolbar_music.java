package main.appTool;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.myapplication2.R;
import com.tonpackage.database.AppDatabase;
import com.tonpackage.database.Musique;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Executors;

import main.animation.animationButton;
import main.animation.animationSeekBar;
import main.animation.animationTextMusic;
import main.music.MusicService;
import main.music.musicFile;
import main.music.popupMenu;


public class toolbar_music {

    private static boolean isSoundOn = true;
    private static boolean isPlaying = true;
    private static boolean isShuffleOn = false;
    private static SeekBar currentSeekBar;
    private static TextView musicTitleView;
    private static ImageView albumImage;
    private static Activity currentActivity;
    private static Musique currentMusique;
    private static long currentDurationMs = 60000;
    private static MediaPlayer mediaPlayer;
    private static long lastPrevClickTime = 0;

    private static AudioManager audioManager;
    private static AudioManager.OnAudioFocusChangeListener focusChangeListener;
    private static AudioFocusRequest focusRequest;

    /**
     * Initialise tous les contrôles de la toolbar musique
     * @param activity L'activité où se trouve la toolbar
     */
    public static void init(Activity activity) {
        currentActivity = activity;
        audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        setupAudioFocus();

        // 1. Récupération des composants
        ImageView btnSound = activity.findViewById(R.id.btn_sound);
        ImageView btnPlayPause = activity.findViewById(R.id.btn_play_pause);
        ImageView btnShuffle = activity.findViewById(R.id.btn_shuffle);
        SeekBar seekBar = activity.findViewById(R.id.music_seekbar);
        ImageView btnNext = activity.findViewById(R.id.btn_next);
        ImageView btnPrev = activity.findViewById(R.id.btn_prev);
        ImageView btnRecord = activity.findViewById(R.id.btn_record);
        
        musicTitleView = activity.findViewById(R.id.txt_music_title);
        albumImage = activity.findViewById(R.id.img_album);
        currentSeekBar = activity.findViewById(R.id.music_seekbar);

        if (musicTitleView != null) {
            animationTextMusic.setupMarquee(musicTitleView);
        }

        // 2. Logique du bouton SON
        if (btnSound != null) {
            btnSound.setOnClickListener(v -> {
                isSoundOn = !isSoundOn;
                btnSound.setImageResource(isSoundOn ? R.drawable.ic_sound_yes : R.drawable.ic_sound_no);
                if (mediaPlayer != null) {
                    float volume = isSoundOn ? 1.0f : 0.0f;
                    mediaPlayer.setVolume(volume, volume);
                }
            });
        }

        // 3. Logique du bouton PLAY / PAUSE
        if (btnPlayPause != null) {
            btnPlayPause.setOnClickListener(v -> togglePlayPause());
        }

        // 4. Logique du bouton SHUFFLE
        if (btnShuffle != null) {
            applyGlowingEffect(btnShuffle, isShuffleOn);
            btnShuffle.setOnClickListener(v -> {
                animationButton.applyBouncingEffect(v);
                onShuffle();
            });
        }

        // 5. Logique de la SEEEKBAR
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        if (mediaPlayer != null) {
                            int seekTo = (int) ((progress / (float) seekBar.getMax()) * currentDurationMs);
                            mediaPlayer.seekTo(seekTo);
                        }
                        float percentage = (progress / (float) seekBar.getMax()) * 100;
                        float roundedPercentage = Math.round(percentage * 10f) / 10f;
                        onSeekBarMoved(roundedPercentage);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    animationSeekBar.stopAnimation();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (isPlaying()) {
                        animationSeekBar.startAnimation(seekBar, currentDurationMs, percentage -> {
                            onSeekBarMoved(percentage);
                        });
                    }
                }
            });
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                animationButton.applyBouncingEffect(v);
                onNext();
            });
        }
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                animationButton.applyBouncingEffect(v);
                onPrevious();
            });
        }

        if (btnRecord != null) {
            btnRecord.setOnClickListener(v -> {
                animationButton.applyBouncingEffect(v);
                if (currentMusique != null) {
                    popupMenu.showPlaylistSelection(currentActivity, currentMusique);
                }
            });
        }
    }

    private static void setupAudioFocus() {
        focusChangeListener = focusChange -> {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                if (isPlaying) {
                    pauseMusic();
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                // Optionnel : reprendre si on veut
                // resumeMusic();
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build();
        }
    }

    private static boolean requestAudioFocus() {
        if (audioManager == null) return false;
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result = audioManager.requestAudioFocus(focusRequest);
        } else {
            result = audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    public static void togglePlayPause() {
        if (isPlaying) {
            pauseMusic();
        } else {
            resumeMusic();
        }
    }

    private static void pauseMusic() {
        isPlaying = false;
        if (mediaPlayer != null) mediaPlayer.pause();
        animationSeekBar.stopAnimation();
        updatePlayPauseUI();
        updateNotification();
    }

    private static void resumeMusic() {
        if (requestAudioFocus()) {
            isPlaying = true;
            if (mediaPlayer != null) mediaPlayer.start();
            if (currentSeekBar != null) {
                animationSeekBar.startAnimation(currentSeekBar, currentDurationMs, percentage -> onSeekBarMoved(percentage));
            }
            updatePlayPauseUI();
            updateNotification();
        }
    }

    private static void updatePlayPauseUI() {
        if (currentActivity == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            ImageView btnPlayPause = currentActivity.findViewById(R.id.btn_play_pause);
            if (btnPlayPause != null) {
                btnPlayPause.setImageResource(isPlaying ? R.drawable.btn_pause : R.drawable.btn_play_pause);
            }
        });
    }

    /**
     * Met à jour l'affichage de la toolbar avec les infos d'une musique via son ID
     */
    public static void setMusicInfoById(int musicId) {
        if (currentActivity == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            Musique m = AppDatabase.getInstance(currentActivity).musicDao().getMusicById(musicId);
            if (m != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    updateUI(m);
                });
            }
        });
    }

    /**
     * Met à jour l'affichage de la toolbar avec un objet Musique directement
     */
    public static void updateUI(Musique m) {
        if (m == null || currentActivity == null || albumImage == null) return;
        currentMusique = m;

        // Arrêt de la musique précédente
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Récupération de la durée réelle de l'audio
        currentDurationMs = getAudioDuration(currentActivity, m.audioPath);

        // Initialisation et lancement du MediaPlayer
        playAudio(currentActivity, m.audioPath);

        // 1. Titre
        if (musicTitleView != null) {
            String fullTitle = m.musicTitle;
            if (m.artisteName != null && !m.artisteName.isEmpty()) {
                fullTitle += " - " + m.artisteName;
            }
            musicTitleView.setText(fullTitle);
            animationTextMusic.setupMarquee(musicTitleView);
        }

        // Relance de l'animation avec la nouvelle durée
        if (currentSeekBar != null) {
            currentSeekBar.setProgress(0);
            isPlaying = true; // On force la lecture au changement de titre
            updatePlayPauseUI();
            
            animationSeekBar.startAnimation(currentSeekBar, currentDurationMs, percentage -> {
                onSeekBarMoved(percentage);
            });
        }

        // 2. Image (Pochette)
        albumImage.clearColorFilter();
        albumImage.setBackground(null);
        albumImage.setPadding(0, 0, 0, 0);
        albumImage.setImageDrawable(null); 
        
        String path = m.imagePath;

        if (path != null && !path.isEmpty()) {
            if (path.startsWith("logo:")) {
                String logoName = path.substring(5);
                int resId = currentActivity.getResources().getIdentifier(logoName, "drawable", currentActivity.getPackageName());
                if (resId != 0) {
                    albumImage.setImageResource(resId);
                    albumImage.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                } else {
                    albumImage.setImageResource(R.drawable.ic_img_default);
                }
            } 
            else if (path.startsWith("/")) {
                Bitmap bmp = BitmapFactory.decodeFile(path);
                if (bmp != null) {
                    albumImage.setImageBitmap(bmp);
                } else {
                    albumImage.setImageResource(R.drawable.ic_img_default);
                }
            } 
            else if (path.startsWith("res/drawable/")) {
                String iconName = path.substring(13);
                int resId = currentActivity.getResources().getIdentifier(iconName, "drawable", currentActivity.getPackageName());
                if (resId != 0) {
                    albumImage.setImageResource(resId);
                } else {
                    albumImage.setImageResource(R.drawable.ic_img_default);
                }
            }
            else {
                String assetPath = path.replace("\\", "/");
                if (assetPath.contains("assets/")) {
                    assetPath = assetPath.substring(assetPath.indexOf("assets/") + 7);
                }
                try (InputStream is = currentActivity.getAssets().open(assetPath)) {
                    Bitmap bmp = BitmapFactory.decodeStream(is);
                    albumImage.setImageBitmap(bmp);
                } catch (IOException e) {
                    albumImage.setImageResource(R.drawable.ic_img_default);
                }
            }
        } else {
            albumImage.setImageResource(R.drawable.ic_img_default);
        }
        
        if (albumImage.getDrawable() == null) {
            albumImage.setImageResource(R.drawable.ic_img_default);
        }
        updateNotification();
    }

    private static void updateNotification() {
        if (currentActivity == null) return;
        Intent serviceIntent = new Intent(currentActivity, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentActivity.startForegroundService(serviceIntent);
        } else {
            currentActivity.startService(serviceIntent);
        }
    }

    private static void playAudio(Context context, String path) {
        if (path == null || path.isEmpty()) return;
        
        if (!requestAudioFocus()) return;

        String assetPath = path.replace("\\", "/");
        if (assetPath.contains("assets/")) {
            assetPath = assetPath.substring(assetPath.indexOf("assets/") + 7);
        }

        mediaPlayer = new MediaPlayer();
        try (AssetFileDescriptor afd = context.getAssets().openFd(assetPath)) {
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
            float volume = isSoundOn ? 1.0f : 0.0f;
            mediaPlayer.setVolume(volume, volume);
            mediaPlayer.start();

            // Lancement automatique de la suivante à la fin
            mediaPlayer.setOnCompletionListener(mp -> onNext());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Récupère la durée d'un fichier audio dans les assets
     */
    private static long getAudioDuration(Context context, String path) {
        if (path == null || path.isEmpty()) return 60000;
        
        String assetPath = path.replace("\\", "/");
        if (assetPath.contains("assets/")) {
            assetPath = assetPath.substring(assetPath.indexOf("assets/") + 7);
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try (AssetFileDescriptor afd = context.getAssets().openFd(assetPath)) {
            retriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return time != null ? Long.parseLong(time) : 60000;
        } catch (Exception e) {
            e.printStackTrace();
            return 60000;
        } finally {
            try {
                retriever.release();
            } catch (IOException ignored) {}
        }
    }

    public static void onNext() {
        Musique next = musicFile.nextMusic(currentActivity);
        if (next != null) {
            updateUI(next);
        } else {
            // Fin de la file : on arrête la lecture visuellement
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            isPlaying = false;
            updatePlayPauseUI();
            animationSeekBar.stopAnimation();
            if (currentSeekBar != null) currentSeekBar.setProgress(0);
            
            // On peut aussi arrêter le service notification ici
            if (currentActivity != null) {
                currentActivity.stopService(new Intent(currentActivity, MusicService.class));
            }
            if (audioManager != null && focusChangeListener != null) {
                audioManager.abandonAudioFocus(focusChangeListener);
            }
        }
    }

    public static void onPrevious() {
        if (currentActivity == null) return;
        
        long currentTime = System.currentTimeMillis();
        int progress = (currentSeekBar != null) ? currentSeekBar.getProgress() : 0;
        
        // Seuil pour considérer qu'on est au début du morceau (ex: < 3%)
        boolean nearStart = progress < 3;

        // Si double clic (< 1s) OU si on est au tout début du morceau
        if ((currentTime - lastPrevClickTime < 1000) || nearStart) {
            Musique prev = musicFile.backMusic(currentActivity);
            if (prev != null) {
                updateUI(prev);
            } else {
                // Déjà au début de la file, on redémarre juste le titre
                restartCurrentSong();
            }
            lastPrevClickTime = 0; // Reset pour éviter les triples clics
        } else {
            // Premier clic ou clic espacé : on redémarre juste le morceau actuel
            restartCurrentSong();
            lastPrevClickTime = currentTime;
        }
    }

    private static void restartCurrentSong() {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            if (isPlaying && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        }
        if (currentSeekBar != null) {
            currentSeekBar.setProgress(0);
            if (isPlaying) {
                animationSeekBar.startAnimation(currentSeekBar, currentDurationMs, percentage -> {
                    onSeekBarMoved(percentage);
                });
            }
        }
    }

    private static void onShuffle() {
        isShuffleOn = !isShuffleOn;
        ImageView btnShuffle = currentActivity.findViewById(R.id.btn_shuffle);
        if (btnShuffle != null) {
            applyGlowingEffect(btnShuffle, isShuffleOn);
        }
        musicFile.setShuffleEnabled(isShuffleOn);
    }
    
    public static boolean isSoundActive() { return isSoundOn; }
    public static boolean isPlaying() { return isPlaying; }
    public static boolean isShuffleActive() { return isShuffleOn; }

    private static void onSeekBarMoved(float percent) {}

    private static void applyGlowingEffect(ImageView img, boolean active) {
        if (active) {
            img.setAlpha(1.0f);
            img.setBackgroundResource(R.drawable.glow_background_shufle);
            img.setColorFilter(Color.WHITE);
            img.setScaleX(1.1f);
            img.setScaleY(1.1f);
        } else {
            img.setAlpha(1.0f);
            img.setBackground(null);
            img.clearColorFilter();
            img.setScaleX(1.0f);
            img.setScaleY(1.0f);
        }
    }
}
