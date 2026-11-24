package dev.jason.gboardpatches.extension.sharedmodel;
import java.lang.reflect.Method;
/** Default-package native adapter is reached only after patch-time validation/installation. */
public final class NativeEditBridge {
 private NativeEditBridge(){}
 private static Class<?> adapter()throws ReflectiveOperationException{return Class.forName("MorpheSharedEditor188",true,NativeEditBridge.class.getClassLoader());}
 public static String prepare()throws ReflectiveOperationException{return (String)adapter().getMethod("prepare").invoke(null);}
 public static boolean apply(String text,String operation)throws ReflectiveOperationException{return (Boolean)adapter().getMethod("apply",String.class,String.class).invoke(null,text,operation);}
}
