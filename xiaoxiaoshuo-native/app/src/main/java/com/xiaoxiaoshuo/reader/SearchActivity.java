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
    private EditText input;
    private LinearLayout results;
    private ScrollView scroll;
    private TextView status,tail;
    private volatile boolean stopped;
    private ExecutorService pool;
    private final Set<String> seen=Collections.synchronizedSet(new LinkedHashSet<>());
    private final ArrayList<LegacyDexBridge.BookResult> all=new ArrayList<>();
    private final AtomicInteger done=new AtomicInteger();
    private String keyword="";
    private int sourcePage=1,rendered=0;
    private final int renderBatch=20;
    private boolean loadingRemote=false,remoteExhausted=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        build();
        String q=getIntent().getStringExtra("query");
        if(q!=null&&q.trim().length()>0){input.setText(q);runSearch(q.trim());}
        else input.requestFocus();
    }

    @Override protected void onDestroy(){
        stopped=true;
        if(pool!=null)pool.shutdownNow();
        super.onDestroy();
    }

    private void build(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247,244,238));
        root.setPadding(dp(16),dp(12),dp(16),0);

        LinearLayout top=new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=tx("‹",32,green,true);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v->finish());
        top.addView(back,new LinearLayout.LayoutParams(dp(44),dp(56)));

        LinearLayout searchBox=new LinearLayout(this);
        searchBox.setGravity(Gravity.CENTER_VERTICAL);
        searchBox.setPadding(dp(16),0,dp(4),0);
        GradientDrawable ib=new GradientDrawable();
        ib.setColor(Color.WHITE);
        ib.setCornerRadius(dp(22));
        searchBox.setBackground(ib);
        input=new EditText(this);
        input.setSingleLine(true);
        input.setHint("输入准确书名或作者");
        input.setTextSize(16);
        input.setPadding(0,0,dp(6),0);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setBackgroundColor(Color.TRANSPARENT);
        searchBox.addView(input,new LinearLayout.LayoutParams(0,dp(56),1));
        SearchIconView go=new SearchIconView(this);
        go.setContentDescription("搜索");
        go.setOnClickListener(v->runSearch(input.getText().toString().trim()));
        searchBox.addView(go,new LinearLayout.LayoutParams(dp(56),dp(56)));
        top.addView(searchBox,new LinearLayout.LayoutParams(0,dp(56),1));
        root.addView(top);

        status=tx("仅显示书名或作者与关键词直接匹配的结果",13,Color.rgb(122,116,106),false);
        status.setPadding(dp(4),dp(12),dp(4),dp(10));
        root.addView(status);

        scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        results=new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setPadding(0,0,0,dp(14));
        tail=tx("",12,Color.rgb(128,121,110),false);
        tail.setGravity(Gravity.CENTER);
        tail.setPadding(0,dp(12),0,dp(24));
        results.addView(tail,new LinearLayout.LayoutParams(-1,-2));
        scroll.addView(results);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        scroll.setOnScrollChangeListener((v,sx,sy,osx,osy)->{
            View child=scroll.getChildAt(0);
            if(child==null)return;
            int remain=child.getMeasuredHeight()-(sy+scroll.getHeight());
            if(remain<dp(520))loadMoreVisible();
        });

        setContentView(root);
        getWindow().setStatusBarColor(Color.rgb(247,244,238));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        input.setOnEditorActionListener((v,id,e)->{
            if(id==EditorInfo.IME_ACTION_SEARCH){runSearch(input.getText().toString().trim());return true;}
            return false;
        });
    }

    private void runSearch(String q){
        if(q.length()==0){Toast.makeText(this,"请输入书名或作者",Toast.LENGTH_SHORT).show();return;}
        keyword=q;
        stopped=false;
        if(pool!=null)pool.shutdownNow();
        synchronized(all){all.clear();}
        seen.clear();
        sourcePage=1;
        rendered=0;
        remoteExhausted=false;
        results.removeAllViews();
        results.addView(tail,new LinearLayout.LayoutParams(-1,-2));
        tail.setText("正在检索全部优选书源…");
        requestRemotePage();
    }

    private void requestRemotePage(){
        if(loadingRemote||remoteExhausted||keyword.length()==0)return;
        LegacySourceStore.State st=LegacySourceStore.prepare(this,80);
        ArrayList<LegacySourceStore.SourceInfo> sources=new ArrayList<>(st.selected);
        if(sources.isEmpty()){status.setText("没有可用外部书源。请到“我的 → 书源管理”添加或恢复书源。");tail.setText("");return;}
        loadingRemote=true;
        done.set(0);
        final int requestedPage=sourcePage;
        final AtomicInteger accepted=new AtomicInteger();
        status.setText("正在检索第 "+requestedPage+" 轮 · 只保留强相关结果");
        tail.setText("正在继续搜索…");
        if(pool!=null)pool.shutdownNow();
        pool=Executors.newFixedThreadPool(8);
        for(LegacySourceStore.SourceInfo s:sources){
            pool.submit(()->{
                try{
                    if(stopped)return;
                    List<LegacyDexBridge.BookResult> list=LegacyDexBridge.get(this).search(s,keyword,requestedPage);
                    for(LegacyDexBridge.BookResult b:list){
                        if(stopped)break;
                        if(!isRelevant(b,keyword))continue;
                        String key=(norm(b.title)+"|"+norm(b.author));
                        if(seen.add(key)){
                            synchronized(all){all.add(b);}
                            accepted.incrementAndGet();
                        }
                    }
                    LegacySourceStore.recordHealth(this,s.url,true,1);
                }catch(Throwable ex){
                    LegacySourceStore.recordHealth(this,s.url,false,6000);
                }finally{
                    int d=done.incrementAndGet();
                    runOnUiThread(()->{
                        int total;
                        synchronized(all){total=all.size();}
                        status.setText("已完成 "+d+" / "+sources.size()+" 个书源 · 强相关结果 "+total+" 条");
                        if(d>=sources.size()){
                            loadingRemote=false;
                            if(accepted.get()==0)remoteExhausted=true;
                            else sourcePage=requestedPage+1;
                            appendVisibleBatch();
                            if(remoteExhausted)tail.setText(total==0?"没有找到与关键词直接匹配的书籍":"已经加载全部匹配结果");
                            else tail.setText("继续下滑自动加载更多");
                            if(total==0&&!remoteExhausted)requestRemotePage();
                        }
                    });
                }
            });
        }
    }

    private boolean isRelevant(LegacyDexBridge.BookResult b,String q){
        String k=norm(q),title=norm(b.title),author=norm(b.author);
        if(k.length()==0)return false;
        if(title.equals(k)||author.equals(k))return true;
        if(title.contains(k)||author.contains(k))return true;
        if(k.length()>=4&&title.length()>=4&&commonRun(title,k)>=Math.min(4,k.length()))return true;
        return false;
    }

    private int commonRun(String a,String b){
        int best=0;
        for(int i=0;i<a.length();i++)for(int j=0;j<b.length();j++){
            int n=0;
            while(i+n<a.length()&&j+n<b.length()&&a.charAt(i+n)==b.charAt(j+n))n++;
            if(n>best)best=n;
        }
        return best;
    }

    private String norm(String s){
        if(s==null)return "";
        return s.toLowerCase(Locale.ROOT).replaceAll("[\\s　《》〈〉【】\\[\\]（）()·•._—-]+","");
    }

    private void loadMoreVisible(){
        int total;
        synchronized(all){total=all.size();}
        if(rendered<total){appendVisibleBatch();return;}
        if(!loadingRemote&&!remoteExhausted)requestRemotePage();
    }

    private void appendVisibleBatch(){
        ArrayList<LegacyDexBridge.BookResult> copy;
        synchronized(all){copy=new ArrayList<>(all);}
        int end=Math.min(copy.size(),rendered+renderBatch);
        if(rendered==0)results.removeAllViews();
        for(int i=rendered;i<end;i++)results.addView(bookCard(copy.get(i)));
        rendered=end;
        if(tail.getParent()!=null)((ViewGroup)tail.getParent()).removeView(tail);
        results.addView(tail,new LinearLayout.LayoutParams(-1,-2));
        if(rendered<copy.size())tail.setText("继续下滑加载更多结果");
        else if(remoteExhausted)tail.setText(copy.isEmpty()?"没有找到与关键词直接匹配的书籍":"已经加载全部匹配结果");
        else tail.setText("继续下滑自动搜索更多书源结果");
    }

    private View bookCard(LegacyDexBridge.BookResult b){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(12),dp(12),dp(12),dp(12));
        GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(18));card.setBackground(g);
        card.setOnClickListener(v->open(b));
        ImageView cover=new ImageView(this);cover.setImageDrawable(new LiteraryCoverDrawable(b.title));
        card.addView(cover,new LinearLayout.LayoutParams(dp(84),dp(118)));
        CoverLoader.load(cover,b.coverUrl,b.bookUrl.length()>0?b.bookUrl:b.sourceUrl,b.sourceJson,b.title);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),0,0,0);
        box.addView(tx(b.title,18,Color.rgb(39,49,44),true));
        TextView a=tx((b.author.length()>0?b.author:"作者未知")+" · "+(b.sourceName.length()>0?b.sourceName:"外部书源"),12,Color.rgb(128,120,108),false);a.setPadding(0,dp(4),0,dp(7));box.addView(a);
        TextView intro=tx(b.intro.length()>0?b.intro:"点击查看书籍详情和章节目录",13,Color.rgb(85,82,77),false);intro.setMaxLines(3);box.addView(intro);
        TextView hint=tx("目录 · 阅读 ›",13,green,true);hint.setGravity(Gravity.RIGHT|Gravity.BOTTOM);box.addView(hint,new LinearLayout.LayoutParams(-1,0,1));
        card.addView(box,new LinearLayout.LayoutParams(0,dp(118),1));
        LinearLayout holder=new LinearLayout(this);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(10);holder.addView(card,cp);return holder;
    }

    private void open(LegacyDexBridge.BookResult b){
        Intent i=new Intent(this,BookDetailActivity.class);
        i.putExtra("title",b.title);i.putExtra("author",b.author);i.putExtra("intro",b.intro);i.putExtra("cover",b.coverUrl);i.putExtra("book_url",b.bookUrl);i.putExtra("source_name",b.sourceName);i.putExtra("source_url",b.sourceUrl);i.putExtra("source_json",b.sourceJson);startActivity(i);
    }

    private TextView tx(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setTypeface(Typeface.create("sans",bold?Typeface.BOLD:Typeface.NORMAL));return v;}
    private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
