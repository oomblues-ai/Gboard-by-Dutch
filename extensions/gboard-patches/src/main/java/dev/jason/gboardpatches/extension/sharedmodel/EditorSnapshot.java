package dev.jason.gboardpatches.extension.sharedmodel;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.view.inputmethod.*;
import java.lang.ref.WeakReference;
import java.util.Objects;

/** Captured and rechecked on the IME thread. Contains only this editor's bounded text; never logged or persisted. */
public final class EditorSnapshot {
    private final WeakReference<InputMethodService> service;
    private final InputConnection connection;
    private final EditorInfo editor;
    private final String text;
    private final int start,end,offset,composingStart,composingEnd;
    private final long revision;
    private EditorSnapshot(InputMethodService ime,InputConnection connection,EditorInfo info,ExtractedText extracted,long revision){service=new WeakReference<>(ime);this.connection=connection;editor=info;text=extracted.text.toString();start=extracted.selectionStart;end=extracted.selectionEnd;offset=extracted.startOffset;composingStart=composing(extracted.text,true);composingEnd=composing(extracted.text,false);this.revision=revision;}
    public static EditorSnapshot capture(InputMethodService ime,long revision) {
        if(ime==null)return null;EditorInfo info=ime.getCurrentInputEditorInfo();InputConnection c=ime.getCurrentInputConnection();
        if(c==null || !allowed(info))return null;
        ExtractedTextRequest request=new ExtractedTextRequest();request.flags=InputConnection.GET_TEXT_WITH_STYLES;request.hintMaxChars=32768;request.hintMaxLines=200;
        ExtractedText e=c.getExtractedText(request,0);
        if(e==null || e.text==null || e.text.length()>32768)return null;
        return new EditorSnapshot(ime,c,info,e,revision);
    }
    public boolean isCurrent(long revision){
        InputMethodService ime=service.get();if(ime==null || revision!=this.revision || ime.getCurrentInputConnection()!=connection || ime.getCurrentInputEditorInfo()!=editor)return false;
        ExtractedTextRequest request=new ExtractedTextRequest();request.flags=InputConnection.GET_TEXT_WITH_STYLES;request.hintMaxChars=32768;request.hintMaxLines=200;ExtractedText e=connection.getExtractedText(request,0);
        return e!=null && e.text!=null && start==e.selectionStart && end==e.selectionEnd && offset==e.startOffset && composingStart==composing(e.text,true) && composingEnd==composing(e.text,false) && text.contentEquals(e.text);
    }
    public boolean isEmpty(){return text.trim().isEmpty();}
    public Handoff handoff(){return new Handoff(this);}
    /** A matching resource ID or empty draft cannot identify a conversation. Require the same live editor. */
    public static final class Handoff {
        private final EditorSnapshot original;private final String packageName,fieldName;private final int fieldId,inputType,imeOptions;
        private Handoff(EditorSnapshot snapshot){original=snapshot;EditorInfo info=snapshot.editor;packageName=info.packageName;fieldName=info.fieldName;fieldId=info.fieldId;inputType=info.inputType;imeOptions=info.imeOptions;}
        public EditorSnapshot resume(){return resume(GboardSharedWritingRuntime.currentService(),GboardSharedWritingRuntime.currentRevision());}
        EditorSnapshot resume(InputMethodService ime,long revision){
            if(ime==null || original.service.get()!=ime || ime.getCurrentInputConnection()!=original.connection || ime.getCurrentInputEditorInfo()!=original.editor || !allowed(ime.getCurrentInputEditorInfo()))return null;
            EditorInfo info=ime.getCurrentInputEditorInfo();if(!Objects.equals(packageName,info.packageName)||!Objects.equals(fieldName,info.fieldName)||fieldId!=info.fieldId||inputType!=info.inputType||imeOptions!=info.imeOptions)return null;
            EditorSnapshot now=capture(ime,revision);
            return now!=null && original.start==now.start && original.end==now.end && original.offset==now.offset && original.composingStart==now.composingStart && original.composingEnd==now.composingEnd && original.text.equals(now.text)?now:null;
        }
    }
    public String completionPrefix(){return start==end && end==text.length() && offset==0?text:null;}
    public boolean appendSuffix(String suffix,long currentRevision){
        if(suffix==null || !isCurrent(currentRevision))return false;
        connection.beginBatchEdit();try{if(!connection.finishComposingText())return false;return connection.commitText(suffix,1);}finally{connection.endBatchEdit();}
    }
    private static int composing(CharSequence text,boolean start){if(!(text instanceof android.text.Spannable))return -1;return start?BaseInputConnection.getComposingSpanStart((android.text.Spannable)text):BaseInputConnection.getComposingSpanEnd((android.text.Spannable)text);}
    public static boolean allowed(EditorInfo info){return info!=null && (info.imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)==0 && !privateInput(info.inputType) && !dev.jason.gboardpatches.extension.manualincognito.GboardManualIncognitoRuntime.applyIncognitoPredicate(false);}
    private static boolean privateInput(int type){int cls=type&InputType.TYPE_MASK_CLASS;int variation=type&InputType.TYPE_MASK_VARIATION;return cls==InputType.TYPE_CLASS_TEXT && (variation==InputType.TYPE_TEXT_VARIATION_PASSWORD || variation==InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD || variation==InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) || cls==InputType.TYPE_CLASS_NUMBER && variation==InputType.TYPE_NUMBER_VARIATION_PASSWORD;}
}
