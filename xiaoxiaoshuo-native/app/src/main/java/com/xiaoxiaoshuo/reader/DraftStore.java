package com.xiaoxiaoshuo.reader;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.util.*;

public final class DraftStore {
    public static final class Draft { public String id="",title="",content=""; public long updatedAt=0; public int words=0; }
    private DraftStore(){}
    public static Draft save(Context c,String id,String title,String content){
        Draft d=new Draft();d.id=(id==null||id.length()==0)?UUID.randomUUID().toString():id;d.title=(title==null||title.trim().length()==0)?"未命名作品":title.trim();d.content=content==null?"":content;d.updatedAt=System.currentTimeMillis();d.words=count(d.content);
        try{JSONObject o=new JSONObject().put("id",d.id).put("title",d.title).put("content",d.content).put("updatedAt",d.updatedAt).put("words",d.words);File f=file(c,d.id);write(f,o.toString());}catch(Throwable ignored){}
        DraftSync.sync(c,d);return d;
    }
    public static List<Draft> list(Context c){ArrayList<Draft> out=new ArrayList<>();File dir=dir(c);File[] fs=dir.listFiles();if(fs!=null)for(File f:fs)if(f.isFile()&&f.getName().endsWith(".json")){try{JSONObject o=new JSONObject(read(f));Draft d=new Draft();d.id=o.optString("id");d.title=o.optString("title","未命名作品");d.content=o.optString("content","");d.updatedAt=o.optLong("updatedAt",f.lastModified());d.words=o.optInt("words",count(d.content));if(d.id.length()>0)out.add(d);}catch(Throwable ignored){}}Collections.sort(out,(a,b)->Long.compare(b.updatedAt,a.updatedAt));return out;}
    public static Draft get(Context c,String id){for(Draft d:list(c))if(d.id.equals(id))return d;return new Draft();}
    public static void delete(Context c,String id){try{file(c,id).delete();}catch(Throwable ignored){}DraftSync.delete(c,id);}
    public static int count(String s){if(s==null)return 0;String x=s.trim();if(x.length()==0)return 0;int n=0;boolean latin=false;for(int i=0;i<x.length();i++){char ch=x.charAt(i);if(Character.isWhitespace(ch)){latin=false;continue;}if(ch>=0x4e00&&ch<=0x9fff){n++;latin=false;}else if(Character.isLetterOrDigit(ch)){if(!latin)n++;latin=true;}else latin=false;}return n;}
    private static File dir(Context c){File d=new File(c.getFilesDir(),"writing_drafts");d.mkdirs();return d;}
    private static File file(Context c,String id){return new File(dir(c),(id==null?"":id.replaceAll("[^A-Za-z0-9_-]",""))+".json");}
    private static void write(File f,String s)throws Exception{File tmp=new File(f.getAbsolutePath()+".tmp");FileOutputStream o=new FileOutputStream(tmp);o.write(s.getBytes("UTF-8"));o.flush();o.getFD().sync();o.close();if(f.exists())f.delete();if(!tmp.renameTo(f)){FileOutputStream x=new FileOutputStream(f);x.write(s.getBytes("UTF-8"));x.close();tmp.delete();}}
    private static String read(File f)throws Exception{FileInputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();return new String(out.toByteArray(),"UTF-8");}
}
