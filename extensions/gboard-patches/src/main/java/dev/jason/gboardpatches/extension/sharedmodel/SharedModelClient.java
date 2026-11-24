package dev.jason.gboardpatches.extension.sharedmodel;

import android.content.Context;
import android.content.ComponentName;
import android.os.Handler;
import android.os.Looper;
import app.johninnl.modelhost.api.*;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;

/** One public SDK connection per Gboard process. Host absence never falls back to a hidden local engine or Google request. */
public final class SharedModelClient {
    private static final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->new Thread(r,"gboard-shared-model"));
    private static final Set<CompletableFuture<String>> pending=ConcurrentHashMap.newKeySet();
    private static volatile ModelHostClient client;
    private static volatile HostSession session;
    private static volatile boolean ready;
    private static volatile boolean connected;
    private static volatile boolean active;
    private static ScheduledFuture<?> poll;
    private static volatile String error="PROVIDER_MISSING";
    private SharedModelClient(){}
    public static synchronized void connect(Context context){
        if(client!=null)return;
        Context app=context.getApplicationContext();
        try{SharedProviderIdentity.verify(app);}catch(HostException failure){error=failure.code;ready=false;return;}
        ModelHostClient connecting=new ModelHostClient(new ComponentName(ModelHostClient.DEFAULT_PACKAGE,ModelHostClient.SERVICE_CLASS),Runnable::run);client=connecting;
        new Handler(Looper.getMainLooper()).post(()->{if(client!=connecting)return;connecting.connect(app,new ConnectionListener(){
            @Override public void onConnected(HostStatus status,HostCapabilities capabilities){if(client!=connecting)return;connected=true;ready=status.isReady();error=status.state;worker.execute(SharedModelClient::openSession);}
            @Override public void onDisconnected(HostException failure){if(client!=connecting)return;connected=false;ready=false;session=null;error=failure.code;cancelRequests();}
        });});
        if(poll!=null)poll.cancel(false);
        poll=worker.scheduleWithFixedDelay(()->{if(!connected)return;try{HostStatus status=client.status();ready=status.isReady();error=status.state;}catch(Exception e){ready=false;error="PROVIDER_UNAVAILABLE";}},1,2,TimeUnit.SECONDS);
    }
    private static void openSession(){if(!active || !connected || session!=null)return;try{session=client.openSession(new SessionOptions("Gboard",null,4096));}catch(Exception e){error="SESSION_UNAVAILABLE";}}
    public static synchronized void activate(Context context){active=true;if(client!=null && !connected){client.close();client=null;}connect(context);worker.execute(SharedModelClient::openSession);}
    public static void releaseSession(){active=false;cancelRequests();worker.execute(()->{HostSession old=session;session=null;if(old!=null)old.close();});}
    public static boolean isReadyCached(){return ready && connected;}
    public static boolean ready(Context context){connect(context);return ready;}
    public static String status(Context context){connect(context);return error;}
    public static CompletableFuture<String> request(Context context,String operation,String text,String instruction){return request(context,new TextRequest(operation,text,instruction,null,null,null,512,30000));}
    public static CompletableFuture<String> request(Context context,TextRequest request){
        connect(context);CompletableFuture<String> future=new CompletableFuture<>();pending.add(future);AtomicReference<RequestHandle> handle=new AtomicReference<>();
        future.whenComplete((v,e)->{pending.remove(future);if(future.isCancelled()){RequestHandle h=handle.get();if(h!=null)h.cancel();}});
        worker.execute(()->{
            if(future.isCancelled())return;
            HostSession active=session;
            if(active==null || !ready){future.completeExceptionally(new IllegalStateException(error));return;}
            try {
                RequestHandle made=active.submitText(request,json->{
                    try{JSONObject o=new JSONObject(json);if(!o.optBoolean("terminal"))return;if("ok".equals(o.optString("status")))future.complete(o.getString("text"));else future.completeExceptionally(new IllegalStateException(o.optString("code","INFERENCE_FAILED")));}catch(Exception e){future.completeExceptionally(new IllegalStateException("PROTOCOL_ERROR"));}
                });
                handle.set(made);if(future.isCancelled())made.cancel();
            }catch(Exception e){future.completeExceptionally(new IllegalStateException("PROVIDER_UNAVAILABLE"));}
        });return future;
    }
    public static void cancelRequests(){for(CompletableFuture<String> future:pending)future.cancel(false);}
}
