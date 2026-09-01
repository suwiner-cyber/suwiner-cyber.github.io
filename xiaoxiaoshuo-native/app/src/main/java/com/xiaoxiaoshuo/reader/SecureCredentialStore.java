package com.xiaoxiaoshuo.reader;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SecureCredentialStore {
    private static final String ALIAS="xiaoxiaoshuo_login_key_v1";
    private static final String PREF="secure_login";
    private SecureCredentialStore(){}

    public static void save(Context c,String username,String password,boolean remember){
        SharedPreferences.Editor e=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit();
        e.putString("username",username==null?"":username).putBoolean("remember",remember);
        if(remember&&password!=null&&password.length()>0){
            try{String[] x=encrypt(password);e.putString("password",x[0]).putString("iv",x[1]);}catch(Throwable ex){e.remove("password").remove("iv");}
        }else e.remove("password").remove("iv");
        e.apply();
    }
    public static String username(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getString("username","");}
    public static boolean remember(Context c){return c.getSharedPreferences(PREF,Context.MODE_PRIVATE).getBoolean("remember",false);}
    public static String password(Context c){
        SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);if(!p.getBoolean("remember",false))return "";
        try{return decrypt(p.getString("password",""),p.getString("iv",""));}catch(Throwable e){return "";}
    }
    public static void clear(Context c){c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().clear().apply();}

    private static SecretKey key()throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(ALIAS))return((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
        KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return kg.generateKey();
    }
    private static String[] encrypt(String s)throws Exception{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());byte[] out=c.doFinal(s.getBytes("UTF-8"));return new String[]{Base64.encodeToString(out,Base64.NO_WRAP),Base64.encodeToString(c.getIV(),Base64.NO_WRAP)};}
    private static String decrypt(String data,String iv)throws Exception{if(data==null||data.length()==0||iv==null||iv.length()==0)return "";Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));return new String(c.doFinal(Base64.decode(data,Base64.NO_WRAP)),"UTF-8");}
}
