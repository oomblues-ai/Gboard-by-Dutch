package dev.jason.gboardpatches.extension.sharedmodel;

import android.content.Context;
import android.os.*;
import android.widget.LinearLayout;
import app.johninnl.modelhost.api.TextRequest;
import java.util.concurrent.CompletableFuture;
import org.json.JSONArray;

/** Extra operations use native-themed body content and the original GenAiEditHandler apply/undo path. */
public final class SharedTextActions {
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 private SharedTextActions(){}
 public static void run(Context context,String operation,String instruction,String explicitContext,String language){
  try{
   String original=NativeEditBridge.prepare();EditorSnapshot snapshot=GboardSharedWritingRuntime.captureEditor();
   if(original==null){failure(context,"NATIVE_EDITOR_UNAVAILABLE");return;}
   if(snapshot==null){failure(context,"EDITOR_UNAVAILABLE");return;}
   if(original.trim().isEmpty()&&!"reply".equals(operation)){message(context,"Select text or place the cursor in an editable draft.");return;}
   request(context,operation,original,instruction,explicitContext,language,snapshot);
  }catch(Exception error){failure(context,SharedLocalAiState.failureCode(error));}
 }
 static void request(Context context,String operation,String original,String instruction,String explicitContext,String language,EditorSnapshot snapshot){
  TextRequest request=new TextRequest(operation,original,instruction,explicitContext,language,null,512,30000);
  LinearLayout progress=SharedKeyboardSurface.body();SharedKeyboardSurface.label(progress,"Working on your text…");
  SharedKeyboardSurface.Panel working=new SharedKeyboardSurface.Panel(progress);
  java.util.concurrent.atomic.AtomicReference<CompletableFuture<String>> active=new java.util.concurrent.atomic.AtomicReference<>();
  SharedKeyboardSurface.button(progress,"Cancel",working::dismiss);
  working.setOnDismissListener(()->{CompletableFuture<String> task=active.get();if(task!=null&&!task.isDone())task.cancel(false);});
  if(!working.show()){failure(context,"NATIVE_THEME_UNAVAILABLE");return;}
  final CompletableFuture<String> pending;
  try{pending=SharedModelClient.request(context,request);active.set(pending);}
  catch(RuntimeException unavailable){working.dismiss();throw unavailable;}
  pending.whenComplete((result,error)->MAIN.post(()->{
   working.dismiss();if(pending.isCancelled())return;
   if(!GboardSharedWritingRuntime.isCurrent(snapshot)){message(context,"The editor changed. Nothing was applied.");return;}
   if(error!=null){failure(context,SharedLocalAiState.failureCode(error));return;}
   try{if("suggest_instructions".equals(operation))showInstructions(context,result,snapshot);else preview(context,result,operation,snapshot);}
   catch(Exception unavailable){failure(context,"NATIVE_THEME_UNAVAILABLE");}
  }));
 }
 private static void showInstructions(Context context,String result,EditorSnapshot snapshot){
  try{
   JSONArray suggestions=new JSONArray(result);if(suggestions.length()<1||suggestions.length()>3)throw new IllegalArgumentException();
   LinearLayout body=SharedKeyboardSurface.body();SharedKeyboardSurface.label(body,"Suggested changes");SharedKeyboardSurface.Panel panel=new SharedKeyboardSurface.Panel(body);
   for(int i=0;i<suggestions.length();i++){String instruction=suggestions.getString(i);if(instruction.length()>160)throw new IllegalArgumentException();SharedKeyboardSurface.button(body,instruction,()->{panel.dismiss();if(GboardSharedWritingRuntime.isCurrent(snapshot))run(context,"edit",instruction,null,null);});}
   SharedKeyboardSurface.button(body,"Back",panel::dismiss);panel.show();
  }catch(Exception unusable){message(context,"No usable editing suggestions were returned.");}
 }
 private static void preview(Context context,String result,String operation,EditorSnapshot snapshot){
  if(result==null || result.isEmpty()){message(context,"No replacement was returned.");return;}
  LinearLayout body=SharedKeyboardSurface.body();SharedKeyboardSurface.label(body,result);SharedKeyboardSurface.Panel panel=new SharedKeyboardSurface.Panel(body);
  SharedKeyboardSurface.button(body,"Apply",()->{panel.dismiss();if(!GboardSharedWritingRuntime.isCurrent(snapshot)){message(context,"The editor changed. Nothing was applied.");return;}try{if(!NativeEditBridge.apply(result,operation))failure(context,"EDITOR_UNAVAILABLE");}catch(Exception unavailable){failure(context,"EDITOR_UNAVAILABLE");}});
  SharedKeyboardSurface.button(body,"Cancel",panel::dismiss);panel.show();
 }
 static void failure(Context context,String code){SharedModelClient.noteFailure(code);message(context,SharedLocalAiState.failureMessage(code));}
 static void message(Context context,String text){MAIN.post(()->{
  try{
   if(SharedKeyboardSurface.available()){
    LinearLayout body=SharedKeyboardSurface.body();SharedKeyboardSurface.label(body,text);SharedKeyboardSurface.Panel panel=new SharedKeyboardSurface.Panel(body);
    SharedKeyboardSurface.button(body,"Back",panel::dismiss);SharedKeyboardSurface.button(body,"Local AI settings",()->{panel.dismiss();SharedModelSetupActivity.open(context);});
    if(panel.show())return;
   }
  }catch(Exception unavailable){SharedModelClient.noteFailure("NATIVE_THEME_UNAVAILABLE");}
  android.widget.Toast.makeText(context,text,android.widget.Toast.LENGTH_LONG).show();
 });}
}
