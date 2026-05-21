package main.animation;

import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;

public class animationSeekBar {

    private static ValueAnimator animator;

    /**
     * Lance l'animation de la SeekBar
     * @param seekBar La vue à animer
     * @param durationMs Durée totale pour faire 0 à 100%
     * @param callback Une interface pour renvoyer le pourcentage précis
     */
    public static void startAnimation(SeekBar seekBar, long durationMs, OnProgressUpdateListener callback) {
        if (seekBar == null) return;

        int startProgress = seekBar.getProgress();
        int maxProgress = seekBar.getMax();

        // Calcul du temps restant pour finir la barre (si elle est déjà entamée)
        long remainingDuration = (long) (durationMs * (1 - (startProgress / (float) maxProgress)));

        stopAnimation(); // On arrête l'ancienne animation si elle existe

        animator = ValueAnimator.ofInt(startProgress, maxProgress);
        animator.setDuration(remainingDuration);
        animator.setInterpolator(new LinearInterpolator()); // Vitesse constante

        animator.addUpdateListener(animation -> {
            int currentVal = (int) animation.getAnimatedValue();
            seekBar.setProgress(currentVal);

            // Calcul du pourcentage à 0.1 près
            float percentage = (currentVal / (float) maxProgress) * 100;
            float rounded = Math.round(percentage * 10f) / 10f;

            if (callback != null) {
                callback.onUpdate(rounded);
            }
        });

        animator.start();
    }

    /**
     * Stoppe l'animation en cours (Pause)
     */
    public static void stopAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    /**
     * Interface pour récupérer le pourcentage dans la toolbar
     */
    public interface OnProgressUpdateListener {
        void onUpdate(float percentage);
    }
}