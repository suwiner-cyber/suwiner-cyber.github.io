package com.xiaoxiaoshuo.reader;

import android.content.Context;
import org.json.JSONObject;

public final class DraftSync {
    private DraftSync(){}
    public static void sync(Context c,DraftStore.Draft d){if(c==null||d==null||!ServerConfig.isConfigured(c)||!AuthSession.loggedIn(c))return;Context app=c.getApplicationContext();new Thread(()->{try{JSONObject req=new JSONObject().put("id",d.id).put("title",d.title).put("content",d.content).put("updatedAt",d.updatedAt).put("words",d.words);ApiClient.post(app,"draft_save",req);}catch(Throwable ignored){}}).start();}
    public static void delete(Context c,String id){if(c==null||id==null||!ServerConfig.isConfigured(c)||!AuthSession.loggedIn(c))return;Context app=c.getApplicationContext();new Thread(()->{try{ApiClient.post(app,"draft_delete",new JSONObject().put("id",id));}catch(Throwable ignored){}}).start();}
}
