package com.xiaoxiaoshuo.reader;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.content.Context;

final class LiteraryCoverDrawable extends Drawable {
    private final String title;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    LiteraryCoverDrawable(String t){title=t==null?"小小说":t;}
    @Override public void draw(Canvas c){
        Rect b=getBounds(); float w=b.width(),h=b.height();
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(245,240,228));c.drawRect(b,p);
        p.setColor(Color.rgb(255,253,248));c.drawRoundRect(b.left+w*.09f,b.top+h*.07f,b.right-w*.09f,b.bottom-h*.07f,w*.08f,w*.08f,p);
        p.setColor(Color.rgb(232,196,119));c.drawCircle(b.left+w*.70f,b.top+h*.24f,w*.105f,p);
        p.setColor(Color.rgb(255,253,248));c.drawCircle(b.left+w*.74f,b.top+h*.21f,w*.10f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,w*.012f));p.setColor(Color.rgb(58,92,72));
        Path branch=new Path();branch.moveTo(b.left+w*.14f,b.top+h*.40f);branch.cubicTo(b.left+w*.28f,b.top+h*.33f,b.left+w*.30f,b.top+h*.24f,b.left+w*.43f,b.top+h*.20f);c.drawPath(branch,p);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(86,124,96));
        for(int i=0;i<5;i++){float x=b.left+w*(.20f+i*.055f),y=b.top+h*(.34f-i*.028f);c.save();c.rotate(-25+i*8,x,y);c.drawOval(x-w*.025f,y-h*.010f,x+w*.025f,y+h*.010f,p);c.restore();}
        p.setColor(Color.rgb(64,94,78));Path book=new Path();book.moveTo(b.left+w*.20f,b.top+h*.69f);book.quadTo(b.left+w*.38f,b.top+h*.63f,b.left+w*.50f,b.top+h*.72f);book.quadTo(b.left+w*.62f,b.top+h*.63f,b.left+w*.80f,b.top+h*.69f);book.lineTo(b.left+w*.76f,b.top+h*.82f);book.quadTo(b.left+w*.61f,b.top+h*.78f,b.left+w*.50f,b.top+h*.84f);book.quadTo(b.left+w*.39f,b.top+h*.78f,b.left+w*.24f,b.top+h*.82f);book.close();c.drawPath(book,p);
        p.setColor(Color.WHITE);p.setStrokeWidth(Math.max(1,w*.006f));p.setStyle(Paint.Style.STROKE);c.drawLine(b.left+w*.50f,b.top+h*.72f,b.left+w*.50f,b.top+h*.83f,p);p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(42,57,49));p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("serif",Typeface.BOLD));p.setTextSize(Math.max(18,w*.105f));
        String a=title; if(a.length()>8)a=a.substring(0,8); c.drawText(a,b.left+w*.50f,b.top+h*.53f,p);
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(Math.max(10,w*.050f));p.setColor(Color.rgb(135,123,102));c.drawText("静心 · 阅读",b.left+w*.50f,b.top+h*.59f,p);
    }
    @Override public void setAlpha(int a){p.setAlpha(a);} @Override public void setColorFilter(android.graphics.ColorFilter f){p.setColorFilter(f);} @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}

final class LiteraryIllustrationView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    LiteraryIllustrationView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();
        p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(250,247,239));c.drawRoundRect(w*.08f,h*.08f,w*.94f,h*.92f,w*.08f,w*.08f,p);
        p.setColor(Color.rgb(238,205,133));c.drawCircle(w*.72f,h*.26f,w*.10f,p);
        p.setColor(Color.rgb(58,96,75));p.setStrokeWidth(Math.max(3,w*.018f));p.setStyle(Paint.Style.STROKE);Path stem=new Path();stem.moveTo(w*.22f,h*.78f);stem.cubicTo(w*.28f,h*.57f,w*.36f,h*.48f,w*.44f,h*.33f);c.drawPath(stem,p);p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(84,126,95));for(int i=0;i<5;i++){float x=w*(.26f+i*.035f),y=h*(.66f-i*.075f);c.save();c.rotate(i%2==0?-30:25,x,y);c.drawOval(x-w*.055f,y-h*.022f,x+w*.055f,y+h*.022f,p);c.restore();}
        p.setColor(Color.rgb(189,151,103));c.drawRoundRect(w*.14f,h*.74f,w*.88f,h*.79f,w*.02f,w*.02f,p);
        p.setColor(Color.rgb(247,241,227));c.drawRoundRect(w*.64f,h*.58f,w*.78f,h*.73f,w*.03f,w*.03f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Math.max(2,w*.015f));c.drawCircle(w*.79f,h*.65f,w*.05f,p);p.setStyle(Paint.Style.FILL);
        p.setColor(Color.rgb(53,88,70));Path book=new Path();book.moveTo(w*.30f,h*.61f);book.quadTo(w*.43f,h*.55f,w*.51f,h*.63f);book.quadTo(w*.60f,h*.55f,w*.72f,h*.61f);book.lineTo(w*.69f,h*.73f);book.quadTo(w*.59f,h*.69f,w*.51f,h*.75f);book.quadTo(w*.43f,h*.69f,w*.33f,h*.73f);book.close();c.drawPath(book,p);
        p.setColor(Color.rgb(226,184,93));for(int i=0;i<3;i++){float x=w*(.52f+i*.10f),y=h*(.22f+i*.08f);c.drawCircle(x,y,w*.012f,p);}
    }
}
