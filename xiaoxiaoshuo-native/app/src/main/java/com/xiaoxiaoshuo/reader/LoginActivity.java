package com.xiaoxiaoshuo.reader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;

public class LoginActivity extends Activity {
    private final int green=Color.rgb(49,88,71);
    private EditText server,user,pass,confirm;
    private CheckBox remember;
    private TextView title,status,modeSwitch;
    private boolean registerMode=false;

    @Override public void onCreate(Bundle b){super.onCreate(b);build();prefill();}
    private void build(){
        ScrollView sv=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(24),dp(28),dp(24),dp(30));root.setBackgroundColor(Color.rgb(247,244,238));
        TextView back=tx("‹",34,green,true);back.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);back.setOnClickListener(v->finish());root.addView(back,new LinearLayout.LayoutParams(-1,dp(48)));
        title=tx("登录小小说",28,Color.rgb(42,54,48),true);root.addView(title);TextView sub=tx("同步码字草稿、阅读进度与私人离线缓存",13,Color.rgb(123,116,106),false);sub.setPadding(0,dp(6),0,dp(20));root.addView(sub);
        server=input("服务器地址，例如 https://example.com/xiaoxiaoshuo",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);root.addView(server,field());
        user=input("用户名",InputType.TYPE_CLASS_TEXT);root.addView(user,field());
        pass=input("密码",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);root.addView(pass,field());
        confirm=input("确认密码",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);confirm.setVisibility(View.GONE);root.addView(confirm,field());
        remember=new CheckBox(this);remember.setText("记住用户名和密码（加密保存）");remember.setTextSize(14);remember.setTextColor(Color.rgb(70,76,72));remember.setChecked(true);root.addView(remember,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView submit=button("登录");submit.setOnClickListener(v->submit(submit));root.addView(submit,new LinearLayout.LayoutParams(-1,dp(52)));
        modeSwitch=tx("没有账号？注册",14,green,true);modeSwitch.setGravity(Gravity.CENTER);modeSwitch.setPadding(0,dp(18),0,dp(10));modeSwitch.setOnClickListener(v->{registerMode=!registerMode;title.setText(registerMode?"注册小小说":"登录小小说");confirm.setVisibility(registerMode?View.VISIBLE:View.GONE);remember.setVisibility(registerMode?View.GONE:View.VISIBLE);submit.setText(registerMode?"注册账号":"登录");modeSwitch.setText(registerMode?"已有账号？登录":"没有账号？注册");status.setText(registerMode?"注册密码必须同时包含英文字母和数字，至少8位。":"");});root.addView(modeSwitch);
        status=tx("",13,Color.rgb(139,78,68),false);status.setGravity(Gravity.CENTER);status.setPadding(0,dp(8),0,0);root.addView(status);
        sv.addView(root);setContentView(sv);getWindow().setStatusBarColor(Color.rgb(247,244,238));getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }
    private void prefill(){server.setText(ServerConfig.get(this));String u=SecureCredentialStore.username(this);user.setText(u);remember.setChecked(SecureCredentialStore.remember(this));if(remember.isChecked())pass.setText(SecureCredentialStore.password(this));}
    private void submit(TextView submit){
        String base=server.getText().toString().trim(),u=user.getText().toString().trim(),p=pass.getText().toString();
        if(!(base.startsWith("http://")||base.startsWith("https://"))){status.setText("请填写正确的服务器地址");return;}if(u.length()<3){status.setText("用户名至少3个字符");return;}if(p.length()<8){status.setText("密码至少8位");return;}
        if(registerMode){if(!p.matches(".*[A-Za-z].*")||!p.matches(".*[0-9].*")){status.setText("密码必须同时包含英文字母和数字");return;}if(!p.equals(confirm.getText().toString())){status.setText("两次密码输入不一致");return;}}
        ServerConfig.set(this,base);submit.setEnabled(false);submit.setAlpha(.55f);status.setText(registerMode?"正在注册…":"正在登录…");
        new Thread(()->{try{JSONObject req=new JSONObject().put("username",u).put("password",p);JSONObject res=ApiClient.post(this,registerMode?"register":"login",req);boolean ok=res.optBoolean("ok",false);String msg=res.optString("message",ok?"成功":"失败"),token=res.optString("token","");runOnUiThread(()->{submit.setEnabled(true);submit.setAlpha(1f);if(ok){if(registerMode){status.setText("注册成功，请直接登录");registerMode=false;title.setText("登录小小说");confirm.setVisibility(View.GONE);remember.setVisibility(View.VISIBLE);submit.setText("登录");modeSwitch.setText("没有账号？注册");}else{AuthSession.set(this,u,token);SecureCredentialStore.save(this,u,p,remember.isChecked());Toast.makeText(this,"登录成功",Toast.LENGTH_SHORT).show();finish();}}else status.setText(msg);});}catch(Throwable e){runOnUiThread(()->{submit.setEnabled(true);submit.setAlpha(1f);status.setText("连接服务器失败："+e.getMessage());});}}).start();
    }
    private EditText input(String hint,int type){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(15);e.setSingleLine(true);e.setInputType(type);e.setPadding(dp(15),0,dp(15),0);GradientDrawable d=new GradientDrawable();d.setColor(Color.WHITE);d.setCornerRadius(dp(16));d.setStroke(dp(1),Color.rgb(218,227,221));e.setBackground(d);return e;}
    private LinearLayout.LayoutParams field(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52));p.bottomMargin=dp(10);return p;}
    private TextView button(String s){TextView v=tx(s,16,Color.WHITE,true);v.setGravity(Gravity.CENTER);GradientDrawable d=new GradientDrawable();d.setColor(green);d.setCornerRadius(dp(16));v.setBackground(d);return v;}
    private TextView tx(String s,int z,int c,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);v.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);return v;}
    private int dp(float n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
