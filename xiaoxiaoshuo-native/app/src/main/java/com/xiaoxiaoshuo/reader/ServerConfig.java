package com.xiaoxiaoshuo.reader;

import android.content.Context;

public final class ServerConfig {
    private static final String PREF="server_config";
    private static final String KEY="api_base";
    private ServerConfig(){}
    public static String get(Context c){
        String s=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString(KEY,"");
        return normalize(s);
    }
    public static void set(Context c,String url){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY,normalize(url)).apply();}
    public static boolean isConfigured(Context c){return get(c).length()>0;}
    public static String endpoint(Context c,String action){String b=get(c);return b.length()==0?"":b+"/api.php?action="+action;}
    private static String normalize(String s){if(s==null)return "";s=s.trim();while(s.endsWith("/"))s=s.substring(0,s.length()-1);if(!(s.startsWith("http://")||s.startsWith("https://")))return "";return s;}
}
