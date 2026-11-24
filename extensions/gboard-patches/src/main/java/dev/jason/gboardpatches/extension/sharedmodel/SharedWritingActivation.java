package dev.jason.gboardpatches.extension.sharedmodel;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CompletableFuture;

/** Enter the existing Gboard writing keyboard before presenting actions that need its apply/undo handler. */
public final class SharedWritingActivation {
    interface Entry { boolean available(); void open() throws Exception; }
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static CompletableFuture<Void> pending;
    private static Runnable readyCheck;
    private SharedWritingActivation() {}

    public static void ensure(Context context,Runnable showMenu) {
        InputMethodService ime=GboardSharedWritingRuntime.currentService();
        EditorSnapshot snapshot=GboardSharedWritingRuntime.captureEditor();
        if(ime==null || snapshot==null){SharedTextActions.failure(context,"EDITOR_UNAVAILABLE");return;}
        CompletableFuture<Void> result=start(snapshot,new Entry(){
            public boolean available(){return NativeEditBridge.available();}
            public void open()throws Exception{NativeEditBridge.activate(ime);}
        });
        result.whenComplete((ignored,error)->MAIN.post(()->{
            if(result.isCancelled())return;
            if(error!=null)SharedTextActions.failure(context,"NATIVE_WRITING_ACTIVATION_FAILED");
            else showMenu.run();
        }));
    }
    static CompletableFuture<Void> start(EditorSnapshot snapshot,Entry entry) {
        cancel();
        CompletableFuture<Void> result=new CompletableFuture<>();
        if(snapshot==null){result.completeExceptionally(new IllegalStateException("EDITOR_UNAVAILABLE"));return result;}
        long revision=GboardSharedWritingRuntime.currentRevision();
        if(entry.available()){result.complete(null);return result;}
        pending=result;
        readyCheck=()->{
            if(result.isDone())return;
            if(!snapshot.isCurrent(GboardSharedWritingRuntime.currentRevision()) || revision!=GboardSharedWritingRuntime.currentRevision()){result.cancel(false);return;}
            if(entry.available())result.complete(null);
        };
        Runnable timeout=()->{if(!result.isDone())result.completeExceptionally(new IllegalStateException("NATIVE_WRITING_ACTIVATION_FAILED"));};
        result.whenComplete((value,error)->{MAIN.removeCallbacks(timeout);if(pending==result){pending=null;readyCheck=null;}});
        MAIN.postDelayed(timeout,3000);
        try{entry.open();nativeReady();}catch(Exception failure){result.completeExceptionally(new IllegalStateException("NATIVE_WRITING_ACTIVATION_FAILED"));}
        return result;
    }
    /** Called only by the validated native JarvisExtension onActivate hook. */
    public static void nativeReady(){Runnable check=readyCheck;if(check!=null)MAIN.post(check);}
    public static void cancel(){CompletableFuture<Void> old=pending;pending=null;readyCheck=null;if(old!=null)old.cancel(false);}
}
