package tech.timzee.companion;
import android.app.Activity;
import android.os.*;
import android.content.*;
import android.provider.Settings;
import android.widget.*;
import java.security.SecureRandom;

public class MainActivity extends Activity {
 public static volatile long expires = 0;
 static String token(Context c) {
  android.content.SharedPreferences p=c.getSharedPreferences("bridge",MODE_PRIVATE);
  String t=p.getString("token",null);
  if(t==null){byte[] b=new byte[32];new SecureRandom().nextBytes(b);t=android.util.Base64.encodeToString(b,android.util.Base64.NO_WRAP);p.edit().putString("token",t).apply();}
  return t;
 }
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
  LinearLayout l=new LinearLayout(this);l.setOrientation(1);l.setPadding(32,48,32,24);setContentView(l);
  TextView info=new TextView(this);info.setText("TIMA phone control preview\n\nThis reads visible app text and can tap, type and swipe. Screen text is sent to your configured AI provider when you use /control. Password nodes are excluded.\n\nEnable Accessibility, copy the token into TIMA_BRIDGE_TOKEN in Termux, then start a session. Only use on your own phone. Stop here at any time.\n");l.addView(info);
  Button settings=new Button(this);settings.setText("Enable Accessibility service");settings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));l.addView(settings);
  Button copy=new Button(this);copy.setText("Copy pairing token");copy.setOnClickListener(v->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("TIMA token",token(this)));Toast.makeText(this,"Token copied",Toast.LENGTH_SHORT).show();});l.addView(copy);
  Button start=new Button(this);start.setText("Start 15-minute session");start.setOnClickListener(v->{expires=SystemClock.elapsedRealtime()+900000;Toast.makeText(this,"Session active for 15 minutes",Toast.LENGTH_LONG).show();});l.addView(start);
  Button stop=new Button(this);stop.setText("STOP control session");stop.setOnClickListener(v->{expires=0;Toast.makeText(this,"Stopped",Toast.LENGTH_SHORT).show();});l.addView(stop);
  TextView status=new TextView(this);l.addView(status);
  Handler h=new Handler(getMainLooper());h.post(new Runnable(){public void run(){if(isDestroyed())return;status.setText(expires>SystemClock.elapsedRealtime()?"Session active: "+((expires-SystemClock.elapsedRealtime())/1000)+" seconds remaining":"Session stopped");h.postDelayed(this,1000);}});
 }
}
