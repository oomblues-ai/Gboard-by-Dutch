package dev.jason.gboardpatches.extension.sharedmodel;

import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import app.johninnl.modelhost.api.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;

/** A selected image goes through its own caller-private session; no camera/screen collection occurs in the provider. */
public final class SharedOcrRuntime {
    private SharedOcrRuntime(){}
    public static String recognize(Context context,Bitmap bitmap)throws Exception {
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("OCR_MUST_RUN_OFF_UI_THREAD");
        if(bitmap==null || bitmap.getWidth()*(long)bitmap.getHeight()>8000000L)throw new IllegalArgumentException("Crop the image below eight megapixels");
        Context app=context.getApplicationContext();SharedProviderIdentity.verify(app);CountDownLatch connected=new CountDownLatch(1);
        AtomicReference<HostCapabilities> capabilities=new AtomicReference<>();AtomicReference<HostStatus> status=new AtomicReference<>();AtomicReference<String> failure=new AtomicReference<>();
        ModelHostClient client=new ModelHostClient(new ComponentName(ModelHostClient.DEFAULT_PACKAGE,ModelHostClient.SERVICE_CLASS),Runnable::run);
        new Handler(Looper.getMainLooper()).post(()->client.connect(app,new ConnectionListener(){
            public void onConnected(HostStatus s,HostCapabilities c){status.set(s);capabilities.set(c);connected.countDown();}
            public void onDisconnected(HostException e){failure.set(e.code);connected.countDown();}
        }));
        File image=null;HostSession session=null;RequestHandle handle=null;
        try {
            if(!connected.await(5,TimeUnit.SECONDS) || status.get()==null)throw new IllegalStateException(failure.get()==null?"PROVIDER_UNAVAILABLE":failure.get());
            if(!status.get().isReady())throw new IllegalStateException("PROVIDER_NOT_READY");
            if(!capabilities.get().supports("ocr"))throw new IllegalStateException("ENABLE_OCR_IN_PROVIDER");
            image=File.createTempFile("shared-ocr-",".png",app.getCacheDir());
            try(FileOutputStream output=new FileOutputStream(image)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,output))throw new IOException("IMAGE_ENCODING_FAILED");}
            session=client.openSession(new SessionOptions("Gboard OCR",null,4096));
            CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
            try(ParcelFileDescriptor descriptor=ParcelFileDescriptor.open(image,ParcelFileDescriptor.MODE_READ_ONLY)){
                handle=session.submitImage(new ImageRequest("ocr",null,null,1024),descriptor,json->{try{JSONObject o=new JSONObject(json);if(o.optBoolean("terminal")){if("ok".equals(o.optString("status")))result.set(o.getString("text"));else failure.set(o.optString("code","OCR_FAILED"));done.countDown();}}catch(Exception e){failure.set("PROTOCOL_ERROR");done.countDown();}});
            }
            if(!done.await(65,TimeUnit.SECONDS))throw new IllegalStateException("OCR_TIMEOUT");
            if(failure.get()!=null)throw new IllegalStateException(failure.get());
            if(result.get()==null)throw new IllegalStateException("OCR_FAILED");return result.get();
        }finally{if(handle!=null && !handle.isComplete())handle.cancel();if(session!=null)session.close();client.close();if(image!=null)image.delete();}
    }
}