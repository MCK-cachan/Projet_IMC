package main.animation;

import android.view.View;
import android.view.animation.OvershootInterpolator;

public class animationButton {

    /**
     * Applique une animation de rebond (scale) sur n'importe quelle vue.
     * @param view L'élément à animer (Button, ImageView, FrameLayout, etc.)
     */

    public static void applyBouncingEffect(View view) {
        if (view == null) return;
        view.animate().cancel();
        // On réduit la taille à 85%
        view.animate()
                .scaleX(0.75f)
                .scaleY(0.75f)
                .setDuration(100)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> {
                    // On revient à la taille normale (100%) avec un léger rebond
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .setInterpolator(new OvershootInterpolator())
                            .start();
                })
                .start();
    }
}