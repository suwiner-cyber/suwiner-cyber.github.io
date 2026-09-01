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
    private static final int GREEN = Color.rgb(22, 83, 75);
    private static final int GREEN_2 = Color.rgb(47, 116, 101);
    private static final int GOLD = Color.rgb(203, 169, 92);
    private static final int TEXT = Color.rgb(31, 37, 35);
    private static final int SUB = Color.rgb(108, 115, 110);
    private static final int BG = Color.rgb(248, 246, 240);
    private static final int CARD = Color.rgb(255, 254, 250);
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
    private boolean receiverRegistered = false;

    private final BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || !PlaybackService.ACTION_STATE.equals(intent.getAction())) return;
            try {
                String error = intent.getStringExtra("error");
                if (error != null && !error.trim().isEmpty()) {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                }
                String title = intent.getStringExtra("title");
                String author = intent.getStringExtra("author");
                boolean playing = intent.getBooleanExtra("playing", false);
                int pos = intent.getIntExtra("position", 0);
                int duration = intent.getIntExtra("duration", 0);
                if (miniTitle != null) miniTitle.setText(title == null || title.trim().isEmpty() ? "选择一本内容开始聆听" : title);
                if (miniMeta != null) miniMeta.setText(author == null || author.trim().isEmpty() ? "聆阅" : author);
                if (miniToggle != null) miniToggle.setText(playing ? "暂停" : "播放");
                if (miniProgress != null) {
                    miniProgress.setMax(Math.max(duration, 1));
                    miniProgress.setProgress(Math.max(0, Math.min(Math.max(duration, 0), pos)));
                }
                if (miniTime != null) miniTime.setText(formatTime(pos) + " / " + formatTime(duration));
            } catch (Throwable ignored) {
                // A malformed playback broadcast must never crash the launcher activity.
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            configureSystemBarsSafely();
            seedBooks();
            View screen = buildScreen();
            setContentView(screen);
            renderBooks(new ArrayList<>(books));
            updateGatewayStatus();
        } catch (Throwable error) {
            showStartupFallback(error);
        }
    }

    private void configureSystemBarsSafely() {
        try {
            Window w = getWindow();
            w.setStatusBarColor(BG);
            w.setNavigationBarColor(Color.rgb(253, 252, 248));
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            w.getDecorView().setSystemUiVisibility(flags);
        } catch (Throwable ignored) {
            // Some HarmonyOS compatibility layers handle system bar APIs differently.
        }
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(30), dp(20), dp(30));
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout brandRow = new LinearLayout(this);
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(brandRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView mark = text("聆", 18, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(round(GREEN, 16));
        brandRow.addView(mark, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setPadding(dp(12), 0, 0, 0);
        brandText.addView(text("聆阅", 27, TEXT, true));
        brandText.addView(text("听见好故事，也听见时间", 11, SUB, false));
        brandRow.addView(brandText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView dayTag = text("今日", 11, GREEN, true);
        dayTag.setGravity(Gravity.CENTER);
        dayTag.setBackground(round(Color.rgb(231, 239, 235), 14));
        brandRow.addView(dayTag, new LinearLayout.LayoutParams(dp(54), dp(34)));

        TextView search = text("搜索有声书、作者、分类", 14, Color.rgb(132, 137, 133), false);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(17), 0, dp(17), 0);
        search.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
        search.setCompoundDrawablePadding(dp(10));
        search.setBackground(round(Color.rgb(239, 237, 231), 18));
        search.setOnClickListener(v -> showSearchDialog());
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        searchLp.topMargin = dp(22);
        content.addView(search, searchLp);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(22), dp(22), dp(22), dp(20));
        hero.setBackground(gradient(GREEN, GREEN_2, 28));
        LinearLayout.LayoutParams heroLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroLp.topMargin = dp(20);
        content.addView(hero, heroLp);

        TextView badge = text("XIMALAYA · OFFICIAL ADAPTER", 10, Color.rgb(215, 235, 229), true);
        hero.addView(badge);
        TextView heroTitle = text("喜马拉雅免费内容", 23, Color.WHITE, true);
        LinearLayout.LayoutParams heroTitleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        heroTitleLp.topMargin = dp(8);
        hero.addView(heroTitle, heroTitleLp);
        gatewayStatus = text("官方 API 接入层已准备", 13, Color.rgb(229, 240, 236), false);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dp(6);
        hero.addView(gatewayStatus, statusLp);

        LinearLayout heroActions = new LinearLayout(this);
        heroActions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionsLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionsLp.topMargin = dp(16);
        hero.addView(heroActions, actionsLp);

        Button setup = pillButton("配置接入", Color.WHITE, Color.argb(40, 255, 255, 255));
        setup.setOnClickListener(v -> showGatewayDialog());
        heroActions.addView(setup, new LinearLayout.LayoutParams(0, dp(42), 1f));
        Button sync = pillButton("同步免费内容", GREEN, Color.WHITE);
        sync.setOnClickListener(v -> syncXimalaya());
        LinearLayout.LayoutParams syncLp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        syncLp.leftMargin = dp(10);
        heroActions.addView(sync, syncLp);

        addSection(content, "今日精选", "开放试听 · 授权内容");
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(listContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addSection(content, "按分类听", "文学 · 历史 · 悬疑 · 儿童");
        content.addView(buildCategories());

        addSection(content, "喜马拉雅接入", "安全服务端模式");
        LinearLayout note = card();
        note.addView(text("密钥不进入 APK", 15, TEXT, true));
        TextView noteText = text("AppKey、Secret、签名与 Token 放在你的 HTTPS 服务端。聆阅只连接你自己的网关，再由服务端访问喜马拉雅官方开放平台，仅返回已授权的免费内容。", 12, SUB, false);
        noteText.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams noteTextLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteTextLp.topMargin = dp(8);
        note.addView(noteText, noteTextLp);
        Button official = outlineButton("打开喜马拉雅开放平台");
        official.setOnClickListener(v -> openUrl("https://open.ximalaya.com/"));
        LinearLayout.LayoutParams officialLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(45));
        officialLp.topMargin = dp(14);
        note.addView(official, officialLp);
        content.addView(note);

        root.addView(buildMiniPlayer(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(100)));
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
            chip.setOnClickListener(v -> filterByCategory(cat));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(82), dp(48));
            lp.rightMargin = dp(10);
            row.addView(chip, lp);
        }
        hsv.addView(row, new HorizontalScrollView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return hsv;
    }

    private View buildMiniPlayer() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(10), dp(14), dp(8));
        box.setBackgroundColor(Color.rgb(253, 252, 248));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        miniTitle = text("选择一本内容开始聆听", 13, TEXT, true);
        miniTitle.setSingleLine(true);
        miniMeta = text("聆阅", 11, SUB, false);
        info.addView(miniTitle);
        info.addView(miniMeta);
        top.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button back = pillButton("-15s", TEXT, Color.rgb(241, 239, 233));
        back.setOnClickListener(v -> sendPlayback(PlaybackService.ACTION_SEEK, -15000));
        top.addView(back, new LinearLayout.LayoutParams(dp(58), dp(38)));
        miniToggle = pillButton("播放", Color.WHITE, GREEN);
        miniToggle.setOnClickListener(v -> sendPlayback(PlaybackService.ACTION_TOGGLE, 0));
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(dp(62), dp(38));
        toggleLp.leftMargin = dp(7);
        top.addView(miniToggle, toggleLp);
        Button fwd = pillButton("+15s", TEXT, Color.rgb(241, 239, 233));
        fwd.setOnClickListener(v -> sendPlayback(PlaybackService.ACTION_SEEK, 15000));
        LinearLayout.LayoutParams fwdLp = new LinearLayout.LayoutParams(dp(58), dp(38));
        fwdLp.leftMargin = dp(7);
        top.addView(fwd, fwdLp);

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
        books.add(new Book("桃花源记 · 公版试听", "中文公版开放音频", "文学", "Wikimedia Commons", "https://upload.wikimedia.org/wikipedia/commons/9/9c/Peach_blossom_shangri_la_Tao_ps.ogg", true));
        books.add(new Book("南国山河 · 普通话试听", "普通话开放授权音频", "诗文", "Wikimedia Commons", "https://upload.wikimedia.org/wikipedia/commons/8/8a/Mandarin_Chinese-%E5%8D%97%E5%9B%BD%E5%B1%B1%E6%B2%B3.ogg", true));
        books.add(new Book("喜马拉雅免费精选 · 文学", "绑定开放平台网关后自动同步", "文学", "喜马拉雅", "", false));
        books.add(new Book("喜马拉雅免费精选 · 历史", "官方免费内容接口预留", "历史", "喜马拉雅", "", false));
    }

    private void renderBooks(List<Book> source) {
        if (listContainer == null) return;
        listContainer.removeAllViews();
        if (source == null || source.isEmpty()) {
            TextView empty = text("没有找到匹配内容", 13, SUB, false);
            empty.setGravity(Gravity.CENTER);
            empty.setBackground(round(CARD, 20));
            listContainer.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(84)));
            return;
        }
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
        cover.setBackground(gradient(GREEN, Color.rgb(73, 126, 112), 18));
        row.addView(cover, new LinearLayout.LayoutParams(dp(78), dp(92)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(dp(13), 0, dp(8), 0);
        TextView title = text(b.title, 15, TEXT, true);
        title.setSingleLine(true);
        info.addView(title);
        TextView meta = text(b.subtitle, 12, SUB, false);
        meta.setSingleLine(true);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        metaLp.topMargin = dp(6);
        info.addView(meta, metaLp);
        TextView source = text(b.source + (b.playable ? " · 可播放" : " · 待授权"), 10, b.playable ? GREEN : Color.rgb(158, 112, 53), true);
        LinearLayout.LayoutParams sourceLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sourceLp.topMargin = dp(8);
        info.addView(source, sourceLp);
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        Button play = pillButton(b.playable ? "播放" : "授权", Color.WHITE, b.playable ? GREEN : Color.rgb(177, 151, 105));
        play.setOnClickListener(v -> {
            if (b.playable) playBook(b);
            else showGatewayDialog();
        });
        row.addView(play, new LinearLayout.LayoutParams(dp(60), dp(40)));
        return row;
    }

    private void playBook(Book b) {
        try {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 53);
            }
            Intent intent = new Intent(this, PlaybackService.class)
                    .setAction(PlaybackService.ACTION_PLAY)
                    .putExtra(PlaybackService.EXTRA_URL, b.url)
                    .putExtra(PlaybackService.EXTRA_TITLE, b.title)
                    .putExtra(PlaybackService.EXTRA_AUTHOR, b.source);
            startForegroundService(intent);
            if (miniTitle != null) miniTitle.setText(b.title);
            if (miniMeta != null) miniMeta.setText("正在连接音频…");
        } catch (Throwable error) {
            Toast.makeText(this, "播放器启动失败，请重试", Toast.LENGTH_LONG).show();
        }
    }

    private void sendPlayback(String action, int delta) {
        try {
            Intent intent = new Intent(this, PlaybackService.class).setAction(action);
            if (PlaybackService.ACTION_SEEK.equals(action)) intent.putExtra(PlaybackService.EXTRA_DELTA, delta);
            startService(intent);
        } catch (Throwable ignored) {
            Toast.makeText(this, "请先选择一本内容播放", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSearchDialog() {
        final EditText input = new EditText(this);
        input.setHint("输入书名、作者或分类");
        input.setSingleLine(true);
        input.setPadding(dp(16), 0, dp(16), 0);
        new AlertDialog.Builder(this)
                .setTitle("搜索聆阅")
                .setView(input)
                .setNegativeButton("取消", null)
                .setNeutralButton("全部", (d, w) -> renderBooks(new ArrayList<>(books)))
                .setPositiveButton("搜索", (d, w) -> {
                    String q = input.getText() == null ? "" : input.getText().toString().trim().toLowerCase(Locale.ROOT);
                    if (q.isEmpty()) {
                        renderBooks(new ArrayList<>(books));
                        return;
                    }
                    List<Book> filtered = new ArrayList<>();
                    for (Book b : books) {
                        String hay = (b.title + " " + b.subtitle + " " + b.category + " " + b.source).toLowerCase(Locale.ROOT);
                        if (hay.contains(q)) filtered.add(b);
                    }
                    renderBooks(filtered);
                })
                .show();
    }

    private void filterByCategory(String category) {
        List<Book> filtered = new ArrayList<>();
        for (Book b : books) if (b.category.contains(category)) filtered.add(b);
        if (filtered.isEmpty()) {
            Toast.makeText(this, "该分类会在接入喜马拉雅授权后同步", Toast.LENGTH_SHORT).show();
            renderBooks(new ArrayList<>(books));
        } else {
            renderBooks(filtered);
        }
    }

    private void showGatewayDialog() {
        final EditText input = new EditText(this);
        input.setHint("https://你的服务端域名");
        input.setSingleLine(true);
        String saved = prefs == null ? "" : prefs.getString(KEY_GATEWAY, "");
        input.setText(saved == null ? "" : saved);
        new AlertDialog.Builder(this)
                .setTitle("喜马拉雅服务端网关")
                .setMessage("仅支持 HTTPS。AppKey、Secret 与签名必须保留在服务端。")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", (dialog, which) -> {
                    String value = input.getText() == null ? "" : input.getText().toString().trim();
                    while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
                    if (!value.isEmpty() && !value.startsWith("https://")) {
                        Toast.makeText(this, "为保护密钥，只允许 HTTPS 地址", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (prefs != null) prefs.edit().putString(KEY_GATEWAY, value).apply();
                    updateGatewayStatus();
                })
                .show();
    }

    private void updateGatewayStatus() {
        if (gatewayStatus == null) return;
        String value = prefs == null ? "" : prefs.getString(KEY_GATEWAY, "");
        gatewayStatus.setText(value == null || value.trim().isEmpty() ? "官方 API 接入层已准备 · 尚未绑定服务端" : "服务端已绑定 · 可同步授权免费内容");
    }

    private void syncXimalaya() {
        String base = prefs == null ? "" : prefs.getString(KEY_GATEWAY, "");
        if (base == null || base.trim().isEmpty()) {
            showGatewayDialog();
            return;
        }
        if (gatewayStatus != null) gatewayStatus.setText("正在同步授权免费内容…");
        final String endpoint = base.trim() + "/ximalaya/free/albums?page=1&count=30";
        io.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(12000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalStateException("HTTP " + code);
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                }
                JSONObject root = new JSONObject(body.toString());
                JSONArray items = root.optJSONArray("items");
                if (items == null) throw new IllegalStateException("items missing");
                List<Book> remote = new ArrayList<>();
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item == null) continue;
                    String title = item.optString("title", "未命名内容");
                    String author = item.optString("author", "喜马拉雅");
                    String category = item.optString("category", "精选");
                    String audio = item.optString("audio_url", "");
                    if (audio.startsWith("https://")) remote.add(new Book(title, author, category, "喜马拉雅", audio, true));
                }
                runOnUiThread(() -> {
                    if (!remote.isEmpty()) {
                        books.clear();
                        books.addAll(remote);
                        renderBooks(new ArrayList<>(books));
                        if (gatewayStatus != null) gatewayStatus.setText("同步完成 · " + remote.size() + " 条授权内容");
                    } else {
                        if (gatewayStatus != null) gatewayStatus.setText("服务端已连接 · 暂无可播放条目");
                        Toast.makeText(this, "服务端返回成功，但没有可播放的 HTTPS 音频", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Throwable error) {
                runOnUiThread(() -> {
                    updateGatewayStatus();
                    Toast.makeText(this, "同步失败，请检查服务端接口配置", Toast.LENGTH_LONG).show();
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Throwable ignored) {
            Toast.makeText(this, "未找到可打开网页的应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void addSection(LinearLayout parent, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.BOTTOM);
        TextView t = text(title, 20, TEXT, true);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView sub = text(subtitle, 11, SUB, false);
        row.addView(sub);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(27);
        lp.bottomMargin = dp(13);
        parent.addView(row, lp);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(17), dp(18), dp(17));
        card.setBackground(round(CARD, 22));
        return card;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        else view.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return view;
    }

    private Button pillButton(String label, int textColor, int bgColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setBackground(round(bgColor, 15));
        return button;
    }

    private Button outlineButton(String label) {
        Button button = pillButton(label, GREEN, Color.TRANSPARENT);
        GradientDrawable bg = round(Color.TRANSPARENT, 15);
        bg.setStroke(dp(1), Color.rgb(209, 217, 211));
        button.setBackground(bg);
        return button;
    }

    private GradientDrawable round(int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable gradient(int start, int end, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String formatTime(int millis) {
        int total = Math.max(0, millis) / 1000;
        return String.format(Locale.CHINA, "%02d:%02d", total / 60, total % 60);
    }

    private void showStartupFallback(Throwable error) {
        try {
            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setGravity(Gravity.CENTER);
            root.setPadding(dp(28), dp(28), dp(28), dp(28));
            root.setBackgroundColor(BG);
            TextView logo = text("聆", 28, Color.WHITE, true);
            logo.setGravity(Gravity.CENTER);
            logo.setBackground(round(GREEN, 22));
            root.addView(logo, new LinearLayout.LayoutParams(dp(74), dp(74)));
            TextView title = text("聆阅已进入兼容模式", 20, TEXT, true);
            title.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            titleLp.topMargin = dp(18);
            root.addView(title, titleLp);
            TextView desc = text("检测到当前系统对部分界面能力存在兼容差异。应用没有退出，你仍可重新进入；该异常已被启动保护拦截。", 13, SUB, false);
            desc.setGravity(Gravity.CENTER);
            desc.setLineSpacing(dp(4), 1f);
            LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            descLp.topMargin = dp(10);
            root.addView(desc, descLp);
            Button retry = pillButton("重新载入", Color.WHITE, GREEN);
            retry.setOnClickListener(v -> recreate());
            LinearLayout.LayoutParams retryLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
            retryLp.topMargin = dp(22);
            root.addView(retry, retryLp);
            setContentView(root);
        } catch (Throwable ignored) {
            TextView emergency = new TextView(this);
            emergency.setText("聆阅\n系统兼容模式");
            emergency.setGravity(Gravity.CENTER);
            setContentView(emergency);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (receiverRegistered) return;
        try {
            IntentFilter filter = new IntentFilter(PlaybackService.ACTION_STATE);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(playbackReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(playbackReceiver, filter);
            receiverRegistered = true;
        } catch (Throwable ignored) {
            receiverRegistered = false;
        }
    }

    @Override protected void onPause() {
        if (receiverRegistered) {
            try { unregisterReceiver(playbackReceiver); } catch (Throwable ignored) { }
            receiverRegistered = false;
        }
        super.onPause();
    }

    @Override protected void onDestroy() {
        try { io.shutdownNow(); } catch (Throwable ignored) { }
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
