package com.xiaoxiaoshuo.reader;

import android.app.*;
import android.os.*;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class WritingActivity extends Activity {
    private final int green=Color.rgb(49,88,71);
    private LinearLayout listBox;
    @Override public void onCreate(Bundle b){super.onCreate(b);showList();}
    @Override protected void onResume(){super.onResume();if(listBox!=null)renderList();}
    private void showList(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(18),dp(20),dp(20));root.setBackgroundColor(Color.rgb(247,244,238));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView back=tx("‹",34,green,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->finish());top.addView(back,new LinearLayout.LayoutParams(dp(44),dp(50)));TextView h=tx("码字",28,Color.rgb(42,54,48),true);top.addView(h,new LinearLayout.LayoutParams(0,dp(50),1));TextView create=button("＋ 新建");create.setOnClickListener(v->openEditor(""));top.addView(create,new LinearLayout.LayoutParams(dp(86),dp(42)));root.addView(top);
        TextView sub=tx("本机自动保存；登录后可同步到你的私人服务器。",13,Color.rgb(123,116,106),false);sub.setPadding(dp(2),0,0,dp(14));root.addView(sub);
        ScrollView sv=new ScrollView(this);listBox=new LinearLayout(this);listBox.setOrientation(LinearLayout.VERTICAL);sv.addView(listBox);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);renderList();getWindow().setStatusBarColor(Color.rgb(247,244,238));getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
    private void renderList(){listBox.removeAllViews();List<DraftStore.Draft> ds=DraftStore.list(this);if(ds.isEmpty()){TextView e=tx("还没有作品。点击右上角“新建”开始码字。",15,Color.rgb(108,102,94),false);e.setPadding(dp(8),dp(30),dp(8),dp(30));listBox.addView(e);return;}for(DraftStore.Draft d:ds)listBox.addView(card(d),margin());}
    private View card(DraftStore.Draft d){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(12));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(18));c.setBackground(bg);TextView t=tx(d.title,18,Color.rgb(44,54,49),true);c.addView(t);String time=new SimpleDateFormat("MM-dd HH:mm",Locale.getDefault()).format(new Date(d.updatedAt));TextView meta=tx(d.words+" 字 · "+time,12,Color.rgb(128,120,109),false);meta.setPadding(0,dp(5),0,dp(8));c.addView(meta);String preview=d.content.replace('\n',' ').trim();if(preview.length()>80)preview=preview.substring(0,80)+"…";TextView p=tx(preview.length()==0?"空白草稿":preview,13,Color.rgb(84,82,76),false);p.setMaxLines(2);c.addView(p);LinearLayout actions=new LinearLayout(this);actions.setPadding(0,dp(10),0,0);TextView edit=small("继续写",false);edit.setOnClickListener(v->openEditor(d.id));TextView del=small("删除",true);del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("删除《"+d.title+"》？").setMessage("本机草稿会删除，登录后也会同步删除服务器上的私人草稿。此操作不可恢复。") .setNegativeButton("取消",null).setPositiveButton("删除",(x,w)->{DraftStore.delete(this,d.id);renderList();}).show());actions.addView(edit,new LinearLayout.LayoutParams(0,dp(42),1));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(42),1);lp.leftMargin=dp(8);actions.addView(del,lp);c.addView(actions);return c;}
    private void openEditor(String id){Intent x=new Intent(this,WritingEditorActivity.class);x.putExtra("id",id);startActivity(x);}
    private TextView button(String s){TextView v=tx(s,14,Color.WHITE,true);v.setGravity(Gravity.CENTER);GradientDrawable d=new GradientDrawable();d.setColor(green);d.setCornerRadius(dp(14));v.setBackground(d);return v;}
    private TextView small(String s,boolean danger){TextView v=tx(s,13,danger?Color.rgb(162,77,70):green,true);v.setGravity(Gravity.CENTER);GradientDrawable d=new GradientDrawable();d.setColor(danger?Color.rgb(252,241,239):Color.rgb(238,246,240));d.setCornerRadius(dp(13));v.setBackground(d);return v;}
    private TextView tx(String s,int z,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setTypeface(Typeface.DEFAULT,b?Typeface.BOLD:Typeface.NORMAL);return v;}
    private LinearLayout.LayoutParams margin(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);return p;}
    private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
