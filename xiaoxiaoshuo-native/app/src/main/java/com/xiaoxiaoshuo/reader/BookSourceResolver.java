package com.xiaoxiaoshuo.reader;

import android.content.Context;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class BookSourceResolver {
    public static final class Resolved {
        public LegacyDexBridge.BookResult book;
        public ArrayList<LegacyDexBridge.Chapter> chapters=new ArrayList<>();
        public int chapterCount(){return chapters.size();}
    }
    private BookSourceResolver(){}

    public static Resolved inspect(Context c, LegacyDexBridge.BookResult input){
        if(input==null)return null;
        try{
            LegacyDexBridge.BookResult b=input;
            try{
                LegacyDexBridge.BookResult detail=LegacyDexBridge.get(c).loadDetail(input);
                if(detail!=null){
                    if(empty(detail.title))detail.title=input.title;
                    if(empty(detail.author))detail.author=input.author;
                    if(empty(detail.intro))detail.intro=input.intro;
                    if(empty(detail.coverUrl))detail.coverUrl=input.coverUrl;
                    if(empty(detail.bookUrl))detail.bookUrl=input.bookUrl;
                    if(empty(detail.sourceName))detail.sourceName=input.sourceName;
                    if(empty(detail.sourceUrl))detail.sourceUrl=input.sourceUrl;
                    detail.sourceJson=input.sourceJson;
                    b=detail;
                }
            }catch(Throwable ignored){}
            List<LegacyDexBridge.Chapter> cs=LegacyDexBridge.get(c).loadCatalog(b);
            if(cs==null||cs.isEmpty())return null;
            Resolved r=new Resolved();r.book=b;r.chapters.addAll(cs);return r;
        }catch(Throwable e){return null;}
    }

    public static Resolved bestForTitle(Context c,String title,String author,LegacyDexBridge.BookResult initial,int maxSources){
        final String wanted=norm(title),wantedAuthor=norm(author);
        final AtomicReference<Resolved> best=new AtomicReference<>();
        if(initial!=null&&norm(initial.title).equals(wanted))updateBest(best,inspect(c,initial),wantedAuthor);
        LegacySourceStore.State st=LegacySourceStore.prepare(c,80);
        ArrayList<LegacySourceStore.SourceInfo> sources=new ArrayList<>(st.selected);
        if(maxSources>0&&sources.size()>maxSources)sources=new ArrayList<>(sources.subList(0,maxSources));
        ExecutorService pool=Executors.newFixedThreadPool(Math.min(8,Math.max(1,sources.size())));
        CountDownLatch latch=new CountDownLatch(sources.size());
        for(LegacySourceStore.SourceInfo s:sources){
            pool.submit(()->{
                long start=System.currentTimeMillis();boolean ok=false;
                try{
                    List<LegacyDexBridge.BookResult> list=LegacyDexBridge.get(c).search(s,title,1);
                    for(LegacyDexBridge.BookResult b:list){
                        if(!norm(b.title).equals(wanted))continue;
                        if(wantedAuthor.length()>0&&norm(b.author).length()>0&&!authorCompatible(wantedAuthor,norm(b.author)))continue;
                        Resolved r=inspect(c,b);if(r==null)continue;ok=true;updateBest(best,r,wantedAuthor);
                    }
                }catch(Throwable ignored){}finally{
                    LegacySourceStore.recordHealth(c,s.url,ok,System.currentTimeMillis()-start);latch.countDown();
                }
            });
        }
        try{latch.await(45,TimeUnit.SECONDS);}catch(Throwable ignored){}
        pool.shutdownNow();
        Resolved r=best.get();
        if(r!=null&&r.book!=null&&!r.chapters.isEmpty())OfflineBookCache.saveBook(c,r.book,r.chapters);
        return r;
    }

    private static void updateBest(AtomicReference<Resolved> ref,Resolved candidate,String wantedAuthor){
        if(candidate==null||candidate.book==null||candidate.chapters.isEmpty())return;
        while(true){
            Resolved old=ref.get();
            if(old!=null&&!better(candidate,old,wantedAuthor))return;
            if(ref.compareAndSet(old,candidate))return;
        }
    }
    private static boolean better(Resolved a,Resolved b,String wantedAuthor){
        if(a.chapterCount()!=b.chapterCount())return a.chapterCount()>b.chapterCount();
        int aa=quality(a.book,wantedAuthor),bb=quality(b.book,wantedAuthor);return aa>bb;
    }
    private static int quality(LegacyDexBridge.BookResult b,String wantedAuthor){
        int q=0;if(b==null)return q;
        if(!empty(b.coverUrl))q+=4;if(!empty(b.intro))q+=2;if(!empty(b.author))q+=1;
        if(wantedAuthor.length()>0&&authorCompatible(wantedAuthor,norm(b.author)))q+=5;
        if(b.sourceUrl!=null&&b.sourceUrl.startsWith("https://"))q+=1;return q;
    }
    private static boolean authorCompatible(String a,String b){return a.equals(b)||a.contains(b)||b.contains(a);}
    private static boolean empty(String s){return s==null||s.trim().length()==0;}
    public static String norm(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replaceAll("[\\s　《》〈〉【】\\[\\]（）()·•._—-]+","");}
}
