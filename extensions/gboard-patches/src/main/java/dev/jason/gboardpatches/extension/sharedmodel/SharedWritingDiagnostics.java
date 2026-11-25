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
    public static void clientEvent(String phase,String id,String operation,String code,int outputChars){
        if(!enabled())return;
        String client=id!=null && id.matches("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")?id:"unassigned";
        String task=java.util.Arrays.asList("generate","proofread","rephrase","professional","friendly","emojify","elaborate","shorten","translate","summarize","reply","complete","edit","grammar","suggest_instructions","my_style","ocr","describe_image").contains(operation)?operation:"unknown";
        String stage="submit".equals(phase)?"submit":"terminal";
        event("client.request phase="+stage+" client="+client+" operation="+task+" code="+SharedLocalAiState.safeCode(code)+" outputChars="+Math.max(0,Math.min(outputChars,1000000)));
    }
}
