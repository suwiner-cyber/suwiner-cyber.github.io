package com.xiaoxiaoshuo.reader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.text.TextPaint;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;

public class ReaderActivity extends Activity {
    private LinearLayout root,top,bottom;
    private TextView body,title,chapterTitle,progress,back,catalogBtn,setup,prev,toc,next;
    private float fontSize=20f,lineExtra=9f;
    private int bg=Color.rgb(248,244,232),fg=Color.rgb(48,44,38);
    private String fontKey="wenkai",catalogKey="",sourceJson="",bookTitle="";
    private final LinkedHashMap<String,String> fonts=new LinkedHashMap<>();
    private final ArrayList<String> pages=new ArrayList<>();
    private CatalogStore.Data catalog;
    private int chapterIndex=0,pageIndex=0;
    private volatile int loadToken=0;
    private boolean immersive=false,pageSound=true,turning=false;
    private GestureDetector gestures;
    private String currentChapterText="";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        fonts.put("文楷","fonts/LXGWWenKaiLite-Regular.ttf");fonts.put("文楷中黑","fonts/LXGWWenKaiLite-Medium.ttf");fonts.put("文楷轻体","fonts/LXGWWenKaiLite-Light.ttf");fonts.put("等宽文楷","fonts/LXGWWenKaiMonoLite-Regular.ttf");fonts.put("系统黑体","system:sans");
        loadPrefs();catalogKey=n(getIntent().getStringExtra("catalog_key"));sourceJson=n(getIntent().getStringExtra("source_json"));bookTitle=n(getIntent().getStringExtra("title"));chapterIndex=getIntent().getIntExtra("chapter_index",0);
        if(catalogKey.length()>0)catalog=CatalogStore.load(this,catalogKey);build();setupGestures();
        if(catalog!=null&&!catalog.chapters.isEmpty()){if(sourceJson.length()==0)sourceJson=catalog.sourceJson;int initial=Math.max(0,Math.min(chapterIndex,catalog.chapters.size()-1));int savedPage=getSharedPreferences("reading_progress",MODE_PRIVATE).getInt(pageKey(),0);loadChapter(initial,savedPage);}else{String raw=getIntent().getStringExtra("body");setChapterText(AiTypesetter.formatNovel(raw==null?"正文加载失败。":raw),0);}setImmersive(immersive,false);
    }
    private String n(String s){return s==null?"":s;}

    private void build(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(10),dp(7),dp(10),dp(6));
        back=label("‹",32);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(44),dp(50)));
        title=label(bookTitle.length()==0?"阅读":bookTitle,16);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER_VERTICAL);title.setSingleLine(true);top.addView(title,new LinearLayout.LayoutParams(0,dp(50),1));
        catalogBtn=label("目录",14);catalogBtn.setGravity(Gravity.CENTER);catalogBtn.setOnClickListener(v->showCatalog());top.addView(catalogBtn,new LinearLayout.LayoutParams(dp(58),dp(50)));
        setup=label("Aa",17);setup.setGravity(Gravity.CENTER);setup.setOnClickListener(v->showSettings());top.addView(setup,new LinearLayout.LayoutParams(dp(54),dp(50)));root.addView(top);
        chapterTitle=label("",13);chapterTitle.setGravity(Gravity.CENTER);root.addView(chapterTitle,new LinearLayout.LayoutParams(-1,dp(30)));
        body=new TextView(this);body.setPadding(dp(26),dp(16),dp(26),dp(20));body.setTextSize(fontSize);body.setLineSpacing(dp(lineExtra),1f);body.setTextIsSelectable(true);body.setGravity(Gravity.TOP|Gravity.LEFT);applyFont();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER);bottom.setPadding(dp(10),dp(5),dp(10),dp(6));prev=nav("‹ 上一页");prev.setOnClickListener(v->previousPage());toc=nav("目录");toc.setOnClickListener(v->showCatalog());next=nav("下一页 ›");next.setOnClickListener(v->nextPage());bottom.addView(prev,new LinearLayout.LayoutParams(0,dp(44),1));bottom.addView(toc,new LinearLayout.LayoutParams(0,dp(44),1));bottom.addView(next,new LinearLayout.LayoutParams(0,dp(44),1));root.addView(bottom);
        progress=label("",11);progress.setGravity(Gravity.CENTER);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(20)));setContentView(root);applyTheme();
    }

    private void setupGestures(){gestures=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(MotionEvent e){return true;}@Override public boolean onDoubleTap(MotionEvent e){if(immersive){setImmersive(false,true);return true;}return false;}@Override public boolean onFling(MotionEvent e1,MotionEvent e2,float vx,float vy){if(e1==null||e2==null)return false;float dx=e2.getX()-e1.getX(),dy=e2.getY()-e1.getY();if(Math.abs(dx)<dp(55)||Math.abs(dx)<Math.abs(dy)*1.25f)return false;if(dx<0)nextPage();else previousPage();return true;}});}
    @Override public boolean dispatchTouchEvent(MotionEvent e){if(gestures!=null)gestures.onTouchEvent(e);return super.dispatchTouchEvent(e);}

    private void nextPage(){if(turning||pages.isEmpty())return;if(pageIndex<pages.size()-1){naturalTurn(1,()->{pageIndex++;body.setText(pages.get(pageIndex));updateProgress();saveProgress();});return;}if(catalog!=null&&chapterIndex<catalog.chapters.size()-1){naturalTurn(1,()->loadChapter(chapterIndex+1,0));}else Toast.makeText(this,"已经是最后一页",Toast.LENGTH_SHORT).show();}
    private void previousPage(){if(turning||pages.isEmpty())return;if(pageIndex>0){naturalTurn(-1,()->{pageIndex--;body.setText(pages.get(pageIndex));updateProgress();saveProgress();});return;}if(catalog!=null&&chapterIndex>0){naturalTurn(-1,()->loadChapter(chapterIndex-1,Integer.MAX_VALUE));}else Toast.makeText(this,"已经是第一页",Toast.LENGTH_SHORT).show();}

    private void naturalTurn(int direction,Runnable swap){
        if(turning)return;turning=true;playPageSound();body.animate().cancel();
        float w=Math.max(dp(240),body.getWidth());float outX=direction>0?-w*.22f:w*.22f;float inX=direction>0?w*.13f:-w*.13f;
        body.setPivotX(direction>0?body.getWidth():0);body.setPivotY(body.getHeight()*.52f);body.setElevation(dp(2));
        body.animate().translationX(outX).scaleX(.992f).scaleY(.998f).alpha(.46f).setDuration(145).setInterpolator(new AccelerateDecelerateInterpolator()).withEndAction(()->{
            try{swap.run();}catch(Throwable ignored){}
            body.setTranslationX(inX);body.setScaleX(.995f);body.setScaleY(.998f);body.setAlpha(.68f);body.setElevation(dp(5));
            body.animate().translationX(0).scaleX(1f).scaleY(1f).alpha(1f).setDuration(205).setInterpolator(new DecelerateInterpolator(1.7f)).withEndAction(()->{turning=false;body.setElevation(0);body.setPivotX(body.getWidth()/2f);body.setTranslationX(0);body.setAlpha(1);body.setScaleX(1);body.setScaleY(1);}).start();
        }).start();
    }
    private void playPageSound(){if(pageSound)PageTurnSound.play(this);}

    private void setImmersive(boolean on,boolean persist){immersive=on;if(top!=null)top.setVisibility(on?View.GONE:View.VISIBLE);if(chapterTitle!=null)chapterTitle.setVisibility(on?View.GONE:View.VISIBLE);if(bottom!=null)bottom.setVisibility(on?View.GONE:View.VISIBLE);if(progress!=null)progress.setVisibility(on?View.GONE:View.VISIBLE);if(body!=null)body.setPadding(dp(26),on?dp(28):dp(16),dp(26),on?dp(30):dp(20));int ui=on?(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE):(bestTextColor(bg)==Color.rgb(48,44,38)?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:0);getWindow().getDecorView().setSystemUiVisibility(ui);if(persist)getSharedPreferences("reader_settings",MODE_PRIVATE).edit().putBoolean("immersive",immersive).apply();body.postDelayed(()->paginateAndShow(Math.min(pageIndex,Math.max(0,pages.size()-1))),120);}

    private void loadChapter(int idx,int wantedPage){
        if(catalog==null||catalog.chapters.isEmpty())return;if(idx<0||idx>=catalog.chapters.size())return;chapterIndex=idx;LegacyDexBridge.Chapter ch=catalog.chapters.get(idx);chapterTitle.setText(ch.name);pages.clear();body.setTranslationX(0);body.setAlpha(1);body.setText("正在加载正文…");updateProgress();int token=++loadToken;final int wantedIndex=idx;final String wantedName=ch.name,initialUrl=ch.url,bt=bookTitle.length()>0?bookTitle:title.getText().toString();String cached=OfflineBookCache.getChapter(this,bt,idx,initialUrl);if(cached.trim().length()>0){setChapterText(AiTypesetter.formatNovel(cached),wantedPage);return;}
        new Thread(()->{String text="",switched="";CatalogStore.Data newCatalog=null;String newSource=sourceJson;int newIndex=wantedIndex;String cacheUrl=initialUrl;try{text=LegacyDexBridge.get(this).loadChapter(sourceJson,wantedName,initialUrl);}catch(Throwable ignored){}if(text.trim().length()==0){Alternate alt=findAlternate(bt,wantedName,wantedIndex);if(alt!=null&&alt.text.trim().length()>0){text=alt.text;switched=alt.sourceName;newCatalog=alt.catalog;newSource=alt.sourceJson;newIndex=alt.index;cacheUrl=alt.catalog.chapters.get(alt.index).url;}}if(text.trim().length()>0)OfflineBookCache.putChapter(this,bt,newIndex,cacheUrl,text);final String ready=text,sourceName=switched,finalSource=newSource;final CatalogStore.Data finalCatalog=newCatalog;final int finalIndex=newIndex;runOnUiThread(()->{if(token!=loadToken)return;if(finalCatalog!=null){catalog=finalCatalog;sourceJson=finalSource;chapterIndex=finalIndex;catalogKey=CatalogStore.save(this,bt,sourceJson,catalog.chapters);chapterTitle.setText(catalog.chapters.get(chapterIndex).name);if(sourceName.length()>0)Toast.makeText(this,"已自动切换书源："+sourceName,Toast.LENGTH_SHORT).show();}String formatted=ready.trim().length()==0?"本章正文加载失败，已自动尝试全部优选书源，暂未找到可读正文。":AiTypesetter.formatNovel(ready);setChapterText(formatted,wantedPage);});}).start();
    }

    private void setChapterText(String text,int wantedPage){currentChapterText=text==null?"":text;body.post(()->paginateAndShow(wantedPage));}
    private void paginateAndShow(int wantedPage){
        if(body.getWidth()<=0||body.getHeight()<=0){body.postDelayed(()->paginateAndShow(wantedPage),80);return;}pages.clear();String text=currentChapterText==null?"":currentChapterText;if(text.length()==0){pages.add("");pageIndex=0;renderPage();return;}int width=body.getWidth()-body.getPaddingLeft()-body.getPaddingRight(),height=body.getHeight()-body.getPaddingTop()-body.getPaddingBottom();if(width<dp(80)||height<dp(100)){body.postDelayed(()->paginateAndShow(wantedPage),100);return;}
        try{TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setTextSize(body.getTextSize());paint.setTypeface(body.getTypeface());paint.setColor(fg);StaticLayout layout=StaticLayout.Builder.obtain(text,0,text.length(),paint,width).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(dp(lineExtra),1f).setIncludePad(false).build();int startLine=0,totalLines=layout.getLineCount();while(startLine<totalLines){int pageTop=layout.getLineTop(startLine),endLine=startLine;while(endLine+1<totalLines&&layout.getLineBottom(endLine+1)-pageTop<=height)endLine++;int start=layout.getLineStart(startLine),end=layout.getLineEnd(endLine);if(end<=start)end=Math.min(text.length(),start+1);pages.add(text.substring(start,end));startLine=endLine+1;}}catch(Throwable e){int approx=Math.max(260,(width/dp(20))*Math.max(8,height/dp(34)));for(int p=0;p<text.length();p+=approx)pages.add(text.substring(p,Math.min(text.length(),p+approx)));}
        if(pages.isEmpty())pages.add(text);pageIndex=wantedPage==Integer.MAX_VALUE?pages.size()-1:Math.max(0,Math.min(wantedPage,pages.size()-1));renderPage();saveProgress();
    }
    private void renderPage(){if(pages.isEmpty())return;pageIndex=Math.max(0,Math.min(pageIndex,pages.size()-1));body.animate().cancel();body.setTranslationX(0);body.setScaleX(1);body.setScaleY(1);body.setAlpha(1);body.setElevation(0);body.setText(pages.get(pageIndex));updateProgress();}
    private void updateProgress(){int chTotal=catalog==null?0:catalog.chapters.size(),pgTotal=Math.max(1,pages.size());progress.setText((chapterIndex+1)+" / "+Math.max(1,chTotal)+"  ·  第 "+(pageIndex+1)+" / "+pgTotal+" 页");}

    private Alternate findAlternate(String bt,String chapterName,int index){LegacySourceStore.State st=LegacySourceStore.prepare(this,80);String currentUrl=sourceUrl(sourceJson);for(LegacySourceStore.SourceInfo s:st.selected){if(s.url.equals(currentUrl))continue;long start=System.currentTimeMillis();try{List<LegacyDexBridge.BookResult> list=LegacyDexBridge.get(this).search(s,bt,1);LegacyDexBridge.BookResult best=null;for(LegacyDexBridge.BookResult b:list)if(norm(b.title).equals(norm(bt))){best=b;break;}if(best==null)continue;BookSourceResolver.Resolved resolved=BookSourceResolver.inspect(this,best);if(resolved==null||resolved.chapters.isEmpty())continue;int target=findChapter(resolved.chapters,chapterName,index);LegacyDexBridge.Chapter ch=resolved.chapters.get(target);String text=LegacyDexBridge.get(this).loadChapter(resolved.book.sourceJson,ch.name,ch.url);if(text!=null&&text.trim().length()>0){LegacySourceStore.recordHealth(this,s.url,true,System.currentTimeMillis()-start);CatalogStore.Data d=new CatalogStore.Data();d.title=bt;d.sourceJson=resolved.book.sourceJson;d.chapters.addAll(resolved.chapters);OfflineBookCache.saveBook(this,resolved.book,resolved.chapters);Alternate a=new Alternate();a.text=text;a.sourceJson=resolved.book.sourceJson;a.sourceName=resolved.book.sourceName.length()>0?resolved.book.sourceName:s.name;a.catalog=d;a.index=target;return a;}}catch(Throwable ex){LegacySourceStore.recordHealth(this,s.url,false,System.currentTimeMillis()-start);}}return null;}
    private int findChapter(List<LegacyDexBridge.Chapter> list,String name,int fallback){String x=normChapter(name);for(int i=0;i<list.size();i++)if(normChapter(list.get(i).name).equals(x))return i;return Math.max(0,Math.min(fallback,list.size()-1));}
    private String sourceUrl(String json){try{return new JSONObject(json).optString("bookSourceUrl","");}catch(Throwable e){return "";}}
    private String norm(String s){return s==null?"":s.replace(" ","").replace("《","").replace("》","").toLowerCase(Locale.ROOT);}
    private String normChapter(String s){return s==null?"":s.replaceAll("[\\s　]+","").replaceAll("[第章节卷回部篇]","").toLowerCase(Locale.ROOT);}
    private static final class Alternate{String text="",sourceJson="",sourceName="";CatalogStore.Data catalog;int index;}

    private void saveProgress(){getSharedPreferences("reading_progress",MODE_PRIVATE).edit().putInt("idx_"+Integer.toHexString((bookTitle.length()>0?bookTitle:title.getText().toString()).hashCode()),chapterIndex).putInt(pageKey(),pageIndex).apply();}
    private String pageKey(){return "page_"+Integer.toHexString((bookTitle.length()==0?"阅读":bookTitle).hashCode());}

    private void showCatalog(){if(catalog==null||catalog.chapters.isEmpty()){Toast.makeText(this,"目录还没有加载完成",Toast.LENGTH_SHORT).show();return;}Dialog d=new Dialog(this);LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(14),dp(12),dp(14),dp(12));panel.setBackgroundColor(bg);TextView h=label("章节目录 · "+catalog.chapters.size()+"章",19);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);panel.addView(h);ScrollView sv=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);for(int i=0;i<catalog.chapters.size();i++){final int q=i;TextView r=label((i+1)+"  "+catalog.chapters.get(i).name,14);r.setPadding(dp(10),dp(12),dp(10),dp(12));if(i==chapterIndex){r.setTextColor(navColor());r.setTypeface(Typeface.DEFAULT,Typeface.BOLD);}r.setOnClickListener(v->{d.dismiss();loadChapter(q,0);});list.addView(r);}sv.addView(list);panel.addView(sv,new LinearLayout.LayoutParams(-1,0,1));d.setContentView(panel);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.copyFrom(w.getAttributes());lp.width=(int)(getResources().getDisplayMetrics().widthPixels*.92f);lp.height=(int)(getResources().getDisplayMetrics().heightPixels*.78f);lp.gravity=Gravity.BOTTOM;w.setAttributes(lp);}}

    private void showSettings(){final Dialog d=new Dialog(this);LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(22),dp(18),dp(22),dp(20));GradientDrawable pbg=new GradientDrawable();pbg.setColor(Color.rgb(255,253,249));pbg.setCornerRadius(dp(24));panel.setBackground(pbg);panel.addView(dark("阅读设置",22,true));LinearLayout im=row();TextView imText=dark("沉浸式阅读\n隐藏导航 · 左右滑页 · 双击退出",14,false);im.addView(imText,new LinearLayout.LayoutParams(0,dp(58),1));Switch sw=new Switch(this);sw.setChecked(immersive);im.addView(sw,new LinearLayout.LayoutParams(dp(64),dp(58)));panel.addView(im);sw.setOnCheckedChangeListener((v,on)->{setImmersive(on,true);if(on)d.dismiss();});LinearLayout snd=row();TextView sndText=dark("翻页声音\n轻柔纸张摩擦声",14,false);snd.addView(sndText,new LinearLayout.LayoutParams(0,dp(58),1));Switch sound=new Switch(this);sound.setChecked(pageSound);snd.addView(sound,new LinearLayout.LayoutParams(dp(64),dp(58)));panel.addView(snd);sound.setOnCheckedChangeListener((v,on)->{pageSound=on;savePrefs();if(on)PageTurnSound.play(this);});
        panel.addView(sectionDark("字号"));LinearLayout sizes=row();sizes.addView(action("A−",v->{fontSize=Math.max(15,fontSize-1);applyAndSave();}),weight());sizes.addView(action("A+",v->{fontSize=Math.min(32,fontSize+1);applyAndSave();}),weight());sizes.addView(action("默认",v->{fontSize=20;lineExtra=9;applyAndSave();}),weight());panel.addView(sizes);
        panel.addView(sectionDark("行距"));LinearLayout lines=row();lines.addView(action("紧凑",v->{lineExtra=5;applyAndSave();}),weight());lines.addView(action("舒适",v->{lineExtra=9;applyAndSave();}),weight());lines.addView(action("宽松",v->{lineExtra=14;applyAndSave();}),weight());panel.addView(lines);
        panel.addView(sectionDark("字体"));LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);int k=0;LinearLayout cur=null;for(Map.Entry<String,String> e:fonts.entrySet()){if(k%2==0){cur=row();wrap.addView(cur);}final String path=e.getValue();cur.addView(action(e.getKey(),v->{fontKey=path;applyFont();savePrefs();body.post(()->paginateAndShow(Math.min(pageIndex,Math.max(0,pages.size()-1))));}),weight());k++;}panel.addView(wrap);
        panel.addView(sectionDark("背景"));LinearLayout backs=row();backs.addView(action("米白",v->setBackgroundTheme(Color.rgb(248,244,232))),weight());backs.addView(action("纯白",v->setBackgroundTheme(Color.WHITE)),weight());backs.addView(action("护眼",v->setBackgroundTheme(Color.rgb(225,238,222))),weight());panel.addView(backs);Button custom=action("自定义背景颜色",v->showCustomColorDialog());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(46));cp.setMargins(dp(3),dp(6),dp(3),0);panel.addView(custom,cp);TextView close=dark("完成",16,true);close.setGravity(Gravity.CENTER);close.setTextColor(Color.rgb(49,88,71));close.setPadding(0,dp(16),0,dp(6));close.setOnClickListener(v->d.dismiss());panel.addView(close);d.setContentView(panel);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.copyFrom(w.getAttributes());lp.width=(int)(getResources().getDisplayMetrics().widthPixels*.92f);lp.height=WindowManager.LayoutParams.WRAP_CONTENT;lp.gravity=Gravity.BOTTOM;w.setAttributes(lp);}}

    private void showCustomColorDialog(){final EditText e=new EditText(this);e.setSingleLine(true);e.setHint("例如 #F4EBD8");e.setText(String.format(Locale.US,"#%06X",0xFFFFFF&bg));e.setSelectAllOnFocus(true);new AlertDialog.Builder(this).setTitle("自定义阅读背景").setView(e).setNegativeButton("取消",null).setPositiveButton("应用",(d,w)->{try{String raw=e.getText().toString().trim();if(!raw.startsWith("#"))raw="#"+raw;setBackgroundTheme(Color.parseColor(raw));}catch(Throwable ex){Toast.makeText(this,"颜色格式不正确，例如 #F4EBD8",Toast.LENGTH_LONG).show();}}).show();}
    private void setBackgroundTheme(int color){bg=color;fg=bestTextColor(color);applyTheme();savePrefs();}
    private int bestTextColor(int color){double y=.299*Color.red(color)+.587*Color.green(color)+.114*Color.blue(color);return y<145?Color.rgb(238,238,234):Color.rgb(48,44,38);}
    private void applyTheme(){if(root!=null)root.setBackgroundColor(bg);if(top!=null)top.setBackgroundColor(bg);if(bottom!=null)bottom.setBackgroundColor(bg);if(body!=null){body.setBackgroundColor(bg);body.setTextColor(fg);}if(title!=null)title.setTextColor(fg);if(back!=null)back.setTextColor(fg);if(catalogBtn!=null)catalogBtn.setTextColor(fg);if(setup!=null)setup.setTextColor(fg);if(chapterTitle!=null){chapterTitle.setBackgroundColor(bg);chapterTitle.setTextColor(mutedColor());}if(progress!=null){progress.setBackgroundColor(bg);progress.setTextColor(mutedColor());}if(prev!=null){prev.setBackgroundColor(bg);prev.setTextColor(navColor());}if(toc!=null){toc.setBackgroundColor(bg);toc.setTextColor(navColor());}if(next!=null){next.setBackgroundColor(bg);next.setTextColor(navColor());}getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);if(!immersive)getWindow().getDecorView().setSystemUiVisibility(bestTextColor(bg)==Color.rgb(48,44,38)?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:0);}
    private int navColor(){return bestTextColor(bg)==Color.rgb(48,44,38)?Color.rgb(49,88,71):Color.rgb(196,224,206);}
    private int mutedColor(){return bestTextColor(bg)==Color.rgb(48,44,38)?Color.rgb(125,115,99):Color.rgb(190,188,180);}
    private void applyAndSave(){body.setTextSize(fontSize);body.setLineSpacing(dp(lineExtra),1f);savePrefs();body.post(()->paginateAndShow(Math.min(pageIndex,Math.max(0,pages.size()-1))));}
    private void applyFont(){try{Typeface tf;if(fontKey.startsWith("system:"))tf=Typeface.create(fontKey.substring(7),Typeface.NORMAL);else if(fontKey.equals("wenkai"))tf=Typeface.create("serif",Typeface.NORMAL);else tf=Typeface.createFromAsset(getAssets(),fontKey);body.setTypeface(tf);}catch(Throwable e){body.setTypeface(Typeface.create("serif",Typeface.NORMAL));}}
    private void savePrefs(){getSharedPreferences("reader_settings",MODE_PRIVATE).edit().putFloat("size",fontSize).putFloat("line",lineExtra).putInt("bg",bg).putInt("fg",fg).putString("font",fontKey).putBoolean("immersive",immersive).putBoolean("page_sound",pageSound).apply();}
    private void loadPrefs(){SharedPreferences p=getSharedPreferences("reader_settings",MODE_PRIVATE);fontSize=p.getFloat("size",20);lineExtra=p.getFloat("line",9);bg=p.getInt("bg",Color.rgb(248,244,232));fg=p.getInt("fg",Color.rgb(48,44,38));fontKey=p.getString("font","wenkai");immersive=p.getBoolean("immersive",false);pageSound=p.getBoolean("page_sound",true);}
    private TextView nav(String s){TextView v=label(s,14);v.setGravity(Gravity.CENTER);v.setTextColor(navColor());return v;}
    private TextView sectionDark(String s){TextView v=dark(s,14,false);v.setTextColor(Color.rgb(108,102,92));v.setPadding(0,dp(14),0,dp(7));return v;}
    private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);return r;}
    private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);p.setMargins(dp(3),0,dp(3),0);return p;}
    private Button action(String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(l);return b;}
    private TextView label(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(fg);return v;}
    private TextView dark(String s,int size,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.rgb(48,44,38));v.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);return v;}
    private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
