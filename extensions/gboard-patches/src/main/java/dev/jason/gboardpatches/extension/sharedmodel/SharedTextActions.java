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
   if(original==null || snapshot==null || (original.trim().isEmpty()&&!"reply".equals(operation))){message(context,"Select text or place the cursor in an editable draft.");return;}
   request(context,operation,original,instruction,explicitContext,language,snapshot);
  }catch(Exception e){message(context,"Writing tools are unavailable in this editor.");}
 }
 static void request(Context context,String operation,String original,String instruction,String explicitContext,String language,EditorSnapshot snapshot){
  TextRequest request=new TextRequest(operation,original,instruction,explicitContext,language,null,512,30000);
  CompletableFuture<String> pending=SharedModelClient.request(context,request);
  pending.whenComplete((result,error)->MAIN.post(()->{
   if(error instanceof java.util.concurrent.CancellationException)return;
   if(!GboardSharedWritingRuntime.isCurrent(snapshot)){message(context,"The editor changed. The result was discarded.");return;}
   if(error!=null){message(context,"Local suggestions are unavailable. Check provider readiness.");return;}
   if("suggest_instructions".equals(operation))showInstructions(context,result,snapshot);
   else preview(context,result,operation,snapshot);
  }));
 }
 private static void showInstructions(Context context,String result,EditorSnapshot snapshot){
  try{JSONArray suggestions=new JSONArray(result);if(suggestions.length()<1||suggestions.length()>3)throw new IllegalArgumentException();LinearLayout body=body(context);PopupWindow popup=popup(body);for(int i=0;i<suggestions.length();i++){String instruction=suggestions.getString(i);if(instruction.length()>160)throw new IllegalArgumentException();button(body,instruction,()->{popup.dismiss();if(GboardSharedWritingRuntime.isCurrent(snapshot))run(context,"edit",instruction,null,null);});}show(popup);}catch(Exception e){message(context,"No usable editing suggestions were returned.");}
 }
 private static void preview(Context context,String result,String operation,EditorSnapshot snapshot){
  if(result==null || result.isEmpty()){message(context,"No replacement was returned.");return;}
  LinearLayout body=body(context);TextView text=new TextView(context);text.setText(result);text.setTextSize(17);body.addView(text);PopupWindow popup=popup(body);
  button(body,"Apply",()->{popup.dismiss();if(!GboardSharedWritingRuntime.isCurrent(snapshot)){message(context,"The editor changed. Nothing was applied.");return;}try{if(!NativeEditBridge.apply(result,operation))message(context,"The editor is no longer available.");}catch(Exception e){message(context,"The result could not be applied.");}});
  button(body,"Cancel",popup::dismiss);show(popup);
 }
 static LinearLayout body(Context context){LinearLayout body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(24,20,24,20);body.setBackgroundColor(0xfffafafa);return body;}
 static PopupWindow popup(LinearLayout body){ScrollView scroll=new ScrollView(body.getContext());scroll.addView(body);PopupWindow p=new PopupWindow(scroll,ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT,true);p.setBackgroundDrawable(new ColorDrawable(0xfffafafa));p.setOutsideTouchable(true);p.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);return p;}
 static void button(LinearLayout body,String title,Runnable action){Button button=new Button(body.getContext());button.setText(title);button.setOnClickListener(v->action.run());body.addView(button);}
 static void show(PopupWindow popup){InputMethodService ime=GboardSharedWritingRuntime.currentService();if(ime==null || ime.getWindow()==null || ime.getWindow().getWindow()==null)return;View anchor=ime.getWindow().getWindow().getDecorView();popup.setHeight(Math.min(anchor.getResources().getDisplayMetrics().heightPixels/2,900));popup.showAtLocation(anchor,Gravity.BOTTOM,0,0);}
 static void message(Context context,String text){MAIN.post(()->Toast.makeText(context,text,Toast.LENGTH_LONG).show());}
}
