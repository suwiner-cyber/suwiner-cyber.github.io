package com.xiaoxiaoshuo.reader;

import android.animation.*;
import android.os.Handler;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;

public final class StablePageAnimator {
    private AnimatorSet current;
    private final Handler handler=new Handler();
    private boolean running=false;
    private boolean aborting=false;

    public boolean isRunning(){return running;}
    public void cancel(View page){
        aborting=true;
        if(current!=null){try{current.removeAllListeners();current.cancel();}catch(Throwable ignored){}}
        current=null;handler.removeCallbacksAndMessages(null);running=false;reset(page);aborting=false;
    }
    public void turn(View page,int direction,Runnable swap,Runnable finished){
        if(page==null||running)return;cancel(page);running=true;aborting=false;
        final float w=Math.max(1f,page.getWidth());final float out=direction>0?-w*.16f:w*.16f;final float in=direction>0?w*.10f:-w*.10f;
        page.setPivotX(w*.5f);page.setPivotY(page.getHeight()*.56f);
        ObjectAnimator outX=ObjectAnimator.ofFloat(page,View.TRANSLATION_X,0f,out);
        ObjectAnimator outA=ObjectAnimator.ofFloat(page,View.ALPHA,1f,.38f);
        ObjectAnimator outS=ObjectAnimator.ofFloat(page,View.SCALE_X,1f,.988f);
        ObjectAnimator outY=ObjectAnimator.ofFloat(page,View.TRANSLATION_Y,0f,-Math.max(1f,page.getHeight())*.008f);
        AnimatorSet first=new AnimatorSet();first.playTogether(outX,outA,outS,outY);first.setDuration(135);first.setInterpolator(new PathInterpolator(.35f,0f,.65f,1f));
        first.addListener(new AnimatorListenerAdapter(){private boolean moved=false;private void proceed(){if(moved||aborting)return;moved=true;try{swap.run();}catch(Throwable ignored){}page.setTranslationX(in);page.setTranslationY(Math.max(1f,page.getHeight())*.006f);page.setAlpha(.55f);page.setScaleX(.992f);ObjectAnimator ix=ObjectAnimator.ofFloat(page,View.TRANSLATION_X,in,0f);ObjectAnimator iy=ObjectAnimator.ofFloat(page,View.TRANSLATION_Y,page.getTranslationY(),0f);ObjectAnimator ia=ObjectAnimator.ofFloat(page,View.ALPHA,.55f,1f);ObjectAnimator is=ObjectAnimator.ofFloat(page,View.SCALE_X,.992f,1f);AnimatorSet second=new AnimatorSet();second.playTogether(ix,iy,ia,is);second.setDuration(180);second.setInterpolator(new DecelerateInterpolator(1.9f));current=second;second.addListener(new AnimatorListenerAdapter(){private boolean ended=false;private void done(){if(ended)return;ended=true;finish(page,finished);}@Override public void onAnimationEnd(Animator a){done();}@Override public void onAnimationCancel(Animator a){done();}});second.start();}
            @Override public void onAnimationEnd(Animator a){proceed();}
        });
        current=first;first.start();handler.postDelayed(()->{if(running)finish(page,finished);},700);
    }
    private void finish(View page,Runnable finished){if(aborting)return;handler.removeCallbacksAndMessages(null);running=false;current=null;reset(page);try{if(finished!=null)finished.run();}catch(Throwable ignored){}}
    private void reset(View page){if(page==null)return;page.animate().cancel();page.setTranslationX(0);page.setTranslationY(0);page.setScaleX(1);page.setScaleY(1);page.setAlpha(1);page.setRotation(0);page.setRotationX(0);page.setRotationY(0);}
}
