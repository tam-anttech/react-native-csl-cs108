package com.tamtran.reactnativecslcs108.converter;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.csl.cs108library4a.Cs108Connector.Cs108ScanData;

import java.util.Map;
import java.util.UUID;

public class ScanResultToJsObjectConverter extends JSObjectConverter<Cs108ScanData> {
    interface Metadata {
        String DEVICE = "device";
        String DEVICE_NAME = "deviceName";
        String DEVICE_NAME_1 = "deviceName1";
        String NAME = "name";
        String ADDRESS = "address";
        String RSSI = "rssi";
        String SCAN_RECORD = "scanRecord";
        String SERVICE_UUID_2P2 = "serviceUUID2p2";
    }

    String byteArrayToString(byte[] packet) {
        if (packet == null)
            return "";
        StringBuilder sb = new StringBuilder(packet.length * 2);
        for (byte b : packet) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    public WritableMap toJSObject(@NonNull Cs108ScanData cs108ScanData) {

        WritableMap result = Arguments.createMap();
        result.putInt(Metadata.RSSI, cs108ScanData.rssi);
        result.putString(Metadata.NAME, cs108ScanData.getName());
        result.putString(Metadata.ADDRESS, cs108ScanData.getAddress());
        result.putString(Metadata.SCAN_RECORD, byteArrayToString(cs108ScanData.getScanRecord()));
        result.putString(Metadata.DEVICE, cs108ScanData.getDevice().toString());
        result.putString(Metadata.DEVICE_NAME, cs108ScanData.getDevice().getName());
        result.putString(Metadata.DEVICE_NAME_1, cs108ScanData.getDevice().getAddress());

        // result.putArray(Metadata.ADDRESS, cs108ScanData.getScanRecord());

        return result;
    }
}
