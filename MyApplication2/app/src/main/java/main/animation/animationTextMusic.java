package main.animation;

import android.text.TextUtils;
import android.widget.TextView;

public class animationTextMusic {

    /**
     * Active le défilement horizontal (Marquee) si le texte est trop long
     * @param textView Le titre de la musique
     */
    public static void setupMarquee(TextView textView) {
        if (textView == null) return;

        // 1. On force le texte sur une seule ligne
        textView.setSingleLine(true);

        // 2. On définit le type de coupure : Marquee (défilement)
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);

        // 3. Nombre de répétitions ( -1 pour l'infini)
        textView.setMarqueeRepeatLimit(-1);

        // 4. TRÈS IMPORTANT : Le défilement ne s'active que si la vue a le focus
        textView.setSelected(true);
    }
}