package dev.jason.gboardpatches.extension.sharedmodel;
import org.junit.Test;
import static org.junit.Assert.*;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=34,manifest=org.robolectric.annotation.Config.NONE)
public class SharedCameraOcrTest {
 @Test public void insertionUsesOnlyTheChosenTextRange(){assertEquals("12.34",SharedCameraOcr.selection("Total 12.34 EUR",6,11));}
 @Test public void absentOrCollapsedSelectionInsertsTheWholeText(){assertEquals("line one\nline two",SharedCameraOcr.selection("line one\nline two",-1,-1));assertEquals("abc",SharedCameraOcr.selection("abc",2,2));}
 @Test public void invalidRangesNeverInsertEverythingAndReverseSelectionsAreNormalized(){assertEquals("",SharedCameraOcr.selection("abc",0,4));assertEquals("b",SharedCameraOcr.selection("abc",2,1));}
}
