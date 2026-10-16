package tech.timzee.companion;
import android.accessibilityservice.*;
import android.view.accessibility.*;
import android.app.KeyguardManager;
import android.os.*;
import android.graphics.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public class ControlService extends AccessibilityService {
 private ServerSocket server;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final Map<Integer,AccessibilityNodeInfo> nodes=new HashMap<>();
 private String snapshot="";
 private long observed=0;
 private int window=-1;
 private void invalidate(){snapshot="";for(AccessibilityNodeInfo n:nodes.values())n.recycle();nodes.clear();}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){invalidate();}
 @Override public void onInterrupt(){MainActivity.expires=0;invalidate();}
 @Override public void onDestroy(){MainActivity.expires=0;invalidate();try{if(server!=null)server.close();}catch(IOException ignored){}super.onDestroy();}
 @Override protected void onServiceConnected(){
  if(server!=null)return;
  new Thread(()->{try{
   server=new ServerSocket();server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),8765));
   while(!server.isClosed()){try(Socket client=server.accept()){serve(client);}catch(Exception ignored){}}
  }catch(IOException ignored){stopSelf();}},"TIMA-loopback").start();
 }
 private String line(InputStream in)throws IOException{
  ByteArrayOutputStream out=new ByteArrayOutputStream();int c;
  while((c=in.read())!=-1){if(c==10)break;if(c!=13)out.write(c);if(out.size()>4096)throw new IOException();}
  if(c==-1&&out.size()==0)throw new EOFException();return out.toString("UTF-8");
 }
 private void serve(Socket client)throws Exception{
  client.setSoTimeout(4000);InputStream in=client.getInputStream();
  if(!line(in).equals("POST /rpc HTTP/1.1"))return;
  Map<String,String> headers=new HashMap<>();int total=0;
  for(String s;(s=line(in)).length()>0;){total+=s.length();if(total>8192)return;int i=s.indexOf(':');if(i<1)return;String k=s.substring(0,i).toLowerCase(Locale.ROOT);if(headers.containsKey(k))return;headers.put(k,s.substring(i+1).trim());}
  String expected="Bearer "+MainActivity.token(this);
  if(headers.containsKey("origin")||headers.containsKey("transfer-encoding")||!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),headers.getOrDefault("authorization","").getBytes(StandardCharsets.UTF_8)))return;
  int length=Integer.parseInt(headers.getOrDefault("content-length","0"));if(length<2||length>16384)return;
  byte[] body=new byte[length];for(int p=0;p<length;){int n=in.read(body,p,length-p);if(n<0)return;p+=n;}
  JSONObject request=new JSONObject(new String(body,StandardCharsets.UTF_8));
  CompletableFuture<JSONObject> future=new CompletableFuture<>();
  long deadline=SystemClock.elapsedRealtime()+5000;
  main.post(()->{if(future.isDone()||SystemClock.elapsedRealtime()>deadline)return;try{execute(request,future);}catch(Exception e){future.complete(error(e.getMessage()));}});
  JSONObject result;
  try{result=future.get(7,TimeUnit.SECONDS);}catch(Exception e){future.cancel(false);result=error("Timed out; action outcome unknown. Observe before retrying.");}
  byte[] data=result.toString().getBytes(StandardCharsets.UTF_8);
  OutputStream out=client.getOutputStream();out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nConnection: close\r\nContent-Length: "+data.length+"\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(data);out.flush();
 }
 private JSONObject error(String message){JSONObject r=new JSONObject();try{r.put("ok",false);r.put("error",message==null?"Operation failed":message);}catch(JSONException ignored){}return r;}
 private JSONObject ok(String status)throws JSONException{return new JSONObject().put("ok",true).put("status",status);}
 private void check()throws Exception{
  if(MainActivity.expires<=SystemClock.elapsedRealtime())throw new Exception("No active session. Start one in TIMA Companion.");
  if(((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked())throw new Exception("Unlock phone manually.");
 }
 private AccessibilityNodeInfo root()throws Exception{
  AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)throw new Exception("No accessible window.");
  String pkg=String.valueOf(root.getPackageName());
  if(pkg.equals(getPackageName())||pkg.equals("com.android.settings")||pkg.contains("permissioncontroller")||pkg.equals("com.android.systemui")){root.recycle();throw new Exception("Protected system screen; handle it manually.");}
  return root;
 }
 private void walk(AccessibilityNodeInfo node,JSONArray list,int depth,int clickParent)throws Exception{
  if(nodes.size()>=200||depth>30||node.isPassword())return;
  int childClickParent=clickParent;
  if(node.isVisibleToUser()){
   int id=nodes.size()+1;nodes.put(id,AccessibilityNodeInfo.obtain(node));Rect r=new Rect();node.getBoundsInScreen(r);
   if(node.isClickable())childClickParent=id;
   JSONObject o=new JSONObject().put("view_id",node.getViewIdResourceName()).put("click_target",childClickParent).put("enabled",node.isEnabled()).put("id",id).put("text",clip(node.getText())).put("description",clip(node.getContentDescription())).put("class",String.valueOf(node.getClassName())).put("clickable",node.isClickable()).put("editable",node.isEditable()).put("scrollable",node.isScrollable()).put("bounds",new JSONArray(new int[]{r.left,r.top,r.right,r.bottom}));list.put(o);
  }
  for(int i=0;i<node.getChildCount()&&nodes.size()<200;i++){AccessibilityNodeInfo c=node.getChild(i);if(c!=null){try{walk(c,list,depth+1,childClickParent);}finally{c.recycle();}}}
 }
 private String clip(CharSequence s){if(s==null)return "";String t=s.toString();return t.substring(0,Math.min(500,t.length()));}
 private void execute(JSONObject q,CompletableFuture<JSONObject> done)throws Exception{
  check();String action=q.getString("action");
  if(action.equals("status")){done.complete(ok("session active; phone unlocked"));return;}
  AccessibilityNodeInfo root=root();
  try{
   if(action.equals("observe")){
    invalidate();JSONArray list=new JSONArray();walk(root,list,0,0);snapshot=UUID.randomUUID().toString();observed=SystemClock.elapsedRealtime();window=root.getWindowId();
    done.complete(ok("observed").put("snapshot",snapshot).put("package",String.valueOf(root.getPackageName())).put("nodes",list).put("width",getResources().getDisplayMetrics().widthPixels).put("height",getResources().getDisplayMetrics().heightPixels));return;
   }
   if(snapshot.isEmpty()||!snapshot.equals(q.optString("snapshot"))||SystemClock.elapsedRealtime()-observed>30000||root.getWindowId()!=window)throw new Exception("Stale screen; observe again.");
   boolean accepted=false;
   if(action.equals("home")||action.equals("back")){accepted=performGlobalAction(action.equals("home")?GLOBAL_ACTION_HOME:GLOBAL_ACTION_BACK);}
   else if(action.equals("gesture")){
    int w=getResources().getDisplayMetrics().widthPixels,h=getResources().getDisplayMetrics().heightPixels;
    int x=q.getInt("x1"),y=q.getInt("y1"),x2=q.getInt("x2"),y2=q.getInt("y2"),ms=q.getInt("duration");
    if(x<0||x>=w||x2<0||x2>=w||y<0||y>=h||y2<0||y2>=h||ms<50||ms>2000)throw new Exception("Gesture out of bounds.");
    Path p=new Path();p.moveTo(x,y);p.lineTo(x2,y2);invalidate();
    boolean sent=dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p,0,ms)).build(),new GestureResultCallback(){
     @Override public void onCompleted(GestureDescription g){try{done.complete(ok("gesture completed; goal unverified"));}catch(Exception e){done.complete(error("Result error"));}}
     @Override public void onCancelled(GestureDescription g){done.complete(error("Gesture cancelled; observe before retrying."));}
    },main);
    if(!sent)done.complete(error("Gesture rejected"));return;
   }else{
    AccessibilityNodeInfo node=nodes.get(q.getInt("node"));
    if(node==null||!node.refresh()||node.isPassword()||!node.isVisibleToUser()||!node.isEnabled())throw new Exception("Node unavailable; observe again.");
    if(action.equals("click")){if(!node.isClickable())throw new Exception("Node is not clickable");accepted=node.performAction(AccessibilityNodeInfo.ACTION_CLICK);}
    else if(action.equals("type")){if(!node.isEditable())throw new Exception("Node is not editable");String text=q.getString("text");if(text.length()>4000)throw new Exception("Text too long");Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);accepted=node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b);}
    else if(action.equals("scroll")){String d=q.getString("direction");if(!node.isScrollable()||!(d.equals("forward")||d.equals("backward")))throw new Exception("Invalid scroll");accepted=node.performAction(d.equals("forward")?AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);}
    else throw new Exception("Unknown action");
   }
   invalidate();if(!accepted)throw new Exception("Android rejected action");done.complete(ok("accepted; goal unverified"));
  }finally{root.recycle();}
 }
}
