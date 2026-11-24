package dev.jason.gboardpatches.extension.sharedmodel;
import android.content.Context;
/** The standard writing actions stay in Gboard's native, keyboard-themed Writing Tools UI. */
public final class SharedToolsMenu {
 private SharedToolsMenu(){}
 public static void open(Context context){
  if(!GboardSharedWritingRuntime.selected(context) || !SharedModelClient.ready(context)){SharedModelSetupActivity.open(context);return;}
  SharedWritingActivation.ensure(context,()->{});
 }
 static void extra(Context context,String operation){
  if(!GboardSharedWritingRuntime.selected(context) || !SharedModelClient.ready(context)){SharedModelSetupActivity.open(context);return;}
  if("reply".equals(operation)||"my_style".equals(operation)){SharedScreenContext.reply(context,"my_style".equals(operation));return;}
  SharedWritingActivation.ensure(context,()->SharedKeyboardSurface.whenAvailable(context,()->{
   SharedModelClient.cancelRequests();
   if("voice_edit".equals(operation)){SharedVoiceEditController.arm(context);return;}
   String supplied=null,instruction=null,language=null;
   if("reply".equals(operation))supplied=SharedWritingPreferences.read(context,"reply");
   if("my_style".equals(operation))supplied=SharedWritingPreferences.read(context,"style");
   if("edit".equals(operation))instruction=SharedWritingPreferences.read(context,"instruction");
   if(("reply".equals(operation)||"my_style".equals(operation))&&(supplied==null||supplied.isEmpty()) || "edit".equals(operation)&&(instruction==null||instruction.isEmpty())){SharedTextActions.message(context,"Add the context or instruction in Writing preferences first.");return;}
   if("translate".equals(operation))language=SharedWritingPreferences.language(context);
   SharedTextActions.run(context,operation,instruction,supplied,language);
  }));
 }
}
