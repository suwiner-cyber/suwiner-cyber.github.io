package com.xiaoxiaoshuo.reader;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.util.Base64;
import android.widget.ImageView;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public final class CoverLoader {
    private static final ExecutorService POOL=Executors.newFixedThreadPool(6);
    private static final String UA="Mozilla/5.0 (Linux; Android 12; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36";
    private CoverLoader(){}

    public static void load(ImageView view,String raw,String base,String sourceJson,String title){
        if(view==null)return;
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        view.setImageDrawable(new LiteraryCoverDrawable(title));
        final String original=clean(raw);
        if(original.length()==0)return;
        final Context c=view.getContext().getApplicationContext();
        final String tagKey=hex(original+"|"+safe(base));
        view.setTag(tagKey);
        POOL.execute(()->{
            Bitmap bm=null;
            try{
                if(original.startsWith("data:image/"))bm=decodeDataUri(original);
                if(bm==null){
                    List<String> candidates=resolveCandidates(original,base,sourceJson);
                    for(String url:candidates){
                        File f=coverFile(c,url);
                        if(f.isFile()&&f.length()>128){bm=BitmapFactory.decodeFile(f.getAbsolutePath());if(bm!=null)break;}
                        byte[] data=downloadWithFallbacks(url,base,sourceJson,8*1024*1024);
                        if(data.length>0){bm=BitmapFactory.decodeByteArray(data,0,data.length);if(bm!=null){writeIfChanged(f,data);break;}}
                    }
                }
            }catch(Throwable ignored){}
            final Bitmap ready=bm;
            if(ready!=null)view.post(()->{Object t=view.getTag();if(t!=null&&tagKey.equals(String.valueOf(t))&&view.getWindowToken()!=null)view.setImageDrawable(new BitmapDrawable(view.getResources(),ready));});
        });
    }

    public static boolean probe(String raw,String base,String sourceJson){
        try{
            String original=clean(raw);if(original.length()==0)return false;
            if(original.startsWith("data:image/"))return decodeDataUri(original)!=null;
            for(String url:resolveCandidates(original,base,sourceJson)){
                byte[] data=downloadWithFallbacks(url,base,sourceJson,256*1024);
                if(data.length==0)continue;
                BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,o);if(o.outWidth>0&&o.outHeight>0)return true;
            }
        }catch(Throwable ignored){}
        return false;
    }

    public static void deleteCached(Context c,String raw,String base){
        try{for(String url:resolveCandidates(clean(raw),base,"")){File f=coverFile(c.getApplicationContext(),url);if(f.exists())f.delete();File old=new File(c.getCacheDir(),"covers/"+hex(url)+".img");if(old.exists())old.delete();}}catch(Throwable ignored){}
    }

    private static byte[] downloadWithFallbacks(String url,String base,String sourceJson,int max){
        LinkedHashSet<String> refs=new LinkedHashSet<>();
        String sourceUrl=sourceUrl(sourceJson);
        if(base!=null&&base.trim().length()>0){refs.add(base.trim());refs.add(origin(base.trim()));}
        if(sourceUrl.length()>0){refs.add(sourceUrl);refs.add(origin(sourceUrl));}
        refs.add(origin(url));refs.add("");
        for(String ref:refs){
            try{byte[] b=download(url,ref,sourceJson,max);if(b.length>0)return b;}catch(Throwable ignored){}
        }
        return new byte[0];
    }

    private static byte[] download(String first,String referer,String sourceJson,int max)throws Exception{
        String current=first;
        for(int redirect=0;redirect<5;redirect++){
            HttpURLConnection h=open(current,referer,sourceJson);
            int code=h.getResponseCode();
            if(code>=300&&code<400){String loc=h.getHeaderField("Location");h.disconnect();if(loc==null||loc.length()==0)throw new IOException("redirect without location");current=new URL(new URL(current),loc).toString();continue;}
            if(code<200||code>=300){h.disconnect();throw new IOException("HTTP "+code);}
            String type=h.getContentType();
            InputStream in=h.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[16384];int n,total=0;while((n=in.read(buf))>0&&total<max){out.write(buf,0,n);total+=n;}in.close();h.disconnect();byte[] data=out.toByteArray();
            if(data.length==0)throw new IOException("empty image");
            if(type!=null&&type.toLowerCase(Locale.ROOT).contains("text/html")&&data.length<128*1024)throw new IOException("html instead of image");
            return data;
        }
        return new byte[0];
    }

    private static HttpURLConnection open(String url,String referer,String sourceJson)throws Exception{
        HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();
        h.setConnectTimeout(7000);h.setReadTimeout(10000);h.setInstanceFollowRedirects(false);h.setUseCaches(true);
        h.setRequestProperty("User-Agent",UA);h.setRequestProperty("Accept","image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");h.setRequestProperty("Accept-Language","zh-CN,zh;q=0.9,en;q=0.6");h.setRequestProperty("Connection","keep-alive");
        if(referer!=null&&referer.length()>0)h.setRequestProperty("Referer",referer);
        applyHeaders(h,sourceJson);return h;
    }

    private static void applyHeaders(HttpURLConnection h,String sourceJson){
        try{
            if(sourceJson==null||sourceJson.trim().length()==0)return;
            JSONObject src=new JSONObject(sourceJson);
            Object x=src.opt("header");if(x==null||x==JSONObject.NULL)x=src.opt("bookSourceHeader");if(x==null||x==JSONObject.NULL)x=src.opt("headers");
            if(x instanceof JSONObject){JSONObject o=(JSONObject)x;Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next(),v=o.optString(k,"");if(v.length()>0)h.setRequestProperty(k,v);}}
            else if(x instanceof String)applyHeaderString(h,(String)x);
        }catch(Throwable ignored){}
    }

    private static void applyHeaderString(HttpURLConnection h,String raw){
        try{
            String s=raw==null?"":raw.trim();if(s.length()==0)return;
            if(s.startsWith("{")&&s.endsWith("}")){JSONObject o=new JSONObject(s);Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next(),v=o.optString(k,"");if(v.length()>0)h.setRequestProperty(k,v);}return;}
            for(String line:s.split("[\\r\\n;]+")){int p=line.indexOf(':');if(p>0){String k=line.substring(0,p).trim(),v=line.substring(p+1).trim();if(k.length()>0&&v.length()>0)h.setRequestProperty(k,v);}}
        }catch(Throwable ignored){}
    }

    private static List<String> resolveCandidates(String raw,String base,String sourceJson){
        LinkedHashSet<String> out=new LinkedHashSet<>();String s=clean(raw);if(s.length()==0)return new ArrayList<>();
        if(s.startsWith("data:image/")){out.add(s);return new ArrayList<>(out);}
        if(s.startsWith("//"))s="https:"+s;
        if(s.startsWith("http://")||s.startsWith("https://")){out.add(s);return new ArrayList<>(out);}
        String source=sourceUrl(sourceJson);
        addResolved(out,s,base);addResolved(out,s,source);
        if(base!=null&&base.length()>0)addResolved(out,s,origin(base));
        if(source.length()>0)addResolved(out,s,origin(source));
        return new ArrayList<>(out);
    }

    private static void addResolved(Set<String> out,String raw,String base){try{if(base==null||base.trim().length()==0)return;out.add(new URL(new URL(base.trim()),raw).toString());}catch(Throwable ignored){}}
    public static String resolve(String raw,String base){List<String> x=resolveCandidates(clean(raw),base,"");return x.isEmpty()?"":x.get(0);}
    private static String sourceUrl(String sourceJson){try{if(sourceJson==null||sourceJson.trim().length()==0)return "";JSONObject o=new JSONObject(sourceJson);String s=o.optString("bookSourceUrl","");if(s.length()==0)s=o.optString("sourceUrl","");if(s.length()==0)s=o.optString("url","");return s;}catch(Throwable e){return "";}}
    private static String clean(String raw){if(raw==null)return "";String s=raw.trim().replace("&amp;","&").replace("\\u0026","&");while(s.length()>1&&((s.startsWith("\"")&&s.endsWith("\""))||(s.startsWith("'")&&s.endsWith("'"))))s=s.substring(1,s.length()-1).trim();return s;}
    private static Bitmap decodeDataUri(String s){try{int comma=s.indexOf(',');if(comma<0)return null;byte[] data=Base64.decode(s.substring(comma+1),Base64.DEFAULT);return BitmapFactory.decodeByteArray(data,0,data.length);}catch(Throwable e){return null;}}
    private static File coverFile(Context c,String url){File dir=new File(c.getFilesDir(),"offline_covers");dir.mkdirs();return new File(dir,hex(url)+".img");}
    private static void writeIfChanged(File f,byte[] data)throws Exception{if(f.isFile()&&f.length()==data.length)return;File tmp=new File(f.getAbsolutePath()+".tmp");FileOutputStream fo=new FileOutputStream(tmp);fo.write(data);fo.flush();fo.getFD().sync();fo.close();if(f.exists())f.delete();if(!tmp.renameTo(f)){FileOutputStream o=new FileOutputStream(f);o.write(data);o.close();tmp.delete();}}
    private static String origin(String s){try{URL u=new URL(s);return u.getProtocol()+"://"+u.getHost()+((u.getPort()>0)?(":"+u.getPort()):"")+"/";}catch(Exception e){return "";}}
    private static String safe(String s){return s==null?"":s;}
    private static String hex(String s){try{MessageDigest m=MessageDigest.getInstance("SHA-256");byte[] b=m.digest(s.getBytes("UTF-8"));StringBuilder x=new StringBuilder();for(byte q:b)x.append(String.format(Locale.US,"%02x",q));return x.toString();}catch(Exception e){return Integer.toHexString(s.hashCode());}}
}
