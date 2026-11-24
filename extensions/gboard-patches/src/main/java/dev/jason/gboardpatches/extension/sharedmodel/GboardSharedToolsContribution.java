package dev.jason.gboardpatches.extension.sharedmodel;
import android.content.Context;
import java.util.*;
import dev.jason.gboardpatches.extension.editingaccesspoints.GboardEditingAccessPointsContribution;
/** Optional local operations share the existing access-point catalog/controller lifecycle. */
public final class GboardSharedToolsContribution {
 private static final String TOKEN="shared_local_tools";
 private GboardSharedToolsContribution(){}
 public static Object extend(Context context,Object original){
  if(!GboardSharedWritingRuntime.selected(context) || !(original instanceof Collection<?>))return original;
  try{List<Object> values=new ArrayList<>((Collection<?>)original);if(values.contains(TOKEN))return original;values.add(TOKEN);Class<?> set=Class.forName("auco",false,original.getClass().getClassLoader());if(!set.isInstance(original))return original;return set.getMethod("n",Collection.class).invoke(null,values);}catch(Exception e){return original;}
 }
 public static void register(Object controller,Context context){try{int icon=0x7f0804b7;if(!"drawable".equals(context.getResources().getResourceTypeName(icon)))return;GboardEditingAccessPointsContribution.registerExtra(controller,context,TOKEN,icon,"Local AI",()->SharedToolsMenu.open(context));}catch(Exception unavailable){}}
}
