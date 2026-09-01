package com.xiaoxiaoshuo.reader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

public class ReaderActivity extends Activity {
    private TextView body,title;
    private ScrollView scroll;
    private float fontSize=20f, lineExtra=9f;
    private int bg=Color.rgb(248,244,232), fg=Color.rgb(48,44,38);
    private String fontKey="wenkai";
    private final LinkedHashMap<String,String> fonts=new LinkedHashMap<>();

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        fonts.put("文楷","fonts/LXGWWenKaiLite-Regular.ttf");
        fonts.put("文楷中黑","fonts/LXGWWenKaiLite-Medium.ttf");
        fonts.put("文楷轻体","fonts/LXGWWenKaiLite-Light.ttf");
        fonts.put("等宽文楷","fonts/LXGWWenKaiMonoLite-Regular.ttf");
        fonts.put("等宽中黑","fonts/LXGWWenKaiMonoLite-Medium.ttf");
        fonts.put("系统黑体","system:sans");
        loadPrefs(); build();
    }

    private void build(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(bg);
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(12),dp(8),dp(12),dp(8));
        TextView back=label("‹",32); back.setGravity(Gravity.CENTER); back.setOnClickListener(v->finish()); top.addView(back,new LinearLayout.LayoutParams(dp(46),dp(48)));
        title=label(getIntent().getStringExtra("title") == null?"阅读":getIntent().getStringExtra("title"),16); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); top.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView setup=label("阅读设置",14); setup.setGravity(Gravity.CENTER); setup.setOnClickListener(v->showSettings()); top.addView(setup,new LinearLayout.LayoutParams(dp(86),dp(48)));
        root.addView(top);

        scroll=new ScrollView(this); scroll.setFillViewport(true); body=new TextView(this); body.setPadding(dp(26),dp(18),dp(26),dp(46)); body.setTextSize(fontSize); body.setTextColor(fg); body.setLineSpacing(dp(lineExtra),1f); body.setTextIsSelectable(true); body.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        String raw=getIntent().getStringExtra("body"); if(raw==null) raw="正文加载失败。";
        // 唯一正文渲染链：永远先经过 AI 排版，不存在默认排版分支。
        body.setText(AiTypesetter.formatNovel(raw)); applyFont();
        scroll.addView(body,new ScrollView.LayoutParams(-1,-2)); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root); getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg); getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private void showSettings(){
        final Dialog d=new Dialog(this); d.getWindow();
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(22),dp(18),dp(22),dp(20)); GradientDrawable pbg=new GradientDrawable(); pbg.setColor(Color.rgb(255,253,249)); pbg.setCornerRadius(dp(24)); panel.setBackground(pbg);
        TextView h=label("阅读排版",22); h.setTypeface(Typeface.DEFAULT,Typeface.BOLD); panel.addView(h);
        TextView hint=label("永久使用 AI 排版 · 长按正文可选择文字",13); hint.setTextColor(Color.GRAY); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.topMargin=dp(4); hp.bottomMargin=dp(16); panel.addView(hint,hp);

        panel.addView(section("字号")); LinearLayout sizes=row(); sizes.addView(action("A−",v->{fontSize=Math.max(15,fontSize-1);applyAndSave();}),weight()); sizes.addView(action("A+",v->{fontSize=Math.min(32,fontSize+1);applyAndSave();}),weight()); sizes.addView(action("默认",v->{fontSize=20;lineExtra=9;applyAndSave();}),weight()); panel.addView(sizes);
        panel.addView(section("行距")); LinearLayout lines=row(); lines.addView(action("紧凑",v->{lineExtra=5;applyAndSave();}),weight()); lines.addView(action("舒适",v->{lineExtra=9;applyAndSave();}),weight()); lines.addView(action("宽松",v->{lineExtra=14;applyAndSave();}),weight()); panel.addView(lines);

        panel.addView(section("字体")); LinearLayout fontWrap=new LinearLayout(this); fontWrap.setOrientation(LinearLayout.VERTICAL); int n=0; LinearLayout current=null;
        for(Map.Entry<String,String> e:fonts.entrySet()){
            if(n%2==0){ current=row(); fontWrap.addView(current); }
            final String name=e.getKey(),path=e.getValue(); Button b=action(name,v->{fontKey=path; applyFont(); savePrefs();}); current.addView(b,weight()); n++;
        } panel.addView(fontWrap);

        panel.addView(section("背景")); LinearLayout backs=row(); backs.addView(action("米白",v->{setColors(Color.rgb(248,244,232),Color.rgb(48,44,38));}),weight()); backs.addView(action("纯白",v->{setColors(Color.WHITE,Color.rgb(40,40,40));}),weight()); backs.addView(action("护眼",v->{setColors(Color.rgb(225,238,222),Color.rgb(42,58,43));}),weight()); panel.addView(backs);
        TextView close=label("完成",16); close.setGravity(Gravity.CENTER); close.setTextColor(Color.rgb(49,88,71)); close.setTypeface(Typeface.DEFAULT,Typeface.BOLD); close.setPadding(0,dp(16),0,dp(6)); close.setOnClickListener(v->d.dismiss()); panel.addView(close);

        d.setContentView(panel); Window w=d.getWindow(); if(w!=null){ w.setBackgroundDrawableResource(android.R.color.transparent); WindowManager.LayoutParams lp=new WindowManager.LayoutParams(); lp.copyFrom(w.getAttributes()); lp.width=(int)(getResources().getDisplayMetrics().widthPixels*0.92f); lp.height=WindowManager.LayoutParams.WRAP_CONTENT; lp.gravity=Gravity.BOTTOM; lp.y=dp(18); w.setAttributes(lp); }
        d.show();
    }

    private void applyAndSave(){ body.setTextSize(fontSize); body.setLineSpacing(dp(lineExtra),1f); savePrefs(); }
    private void setColors(int b,int f){ bg=b;fg=f; body.setBackgroundColor(bg);body.setTextColor(fg);scroll.setBackgroundColor(bg);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);savePrefs(); }
    private void applyFont(){ try{ Typeface tf; if(fontKey.startsWith("system:")) tf=Typeface.create(fontKey.substring(7),Typeface.NORMAL); else if(fontKey.equals("wenkai")) tf=Typeface.create("serif",Typeface.NORMAL); else tf=Typeface.createFromAsset(getAssets(),fontKey); body.setTypeface(tf); }catch(Throwable e){ body.setTypeface(Typeface.create("serif",Typeface.NORMAL)); } }
    private void savePrefs(){ getSharedPreferences("reader_settings",MODE_PRIVATE).edit().putFloat("size",fontSize).putFloat("line",lineExtra).putInt("bg",bg).putInt("fg",fg).putString("font",fontKey).apply(); }
    private void loadPrefs(){ android.content.SharedPreferences p=getSharedPreferences("reader_settings",MODE_PRIVATE);fontSize=p.getFloat("size",20);lineExtra=p.getFloat("line",9);bg=p.getInt("bg",Color.rgb(248,244,232));fg=p.getInt("fg",Color.rgb(48,44,38));fontKey=p.getString("font","wenkai"); }
    private TextView section(String s){ TextView v=label(s,14);v.setTextColor(Color.rgb(108,102,92));v.setPadding(0,dp(14),0,dp(7));return v; }
    private LinearLayout row(){ LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setPadding(0,0,0,dp(5));return r; }
    private LinearLayout.LayoutParams weight(){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);p.setMargins(dp(3),0,dp(3),0);return p; }
    private Button action(String s,View.OnClickListener l){ Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setAllCaps(false);b.setOnClickListener(l);return b; }
    private TextView label(String s,int size){ TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(fg);return v; }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
}
