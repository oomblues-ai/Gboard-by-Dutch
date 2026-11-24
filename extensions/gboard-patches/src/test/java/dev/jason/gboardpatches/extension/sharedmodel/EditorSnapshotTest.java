package dev.jason.gboardpatches.extension.sharedmodel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import android.inputmethodservice.InputMethodService;
import android.view.inputmethod.*;
import android.text.InputType;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34,manifest=Config.NONE)
public class EditorSnapshotTest {
 private static final class Connection extends BaseInputConnection {
  CharSequence text="I have 12 messages.";int start=0,end=19;int commits;String inserted;
  @Override public boolean beginBatchEdit(){return true;}
  @Override public boolean endBatchEdit(){return true;}
  @Override public boolean finishComposingText(){return true;}
  @Override public boolean commitText(CharSequence value,int cursor){commits++;inserted=value.toString();return true;}
  Connection(){super(new android.view.View(org.robolectric.RuntimeEnvironment.getApplication()),true);}
  @Override public ExtractedText getExtractedText(ExtractedTextRequest request,int flags){ExtractedText e=new ExtractedText();e.text=text;e.selectionStart=start;e.selectionEnd=end;return e;}
 }
 private static final class Ime extends InputMethodService {
  Connection connection=new Connection();EditorInfo info=new EditorInfo();
  @Override public InputConnection getCurrentInputConnection(){return connection;}
  @Override public EditorInfo getCurrentInputEditorInfo(){return info;}
 }
 @Test public void changedEditorOrSelectionDropsResult(){Ime ime=new Ime();EditorSnapshot s=EditorSnapshot.capture(ime,4);assertNotNull(s);assertTrue(s.isCurrent(4));ime.connection.end=0;assertFalse(s.isCurrent(4));}
 @Test public void sameLengthTextChangeIsStillStale(){Ime ime=new Ime();EditorSnapshot s=EditorSnapshot.capture(ime,4);ime.connection.text="I have 13 messages.";assertFalse(s.isCurrent(4));}
 @Test public void newInputConnectionInvalidatesSnapshot(){Ime ime=new Ime();EditorSnapshot s=EditorSnapshot.capture(ime,4);ime.connection=new Connection();assertFalse(s.isCurrent(4));}
 @Test public void revisionChangeInvalidatesIdenticalText(){Ime ime=new Ime();EditorSnapshot s=EditorSnapshot.capture(ime,4);assertFalse(s.isCurrent(5));}
 @Test public void composingRangeChangeInvalidatesSnapshot(){Ime ime=new Ime();EditorSnapshot s=EditorSnapshot.capture(ime,4);android.text.SpannableStringBuilder composing=new android.text.SpannableStringBuilder(ime.connection.text);BaseInputConnection.setComposingSpans(composing);ime.connection.text=composing;assertFalse(s.isCurrent(4));}
 @Test public void passwordEditorIsNeverCaptured(){Ime ime=new Ime();ime.info.inputType=InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD;assertNull(EditorSnapshot.capture(ime,4));}
 @Test public void inlineAcceptanceCannotWriteIntoAChangedEditor(){Ime ime=new Ime();ime.connection.start=ime.connection.end=ime.connection.text.length();EditorSnapshot snapshot=EditorSnapshot.capture(ime,4);assertNotNull(snapshot.completionPrefix());ime.connection.text="I have 13 messages.";assertFalse(snapshot.appendSuffix(" now",4));assertEquals(0,ime.connection.commits);}
 @Test public void inlineAcceptanceAppendsOnlyTheSuffixInAnUnchangedEditor(){Ime ime=new Ime();ime.connection.start=ime.connection.end=ime.connection.text.length();EditorSnapshot snapshot=EditorSnapshot.capture(ime,4);assertTrue(snapshot.appendSuffix(" now",4));assertEquals(" now",ime.connection.inserted);assertEquals(1,ime.connection.commits);assertFalse(snapshot.appendSuffix(" later",5));assertEquals(1,ime.connection.commits);}

}
