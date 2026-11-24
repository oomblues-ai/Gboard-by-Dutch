package dev.jason.gboardpatches.extension.sharedmodel;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.*;
import android.graphics.drawable.ColorDrawable;
import android.view.*;
import android.widget.*;
import app.johninnl.modelhost.api.TextRequest;
import java.util.concurrent.CompletableFuture;
import org.json.JSONArray;

/** Custom text actions still apply through Gboard's original GenAiEditHandler and native undo. */
public final class SharedTextActions {
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 private SharedTextActions(){}
 public static void run(Context context,String operation,String instruction,String explicitContext,String language){
  try{
   String original=NativeEditBridge.prepare();EditorSnapshot snapshot=GboardSharedWritingRuntime.captureEditor();
   if(original==null){failure(context,"NATIVE_EDITOR_UNAVAILABLE");return;}
   if(snapshot==null){failure(context,"EDITOR_UNAVAILABLE");return;}
   if(original.trim().isEmpty()&&!"reply".equals(operation)){showFailure(context,"Select text or place the cursor in an editable draft.");return;}
   request(context,operation,original,instruction,explicitContext,language,snapshot);
  }catch(Exception e){failure(context,"EDITOR_UNAVAILABLE");}
 }
 static void request(Context context,String operation,String original,String instruction,String explicitContext,String language,EditorSnapshot snapshot){
  TextRequest request=new TextRequest(operation,original,instruction,explicitContext,language,null,512,30000);
  CompletableFuture<String> pending=SharedModelClient.request(context,request);
  LinearLayout progress=body(context);label(progress,"Local AI is working…");PopupWindow working=popup(progress);button(progress,"Cancel",()->{pending.cancel(false);working.dismiss();});working.setOnDismissListener(()->{if(!pending.isDone())pending.cancel(false);});show(working);
  pending.whenComplete((result,error)->MAIN.post(()->{
   working.dismiss();
   if(error instanceof java.util.concurrent.CancellationException)return;
   if(!GboardSharedWritingRuntime.isCurrent(snapshot)){message(context,"The editor changed. The result was discarded.");return;}
   if(error!=null){failure(context,SharedLocalAiState.failureCode(error));return;}
   if("suggest_instructions".equals(operation))showInstructions(context,result,snapshot);
   else preview(context,result,operation,snapshot);
  }));
 }
 private static void showInstructions(Context context,String result,EditorSnapshot snapshot){
  try{JSONArray suggestions=new JSONArray(result);if(suggestions.length()<1||suggestions.length()>3)throw new IllegalArgumentException();LinearLayout body=body(context);PopupWindow popup=popup(body);for(int i=0;i<suggestions.length();i++){String instruction=suggestions.getString(i);if(instruction.length()>160)throw new IllegalArgumentException();button(body,instruction,()->{popup.dismiss();if(GboardSharedWritingRuntime.isCurrent(snapshot))run(context,"edit",instruction,null,null);});}show(popup);}catch(Exception e){message(context,"No usable editing suggestions were returned.");}
 }
 private static void preview(Context context,String result,String operation,EditorSnapshot snapshot){
  if(result==null || result.isEmpty()){message(context,"No replacement was returned.");return;}
  LinearLayout body=body(context);TextView text=new TextView(context);text.setText(result);text.setTextSize(17);text.setTextColor(0xff111318);body.addView(text);PopupWindow popup=popup(body);
  button(body,"Apply",()->{popup.dismiss();if(!GboardSharedWritingRuntime.isCurrent(snapshot)){message(context,"The editor changed. Nothing was applied.");return;}try{if(!NativeEditBridge.apply(result,operation))message(context,"The editor is no longer available.");}catch(Exception e){message(context,"The result could not be applied.");}});
  button(body,"Cancel",popup::dismiss);show(popup);
 }
 static LinearLayout body(Context context){LinearLayout body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(24,20,24,20);body.setBackgroundColor(0xfffafafa);return body;}
 static PopupWindow popup(LinearLayout body){ScrollView scroll=new ScrollView(body.getContext());scroll.addView(body);PopupWindow p=new PopupWindow(scroll,ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,true);p.setBackgroundDrawable(new ColorDrawable(0xfffafafa));p.setOutsideTouchable(true);p.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);return p;}
 static void button(LinearLayout body,String title,Runnable action){button(body,title,action,true);}
 static void button(LinearLayout body,String title,Runnable action,boolean enabled){Button button=new Button(body.getContext());button.setText(title);button.setEnabled(enabled);button.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{0xff62676f,0xff10264a}));button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffdce6f9));button.setOnClickListener(v->action.run());body.addView(button);}
 static void label(LinearLayout body,String value){TextView label=new TextView(body.getContext());label.setText(value);label.setTextSize(16);label.setTextColor(0xff111318);label.setPadding(0,12,0,12);body.addView(label);}
 static boolean show(PopupWindow popup){InputMethodService ime=GboardSharedWritingRuntime.currentService();if(ime==null || ime.getWindow()==null || ime.getWindow().getWindow()==null)return false;View anchor=ime.getWindow().getWindow().getDecorView();popup.setHeight(Math.min(anchor.getResources().getDisplayMetrics().heightPixels/2,900));try{popup.showAtLocation(anchor,Gravity.BOTTOM,0,0);return true;}catch(WindowManager.BadTokenException unavailable){return false;}}
 static void failure(Context context,String code){SharedModelClient.noteFailure(code);showFailure(context,SharedLocalAiState.failureMessage(code));}
 private static void showFailure(Context context,String value){LinearLayout body=body(context);label(body,value);PopupWindow popup=popup(body);button(body,"Local AI status / On or off",()->{popup.dismiss();SharedModelSetupActivity.open(context);});button(body,"Close",popup::dismiss);if(!show(popup))message(context,value);}
 static void message(Context context,String text){MAIN.post(()->Toast.makeText(context,text,Toast.LENGTH_LONG).show());}
}
