package com.xiaoxiaoshuo.reader;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

public final class OfflineBookCache {
    private OfflineBookCache(){}

    public static void saveBook(Context c, LegacyDexBridge.BookResult b, List<LegacyDexBridge.Chapter> chapters){
        saveBookMetadata(c,b,chapters);
        if(c!=null&&b!=null&&chapters!=null&&!chapters.isEmpty()) FullBookDownloadService.enqueue(c,b.title);
    }

    public static void saveBookMetadata(Context c, LegacyDexBridge.BookResult b, List<LegacyDexBridge.Chapter> chapters){
        if(c==null||b==null||b.title==null||b.title.trim().length()==0)return;
        try{
            File dir=bookDir(c,b.title);dir.mkdirs();
            JSONObject root=new JSONObject();
            root.put("title",safe(b.title));root.put("author",safe(b.author));root.put("intro",safe(b.intro));root.put("cover",safe(b.coverUrl));root.put("bookUrl",safe(b.bookUrl));root.put("sourceName",safe(b.sourceName));root.put("sourceUrl",safe(b.sourceUrl));root.put("sourceJson",safe(b.sourceJson));root.put("updatedAt",System.currentTimeMillis());
            JSONArray a=new JSONArray();if(chapters!=null)for(LegacyDexBridge.Chapter ch:chapters){JSONObject o=new JSONObject();o.put("name",safe(ch.name));o.put("url",safe(ch.url));a.put(o);}root.put("chapters",a);
            writeAtomic(new File(dir,"book.json"),root.toString().getBytes("UTF-8"));
        }catch(Throwable ignored){}
    }

