package com.xiaoxiaoshuo.reader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;

public class ReaderActivity extends Activity{
 private LinearLayout root,top,bottom;
 private TextView body,title,chapterTitle,progress,back,catalogBtn,setup,prev,toc,next;
 private ScrollView scroll;
 private float fontSize=20f,lineExtra=9f;
 private int bg=Color.rgb(248,244,232),fg=Color.rgb(48,44,38);
 private String fontKey="wenkai";
 private final LinkedHashMap<String,String> fonts=new LinkedHashMap<>();
 private CatalogStore.Data catalog;
 private String catalogKey="",sourceJson="";
 private int chapterIndex=0;
 private volatile int loadToken=0;

 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  fonts.put("文楷","fonts/LXGWWenKaiLite-Regular.ttf");
  fonts.put("文楷中黑","fonts/LXGWWenKaiLite-Medium.ttf");
  fonts.put("文楷轻体","fonts/LXGWWenKaiLite-Light.ttf");
  fonts.put("等宽文楷","fonts/LXGWWenKaiMonoLite-Regular.ttf");
  fonts.put("系统黑体","system:sans");
  loadPrefs();
  catalogKey=n(getIntent().getStringExtra("catalog_key"));
  sourceJson=n(getIntent().getStringExtra("source_json"));
  chapterIndex=getIntent().getIntExtra("chapter_index",0);
  if(catalogKey.length()>0)catalog=CatalogStore.load(this,catalogKey);
  build();
  if(catalog!=null&&!catalog.chapters.isEmpty()){
   if(sourceJson.length()==0)sourceJson=catalog.sourceJson;
   loadChapter(Math.max(0,Math.min(chapterIndex,catalog.chapters.size()-1)));
  }else{
   String raw=getIntent().getStringExtra("body");
   body.setText(AiTypesetter.formatNovel(raw==null?"正文加载失败。":raw));
  }
 }
 private String n(String s){return s==null?"":s;}

 private void build(){
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(10),dp(7),dp(10),dp(6));
  back=label("‹",32);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(44),dp(50)));
  String t=n(getIntent().getStringExtra("title"));title=label(t.length()==0?"阅读":t,16);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER_VERTICAL);title.setSingleLine(true);top.addView(title,new LinearLayout.LayoutParams(0,dp(50),1));
  catalogBtn=label("目录",14);catalogBtn.setGravity(Gravity.CENTER);catalogBtn.setOnClickListener(v->showCatalog());top.addView(catalogBtn,new LinearLayout.LayoutParams(dp(58),dp(50)));
  setup=label("Aa",17);setup.setGravity(Gravity.CENTER);setup.setOnClickListener(v->showSettings());top.addView(setup,new LinearLayout.LayoutParams(dp(54),dp(50)));
  root.addView(top);

  chapterTitle=label("",13);chapterTitle.setGravity(Gravity.CENTER);root.addView(chapterTitle,new LinearLayout.LayoutParams(-1,dp(30)));
  scroll=new ScrollView(this);scroll.setFillViewport(true);
  body=new TextView(this);body.setPadding(dp(26),dp(16),dp(26),dp(50));body.setTextSize(fontSize);body.setLineSpacing(dp(lineExtra),1f);body.setTextIsSelectable(true);applyFont();scroll.addView(body,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

  bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER);bottom.setPadding(dp(10),dp(5),dp(10),dp(6));
  prev=nav("‹ 上一章");prev.setOnClickListener(v->loadChapter(chapterIndex-1));
  toc=nav("目录");toc.setOnClickListener(v->showCatalog());
  next=nav("下一章 ›");next.setOnClickListener(v->loadChapter(chapterIndex+1));
  bottom.addView(prev,new LinearLayout.LayoutParams(0,dp(44),1));bottom.addView(toc,new LinearLayout.LayoutParams(0,dp(44),1));bottom.addView(next,new LinearLayout.LayoutParams(0,dp(44),1));root.addView(bottom);
  progress=label("",11);progress.setGravity(Gravity.CENTER);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(20)));
  setContentView(root);
  applyTheme();
 }

 private void loadChapter(int idx){
  if(catalog==null||catalog.chapters.isEmpty())return;
  if(idx<0){Toast.makeText(this,"已经是第一章",Toast.LENGTH_SHORT).show();return;}
  if(idx>=catalog.chapters.size()){Toast.makeText(this,"已经是最新章节",Toast.LENGTH_SHORT).show();return;}
  chapterIndex=idx;LegacyDexBridge.Chapter ch=catalog.chapters.get(idx);chapterTitle.setText(ch.name);progress.setText((idx+1)+" / "+catalog.chapters.size());body.setText("正在加载正文…");scroll.scrollTo(0,0);int token=++loadToken;final int wantedIndex=idx;final String wantedName=ch.name;
  new Thread(()->{
   String text="",switched="";CatalogStore.Data newCatalog=null;String newSource=sourceJson;int newIndex=wantedIndex;
   try{text=LegacyDexBridge.get(this).loadChapter(sourceJson,ch.name,ch.url);}catch(Throwable ignored){}
   if(text.trim().length()==0){Alternate alt=findAlternate(title.getText().toString(),wantedName,wantedIndex);if(alt!=null&&alt.text.trim().length()>0){text=alt.text;switched=alt.sourceName;newCatalog=alt.catalog;newSource=alt.sourceJson;newIndex=alt.index;}}
   final String ready=text,sourceName=switched,finalSource=newSource;final CatalogStore.Data finalCatalog=newCatalog;final int finalIndex=newIndex;
   runOnUiThread(()->{
    if(token!=loadToken)return;
    if(finalCatalog!=null){catalog=finalCatalog;sourceJson=finalSource;chapterIndex=finalIndex;catalogKey=CatalogStore.save(this,title.getText().toString(),sourceJson,catalog.chapters);LegacyDexBridge.Chapter real=catalog.chapters.get(chapterIndex);chapterTitle.setText(real.name);progress.setText((chapterIndex+1)+" / "+catalog.chapters.size());if(sourceName.length()>0)Toast.makeText(this,"已自动切换书源："+sourceName,Toast.LENGTH_SHORT).show();}
    body.setText(ready.trim().length()==0?"本章正文加载失败，已自动尝试全部优选书源，暂未找到可读正文。":AiTypesetter.formatNovel(ready));scroll.scrollTo(0,0);saveProgress();
   });
  }).start();
 }

 private Alternate findAlternate(String bookTitle,String chapterName,int index){
  LegacySourceStore.State st=LegacySourceStore.prepare(this,80);String currentUrl=sourceUrl(sourceJson);int tried=0;
  for(LegacySourceStore.SourceInfo s:st.selected){
   if(tried++>=80)break;if(s.url.equals(currentUrl))continue;long start=System.currentTimeMillis();
   try{
    List<LegacyDexBridge.BookResult> list=LegacyDexBridge.get(this).search(s,bookTitle,1);LegacyDexBridge.BookResult best=null;
    for(LegacyDexBridge.BookResult b:list){if(norm(b.title).equals(norm(bookTitle))){best=b;break;}}
    if(best==null)continue;
    List<LegacyDexBridge.Chapter> cs=LegacyDexBridge.get(this).loadCatalog(best);if(cs==null||cs.isEmpty())continue;
    int target=findChapter(cs,chapterName,index);LegacyDexBridge.Chapter ch=cs.get(target);String text=LegacyDexBridge.get(this).loadChapter(best.sourceJson,ch.name,ch.url);
    if(text!=null&&text.trim().length()>0){LegacySourceStore.recordHealth(this,s.url,true,System.currentTimeMillis()-start);CatalogStore.Data d=new CatalogStore.Data();d.title=bookTitle;d.sourceJson=best.sourceJson;d.chapters.addAll(cs);Alternate a=new Alternate();a.text=text;a.sourceJson=best.sourceJson;a.sourceName=best.sourceName.length()>0?best.sourceName:s.name;a.catalog=d;a.index=target;return a;}
   }catch(Throwable ex){LegacySourceStore.recordHealth(this,s.url,false,System.currentTimeMillis()-start);}
  }
  return null;
 }

 private int findChapter(List<LegacyDexBridge.Chapter> list,String name,int fallback){String n=normChapter(name);for(int i=0;i<list.size();i++)if(normChapter(list.get(i).name).equals(n))return i;return Math.max(0,Math.min(fallback,list.size()-1));}
 private String sourceUrl(String json){try{return new JSONObject(json).optString("bookSourceUrl","");}catch(Throwable e){return "";}}
 private String norm(String s){return s==null?"":s.replace(" ","").replace("《","").replace("》","").toLowerCase(Locale.ROOT);}
 private String normChapter(String s){return s==null?"":s.replaceAll("[\\s　]+","").replaceAll("[第章节卷回部篇]","").toLowerCase(Locale.ROOT);}
 private static final class Alternate{String text="",sourceJson="",sourceName="";CatalogStore.Data catalog;int index;}
 private void saveProgress(){getSharedPreferences("reading_progress",MODE_PRIVATE).edit().putInt("idx_"+Integer.toHexString(title.getText().toString().hashCode()),chapterIndex).apply();}

 private void showCatalog(){
  if(catalog==null||catalog.chapters.isEmpty()){Toast.makeText(this,"目录还没有加载完成",Toast.LENGTH_SHORT).show();return;}
  Dialog d=new Dialog(this);LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(14),dp(12),dp(14),dp(12));panel.setBackgroundColor(bg);
  TextView h=label("章节目录 · "+catalog.chapters.size()+"章",19);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);panel.addView(h);
  ScrollView sv=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
  for(int i=0;i<catalog.chapters.size();i++){final int idx=i;TextView r=label((i+1)+"  "+catalog.chapters.get(i).name,14);r.setPadding(dp(10),dp(12),dp(10),dp(12));if(i==chapterIndex){r.setTextColor(Color.rgb(49,88,71));r.setTypeface(Typeface.DEFAULT,Typeface.BOLD);}r.setOnClickListener(v->{d.dismiss();loadChapter(idx);});list.addView(r);}
  sv.addView(list);panel.addView(sv,new LinearLayout.LayoutParams(-1,0,1));d.setContentView(panel);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.copyFrom(w.getAttributes());lp.width=(int)(getResources().getDisplayMetrics().widthPixels*.92f);lp.height=(int)(getResources().getDisplayMetrics().heightPixels*.78f);lp.gravity=Gravity.BOTTOM;w.setAttributes(lp);}
 }

 private void showSettings(){
  final Dialog d=new Dialog(this);LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(22),dp(18),dp(22),dp(20));GradientDrawable pbg=new GradientDrawable();pbg.setColor(Color.rgb(255,253,249));pbg.setCornerRadius(dp(24));panel.setBackground(pbg);
  TextView h=new TextView(this);h.setText("阅读排版");h.setTextSize(22);h.setTextColor(Color.rgb(48,44,38));h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);panel.addView(h);
  panel.addView(sectionDark("字号"));LinearLayout sizes=row();sizes.addView(action("A−",v->{fontSize=Math.max(15,fontSize-1);applyAndSave();}),weight());sizes.addView(action("A+",v->{fontSize=Math.min(32,fontSize+1);applyAndSave();}),weight());sizes.addView(action("默认",v->{fontSize=20;lineExtra=9;applyAndSave();}),weight());panel.addView(sizes);
  panel.addView(sectionDark("行距"));LinearLayout lines=row();lines.addView(action("紧凑",v->{lineExtra=5;applyAndSave();}),weight());lines.addView(action("舒适",v->{lineExtra=9;applyAndSave();}),weight());lines.addView(action("宽松",v->{lineExtra=14;applyAndSave();}),weight());panel.addView(lines);
  panel.addView(sectionDark("字体"));LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);int n=0;LinearLayout cur=null;for(Map.Entry<String,String> e:fonts.entrySet()){if(n%2==0){cur=row();wrap.addView(cur);}final String path=e.getValue();cur.addView(action(e.getKey(),v->{fontKey=path;applyFont();savePrefs();}),weight());n++;}panel.addView(wrap);
  panel.addView(sectionDark("背景"));LinearLayout backs=row();backs.addView(action("米白",v->setBackgroundTheme(Color.rgb(248,244,232))),weight());backs.addView(action("纯白",v->setBackgroundTheme(Color.WHITE)),weight());backs.addView(action("护眼",v->setBackgroundTheme(Color.rgb(225,238,222))),weight());panel.addView(backs);
  Button custom=action("自定义背景颜色",v->showCustomColorDialog());LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(46));cp.setMargins(dp(3),dp(6),dp(3),0);panel.addView(custom,cp);
  TextView close=new TextView(this);close.setText("完成");close.setTextSize(16);close.setGravity(Gravity.CENTER);close.setTextColor(Color.rgb(49,88,71));close.setTypeface(Typeface.DEFAULT,Typeface.BOLD);close.setPadding(0,dp(16),0,dp(6));close.setOnClickListener(v->d.dismiss());panel.addView(close);
  d.setContentView(panel);d.show();Window w=d.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);WindowManager.LayoutParams lp=new WindowManager.LayoutParams();lp.copyFrom(w.getAttributes());lp.width=(int)(getResources().getDisplayMetrics().widthPixels*.92f);lp.height=WindowManager.LayoutParams.WRAP_CONTENT;lp.gravity=Gravity.BOTTOM;w.setAttributes(lp);}
 }

 private void showCustomColorDialog(){
  final EditText e=new EditText(this);e.setSingleLine(true);e.setHint("例如 #F4EBD8");e.setText(String.format(Locale.US,"#%06X",0xFFFFFF&(bg)));e.setSelectAllOnFocus(true);
  new AlertDialog.Builder(this).setTitle("自定义阅读背景").setMessage("输入十六进制颜色值，应用后顶部、正文和底部导航会同步变色。").setView(e).setNegativeButton("取消",null).setPositiveButton("应用",(d,w)->{
   try{String raw=e.getText().toString().trim();if(!raw.startsWith("#"))raw="#"+raw;setBackgroundTheme(Color.parseColor(raw));}
   catch(Throwable ex){Toast.makeText(this,"颜色格式不正确，例如 #F4EBD8",Toast.LENGTH_LONG).show();}
  }).show();
 }

 private void setBackgroundTheme(int color){bg=color;fg=bestTextColor(color);applyTheme();savePrefs();}
 private int bestTextColor(int color){double y=.299*Color.red(color)+.587*Color.green(color)+.114*Color.blue(color);return y<145?Color.rgb(238,238,234):Color.rgb(48,44,38);}
 private void applyTheme(){
  if(root!=null)root.setBackgroundColor(bg);if(top!=null)top.setBackgroundColor(bg);if(bottom!=null)bottom.setBackgroundColor(bg);if(scroll!=null)scroll.setBackgroundColor(bg);if(body!=null){body.setBackgroundColor(bg);body.setTextColor(fg);}if(title!=null)title.setTextColor(fg);if(back!=null)back.setTextColor(fg);if(catalogBtn!=null)catalogBtn.setTextColor(fg);if(setup!=null)setup.setTextColor(fg);if(chapterTitle!=null){chapterTitle.setBackgroundColor(bg);chapterTitle.setTextColor(mutedColor());}if(progress!=null){progress.setBackgroundColor(bg);progress.setTextColor(mutedColor());}if(prev!=null){prev.setBackgroundColor(bg);prev.setTextColor(navColor());}if(toc!=null){toc.setBackgroundColor(bg);toc.setTextColor(navColor());}if(next!=null){next.setBackgroundColor(bg);next.setTextColor(navColor());}getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);int flags=bestTextColor(bg)==Color.rgb(48,44,38)?View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR:0;getWindow().getDecorView().setSystemUiVisibility(flags);
 }
 private int navColor(){return bestTextColor(bg)==Color.rgb(48,44,38)?Color.rgb(49,88,71):Color.rgb(196,224,206);}
 private int mutedColor(){return bestTextColor(bg)==Color.rgb(48,44,38)?Color.rgb(125,115,99):Color.rgb(190,188,180);}
 private void applyAndSave(){body.setTextSize(fontSize);body.setLineSpacing(dp(lineExtra),1f);savePrefs();}
 private void applyFont(){try{Typeface tf;if(fontKey.startsWith("system:"))tf=Typeface.create(fontKey.substring(7),Typeface.NORMAL);else if(fontKey.equals("wenkai"))tf=Typeface.create("serif",Typeface.NORMAL);else tf=Typeface.createFromAsset(getAssets(),fontKey);body.setTypeface(tf);}catch(Throwable e){body.setTypeface(Typeface.create("serif",Typeface.NORMAL));}}
 private void savePrefs(){getSharedPreferences("reader_settings",MODE_PRIVATE).edit().putFloat("size",fontSize).putFloat("line",lineExtra).putInt("bg",bg).putInt("fg",fg).putString("font",fontKey).apply();}
 private void loadPrefs(){android.content.SharedPreferences p=getSharedPreferences("reader_settings",MODE_PRIVATE);fontSize=p.getFloat("size",20);lineExtra=p.getFloat("line",9);bg=p.getInt("bg",Color.rgb(248,244,232));fg=p.getInt("fg",bestTextColor(bg));fontKey=p.getString("font","wenkai");}
 private TextView nav(String s){TextView v=label(s,14);v.setGravity(Gravity.CENTER);return v;}
 private TextView sectionDark(String s){TextView v=new TextView(this);v.setText(s);v.setTextSize(14);v.setTextColor(Color.rgb(108,102,92));v.setPadding(0,dp(14),0,dp(7));return v;}
 private LinearLayout row(){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);return r;}
 private LinearLayout.LayoutParams weight(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);p.setMargins(dp(3),0,dp(3),0);return p;}
 private Button action(String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(l);return b;}
 private TextView label(String s,int size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(fg);return v;}
 private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
