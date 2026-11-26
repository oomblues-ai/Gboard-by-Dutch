package dev.jason.gboardpatches.extension.sharedmodel;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import app.johninnl.modelhost.api.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.json.JSONObject;

/** User-initiated setup work; the existing Patches screen renders its progress. */
final class SharedProviderSetup {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor(r->new Thread(r,"gboard-provider-setup"));
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static volatile boolean busy;
    private static volatile String message="";
    private static volatile VerifiedApk pending;
    private static volatile long generation;
    private static Future<?> job;
    static boolean installedMatchesReview(Context context){try{SharedProviderIdentity.verify(context);android.content.pm.PackageInfo info=context.getPackageManager().getPackageInfo(ModelHostClient.DEFAULT_PACKAGE,0);long version=android.os.Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;return version==release(context).versionCode;}catch(Exception unavailable){return false;}}
    static boolean needsUpdate(Context context){try{android.content.pm.PackageInfo info=context.getPackageManager().getPackageInfo(ModelHostClient.DEFAULT_PACKAGE,0);long version=android.os.Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;return version<release(context).versionCode;}catch(Exception unavailable){return false;}}
    static String platformMessage(){return "Local AI requires Android 11 or later and a 64-bit ARM device.";}
    static boolean busy(){return busy;}
    static String message(){return message;}
    static VerifiedApk pending(){return pending;}
    static void finished(String status){pending=null;busy=false;message=status;}
    static SharedProviderRelease release(Context context)throws Exception{
        try(InputStream input=context.getAssets().open("morphe/model-host-release.json")){
            ByteArrayOutputStream data=new ByteArrayOutputStream();byte[] buffer=new byte[2048];int count;
            while((count=input.read(buffer))!=-1){if(data.size()+count>8192)throw new IOException("Release identity too large");data.write(buffer,0,count);}
            return new SharedProviderRelease(new JSONObject(data.toString("UTF-8")));
        }
    }
    private static VerifiedApk verify(Context context,Uri uri,SharedProviderRelease release)throws Exception{
        VerifiedApk result=VerifiedApk.verify(context,uri,release.packageName,release.apkSha256,release.signerSha256);
        if(result.versionCode!=release.versionCode)throw new IOException("Version mismatch");return result;
    }
    static synchronized void cancel(){generation++;if(job!=null)job.cancel(true);busy=false;message="Download cancelled.";}
    static synchronized void download(Context context){
        if(dev.jason.gboardpatches.extension.privacy.GboardTelemetryPolicy.isEnabled(context) && pending==null){
            message="Gboard network access is blocked. Download Local Model Host separately and open the APK here.";
            return;
        }
        if(!ProviderDiscovery.isOfficialProviderSupported()){message=platformMessage();return;}
        if(busy)return;
        if(pending!=null){SharedModelSetupActivity.install(context);return;}
        Context app=context.getApplicationContext();java.lang.ref.WeakReference<Context> source=new java.lang.ref.WeakReference<>(context);long current=++generation;busy=true;message="Downloading companion app…";
        job=WORKER.submit(()->{
            File copy=null;
            try{
                SharedProviderRelease release=release(app);if(release.download==null)throw new IOException("Release unavailable");
                copy=File.createTempFile("local-ai-download-",".apk",app.getCacheDir());
                release.downloadTo(copy,(done,total)->{if(generation==current)message="Downloading companion app"+(total>0?" · "+done*100/total+"%":"…");});
                if(generation!=current)return;message="Checking the companion app…";
                VerifiedApk verified=verify(app,Uri.fromFile(copy),release);
                MAIN.post(()->{if(generation!=current)return;pending=verified;busy=false;message="Ready to install.";Context origin=source.get();if(origin instanceof android.app.Activity && ((android.app.Activity)origin).hasWindowFocus()){try{SharedModelSetupActivity.install(origin);}catch(RuntimeException unavailable){message="Ready to install. Tap Install companion app to continue.";}}});
            }catch(Exception failure){if(generation==current){busy=false;message="Could not download the companion app. Check your connection and try again.";}}
            finally{if(copy!=null)copy.delete();}
        });
    }
    static synchronized void verifySelected(Context context,Uri uri,Consumer<Boolean> result){
        if(!ProviderDiscovery.isOfficialProviderSupported()){message=platformMessage();MAIN.post(()->result.accept(false));return;}
        if(busy){result.accept(false);return;}
        Context app=context.getApplicationContext();long current=++generation;busy=true;message="Checking the selected app…";
        job=WORKER.submit(()->{
            try{VerifiedApk verified=verify(app,uri,release(app));MAIN.post(()->{if(generation!=current)return;pending=verified;busy=false;message="Ready to install.";result.accept(true);});}
            catch(Exception failure){MAIN.post(()->{if(generation!=current)return;busy=false;message="That APK does not match the verified companion app.";result.accept(false);});}
        });
    }
}
