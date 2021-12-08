package com.tamtran.reactnativecslcs108;

import androidx.annotation.NonNull;
import android.content.Context;
import android.app.Activity;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.module.annotations.ReactModule;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.csl.cs108library4a.Cs108Library4A;
import com.csl.cs108library4a.Cs108Connector;
import com.csl.cs108library4a.ReaderDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@ReactModule(name = CslCs108Module.NAME)
public class CslCs108Module extends ReactContextBaseJavaModule {

    public static final String NAME = "CslCs108";
    private final ReactApplicationContext context;

    public static Cs108Library4A mCs108Library4a;
    private ArrayList<Cs108Connector.Cs108ScanData> mScanResultList = new ArrayList<>();
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

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("DEFAULT_EVENT_NAME", "New Event");
        return constants;
    }
    // -------------------------------------------------------------------------------------------

    @ReactMethod
    public void createClient() {
        mContext = this.context.getCurrentActivity();
        mCs108Library4a = new Cs108Library4A(mContext, null);
    }

    @ReactMethod
    public void startDeviceScan() {
        mCs108Library4a.scanLeDevice(true);
    }

    @ReactMethod
    public void stopDeviceScan() {
        mCs108Library4a.scanLeDevice(false);
    }

    @ReactMethod
    public void getNewDeviceScanned(Callback callback) {
        Cs108Connector.Cs108ScanData cs108ScanData = mCs108Library4a.getNewDeviceScanned();
        if (cs108ScanData != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String json = mapper.writeValueAsString(cs108ScanData);
                callback.invoke(json);
            } catch (Exception e) {
                callback.invoke("ERROR_GET_DEVICE_EXCEPTION");
            }
        } else {
            callback.invoke("ERROR_GET_DEVICE_NULL");
        }
    }

    @ReactMethod
    public void connectDevice(String address, Callback callback) {
        ReaderDevice readerDevice = new ReaderDevice(null, address, false, "", 1, 0);
        boolean connectFlag = mCs108Library4a.connect(readerDevice);
        if (connectFlag) {
            callback.invoke(readerDevice.getAddress());
        } else {
            callback.invoke("ERROR_CONNECT_FAIL");
        }
    }

    @ReactMethod
    public void disconnectDevice() {
        mCs108Library4a.disconnect(true);
    }

    @ReactMethod
    public void startOperation() {
        mCs108Library4a.startOperation(Cs108Library4A.OperationTypes.TAG_INVENTORY_COMPACT);
    }

    @ReactMethod
    public void getRfidData(Callback callback) {
        Cs108Connector.Rx000pkgData rx000pkgData = mCs108Library4a.onRFIDEvent();
        if (rx000pkgData != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                String json = mapper.writeValueAsString(rx000pkgData);
                callback.invoke(json);
            } catch (Exception e) {
                callback.invoke("ERROR_RFID_EVENT_EXCEPTION");
            }
        } else {
            callback.invoke("ERROR_RFID_EVENT_NULL");
        }
    }

    @ReactMethod
    public void abortOperation() {
        mCs108Library4a.abortOperation();
    }

    private void sendEvent(@NonNull Event event, @Nullable Object params) {
        getReactApplicationContext()
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(event.name, params);
    }

}
