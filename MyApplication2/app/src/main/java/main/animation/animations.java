package main.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;

public class animations {

    private static boolean hasChanged = false;

    public static void init(Activity activity) {
        if (!hasChanged) return;

        hasChanged = false;
        activity.getWindow().getDecorView().post(() -> {
            startCircularReveal(activity);
        });
    }

    public static void triggerThemeChanged() {
        hasChanged = true;
    }
    private static void startCircularReveal(Activity activity) {

        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;

        final View rootLayout = content.getChildAt(0);

        rootLayout.post(() -> {

            int cx = rootLayout.getWidth() / 2;
            int cy = 0;

            float finalRadius = (float) Math.hypot(
                    rootLayout.getWidth(),
                    rootLayout.getHeight()
            );

            Animator anim = ViewAnimationUtils.createCircularReveal(
                    rootLayout, cx, cy, 0f, finalRadius
            );

            anim.setDuration(400);

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    rootLayout.requestLayout();
                    rootLayout.invalidate();
                }
            });

            anim.start();
        });
    }
}
