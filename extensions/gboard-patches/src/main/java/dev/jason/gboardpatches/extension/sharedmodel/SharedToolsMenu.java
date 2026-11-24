package dev.jason.gboardpatches.extension.sharedmodel;
import android.content.*;
import android.widget.*;
public final class SharedToolsMenu {
 private SharedToolsMenu(){}
 public static void open(Context context){
  LinearLayout body=SharedTextActions.body(context);PopupWindow menu=SharedTextActions.popup(body);
  SharedTextActions.label(body,GboardSharedWritingRuntime.selected(context)?"Local AI: ON for Gboard":"Local AI: OFF for Gboard");
  SharedTextActions.label(body,SharedModelClient.readinessMessage(context));
  SharedTextActions.button(body,"Local AI status / On or off",()->{menu.dismiss();SharedModelSetupActivity.open(context);});
  if(!GboardSharedWritingRuntime.selected(context) || !SharedModelClient.isReadyCached()){SharedTextActions.show(menu);return;}
  String[][] writing={{"Proofread / Auto Fix","proofread"},{"Rephrase","rephrase"},{"Professional","professional"},{"Friendly","friendly"},{"Emojify","emojify"},{"Elaborate","elaborate"},{"Shorten","shorten"}};
  for(String[] action:writing)SharedTextActions.button(body,action[0],()->{menu.dismiss();SharedTextActions.run(context,action[1],null,null,null);});
  SharedTextActions.button(body,"Cancel Voice edit",()->{menu.dismiss();SharedVoiceEditController.cancel();});
  SharedTextActions.button(body,"Voice edit",()->{menu.dismiss();SharedVoiceEditController.arm(context);});
  SharedTextActions.button(body,"Translate",()->{menu.dismiss();languages(context);});
  SharedTextActions.button(body,"Suggested reply",()->{menu.dismiss();String supplied=SharedWritingPreferences.read(context,"reply");if(supplied.isEmpty())SharedTextActions.message(context,"Enter the message to reply to in Local AI context settings first.");else SharedTextActions.run(context,"reply",null,supplied,null);});
  SharedTextActions.button(body,"My style",()->{menu.dismiss();String style=SharedWritingPreferences.read(context,"style");if(style.isEmpty())SharedTextActions.message(context,"Add your own style examples in Local AI context settings first.");else SharedTextActions.run(context,"my_style",null,style,null);});
  SharedTextActions.button(body,"Suggest changes",()->{menu.dismiss();SharedTextActions.run(context,"suggest_instructions",null,null,null);});
  SharedTextActions.button(body,"Saved instruction",()->{menu.dismiss();String instruction=SharedWritingPreferences.read(context,"instruction");if(instruction.isEmpty())SharedTextActions.message(context,"Enter an explicit instruction in Local AI context settings first.");else SharedTextActions.run(context,"edit",instruction,null,null);});
  SharedTextActions.button(body,"Local AI context settings",()->{menu.dismiss();context.startActivity(new Intent(context,SharedWritingContextActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));});
  SharedTextActions.show(menu);
 }
 private static void languages(Context context){String[][] languages={{"English","en"},{"French","fr"},{"German","de"},{"Spanish","es"},{"Italian","it"},{"Portuguese","pt"},{"Dutch","nl"}};LinearLayout body=SharedTextActions.body(context);PopupWindow menu=SharedTextActions.popup(body);
  SharedTextActions.label(body,GboardSharedWritingRuntime.selected(context)?"Local AI: ON for Gboard":"Local AI: OFF for Gboard");
  SharedTextActions.label(body,SharedModelClient.readinessMessage(context));
  SharedTextActions.button(body,"Local AI status / On or off",()->{menu.dismiss();SharedModelSetupActivity.open(context);});
  if(!GboardSharedWritingRuntime.selected(context) || !SharedModelClient.isReadyCached()){SharedTextActions.show(menu);return;}for(String[] language:languages)SharedTextActions.button(body,language[0],()->{menu.dismiss();SharedTextActions.run(context,"translate",null,null,language[1]);});SharedTextActions.show(menu);}
}
