package dev.jason.gboardpatches.extension.sharedmodel;
import java.lang.reflect.Method;
/** Default-package native adapter is reached only after patch-time validation/installation. */
public final class NativeEditBridge {
 private NativeEditBridge(){}
 private static Class<?> adapter()throws ReflectiveOperationException{return Class.forName("MorpheSharedEditor188",true,NativeEditBridge.class.getClassLoader());}
 public static boolean available(){try{return (Boolean)adapter().getMethod("available").invoke(null);}catch(ReflectiveOperationException missing){return false;}}
 public static void invalidate(){try{adapter().getMethod("invalidate").invoke(null);}catch(ReflectiveOperationException ignored){}}
 /** Same secondary-keyboard event emitted by stock JarvisExtension; the native input bundle owns activation. */
 static void activate(android.inputmethodservice.InputMethodService ime)throws ReflectiveOperationException{
  ClassLoader loader=ime.getClass().getClassLoader();
  Class<?> keyboard=Class.forName("agoo",false,loader),payload=Class.forName("agoq",false,loader),event=Class.forName("adan",false,loader),service=Class.forName("afae",false,loader);
  if(!service.isInstance(ime))throw new IllegalStateException("Native IME type changed");
  java.lang.reflect.Field type=Class.forName("pva",true,loader).getDeclaredField("a");type.setAccessible(true);
  java.lang.reflect.Constructor<?> constructor=payload.getDeclaredConstructor(keyboard,Object.class);constructor.setAccessible(true);
  Object data=constructor.newInstance(type.get(null),null);
  Method make=event.getDeclaredMethod("e",int.class,Object.class);make.setAccessible(true);
  Object request=make.invoke(null,-10198,data);
  Method dispatch=service.getDeclaredMethod("bT",event);dispatch.setAccessible(true);dispatch.invoke(ime,request);
 }
 public static String prepare()throws ReflectiveOperationException{return (String)adapter().getMethod("prepare").invoke(null);}
 public static boolean apply(String text,String operation)throws ReflectiveOperationException{return (Boolean)adapter().getMethod("apply",String.class,String.class).invoke(null,text,operation);}
}
