package main.music;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication2.R;

public class module {

    /**
     * Configure la mécanique du module (Swipe + Points)
     */
    public static void setupLogic(Activity activity, ViewPager2 viewPager, LinearLayout dotsLayout, int pageCount) {
        if (viewPager == null || dotsLayout == null) return;

        // Si une seule page, on cache tout et on arrête
        if (pageCount <= 1) {
            dotsLayout.setVisibility(View.GONE);
            return;
        }

        dotsLayout.setVisibility(View.VISIBLE);
        dotsLayout.removeAllViews();

        // On crée les points immédiatement
        float density = activity.getResources().getDisplayMetrics().density;
        int size = (int) (8 * density);

        for (int i = 0; i < pageCount; i++) {
            ImageView dot = new ImageView(activity);
            dot.setImageResource(R.drawable.dot_indicator);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins((int) (6 * density), 0, (int) (6 * density), 0);
            dot.setLayoutParams(params);

            dot.setAlpha(i == 0 ? 1.0f : 0.3f);
            dotsLayout.addView(dot);
        }

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // On vérifie le nombre réel de vues présentes
                int childCount = dotsLayout.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View d = dotsLayout.getChildAt(i);
                    if (d != null) {
                        d.animate()
                                .alpha(i == position ? 1.0f : 0.3f)
                                .scaleX(i == position ? 1.2f : 1.0f)
                                .scaleY(i == position ? 1.2f : 1.0f)
                                .setDuration(200)
                                .start();
                    }
                }
            }
        });
    }
}