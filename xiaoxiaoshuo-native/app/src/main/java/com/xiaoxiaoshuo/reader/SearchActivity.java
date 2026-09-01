package com.xiaoxiaoshuo.reader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class SearchActivity extends Activity {
    private final int green=Color.rgb(49,88,71);
    private static final String[] HOT={"剑来","雪中悍刀行","诡秘之主","庆余年","凡人修仙传","大奉打更人","遮天","斗破苍穹","全职高手","将夜","深空彼岸","宿命之环"};
    private EditText input;
    private LinearLayout results,suggestBox,historyRow;
    private ScrollView scroll;
    private TextView status,tail;
    private volatile boolean stopped;
    private ExecutorService pool;
    private final LinkedHashMap<String,RankedBook> ranked=new LinkedHashMap<>();
    private final AtomicInteger done=new AtomicInteger();
    private String keyword="";
    private int sourcePage=1,rendered=0;
    private final int renderBatch=20;
    private boolean loadingRemote=false,remoteExhausted=false;

    private static final class RankedBook {
        LegacyDexBridge.BookResult book;
        int chapters;
        RankedBook(LegacyDexBridge.BookResult b,int c){book=b;chapters=c;}
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);build();
        String q=getIntent().getStringExtra("query");
        if(q!=null&&q.trim().length()>0){input.setText(q);runSearch(q.trim());}else input.requestFocus();
    }
    @Override protected void onDestroy(){stopped=true;if(pool!=null)pool.shutdownNow();super.onDestroy();}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(247,244,238));root.setPadding(dp(16),dp(12),dp(16),0);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=tx("‹",32,green,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(44),dp(58)));
        LinearLayout searchBox=new LinearLayout(this);searchBox.setGravity(Gravity.CENTER_VERTICAL);searchBox.setPadding(dp(16),0,dp(4),0);GradientDrawable ib=new GradientDrawable();ib.setColor(Color.WHITE);ib.setCornerRadius(dp(23));ib.setStroke(dp(1),Color.rgb(222,231,224));searchBox.setBackground(ib);
        input=new EditText(this);input.setSingleLine(true);input.setHint("搜索书名或作者");input.setTextSize(16);input.setPadding(0,0,dp(6),0);input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);input.setBackgroundColor(Color.TRANSPARENT);searchBox.addView(input,new LinearLayout.LayoutParams(0,dp(58),1));
        SearchIconView go=new SearchIconView(this);go.setContentDescription("搜索");go.setOnClickListener(v->runSearch(input.getText().toString().trim()));searchBox.addView(go,new LinearLayout.LayoutParams(dp(58),dp(58)));top.addView(searchBox,new LinearLayout.LayoutParams(0,dp(58),1));root.addView(top);

        suggestBox=new LinearLayout(this);suggestBox.setOrientation(LinearLayout.VERTICAL);suggestBox.setPadding(dp(2),dp(11),dp(2),dp(3));
        suggestBox.addView(tx("热门搜索",13,Color.rgb(95,100,94),true));
        suggestBox.addView(chipScroller(Arrays.asList(HOT),false),new LinearLayout.LayoutParams(-1,dp(46)));
        LinearLayout histTitle=new LinearLayout(this);histTitle.setGravity(Gravity.CENTER_VERTICAL);histTitle.addView(tx("搜索历史",13,Color.rgb(95,100,94),true),new LinearLayout.LayoutParams(0,-2,1));TextView clear=tx("清空",12,green,true);clear.setPadding(dp(12),dp(5),dp(4),dp(5));clear.setOnClickListener(v->{getSharedPreferences("search_history",MODE_PRIVATE).edit().clear().apply();refreshHistory();});histTitle.addView(clear);suggestBox.addView(histTitle);
        historyRow=new LinearLayout(this);historyRow.setOrientation(LinearLayout.HORIZONTAL);refreshHistory();HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);hs.addView(historyRow);suggestBox.addView(hs,new LinearLayout.LayoutParams(-1,dp(46)));root.addView(suggestBox);

        status=tx("搜索结果会解析真实目录，并按章节数从多到少排列",13,Color.rgb(122,116,106),false);status.setPadding(dp(4),dp(9),dp(4),dp(10));root.addView(status);
        scroll=new ScrollView(this);scroll.setFillViewport(true);results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);results.setPadding(0,0,0,dp(14));tail=tx("",12,Color.rgb(128,121,110),false);tail.setGravity(Gravity.CENTER);tail.setPadding(0,dp(12),0,dp(24));results.addView(tail);scroll.addView(results);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        scroll.setOnScrollChangeListener((v,sx,sy,osx,osy)->{View child=scroll.getChildAt(0);if(child==null)return;int remain=child.getMeasuredHeight()-(sy+scroll.getHeight());if(remain<dp(520))loadMoreVisible();});
        setContentView(root);getWindow().setStatusBarColor(Color.rgb(247,244,238));getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        input.setOnEditorActionListener((v,id,e)->{if(id==EditorInfo.IME_ACTION_SEARCH){runSearch(input.getText().toString().trim());return true;}return false;});
    }

    private HorizontalScrollView chipScroller(List<String> labels,boolean history){HorizontalScrollView hs=new HorizontalScrollView(this);hs.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);for(String s:labels)row.addView(chip(s,history));hs.addView(row);return hs;}
    private TextView chip(String s,boolean history){TextView v=tx(s,13,Color.rgb(54,83,69),false);v.setGravity(Gravity.CENTER);v.setPadding(dp(13),dp(6),dp(13),dp(6));GradientDrawable d=new GradientDrawable();d.setColor(history?Color.WHITE:Color.rgb(236,244,239));d.setCornerRadius(dp(16));if(history)d.setStroke(dp(1),Color.rgb(225,230,226));v.setBackground(d);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(34));p.setMargins(0,dp(5),dp(8),0);v.setLayoutParams(p);v.setOnClickListener(x->{input.setText(s);input.setSelection(s.length());runSearch(s);});return v;}
    private void refreshHistory(){if(historyRow==null)return;historyRow.removeAllViews();for(String s:readHistory())historyRow.addView(chip(s,true));if(historyRow.getChildCount()==0){TextView e=tx("暂无搜索历史",12,Color.rgb(150,144,135),false);e.setPadding(dp(2),dp(12),0,0);historyRow.addView(e);}}
    private List<String> readHistory(){String raw=getSharedPreferences("search_history",MODE_PRIVATE).getString("items","");ArrayList<String> out=new ArrayList<>();if(raw.length()>0)for(String x:raw.split("\\n"))if(x.trim().length()>0&&!out.contains(x.trim()))out.add(x.trim());return out;}
    private void saveHistory(String q){ArrayList<String> h=new ArrayList<>();h.add(q);for(String x:readHistory())if(!x.equalsIgnoreCase(q)&&h.size()<20)h.add(x);StringBuilder b=new StringBuilder();for(String x:h){if(b.length()>0)b.append('\n');b.append(x);}getSharedPreferences("search_history",MODE_PRIVATE).edit().putString("items",b.toString()).apply();refreshHistory();}

    private void runSearch(String q){
        if(q.length()==0){Toast.makeText(this,"请输入书名或作者",Toast.LENGTH_SHORT).show();return;}
        saveHistory(q);suggestBox.setVisibility(View.GONE);keyword=q;stopped=false;if(pool!=null)pool.shutdownNow();synchronized(ranked){ranked.clear();}sourcePage=1;rendered=0;remoteExhausted=false;loadingRemote=false;results.removeAllViews();results.addView(tail);tail.setText("正在检索并统计章节数…");requestRemotePage();
    }

    private void requestRemotePage(){
        if(loadingRemote||remoteExhausted||keyword.length()==0)return;
        LegacySourceStore.State st=LegacySourceStore.prepare(this,80);ArrayList<LegacySourceStore.SourceInfo> sources=new ArrayList<>(st.selected);
        if(sources.isEmpty()){status.setText("没有可用外部书源。请到“我的 → 书源管理”添加或恢复书源。");tail.setText("");return;}
        loadingRemote=true;done.set(0);final int requestedPage=sourcePage;final AtomicInteger accepted=new AtomicInteger();status.setText("第 "+requestedPage+" 轮：检索并解析目录中…");tail.setText("正在统计章节数…");if(pool!=null)pool.shutdownNow();pool=Executors.newFixedThreadPool(10);
        for(LegacySourceStore.SourceInfo s:sources){pool.submit(()->{
            boolean sourceOk=false;long begin=System.currentTimeMillis();
            try{
                if(stopped)return;List<LegacyDexBridge.BookResult> list=LegacyDexBridge.get(this).search(s,keyword,requestedPage);
                for(LegacyDexBridge.BookResult raw:list){if(stopped)break;if(!isRelevant(raw,keyword))continue;BookSourceResolver.Resolved rr=BookSourceResolver.inspect(this,raw);if(rr==null||rr.chapters.isEmpty())continue;sourceOk=true;LegacyDexBridge.BookResult b=rr.book;String key=norm(b.title)+"|"+norm(b.author);synchronized(ranked){RankedBook old=ranked.get(key);if(old==null||rr.chapterCount()>old.chapters){ranked.put(key,new RankedBook(b,rr.chapterCount()));accepted.incrementAndGet();}}}
            }catch(Throwable ignored){}finally{
                LegacySourceStore.recordHealth(this,s.url,sourceOk,System.currentTimeMillis()-begin);int d=done.incrementAndGet();runOnUiThread(()->{int total; synchronized(ranked){total=ranked.size();}status.setText("已解析 "+d+" / "+sources.size()+" 个书源 · 有效书籍 "+total+" 本");if(d>=sources.size()){loadingRemote=false;if(accepted.get()==0)remoteExhausted=true;else sourcePage=requestedPage+1;rendered=0;renderAllSorted();if(remoteExhausted)tail.setText(total==0?"没有找到匹配且有完整目录的书籍":"已加载全部匹配结果");else tail.setText("继续下滑自动搜索更多结果");if(total==0&&!remoteExhausted)requestRemotePage();}});
            }
        });}
    }

    private boolean isRelevant(LegacyDexBridge.BookResult b,String q){String k=norm(q),t=norm(b.title),a=norm(b.author);if(k.length()==0)return false;if(t.equals(k)||a.equals(k)||t.contains(k)||a.contains(k))return true;return k.length()>=4&&t.length()>=4&&commonRun(t,k)>=Math.min(4,k.length());}
    private int commonRun(String a,String b){int best=0;for(int i=0;i<a.length();i++)for(int j=0;j<b.length();j++){int n=0;while(i+n<a.length()&&j+n<b.length()&&a.charAt(i+n)==b.charAt(j+n))n++;if(n>best)best=n;}return best;}
    private String norm(String s){return BookSourceResolver.norm(s);}

    private ArrayList<RankedBook> sorted(){ArrayList<RankedBook> copy; synchronized(ranked){copy=new ArrayList<>(ranked.values());}Collections.sort(copy,(a,b)->{int c=Integer.compare(b.chapters,a.chapters);if(c!=0)return c;return a.book.title.compareToIgnoreCase(b.book.title);});return copy;}
    private void renderAllSorted(){ArrayList<RankedBook> copy=sorted();results.removeAllViews();rendered=Math.min(renderBatch,copy.size());for(int i=0;i<rendered;i++)results.addView(bookCard(copy.get(i)));results.addView(tail);if(rendered<copy.size())tail.setText("继续下滑加载更多 · 已按章节数排序");}
    private void loadMoreVisible(){ArrayList<RankedBook> copy=sorted();if(rendered<copy.size()){int end=Math.min(copy.size(),rendered+renderBatch);if(tail.getParent()!=null)((ViewGroup)tail.getParent()).removeView(tail);for(int i=rendered;i<end;i++)results.addView(bookCard(copy.get(i)));rendered=end;results.addView(tail);tail.setText(rendered<copy.size()?"继续下滑加载更多":"继续下滑自动搜索更多结果");return;}if(!loadingRemote&&!remoteExhausted)requestRemotePage();}

    private View bookCard(RankedBook r){LegacyDexBridge.BookResult b=r.book;LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setPadding(dp(12),dp(12),dp(12),dp(12));GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(18));card.setBackground(g);card.setOnClickListener(v->open(b));ImageView cover=new ImageView(this);cover.setImageDrawable(new LiteraryCoverDrawable(b.title));card.addView(cover,new LinearLayout.LayoutParams(dp(84),dp(118)));CoverLoader.load(cover,b.coverUrl,b.bookUrl.length()>0?b.bookUrl:b.sourceUrl,b.sourceJson,b.title);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),0,0,0);LinearLayout titleLine=new LinearLayout(this);titleLine.setGravity(Gravity.CENTER_VERTICAL);titleLine.addView(tx(b.title,18,Color.rgb(39,49,44),true),new LinearLayout.LayoutParams(0,-2,1));TextView count=tx(r.chapters+"章",12,Color.WHITE,true);count.setGravity(Gravity.CENTER);count.setPadding(dp(8),dp(3),dp(8),dp(3));GradientDrawable badge=new GradientDrawable();badge.setColor(green);badge.setCornerRadius(dp(12));count.setBackground(badge);titleLine.addView(count);box.addView(titleLine);TextView a=tx((b.author.length()>0?b.author:"作者未知")+" · "+(b.sourceName.length()>0?b.sourceName:"外部书源"),12,Color.rgb(128,120,108),false);a.setPadding(0,dp(4),0,dp(7));box.addView(a);TextView intro=tx(b.intro.length()>0?b.intro:"已获取完整目录，点击查看详情",13,Color.rgb(85,82,77),false);intro.setMaxLines(3);box.addView(intro);TextView hint=tx("目录 · 阅读 ›",13,green,true);hint.setGravity(Gravity.RIGHT|Gravity.BOTTOM);box.addView(hint,new LinearLayout.LayoutParams(-1,0,1));card.addView(box,new LinearLayout.LayoutParams(0,dp(118),1));LinearLayout holder=new LinearLayout(this);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(10);holder.addView(card,cp);return holder;}
    private void open(LegacyDexBridge.BookResult b){Intent i=new Intent(this,BookDetailActivity.class);i.putExtra("title",b.title);i.putExtra("author",b.author);i.putExtra("intro",b.intro);i.putExtra("cover",b.coverUrl);i.putExtra("book_url",b.bookUrl);i.putExtra("source_name",b.sourceName);i.putExtra("source_url",b.sourceUrl);i.putExtra("source_json",b.sourceJson);startActivity(i);}
    private TextView tx(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));return v;}
    private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
