package com.xiaoxiaoshuo.reader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.*;
import android.view.*;
import android.widget.*;

public class WritingEditorActivity extends Activity {
    private final int green=Color.rgb(49,88,71);
    private EditText title,editor;
    private TextView count,status;
    private String id="";
    private final Handler handler=new Handler();
    private final Runnable autosave=new Runnable(){public void run(){save(false);}};

    @Override public void onCreate(Bundle b){super.onCreate(b);id=getIntent().getStringExtra("id");if(id==null)id="";build();if(id.length()>0)load();}
    @Override protected void onPause(){save(false);super.onPause();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}

    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(12));root.setBackgroundColor(Color.rgb(250,248,243));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView back=tx("‹",34,green,true);back.setGravity(Gravity.CENTER);back.setOnClickListener(v->{save(false);finish();});top.addView(back,new LinearLayout.LayoutParams(dp(44),dp(50)));
        title=new EditText(this);title.setHint("作品标题");title.setTextSize(19);title.setTextColor(Color.rgb(44,54,49));title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setSingleLine(true);title.setBackgroundColor(Color.TRANSPARENT);top.addView(title,new LinearLayout.LayoutParams(0,dp(50),1));TextView save=button("保存");save.setOnClickListener(v->save(true));top.addView(save,new LinearLayout.LayoutParams(dp(70),dp(40)));root.addView(top);
        LinearLayout meta=new LinearLayout(this);meta.setGravity(Gravity.CENTER_VERTICAL);count=tx("0 字",12,Color.rgb(122,115,105),false);meta.addView(count,new LinearLayout.LayoutParams(0,dp(32),1));status=tx("本机自动保存",12,Color.rgb(122,115,105),false);status.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);meta.addView(status,new LinearLayout.LayoutParams(0,dp(32),1));root.addView(meta);
        editor=new EditText(this);editor.setGravity(Gravity.TOP|Gravity.LEFT);editor.setTextSize(18);editor.setTextColor(Color.rgb(48,44,38));editor.setHint("从这里开始写故事…");editor.setHintTextColor(Color.rgb(168,160,149));editor.setPadding(dp(12),dp(14),dp(12),dp(18));editor.setBackgroundColor(Color.WHITE);editor.setLineSpacing(dp(7),1f);editor.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);GradientDrawable ebg=new GradientDrawable();ebg.setColor(Color.WHITE);ebg.setCornerRadius(dp(16));ebg.setStroke(dp(1),Color.rgb(229,225,216));editor.setBackground(ebg);root.addView(editor,new LinearLayout.LayoutParams(-1,0,1));
        TextWatcher w=new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){refreshCount();scheduleSave();}public void afterTextChanged(Editable e){}};editor.addTextChangedListener(w);title.addTextChangedListener(w);
        setContentView(root);getWindow().setStatusBarColor(Color.rgb(250,248,243));getWindow().setNavigationBarColor(Color.rgb(250,248,243));getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
    private void load(){DraftStore.Draft d=DraftStore.get(this,id);if(d.id.length()==0)return;title.setText(d.title);editor.setText(d.content);refreshCount();}
    private void scheduleSave(){handler.removeCallbacks(autosave);handler.postDelayed(autosave,1600);status.setText("正在编辑…");}
    private void save(boolean toast){handler.removeCallbacks(autosave);DraftStore.Draft d=DraftStore.save(this,id,title.getText().toString(),editor.getText().toString());id=d.id;status.setText(AuthSession.loggedIn(this)&&ServerConfig.isConfigured(this)?"已保存 · 后台同步":"已保存到本机");if(toast)Toast.makeText(this,"已保存",Toast.LENGTH_SHORT).show();}
    private void refreshCount(){count.setText(DraftStore.count(editor.getText().toString())+" 字");}
    private TextView button(String s){TextView v=tx(s,14,Color.WHITE,true);v.setGravity(Gravity.CENTER);GradientDrawable d=new GradientDrawable();d.setColor(green);d.setCornerRadius(dp(13));v.setBackground(d);return v;}
    private TextView tx(String s,int z,int c,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setTypeface(Typeface.DEFAULT,b?Typeface.BOLD:Typeface.NORMAL);return v;}
    private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
