package main.music;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import main.appTool.toolbar_music;

public class MusicNotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_PLAY_PAUSE = "main.music.PLAY_PAUSE";
    public static final String ACTION_NEXT = "main.music.NEXT";
    public static final String ACTION_PREV = "main.music.PREV";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case ACTION_PLAY_PAUSE:
                toolbar_music.togglePlayPause();
                break;
            case ACTION_NEXT:
                toolbar_music.onNext();
                break;
            case ACTION_PREV:
                toolbar_music.onPrevious();
                break;
        }
    }
}
