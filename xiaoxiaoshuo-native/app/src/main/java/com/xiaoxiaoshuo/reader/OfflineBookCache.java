package com.xiaoxiaoshuo.reader;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

public final class OfflineBookCache {
    private OfflineBookCache(){}

    public static void saveBook(Context c, LegacyDexBridge.BookResult b, List<LegacyDexBridge.Chapter> chapters){
        if(c==null||b==null||b.title.trim().length()==0)return;
        try{
            File dir=bookDir(c,b.title);dir.mkdirs();
            JSONObject root=new JSONObject();
            root.put("title",b.title);root.put("author",b.author);root.put("intro",b.intro);root.put("cover",b.coverUrl);root.put("bookUrl",b.bookUrl);root.put("sourceName",b.sourceName);root.put("sourceUrl",b.sourceUrl);root.put("sourceJson",b.sourceJson);root.put("updatedAt",System.currentTimeMillis());
            JSONArray a=new JSONArray();if(chapters!=null)for(LegacyDexBridge.Chapter ch:chapters){JSONObject o=new JSONObject();o.put("name",ch.name);o.put("url",ch.url);a.put(o);}root.put("chapters",a);
            writeAtomic(new File(dir,"book.json"),root.toString().getBytes("UTF-8"));
        }catch(Throwable ignored){}
    }

    public static String getChapter(Context c,String title,int index,String chapterUrl){
        try{File f=chapterFile(c,title,index,chapterUrl);if(!f.isFile()||f.length()==0)return "";return readText(f,3*1024*1024);}catch(Throwable e){return "";}
    }

    public static void putChapter(Context c,String title,int index,String chapterUrl,String text){
        if(text==null||text.trim().length()==0)return;
        try{File dir=new File(bookDir(c,title),"chapters");dir.mkdirs();File f=chapterFile(c,title,index,chapterUrl);if(f.isFile()&&f.length()>0)return;writeAtomic(f,text.getBytes("UTF-8"));}catch(Throwable ignored){}
    }

    public static void deleteBook(Context c,String title,String coverUrl,String coverBase,String sourceJson){
        try{deleteTree(bookDir(c,title));}catch(Throwable ignored){}
        try{CoverLoader.deleteCached(c,coverUrl,coverBase);}catch(Throwable ignored){}
        try{CatalogStore.deleteFor(c,title,sourceJson);}catch(Throwable ignored){}
        try{c.getSharedPreferences("reading_progress",Context.MODE_PRIVATE).edit().remove("idx_"+Integer.toHexString(title.hashCode())).apply();}catch(Throwable ignored){}
    }

    public static boolean hasBook(Context c,String title){return new File(bookDir(c,title),"book.json").isFile();}

    private static File chapterFile(Context c,String title,int index,String url){File dir=new File(bookDir(c,title),"chapters");dir.mkdirs();String suffix=(url==null||url.length()==0)?"":("_"+shortHash(url));return new File(dir,String.format(Locale.US,"%06d%s.txt",Math.max(0,index),suffix));}
    private static File bookDir(Context c,String title){File root=new File(c.getFilesDir(),"offline_books");root.mkdirs();return new File(root,shortHash(title==null?"":title));}
    private static void writeAtomic(File f,byte[] data)throws Exception{File parent=f.getParentFile();if(parent!=null)parent.mkdirs();File tmp=new File(f.getAbsolutePath()+".tmp");FileOutputStream o=new FileOutputStream(tmp);o.write(data);o.flush();o.getFD().sync();o.close();if(f.exists())f.delete();if(!tmp.renameTo(f)){FileOutputStream x=new FileOutputStream(f);x.write(data);x.close();tmp.delete();}}
    private static String readText(File f,int max)throws Exception{FileInputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n,total=0;while((n=in.read(b))>0&&total<max){out.write(b,0,n);total+=n;}in.close();return new String(out.toByteArray(),"UTF-8");}
    private static void deleteTree(File f){if(f==null||!f.exists())return;if(f.isDirectory()){File[] cs=f.listFiles();if(cs!=null)for(File x:cs)deleteTree(x);}f.delete();}
    private static String shortHash(String s){try{MessageDigest m=MessageDigest.getInstance("SHA-256");byte[] b=m.digest(s.getBytes("UTF-8"));StringBuilder x=new StringBuilder();for(int i=0;i<12;i++)x.append(String.format(Locale.US,"%02x",b[i]));return x.toString();}catch(Throwable e){return Integer.toHexString(s.hashCode());}}
}
