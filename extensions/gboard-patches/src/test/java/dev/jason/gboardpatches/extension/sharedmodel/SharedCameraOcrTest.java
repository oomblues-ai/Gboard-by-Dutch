package dev.jason.gboardpatches.extension.sharedmodel;
import org.junit.Test;
import static org.junit.Assert.*;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,manifest=org.robolectric.annotation.Config.NONE)
public class SharedCameraOcrTest {
 private static final class Editor extends android.inputmethodservice.InputMethodService {
  final android.view.inputmethod.EditorInfo info=new android.view.inputmethod.EditorInfo();
  final android.view.inputmethod.InputConnection input=new android.view.inputmethod.BaseInputConnection(new android.view.View(org.robolectric.RuntimeEnvironment.getApplication()),true){
   @Override public android.view.inputmethod.ExtractedText getExtractedText(android.view.inputmethod.ExtractedTextRequest request,int flags){android.view.inputmethod.ExtractedText value=new android.view.inputmethod.ExtractedText();value.text="Draft";value.selectionStart=value.selectionEnd=5;return value;}
  };
  Editor(){info.inputType=android.text.InputType.TYPE_CLASS_TEXT;}
  @Override public android.content.Context getApplicationContext(){return org.robolectric.RuntimeEnvironment.getApplication();}
  @Override public android.view.inputmethod.EditorInfo getCurrentInputEditorInfo(){return info;}
  @Override public android.view.inputmethod.InputConnection getCurrentInputConnection(){return input;}
 }
 private static final class Camera {
  final android.content.Context b;final android.widget.TextView f;
  Camera(android.content.Context context){b=context;f=new android.widget.TextView(context);}
 }
 @Test public void insertionUsesOnlyTheChosenTextRange(){assertEquals("12.34",SharedCameraOcr.selection("Total 12.34 EUR",6,11));}
 @Test public void absentOrCollapsedSelectionInsertsTheWholeText(){assertEquals("line one\nline two",SharedCameraOcr.selection("line one\nline two",-1,-1));assertEquals("abc",SharedCameraOcr.selection("abc",2,2));}
 @Test public void invalidRangesNeverInsertEverythingAndReverseSelectionsAreNormalized(){assertEquals("",SharedCameraOcr.selection("abc",0,4));assertEquals("b",SharedCameraOcr.selection("abc",2,1));}
 @Test public void memoryPauseIsNotMisreportedAsDisabledImageSupport(){String message=SharedCameraOcr.failureMessage("MEMORY_PAUSED");assertTrue(message.contains("memory"));assertTrue(message.contains("Model setup"));assertFalse(message.contains("Image support is OFF"));}
 @Test public void disabledVisionAndStoppedHostHaveDifferentRecoveryInstructions(){assertTrue(SharedCameraOcr.failureMessage("ENABLE_OCR_IN_PROVIDER").contains("Image support is OFF"));assertTrue(SharedCameraOcr.failureMessage("PROVIDER_STOPPED").contains("Provider OFF"));}
 @Test public void unknownFailureNeverExposesPrivateExceptionText(){assertFalse(SharedCameraOcr.failureMessage("private image text").contains("private image text"));}
 @Test public void imageChoiceIsNotBlockedByAnUnreadyModel(){
  android.content.Context app=org.robolectric.RuntimeEnvironment.getApplication();
  dev.jason.gboardpatches.extension.writingtools.GboardAiWritingToolsSettings.preferences(app).edit().clear().commit();
  dev.jason.gboardpatches.extension.ocr.GboardOcrSettings.preferences(app).edit().putBoolean(dev.jason.gboardpatches.extension.ocr.GboardOcrSettings.PREF_KEY_SHARED_IMAGE_OCR,true).commit();
  final android.content.Intent[] opened={null};android.content.Context context=new android.content.ContextWrapper(app){@Override public void startActivity(android.content.Intent intent){opened[0]=intent;throw new android.content.ActivityNotFoundException();}};
  Editor editor=new Editor();GboardSharedWritingRuntime.onInputStarting(editor,editor.info);
  assertFalse(SharedModelClient.isReadyCached());Camera camera=new Camera(context);SharedCameraOcr.bind(camera);assertTrue(camera.f.performClick());assertNotNull(opened[0]);assertEquals(SharedImageInputActivity.class.getName(),opened[0].getComponent().getClassName());SharedCameraOcr.clear(camera);GboardSharedWritingRuntime.onInputHidden(editor);
 }
 @Test public void privateEditorStillCannotLaunchThePicker(){
  android.content.Context app=org.robolectric.RuntimeEnvironment.getApplication();
  dev.jason.gboardpatches.extension.writingtools.GboardAiWritingToolsSettings.preferences(app).edit().clear().commit();
  dev.jason.gboardpatches.extension.ocr.GboardOcrSettings.preferences(app).edit().putBoolean(dev.jason.gboardpatches.extension.ocr.GboardOcrSettings.PREF_KEY_SHARED_IMAGE_OCR,true).commit();
  final boolean[] opened={false};android.content.Context context=new android.content.ContextWrapper(app){@Override public void startActivity(android.content.Intent intent){opened[0]=true;}};
  Editor editor=new Editor();editor.info.inputType=android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD;GboardSharedWritingRuntime.onInputStarting(editor,editor.info);
  Camera camera=new Camera(context);SharedCameraOcr.bind(camera);camera.f.performClick();assertFalse(opened[0]);SharedCameraOcr.clear(camera);GboardSharedWritingRuntime.onInputHidden(editor);
 }
}
