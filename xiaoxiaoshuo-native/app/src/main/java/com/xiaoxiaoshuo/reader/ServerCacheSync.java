package com.xiaoxiaoshuo.reader;

import android.content.Context;
import org.json.*;
import java.net.URLEncoder;

public final class ServerCacheSync {
    private ServerCacheSync(){}
    public static void pushBook(Context c,LegacyDexBridge.BookResult b,java.util.List<LegacyDexBridge.Chapter> chapters){
        if(c==null||b==null||!ready(c))return;Context app=c.getApplicationContext();new Thread(()->{try{JSONArray a=new JSONArray();for(LegacyDexBridge.Chapter ch:chapters)a.put(new JSONObject().put("name",ch.name).put("url",ch.url));JSONObject req=new JSONObject().put("title",b.title).put("author",b.author).put("intro",b.intro).put("cover",b.coverUrl).put("sourceName",b.sourceName).put("sourceUrl",b.sourceUrl).put("chapters",a);ApiClient.post(app,"cache_book",req);}catch(Throwable ignored){}}).start();
    }
    public static void pushChapter(Context c,String title,int index,String chapterName,String text){
        if(c==null||title==null||text==null||text.trim().length()==0||!ready(c))return;Context app=c.getApplicationContext();new Thread(()->{try{JSONObject req=new JSONObject().put("title",title).put("chapterIndex",index).put("chapterName",chapterName==null?"":chapterName).put("content",text);ApiClient.post(app,"cache_chapter",req);}catch(Throwable ignored){}}).start();
    }
    public static String pullChapter(Context c,String title,int index){
        if(c==null||!ready(c))return "";try{String q="title="+URLEncoder.encode(title,"UTF-8")+"&chapterIndex="+index;JSONObject o=ApiClient.get(c,"cache_chapter",q);return o.optBoolean("ok",false)?o.optString("content",""):"";}catch(Throwable e){return "";}
    }
    private static boolean ready(Context c){return ServerConfig.isConfigured(c)&&AuthSession.loggedIn(c);}
}
