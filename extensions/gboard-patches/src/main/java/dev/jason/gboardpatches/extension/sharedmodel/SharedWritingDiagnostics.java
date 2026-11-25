package dev.jason.gboardpatches.extension.sharedmodel;

import android.util.Log;

/** Default-off lifecycle diagnostics; never accepts draft text, prompts, images or credentials. */
public final class SharedWritingDiagnostics {
    private static final String TAG="GboardLocalAi";
    private SharedWritingDiagnostics(){}
    public static boolean enabled(){try{return Log.isLoggable(TAG,Log.DEBUG);}catch(Throwable ignored){return false;}}
    public static void event(String state){
        try{if(Log.isLoggable(TAG,Log.DEBUG))Log.d(TAG,state);}catch(Throwable ignored){/* Observation must not change keyboard behavior. */}
    }
}
