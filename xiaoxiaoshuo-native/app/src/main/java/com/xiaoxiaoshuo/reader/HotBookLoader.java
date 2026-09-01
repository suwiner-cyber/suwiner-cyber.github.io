package com.xiaoxiaoshuo.reader;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public final class HotBookLoader {
    public interface Callback { void onLoaded(List<LegacyDexBridge.BookResult> books); }
    private static final String[] CANDIDATES={"雪中悍刀行","庆余年","将夜","剑来","诡秘之主","凡人修仙传","斗破苍穹","全职高手","大奉打更人","遮天","一念永恒","斗罗大陆","牧神记","完美世界","吞噬星空","沧元图","道诡异仙","深空彼岸","第一序列","夜的命名术"};
    private HotBookLoader(){}

    public static void load(Context c, Callback cb){
        List<LegacyDexBridge.BookResult> cached=readCache(c);
        if(!cached.isEmpty())cb.onLoaded(cached);
        long updated=c.getSharedPreferences("hot_books",Context.MODE_PRIVATE).getLong("updated",0L);
        if(!cached.isEmpty()&&System.currentTimeMillis()-updated<4L*60L*60L*1000L)return;
        new Thread(()->{
            List<LegacyDexBridge.BookResult> fresh=refresh(c.getApplicationContext());
            if(!fresh.isEmpty()){writeCache(c,fresh);cb.onLoaded(fresh);}
            else if(cached.isEmpty()){List<LegacyDexBridge.BookResult> fallback=fallbackFromLocal(c);if(!fallback.isEmpty())cb.onLoaded(fallback);}
        }).start();
    }

    private static List<LegacyDexBridge.BookResult> refresh(Context c){
        final ArrayList<LegacyDexBridge.BookResult> out=new ArrayList<>();
        final Set<String> seen=Collections.synchronizedSet(new HashSet<String>());
        final LegacySourceStore.State st=LegacySourceStore.prepare(c,80);
        if(st.selected.isEmpty())return out;
        ExecutorService pool=Executors.newFixedThreadPool(6);
        ArrayList<Future<LegacyDexBridge.BookResult>> futures=new ArrayList<>();
        for(String q:CANDIDATES)futures.add(pool.submit(()->validateCandidate(c,st,q)));
        for(Future<LegacyDexBridge.BookResult> f:futures){
            if(out.size()>=8)break;
            try{
                LegacyDexBridge.BookResult b=f.get(45,TimeUnit.SECONDS);
                if(b!=null&&seen.add(norm(b.title)))out.add(b);
            }catch(Throwable ignored){}
        }
        pool.shutdownNow();
        return out;
    }

    private static LegacyDexBridge.BookResult validateCandidate(Context c,LegacySourceStore.State st,String q){
        int tried=0;
        for(LegacySourceStore.SourceInfo s:st.selected){
            if(tried++>=40)break;
            try{
                List<LegacyDexBridge.BookResult> rs=LegacyDexBridge.get(c).search(s,q,1);
                for(LegacyDexBridge.BookResult raw:rs){
                    if(!norm(raw.title).equals(norm(q)))continue;
                    LegacyDexBridge.BookResult b=raw;
                    try{
                        LegacyDexBridge.BookResult detail=LegacyDexBridge.get(c).loadDetail(raw);
                        if(detail!=null){
                            if(detail.sourceJson==null||detail.sourceJson.length()==0)detail.sourceJson=raw.sourceJson;
                            if(detail.sourceName==null||detail.sourceName.length()==0)detail.sourceName=raw.sourceName;
                            if(detail.sourceUrl==null||detail.sourceUrl.length()==0)detail.sourceUrl=raw.sourceUrl;
                            if(detail.bookUrl==null||detail.bookUrl.length()==0)detail.bookUrl=raw.bookUrl;
                            if(detail.title==null||detail.title.length()==0)detail.title=raw.title;
                            if(detail.author==null||detail.author.length()==0)detail.author=raw.author;
                            b=detail;
                        }
                    }catch(Throwable ignored){}
                    if(b.coverUrl==null||b.coverUrl.trim().length()==0)continue;
                    List<LegacyDexBridge.Chapter> chapters=LegacyDexBridge.get(c).loadCatalog(b);
                    if(chapters==null||chapters.size()<20)continue;
                    if(!CoverLoader.probe(b.coverUrl,b.bookUrl!=null&&b.bookUrl.length()>0?b.bookUrl:b.sourceUrl,b.sourceJson))continue;
                    LegacyDexBridge.Chapter first=chapters.get(0);
                    String firstText=LegacyDexBridge.get(c).loadChapter(b.sourceJson,first.name,first.url);
                    if(firstText==null||firstText.trim().length()<60)continue;
                    OfflineBookCache.saveBookMetadata(c,b,chapters);
                    OfflineBookCache.putChapter(c,b.title,0,first.url,AiTypesetter.formatNovel(firstText));
                    return b;
                }
            }catch(Throwable ignored){}
        }
        return null;
    }

    private static List<LegacyDexBridge.BookResult> fallbackFromLocal(Context c){
        ArrayList<LegacyDexBridge.BookResult> out=new ArrayList<>();
        for(String q:CANDIDATES){
            OfflineBookCache.BookData d=OfflineBookCache.loadBook(c,q);
            if(d.title.length()==0||d.cover.length()==0||d.chapters.size()<20)continue;
            LegacyDexBridge.BookResult b=new LegacyDexBridge.BookResult();b.title=d.title;b.author=d.author;b.intro=d.intro;b.coverUrl=d.cover;b.bookUrl=d.bookUrl;b.sourceName=d.sourceName;b.sourceUrl=d.sourceUrl;b.sourceJson=d.sourceJson;out.add(b);if(out.size()>=8)break;
        }
        return out;
    }

    private static String norm(String s){return s==null?"":s.replace(" ","").replace("《","").replace("》","").toLowerCase(Locale.ROOT);}
    private static void writeCache(Context c,List<LegacyDexBridge.BookResult> list){try{JSONArray a=new JSONArray();for(LegacyDexBridge.BookResult b:list){JSONObject o=new JSONObject();o.put("title",b.title);o.put("author",b.author);o.put("intro",b.intro);o.put("cover",b.coverUrl);o.put("bookUrl",b.bookUrl);o.put("sourceName",b.sourceName);o.put("sourceUrl",b.sourceUrl);o.put("sourceJson",b.sourceJson);a.put(o);}c.getSharedPreferences("hot_books",Context.MODE_PRIVATE).edit().putString("items",a.toString()).putLong("updated",System.currentTimeMillis()).apply();}catch(Throwable ignored){}}
    private static List<LegacyDexBridge.BookResult> readCache(Context c){ArrayList<LegacyDexBridge.BookResult> out=new ArrayList<>();try{SharedPreferences p=c.getSharedPreferences("hot_books",Context.MODE_PRIVATE);String raw=p.getString("items","[]");JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;LegacyDexBridge.BookResult b=new LegacyDexBridge.BookResult();b.title=o.optString("title");b.author=o.optString("author");b.intro=o.optString("intro");b.coverUrl=o.optString("cover");b.bookUrl=o.optString("bookUrl");b.sourceName=o.optString("sourceName");b.sourceUrl=o.optString("sourceUrl");b.sourceJson=o.optString("sourceJson");if(b.title.length()>0&&b.coverUrl.length()>0)out.add(b);}}catch(Throwable ignored){}return out;}
}
