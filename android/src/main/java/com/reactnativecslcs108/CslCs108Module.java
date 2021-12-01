package com.reactnativecslcs108;

import androidx.annotation.NonNull;
import android.content.Context;
import android.app.Activity;
import android.util.Log;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;

// import com.csl.cs108library4a.Cs108Library4A;

@ReactModule(name = CslCs108Module.NAME)
public class CslCs108Module extends ReactContextBaseJavaModule {

    public static final String NAME = "CslCs108";
    private final ReactApplicationContext context;

    // public static Cs108Library4A mCs108Library4a;
    public static Context mContext;

    public CslCs108Module(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    // -------------------------------------------------------------------------------------------

    @ReactMethod
    public void createClient() {
        // mContext = this.context.getCurrentActivity();
        // mCs108Library4a = new Cs108Library4A(mContext, null);
        Log.i("CslCs108Module", "createClient");
    }
}
