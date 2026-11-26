package nl.johninnl.gboard;
import android.content.Context;
import java.lang.reflect.Method;
/** Optional downstream integration. A standalone Parakeet installation has no dependency on AI Writing Tools. */
final class VoiceEditHook {
 private VoiceEditHook(){}
 private static Class<?> controller()throws ClassNotFoundException{return Class.forName("dev.jason.gboardpatches.extension.sharedmodel.SharedVoiceEditController",false,ParakeetRecognizer.class.getClassLoader());}
 static boolean needsLocalRecognizer(){
  try{return Boolean.TRUE.equals(controller().getMethod("needsLocalRecognizer").invoke(null));}
  catch(ReflectiveOperationException | LinkageError unavailable){return false;}
 }
 static boolean begin(Context context){try{return Boolean.TRUE.equals(controller().getMethod("beginParakeetSession",Context.class).invoke(null,context));}catch(Throwable unavailable){return false;}}
 static void finish(Context context,String transcript){try{controller().getMethod("onParakeetCommand",Context.class,String.class).invoke(null,context,transcript);}catch(Throwable unavailable){new android.os.Handler(android.os.Looper.getMainLooper()).post(()->android.widget.Toast.makeText(context,"Voice edit was cancelled. Your text was kept.",android.widget.Toast.LENGTH_LONG).show());}}
}
