package main.music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.myapplication2.R;
import com.tonpackage.database.Musique;

import java.io.IOException;
import java.io.InputStream;

import main.MainActivity;
import main.appTool.toolbar_music;

public class MusicService extends Service {

    public static final String CHANNEL_ID = "MusicChannel";
    public static final int NOTIFICATION_ID = 1;
    public static final String ACTION_UPDATE = "UPDATE";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_UPDATE.equals(intent.getAction())) {
            showNotification();
        }
        return START_NOT_STICKY;
    }

    private void showNotification() {
        Musique current = musicFile.getCurrentMusic();
        if (current == null) return;

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.noty_bar);
        remoteViews.setTextViewText(R.id.noty_txt_title, current.musicTitle);
        remoteViews.setTextViewText(R.id.noty_txt_artist, current.artisteName != null ? current.artisteName : "Inconnu");

        setAlbumImage(remoteViews, current.imagePath);

        boolean isPlaying = toolbar_music.isPlaying();
        remoteViews.setImageViewResource(R.id.noty_btn_play_pause, isPlaying ? R.drawable.btn_pause : R.drawable.btn_play_pause);

        // Intents pour les boutons
        remoteViews.setOnClickPendingIntent(R.id.noty_btn_play_pause, getPendingIntent(MusicNotificationReceiver.ACTION_PLAY_PAUSE, 0));
        remoteViews.setOnClickPendingIntent(R.id.noty_btn_next, getPendingIntent(MusicNotificationReceiver.ACTION_NEXT, 1));
        remoteViews.setOnClickPendingIntent(R.id.noty_btn_prev, getPendingIntent(MusicNotificationReceiver.ACTION_PREV, 2));

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music)
                .setCustomContentView(remoteViews)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private PendingIntent getPendingIntent(String action, int requestCode) {
        Intent intent = new Intent(this, MusicNotificationReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void setAlbumImage(RemoteViews rv, String path) {
        if (path == null || path.isEmpty()) {
            rv.setImageViewResource(R.id.noty_img_album, R.drawable.ic_img_default);
            return;
        }
        try {
            InputStream is;
            if (path.startsWith("/")) {
                Bitmap bmp = BitmapFactory.decodeFile(path);
                if (bmp != null) rv.setImageViewBitmap(R.id.noty_img_album, bmp);
                else rv.setImageViewResource(R.id.noty_img_album, R.drawable.ic_img_default);
            } else {
                String assetPath = path.contains("assets/") ? path.substring(path.indexOf("assets/") + 7) : path;
                is = getAssets().open(assetPath);
                rv.setImageViewBitmap(R.id.noty_img_album, BitmapFactory.decodeStream(is));
                is.close();
            }
        } catch (IOException e) {
            rv.setImageViewResource(R.id.noty_img_album, R.drawable.ic_img_default);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Music Player", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Contrôles de lecture");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
