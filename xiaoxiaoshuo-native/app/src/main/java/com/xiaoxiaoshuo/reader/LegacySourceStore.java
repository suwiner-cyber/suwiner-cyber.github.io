package com.xiaoxiaoshuo.reader;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.util.*;

public final class LegacySourceStore {
    public static final class SourceInfo {
        public String url="", name="", group="", json="";
        public boolean enabled, compatible;
        public int healthState, failures, score;
        public long latency, checkedAt, successAt;
    }
    public static final class BookInfo {
        public String title="", author="", intro="", cover="", content="";
    }
    public static final class State {
        public final ArrayList<SourceInfo> all=new ArrayList<>();
        public final ArrayList<SourceInfo> selected=new ArrayList<>();
        public boolean legacyDatabaseFound;
        public String message="";
    }

    private LegacySourceStore(){}

    public static State prepare(Context context, int limit){
        State out=new State();
        File dbFile=context.getDatabasePath("xiaoxiaoshuo.db");
        if(!dbFile.exists()){
            out.message="未检测到旧书源数据库";
            return out;
        }
        out.legacyDatabaseFound=true;
        SQLiteDatabase db=null; Cursor c=null;
        try{
            db=SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
            if(!tableExists(db,"sources")){
                out.message="旧数据库存在，但没有书源表";
                return out;
            }
            c=db.rawQuery("SELECT * FROM sources",null);
            while(c.moveToNext()){
                SourceInfo s=new SourceInfo();
                s.url=str(c,"source_url"); s.name=str(c,"name"); s.group=str(c,"source_group"); s.json=str(c,"json");
                s.enabled=num(c,"enabled",1)==1; s.compatible=num(c,"compatible",0)==1;
                s.healthState=num(c,"health_state",0); s.latency=longNum(c,"health_latency",0); s.failures=num(c,"health_failures",0);
                s.checkedAt=longNum(c,"health_checked_at",0); s.successAt=longNum(c,"health_success_at",0);
                if(s.url.startsWith("http://") || s.url.startsWith("https://")){
                    s.score=score(s); out.all.add(s);
                }
            }
        }catch(Throwable e){ out.message="旧书源读取失败："+e.getClass().getSimpleName(); }
        finally{ if(c!=null)c.close(); if(db!=null)db.close(); }

        ArrayList<SourceInfo> candidates=new ArrayList<>();
        for(SourceInfo s:out.all) if(s.enabled) candidates.add(s);
        Collections.sort(candidates,(a,b)->{
            int by=Integer.compare(b.score,a.score); if(by!=0)return by;
            int lat=Long.compare(normalLatency(a.latency),normalLatency(b.latency)); if(lat!=0)return lat;
            return a.name.compareToIgnoreCase(b.name);
        });
        for(SourceInfo s:candidates){ if(out.selected.size()>=limit)break; out.selected.add(s); }
        persistSelection(context,out.selected);
        if(out.message.length()==0) out.message="已从旧数据库恢复 "+out.all.size()+" 个书源，默认优选 "+out.selected.size()+" 个";
        return out;
    }

    public static ArrayList<BookInfo> loadLegacyBooks(Context context,int limit){
        ArrayList<BookInfo> list=new ArrayList<>(); File f=context.getDatabasePath("xiaoxiaoshuo.db");
        if(!f.exists())return list; SQLiteDatabase db=null; Cursor c=null;
        try{
            db=SQLiteDatabase.openDatabase(f.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
            if(!tableExists(db,"books"))return list;
            c=db.rawQuery("SELECT * FROM books ORDER BY last_read_at DESC, updated_at DESC LIMIT "+Math.max(1,limit),null);
            while(c.moveToNext()){
                BookInfo b=new BookInfo(); b.title=str(c,"title"); b.author=str(c,"author"); b.intro=AiTypesetter.compactIntro(str(c,"intro")); b.cover=str(c,"cover_url"); b.content=str(c,"content");
                if(b.title.length()>0)list.add(b);
            }
        }catch(Throwable ignored){} finally{ if(c!=null)c.close(); if(db!=null)db.close(); }
        return list;
    }

    private static void persistSelection(Context context,List<SourceInfo> selected){
        LinkedHashSet<String> urls=new LinkedHashSet<>(); for(SourceInfo s:selected)urls.add(s.url);
        SharedPreferences p=context.getSharedPreferences("source_selection",Context.MODE_PRIVATE);
        p.edit().putStringSet("smart_80_urls",urls).putInt("smart_80_count",urls.size()).putLong("smart_80_updated",System.currentTimeMillis()).apply();
    }

    private static int score(SourceInfo s){
        int v=0; if(s.compatible)v+=50000; if(s.healthState==1)v+=45000; else if(s.healthState==0)v+=8000; else v-=25000;
        if(s.enabled)v+=15000; if(s.successAt>0)v+=12000; v-=Math.min(20000,s.failures*2500);
        if(s.latency>0){ if(s.latency<800)v+=12000; else if(s.latency<1500)v+=9000; else if(s.latency<3000)v+=5000; else v-=3000; }
        String u=s.url.toLowerCase(Locale.ROOT); if(u.startsWith("https://"))v+=2500;
        if(s.json!=null && s.json.length()>100)v+=1500;
        return v;
    }
    private static long normalLatency(long l){return l<=0?Long.MAX_VALUE/4:l;}
    private static boolean tableExists(SQLiteDatabase db,String table){ Cursor c=null; try{ c=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?",new String[]{table}); return c.moveToFirst(); }catch(Throwable e){return false;}finally{if(c!=null)c.close();} }
    private static String str(Cursor c,String n){ int i=c.getColumnIndex(n); if(i<0||c.isNull(i))return ""; String s=c.getString(i); return s==null?"":s; }
    private static int num(Cursor c,String n,int d){ int i=c.getColumnIndex(n); return i<0||c.isNull(i)?d:c.getInt(i); }
    private static long longNum(Cursor c,String n,long d){ int i=c.getColumnIndex(n); return i<0||c.isNull(i)?d:c.getLong(i); }
}
