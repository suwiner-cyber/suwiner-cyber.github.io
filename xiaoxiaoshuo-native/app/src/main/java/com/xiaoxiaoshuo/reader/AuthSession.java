package com.xiaoxiaoshuo.reader;

import android.content.Context;
import android.content.SharedPreferences;

public final class AuthSession {
    private static final String PREF="auth_session";
    private AuthSession(){}
    public static void set(Context c,String username,String token){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString("username",username==null?"":username).putString("token",token==null?"":token).apply();}
    public static String token(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString("token","");}
    public static String username(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString("username","");}
    public static boolean loggedIn(Context c){return token(c).length()>10;}
    public static void clear(Context c){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().clear().apply();}
}
