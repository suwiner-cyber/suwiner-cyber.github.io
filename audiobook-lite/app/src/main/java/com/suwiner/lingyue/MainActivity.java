package com.suwiner.lingyue;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(27, 86, 78);
    private static final int GREEN_2 = Color.rgb(46, 112, 99);
    private static final int TEXT = Color.rgb(33, 39, 37);
    private static final int SUB = Color.rgb(112, 118, 113);
    private static final int BG = Color.rgb(248, 246, 241);
    private static final int CARD = Color.rgb(255, 254, 251);
    private static final String PREFS = "lingyue_prefs";
    private static final String KEY_GATEWAY = "xmly_gateway";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<Book> books = new ArrayList<>();
    private LinearLayout listContainer;
    private TextView gatewayStatus;
    private TextView miniTitle;
    private TextView miniMeta;
    private TextView miniTime;
    private Button miniToggle;
    private ProgressBar miniProgress;
    private SharedPreferences prefs;

    private final BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!PlaybackService.ACTION_STATE.equals(intent.getAction())) return;
            String error = intent.getStringExtra("error");
            if (error != null && !error.trim().isEmpty()) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
            String title = intent.getStringExtra("title");
            String author = intent.getStringExtra("author");
            boolean playing = intent.getBooleanExtra("playing", false);
            int pos = intent.getIntExtra("position", 0);
            int duration = intent.getIntExtra("duration", 0);
            miniTitle.setText(title == null || title.trim().isEmpty() ? "选择一本内容开始聆听" : title);
            miniMeta.setText(author == null || author.trim().isEmpty() ? "聆阅" : author);
            miniToggle.setText(playing ? "暂停" : "播放");
            miniProgress.setMax(Math.max(duration, 1));
            miniProgress.setProgress(Math.max(0, Math.min(duration, pos)));
            miniTime.setText(formatTime(pos) + " / " + formatTime(duration));
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        configureSystemBars();
        seedBooks();
        setContentView(buildScreen());
        renderBooks(books);
        updateGatewayStatus();
    }

    private void configureSystemBars() {
        Window w = getWindow();
        w.setStatusBarColor(BG);
        w.setNavigationBarColor(Color.rgb(253, 252, 249));
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = w.getInsetsController();
            if (c != null) c.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(26));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView hello = text("早上好，今天想听点什么？", 13, SUB, false);
        content.addView(hello);
        TextView title = text("聆阅", 31, TEXT, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(5);
        content.addView(title, titleLp);

        TextView search = text("⌕  搜索有声书、作者、分类", 14, Color.rgb(132, 136, 132), false);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(17), 0, dp(17), 0);
        search.setBackground(round(Color.rgb(239, 237, 231), 18));
        search.setOnClickListener(v -> showSearchDialog());
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        searchLp.topMargin = dp(18);
        content.addView(search, searchLp);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(22), dp(20), dp(22), dp(18));
        hero.setBackground(gradient(GREEN, GREEN_2, 28));
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.topMargin = dp(20);
        content.addView(hero, heroLp);
        hero.addView(text("XIMALAYA OPEN PLATFORM", 11, Color.rgb(212, 235, 228), true));
        TextView heroTitle = text("喜马拉雅免费内容", 23, Color.WHITE, true);
        LinearLayout.LayoutParams htlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        htlp.topMargin = dp(8);
        hero.addView(heroTitle, htlp);
        gatewayStatus = text("官方接口适配层已就绪", 13, Color.rgb(225, 238, 234), false);
        LinearLayout.LayoutParams gslp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gslp.topMargin = dp(7);
        hero.addView(gatewayStatus, gslp);
        LinearLayout heroActions = new LinearLayout(this);
        heroActions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsLp.topMargin = dp(16);
        hero.addView(heroActions, actionsLp);
        Button setup = pillButton("配置接入", Color.WHITE, Color.argb(44, 255, 255, 255));
        setup.setOnClickListener(v -> showGatewayDialog());
        heroActions.addView(setup, new LinearLayout.LayoutParams(0, dp(42), 1f));
        Button sync = pillButton("同步免费内容", Color.WHITE, Color.argb(44, 255, 255, 255));
        sync.setOnClickListener(v -> syncXimalaya());
        LinearLayout.LayoutParams syncLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        syncLp.leftMargin = dp(10);
        heroActions.addView(sync, syncLp);

        addSection(content, "今日精选", "合法试听 + 授权内容");
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(listContainer);

        addSection(content, "按分类听", "文学 · 历史 · 儿童 · 商业");
        content.addView(buildCategories());

        addSection(content, "关于喜马拉雅接入", "官方开放平台");
        LinearLayout note = card();
        note.addView(text("密钥不上 APK", 15, TEXT, true));
        TextView noteText = text("AppKey、Secret、签名与 Token 应放在你的 HTTPS 服务端。聆阅只调用你自己的网关，服务端再请求喜马拉雅官方开放平台，并仅返回已获授权的免费内容。", 12, SUB, false);
        noteText.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteLp.topMargin = dp(8);
        note.addView(noteText, noteLp);
        Button official = outlineButton("打开喜马拉雅开放平台");
        official.setOnClickListener(v -> openUrl("https://open.ximalaya.com/"));
        LinearLayout.LayoutParams officialLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(45));
        officialLp.topMargin = dp(14);
        note.addView(official, officialLp);
        content.addView(note);

        root.addView(buildMiniPlayer(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(98)));
        return root;
    }

    private View buildCategories() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        String[] cats = {"文学", "历史", "悬疑", "儿童", "商业", "人文"};
        for (String cat : cats) {
            TextView chip = text(cat, 13, TEXT, true);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(round(CARD, 16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(82), dp(48));
            lp.rightMargin = dp(10);
            row.addView(chip, lp);
        }
        hsv.addView(row);
        return hsv;
    }

    private View buildMiniPlayer() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(10), dp(14), dp(8));
        box.setBackgroundColor(Color.rgb(253, 252, 249));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        miniTitle = text("选择一本内容开始聆听", 13, TEXT, true);
        miniMeta = text("聆阅", 11, SUB, false);
        info.addView(miniTitle);
        info.addView(miniMeta);
        top.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button back = pillButton("-15s", TEXT, Color.rgb(241, 239, 233));
        back.setOnClickListener(v -> sendPlayback(PlaybackService.ACTION_SEEK, -15000));
        top.addView(back, new LinearLayout.LayoutParams(dp(58), dp(38)));
        miniToggle = pillButton("播放", Color.WHITE, GREEN);
        miniToggle.setOnClickListener(v -> sendPlayback(PlaybackService.ACTION_TOGGLE, 0));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(dp(62), dp(38));
        tLp.leftMargin = dp(7);
        top.addView(miniToggle, tLp);
        Button fwd = pillButton("+15s", TEXT, Color.rgb(241, 239, 233));
        fwd.setOnClickListener(v -> sendPlayback(PlaybackService.ACTION_SEEK, 15000));
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(dp(58), dp(38));
        fLp.leftMargin = dp(7);
        top.addView(fwd, fLp);

        LinearLayout progressRow = new LinearLayout(this);
        progressRow.setOrientation(LinearLayout.HORIZONTAL);
        progressRow.setGravity(Gravity.CENTER_VERTICAL);
        miniProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        miniProgress.setMax(1);
        progressRow.addView(miniProgress, new LinearLayout.LayoutParams(0, dp(5), 1f));
        miniTime = text("00:00 / 00:00", 10, SUB, false);
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        timeLp.leftMargin = dp(10);
        progressRow.addView(miniTime, timeLp);
        box.addView(progressRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
        return box;
    }

    private void seedBooks() {
        books.clear();
        books.add(new Book("桃花源记 · 公版试听", "中文公版/开放授权音频", "文学", "Wikimedia Commons", "https://upload.wikimedia.org/wikipedia/commons/9/9c/Peach_blossom_shangri_la_Tao_ps.ogg", true));
        books.add(new Book("南国山河 · 普通话试听", "普通话开放授权音频", "诗文", "Wikimedia Commons", "https://upload.wikimedia.org/wikipedia/commons/8/8a/Mandarin_Chinese-%E5%8D%97%E5%9B%BD%E5%B1%B1%E6%B2%B3.ogg", true));
        books.add(new Book("喜马拉雅免费精选 · 文学", "绑定开放平台网关后自动同步", "文学", "喜马拉雅", "", false));
        books.add(new Book("喜马拉雅免费精选 · 历史", "官方免费内容接口预留", "历史", "喜马拉雅", "", false));
    }

    private void renderBooks(List<Book> source) {
        listContainer.removeAllViews();
        for (Book b : source) listContainer.addView(bookCard(b));
    }

    private View bookCard(Book b) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(round(CARD, 22));
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(116));
        rowLp.bottomMargin = dp(12);
        row.setLayoutParams(rowLp);

        TextView cover = text(b.category, 13, Color.WHITE, true);
        cover.setGravity(Gravity.CENTER);
        cover.setBackground(gradient(GREEN, Color.rgb(69, 116, 104), 18));
        row.addView(cover, new LinearLayout.LayoutParams(dp(78), dp(92)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(dp(13), 0, dp(8), 0);
        TextView t = text(b.title, 15, TEXT, true);
        info.addView(t);
        TextView meta = text(b.subtitle, 12, SUB, false);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaLp.topMargin = dp(6);
        info.addView(meta, metaLp);
        TextView source = text(b.source + (b.playable ? " · 可播放" : " · 待授权"), 10, b.playable ? GREEN : Color.rgb(158, 112, 53), true);
        LinearLayout.LayoutParams sourceLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sourceLp.topMargin = dp(8);
        info.addView(source, sourceLp);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        Button play = pillButton(b.playable ? "播放" : "授权", Color.WHITE, b.playable ? GREEN : Color.rgb(178, 152, 106));
        play.setOnClickListener(v -> {
            if (b.playable) playBook(b);
            else showGatewayDialog();
        });
        row.addView(play, new LinearLayout.LayoutParams(dp(60), dp(40)));
        return row;
    }

    private void playBook(Book b) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 530);
        }
        Intent i = new Intent(this, PlaybackService.class)
                .setAction(PlaybackService.ACTION_PLAY)
                .putExtra(PlaybackService.EXTRA_URL, b.url)
                .putExtra(PlaybackService.EXTRA_TITLE, b.title)
                .putExtra(PlaybackService.EXTRA_AUTHOR, b.subtitle);
        startForegroundService(i);
    }

    private void sendPlayback(String action, int delta) {
        Intent i = new Intent(this, PlaybackService.class).setAction(action);
        if (PlaybackService.ACTION_SEEK.equals(action)) i.putExtra(PlaybackService.EXTRA_DELTA, delta);
        startService(i);
    }

    private void showSearchDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("例如：文学、历史、儿童");
        new AlertDialog.Builder(this)
                .setTitle("搜索有声内容")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("搜索", (d, w) -> {
                    String q = input.getText().toString().trim();
                    if (q.isEmpty()) return;
                    ArrayList<Book> result = new ArrayList<>();
                    for (Book b : books) {
                        if (b.title.contains(q) || b.subtitle.contains(q) || b.category.contains(q) || b.source.contains(q)) result.add(b);
                    }
                    if (result.isEmpty()) Toast.makeText(this, "当前内容中没有匹配结果，绑定喜马拉雅官方网关后可扩展搜索", Toast.LENGTH_LONG).show();
                    else renderBooks(result);
                }).show();
    }

    private void showGatewayDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://你的服务端域名");
        input.setText(prefs.getString(KEY_GATEWAY, ""));
        new AlertDialog.Builder(this)
                .setTitle("喜马拉雅官方授权网关")
                .setMessage("请填写你的 HTTPS 服务端地址。AppKey、Secret、签名和 Token 留在服务端，不写入 APK。")
                .setView(input)
                .setNegativeButton("取消", null)
                .setNeutralButton("清除", (d, w) -> {
                    prefs.edit().remove(KEY_GATEWAY).apply();
                    updateGatewayStatus();
                })
                .setPositiveButton("保存", (d, w) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty() && !value.startsWith("https://")) {
                        Toast.makeText(this, "只允许 HTTPS 服务端地址", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (value.endsWith("/")) value = value.substring(0, value.length() - 1);
                    prefs.edit().putString(KEY_GATEWAY, value).apply();
                    updateGatewayStatus();
                }).show();
    }

    private void syncXimalaya() {
        String base = prefs.getString(KEY_GATEWAY, "").trim();
        if (base.isEmpty()) {
            showGatewayDialog();
            return;
        }
        Toast.makeText(this, "正在同步喜马拉雅官方免费内容…", Toast.LENGTH_SHORT).show();
        io.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(base + "/ximalaya/free/albums?page=1&count=30");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(12000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
                StringBuilder raw = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) raw.append(line);
                }
                JSONObject root = new JSONObject(raw.toString());
                JSONArray items = root.optJSONArray("items");
                ArrayList<Book> remote = new ArrayList<>();
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject o = items.optJSONObject(i);
                        if (o == null) continue;
                        String audio = o.optString("audio_url", "");
                        remote.add(new Book(
                                o.optString("title", "喜马拉雅免费内容"),
                                o.optString("author", "喜马拉雅开放平台"),
                                o.optString("category", "有声书"),
                                "喜马拉雅官方授权",
                                audio,
                                !audio.trim().isEmpty()));
                    }
                }
                runOnUiThread(() -> {
                    if (remote.isEmpty()) {
                        Toast.makeText(this, "网关连接成功，但没有返回可播放的免费内容", Toast.LENGTH_LONG).show();
                    } else {
                        books.clear();
                        books.addAll(remote);
                        renderBooks(books);
                        Toast.makeText(this, "已同步 " + remote.size() + " 条官方授权免费内容", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "同步失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void updateGatewayStatus() {
        if (gatewayStatus == null) return;
        String base = prefs.getString(KEY_GATEWAY, "").trim();
        gatewayStatus.setText(base.isEmpty() ? "官方 API 适配层已就绪 · 尚未绑定服务端" : "已绑定授权网关 · 可同步官方免费内容");
    }

    private void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { Toast.makeText(this, "无法打开浏览器", Toast.LENGTH_SHORT).show(); }
    }

    private void addSection(LinearLayout parent, String left, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(27);
        rowLp.bottomMargin = dp(13);
        parent.addView(row, rowLp);
        row.addView(text(left, 18, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text(right, 11, SUB, false));
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(17), dp(17), dp(17), dp(17));
        c.setBackground(round(CARD, 22));
        return c;
    }

    private TextView text(String value, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans", Typeface.BOLD));
        return t;
    }

    private Button pillButton(String label, int textColor, int bgColor) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(textColor);
        b.setTypeface(Typeface.create("sans", Typeface.BOLD));
        b.setAllCaps(false);
        b.setPadding(dp(7), 0, dp(7), 0);
        b.setBackground(round(bgColor, 16));
        return b;
    }

    private Button outlineButton(String label) {
        Button b = pillButton(label, GREEN, Color.rgb(237, 244, 241));
        return b;
    }

    private GradientDrawable round(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private GradientDrawable gradient(int start, int end, float radiusDp) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private String formatTime(int ms) {
        int sec = Math.max(0, ms / 1000);
        return String.format(Locale.CHINA, "%02d:%02d", sec / 60, sec % 60);
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(PlaybackService.ACTION_STATE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(playbackReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(playbackReceiver, filter);
    }

    @Override protected void onStop() {
        try { unregisterReceiver(playbackReceiver); } catch (Exception ignored) { }
        super.onStop();
    }

    @Override protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private static final class Book {
        final String title;
        final String subtitle;
        final String category;
        final String source;
        final String url;
        final boolean playable;
        Book(String title, String subtitle, String category, String source, String url, boolean playable) {
            this.title = title;
            this.subtitle = subtitle;
            this.category = category;
            this.source = source;
            this.url = url;
            this.playable = playable;
        }
    }
}
