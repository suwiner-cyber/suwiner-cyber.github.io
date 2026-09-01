package com.xiaoxiaoshuo.reader;

import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.net.*;

public final class ApiClient {
    private ApiClient(){}
    public static JSONObject post(Context c,String action,JSONObject body)throws Exception{
        String url=ServerConfig.endpoint(c,action);if(url.length()==0)throw new IOException("服务器地址未配置");
        HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();h.setConnectTimeout(8000);h.setReadTimeout(12000);h.setRequestMethod("POST");h.setDoOutput(true);h.setRequestProperty("Content-Type","application/json; charset=utf-8");h.setRequestProperty("Accept","application/json");String token=AuthSession.token(c);if(token.length()>0)h.setRequestProperty("Authorization","Bearer "+token);
        byte[] data=(body==null?new JSONObject():body).toString().getBytes("UTF-8");OutputStream out=h.getOutputStream();out.write(data);out.flush();out.close();int code=h.getResponseCode();InputStream in=code>=200&&code<400?h.getInputStream():h.getErrorStream();String text=read(in);h.disconnect();JSONObject o=text.trim().startsWith("{")?new JSONObject(text):new JSONObject().put("ok",false).put("message","服务器返回异常");if(code<200||code>=400)o.put("ok",false);return o;
    }
    public static JSONObject get(Context c,String action,String query)throws Exception{
        String url=ServerConfig.endpoint(c,action);if(url.length()==0)throw new IOException("服务器地址未配置");if(query!=null&&query.length()>0)url+="&"+query;
        HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();h.setConnectTimeout(8000);h.setReadTimeout(12000);h.setRequestProperty("Accept","application/json");String token=AuthSession.token(c);if(token.length()>0)h.setRequestProperty("Authorization","Bearer "+token);int code=h.getResponseCode();InputStream in=code>=200&&code<400?h.getInputStream():h.getErrorStream();String text=read(in);h.disconnect();JSONObject o=text.trim().startsWith("{")?new JSONObject(text):new JSONObject().put("ok",false).put("message","服务器返回异常");if(code<200||code>=400)o.put("ok",false);return o;
    }
    private static String read(InputStream in)throws Exception{if(in==null)return "{}";ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))>0&&total<10*1024*1024){out.write(b,0,n);total+=n;}in.close();return new String(out.toByteArray(),"UTF-8");}
}
