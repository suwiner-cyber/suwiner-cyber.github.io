package com.xiaoxiaoshuo.reader;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PageTurnSound {
    private static final int RATE=24000;
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
                AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
                AudioFormat format=new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
                track=new AudioTrack(attrs,format,pcm.length,AudioTrack.MODE_STATIC,0);
                track.write(pcm,0,pcm.length);
                track.setVolume(.78f);
                track.play();
                Thread.sleep(260);
            }catch(Throwable ignored){}finally{try{if(track!=null){if(track.getPlayState()==AudioTrack.PLAYSTATE_PLAYING)track.stop();track.release();}}catch(Throwable ignored){}}
        });
    }

    private static byte[] pcm(){
        if(PCM!=null)return PCM;
        synchronized(PageTurnSound.class){
            if(PCM!=null)return PCM;
            int ms=235,n=RATE*ms/1000;byte[] out=new byte[n*2];Random r=new Random(5319);double low=0,high=0;
            for(int i=0;i<n;i++){
                double t=i/(double)n;
                double attack=Math.min(1.0,t/.08);
                double release=Math.pow(Math.max(0,1.0-t),.62);
                double envelope=attack*release;
                double white=r.nextDouble()*2.0-1.0;
                low=low*.84+white*.16;
                high=high*.35+(white-low)*.65;
                double flutter=Math.sin(2*Math.PI*(95+65*t)*i/RATE)*.10;
                double fiber=(low*.85+high*.32+flutter)*envelope;
                double v=fiber*.72;
                short s=(short)Math.max(Short.MIN_VALUE,Math.min(Short.MAX_VALUE,(int)(v*32767)));
                out[i*2]=(byte)(s&0xff);out[i*2+1]=(byte)((s>>8)&0xff);
            }
            PCM=out;return out;
        }
    }
}
