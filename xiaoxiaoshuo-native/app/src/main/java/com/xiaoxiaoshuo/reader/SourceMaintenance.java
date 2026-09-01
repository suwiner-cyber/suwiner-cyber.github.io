package com.xiaoxiaoshuo.reader;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

public final class SourceMaintenance {
    public static final String ACTION_DAILY="com.xiaoxiaoshuo.reader.DAILY_SOURCE_MAINTENANCE";
    private static final long DAY=24L*60L*60L*1000L;
    private static final String INDEX="https://www.yckceo.com/yuedu/shuyuans/index.html";
    private SourceMaintenance(){}

    public static void schedule(Context c){
        try{
            AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            Intent i=new Intent(c,SourceMaintenanceReceiver.class).setAction(ACTION_DAILY);
            PendingIntent pi=PendingIntent.getBroadcast(c,9531,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            long first=System.currentTimeMillis()+DAY;
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP,first,DAY,pi);
        }catch(Throwable ignored){}
    }
    public static void runIfDue(Context c){
        long last=c.getSharedPreferences("source_maintenance",Context.MODE_PRIVATE).getLong("last_run",0L);
        if(System.currentTimeMillis()-last>=DAY) runAsync(c,false,null);
    }
    public static void runAsync(Context c,boolean force,Runnable done){
        Context app=c.getApplicationContext();
        new Thread(()->{try{run(app,force);}catch(Throwable ignored){}finally{if(done!=null)done.run();}},"source-maintenance").start();
    }
    private static void run(Context c,boolean force)throws Exception{
        SharedPreferences state=c.getSharedPreferences("source_maintenance",Context.MODE_PRIVATE);
        long now=System.currentTimeMillis(),last=state.getLong("last_run",0L);
        if(!force&&now-last<DAY)return;
        int imported=syncRepository(c);
        int removed=healthSweep(c);
        state.edit().putLong("last_run",now).putInt("last_imported",imported).putInt("last_removed",removed).apply();
    }

