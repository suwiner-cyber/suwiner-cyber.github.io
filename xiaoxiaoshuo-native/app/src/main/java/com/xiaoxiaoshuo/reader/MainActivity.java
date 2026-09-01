package com.xiaoxiaoshuo.reader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.io.*;
import java.net.*;
import org.json.*;

public class MainActivity extends Activity {
    private LinearLayout root, content, nav;
    private final int green = Color.rgb(49,88,71);
    private LegacySourceStore.State sourceState;
    private final Handler quoteHandler=new Handler();
    private TextView quoteView;
    private int quoteIndex=0;
    private final String[] quotes={
            "日子缓缓，心里有光，寻常的烟火也会开出花。",
            "把今天过好，就是对明天最温柔的回答。",
            "愿你在琐碎的生活里，始终保留一方安静的月光。",
            "山高水长，慢一点也没关系，喜欢的日子会慢慢靠近。",
            "生活不是赶路，偶尔停下来，也能看见风吹过树梢。",
            "读几页书，喝一杯热茶，把心放回自己的生活里。"
    };

    private final String[][] books={
            {"长安的荔枝","马伯庸","大唐天宝年间，一名小吏接到几乎不可能完成的差事：把岭南鲜荔枝送到长安。路途遥远、期限严苛，他只能在制度与现实之间寻找一线生机。",""},
            {"雪中悍刀行","烽火戏诸侯","江湖庙堂交织，少年从北凉王府走入天下。故事在侠义、家国与个人选择之间展开。",""},
            {"庆余年","猫腻","一个带着现代记忆的年轻人进入陌生时代，在家族、朝堂与江湖之间寻找自己的道路。",""},
            {"将夜","猫腻","边城少年入都城求学，从书院走向更辽阔的世界。成长、信念与命运在漫长旅途中不断碰撞。",""}
    };

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(247,244,238));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        sourceState=LegacySourceStore.prepare(this,80);
        buildShell();
        showHome();
    }

    @Override protected void onDestroy(){ quoteHandler.removeCallbacksAndMessages(null); super.onDestroy(); }

    private void buildShell(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(247,244,238));
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        nav=new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setPadding(dp(10),dp(8),dp(10),dp(10)); nav.setBackgroundColor(Color.WHITE);
        root.addView(nav,new LinearLayout.LayoutParams(-1,dp(72))); setContentView(root); renderNav(0);
    }

    private void renderNav(int active){
        nav.removeAllViews(); String[] labels={"首页","发现","书架","我的"}; String[] glyph={"⌂","✦","▣","●"};
        for(int i=0;i<4;i++){ final int idx=i; TextView v=new TextView(this); v.setGravity(Gravity.CENTER); v.setText(glyph[i]+"\n"+labels[i]); v.setTextSize(13); v.setTypeface(Typeface.DEFAULT,i==active?Typeface.BOLD:Typeface.NORMAL); v.setTextColor(i==active?green:Color.rgb(110,110,110)); v.setOnClickListener(x->{ if(idx==0)showHome(); else if(idx==1)showDiscover(); else if(idx==2)showShelf(); else showMe(); }); nav.addView(v,new LinearLayout.LayoutParams(0,-1,1)); }
    }

    private ScrollView page(String title,String subtitle,int active){
        quoteHandler.removeCallbacksAndMessages(null); content.removeAllViews(); renderNav(active);
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22),dp(22),dp(22),dp(30));
        box.addView(text(title,28,green,true)); TextView s=text(subtitle,14,Color.rgb(120,115,105),false); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2); sp.topMargin=dp(6); sp.bottomMargin=dp(18); box.addView(s,sp); sv.addView(box); content.addView(sv,new LinearLayout.LayoutParams(-1,-1)); return sv;
    }
    private LinearLayout bodyOf(ScrollView sv){return (LinearLayout)sv.getChildAt(0);}

    private void showHome(){
        ScrollView sv=page("小小说","把阅读放回生活里。",0); LinearLayout box=bodyOf(sv);
        box.addView(homeHero());
        box.addView(section("今日推荐"));
        ArrayList<LegacySourceStore.BookInfo> legacy=LegacySourceStore.loadLegacyBooks(this,4);
        if(!legacy.isEmpty()){
            for(int i=0;i<Math.min(4,legacy.size());i++){ LegacySourceStore.BookInfo b=legacy.get(i); box.addView(bookCard(new String[]{b.title,b.author,b.intro,b.cover})); }
        }else{
            for(int i=0;i<books.length;i++)box.addView(bookCard(books[i]));
        }
    }

    private View homeHero(){
        LinearLayout card=card(); card.setPadding(dp(18),dp(18),dp(18),dp(18));
        LinearLayout tags=new LinearLayout(this); tags.setOrientation(LinearLayout.HORIZONTAL); tags.setPadding(0,0,0,dp(12));
        String[] names={"文学","治愈","生活","今日共读"}; for(String n:names){ TextView t=text(n,12,green,true); GradientDrawable g=new GradientDrawable(); g.setColor(Color.rgb(236,242,238)); g.setCornerRadius(dp(20)); t.setBackground(g); t.setGravity(Gravity.CENTER); t.setPadding(dp(10),dp(5),dp(10),dp(5)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2); p.rightMargin=dp(7); tags.addView(t,p); }
        card.addView(tags);
        quoteView=text(quotes[quoteIndex%quotes.length],19,Color.rgb(48,57,52),true); quoteView.setLineSpacing(dp(4),1f); card.addView(quoteView);
        TextView hint=text("每 7 秒换一句 · 经典 / 文艺 / 生活 / 暖心",12,Color.rgb(132,125,114),false); LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2); hp.topMargin=dp(7); card.addView(hint,hp);
        card.addView(new WarmIllustrationView(this),new LinearLayout.LayoutParams(-1,dp(150)));
        quoteHandler.postDelayed(new Runnable(){public void run(){ if(quoteView==null)return; quoteIndex=(quoteIndex+1)%quotes.length; quoteView.animate().alpha(0f).setDuration(180).withEndAction(()->{quoteView.setText(quotes[quoteIndex]); quoteView.animate().alpha(1f).setDuration(220).start();}).start(); quoteHandler.postDelayed(this,7000); }},7000);
        LinearLayout holder=new LinearLayout(this); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(18); holder.addView(card,p); return holder;
    }

    private void showDiscover(){
        ScrollView sv=page("发现","优质书源优先，简介统一经过 AI 净排版。",1); LinearLayout box=bodyOf(sv);
        TextView source=text(sourceState==null?"书源准备中":sourceState.message,13,Color.rgb(112,106,96),false); source.setPadding(0,0,0,dp(12)); box.addView(source);
        for(String[] b:books)box.addView(bookCard(b));
        fetchDiscover(box);
    }

    private void fetchDiscover(LinearLayout box){
        new Thread(()->{
            try{
                URL u=new URL("https://raw.githubusercontent.com/suwiner-cyber/suwiner-cyber.github.io/xiaoxiaoshuo-native-rebuild/xiaoxiaoshuo-native/discover.json"); HttpURLConnection c=(HttpURLConnection)u.openConnection(); c.setConnectTimeout(6000); c.setReadTimeout(6000); c.setRequestProperty("User-Agent","XiaoXiaoShuo/10.0");
                BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8")); StringBuilder sb=new StringBuilder(); String line; while((line=r.readLine())!=null)sb.append(line); r.close(); JSONArray a=new JSONArray(sb.toString()); final ArrayList<String[]> remote=new ArrayList<>();
                for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i); remote.add(new String[]{o.optString("title"),o.optString("author"),AiTypesetter.compactIntro(o.optString("intro")),o.optString("cover")});}
                runOnUiThread(()->{ if(remote.isEmpty())return; while(box.getChildCount()>3)box.removeViewAt(box.getChildCount()-1); for(String[] b:remote)box.addView(bookCard(b)); });
            }catch(Throwable ignored){}
        }).start();
    }

    private void showShelf(){
        ScrollView sv=page("书架","旧书架数据优先恢复，封面继续沿用原 cover_url。",2); LinearLayout box=bodyOf(sv);
        ArrayList<LegacySourceStore.BookInfo> legacy=LegacySourceStore.loadLegacyBooks(this,80);
        if(!legacy.isEmpty()){ for(LegacySourceStore.BookInfo b:legacy)box.addView(bookCard(new String[]{b.title,b.author,b.intro,b.cover})); return; }
        Set<String> shelf=getPreferences(MODE_PRIVATE).getStringSet("shelf",new LinkedHashSet<>()); if(shelf.isEmpty()){TextView e=text("书架还是空的。去“发现”添加一本书。",16,Color.DKGRAY,false);e.setPadding(0,dp(30),0,0);box.addView(e);return;} for(String title:shelf)for(String[] b:books)if(b[0].equals(title))box.addView(bookCard(b));
    }

    private void showMe(){
        ScrollView sv=page("我的","书源、阅读和数据都单独管理。",3); LinearLayout box=bodyOf(sv);
        int total=sourceState==null?0:sourceState.all.size(); int selected=sourceState==null?0:sourceState.selected.size();
        box.addView(actionCard("外部书源","检测到 "+total+" 个旧外部书源 · 智能默认加载 "+selected+" 个优质书源"));
        if(sourceState!=null && !sourceState.selected.isEmpty()){
            StringBuilder names=new StringBuilder(); for(int i=0;i<Math.min(12,sourceState.selected.size());i++){ if(i>0)names.append(" · "); String n=sourceState.selected.get(i).name; names.append(n.length()==0?sourceState.selected.get(i).url:n); }
            box.addView(actionCard("优选书源示例",names.toString()));
        }
        box.addView(actionCard("阅读设置","字号、字体、背景、行距都在阅读页内直接调整。")); box.addView(actionCard("版本","10.0.2 华为兼容原生重建分支"));
    }

    private View bookCard(String[] b){
        LinearLayout card=card(); LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); ImageView cover=new ImageView(this); cover.setScaleType(ImageView.ScaleType.CENTER_CROP); GradientDrawable cg=new GradientDrawable(); cg.setColor(Color.rgb(228,219,199)); cg.setCornerRadius(dp(12)); cover.setBackground(cg); cover.setImageDrawable(new LetterCoverDrawable(b[0])); row.addView(cover,new LinearLayout.LayoutParams(dp(88),dp(124)));
        LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(0,-2,1); fp.leftMargin=dp(14); row.addView(info,fp); info.addView(text(b[0],20,Color.rgb(37,45,41),true)); TextView author=text(b[1],13,Color.rgb(128,122,112),false); LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2); ap.topMargin=dp(3); info.addView(author,ap); TextView intro=text(AiTypesetter.compactIntro(b[2]),14,Color.rgb(78,78,74),false); intro.setMaxLines(4); intro.setLineSpacing(dp(3),1f); LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2); ip.topMargin=dp(10); info.addView(intro,ip); card.addView(row);
        if(b.length>3 && b[3]!=null && (b[3].startsWith("http://")||b[3].startsWith("https://")))loadCover(cover,b[3]);
        LinearLayout actions=new LinearLayout(this); actions.setGravity(Gravity.END); actions.setPadding(0,dp(12),0,0); Button add=button("加入书架"); add.setOnClickListener(v->{Set<String> cur=new LinkedHashSet<>(getPreferences(MODE_PRIVATE).getStringSet("shelf",new LinkedHashSet<>()));cur.add(b[0]);getPreferences(MODE_PRIVATE).edit().putStringSet("shelf",cur).apply();Toast.makeText(this,"已加入书架",Toast.LENGTH_SHORT).show();}); Button read=button("开始阅读"); read.setOnClickListener(v->{Intent it=new Intent(this,ReaderActivity.class);it.putExtra("title",b[0]);it.putExtra("body",sampleBody(b));startActivity(it);}); actions.addView(add); LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-2,-2); rp.leftMargin=dp(8); actions.addView(read,rp); card.addView(actions);
        LinearLayout holder=new LinearLayout(this); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2); cp.bottomMargin=dp(14); holder.addView(card,cp); return holder;
    }

    private void loadCover(ImageView target,String url){ new Thread(()->{ try{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(5000);c.setReadTimeout(7000);c.setInstanceFollowRedirects(true);c.setRequestProperty("User-Agent","Mozilla/5.0 XiaoXiaoShuo");InputStream in=c.getInputStream();final Bitmap bm=BitmapFactory.decodeStream(in);in.close();if(bm!=null)runOnUiThread(()->target.setImageBitmap(bm));}catch(Throwable ignored){} }).start(); }
    private String sampleBody(String[] b){return "第一章 重新开始\n\n"+b[2]+"\n\n夜色从窗外慢慢落下来。屋里只留一盏灯，纸页上的字却比白天更清楚。\n\n好的阅读器不应该让人注意到自己。字号、行距、字体和背景都可以改变，但正文始终只经过同一套 AI 排版引擎。";}
    private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(18),dp(16),dp(18),dp(16));GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(18));c.setBackground(g);return c;}
    private View actionCard(String title,String sub){LinearLayout c=card();c.addView(text(title,17,Color.rgb(38,48,43),true));TextView s=text(sub,14,Color.rgb(102,98,91),false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(7);c.addView(s,p);LinearLayout h=new LinearLayout(this);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.bottomMargin=dp(12);h.addView(c,hp);return h;}
    private TextView section(String s){TextView v=text(s,18,Color.rgb(45,61,53),true);v.setPadding(0,dp(6),0,dp(10));return v;}
    private TextView text(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s==null?"":s);v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setAllCaps(false);b.setTextColor(green);return b;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}

    private class WarmIllustrationView extends View{
        Paint p=new Paint(3); public WarmIllustrationView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);} protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();p.setColor(Color.rgb(239,232,218));c.drawRoundRect(w*.05f,h*.20f,w*.95f,h*.92f,dp(24),dp(24),p);p.setColor(Color.rgb(91,119,99));c.drawCircle(w*.77f,h*.39f,dp(31),p);p.setColor(Color.rgb(250,246,237));c.drawRoundRect(w*.28f,h*.46f,w*.72f,h*.81f,dp(10),dp(10),p);p.setColor(Color.rgb(196,173,134));c.drawRect(w*.49f,h*.46f,w*.51f,h*.81f,p);p.setColor(Color.rgb(74,101,83));p.setStrokeWidth(dp(4));c.drawLine(w*.14f,h*.78f,w*.28f,h*.61f,p);c.drawLine(w*.14f,h*.78f,w*.22f,h*.46f,p);c.drawCircle(w*.22f,h*.46f,dp(8),p);c.drawCircle(w*.27f,h*.58f,dp(7),p);p.setColor(Color.rgb(69,75,70));p.setTextSize(dp(15));p.setTypeface(Typeface.create("serif",Typeface.BOLD));c.drawText("读书，也是在照顾生活",w*.12f,h*.18f,p);}}
    private class LetterCoverDrawable extends Drawable{
        Paint p=new Paint(3); String title; LetterCoverDrawable(String t){title=t==null?"书":t;} public void draw(Canvas c){Rect b=getBounds();p.setColor(Color.rgb(223,211,187));c.drawRoundRect(new RectF(b),dp(10),dp(10),p);p.setColor(green);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(Math.max(dp(14),b.width()/6f));String s=title.length()>4?title.substring(0,4):title;float y=b.centerY()-(p.ascent()+p.descent())/2;c.drawText(s,b.centerX(),y,p);} public void setAlpha(int a){p.setAlpha(a);} public void setColorFilter(android.graphics.ColorFilter f){p.setColorFilter(f);} public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}}
}
