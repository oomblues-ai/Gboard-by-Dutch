package dev.jason.gboardpatches.extension.sharedmodel;
import android.content.Context;
import dev.jason.gboardpatches.extension.R;
import dev.jason.gboardpatches.extension.settings.*;
import java.util.Collections;
/** Setup remains reachable before a backend has been selected or a provider installed. */
public final class SharedLocalAiSettingsFeature implements GboardPatchesSettingsContract.Feature {
    private final String title,summary,badge;
    private final Context context;
    public SharedLocalAiSettingsFeature(Context context){this.context=context;title=GboardSettingsText.get(context,R.string.gboard_patches_shared_model_entry);summary=GboardSettingsText.get(context,R.string.gboard_patches_shared_model_entry_summary);badge=GboardSettingsText.get(context,R.string.gboard_patches_header_badge);}
    @Override public String getEntryTitle(){return title;}
    @Override public String getEntrySummary(){return GboardSharedWritingRuntime.selected(context)?"ON for Gboard · Open to check model readiness":"OFF for Gboard · Set up and select the local model";}
    @Override public boolean isAvailable(Context context){return GboardPatchesFeatureAvailability.hasFeature(context,GboardPatchesFeatureAvailability.FEATURE_SHARED_LOCAL_MODEL);}
    @Override public GboardPatchesSettingsContract.Screen buildScreen(GboardPatchesSettingsContract.FeatureHost host){
        GboardPatchesSettingsContract.ActionRow action=new GboardPatchesSettingsContract.ActionRow("Manage Local AI",getEntrySummary(),true,()->SharedModelSetupActivity.open(host.getContext()));
        return new GboardPatchesSettingsContract.Screen(title,badge,title,summary,Collections.emptyList(),Collections.singletonList(new GboardPatchesSettingsContract.Section(title,Collections.singletonList(action))));
    }
}
