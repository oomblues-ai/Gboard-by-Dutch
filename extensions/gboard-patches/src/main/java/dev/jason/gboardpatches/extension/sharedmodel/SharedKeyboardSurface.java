package dev.jason.gboardpatches.extension.sharedmodel;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;

/** Additional tasks live inside the native Writing Tools body and copy its actual themed view styles. */
public final class SharedKeyboardSurface {
    private static final int BODY_SWITCHER=0x7f0b2a8b;
    private static final int MESSAGE_TEXT=0x7f0b0436;
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static WeakReference<ViewGroup> keyboard=new WeakReference<>(null);
    private static WeakReference<ViewSwitcher> switcher=new WeakReference<>(null);
    private static Panel current;
    private SharedKeyboardSurface() {}

    public static void bind(Object owner,Object root){
        if(!(root instanceof ViewGroup))return;
        View candidate=((ViewGroup)root).findViewById(BODY_SWITCHER);
        if(!(candidate instanceof ViewSwitcher))return;
        if(switcher.get()!=candidate)clear();
        keyboard=new WeakReference<>((ViewGroup)root);switcher=new WeakReference<>((ViewSwitcher)candidate);((ViewGroup)root).post(SharedWritingActivation::nativeReady);
    }
    public static void clear(){Panel old=current;current=null;if(old!=null)old.detach();keyboard.clear();switcher.clear();}
    static boolean available(){ViewGroup root=keyboard.get();ViewSwitcher body=switcher.get();return root!=null && body!=null && root.isAttachedToWindow() && root.isShown();}
    static void whenAvailable(Context context,Runnable show){await(context,show,20);}
    private static void await(Context context,Runnable show,int remaining){
        if(available()){show.run();return;}
        if(remaining==0){SharedModelClient.noteFailure("NATIVE_THEME_UNAVAILABLE");SharedTextActions.message(context,"Gboard’s writing surface is not available in this editor.");return;}
        long revision=GboardSharedWritingRuntime.currentRevision();
        MAIN.postDelayed(()->{if(revision==GboardSharedWritingRuntime.currentRevision() && GboardSharedWritingRuntime.currentService()!=null)await(context,show,remaining-1);},100);
    }
    private static ViewGroup root(){ViewGroup root=keyboard.get();if(root==null)throw new IllegalStateException("NATIVE_THEME_UNAVAILABLE");return root;}
    private static ViewGroup templateRoot(){ViewSwitcher body=switcher.get();View current=body==null?null:body.getCurrentView();if(!(current instanceof ViewGroup))throw new IllegalStateException("NATIVE_THEME_UNAVAILABLE");return (ViewGroup)current;}
    private static TextView messageTemplate(){
        ViewGroup root=templateRoot();View known=root.findViewById(MESSAGE_TEXT);
        if(known instanceof TextView)return (TextView)known;
        TextView text=findText(root,false);if(text==null)throw new IllegalStateException("NATIVE_THEME_UNAVAILABLE");return text;
    }
    private static TextView buttonTemplate(){TextView text=findText(templateRoot(),true);if(text==null)throw new IllegalStateException("NATIVE_THEME_UNAVAILABLE");return text;}
    private static TextView findText(View view,boolean clickable){
        if(view instanceof TextView && !(view instanceof EditText)){
            TextView text=(TextView)view;
            if(text.length()>0 && (!clickable || clickableOwner(text)!=null))return text;
        }
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){TextView text=findText(group.getChildAt(i),clickable);if(text!=null)return text;}}
        return null;
    }
    private static View clickableOwner(View source){
        ViewGroup boundary=root();View view=source;
        while(view!=null && view!=boundary){if(view.isClickable())return view;ViewParent parent=view.getParent();view=parent instanceof View?(View)parent:null;}
        return null;
    }
    private static Drawable copy(Drawable drawable,Context context){if(drawable==null || drawable.getConstantState()==null)return null;Drawable result=drawable.getConstantState().newDrawable(context.getResources(),context.getTheme()).mutate();result.setAlpha(drawable.getAlpha());result.setColorFilter(drawable.getColorFilter());result.setLevel(drawable.getLevel());result.setLayoutDirection(drawable.getLayoutDirection());return result;}
    static TextView styledText(TextView source,String value){
        TextView text=new TextView(source.getContext());text.setText(value);text.setTextColor(source.getTextColors());text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,source.getTextSize());text.setTypeface(source.getTypeface());text.setLetterSpacing(source.getLetterSpacing());text.setFontFeatureSettings(source.getFontFeatureSettings());text.setGravity(source.getGravity());text.setIncludeFontPadding(source.getIncludeFontPadding());text.setTextDirection(source.getTextDirection());text.setTextAlignment(source.getTextAlignment());if(source.getFontVariationSettings()!=null)text.setFontVariationSettings(source.getFontVariationSettings());text.setLineSpacing(source.getLineSpacingExtra(),source.getLineSpacingMultiplier());return text;
    }
    static LinearLayout body(){
        ViewSwitcher nativeBody=switcher.get();if(nativeBody==null)throw new IllegalStateException("NATIVE_THEME_UNAVAILABLE");
        LinearLayout result=new LinearLayout(nativeBody.getContext());result.setOrientation(LinearLayout.VERTICAL);
        View template=nativeBody.getChildAt(Math.max(0,Math.min(nativeBody.getDisplayedChild(),nativeBody.getChildCount()-1)));
        if(template!=null){result.setPadding(template.getPaddingLeft(),template.getPaddingTop(),template.getPaddingRight(),template.getPaddingBottom());}
        return result;
    }
    static void label(LinearLayout body,String value){TextView text=styledText(messageTemplate(),value);text.setTextIsSelectable(false);body.addView(text,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));}
    static void button(LinearLayout body,String title,Runnable action){
        TextView template=buttonTemplate();View shape=clickableOwner(template);Drawable background=copy(shape.getBackground(),template.getContext());
        if(background==null)throw new IllegalStateException("NATIVE_THEME_UNAVAILABLE");
        TextView button=styledText(template,title);button.setBackground(background);button.setBackgroundTintList(shape.getBackgroundTintList());button.setPadding(shape.getPaddingLeft(),shape.getPaddingTop(),shape.getPaddingRight(),shape.getPaddingBottom());button.setMinHeight(Math.max(shape.getMinimumHeight(),shape.getHeight()));button.setMinWidth(shape.getMinimumWidth());button.setElevation(shape.getElevation());if(shape.isEnabled())button.setAlpha(shape.getAlpha());button.setClickable(true);button.setFocusable(true);button.setEnabled(true);button.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        ViewGroup.LayoutParams source=shape.getLayoutParams();if(source instanceof ViewGroup.MarginLayoutParams){ViewGroup.MarginLayoutParams margins=(ViewGroup.MarginLayoutParams)source;params.setMargins(margins.leftMargin,margins.topMargin,margins.rightMargin,margins.bottomMargin);}body.addView(button,params);
    }
    static final class Panel {
        final LinearLayout content;private FrameLayout wrapper,parent;private Runnable onDismiss;private boolean detached;
        private final java.util.Map<View,Integer> hidden=new java.util.LinkedHashMap<>();
        Panel(LinearLayout content){this.content=content;}
        void setOnDismissListener(Runnable action){onDismiss=action;}
        boolean show(){
            ViewSwitcher nativeBody=switcher.get();if(detached || nativeBody==null || !available())return false;
            View child=nativeBody.getCurrentView();if(!(child instanceof FrameLayout))return false;
            if(current!=null)current.dismiss();
            parent=(FrameLayout)child;
            for(int i=0;i<parent.getChildCount();i++){View original=parent.getChildAt(i);hidden.put(original,original.getVisibility());original.setVisibility(View.INVISIBLE);}
            wrapper=new FrameLayout(parent.getContext());ScrollView scroll=new ScrollView(parent.getContext());scroll.addView(content);wrapper.addView(scroll,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
            parent.addView(wrapper,new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));current=this;return true;
        }
        void dismiss(){if(current==this)current=null;detach();}
        private void detach(){
            if(detached)return;detached=true;
            if(wrapper!=null && parent!=null)parent.removeView(wrapper);
            for(java.util.Map.Entry<View,Integer> original:hidden.entrySet())original.getKey().setVisibility(original.getValue());hidden.clear();
            if(onDismiss!=null)onDismiss.run();
        }
    }
}
