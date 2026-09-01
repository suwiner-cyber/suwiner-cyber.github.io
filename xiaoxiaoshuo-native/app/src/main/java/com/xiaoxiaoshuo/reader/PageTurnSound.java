package com.xiaoxiaoshuo.reader;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PageTurnSound {
    private static final int RATE=22050;
    private static final ExecutorService AUDIO=Executors.newSingleThreadExecutor();
    private static volatile byte[] PCM;
    private PageTurnSound(){}

    public static void play(Context c){
        if(c==null)return;
        if(!c.getSharedPreferences("reader_settings",Context.MODE_PRIVATE).getBoolean("page_sound",true))return;
        AUDIO.execute(()->{
            AudioTrack track=null;
            try{
                byte[] pcm=pcm();
                track=new AudioTrack(AudioManager.STREAM_MUSIC,RATE,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,pcm.length,AudioTrack.MODE_STATIC);
                track.write(pcm,0,pcm.length);
                track.setStereoVolume(.22f,.22f);
                track.play();
                Thread.sleep(180);
            }catch(Throwable ignored){}finally{try{if(track!=null){track.stop();track.release();}}catch(Throwable ignored){}}
        });
    }

    private static byte[] pcm(){
        if(PCM!=null)return PCM;
        synchronized(PageTurnSound.class){
            if(PCM!=null)return PCM;
            int ms=165,n=RATE*ms/1000;byte[] out=new byte[n*2];Random r=new Random(5319);double prev=0;
            for(int i=0;i<n;i++){
                double t=i/(double)n;
                double envelope=Math.sin(Math.PI*Math.min(1.0,t*1.18))*Math.pow(1.0-t,.72);
                double white=r.nextDouble()*2.0-1.0;
                prev=prev*.72+white*.28;
                double scratch=prev*.72+(r.nextDouble()*2.0-1.0)*.28;
                double sweep=Math.sin(2*Math.PI*(720+1800*t)*i/RATE)*.12;
                double v=(scratch+sweep)*envelope*.36;
                short s=(short)Math.max(Short.MIN_VALUE,Math.min(Short.MAX_VALUE,(int)(v*32767)));
                out[i*2]=(byte)(s&0xff);out[i*2+1]=(byte)((s>>8)&0xff);
            }
            PCM=out;return out;
        }
    }
}
