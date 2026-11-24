package dev.jason.gboardpatches.extension.sharedmodel;
import java.lang.reflect.Method;
/** Default-package native adapter is reached only after patch-time validation/installation. */
public final class NativeEditBridge {
 private NativeEditBridge(){}
 private static Class<?> adapter()throws ReflectiveOperationException{return Class.forName("MorpheSharedEditor188",true,NativeEditBridge.class.getClassLoader());}
 public static boolean available(){try{return (Boolean)adapter().getMethod("available").invoke(null);}catch(ReflectiveOperationException missing){return false;}}
 public static void invalidate(){try{adapter().getMethod("invalidate").invoke(null);}catch(ReflectiveOperationException ignored){}}
 /** Use the original Writing Tools access-point request, including its native trigger context. */
 static void activate(android.inputmethodservice.InputMethodService ime)throws ReflectiveOperationException{
  ClassLoader loader=ime.getClass().getClassLoader();
  Class<?> service=Class.forName("afae",false,loader),event=Class.forName("adan",false,loader),source=Class.forName("adbd",false,loader),trigger=Class.forName("aicj",false,loader),triggerType=Class.forName("avgf",false,loader),style=Class.forName("aeaj",false,loader);
  if(!service.isInstance(ime))throw new IllegalStateException("Native IME type changed");
  java.lang.reflect.Field accessPoint=source.getDeclaredField("d");accessPoint.setAccessible(true);
  Object reason=null;for(Object value:triggerType.getEnumConstants())if("KEYBOARD_FROM_ACCESS_POINT".equals(((Enum<?>)value).name()))reason=value;
  if(reason==null)throw new NoSuchFieldException("Native access-point trigger changed");
  Method make=trigger.getDeclaredMethod("d",triggerType);make.setAccessible(true);Object info=make.invoke(null,reason);
  Method dispatch=service.getDeclaredMethod("bT",event);dispatch.setAccessible(true);
  java.util.function.Consumer<Object> send=value->{try{dispatch.invoke(ime,value);}catch(ReflectiveOperationException failure){throw new IllegalStateException("Native writing activation failed",failure);}};
  Method open=Class.forName("pve",false,loader).getDeclaredMethod("f",source,trigger,boolean.class,style,java.util.function.Consumer.class);open.setAccessible(true);open.invoke(null,accessPoint.get(null),info,false,null,send);
 }
 static void leaveWriting(android.inputmethodservice.InputMethodService ime)throws ReflectiveOperationException{
  ClassLoader loader=ime.getClass().getClassLoader();Class<?> event=Class.forName("adan",false,loader),service=Class.forName("afae",false,loader);
  java.lang.reflect.Field type=Class.forName("pva",true,loader).getDeclaredField("a");type.setAccessible(true);
  Method make=event.getDeclaredMethod("e",int.class,Object.class);make.setAccessible(true);Object request=make.invoke(null,-10151,type.get(null));
  Method dispatch=service.getDeclaredMethod("bT",event);dispatch.setAccessible(true);dispatch.invoke(ime,request);
 }
 public static String prepare()throws ReflectiveOperationException{return (String)adapter().getMethod("prepare").invoke(null);}
 public static boolean apply(String text,String operation)throws ReflectiveOperationException{return (Boolean)adapter().getMethod("apply",String.class,String.class).invoke(null,text,operation);}
}
