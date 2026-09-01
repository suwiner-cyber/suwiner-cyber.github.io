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
    private Runnable safety;

    public boolean isRunning(){return running;}
    public void cancel(View page){
        if(current!=null){try{current.cancel();}catch(Throwable ignored){}}
        handler.removeCallbacksAndMessages(null);running=false;reset(page);
    }
    public void turn(View page,int direction,Runnable swap,Runnable finished){
        if(page==null||running)return;cancel(page);running=true;
        final float w=Math.max(1f,page.getWidth());final float out=direction>0?-w*.18f:w*.18f;final float in=direction>0?w*.12f:-w*.12f;
        page.setPivotX(w*.5f);page.setPivotY(page.getHeight()*.55f);
        ObjectAnimator outX=ObjectAnimator.ofFloat(page,View.TRANSLATION_X,0f,out);
        ObjectAnimator outA=ObjectAnimator.ofFloat(page,View.ALPHA,1f,.34f);
        ObjectAnimator outS=ObjectAnimator.ofFloat(page,View.SCALE_X,1f,.985f);
        AnimatorSet first=new AnimatorSet();first.playTogether(outX,outA,outS);first.setDuration(135);first.setInterpolator(new PathInterpolator(.35f,0f,.65f,1f));
        first.addListener(new AnimatorListenerAdapter(){private boolean done=false;private void go(){if(done)return;done=true;try{swap.run();}catch(Throwable ignored){}page.setTranslationX(in);page.setAlpha(.52f);page.setScaleX(.99f);ObjectAnimator ix=ObjectAnimator.ofFloat(page,View.TRANSLATION_X,in,0f);ObjectAnimator ia=ObjectAnimator.ofFloat(page,View.ALPHA,.52f,1f);ObjectAnimator is=ObjectAnimator.ofFloat(page,View.SCALE_X,.99f,1f);AnimatorSet second=new AnimatorSet();second.playTogether(ix,ia,is);second.setDuration(185);second.setInterpolator(new DecelerateInterpolator(1.9f));current=second;second.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){finish(page,finished);}@Override public void onAnimationCancel(Animator a){finish(page,finished);}});second.start();}
            @Override public void onAnimationEnd(Animator a){go();}
            @Override public void onAnimationCancel(Animator a){go();}
        });
        current=first;first.start();
        safety=()->finish(page,finished);handler.postDelayed(safety,750);
    }
    private void finish(View page,Runnable finished){handler.removeCallbacksAndMessages(null);running=false;reset(page);try{if(finished!=null)finished.run();}catch(Throwable ignored){}}
    private void reset(View page){if(page==null)return;page.animate().cancel();page.setTranslationX(0);page.setTranslationY(0);page.setScaleX(1);page.setScaleY(1);page.setAlpha(1);page.setRotation(0);page.setRotationX(0);page.setRotationY(0);}
}
