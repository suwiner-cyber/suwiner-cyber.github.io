package com.xiaoxiaoshuo.reader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout root, content, nav;
    private final int green = Color.rgb(49,88,71);
    private final String[][] books = {
            {"长安的荔枝","马伯庸","大唐天宝年间，一名小吏接到几乎不可能完成的差事：把岭南鲜荔枝送到长安。路途遥远、期限严苛，他只能在制度与现实之间寻找一线生机。"},
            {"雪中悍刀行","烽火戏诸侯","江湖庙堂交织，少年从北凉王府走入天下。故事在侠义、家国与个人选择之间展开。"},
            {"庆余年","猫腻","一个带着现代记忆的年轻人进入陌生时代，在家族、朝堂与江湖之间寻找自己的道路。"},
            {"将夜","猫腻","边城少年入都城求学，从书院走向更辽阔的世界。成长、信念与命运在漫长旅途中不断碰撞。"}
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(247,244,238));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildShell();
        showHome();
    }

    private void buildShell() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(247,244,238));
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(-1,0,1));
        nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setPadding(dp(10),dp(8),dp(10),dp(10)); nav.setBackgroundColor(Color.WHITE);
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(72)));
        setContentView(root);
        renderNav(0);
    }

    private void renderNav(int active){
        nav.removeAllViews();
        String[] labels={"首页","发现","书架","我的"};
        String[] glyph={"⌂","✦","▣","●"};
        for(int i=0;i<4;i++){
            final int idx=i;
            TextView v=new TextView(this); v.setGravity(Gravity.CENTER); v.setText(glyph[i]+"\n"+labels[i]); v.setTextSize(13); v.setTypeface(Typeface.DEFAULT, i==active?Typeface.BOLD:Typeface.NORMAL); v.setTextColor(i==active?green:Color.rgb(110,110,110));
            v.setOnClickListener(x->{ if(idx==0)showHome(); else if(idx==1)showDiscover(); else if(idx==2)showShelf(); else showMe(); });
            nav.addView(v,new LinearLayout.LayoutParams(0,-1,1));
        }
    }

    private ScrollView page(String title,String subtitle,int active){
        content.removeAllViews(); renderNav(active);
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22),dp(22),dp(22),dp(30));
        TextView t=text(title,28,green,true); box.addView(t); TextView s=text(subtitle,14,Color.rgb(120,115,105),false); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2); sp.topMargin=dp(6); sp.bottomMargin=dp(18); box.addView(s,sp); sv.addView(box); content.addView(sv,new LinearLayout.LayoutParams(-1,-1)); return sv;
    }

    private LinearLayout bodyOf(ScrollView sv){ return (LinearLayout)sv.getChildAt(0); }

    private void showHome(){
        ScrollView sv=page("小小说","安静阅读，清爽一点。",0); LinearLayout box=bodyOf(sv);
        box.addView(section("今日推荐"));
        for(int i=0;i<2;i++) box.addView(bookCard(books[i],true));
        box.addView(section("阅读工具"));
        box.addView(actionCard("永久 AI 排版","正文只走一种排版链，不再在默认排版与 AI 排版之间跳动。",null));
        box.addView(actionCard("四导航统一","首页、发现、书架、我的使用同一套底部导航。",null));
    }

    private void showDiscover(){
        ScrollView sv=page("发现","简介统一净排版，不留异常空格。",1); LinearLayout box=bodyOf(sv);
        for(String[] b:books) box.addView(bookCard(new String[]{b[0],b[1],AiTypesetter.compactIntro(b[2])},true));
    }

    private void showShelf(){
        ScrollView sv=page("书架","保留简单稳定的本地书架。",2); LinearLayout box=bodyOf(sv);
        Set<String> shelf=getPreferences(MODE_PRIVATE).getStringSet("shelf",new LinkedHashSet<>());
        if(shelf.isEmpty()) { TextView e=text("书架还是空的。去“发现”添加一本书。",16,Color.DKGRAY,false); e.setPadding(0,dp(30),0,0); box.addView(e); return; }
        for(String title:shelf){ for(String[] b:books){ if(b[0].equals(title)) box.addView(bookCard(b,true)); } }
    }

    private void showMe(){
        ScrollView sv=page("我的","干净、独立、稳定。",3); LinearLayout box=bodyOf(sv);
        box.addView(actionCard("阅读设置","字号、字体、背景、行距都在阅读页内直接调整。",null));
        box.addView(actionCard("版本","10.0.0 原生重建分支",null));
        box.addView(actionCard("兼容目标","Android 7.0+，重点兼容华为 / HarmonyOS Android 兼容层。",null));
    }

    private View bookCard(String[] b, boolean buttons){
        LinearLayout card=card();
        TextView title=text(b[0],20,Color.rgb(37,45,41),true); card.addView(title);
        TextView author=text(b[1],13,Color.rgb(128,122,112),false); LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2); ap.topMargin=dp(3); card.addView(author,ap);
        TextView intro=text(AiTypesetter.compactIntro(b[2]),14,Color.rgb(78,78,74),false); intro.setLineSpacing(dp(3),1f); LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2); ip.topMargin=dp(10); card.addView(intro,ip);
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.END); row.setPadding(0,dp(12),0,0);
        Button add=button("加入书架"); add.setOnClickListener(v->{ Set<String> cur=new LinkedHashSet<>(getPreferences(MODE_PRIVATE).getStringSet("shelf",new LinkedHashSet<>())); cur.add(b[0]); getPreferences(MODE_PRIVATE).edit().putStringSet("shelf",cur).apply(); Toast.makeText(this,"已加入书架",Toast.LENGTH_SHORT).show(); });
        Button read=button("开始阅读"); read.setOnClickListener(v->{ Intent it=new Intent(this,ReaderActivity.class); it.putExtra("title",b[0]); it.putExtra("body",sampleBody(b)); startActivity(it); });
        row.addView(add); LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-2,-2); rp.leftMargin=dp(8); row.addView(read,rp); card.addView(row);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=dp(14); return wrap(card,cp);
    }

    private String sampleBody(String[] b){
        return "第一章 重新开始\n\n"+b[2]+"\n\n夜色从窗外慢慢落下来。屋里只留一盏灯，纸页上的字却比白天更清楚。\n他没有急着翻下一页，只是把上一段又读了一遍。过去那些凌乱的空格、突然跳动的段落和不同的排版方式，都不应该再打断阅读。\n\n第二章 安静的页面\n\n好的阅读器不应该让人注意到自己。字号、行距、字体和背景都可以改变，但正文的逻辑不该反复切换。于是这一次，正文从进入页面开始就只经过同一套排版引擎。";
    }

    private View wrap(View v, LinearLayout.LayoutParams p){ LinearLayout holder=new LinearLayout(this); holder.addView(v,p); return holder; }
    private LinearLayout card(){ LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(18),dp(16),dp(18),dp(16)); GradientDrawable g=new GradientDrawable(); g.setColor(Color.WHITE); g.setCornerRadius(dp(18)); c.setBackground(g); return c; }
    private View actionCard(String title,String sub,View.OnClickListener l){ LinearLayout c=card(); c.addView(text(title,17,Color.rgb(38,48,43),true)); TextView s=text(sub,14,Color.rgb(102,98,91),false); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(7); c.addView(s,p); if(l!=null)c.setOnClickListener(l); LinearLayout h=new LinearLayout(this); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.bottomMargin=dp(12); h.addView(c,hp); return h; }
    private TextView section(String s){ TextView v=text(s,18,Color.rgb(45,61,53),true); v.setPadding(0,dp(6),0,dp(10)); return v; }
    private TextView text(String s,int size,int color,boolean bold){ TextView v=new TextView(this); v.setText(s); v.setTextSize(size); v.setTextColor(color); v.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL)); v.setTextIsSelectable(false); return v; }
    private Button button(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(13); b.setAllCaps(false); b.setTextColor(green); return b; }
    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
}
