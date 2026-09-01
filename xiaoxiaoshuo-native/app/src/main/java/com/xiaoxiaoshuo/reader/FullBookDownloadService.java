package com.xiaoxiaoshuo.reader;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FullBookDownloadService extends IntentService {
    private static final Set<String> ENQUEUED=Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
    public FullBookDownloadService(){super("FullBookDownloadService");setIntentRedelivery(false);}

    public static void enqueue(Context c,String title){
        if(c==null||title==null||title.trim().length()==0)return;
        String key=title.trim();
        if(!ENQUEUED.add(key))return;
        Intent i=new Intent(c,FullBookDownloadService.class);i.putExtra("title",key);
        try{c.startService(i);}catch(Throwable e){ENQUEUED.remove(key);new Thread(()->downloadNow(c.getApplicationContext(),key)).start();}
    }

    @Override protected void onHandleIntent(Intent intent){
        String title=intent==null?"":intent.getStringExtra("title");
        if(title==null)title="";
        try{downloadNow(getApplicationContext(),title);}finally{ENQUEUED.remove(title);}
    }

    private static void downloadNow(Context c,String title){
        if(title==null||title.trim().length()==0)return;
        OfflineBookCache.BookData d=OfflineBookCache.loadBook(c,title);
        if(d.chapters.isEmpty()||d.sourceJson.length()==0)return;
        int total=d.chapters.size();
        if(OfflineBookCache.isComplete(c,title,total))return;
        int done=OfflineBookCache.cachedChapterCount(c,title,total);
        OfflineBookCache.setDownloadState(c,title,done,total,false,"正在下载全本");
        int consecutiveFail=0;
        for(int i=0;i<total;i++){
            LegacyDexBridge.Chapter ch=d.chapters.get(i);
            if(OfflineBookCache.getChapter(c,title,i,ch.url).trim().length()>0)continue;
            String text="";
            try{text=LegacyDexBridge.get(c).loadChapter(d.sourceJson,ch.name,ch.url);}catch(Throwable ignored){}
            if(text!=null&&text.trim().length()>0){
                OfflineBookCache.putChapter(c,title,i,ch.url,AiTypesetter.formatNovel(text));
                done++;consecutiveFail=0;
            }else{
                consecutiveFail++;
                OfflineBookCache.setDownloadState(c,title,done,total,false,"部分章节暂时下载失败，稍后继续");
                if(consecutiveFail>=8)break;
            }
            if(i%10==0||i==total-1)OfflineBookCache.setDownloadState(c,title,done,total,done>=total,"已缓存 "+done+" / "+total+" 章");
        }
        int finalDone=OfflineBookCache.cachedChapterCount(c,title,total);
        OfflineBookCache.setDownloadState(c,title,finalDone,total,finalDone>=total,finalDone>=total?"全本离线完成":"已缓存 "+finalDone+" / "+total+" 章，下次继续");
    }
}
