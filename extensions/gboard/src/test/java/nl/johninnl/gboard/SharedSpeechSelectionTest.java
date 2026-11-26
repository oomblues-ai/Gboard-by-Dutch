package nl.johninnl.gboard;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import java.lang.reflect.Proxy;
import org.junit.Test;
import static org.junit.Assert.*;

public class SharedSpeechSelectionTest {
    private Context context(boolean selected){
        SharedPreferences preferences=(SharedPreferences)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{SharedPreferences.class},(proxy,method,args)->{
            if(method.getName().equals("getBoolean"))return selected;
            if(method.getName().equals("getString"))return args[1];
            return null;
        });
        return new ContextWrapper(null){
            @Override public Context getApplicationContext(){return this;}
            @Override public SharedPreferences getSharedPreferences(String name,int mode){return preferences;}
            @Override public int checkSelfPermission(String permission){return PackageManager.PERMISSION_DENIED;}
        };
    }
    @Test public void selectedLocalSpeechDoesNotFallBackToGoogleWhenPermissionIsRevoked(){ParakeetRecognizer recognizer=ParakeetRecognizer.create(context(true));assertNotNull(recognizer);recognizer.shutdown();}
    @Test public void explicitlyUnselectedLocalSpeechKeepsTheOriginalFactory(){assertNull(ParakeetRecognizer.create(context(false)));}
    @Test public void unarmedRamblerKeepsTheOriginalRecognizerEvenWithSharedSpeechSelected() {
        assertNull(ParakeetRecognizer.createForVoiceRequest(context(true), true));
    }
    @Test public void standardVoiceRequestUsesSharedSpeechWhenSelected() {
        ParakeetRecognizer recognizer = ParakeetRecognizer.createForVoiceRequest(context(true), false);
        assertNotNull(recognizer);
        recognizer.shutdown();
    }
}
