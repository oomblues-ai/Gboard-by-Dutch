package dev.jason.gboardpatches.extension.privacy;

import android.content.Context;
import android.os.Parcel;
import android.os.RemoteException;
import dev.jason.gboardpatches.extension.settings.GboardPatchesFeatureAvailability;

/** The opt-in patch installs this gate at the stock Play services proxy's send methods. */
public final class GboardTelemetryPolicy {
    public static final String FEATURE = "dev.jason.gboardpatches.feature.block_telemetry";

    private GboardTelemetryPolicy() {}

    public static boolean isEnabled(Context context) {
        return GboardPatchesFeatureAvailability.hasFeature(context, FEATURE);
    }

    public static void checkTransaction(String descriptor, Parcel request) throws RemoteException {
        if (!blocks(descriptor)) return;
        if (request != null) request.recycle();
        throw new RemoteException("Gboard telemetry is disabled");
    }

    static boolean blocks(String descriptor) {
        if (descriptor == null) return false;
        switch (descriptor) {
            case "com.google.android.gms.clearcut.internal.IClearcutLoggerService":
            case "com.google.android.gms.common.internal.service.IClientTelemetryService":
            case "com.google.android.gms.common.internal.service.IClientNotificationTelemetryService":
            case "com.google.android.gms.common.internal.service.IClientThrottlingTelemetryService":
            case "com.google.android.gms.usagereporting.internal.IUsageReportingService":
            case "com.google.android.gms.feedback.internal.IFeedbackService":
                return true;
            default:
                return false;
        }
    }
}
