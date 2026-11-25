package dev.jason.gboardpatches.extension.sharedmodel;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34,manifest=Config.NONE)
public class SharedSpeechControllerTest {
    @Test public void speechReadinessDoesNotDependOnGemmaReadiness()throws Exception{JSONObject host=new JSONObject("{\"state\":\"MEMORY_PAUSED\",\"enabled\":true,\"speech\":{\"enabled\":true,\"engineLoaded\":true,\"state\":\"READY\"}}");assertTrue(SharedSpeechController.readyStatus(host.getJSONObject("speech")));}
    @Test public void transientReadySnapshotDuringReconfigurationCannotBeSelected()throws Exception{assertFalse(SharedSpeechController.readyStatus(new JSONObject("{\"enabled\":true,\"engineLoaded\":true,\"state\":\"READY\",\"reconfiguring\":true}")));}
    @Test public void stoppedOrUnloadedSpeechCannotBecomeSelectable()throws Exception{assertFalse(SharedSpeechController.readyStatus(null));assertFalse(SharedSpeechController.readyStatus(new JSONObject("{\"enabled\":false,\"engineLoaded\":true,\"state\":\"READY\"}")));assertFalse(SharedSpeechController.readyStatus(new JSONObject("{\"enabled\":true,\"engineLoaded\":false,\"state\":\"READY\"}")));assertFalse(SharedSpeechController.readyStatus(new JSONObject("{\"enabled\":true,\"engineLoaded\":true,\"state\":\"MEMORY_PAUSED\"}")));}
}
