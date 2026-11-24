package patches.gboard.features.sharedmodel
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.smali.ExternalLabel
import patches.gboard.Constants.COMPATIBILITY_GBOARD
import patches.gboard.shared.*
import patches.gboard.shared.runtimeabi.*

internal val gboardSharedGrammarPatch=bytecodePatch(description="Route optional inline grammar through the shared model's native grammar-result adapter."){
 compatibleWith(COMPATIBILITY_GBOARD)
 dependsOn(gboardSharedModelPatch,patches.gboard.features.featureflags.gboardGrammarCheckerFlagValuePatch)
 execute{
  listOf(GboardMethodTarget("Luum;","a",listOf("Lakpu;"),"Z"),GboardMethodTarget("Luum;","b",listOf("Ljava/lang/CharSequence;","Z"),"Lauaz;"),GboardMethodTarget("Luum;","close",emptyList(),"V"),GboardMethodTarget("Luuk;","<init>",emptyList(),"V"),GboardMethodTarget("Luuk;","a",emptyList(),"Luul;"),GboardMethodTarget("Lauaz;","n",listOf("Ljava/util/Collection;"),"Lauaz;")).forEach{it.resolve(this)}
  listOf(GboardFieldTarget("Luuj;","l","Landroid/content/Context;"),GboardFieldTarget("Luuk;","a","I"),GboardFieldTarget("Luuk;","b","I"),GboardFieldTarget("Luuk;","c","Ljava/util/List;")).forEach{it.resolve(this)}
  val factory=GboardMethodTarget("Luuj;","b",listOf("Lakpu;"),"Luum;").resolve(this)
  check(factory.implementation!!.registerCount>2)
  factory.addInstructionsWithLabels(0,"""
   iget-object v0, p0, Luuj;->l:Landroid/content/Context;
   ${RuntimeCallEmitter.invoke(RuntimeCallId.SHARED_GRAMMAR_CREATE,"v0")}
   move-result-object v0
   if-eqz v0, :original
   return-object v0
  """,ExternalLabel("original",factory.implementation!!.instructions.first()))
 }
}
