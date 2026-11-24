package dev.jason.gboardpatches.extension.sharedmodel;

import android.content.Context;
import android.os.*;
import java.lang.reflect.Method;

/** One explicitly armed command. Recognition stays Parakeet; only transcript-to-edit routing changes. */
public final class SharedVoiceEditController {
 private static final Handler MAIN=new Handler(Looper.getMainLooper());
 private static volatile Armed armed;
 private static final class Armed {final Context context;final String original;final EditorSnapshot snapshot;final long expires;boolean started;Armed(Context c,String t,EditorSnapshot s){context=c.getApplicationContext();original=t;snapshot=s;expires=SystemClock.uptimeMillis()+60000;}}
 private SharedVoiceEditController(){}
 public static void arm(Context context){
  try{
   if(!GboardSharedWritingRuntime.selected(context)||!SharedModelClient.ready(context)){SharedTextActions.message(context,"Choose Shared local model and wait until the provider is ready.");return;}
   Class<?> recognizer=Class.forName("nl.johninnl.gboard.ParakeetRecognizer",false,SharedVoiceEditController.class.getClassLoader());
   if(!Boolean.TRUE.equals(recognizer.getMethod("availableForVoiceEdit",Context.class).invoke(null,context))){SharedTextActions.message(context,"Enable and set up Parakeet before arming Voice edit.");return;}
   String original=NativeEditBridge.prepare();EditorSnapshot snapshot=GboardSharedWritingRuntime.captureEditor();
   if(original==null||original.trim().isEmpty()||snapshot==null){SharedTextActions.message(context,"Select the text to edit first.");return;}
   Armed next=new Armed(context,original,snapshot);armed=next;
   MAIN.postDelayed(()->{if(armed==next)armed=null;},60000);
   android.inputmethodservice.InputMethodService ime=GboardSharedWritingRuntime.currentService();if(ime!=null)NativeEditBridge.leaveWriting(ime);
   android.widget.Toast.makeText(context,"Voice edit ready. Tap the microphone and speak your edit.",android.widget.Toast.LENGTH_LONG).show();
  }catch(Exception e){SharedTextActions.message(context,"Parakeet Voice edit is unavailable in this installation.");}
 }
 public static synchronized boolean beginParakeetSession(Context context){Armed state=armed;if(state==null||state.started||SystemClock.uptimeMillis()>state.expires ){armed=null;return false;}state.started=true;return true;}
 public static void onParakeetCommand(Context context,String transcript){
  Armed state=armed;armed=null;
  MAIN.post(()->{if(state==null||!state.started||SystemClock.uptimeMillis()>state.expires||!GboardSharedWritingRuntime.isCurrent(state.snapshot)){SharedTextActions.message(context,"Voice edit was cancelled because the editor changed.");return;}
   if(transcript==null||transcript.trim().isEmpty()){SharedTextActions.message(context,"No edit command was recognized.");return;}
   String command=transcript.trim();String operation=VoiceEditPolicy.operation(true,command);
   SharedWritingActivation.ensure(state.context,()->SharedKeyboardSurface.whenAvailable(state.context,()->{
    if(!GboardSharedWritingRuntime.isCurrent(state.snapshot)){SharedTextActions.message(context,"The draft changed. Nothing was applied.");return;}
    try{String current=NativeEditBridge.prepare();EditorSnapshot snapshot=GboardSharedWritingRuntime.captureEditor();if(!state.original.equals(current)||snapshot==null){SharedTextActions.message(context,"The draft changed. Nothing was applied.");return;}SharedTextActions.request(state.context,operation,state.original,"edit".equals(operation)?command:null,null,null,snapshot);}
    catch(Exception unavailable){SharedTextActions.failure(context,"EDITOR_UNAVAILABLE");}
   }));
  });
 }
 public static void cancel(){armed=null;}
}
