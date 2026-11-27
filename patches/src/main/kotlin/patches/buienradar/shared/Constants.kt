package patches.buienradar.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal object Constants {
    private fun compatibility(vararg targets: AppTarget) = Compatibility(
        name = "Buienradar",
        packageName = "com.supportware.Buienradar",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x243C88,
        // Rotated signing lineage: the API 24-32 signer and the API 33+ signer.
        signatures = setOf(
            "4392d8f67eedd4d2c52370e69911df3ad465d6d6f7d7176cd5f2b4bdefcca9cc",
            "80d91716e88298892a4d34076ae3562ac9e2212e08048d09dbfeca2c26a6ed92",
        ),
        targets = targets.toList(),
    )

    val COMPATIBILITY_BUIENRADAR = compatibility(
        AppTarget(version = "8.1.0", versionCode = 7989, minSdk = 28),
        AppTarget(version = "7.4.8", versionCode = 7915, minSdk = 28),
    )

    // Morphe allows one catalog target per version name. Keep the earlier signed build in preflight.
    val originalInput = patches.shared.originalInput(
        COMPATIBILITY_BUIENRADAR,
        variants = listOf(compatibility(AppTarget(version = "7.4.8", versionCode = 7897, minSdk = 28))),
        additionalMinSdksByVersion = mapOf("7.4.8" to setOf(29, 32)),
    )
}
