package dev.jason.gboardpatches.extension.sharedmodel;

import android.content.Context;
import android.inputmethodservice.InputMethodService;

/** Keep the original jarvis access point; its availability follows the selected provider. */
public final class SharedNativeWritingEntry {
    private SharedNativeWritingEntry() {}
    public static int state(Object provider,int original){
        Context context=GboardSharedWritingRuntime.context();
        if(provider==null || !GboardSharedWritingRuntime.selected(context))return original;
        InputMethodService ime=GboardSharedWritingRuntime.currentService();
        boolean nativePolicy=false;
        try{Class<?> owner=Class.forName("ppq",false,provider.getClass().getClassLoader());nativePolicy=true;for(String name:new String[]{"p","h","e"}){java.lang.reflect.Field field=owner.getDeclaredField(name);field.setAccessible(true);nativePolicy&=field.getBoolean(provider);}}
        catch(ReflectiveOperationException unavailable){return original;}
        return resolve(original,true,SharedModelClient.isReadyCached(),nativePolicy && ime!=null && EditorSnapshot.allowed(ime.getCurrentInputEditorInfo()));
    }
    public static boolean open(){
        Context context=GboardSharedWritingRuntime.context();if(!GboardSharedWritingRuntime.selected(context))return false;
        if(!SharedModelClient.isReadyCached()){SharedModelSetupActivity.open(context);return true;}
        EditorSnapshot snapshot=GboardSharedWritingRuntime.captureEditor();
        if(snapshot!=null && snapshot.isEmpty()){SharedScreenContext.reply(context,true,snapshot);return true;}
        return false;
    }
    static int resolve(int original,boolean selected,boolean ready,boolean editorAllowed){
        if(!selected || original==3)return original;
        return ready && editorAllowed?2:4;
    }
}
