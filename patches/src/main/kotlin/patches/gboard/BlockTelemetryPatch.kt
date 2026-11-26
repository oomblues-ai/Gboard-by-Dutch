package patches.gboard

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import patches.gboard.Constants.COMPATIBILITY_GBOARD
import patches.gboard.shared.GboardFieldTarget
import patches.gboard.shared.GboardFlagFamilyFeature
import patches.gboard.shared.GboardMethodTarget
import patches.gboard.shared.childElements
import patches.gboard.shared.ensureManifestMetaData
import patches.gboard.shared.gboardFlagFamilyFeaturePatch
import patches.gboard.shared.isMethodReference
import patches.gboard.shared.manifestAndroidAttribute
import patches.gboard.shared.runtimeabi.RuntimeCallEmitter
import patches.gboard.shared.runtimeabi.RuntimeCallId
import org.w3c.dom.Document

internal const val GBOARD_OFFLINE_TELEMETRY_MARKER = "dev.jason.gboardpatches.feature.block_telemetry"

private fun removeNetworkPermissions(document: Document) {
    val manifest = document.documentElement
    check(manifest.manifestAndroidAttribute("sharedUserId") == null) {
        "Gboard telemetry isolation requires its own Android UID"
    }
    manifest.childElements().filter {
        it.tagName in setOf("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m") &&
            it.manifestAndroidAttribute("name") in setOf("android.permission.INTERNET", "android.permission.ACCESS_LOCAL_NETWORK")
    }.toList().forEach(manifest::removeChild)
}

private val gboardOfflineManifestPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { doc ->
            val application = doc.documentElement.childElements("application").single()
            ensureManifestMetaData(doc, application, GBOARD_OFFLINE_TELEMETRY_MARKER, "true")
            removeNetworkPermissions(doc)
        }
    }
    finalize {
        document("AndroidManifest.xml").use(::removeNetworkPermissions)
    }
}

private val gboardOfflineFlagsPatch = gboardFlagFamilyFeaturePatch(
    description = "Keep the writing and Rambler capability flags offline under the opt-in privacy patch.",
    feature = GboardFlagFamilyFeature.OFFLINE_TELEMETRY,
)

@Suppress("unused")
val gboardBlockTelemetryPatch = bytecodePatch(
    name = "Block Gboard telemetry",
    description = "Removes Gboard network access and blocks its Play services telemetry submissions. " +
        "Use offline typing or the separate Local Model Host. Gboard network and LAN features are unavailable.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardOfflineManifestPatch, gboardOfflineFlagsPatch)

    execute {
        // Complete Clearcut's pending event locally, using its existing sampled-out result.
        val result = "Lcom/google/android/gms/common/api/internal/BasePendingResult;"
        val status = "Lcom/google/android/gms/common/api/Status;"
        val clearcut = GboardMethodTarget("Lxkg;", "b", listOf("Lxmr;"), "V").resolve(this)
        val pendingResult = "${result}->j(Lxny;)V"
        check(clearcut.implementation!!.instructions.any { it.isMethodReference(pendingResult) }) {
            "Gboard Clearcut pending-result completion changed"
        }
        clearcut.addInstructions(0, """
            sget-object v0, ${status}->a:$status
            move-object/from16 v1, p0
            invoke-virtual {v1, v0}, $pendingResult
            return-void
        """.trimIndent())

        val proxy = "Lhrd;"
        GboardFieldTarget(proxy, "b", "Ljava/lang/String;").resolve(this)
        val parameters = listOf("I", "Landroid/os/Parcel;")
        for ((name, returnType) in listOf("J" to "Landroid/os/Parcel;", "K" to "V", "L" to "V")) {
            val method = GboardMethodTarget(proxy, name, parameters, returnType).resolve(this)
            check(method.implementation!!.registerCount in 4..16) {
                "Gboard Play services proxy register layout changed for $name"
            }
            method.addInstructions(0, """
                iget-object v0, p0, Lhrd;->b:Ljava/lang/String;
                ${RuntimeCallEmitter.invoke(RuntimeCallId.TELEMETRY_CHECK_TRANSACTION, "v0, p2")}
            """.trimIndent())
        }
    }
}
