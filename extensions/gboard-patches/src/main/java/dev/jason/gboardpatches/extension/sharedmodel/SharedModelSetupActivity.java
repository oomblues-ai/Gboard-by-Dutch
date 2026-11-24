package dev.jason.gboardpatches.extension.sharedmodel;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import app.johninnl.modelhost.api.*;
import dev.jason.gboardpatches.extension.R;
import dev.jason.gboardpatches.extension.settings.GboardSettingsText;
import java.io.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Normal Android installation initiated from a visible Gboard settings action. */
public final class SharedModelSetupActivity extends Activity {
    private static final int PICK_APK=7401,APPROVE_SOURCE=7402,INSTALL_APK=7403;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->new Thread(r,"gboard-provider-setup"));
    private TextView status,selection,lastFailure;
    private Button setup;
    private Button download,useLocal;
    private Future<?> downloadJob;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean resumed,busy,showingError;
    private final Runnable refreshLoop=new Runnable(){@Override public void run(){if(!resumed)return;if(!busy && !showingError)refresh();main.postDelayed(this,1000);}};
    private VerifiedApk pending;
    public static void open(Context context){context.startActivity(new Intent(context,SharedModelSetupActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    private String text(int id){return GboardSettingsText.get(this,id);}
    @Override public void onCreate(Bundle state){
        boolean dark=(getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
        setTheme(dark?android.R.style.Theme_Material_NoActionBar:android.R.style.Theme_Material_Light_NoActionBar);super.onCreate(state);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(20),dp(20),dp(24));
        TextView title=new TextView(this);title.setText("Local AI for Gboard");title.setTextSize(26);body.addView(title);
        selection=new TextView(this);selection.setTextSize(22);selection.setTypeface(null,android.graphics.Typeface.BOLD);selection.setPadding(0,dp(20),0,dp(12));body.addView(selection);
        TextView note=new TextView(this);note.setText("Gboard selection and model readiness are separate. Installing the provider does not turn Local AI on.");note.setTextSize(16);body.addView(note);
        status=new TextView(this);status.setTextSize(18);status.setPadding(0,dp(20),0,dp(16));body.addView(status);
        useLocal=new Button(this);useLocal.setOnClickListener(v->enableLocal());body.addView(useLocal);
        setup=new Button(this);setup.setText("Open provider setup / Start or stop model");setup.setOnClickListener(v->openProvider());body.addView(setup);
        lastFailure=new TextView(this);lastFailure.setTextSize(16);lastFailure.setPadding(0,dp(16),0,dp(16));body.addView(lastFailure);
        TextView installNote=new TextView(this);installNote.setText(text(R.string.gboard_patches_shared_model_install_note));installNote.setTextSize(16);installNote.setPadding(0,dp(16),0,dp(12));body.addView(installNote);
        download=new Button(this);download.setText(text(R.string.gboard_patches_shared_model_download));download.setOnClickListener(v->downloadRelease());body.addView(download);
        Button install=new Button(this);install.setText(text(R.string.gboard_patches_shared_model_choose_apk));install.setOnClickListener(v->{
            try{releaseIdentity();startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/vnd.android.package-archive").addCategory(Intent.CATEGORY_OPENABLE),PICK_APK);}catch(Exception error){status.setText(text(R.string.gboard_patches_shared_model_release_unavailable));}
        });body.addView(install);
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        scroll.setOnApplyWindowInsetsListener((view,insets)->{body.setPadding(dp(20),Math.max(dp(20),insets.getSystemWindowInsetTop()),dp(20),Math.max(dp(24),insets.getSystemWindowInsetBottom()));return insets;});
        setContentView(scroll);scroll.requestApplyInsets();refresh();
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override protected void onResume(){super.onResume();SharedModelClient.reconnect(this);resumed=true;main.removeCallbacks(refreshLoop);main.post(refreshLoop);}
    @Override protected void onPause(){resumed=false;main.removeCallbacks(refreshLoop);super.onPause();}
    private void refresh(){
        boolean installed=ProviderDiscovery.inspect(this).installed;
        boolean selected=SharedLocalAiState.selected(dev.jason.gboardpatches.extension.writingtools.GboardAiWritingToolsSettings.preferences(this));
        boolean ready=installed && SharedModelClient.ready(this);
        selection.setText(selected?"ON — Local AI selected for Gboard":"OFF — Local AI not selected for Gboard");
        boolean dark=(getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
        selection.setTextColor(selected?(dark?0xff7bd89c:0xff146c2e):(dark?0xffe0e3e7:0xff343940));
        useLocal.setText(selected?"Turn Local AI OFF for Gboard":"Turn Local AI ON for Gboard");
        useLocal.setEnabled(selected || ready);
        setup.setEnabled(installed);
        if(installed){
            try{SharedProviderIdentity.verify(this);}catch(HostException error){useLocal.setEnabled(selected);showingError=true;status.setText(text(R.string.gboard_patches_shared_model_untrusted));return;}
            status.setText(SharedModelClient.readinessMessage(this));
        }else status.setText(text(R.string.gboard_patches_shared_model_missing));
        String failure=SharedModelClient.lastRequestError();
        lastFailure.setVisibility(failure.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);
        if(!failure.isEmpty())lastFailure.setText("Last action: "+SharedLocalAiState.failureMessage(failure));
        try{download.setEnabled(releaseIdentity().download!=null);}catch(Exception error){download.setEnabled(false);}
    }
    private void openProvider(){showingError=false;try{startActivity(new Intent().setComponent(new ComponentName(ModelHostClient.DEFAULT_PACKAGE,"app.johninnl.modelhost.setup.HostSettingsActivity")));}catch(ActivityNotFoundException e){status.setText(text(R.string.gboard_patches_shared_model_missing));}}
    private SharedProviderRelease releaseIdentity()throws Exception{
        try(InputStream input=getAssets().open("morphe/model-host-release.json")){
            ByteArrayOutputStream data=new ByteArrayOutputStream();byte[] buffer=new byte[2048];int count;
            while((count=input.read(buffer))!=-1){if(data.size()+count>8192)throw new IOException("Invalid release identity");data.write(buffer,0,count);}
            return new SharedProviderRelease(new JSONObject(data.toString("UTF-8")));
        }
    }
    private VerifiedApk verify(Uri uri,SharedProviderRelease release)throws Exception{
        VerifiedApk verified=VerifiedApk.verify(this,uri,release.packageName,release.apkSha256,release.signerSha256);
        if(verified.versionCode!=release.versionCode)throw new IOException("APK version mismatch");return verified;
    }
    private void downloadRelease(){
        if(busy)return;showingError=false;
        final SharedProviderRelease release;try{release=releaseIdentity();if(release.download==null)throw new IOException();}
        catch(Exception error){status.setText(text(R.string.gboard_patches_shared_model_release_unavailable));return;}
        busy=true;download.setEnabled(false);useLocal.setEnabled(false);status.setText(text(R.string.gboard_patches_shared_model_downloading));
        downloadJob=worker.submit(()->{
            File copy=null;
            try{
                copy=File.createTempFile("modelhost-release-",".apk",getCacheDir());
                release.downloadTo(copy,(done,total)->main.post(()->{if(!isDestroyed())status.setText(text(R.string.gboard_patches_shared_model_downloading)+(total>0?" "+(done*100/total)+"%":""));}));
                VerifiedApk verified=verify(Uri.fromFile(copy),release);
                main.post(()->{busy=false;if(isDestroyed())return;pending=verified;continueInstallation();});
            }catch(Exception error){main.post(()->{busy=false;if(!isDestroyed()){download.setEnabled(true);showingError=true;status.setText(text(R.string.gboard_patches_shared_model_download_failed));}});}
            finally{if(copy!=null)copy.delete();}
        });
    }
    private void enableLocal(){
        android.content.SharedPreferences preferences=dev.jason.gboardpatches.extension.writingtools.GboardAiWritingToolsSettings.preferences(this);
        boolean selected=SharedLocalAiState.selected(preferences);
        if(!selected && !SharedModelClient.ready(this)){refresh();return;}
        if(SharedLocalAiState.select(preferences,!selected)){
            if(selected)SharedModelClient.releaseSession();else SharedModelClient.activate(this);
            showingError=false;refresh();
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_APK && result==RESULT_OK && data!=null && data.getData()!=null){
            showingError=false;Uri uri=data.getData();status.setText(text(R.string.gboard_patches_shared_model_verifying));
            busy=true;worker.execute(()->{try{VerifiedApk verified=verify(uri,releaseIdentity());runOnUiThread(()->{busy=false;if(isDestroyed())return;pending=verified;continueInstallation();});}catch(Exception failure){runOnUiThread(()->{busy=false;if(!isDestroyed()){showingError=true;status.setText(text(R.string.gboard_patches_shared_model_verification_failed));}});}});
        }else if(request==APPROVE_SOURCE && pending!=null){
            if(getPackageManager().canRequestPackageInstalls())continueInstallation();else status.setText(text(R.string.gboard_patches_shared_model_source_required));
        }else if(request==INSTALL_APK){pending=null;refresh();}
    }
    private void continueInstallation(){
        if(pending==null)return;
        if(!getPackageManager().canRequestPackageInstalls()){startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())),APPROVE_SOURCE);return;}
        try{startActivityForResult(ProviderInstallIntent.create(this,pending),INSTALL_APK);}catch(RuntimeException error){status.setText(text(R.string.gboard_patches_shared_model_install_failed));}
    }
    @Override protected void onDestroy(){main.removeCallbacks(refreshLoop);if(downloadJob!=null)downloadJob.cancel(true);worker.shutdownNow();super.onDestroy();}
}