    public static BookData loadBook(Context c,String title){
        BookData d=new BookData();
        try{
            File f=new File(bookDir(c,title),"book.json");if(!f.isFile())return d;
            JSONObject root=new JSONObject(readText(f,6*1024*1024));
            d.title=root.optString("title",title);d.author=root.optString("author","");d.intro=root.optString("intro","");d.cover=root.optString("cover","");d.bookUrl=root.optString("bookUrl","");d.sourceName=root.optString("sourceName","");d.sourceUrl=root.optString("sourceUrl","");d.sourceJson=root.optString("sourceJson","");
            JSONArray a=root.optJSONArray("chapters");if(a!=null)for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)d.chapters.add(new LegacyDexBridge.Chapter(o.optString("name",""),o.optString("url","")));}
        }catch(Throwable ignored){}
        return d;
    }

    public static String getChapter(Context c,String title,int index,String chapterUrl){
        try{
            File canonical=canonicalChapterFile(c,title,index);if(canonical.isFile()&&canonical.length()>0)return readText(canonical,5*1024*1024);
            File old=legacyChapterFile(c,title,index,chapterUrl);if(old.isFile()&&old.length()>0)return readText(old,5*1024*1024);
            File dir=new File(bookDir(c,title),"chapters");String prefix=String.format(Locale.US,"%06d",Math.max(0,index));File[] fs=dir.listFiles();if(fs!=null)for(File f:fs)if(f.isFile()&&f.getName().startsWith(prefix)&&f.length()>0)return readText(f,5*1024*1024);
        }catch(Throwable ignored){}
        return "";
    }

    public static void putChapter(Context c,String title,int index,String chapterUrl,String text){
        if(text==null||text.trim().length()==0)return;
        try{File f=canonicalChapterFile(c,title,index);if(f.isFile()&&f.length()>0)return;writeAtomic(f,text.getBytes("UTF-8"));}catch(Throwable ignored){}
    }

    public static int cachedChapterCount(Context c,String title,int total){
        int n=0;try{for(int i=0;i<Math.max(0,total);i++)if(canonicalChapterFile(c,title,i).isFile()&&canonicalChapterFile(c,title,i).length()>0)n++;}catch(Throwable ignored){}return n;
    }

    public static void setDownloadState(Context c,String title,int done,int total,boolean complete,String message){
        try{c.getSharedPreferences("offline_download",Context.MODE_PRIVATE).edit().putInt(key(title)+"_done",done).putInt(key(title)+"_total",total).putBoolean(key(title)+"_complete",complete).putString(key(title)+"_message",message==null?"":message).putLong(key(title)+"_updated",System.currentTimeMillis()).apply();}catch(Throwable ignored){}
    }

    public static boolean isComplete(Context c,String title,int total){
        try{android.content.SharedPreferences p=c.getSharedPreferences("offline_download",Context.MODE_PRIVATE);return p.getBoolean(key(title)+"_complete",false)&&p.getInt(key(title)+"_total",0)==total;}catch(Throwable e){return false;}
    }

    public static void deleteBook(Context c,String title,String coverUrl,String coverBase,String sourceJson){
        try{deleteTree(bookDir(c,title));}catch(Throwable ignored){}
        try{CoverLoader.deleteCached(c,coverUrl,coverBase);}catch(Throwable ignored){}
        try{CatalogStore.deleteFor(c,title,sourceJson);}catch(Throwable ignored){}
        try{c.getSharedPreferences("reading_progress",Context.MODE_PRIVATE).edit().remove("idx_"+Integer.toHexString(title.hashCode())).apply();}catch(Throwable ignored){}
        try{android.content.SharedPreferences p=c.getSharedPreferences("offline_download",Context.MODE_PRIVATE);p.edit().remove(key(title)+"_done").remove(key(title)+"_total").remove(key(title)+"_complete").remove(key(title)+"_message").remove(key(title)+"_updated").apply();}catch(Throwable ignored){}
    }

    public static boolean hasBook(Context c,String title){return new File(bookDir(c,title),"book.json").isFile();}

    private static File canonicalChapterFile(Context c,String title,int index){File dir=new File(bookDir(c,title),"chapters");dir.mkdirs();return new File(dir,String.format(Locale.US,"%06d.txt",Math.max(0,index)));}
    private static File legacyChapterFile(Context c,String title,int index,String url){File dir=new File(bookDir(c,title),"chapters");dir.mkdirs();String suffix=(url==null||url.length()==0)?"":("_"+shortHash(url));return new File(dir,String.format(Locale.US,"%06d%s.txt",Math.max(0,index),suffix));}
    private static File bookDir(Context c,String title){File root=new File(c.getFilesDir(),"offline_books");root.mkdirs();return new File(root,shortHash(title==null?"":title));}
    private static void writeAtomic(File f,byte[] data)throws Exception{File parent=f.getParentFile();if(parent!=null)parent.mkdirs();File tmp=new File(f.getAbsolutePath()+".tmp");FileOutputStream o=new FileOutputStream(tmp);o.write(data);o.flush();o.getFD().sync();o.close();if(f.exists())f.delete();if(!tmp.renameTo(f)){FileOutputStream x=new FileOutputStream(f);x.write(data);x.close();tmp.delete();}}
    private static String readText(File f,int max)throws Exception{FileInputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n,total=0;while((n=in.read(b))>0&&total<max){out.write(b,0,n);total+=n;}in.close();return new String(out.toByteArray(),"UTF-8");}
    private static void deleteTree(File f){if(f==null||!f.exists())return;if(f.isDirectory()){File[] cs=f.listFiles();if(cs!=null)for(File x:cs)deleteTree(x);}f.delete();}
    private static String shortHash(String s){try{MessageDigest m=MessageDigest.getInstance("SHA-256");byte[] b=m.digest(s.getBytes("UTF-8"));StringBuilder x=new StringBuilder();for(int i=0;i<12;i++)x.append(String.format(Locale.US,"%02x",b[i]));return x.toString();}catch(Throwable e){return Integer.toHexString(s.hashCode());}}
    private static String key(String title){return "book_"+shortHash(title==null?"":title);}
    private static String safe(String s){return s==null?"":s;}

    public static final class BookData{
        public String title="",author="",intro="",cover="",bookUrl="",sourceName="",sourceUrl="",sourceJson="";
        public final ArrayList<LegacyDexBridge.Chapter> chapters=new ArrayList<>();
    }
}
