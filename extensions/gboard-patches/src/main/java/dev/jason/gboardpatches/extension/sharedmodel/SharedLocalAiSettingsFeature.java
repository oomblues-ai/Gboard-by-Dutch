package dev.jason.gboardpatches.extension.sharedmodel;

import android.content.*;
import app.johninnl.modelhost.api.*;
import dev.jason.gboardpatches.extension.R;
import dev.jason.gboardpatches.extension.settings.*;
import dev.jason.gboardpatches.extension.writingtools.GboardAiWritingToolsSettings;
import java.util.*;

/** Local AI uses the same settings renderer, navigation, controls and dialogs as the rest of Patches. */
public final class SharedLocalAiSettingsFeature implements GboardPatchesSettingsContract.Feature {
    private final Context context;
    private final String title,badge;
    private final WritingOptions writing;
    private final Advanced advanced;
    public SharedLocalAiSettingsFeature(Context context){
        this.context=context.getApplicationContext();
        title=GboardSettingsText.get(context,R.string.gboard_patches_shared_model_entry);
        badge=GboardSettingsText.get(context,R.string.gboard_patches_header_badge);
        writing=new WritingOptions(this.context,badge);advanced=new Advanced(this.context,badge);
    }
    @Override public String getEntryTitle(){return title;}
    @Override public String getEntrySummary(){return GboardSharedWritingRuntime.selected(context)?"On · Writing tools use the local model":"Off · Set up local writing tools";}
    @Override public boolean isAvailable(Context context){return GboardPatchesFeatureAvailability.hasFeature(context,GboardPatchesFeatureAvailability.FEATURE_SHARED_LOCAL_MODEL);}
    @Override public List<GboardPatchesSettingsContract.Feature> getNavigationChildren(){return Arrays.asList(writing,advanced);}
    @Override public void onVisible(GboardPatchesSettingsContract.FeatureHost host){SharedModelClient.reconnect(host.getContext());}
    @Override public GboardPatchesSettingsContract.Screen buildScreen(GboardPatchesSettingsContract.FeatureHost host){
        Context c=host.getContext();boolean installed=ProviderDiscovery.inspect(c).installed;
        boolean selected=GboardSharedWritingRuntime.selected(c),ready=installed && SharedModelClient.ready(c);
        List<GboardPatchesSettingsContract.StatusBlock> status=new ArrayList<>();
        status.add(new GboardPatchesSettingsContract.StatusBlock(ready?"Model ready":"Model status",installed?SharedModelClient.readinessMessage(c):"Install Local Model Host to use Gemma on this device.",ready?GboardPatchesSettingsContract.StatusTone.INFO:GboardPatchesSettingsContract.StatusTone.WARNING));
        if(!SharedProviderSetup.message().isEmpty())status.add(new GboardPatchesSettingsContract.StatusBlock("Setup",SharedProviderSetup.message()));
        String failure=SharedModelClient.lastRequestError();
        if(!failure.isEmpty())status.add(new GboardPatchesSettingsContract.StatusBlock("Last action",SharedLocalAiState.failureMessage(failure),GboardPatchesSettingsContract.StatusTone.WARNING));
        List<GboardPatchesSettingsContract.Row> rows=new ArrayList<>();
        rows.add(new GboardPatchesSettingsContract.ToggleRow("Use Local AI",selected?"On · Writing tools use Gemma on this device.":"Off · Turn on when the model is ready.",selected||ready,selected,value->{
            if(value&&!SharedModelClient.ready(c)){host.submit(adapter->adapter.showMessage("Wait until the model is ready."));GboardPatchesSettingsContract.refresh(host);return;}
            boolean saved=SharedLocalAiState.select(GboardAiWritingToolsSettings.preferences(c),value);
            if(saved){if(value)SharedModelClient.activate(c);else SharedModelClient.releaseSession();}
            if(!saved)host.submit(adapter->adapter.showMessage("Could not save the Local AI setting."));
            GboardPatchesSettingsContract.refresh(host);
        }));
        if(!installed || SharedProviderSetup.needsUpdate(c) || SharedProviderSetup.pending()!=null)rows.add(new GboardPatchesSettingsContract.ActionRow(SharedProviderSetup.pending()!=null?"Install companion app":installed?"Update Local Model Host":"Get Local Model Host","Download and verify the companion app, then confirm installation in Android.",!SharedProviderSetup.busy(),()->SharedProviderSetup.download(c)));
        if(SharedProviderSetup.busy())rows.add(new GboardPatchesSettingsContract.ActionRow("Cancel download","Keep your current setup.",true,SharedProviderSetup::cancel));
        rows.add(new GboardPatchesSettingsContract.ActionRow("Model setup","Download the model, start or stop it, and choose image support.",installed,()->openProvider(c)));
        rows.add(new GboardPatchesSettingsContract.ActionRow("Writing preferences","Your style, reply context, saved instructions and experimental typing features.",true,()->GboardPatchesSettingsContract.openFeature(host,writing)));
        rows.add(new GboardPatchesSettingsContract.ActionRow("Advanced","Manual installation and technical status.",true,()->GboardPatchesSettingsContract.openFeature(host,advanced)));
        return new GboardPatchesSettingsContract.Screen(title,badge,title,"Writing help that stays on your device.",status,Collections.singletonList(new GboardPatchesSettingsContract.Section("Local AI",rows)),GboardPatchesSettingsContract.RefreshPolicy.fullScreen(1500),GboardPatchesSettingsContract.PanelStyle.FLAT);
    }
    static void openProvider(Context context){
        try{context.startActivity(new Intent().setComponent(new ComponentName(ModelHostClient.DEFAULT_PACKAGE,"app.johninnl.modelhost.setup.HostSettingsActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        catch(ActivityNotFoundException missing){SharedProviderSetup.finished("Install Local Model Host first.");}
    }
    public static final class WritingOptions implements GboardPatchesSettingsContract.Feature {
        private final Context context;private final String badge;private String error="";
        WritingOptions(Context context,String badge){this.context=context;this.badge=badge;}
        public String getEntryTitle(){return "Writing preferences";}
        public String getEntrySummary(){return "Your style, context and typing features";}
        public GboardPatchesSettingsContract.Screen buildScreen(GboardPatchesSettingsContract.FeatureHost host){
            List<GboardPatchesSettingsContract.Row> rows=new ArrayList<>();
            textRow(host,rows,"Your style","style","Add your own examples and preferences.");
            textRow(host,rows,"Message to reply to","reply","Only the message you enter here is used for suggested replies.");
            textRow(host,rows,"Saved instruction","instruction","Choose how Local AI should edit your draft.");
            String[] languages={"English","French","German","Spanish","Italian","Portuguese","Dutch"},codes={"en","fr","de","es","it","pt","nl"};
            String selected=SharedWritingPreferences.language(context);int index=java.util.Arrays.asList(codes).indexOf(selected);
            rows.add(new GboardPatchesSettingsContract.SelectorRow("Translation language","Used by the AI Translate keyboard shortcut.",languages[Math.max(0,index)],true,()->GboardPatchesSettingsContract.showChoiceDialog(host,"Translate to",languages,codes,selected,"__unused__",()->{},value->{try{SharedWritingPreferences.writeLanguage(context,value);error="";}catch(Exception invalid){error="Could not save the translation language.";}GboardPatchesSettingsContract.refresh(host);})));

            rows.add(new GboardPatchesSettingsContract.ToggleRow("Smart Compose","Experimental · Offer a short continuation while typing.",true,"true".equals(SharedWritingPreferences.read(context,"compose")),value->save(host,"compose",Boolean.toString(value))));
            rows.add(new GboardPatchesSettingsContract.ToggleRow("Inline grammar","Experimental · Suggest corrections without replacing your draft.",true,"true".equals(SharedWritingPreferences.read(context,"grammar")),value->save(host,"grammar",Boolean.toString(value))));
            return new GboardPatchesSettingsContract.Screen(getEntryTitle(),badge,getEntryTitle(),"Examples and context stay in Gboard’s private storage. Incoming conversations are not collected.",error.isEmpty()?Collections.emptyList():Collections.singletonList(new GboardPatchesSettingsContract.StatusBlock("Could not save",error,GboardPatchesSettingsContract.StatusTone.WARNING)),Collections.singletonList(new GboardPatchesSettingsContract.Section("Writing",rows)),GboardPatchesSettingsContract.RefreshPolicy.none(),GboardPatchesSettingsContract.PanelStyle.FLAT);
        }
        private void textRow(GboardPatchesSettingsContract.FeatureHost host,List<GboardPatchesSettingsContract.Row> rows,String title,String key,String description){
            boolean present=!SharedWritingPreferences.read(context,key).isEmpty();
            rows.add(new GboardPatchesSettingsContract.ActionRow(title,(present?"Saved · ":"Not set · ")+description,true,()->GboardPatchesSettingsContract.showTextInputDialog(host,title,description,SharedWritingPreferences.read(context,key),value->save(host,key,value))));
        }
        private void save(GboardPatchesSettingsContract.FeatureHost host,String key,String value){error=write(context,key,value)?"":"That entry is too long. Shorten it and try again.";GboardPatchesSettingsContract.refresh(host);}
        private static boolean write(Context c,String key,String value){
            try{
                String style="style".equals(key)?value:SharedWritingPreferences.read(c,"style"),reply="reply".equals(key)?value:SharedWritingPreferences.read(c,"reply"),instruction="instruction".equals(key)?value:SharedWritingPreferences.read(c,"instruction");
                boolean compose="true".equals("compose".equals(key)?value:SharedWritingPreferences.read(c,"compose")),grammar="true".equals("grammar".equals(key)?value:SharedWritingPreferences.read(c,"grammar"));
                SharedWritingPreferences.write(c,style,reply,instruction,compose,grammar);SharedInlineSettings.updated(compose,grammar);return true;
            }catch(Exception tooLong){return false;}
        }
    }
    public static final class Advanced implements GboardPatchesSettingsContract.Feature {
        private final Context context;private final String badge;
        Advanced(Context context,String badge){this.context=context;this.badge=badge;}
        public String getEntryTitle(){return "Advanced";}
        public String getEntrySummary(){return "Installation and technical status";}
        public GboardPatchesSettingsContract.Screen buildScreen(GboardPatchesSettingsContract.FeatureHost host){
            List<GboardPatchesSettingsContract.Row> rows=new ArrayList<>();
            rows.add(new GboardPatchesSettingsContract.ActionRow("Choose companion APK","Only the matching signed APK will be accepted.",!SharedProviderSetup.busy(),()->SharedModelSetupActivity.choose(context)));
            rows.add(new GboardPatchesSettingsContract.ActionRow("Download companion app again","Verify the current review and open Android’s installer.",!SharedProviderSetup.busy(),()->SharedProviderSetup.download(context)));
            rows.add(new GboardPatchesSettingsContract.DetailRow("Provider state",SharedModelClient.status(context),true,true));
            if(!SharedModelClient.lastRequestError().isEmpty())rows.add(new GboardPatchesSettingsContract.DetailRow("Last error code",SharedModelClient.lastRequestError(),true,true));
            return new GboardPatchesSettingsContract.Screen(getEntryTitle(),badge,getEntryTitle(),"Details for installation and troubleshooting.",Collections.emptyList(),Collections.singletonList(new GboardPatchesSettingsContract.Section("Advanced",rows)),GboardPatchesSettingsContract.RefreshPolicy.none(),GboardPatchesSettingsContract.PanelStyle.FLAT);
        }
    }
}
