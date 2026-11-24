package dev.jason.gboardpatches.extension.sharedmodel;
import android.content.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import dev.jason.gboardpatches.extension.settings.GboardPatchesSettingsContract;
import dev.jason.gboardpatches.extension.writingtools.GboardAiWritingToolsSettings;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=34,manifest=Config.NONE)
public class SharedLocalAiSettingsFeatureTest {
 private Context context;private GboardPatchesSettingsContract.FeatureHost host;
 @Before public void prepare(){context=RuntimeEnvironment.getApplication();GboardAiWritingToolsSettings.preferences(context).edit().clear().commit();host=new GboardPatchesSettingsContract.FeatureHost(){public Context getContext(){return context;}public void submit(GboardPatchesSettingsContract.Intent intent){}};}
 @Test public void localAiUsesTheExistingSettingsToggleAndStatusRenderer(){SharedLocalAiSettingsFeature feature=new SharedLocalAiSettingsFeature(context);GboardPatchesSettingsContract.Screen screen=feature.buildScreen(host);boolean toggle=false;for(GboardPatchesSettingsContract.Section section:screen.getSections())for(GboardPatchesSettingsContract.Row row:section.getItems())if(row instanceof GboardPatchesSettingsContract.ToggleRow)toggle=true;assertTrue(toggle);assertFalse(screen.getStatusBlocks().isEmpty());}
 @Test public void aSelectedLocalBackendCanBeTurnedOffEvenWithTheProviderMissing(){SharedLocalAiState.select(GboardAiWritingToolsSettings.preferences(context),true);GboardPatchesSettingsContract.Screen screen=new SharedLocalAiSettingsFeature(context).buildScreen(host);for(GboardPatchesSettingsContract.Section section:screen.getSections())for(GboardPatchesSettingsContract.Row row:section.getItems())if(row instanceof GboardPatchesSettingsContract.ToggleRow){GboardPatchesSettingsContract.ToggleRow toggle=(GboardPatchesSettingsContract.ToggleRow)row;assertTrue(toggle.isEnabled());toggle.getToggleAction().accept(false);}assertFalse(SharedLocalAiState.selected(GboardAiWritingToolsSettings.preferences(context)));}
 @Test public void translationPreferencesSurviveOtherWritingPreferenceUpdates()throws Exception{SharedWritingPreferences.write(context,"Style","Message","Instruction",false,false);SharedWritingPreferences.writeLanguage(context,"fr");SharedWritingPreferences.write(context,"Updated style","Message","Instruction",true,false);assertEquals("fr",SharedWritingPreferences.language(context));assertEquals("Updated style",SharedWritingPreferences.read(context,"style"));}
}
