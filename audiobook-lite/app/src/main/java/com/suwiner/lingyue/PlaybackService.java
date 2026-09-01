package com.suwiner.lingyue;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public final class PlaybackService extends Service implements AudioManager.OnAudioFocusChangeListener {
    public static final String ACTION_PLAY = "com.suwiner.lingyue.PLAY";
    public static final String ACTION_TOGGLE = "com.suwiner.lingyue.TOGGLE";
    public static final String ACTION_SEEK = "com.suwiner.lingyue.SEEK";
    public static final String ACTION_STOP = "com.suwiner.lingyue.STOP";
    public static final String ACTION_STATE = "com.suwiner.lingyue.STATE";
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_AUTHOR = "author";
    public static final String EXTRA_DELTA = "delta";

    private static final String CHANNEL_ID = "lingyue_playback";
    private static final int NOTIFICATION_ID = 53;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaPlayer player;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private String title = "聆阅";
    private String author = "有声内容";
    private String currentUrl = "";

    private final Runnable stateTicker = new Runnable() {
        @Override public void run() {
            broadcastState();
            handler.postDelayed(this, 800);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createChannel();
        handler.post(stateTicker);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_PLAY.equals(action)) {
            String url = intent.getStringExtra(EXTRA_URL);
            title = safe(intent.getStringExtra(EXTRA_TITLE), "正在播放");
            author = safe(intent.getStringExtra(EXTRA_AUTHOR), "聆阅");
            if (url != null && !url.trim().isEmpty()) play(url.trim());
        } else if (ACTION_TOGGLE.equals(action)) {
            toggle();
        } else if (ACTION_SEEK.equals(action)) {
            seekBy(intent.getIntExtra(EXTRA_DELTA, 0));
        } else if (ACTION_STOP.equals(action)) {
            releasePlayer();
            abandonFocus();
            stopForeground(true);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void play(String url) {
        currentUrl = url;
        releasePlayer();
        requestFocus();
        player = new MediaPlayer();
        player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build());
        player.setOnPreparedListener(mp -> {
            mp.start();
            updateNotification();
            broadcastState();
        });
        player.setOnCompletionListener(mp -> {
            updateNotification();
            broadcastState();
        });
        player.setOnErrorListener((mp, what, extra) -> {
            broadcastError("音频加载失败，请检查网络后重试");
            return false;
        });
        try {
            player.setDataSource(url);
            startForeground(NOTIFICATION_ID, buildNotification());
            player.prepareAsync();
        } catch (Exception e) {
            broadcastError("无法打开该音频");
            releasePlayer();
        }
    }

    private void toggle() {
        if (player == null) return;
        try {
            if (player.isPlaying()) {
                player.pause();
            } else {
                requestFocus();
                player.start();
            }
            updateNotification();
            broadcastState();
        } catch (Exception ignored) { }
    }

    private void seekBy(int deltaMs) {
        if (player == null) return;
        try {
            int duration = player.getDuration();
            int target = Math.max(0, Math.min(duration, player.getCurrentPosition() + deltaMs));
            player.seekTo(target);
            broadcastState();
        } catch (Exception ignored) { }
    }

    private void releasePlayer() {
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }

    private void createChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.playback_channel), NotificationManager.IMPORTANCE_LOW);
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 10, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent toggleIntent = PendingIntent.getService(this, 11,
                new Intent(this, PlaybackService.class).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stopIntent = PendingIntent.getService(this, 12,
                new Intent(this, PlaybackService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean isPlaying = false;
        if (player != null) {
            try { isPlaying = player.isPlaying(); } catch (Exception ignored) { }
        }

        Notification.Action toggleAction = new Notification.Action.Builder(
                android.R.drawable.ic_media_play, isPlaying ? "暂停" : "播放", toggleIntent).build();
        Notification.Action stopAction = new Notification.Action.Builder(
                android.R.drawable.ic_menu_close_clear_cancel, "停止", stopIntent).build();

        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_play)
                .setContentTitle(title)
                .setContentText(author)
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(isPlaying)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .addAction(toggleAction)
                .addAction(stopAction)
                .setStyle(new Notification.MediaStyle().setShowActionsInCompactView(0, 1))
                .build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, buildNotification());
    }

    private void requestFocus() {
        if (audioManager == null) return;
        if (focusRequest == null) {
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .setOnAudioFocusChangeListener(this)
                    .build();
        }
        audioManager.requestAudioFocus(focusRequest);
    }

    private void abandonFocus() {
        if (audioManager != null && focusRequest != null) audioManager.abandonAudioFocusRequest(focusRequest);
    }

    @Override public void onAudioFocusChange(int focusChange) {
        if (player == null) return;
        try {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                player.pause();
            }
        } catch (Exception ignored) { }
        updateNotification();
        broadcastState();
    }

    private void broadcastState() {
        Intent state = new Intent(ACTION_STATE).setPackage(getPackageName());
        state.putExtra("title", title);
        state.putExtra("author", author);
        state.putExtra("url", currentUrl);
        boolean isPlaying = false;
        int position = 0;
        int duration = 0;
        if (player != null) {
            try {
                isPlaying = player.isPlaying();
                position = player.getCurrentPosition();
                duration = player.getDuration();
            } catch (Exception ignored) { }
        }
        state.putExtra("playing", isPlaying);
        state.putExtra("position", position);
        state.putExtra("duration", duration);
        sendBroadcast(state);
    }

    private void broadcastError(String message) {
        Intent state = new Intent(ACTION_STATE).setPackage(getPackageName());
        state.putExtra("error", message);
        state.putExtra("title", title);
        state.putExtra("author", author);
        sendBroadcast(state);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        abandonFocus();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
