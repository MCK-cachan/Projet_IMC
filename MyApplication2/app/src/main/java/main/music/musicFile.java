package main.music;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.tonpackage.database.Musique;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Classe gérant la file de lecture (Queue) des musiques.
 * Utilise trois variables : historique, musique actuelle et file future.
 */
public class musicFile {

    private static List<Musique> history = new ArrayList<>();
    private static Musique currentMusic = null;
    private static List<Musique> futureQueue = new ArrayList<>();
    
    private static boolean shuffleEnabled = false;
    private static final Random random = new Random();

    /**
     * Retourne la file complète (Historique + Actuelle + Futur).
     */
    public static List<Musique> getQueue() {
        List<Musique> fullQueue = new ArrayList<>(history);
        if (currentMusic != null) {
            fullQueue.add(currentMusic);
        }
        fullQueue.addAll(futureQueue);
        return fullQueue;
    }

    /**
     * Définit une nouvelle file de lecture.
     */
    public static void setQueue(List<Musique> newQueue) {
        history.clear();
        futureQueue.clear();
        if (newQueue != null && !newQueue.isEmpty()) {
            currentMusic = newQueue.get(0);
            if (newQueue.size() > 1) {
                futureQueue.addAll(newQueue.subList(1, newQueue.size()));
            }
        } else {
            currentMusic = null;
        }
    }

    public static void createQueue(List<Musique> musics) {
        createQueue(musics, null);
    }

    public static void createQueue(List<Musique> musics, Musique selectedMusic) {
        if (musics == null || musics.isEmpty()) return;

        // On bascule l'actuelle dans l'historique et on vide le futur
        if (currentMusic != null) {
            history.add(currentMusic);
        }
        futureQueue.clear();

        List<Musique> listToQueue = new ArrayList<>(musics);

        if (selectedMusic != null) {
            int idx = -1;
            for (int i = 0; i < listToQueue.size(); i++) {
                if (listToQueue.get(i).musicTitle.equals(selectedMusic.musicTitle)) {
                    idx = i;
                    break;
                }
            }
            if (idx != -1) {
                List<Musique> followers = new ArrayList<>(listToQueue.subList(idx, listToQueue.size()));
                List<Musique> predecessors = new ArrayList<>(listToQueue.subList(0, idx));

                currentMusic = followers.remove(0);
                List<Musique> others = new ArrayList<>(followers);
                others.addAll(predecessors);

                if (shuffleEnabled) {
                    Collections.shuffle(others, random);
                }
                futureQueue.addAll(others);
            } else {
                if (shuffleEnabled) Collections.shuffle(listToQueue, random);
                currentMusic = listToQueue.remove(0);
                futureQueue.addAll(listToQueue);
            }
        } else {
            if (shuffleEnabled) Collections.shuffle(listToQueue, random);
            currentMusic = listToQueue.remove(0);
            futureQueue.addAll(listToQueue);
        }
    }

    /**
     * Supprime seulement les titres à venir.
     */
    public static void clearQueue() {
        futureQueue.clear();
    }

    /**
     * Ajoute une musique : supprime le futur, déplace l'actuelle en historique, 
     * et met la nouvelle en actuelle (le pointeur "avance" sur elle).
     */
    public static void addMusic(Musique music) {
        if (music == null) return;
        
        // 1. Supprime les musiques plus hautes que le pointeur (le futur)
        futureQueue.clear();
        
        // 2. L'actuelle passe en historique
        if (currentMusic != null) {
            history.add(currentMusic);
        }
        
        // 3. La nouvelle devient l'actuelle
        currentMusic = music;
    }

    /**
     * Ajoute une musique à la fin de la file d'attente future sans interrompre la lecture actuelle.
     */
    public static void addQueue(Musique music) {
        if (music == null) return;
        futureQueue.add(music);
    }

    /**
     * Mélange le futur.
     */
    public static void shuffleQueue() {
        Collections.shuffle(futureQueue, random);
    }

    public static boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    public static void setShuffleEnabled(boolean enabled) {
        shuffleEnabled = enabled;
        if (shuffleEnabled) {
            shuffleQueue();
        }
    }

    /**
     * Retourne l'index de la musique actuelle dans la file globale.
     */
    public static int getCurrentPosition() {
        return history.size();
    }

    /**
     * Repositionne le lecteur dans la file globale.
     */
    public static void setCurrentPosition(int position) {
        List<Musique> full = getQueue();
        if (position >= 0 && position < full.size()) {
            history.clear();
            futureQueue.clear();
            
            for (int i = 0; i < position; i++) {
                history.add(full.get(i));
            }
            currentMusic = full.get(position);
            for (int i = position + 1; i < full.size(); i++) {
                futureQueue.add(full.get(i));
            }
        }
    }

    public static Musique getCurrentMusic() {
        return currentMusic;
    }

    /**
     * Recule d'une position.
     */
    public static Musique backMusic(Context context) {
        if (!history.isEmpty()) {
            if (currentMusic != null) {
                futureQueue.add(0, currentMusic);
            }
            currentMusic = history.remove(history.size() - 1);
            return currentMusic;
        } else {
            if (context != null) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(context, "Début de la file", Toast.LENGTH_SHORT).show());
            }
            return null;
        }
    }

    /**
     * Avance à la musique suivante.
     */
    public static Musique nextMusic(Context context) {
        if (!futureQueue.isEmpty()) {
            if (currentMusic != null) {
                history.add(currentMusic);
            }
            currentMusic = futureQueue.remove(0);
            return currentMusic;
        } else {
            randomMusic(context);
            return null;
        }
    }

    public static Musique getMusicById(int idMusic) {
        if (currentMusic != null && currentMusic.idMusic != null && currentMusic.idMusic == idMusic) {
            return currentMusic;
        }
        for (Musique m : history) {
            if (m.idMusic != null && m.idMusic == idMusic) return m;
        }
        for (Musique m : futureQueue) {
            if (m.idMusic != null && m.idMusic == idMusic) return m;
        }
        return null;
    }

    public static void randomMusic(Context context) {
        if (context == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "fin de la file", Toast.LENGTH_SHORT).show();
        });
    }
}