    private static int syncRepository(Context c){
        LinkedHashSet<String> detailUrls=new LinkedHashSet<>();
        try{
            for(int page=1;page<=3;page++){
                String url=page==1?INDEX:"https://www.yckceo.com/yuedu/shuyuans/index/page/"+page+".html";
                String html=getText(url,12000,2*1024*1024);
                Matcher m=Pattern.compile("href=[\"']([^\"']+/yuedu/shuyuans/content/id/\\d+\\.html)[\"']",Pattern.CASE_INSENSITIVE).matcher(html);
                while(m.find()&&detailUrls.size()<90)detailUrls.add(abs(m.group(1),url));
            }
        }catch(Throwable ignored){}
        LinkedHashMap<String,String> jsonBySource=new LinkedHashMap<>();
        int checked=0;
        for(String detail:detailUrls){
            if(checked++>=60)break;
            try{
                String html=getText(detail,12000,2*1024*1024);
                LinkedHashSet<String> downloads=new LinkedHashSet<>();
                Matcher h=Pattern.compile("href=[\"']([^\"']+)[\"']",Pattern.CASE_INSENSITIVE).matcher(html);
                while(h.find()){
                    String u=abs(h.group(1),detail).replace("&amp;","&");
                    String low=u.toLowerCase(Locale.ROOT);
                    if(low.contains(".json")||low.contains(".zip")||low.contains("download")||low.contains("down/"))downloads.add(u);
                }
                for(String u:downloads){
                    try{for(String json:downloadSourcePayloads(u)){String sourceUrl=sourceUrl(json);if(sourceUrl.length()>0)jsonBySource.put(sourceUrl,json);}}catch(Throwable ignored){}
                }
                Matcher inline=Pattern.compile("\\{[^{}]{0,3000}[\"']?bookSourceUrl[\"']?\\s*:\\s*[\"'][^\"']+[\"'][^{}]{0,12000}\\}",Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(html);
                while(inline.find()){String raw=htmlDecode(inline.group());try{JSONObject o=new JSONObject(raw);String su=o.optString("bookSourceUrl","");if(su.startsWith("http"))jsonBySource.put(su,o.toString());}catch(Throwable ignored){}}
            }catch(Throwable ignored){}
        }
        if(jsonBySource.isEmpty())return 0;
        return LegacySourceStore.replaceAutoSources(c,new ArrayList<>(jsonBySource.values()));
    }

    private static List<String> downloadSourcePayloads(String url)throws Exception{
        byte[] bytes=getBytes(url,15000,5*1024*1024);ArrayList<String> out=new ArrayList<>();
        if(bytes.length>=4&&bytes[0]=='P'&&bytes[1]=='K'){
            ZipInputStream zin=new ZipInputStream(new ByteArrayInputStream(bytes));ZipEntry e;int files=0;
            while((e=zin.getNextEntry())!=null&&files++<40){if(e.isDirectory())continue;String name=e.getName().toLowerCase(Locale.ROOT);if(!name.endsWith(".json")&&!name.endsWith(".txt"))continue;ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n,total=0;while((n=zin.read(buf))>0&&total<4*1024*1024){b.write(buf,0,n);total+=n;}parseJsonPayload(new String(b.toByteArray(),"UTF-8"),out);}zin.close();
        }else parseJsonPayload(new String(bytes,"UTF-8"),out);
        return out;
    }
    private static void parseJsonPayload(String raw,List<String> out){
        try{Object root=new JSONTokener(raw.trim()).nextValue();if(root instanceof JSONArray){JSONArray a=(JSONArray)root;for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optString("bookSourceUrl","").startsWith("http"))out.add(o.toString());}}else if(root instanceof JSONObject){JSONObject o=(JSONObject)root;if(o.optString("bookSourceUrl","").startsWith("http"))out.add(o.toString());}}catch(Throwable ignored){}
    }
    private static int healthSweep(Context c){
        LegacySourceStore.State st=LegacySourceStore.prepare(c,Integer.MAX_VALUE);int removed=0,checked=0;
        for(LegacySourceStore.SourceInfo s:st.all){if(checked++>=180)break;boolean ok=false;long start=System.currentTimeMillis();
            try{URL u=new URL(s.url);HttpURLConnection h=(HttpURLConnection)u.openConnection();h.setConnectTimeout(5000);h.setReadTimeout(6000);h.setInstanceFollowRedirects(true);h.setRequestProperty("User-Agent","Mozilla/5.0 XiaoXiaoShuo/10.0");h.setRequestMethod("GET");int code=h.getResponseCode();ok=code>=200&&code<500;try{InputStream in=h.getInputStream();if(in!=null)in.close();}catch(Throwable ignored){}h.disconnect();}catch(Throwable ignored){}
            long latency=Math.max(1,System.currentTimeMillis()-start);LegacySourceStore.recordHealth(c,s.url,ok,latency);if(!ok&&LegacySourceStore.failureCount(c,s.url)>=3){if(LegacySourceStore.removeNonLegacySource(c,s.url))removed++;else LegacySourceStore.blacklist(c,s.url);}
        }
        return removed;
    }
    private static String sourceUrl(String json){try{return new JSONObject(json).optString("bookSourceUrl","").trim();}catch(Throwable e){return "";}}
    private static String abs(String href,String base){try{return new URL(new URL(base),href).toString();}catch(Throwable e){return href;}}
    private static String htmlDecode(String s){return s.replace("&quot;","\"").replace("&#34;","\"").replace("&amp;","&").replace("&#39;","'").replace("&lt;","<").replace("&gt;",">");}
    private static String getText(String url,int timeout,int max)throws Exception{return new String(getBytes(url,timeout,max),"UTF-8");}
    private static byte[] getBytes(String url,int timeout,int max)throws Exception{HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();h.setConnectTimeout(timeout);h.setReadTimeout(timeout);h.setInstanceFollowRedirects(true);h.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 XiaoXiaoShuo/10.0");InputStream in=h.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n,total=0;while((n=in.read(b))>0&&total<max){out.write(b,0,n);total+=n;}in.close();h.disconnect();return out.toByteArray();}
}
