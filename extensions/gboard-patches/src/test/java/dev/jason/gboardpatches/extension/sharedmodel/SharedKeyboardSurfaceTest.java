package dev.jason.gboardpatches.extension.sharedmodel;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=34,manifest=Config.NONE)
public class SharedKeyboardSurfaceTest {
 private Activity activity;private FrameLayout root,nativeFrame;private ViewSwitcher switcher;private TextView message;
 @Before public void create(){activity=Robolectric.buildActivity(Activity.class).setup().get();install(0xff123456);}
 private void install(int color){
  SharedKeyboardSurface.clear();root=new FrameLayout(activity);switcher=new ViewSwitcher(activity);switcher.setId(0x7f0b2a8b);nativeFrame=new FrameLayout(activity);
  message=new TextView(activity);message.setId(0x7f0b0436);message.setText("Native message");message.setTextColor(color);message.setTextSize(19);message.setTypeface(Typeface.create("serif",Typeface.BOLD_ITALIC));nativeFrame.addView(message);
  TextView chip=new TextView(activity);chip.setText("Native action");chip.setTextColor(color);chip.setClickable(true);GradientDrawable background=new GradientDrawable();background.setColor(color);background.setCornerRadius(42);chip.setBackground(background);nativeFrame.addView(chip);
  switcher.addView(nativeFrame);switcher.addView(new FrameLayout(activity));root.addView(switcher);activity.setContentView(root);SharedKeyboardSurface.bind(null,root);
 }
 @After public void cleanup(){SharedKeyboardSurface.clear();activity.finish();}
 @Test public void textInheritsActualNativeThemeRatherThanAFixedPalette(){TextView copy=SharedKeyboardSurface.styledText(message,"Local result");assertEquals(message.getTextColors(),copy.getTextColors());assertEquals(message.getTypeface(),copy.getTypeface());assertEquals(message.getTextSize(),copy.getTextSize(),0.01f);}
 @Test public void aThemeRebindUsesTheNewNativeTextStyle(){install(0xffab7812);LinearLayout body=SharedKeyboardSurface.body();SharedKeyboardSurface.label(body,"Local result");assertEquals(0xffab7812,((TextView)body.getChildAt(0)).getCurrentTextColor());}
 @Test public void extraSurfacePreservesBothNativeSwitcherChildrenAndRestoresVisibility(){LinearLayout body=SharedKeyboardSurface.body();SharedKeyboardSurface.label(body,"Local result");SharedKeyboardSurface.Panel panel=new SharedKeyboardSurface.Panel(body);assertTrue(panel.show());assertEquals(2,switcher.getChildCount());assertEquals(View.INVISIBLE,message.getVisibility());panel.dismiss();assertEquals(2,switcher.getChildCount());assertEquals(View.VISIBLE,message.getVisibility());assertEquals(2,nativeFrame.getChildCount());}
 @Test public void dismissCancelsOnlyItsOwnedSurfaceOnce(){LinearLayout body=SharedKeyboardSurface.body();SharedKeyboardSurface.Panel panel=new SharedKeyboardSurface.Panel(body);int[] calls={0};panel.setOnDismissListener(()->calls[0]++);assertTrue(panel.show());panel.dismiss();panel.dismiss();assertEquals(1,calls[0]);}
 @Test public void nativeSurfaceBackgroundIsNotCompositedTwice(){GradientDrawable background=new GradientDrawable();background.setColor(0x40123456);nativeFrame.setBackground(background);LinearLayout body=SharedKeyboardSurface.body();assertNull(body.getBackground());SharedKeyboardSurface.Panel panel=new SharedKeyboardSurface.Panel(body);assertTrue(panel.show());assertSame(background,nativeFrame.getBackground());panel.dismiss();assertSame(background,nativeFrame.getBackground());}
 @Test public void nativeBackgroundOpacitySurvivesCloning(){TextView original=(TextView)nativeFrame.getChildAt(1);original.getBackground().setAlpha(73);LinearLayout body=SharedKeyboardSurface.body();SharedKeyboardSurface.button(body,"Apply",()->{});assertEquals(73,body.getChildAt(0).getBackground().getAlpha());}
 @Test public void buttonsCloneTheNativeDrawableWithoutSharingItsMutableInstance(){LinearLayout body=SharedKeyboardSurface.body();TextView original=(TextView)nativeFrame.getChildAt(1);SharedKeyboardSurface.button(body,"Apply",()->{});TextView copy=(TextView)body.getChildAt(0);assertEquals(original.getTextColors(),copy.getTextColors());assertNotSame(original.getBackground(),copy.getBackground());assertNotNull(copy.getBackground());}
}
