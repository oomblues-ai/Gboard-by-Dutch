package dev.jason.gboardpatches.extension.sharedmodel;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.media.ExifInterface;
import android.os.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Private, transparent handoff to the normal Android image picker; it adds no settings screen. */
public final class SharedImageInputActivity extends Activity {
    private static final int PICK=7411;
    private static Pending pending;
    private static final class Pending {final String ticket=java.util.UUID.randomUUID().toString();final Consumer<Bitmap> callback;final WeakReference<android.inputmethodservice.InputMethodService> ime;Pending(Consumer<Bitmap> c){callback=c;ime=new WeakReference<>(GboardSharedWritingRuntime.currentService());}}
    static void choose(Context context,Consumer<Bitmap> callback){
        if(pending!=null){callback.accept(null);return;}
        Pending request=new Pending(callback);pending=request;
        try{context.startActivity(new Intent(context,SharedImageInputActivity.class).putExtra("ticket",request.ticket).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        catch(RuntimeException unavailable){if(pending==request)pending=null;callback.accept(null);return;}
        new Handler(Looper.getMainLooper()).postDelayed(()->{if(pending==request){pending=null;request.callback.accept(null);}},120000);
    }
    @Override public void onCreate(Bundle state){setTheme(android.R.style.Theme_Translucent_NoTitleBar);super.onCreate(state);if(pending==null||!pending.ticket.equals(getIntent().getStringExtra("ticket"))){finish();return;}if(state==null)try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),PICK);}catch(RuntimeException unavailable){complete(null);}}
    @Override protected void onActivityResult(int request,int code,Intent data){super.onActivityResult(request,code,data);if(request!=PICK)return;if(code!=RESULT_OK||data==null||data.getData()==null){complete(null);return;}android.net.Uri uri=data.getData();ExecutorService worker=Executors.newSingleThreadExecutor();worker.execute(()->{Bitmap bitmap=null;try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>16*1024*1024)throw new IOException();bytes.write(buffer,0,n);}}byte[] encoded=bytes.toByteArray();BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(encoded,0,encoded.length,bounds);if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth*(long)bounds.outHeight>8000000L)throw new IOException();bitmap=BitmapFactory.decodeByteArray(encoded,0,encoded.length);int orientation=ExifInterface.ORIENTATION_NORMAL;try{orientation=new ExifInterface(new ByteArrayInputStream(encoded)).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);}catch(IOException unsupportedMetadata){}Matrix transform=new Matrix();switch(orientation){case 2:transform.setScale(-1,1);break;case 3:transform.setRotate(180);break;case 4:transform.setScale(1,-1);break;case 5:transform.setRotate(90);transform.postScale(-1,1);break;case 6:transform.setRotate(90);break;case 7:transform.setRotate(270);transform.postScale(-1,1);break;case 8:transform.setRotate(270);break;}if(bitmap!=null&&!transform.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),transform,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;}}catch(Exception failure){if(bitmap!=null)bitmap.recycle();bitmap=null;}Bitmap selected=bitmap;runOnUiThread(()->complete(selected));worker.shutdown();});}
    private void complete(Bitmap bitmap){
        Pending request=pending;
        if(request==null||!request.ticket.equals(getIntent().getStringExtra("ticket"))){if(bitmap!=null)bitmap.recycle();finish();return;}
        finish();
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            if(pending!=request){if(bitmap!=null)bitmap.recycle();return;}
            pending=null;
            android.inputmethodservice.InputMethodService keyboard=request.ime.get();
            if(keyboard!=null){if(Build.VERSION.SDK_INT>=28)keyboard.requestShowSelf(0);else{android.view.inputmethod.InputMethodManager manager=(android.view.inputmethod.InputMethodManager)keyboard.getSystemService(INPUT_METHOD_SERVICE);manager.showSoftInputFromInputMethod(keyboard.getWindow().getWindow().getAttributes().token,0);}}
            request.callback.accept(bitmap);
        },600);
    }
}
