package com.xiaoxiaoshuo.reader;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

public final class HotBookLoader {
    public interface Callback { void onLoaded(List<LegacyDexBridge.BookResult> books); }
    private static final String[] CANDIDATES={"雪中悍刀行","庆余年","将夜","剑来","诡秘之主","凡人修仙传","斗破苍穹","全职高手","大奉打更人","遮天","一念永恒","斗罗大陆"};
    private HotBookLoader(){}

    public static void load(Context c, Callback cb){
        List<LegacyDexBridge.BookResult> cached=readCache(c);
        if(!cached.isEmpty())cb.onLoaded(cached);
        new Thread(()->{List<LegacyDexBridge.BookResult> fresh=refresh(c);if(!fresh.isEmpty()){writeCache(c,fresh);cb.onLoaded(fresh);}}).start();
    }

    private static List<LegacyDexBridge.BookResult> refresh(Context c){
        ArrayList<LegacyDexBridge.BookResult> out=new ArrayList<>();
        try{
            LegacySourceStore.State st=LegacySourceStore.prepare(c,80);
            for(String q:CANDIDATES){
                if(out.size()>=8)break;
                LegacyDexBridge.BookResult hit=null;
                int tried=0;
                for(LegacySourceStore.SourceInfo s:st.selected){
                    if(tried++>=24)break;
                    try{
                        List<LegacyDexBridge.BookResult> rs=LegacyDexBridge.get(c).search(s,q,1);
                        for(LegacyDexBridge.BookResult b:rs){
                            if(!norm(b.title).equals(norm(q)))continue;
                            if(b.coverUrl==null||b.coverUrl.trim().length()==0)continue;
                            List<LegacyDexBridge.Chapter> chapters=LegacyDexBridge.get(c).loadCatalog(b);
                            if(chapters==null||chapters.size()<20)continue;
                            if(!CoverLoader.probe(b.coverUrl,b.bookUrl.length()>0?b.bookUrl:b.sourceUrl,b.sourceJson))continue;
                            hit=b;OfflineBookCache.saveBook(c,b,chapters);break;
                        }
                    }catch(Throwable ignored){}
                    if(hit!=null)break;
                }
                if(hit!=null)out.add(hit);
            }
        }catch(Throwable ignored){}
        return out;
    }

    private static String norm(String s){return s==null?"":s.replace(" ","").replace("《","").replace("》","").toLowerCase(Locale.ROOT);}
    private static void writeCache(Context c,List<LegacyDexBridge.BookResult> list){
        try{JSONArray a=new JSONArray();for(LegacyDexBridge.BookResult b:list){JSONObject o=new JSONObject();o.put("title",b.title);o.put("author",b.author);o.put("intro",b.intro);o.put("cover",b.coverUrl);o.put("bookUrl",b.bookUrl);o.put("sourceName",b.sourceName);o.put("sourceUrl",b.sourceUrl);o.put("sourceJson",b.sourceJson);a.put(o);}c.getSharedPreferences("hot_books",Context.MODE_PRIVATE).edit().putString("items",a.toString()).putLong("updated",System.currentTimeMillis()).apply();}catch(Throwable ignored){}
    }
    private static List<LegacyDexBridge.BookResult> readCache(Context c){
        ArrayList<LegacyDexBridge.BookResult> out=new ArrayList<>();
        try{SharedPreferences p=c.getSharedPreferences("hot_books",Context.MODE_PRIVATE);String raw=p.getString("items","[]");JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o==null)continue;LegacyDexBridge.BookResult b=new LegacyDexBridge.BookResult();b.title=o.optString("title");b.author=o.optString("author");b.intro=o.optString("intro");b.coverUrl=o.optString("cover");b.bookUrl=o.optString("bookUrl");b.sourceName=o.optString("sourceName");b.sourceUrl=o.optString("sourceUrl");b.sourceJson=o.optString("sourceJson");if(b.title.length()>0&&b.coverUrl.length()>0)out.add(b);}}catch(Throwable ignored){}
        return out;
    }
}
