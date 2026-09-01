package com.xiaoxiaoshuo.reader;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public final class CoverLoader {
    private static final ExecutorService POOL=Executors.newFixedThreadPool(4);
    private CoverLoader(){}

    public static void load(ImageView view,String raw,String base,String sourceJson,String title){
        if(view==null)return;
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setImageDrawable(new LiteraryCoverDrawable(title));
        final String url=resolve(raw,base);
        if(url.length()==0)return;
        final Context c=view.getContext().getApplicationContext();
        POOL.execute(()->{
            try{
                File f=coverFile(c,url); Bitmap bm=null;
                if(f.isFile()&&f.length()>128)bm=BitmapFactory.decodeFile(f.getAbsolutePath());
                if(bm==null){byte[] data=download(url,base,sourceJson,8*1024*1024);bm=BitmapFactory.decodeByteArray(data,0,data.length);if(bm!=null)writeIfChanged(f,data);}
                final Bitmap ready=bm;
                if(ready!=null)view.post(()->{if(view.getWindowToken()!=null)view.setImageDrawable(new BitmapDrawable(view.getResources(),ready));});
            }catch(Throwable ignored){}
        });
    }

    public static boolean probe(String raw,String base,String sourceJson){
        try{
            String url=resolve(raw,base);if(url.length()==0)return false;
            HttpURLConnection h=open(url,base,sourceJson);h.setReadTimeout(6500);InputStream in=h.getInputStream();byte[] head=new byte[32768];int n=in.read(head);in.close();if(n<=0)return false;
            BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(head,0,n,o);return o.outWidth>0&&o.outHeight>0;
        }catch(Throwable e){return false;}
    }

    public static void deleteCached(Context c,String raw,String base){
        try{String url=resolve(raw,base);if(url.length()==0)return;File f=coverFile(c.getApplicationContext(),url);if(f.exists())f.delete();File old=new File(c.getCacheDir(),"covers/"+hex(url)+".img");if(old.exists())old.delete();}catch(Throwable ignored){}
    }

    private static byte[] download(String url,String base,String sourceJson,int max)throws Exception{
        HttpURLConnection h=open(url,base,sourceJson);InputStream in=h.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[16384];int n,total=0;while((n=in.read(buf))>0&&total<max){out.write(buf,0,n);total+=n;}in.close();return out.toByteArray();
    }
    private static HttpURLConnection open(String url,String base,String sourceJson)throws Exception{
        HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();h.setConnectTimeout(6500);h.setReadTimeout(9000);h.setInstanceFollowRedirects(true);h.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 XiaoXiaoShuo/10.0");String referer=base;if(referer==null||referer.length()==0)referer=origin(url);if(referer.length()>0)h.setRequestProperty("Referer",referer);applyHeaders(h,sourceJson);return h;
    }
    private static File coverFile(Context c,String url){File dir=new File(c.getFilesDir(),"offline_covers");dir.mkdirs();return new File(dir,hex(url)+".img");}
    private static void writeIfChanged(File f,byte[] data)throws Exception{if(f.isFile()&&f.length()==data.length)return;File tmp=new File(f.getAbsolutePath()+".tmp");FileOutputStream fo=new FileOutputStream(tmp);fo.write(data);fo.flush();fo.getFD().sync();fo.close();if(f.exists())f.delete();if(!tmp.renameTo(f)){FileOutputStream o=new FileOutputStream(f);o.write(data);o.close();tmp.delete();}}
    private static void applyHeaders(HttpURLConnection h,String sourceJson){try{if(sourceJson==null||sourceJson.length()==0)return;JSONObject src=new JSONObject(sourceJson);Object x=src.opt("header");JSONObject o=null;if(x instanceof JSONObject)o=(JSONObject)x;else if(x instanceof String&&((String)x).trim().startsWith("{"))o=new JSONObject((String)x);if(o!=null){Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next(),v=o.optString(k,"");if(v.length()>0)h.setRequestProperty(k,v);}}}catch(Throwable ignored){}}
    public static String resolve(String raw,String base){try{if(raw==null)return "";raw=raw.trim();if(raw.startsWith("//"))return "https:"+raw;if(raw.startsWith("http://")||raw.startsWith("https://"))return raw;if(base!=null&&base.length()>0)return new URL(new URL(base),raw).toString();}catch(Throwable ignored){}return "";}
    private static String origin(String s){try{URL u=new URL(s);return u.getProtocol()+"://"+u.getHost()+"/";}catch(Exception e){return "";}}
    private static String hex(String s){try{MessageDigest m=MessageDigest.getInstance("SHA-256");byte[] b=m.digest(s.getBytes("UTF-8"));StringBuilder x=new StringBuilder();for(byte q:b)x.append(String.format(Locale.US,"%02x",q));return x.toString();}catch(Exception e){return Integer.toHexString(s.hashCode());}}
}
