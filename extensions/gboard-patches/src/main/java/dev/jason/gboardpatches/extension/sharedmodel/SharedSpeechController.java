package dev.jason.gboardpatches.extension.sharedmodel;

import android.content.*;
import android.os.*;
import app.johninnl.modelhost.api.*;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Public host speech only. No local model load, downloader or cloud fallback. */
public final class SharedSpeechController {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final ScheduledExecutorService POLL=Executors.newSingleThreadScheduledExecutor(r->new Thread(r,"gboard-shared-speech"));
    private static volatile ModelHostClient client;
    private static volatile boolean connected;
    private static volatile JSONObject status;
    private static volatile String error="PROVIDER_MISSING";
    private static volatile boolean polling;
    private SharedSpeechController(){}
    public static synchronized void connect(Context context){
        if(client!=null)return;Context app=context.getApplicationContext();
        try{SharedProviderIdentity.verify(app);}catch(Exception failure){error="PROVIDER_UNAVAILABLE";return;}
        ModelHostClient connecting=new ModelHostClient(new ComponentName(ModelHostClient.DEFAULT_PACKAGE,ModelHostClient.SERVICE_CLASS),Runnable::run);client=connecting;
        MAIN.post(()->{if(client!=connecting)return;connecting.connect(app,new ConnectionListener(){
            @Override public void onConnected(HostStatus value,HostCapabilities capabilities){if(client!=connecting)return;connected=true;accept(value);}
            @Override public void onDisconnected(HostException failure){if(client!=connecting)return;connected=false;status=null;error=failure.code;client=null;connecting.close();}
        });});
        if(!polling){polling=true;POLL.scheduleWithFixedDelay(()->{ModelHostClient current=client;if(current==null||!connected)return;try{accept(current.status());}catch(Exception failure){status=null;error="PROVIDER_UNAVAILABLE";}},1,2,TimeUnit.SECONDS);}
    }
    private static void accept(HostStatus value){try{JSONObject json=new JSONObject(value.toJson());status=json.optJSONObject("speech");error=status==null?"HOST_UPDATE_REQUIRED":status.optString("state","SPEECH_UNAVAILABLE");}catch(Exception failure){status=null;error="PROTOCOL_ERROR";}}
    public static boolean ready(Context context){connect(context);return connected&&readyStatus(status);}
    static boolean readyStatus(JSONObject speech){return speech!=null&&speech.optBoolean("enabled")&&speech.optBoolean("engineLoaded")&&("READY".equals(speech.optString("state"))||"BUSY".equals(speech.optString("state")));}
    public static String summary(Context context){connect(context);if(SharedProviderSetup.busy())return SharedProviderSetup.message();JSONObject speech=status;if(speech==null)return "Shared speech: "+error+". Open Local AI to set it up.";return speech.optString("modelLabel","Shared speech")+" · "+speech.optString("state")+(speech.optBoolean("enabled")?"":" · off");}
    public static void openSettings(Context context){if(!ProviderDiscovery.inspect(context).installed || SharedProviderSetup.needsUpdate(context) || SharedProviderSetup.pending()!=null){SharedProviderSetup.download(context);return;}try{context.startActivity(new Intent().setClassName(ModelHostClient.DEFAULT_PACKAGE,"app.johninnl.modelhost.setup.HostSettingsActivity").putExtra("section","speech").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}catch(ActivityNotFoundException failure){android.widget.Toast.makeText(context,"Install or update Local Model Host first.",android.widget.Toast.LENGTH_LONG).show();}}
    /** Reflection entry used by the standalone Gboard extension. Call on its microphone worker. */
    public static Object open(Context context) throws Exception {
        connect(context);long deadline=SystemClock.elapsedRealtime()+8000;
        while(!connected&&SystemClock.elapsedRealtime()<deadline){Thread.sleep(25);}
        if(!connected||status==null||!status.optBoolean("enabled"))throw new HostException("SHARED_SPEECH_NOT_READY");
        SpeechSession vad=client.openSpeechSession("auto");
        try{deadline=SystemClock.elapsedRealtime()+30000;while(!ready(context)&&SystemClock.elapsedRealtime()<deadline){
            accept(client.status());String state=error;if("MEMORY_PAUSED".equals(state)||"FAILED".equals(state)||"MODEL_MISSING".equals(state))throw new HostException("SHARED_SPEECH_"+state);Thread.sleep(100);
        }if(!ready(context))throw new HostException("SHARED_SPEECH_NOT_READY");return new CaptureSession(client,vad);}catch(Exception failure){vad.close();throw failure;}
    }
    public static final class CaptureSession implements AutoCloseable {
        private final ModelHostClient connection;private final SpeechSession vad;
        private SpeechSession decoder;private int sent;private final AtomicBoolean closed=new AtomicBoolean();
        CaptureSession(ModelHostClient connection,SpeechSession vad){this.connection=connection;this.vad=vad;}
        public float probability(float[] window)throws HostException{if(closed.get())throw new HostException("SPEECH_SESSION_CLOSED");return vad.voiceProbability(window);}
        public void reset(){SpeechSession current=decoder;decoder=null;if(current!=null)current.close();sent=0;lastFeedEnded=false;}
        public void releaseVad(){vad.close();}
        /** Decoder worker only: delta feeds preserve Nemotron caches and the other backend's stream merger. */
        public String transcribe(float[] all,int count,boolean end)throws Exception {
            if(closed.get())throw new HostException("SPEECH_SESSION_CLOSED");if(count<sent)throw new IllegalArgumentException("Audio went backwards");
            if(decoder==null)decoder=connection.openSpeechSession("auto");String text="";
            while(sent<count){int next=Math.min(count,sent+16000);boolean finish=end&&next==count;text=feed(Arrays.copyOfRange(all,sent,next),finish);sent=next;}
            // An utterance may end with no new samples after its last partial.
            if(end && !lastFeedEnded)text=feed(new float[0],true);
            if(end){decoder.close();decoder=null;sent=0;lastFeedEnded=false;}return text;
        }
        private boolean lastFeedEnded;
        private String feed(float[] samples,boolean end)throws Exception {
            CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();AtomicReference<String> failure=new AtomicReference<>();
            RequestHandle handle=decoder.feed(samples,end,json->{try{JSONObject value=new JSONObject(json);if(!value.optBoolean("terminal"))return;if("ok".equals(value.optString("status")))result.set(value.optString("text",""));else failure.set(value.optString("code","SPEECH_FAILED"));}catch(Exception error){failure.set("PROTOCOL_ERROR");}finally{done.countDown();}});
            try{if(!done.await(30,TimeUnit.SECONDS)){handle.cancel();throw new HostException("SPEECH_TIMEOUT");}}catch(InterruptedException error){handle.cancel();Thread.currentThread().interrupt();throw error;}
            if(closed.get())throw new HostException("SPEECH_SESSION_CLOSED");if(failure.get()!=null)throw new HostException(failure.get());lastFeedEnded=end;return result.get()==null?"":result.get();
        }
        @Override public void close(){if(!closed.compareAndSet(false,true))return;vad.close();SpeechSession current=decoder;if(current!=null)current.close();}
    }
}
