package com.csl.cs108library4a;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static java.lang.Math.log10;
import static java.lang.Math.pow;

public class Cs108Connector extends BleConnector {
    final boolean appendToLogViewDisable = false;
    final boolean DEBUG = false; final boolean DEBUGTHREAD = false;
    boolean sameCheck = true;

    public static class Cs108ScanData {
        public BluetoothDevice device; String name, address;
        public int rssi;
        public byte[] scanRecord;
        ArrayList<byte[]> decoded_scanRecord;
        public int serviceUUID2p2;

        Cs108ScanData(BluetoothDevice device, int rssi, byte[] scanRecord) {
            this.device = device;
            this.rssi = rssi;
            this.scanRecord = scanRecord;
            decoded_scanRecord = new ArrayList<byte[]>();
        }
        Cs108ScanData(String name, String address, int rssi, byte[] scanRecord) {
            this.device = device; this.name = name; this.address = address;
            this.rssi = rssi;
            this.scanRecord = scanRecord;
        }
        public BluetoothDevice getDevice() { return device; }
        public String getName() {
            return name;
        }
        public String getAddress() {
            return address;
        }
        public byte[] getScanRecord() { return scanRecord; }
    }

    @Override
    boolean connectBle(ReaderDevice readerDevice) {
        boolean result = false;
        appendToLog("ConnectBle(" + readerDevice.getCompass() + ")");
        result = super.connectBle(readerDevice);
        if (result)
            writeDataCount = 0;
        return result;
    }

    @Override
    boolean isBleConnected() { return super.isBleConnected(); }

    @Override
    void disconnect() {
        super.disconnect();
        appendToLog("done");
        mRfidDevice.mRfidToWrite.clear();
        mRfidDevice.mRfidReaderChip.mRx000ToWrite.clear();
    }

    long getStreamInRate() { return super.getStreamInRate(); }

    int writeDataCount; int btSendTimeOut = 0; long btSendTime = 0;
    boolean writeData(byte[] buffer, int timeout) {
        boolean result = writeBleStreamOut(buffer);
        if (true) {
            btSendTime = System.currentTimeMillis();
            btSendTimeOut = timeout + 200;
            if (isCharacteristicListRead() == false) btSendTimeOut += 3000;
        }
        return result;
    }

    int[] crc_lookup_table = new int[]{
            0x0000, 0x1189, 0x2312, 0x329b, 0x4624, 0x57ad, 0x6536, 0x74bf,
            0x8c48, 0x9dc1, 0xaf5a, 0xbed3, 0xca6c, 0xdbe5, 0xe97e, 0xf8f7,
            0x1081, 0x0108, 0x3393, 0x221a, 0x56a5, 0x472c, 0x75b7, 0x643e,
            0x9cc9, 0x8d40, 0xbfdb, 0xae52, 0xdaed, 0xcb64, 0xf9ff, 0xe876,
            0x2102, 0x308b, 0x0210, 0x1399, 0x6726, 0x76af, 0x4434, 0x55bd,
            0xad4a, 0xbcc3, 0x8e58, 0x9fd1, 0xeb6e, 0xfae7, 0xc87c, 0xd9f5,
            0x3183, 0x200a, 0x1291, 0x0318, 0x77a7, 0x662e, 0x54b5, 0x453c,
            0xbdcb, 0xac42, 0x9ed9, 0x8f50, 0xfbef, 0xea66, 0xd8fd, 0xc974,
            0x4204, 0x538d, 0x6116, 0x709f, 0x0420, 0x15a9, 0x2732, 0x36bb,
            0xce4c, 0xdfc5, 0xed5e, 0xfcd7, 0x8868, 0x99e1, 0xab7a, 0xbaf3,
            0x5285, 0x430c, 0x7197, 0x601e, 0x14a1, 0x0528, 0x37b3, 0x263a,
            0xdecd, 0xcf44, 0xfddf, 0xec56, 0x98e9, 0x8960, 0xbbfb, 0xaa72,
            0x6306, 0x728f, 0x4014, 0x519d, 0x2522, 0x34ab, 0x0630, 0x17b9,
            0xef4e, 0xfec7, 0xcc5c, 0xddd5, 0xa96a, 0xb8e3, 0x8a78, 0x9bf1,
            0x7387, 0x620e, 0x5095, 0x411c, 0x35a3, 0x242a, 0x16b1, 0x0738,
            0xffcf, 0xee46, 0xdcdd, 0xcd54, 0xb9eb, 0xa862, 0x9af9, 0x8b70,
            0x8408, 0x9581, 0xa71a, 0xb693, 0xc22c, 0xd3a5, 0xe13e, 0xf0b7,
            0x0840, 0x19c9, 0x2b52, 0x3adb, 0x4e64, 0x5fed, 0x6d76, 0x7cff,
            0x9489, 0x8500, 0xb79b, 0xa612, 0xd2ad, 0xc324, 0xf1bf, 0xe036,
            0x18c1, 0x0948, 0x3bd3, 0x2a5a, 0x5ee5, 0x4f6c, 0x7df7, 0x6c7e,
            0xa50a, 0xb483, 0x8618, 0x9791, 0xe32e, 0xf2a7, 0xc03c, 0xd1b5,
            0x2942, 0x38cb, 0x0a50, 0x1bd9, 0x6f66, 0x7eef, 0x4c74, 0x5dfd,
            0xb58b, 0xa402, 0x9699, 0x8710, 0xf3af, 0xe226, 0xd0bd, 0xc134,
            0x39c3, 0x284a, 0x1ad1, 0x0b58, 0x7fe7, 0x6e6e, 0x5cf5, 0x4d7c,
            0xc60c, 0xd785, 0xe51e, 0xf497, 0x8028, 0x91a1, 0xa33a, 0xb2b3,
            0x4a44, 0x5bcd, 0x6956, 0x78df, 0x0c60, 0x1de9, 0x2f72, 0x3efb,
            0xd68d, 0xc704, 0xf59f, 0xe416, 0x90a9, 0x8120, 0xb3bb, 0xa232,
            0x5ac5, 0x4b4c, 0x79d7, 0x685e, 0x1ce1, 0x0d68, 0x3ff3, 0x2e7a,
            0xe70e, 0xf687, 0xc41c, 0xd595, 0xa12a, 0xb0a3, 0x8238, 0x93b1,
            0x6b46, 0x7acf, 0x4854, 0x59dd, 0x2d62, 0x3ceb, 0x0e70, 0x1ff9,
            0xf78f, 0xe606, 0xd49d, 0xc514, 0xb1ab, 0xa022, 0x92b9, 0x8330,
            0x7bc7, 0x6a4e, 0x58d5, 0x495c, 0x3de3, 0x2c6a, 0x1ef1, 0x0f78};

    boolean dataRead = false; int dataReadDisplayCount = 0; boolean mCs108DataReadRequest = false;
    int inventoryLength = 0;
    int iSequenceNumber; boolean bDifferentSequence = false, bFirstSequence = true;
    public int invalidata, invalidUpdata, validata;
    boolean dataInBufferResetting;
    @Override
    void processBleStreamInData() {
        final boolean DEBUG = false;
        int cs108DataReadStartOld = 0;
        int cs108DataReadStart = 0;
        boolean validHeader = false;

        if (dataInBufferResetting) {
            appendToLog("RESET.");
            dataInBufferResetting = false;
            /*cs108DataLeft = new byte[CS108DATALEFT_SIZE];*/
            cs108DataLeftOffset = 0;
            mCs108DataRead.clear();
        }
        if (DEBUG) appendToLog("START, cs108DataLeftOffset=" + cs108DataLeftOffset + ", streamInBufferSize=" + getStreamInBufferSize());
        boolean bFirst = true; long lTime = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - lTime > (getIntervalProcessBleStreamInData()/2)) {
                writeDebug2File("B" + String.valueOf(getIntervalProcessBleStreamInData()) + ", " + System.currentTimeMillis() + ", Timeout");
                appendToLogView("processCs108DataIn_TIMEOUT");
                break;
            }

            long streamInOverflowTime = getStreamInOverflowTime();
            int streamInMissing = getStreamInBytesMissing();
            if (streamInMissing != 0) appendToLogView("processCs108DataIn(" + getStreamInTotalCounter() + ", " + getStreamInAddCounter() + "): len=0, getStreamInOverflowTime()=" + streamInOverflowTime + ", MissBytes=" + streamInMissing + ", Offset=" + cs108DataLeftOffset);
            int len = readData(cs108DataLeft, cs108DataLeftOffset, cs108DataLeft.length);
            if (len != 0) {
                byte[] debugData = new byte[len];
                System.arraycopy(cs108DataLeft, cs108DataLeftOffset, debugData, 0, len);
                if (DEBUG) appendToLog("DataIn = " + byteArrayToString(debugData));
            }
            if (len != 0 && bFirst) { bFirst = false; writeDebug2File("B" + String.valueOf(getIntervalProcessBleStreamInData()) + ", " + System.currentTimeMillis()); }
            cs108DataLeftOffset += len;
            if (len == 0) {
                if (zeroLenDisplayed == false) {
                    zeroLenDisplayed = true;
                    if (getStreamInTotalCounter() != getStreamInAddCounter() || getStreamInAddTime() != 0 || cs108DataLeftOffset != 0) {
                        if (DEBUG) appendToLog("processCs108DataIn(" + getStreamInTotalCounter() + "," + getStreamInAddCounter() + "): len=0, getStreamInAddTime()=" + getStreamInAddTime() + ", Offset=" + cs108DataLeftOffset);
                    }
                }
                if (cs108DataLeftOffset == cs108DataLeft.length) {
                    if (DEBUG) appendToLog("cs108DataLeftOffset=" + cs108DataLeftOffset + ", cs108DataLeft=" + byteArrayToString(cs108DataLeft));
                }
                break;
            } else {
                dataRead = true;
                zeroLenDisplayed = false;

                while (cs108DataLeftOffset >= cs108DataReadStart + 8) {
                    validHeader = false;
                    byte[] dataIn = cs108DataLeft;
                    int iPayloadLength = (dataIn[cs108DataReadStart + 2] & 0xFF);
                    if ((dataIn[cs108DataReadStart + 0] == (byte) 0xA7)
                            && (dataIn[cs108DataReadStart + 1] == (byte) 0xB3)
                            && (dataIn[cs108DataReadStart + 3] == (byte) 0xC2
                            || dataIn[cs108DataReadStart + 3] == (byte) 0x6A
                            || dataIn[cs108DataReadStart + 3] == (byte) 0xD9
                            || dataIn[cs108DataReadStart + 3] == (byte) 0xE8
                            || dataIn[cs108DataReadStart + 3] == (byte) 0x5F)
                            && ((dataIn[cs108DataReadStart + 4] == (byte) 0x82) || ((dataIn[cs108DataReadStart + 3] == (byte) 0xC2) && (dataIn[cs108DataReadStart + 8] == (byte) 0x81)))
                            && (dataIn[cs108DataReadStart + 5] == (byte) 0x9E)) {
                        if (cs108DataLeftOffset - cs108DataReadStart < (iPayloadLength + 8))
                            break;

                        boolean bcheckChecksum = true;
                        int checksum = ((byte) dataIn[cs108DataReadStart + 6] & 0xFF) * 256 + ((byte) dataIn[cs108DataReadStart + 7] & 0xFF);
                        int checksum2 = 0;
                        if (bcheckChecksum) {
                            for (int i = cs108DataReadStart; i < cs108DataReadStart + 8 + iPayloadLength; i++) {
                                if (i != (cs108DataReadStart + 6) && i != (cs108DataReadStart + 7)) {
                                    int index = (checksum2 ^ ((byte) dataIn[i] & 0x0FF)) & 0x0FF;
                                    int table_value = crc_lookup_table[index];
                                    checksum2 = (checksum2 >> 8) ^ table_value;
                                }
                            }
                            if (false) appendToLog("checksum = " + String.format("%04X", checksum) + ", checksum2 = " + String.format("%04X", checksum2));
                        }
                        if (bcheckChecksum && checksum != checksum2) {
                            if (iPayloadLength < 0) appendToLog("processCs108DataIn_ERROR, iPayloadLength=" + iPayloadLength + ", cs108DataLeftOffset=" + cs108DataLeftOffset + ", dataIn=" + byteArrayToString(dataIn));
                            if (true) {
                                byte[] invalidPart = new byte[8 + iPayloadLength];
                                System.arraycopy(dataIn, cs108DataReadStart, invalidPart, 0, invalidPart.length);
                                appendToLog("processCs108DataIn_ERROR, INCORRECT RevChecksum=" + Integer.toString(checksum, 16) + ", CalChecksum2=" + Integer.toString(checksum2, 16) + ",data=" + byteArrayToString(invalidPart));
                            }
                        } else {
                            validHeader = true;
                            if (cs108DataReadStart > cs108DataReadStartOld) {
                                if (true) {
                                    byte[] invalidPart = new byte[cs108DataReadStart - cs108DataReadStartOld];
                                    System.arraycopy(dataIn, cs108DataReadStartOld, invalidPart, 0, invalidPart.length);
                                    appendToLog("processCs108DataIn_ERROR, before valid data, invalid unused data: " + invalidPart.length + ", " + byteArrayToString(invalidPart));
                                }
                            } else if (cs108DataReadStart < cs108DataReadStartOld)
                                appendToLog("processCs108DataIn_ERROR, invalid cs108DataReadStartdata=" + cs108DataReadStart + " < cs108DataReadStartOld=" + cs108DataReadStartOld);
                            cs108DataReadStartOld = cs108DataReadStart;

                            Cs108ReadData cs108ReadData = new Cs108ReadData();
                            byte[] dataValues = new byte[iPayloadLength];
                            System.arraycopy(dataIn, cs108DataReadStart + 8, dataValues, 0, dataValues.length);
                            cs108ReadData.dataValues = dataValues;
                            cs108ReadData.milliseconds = System.currentTimeMillis(); //getStreamInDataMilliSecond(); //
                            if (false) appendToLog("current:" + System.currentTimeMillis() + ", streamInData:" + getStreamInDataMilliSecond());
                            if (false) {
                                byte[] headerbytes = new byte[8];
                                System.arraycopy(dataIn, cs108DataReadStart, headerbytes, 0, headerbytes.length);
                                appendToLog("processCs108DataIn: Got package=" + byteArrayToString(headerbytes) + " " + byteArrayToString(dataValues));
                            }
                            boolean bRecdOldSequence = false;
                            switch (dataIn[cs108DataReadStart + 3]) {
                                case (byte) 0xC2:
                                    cs108ReadData.cs108ConnectedDevices = Cs108ConnectedDevices.RFID;
                                    if (dataIn[cs108DataReadStart + 8] == (byte) 0x81) {
                                        int iSequenceNumber = (int) (dataIn[cs108DataReadStart + 4] & 0xFF);
                                        int itemp = iSequenceNumber;
                                        if (itemp < this.iSequenceNumber) {
                                            itemp += 256;
                                        }
                                        itemp -= (this.iSequenceNumber + 1);
                                        if (itemp != 0) {
                                            cs108ReadData.invalidSequence = true;
                                            if (bFirstSequence == false) {
                                                if (itemp > 128) {
                                                    bRecdOldSequence = true;
                                                    appendToLogView(String.format("processCs108DataIn_ERROR: invalidata = %d, %X - %X = %d. Assume old package.", invalidata, iSequenceNumber, this.iSequenceNumber, itemp));
                                                }
                                                else {
                                                    invalidata += itemp;
                                                    if (true) {
                                                        String stringSequenceList = "";
                                                        for (int i = 0; i < itemp; i++) {
                                                            int iMissedNumber = (iSequenceNumber - i - 1);
                                                            if (iMissedNumber < 0)
                                                                iMissedNumber += 256;
                                                            stringSequenceList += (i != 0 ? ", " : "") + String.format("%X", iMissedNumber);
                                                        }
                                                        appendToLogView(String.format("processCs108DataIn_ERROR: invalidata = %d, %X - %X, miss %d: ", invalidata, iSequenceNumber, this.iSequenceNumber, itemp) + stringSequenceList);
                                                    }
                                                    appendToLog("New 1 sequence = " + iSequenceNumber + ", old = " + this.iSequenceNumber); this.iSequenceNumber = iSequenceNumber;
                                                }
                                            }
                                        }
                                        bFirstSequence = false;
                                        if (bRecdOldSequence == false) {
                                            appendToLog("New 2 sequence = " + iSequenceNumber + ", old = " + this.iSequenceNumber);
                                            this.iSequenceNumber = iSequenceNumber;
                                        }
                                    }
                                    if (true) appendToLogView("Rin: " + (cs108ReadData.invalidSequence ? "invalid sequence" : "ok") + "," + byteArrayToString(cs108ReadData.dataValues));
                                    validata++;
                                    break;
                                case (byte) 0x6A:
                                    if (true) {
                                        appendToLog("BarStreamIn: " + byteArrayToString(cs108ReadData.dataValues));
                                        appendToLogView("BIn: " + byteArrayToString(cs108ReadData.dataValues));
                                    }
                                    cs108ReadData.cs108ConnectedDevices = Cs108ConnectedDevices.BARCODE;
                                    break;
                                case (byte) 0xD9:
                                    if (DEBUG)
                                        appendToLog("BARTRIGGER NotificationData = " + byteArrayToString(cs108ReadData.dataValues));
                                    cs108ReadData.cs108ConnectedDevices = Cs108ConnectedDevices.NOTIFICATION;
                                    break;
                                case (byte) 0xE8:
                                    cs108ReadData.cs108ConnectedDevices = Cs108ConnectedDevices.SILICON_LAB;
                                    break;
                                case (byte) 0x5F:
                                    cs108ReadData.cs108ConnectedDevices = Cs108ConnectedDevices.BLUETOOTH;
                                    break;
                            }
                            mCs108DataRead.add(cs108ReadData);
                            cs108DataReadStart += ((8 + iPayloadLength));

                            byte[] cs108DataLeftNew = new byte[CS108DATALEFT_SIZE];
                            System.arraycopy(cs108DataLeft, cs108DataReadStart, cs108DataLeftNew, 0, cs108DataLeftOffset - cs108DataReadStart);
                            cs108DataLeft = cs108DataLeftNew;
                            cs108DataLeftOffset -= cs108DataReadStart;
                            cs108DataReadStart = 0;
                            cs108DataReadStart = -1;
                            if (mCs108DataReadRequest == false) {
                                mCs108DataReadRequest = true;
                                mHandler.removeCallbacks(mReadWriteRunnable); mHandler.post(mReadWriteRunnable);
                            }
                        }
                    }
                    if (validHeader && cs108DataReadStart < 0) {
                        cs108DataReadStart = 0;
                        cs108DataReadStartOld = 0;
                    } else {
                        cs108DataReadStart++;
                    }
                }
                if (cs108DataReadStart != 0 && cs108DataLeftOffset >= 8) {
                    if (true) {
                        byte[] invalidPart = new byte[cs108DataReadStart];
                        System.arraycopy(cs108DataLeft, 0, invalidPart, 0, invalidPart.length);
                        byte[] validPart = new byte[cs108DataLeftOffset - cs108DataReadStart];
                        System.arraycopy(cs108DataLeft, cs108DataReadStart, validPart, 0, validPart.length);
                        if (true) appendToLog("processCs108DataIn_ERROR, ENDLOOP invalid unused data: " + invalidPart.length + ", " + byteArrayToString(invalidPart) + ", with valid data length=" + validPart.length + ", " + byteArrayToString(validPart));
                    }

                    byte[] cs108DataLeftNew = new byte[CS108DATALEFT_SIZE];
                    System.arraycopy(cs108DataLeft, cs108DataReadStart, cs108DataLeftNew, 0, cs108DataLeftOffset - cs108DataReadStart);
                    cs108DataLeft = cs108DataLeftNew;
                    cs108DataLeftOffset -= cs108DataReadStart; cs108DataReadStart = 0;
                }
            }
        }
        if (DEBUG) appendToLog("END, cs108DataLeftOffset=" + cs108DataLeftOffset + ", streamInBufferSize=" + getStreamInBufferSize());
    }

    private int readData(byte[] buffer, int byteOffset, int byteCount) { return readBleSteamIn(buffer, byteOffset, byteCount); }

    class Cs108ConnectorData {
        int mVoltageValue; int getVoltageMv() { return mVoltageValue; }
        int mVoltageCount; int getVoltageCnt() { return mVoltageCount; }
        boolean triggerButtonStatus; boolean getTriggerButtonStatus() { return triggerButtonStatus; }
        int iTriggerCount; int getTriggerCount() { return iTriggerCount; }
        Date timeStamp;
        String getTimeStamp() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            return sdf.format(mCs108ConnectorData.timeStamp);
        }

    }
    Cs108ConnectorData mCs108ConnectorData;

    RfidDevice mRfidDevice;
    BarcodeDevice mBarcodeDevice;
    NotificationDevice mNotificationDevice;
    SiliconLabIcDevice mSiliconLabIcDevice;
    BluetoothConnector mBluetoothConnector;

    private Handler mHandler = new Handler();

    void cs108ConnectorDataInit() {
        mCs108DataRead = new ArrayList<>();
        cs108DataLeft = new byte[CS108DATALEFT_SIZE];
        cs108DataLeftOffset = 0;
        zeroLenDisplayed = false;

        invalidata = 0;
        validata = 0;
        dataInBufferResetting = false;

        writeDataCount = 0;

        mCs108ConnectorData = new Cs108ConnectorData();

        mRfidDevice = new RfidDevice();
        mBarcodeDevice = new BarcodeDevice();
        mNotificationDevice = new NotificationDevice();
        mSiliconLabIcDevice = new SiliconLabIcDevice();
        mBluetoothConnector = new BluetoothConnector(context, mLogView);
    }

    private enum CS108Connection {
        BLUETOOTH, USB, OTHER
    }

    enum Cs108ConnectedDevices {
        RFID, BARCODE, NOTIFICATION, SILICON_LAB, BLUETOOTH, OTHER
    }
    class Cs108ReadData {
        CS108Connection cs108Connection;
        Cs108ConnectedDevices cs108ConnectedDevices;
        byte[] dataValues;
        boolean invalidSequence;
        long milliseconds;

    }
    final int CS108DATALEFT_SIZE = 300; //4000;    //100;
    private ArrayList<Cs108ReadData> mCs108DataRead;
    byte[] cs108DataLeft;
    int cs108DataLeftOffset;
    boolean zeroLenDisplayed;

    Context context; TextView mLogView;
    Cs108Connector(Context context, TextView mLogView) {
        super(context, mLogView);
//        mUsbConnector = new UsbConnector(context, mLogView, 33896, 4292);

        this.context = context;
        this.mLogView = mLogView;

        cs108ConnectorDataInit();
        mHandler.removeCallbacks(runnableProcessBleStreamInData); mHandler.post(runnableProcessBleStreamInData);
        mHandler.removeCallbacks(mReadWriteRunnable); mHandler.post(mReadWriteRunnable);
        mHandler.removeCallbacks(runnableRx000UplinkHandler); mHandler.post(runnableRx000UplinkHandler);
    }
/*
    boolean batteryReportRequest = false; boolean batteryReportOn = false;
    void setBatteryAutoReport(boolean on) {
        appendToLog("setBatteryAutoReport(" + on + ")");
        batteryReportRequest = true; batteryReportOn = on;
    }
    void setBatteryAutoReport() {
        if (batteryReportRequest) {
            batteryReportRequest = false;
            appendToLog("setBatteryAutoReport()");
            boolean retValue = false;
            if (checkHostProcessorVersion(mSiliconLabIcDevice.getSiliconLabIcVersion(), 1, 0, 2)) {
                appendToLog("setBatteryAutoReport(): 111");
                if (batteryReportOn)
                    retValue = mNotificationDevice.mNotificationToWrite.add(NotificationPayloadEvents.NOTIFICATION_AUTO_BATTERY_VOLTAGE);
                else
                    retValue = mNotificationDevice.mNotificationToWrite.add(NotificationPayloadEvents.NOTIFICATION_STOPAUTO_BATTERY_VOLTAGE);
            }
        }
    }*/
    public boolean checkHostProcessorVersion(String version, int majorVersion, int minorVersion, int buildVersion) {
        if (version == null) return false;
        if (version.length() == 0) return false;
        String[] versionPart = version.split("\\.");

        if (versionPart == null) { appendToLog("NULL VersionPart"); return false; }
        try {
            int value = Integer.valueOf(versionPart[0]);
            if (value < majorVersion) return false;
            if (value > majorVersion) return true;

            if (versionPart.length < 2) return true;
            value = Integer.valueOf(versionPart[1]);
            if (value < minorVersion) return false;
            if (value > minorVersion) return true;

            if (versionPart.length < 3) return true;
            value = Integer.valueOf(versionPart[2]);
            if (value < buildVersion) return false;
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    int rfidPowerOnTimeOut = 0; int barcodePowerOnTimeOut = 0;
    long timeReady; boolean aborting = false, sendFailure = false;
    private final Runnable mReadWriteRunnable = new Runnable() {
        boolean ready2Write = false;
        int timer2Write = 0;
        boolean validBuffer;

        @Override
        public void run() {
            if (DEBUGTHREAD) appendToLog("mReadWriteRunnable starts");
            if (timer2Write != 0 || getStreamInBufferSize() != 0 || mRfidDevice.mRfidToRead.size() != 0) {
                validBuffer = true;
                if (DEBUG) appendToLog("mReadWriteRunnable(): START, timer2Write=" + timer2Write + ", streamInBufferSize = " + getStreamInBufferSize() + ", mRfidToRead.size=" + mRfidDevice.mRfidToRead.size() + ", mRx000ToRead.size=" + mRfidDevice.mRfidReaderChip.mRx000ToRead.size());
            } else  validBuffer = false;
            int intervalReadWrite = 250; //50;   //50;    //500;   //500, 100;
            if (rfidPowerOnTimeOut >= intervalReadWrite) {
                rfidPowerOnTimeOut -= intervalReadWrite;
                if (rfidPowerOnTimeOut <= 0) {
                    rfidPowerOnTimeOut = 0;
                }
            }
            if (barcodePowerOnTimeOut >= intervalReadWrite) {
                barcodePowerOnTimeOut -= intervalReadWrite;
                if (barcodePowerOnTimeOut <= 0) {
                    barcodePowerOnTimeOut = 0;
                }
            }
            if (barcodePowerOnTimeOut != 0)
                if (DEBUG) appendToLog("mReadWriteRunnable(): barcodePowerOnTimeOut = " + barcodePowerOnTimeOut);

            long lTime = System.currentTimeMillis();
            mHandler.postDelayed(mReadWriteRunnable, intervalReadWrite);
            if (mRfidDevice.mRfidReaderChip == null) return;

            boolean bFirst = true;
            mCs108DataReadRequest = false;
            while (mCs108DataRead.size() != 0) {
                if (isBleConnected() == false) {
                    mCs108DataRead.clear();
                } else if (System.currentTimeMillis() - lTime > (intervalRx000UplinkHandler / 2)) {
                        writeDebug2File("C" + String.valueOf(intervalReadWrite) + ", " + System.currentTimeMillis() + ", Timeout");
                        appendToLogView("mReadWriteRunnable_TIMEOUT !!! mCs108DataRead.size() = " + mCs108DataRead.size());
                        break;
                } else {
                    if (bFirst) { bFirst = false; writeDebug2File("C" + String.valueOf(intervalReadWrite) + ", " + System.currentTimeMillis()); }
                    try {
                        Cs108ReadData cs108ReadData = mCs108DataRead.get(0);
                        mCs108DataRead.remove(0);
                        if (DEBUG) appendToLog("mReadWriteRunnable(): mCs108DataRead.dataValues = " + byteArrayToString(cs108ReadData.dataValues));
                        if (mRfidDevice.isMatchRfidToWrite(cs108ReadData)) {
                            if (writeDataCount > 0) writeDataCount--; ready2Write = true; btSendTime = 0; aborting = false;
                        } else if (mBarcodeDevice.isMatchBarcodeToWrite(cs108ReadData)) {
                            if (writeDataCount > 0) writeDataCount--; ready2Write = true; btSendTime = 0;
                        } else if (mNotificationDevice.isMatchNotificationToWrite(cs108ReadData)) {
                            if (writeDataCount > 0) writeDataCount--; ready2Write = true; btSendTime = 0;
                        } else if (mSiliconLabIcDevice.isMatchSiliconLabIcToWrite(cs108ReadData)) {
                            if (writeDataCount > 0) writeDataCount--; ready2Write = true; btSendTime = 0;
                        } else if (mBluetoothConnector.mBluetoothIcDevice.isMatchBluetoothIcToWrite(cs108ReadData)) {
                            if (writeDataCount > 0) writeDataCount--; ready2Write = true; btSendTime = 0;
                        } else if (mRfidDevice.isRfidToRead(cs108ReadData)) { mRfidDevice.rfidValid = true;
                        } else if (mBarcodeDevice.isBarcodeToRead(cs108ReadData)) {
                        } else if (mNotificationDevice.isNotificationToRead(cs108ReadData)) {
                            /* if (mRfidDevice.mRfidToWrite.size() != 0 && mNotificationDevice.mNotificationToRead.size() != 0) {
                                mNotificationDevice.mNotificationToRead.remove(0);
                                mRfidDevice.mRfidToWrite.clear();
                                mSiliconLabIcDevice.mSiliconLabIcToWrite.add(SiliconLabIcPayloadEvents.RESET);

                                timeReady = System.currentTimeMillis() - 1500;
                                appendToLog("endingMessage: changed timeReady");
                            }*/
                        }
                        if (mBarcodeDevice.mDataToWriteRemoved)  { mBarcodeDevice.mDataToWriteRemoved = false; ready2Write = true; btSendTime = 0; }
                    } catch (Exception ex) {
                    }
                }
            }
            if (mRfidDevice.mRfidToWriteRemoved)  { mRfidDevice.mRfidToWriteRemoved = false; ready2Write = true; btSendTime = 0; }
            int timeout2Ready = 2000; if (aborting || sendFailure) timeout2Ready = 200;
            if (System.currentTimeMillis() > timeReady + timeout2Ready) ready2Write = true;
            if (ready2Write) {
                timeReady = System.currentTimeMillis();
                timer2Write = 0;
                if (mRfidDevice.mRfidReaderChip.mRx000ToWrite.size() != 0 && mRfidDevice.mRfidToWrite.size() == 0) {
                    if (DEBUG) appendToLog("mReadWriteRunnable(): mRx000ToWrite.size=" + mRfidDevice.mRfidReaderChip.mRx000ToWrite.size() + ", mRfidToWrite.size=" + mRfidDevice.mRfidToWrite.size());
                    mRfidDevice.mRfidReaderChip.addRfidToWrite(mRfidDevice.mRfidReaderChip.mRx000ToWrite.get(0));
                }
                boolean bisRfidCommandStop = false, bisRfidCommandExecute = false;
                if (mRfidDevice.mRfidToWrite.size() != 0) {
                    Cs108RfidData cs108RfidData = mRfidDevice.mRfidToWrite.get(0);
                    if (cs108RfidData.rfidPayloadEvent == RfidPayloadEvents.RFID_COMMAND) {
                        int ii;
                        if (false) {
                            byte[] byCommandExeccute = new byte[]{0x70, 1, 0, (byte) 0xF0};
                            for (ii = 0; ii < 4; ii++) {
                                if (byCommandExeccute[ii] != cs108RfidData.dataValues[ii]) break;
                            }
                            if (ii == 4) bisRfidCommandExecute = true;
                        }

                        byte[] byCommandStop = new byte[]{(byte) 0x40, 3, 0, 0, 0, 0, 0, 0};
                        for (ii = 0; ii < 4; ii++) {
                            if (byCommandStop[ii] != cs108RfidData.dataValues[ii]) break;
                        }
                        if (ii == 4) bisRfidCommandStop = true;
                        appendToLog("mRfidToWrite(0).dataValues = " + byteArrayToString(mRfidDevice.mRfidToWrite.get(0).dataValues) + ", bisRfidCommandExecute = " + bisRfidCommandExecute + ", bisRfidCommandStop = " + bisRfidCommandStop);
                    }
                }
                if (bisRfidCommandStop) {
                    mRfidDevice.sendRfidToWrite();
                    ready2Write = false;    //
                } else if (mSiliconLabIcDevice.sendSiliconLabIcToWrite()) { //SiliconLab version afffects Notification operation
                    ready2Write = false;    //
                } else if (mBluetoothConnector.mBluetoothIcDevice.mBluetoothIcToWrite.size() != 0) {   //Bluetooth version affects Barcode operation
                    if (isBleConnected() == false) mBluetoothConnector.mBluetoothIcDevice.mBluetoothIcToWrite.clear();
                    else if (System.currentTimeMillis() - btSendTime > btSendTimeOut) {
                        byte[] dataOut = mBluetoothConnector.mBluetoothIcDevice.sendBluetoothIcToWrite();
                        boolean retValue = false;
                        if (dataOut != null) retValue = writeData(dataOut, 0);
                        if (retValue) {
                            mBluetoothConnector.mBluetoothIcDevice.sendDataToWriteSent++;
                        } else {
                            appendToLogView("failure to send " + mBluetoothConnector.mBluetoothIcDevice.mBluetoothIcToWrite.get(0).bluetoothIcPayloadEvent.toString());
                            mBluetoothConnector.mBluetoothIcDevice.mBluetoothIcToWrite.remove(0);
                        }
                    }
                    ready2Write = false;
                } else if (mNotificationDevice.sendNotificationToWrite()) {
                    ready2Write = false;
                } else if (mBarcodeDevice.sendBarcodeToWrite()) {
                    ready2Write = false;
                } else if (mRfidDevice.sendRfidToWrite()) {
                    ready2Write = false;
                }
            }
            if (validBuffer) {
                if (DEBUG)  appendToLog("mReadWriteRunnable(): END, timer2Write=" + timer2Write + ", streamInBufferSize = " + getStreamInBufferSize() + ", mRfidToRead.size=" + mRfidDevice.mRfidToRead.size() + ", mRx000ToRead.size=" + mRfidDevice.mRfidReaderChip.mRx000ToRead.size());
            }
            mRfidDevice.mRfidReaderChip.mRx000UplinkHandler();
            if (DEBUGTHREAD) appendToLog("mReadWriteRunnable ends");
        }
    };

    int intervalRx000UplinkHandler = 250;
    private final Runnable runnableRx000UplinkHandler = new Runnable() {
        @Override
        public void run() {
//            mRfidDevice.mRx000Device.mRx000UplinkHandler();
            mHandler.postDelayed(runnableRx000UplinkHandler, intervalRx000UplinkHandler);
        }
    };

    private enum RfidPayloadEvents {
        RFID_POWER_ON, RFID_POWER_OFF, RFID_COMMAND,
        RFID_DATA_READ
    }
    class Cs108RfidData {
        boolean waitUplinkResponse = false;
        boolean downlinkResponsed = false;
        RfidPayloadEvents rfidPayloadEvent;
        byte[] dataValues;
        boolean invalidSequence;
        long milliseconds;
    }
    class RfidDevice {
        private boolean onStatus = false; boolean getOnStatus() { return onStatus;}
        ArrayList<Cs108RfidData> mRfidToWrite = new ArrayList<>();
        ArrayList<Cs108RfidData> mRfidToRead = new ArrayList<>();

        boolean inventoring = false;
        boolean isInventoring() { return  inventoring; }
        void setInventoring(boolean enable) { inventoring = enable; debugFileEnable(enable); }

        RfidReaderChip mRfidReaderChip = new RfidReaderChip();

        private boolean arrayTypeSet(byte[] dataBuf, int pos, RfidPayloadEvents event) {
            boolean validEvent = false;
            switch (event) {
                case RFID_POWER_ON:
                    validEvent = true;
                    break;
                case RFID_POWER_OFF:
                    dataBuf[pos] = 1;
                    validEvent = true;
                    break;
                case RFID_COMMAND:
                    dataBuf[pos] = 2;
                    validEvent = true;
                    break;
            }
            return validEvent;
        }

        boolean writeRfid(Cs108RfidData dataIn) {
            byte[] dataOut = new byte[]{(byte) 0xA7, (byte) 0xB3, 2, (byte) 0xC2, (byte) 0x82, (byte) 0x37, 0, 0, (byte) 0x80, 0};
            if (dataIn.rfidPayloadEvent == RfidPayloadEvents.RFID_COMMAND) {
                if (dataIn.dataValues != null) {
                    byte[] dataOut1 = new byte[dataOut.length + dataIn.dataValues.length];
                    System.arraycopy(dataOut, 0, dataOut1, 0, dataOut.length);
                    dataOut1[2] += dataIn.dataValues.length;
                    System.arraycopy(dataIn.dataValues, 0, dataOut1, dataOut.length, dataIn.dataValues.length);
                    dataOut = dataOut1;
                }
            }
            if (arrayTypeSet(dataOut, 9, dataIn.rfidPayloadEvent)) {
                appendToLogView(byteArrayToString(dataOut));
                return writeData(dataOut, (dataIn.waitUplinkResponse ? 500 : 0));
            }
            return false;
        }

        private boolean isMatchRfidToWrite(Cs108ReadData cs108ReadData) {
            boolean match = false;
            if (mRfidToWrite.size() != 0) {
                byte[] dataInCompare = new byte[]{(byte) 0x80, 0};
                if (arrayTypeSet(dataInCompare, 1, mRfidToWrite.get(0).rfidPayloadEvent) && (cs108ReadData.dataValues.length == dataInCompare.length + 1)) {
                    if (match = compareArray(cs108ReadData.dataValues, dataInCompare, dataInCompare.length)) {
                        if (true) appendToLog("found Rfid.read data = " + byteArrayToString(cs108ReadData.dataValues) + ", writeData = " + byteArrayToString(mRfidToWrite.get(0).dataValues));
                        if (cs108ReadData.dataValues[2] != 0) {
                            if (DEBUG) appendToLog("Rfid.reply data is found with error");
                        } else {
                            if (mRfidToWrite.get(0).rfidPayloadEvent == RfidPayloadEvents.RFID_POWER_ON) {
                                rfidPowerOnTimeOut = 3000;
                                onStatus = true;
                            } else if (mRfidToWrite.get(0).rfidPayloadEvent == RfidPayloadEvents.RFID_POWER_OFF) {
                                onStatus = false;
                            }
                            Cs108RfidData cs108RfidData = mRfidToWrite.get(0);
                            if (cs108RfidData.waitUplinkResponse) {
                                cs108RfidData.downlinkResponsed = true;
                                mRfidToWrite.set(0, cs108RfidData);
                                if (false) {
                                    for (int i = 0; i < mRfidReaderChip.mRx000ToRead.size(); i++) {
                                        if (mRfidReaderChip.mRx000ToRead.get(i).responseType == HostCmdResponseTypes.TYPE_COMMAND_END)
                                            appendToLog("mRx0000ToRead with COMMAND_END is removed");
                                    }
                                    appendToLog("mRx000ToRead.clear !!!");
                                }
                                mRfidReaderChip.mRx000ToRead.clear();
                                return false;
                            }
                            if (DEBUG) appendToLog("matched Rfid.reply data is found with mRfidToWrite.size=" + mRfidToWrite.size());
                        }
                        mRfidToWrite.remove(0); sendRfidToWriteSent = 0; mRfidToWriteRemoved = true; appendToLog("mmRfidToWrite remove 1 with remained write size = " + mRfidToWrite.size());
                        if (false) {
                            for (int i = 0; i < mRfidReaderChip.mRx000ToRead.size(); i++) {
                                if (mRfidReaderChip.mRx000ToRead.get(i).responseType == HostCmdResponseTypes.TYPE_COMMAND_END)
                                    appendToLog("mRx0000ToRead with COMMAND_END is removed");
                            }
                            appendToLog("mRx000ToRead.clear !!!");
                        }
                        mRfidReaderChip.mRx000ToRead.clear();
                    }
                }
            }
            return match;
        }

        private long logTime;
        private int sendRfidToWriteSent = 0; boolean mRfidToWriteRemoved = false;
        boolean rfidFailure = false; boolean rfidValid = false;
        private boolean sendRfidToWrite() {
            if (rfidPowerOnTimeOut != 0) {
                if (DEBUG) appendToLog("rfidPowerOnTimeOut = " + rfidPowerOnTimeOut + ", mRfidToWrite.size() = " + mRfidToWrite.size());
                return false;
            }
            if (rfidFailure == false && mRfidToWrite.size() != 0) {
                if (isBleConnected() == false) {
                    mRfidToWrite.clear();
                } else if (System.currentTimeMillis() - btSendTime > btSendTimeOut) {
                    RfidPayloadEvents rfidPayloadEvents = mRfidToWrite.get(0).rfidPayloadEvent;
                    if (sendRfidToWriteSent >= 20 && rfidPayloadEvents != RfidPayloadEvents.RFID_POWER_ON && rfidPayloadEvents != RfidPayloadEvents.RFID_POWER_OFF) {
                        mRfidToWrite.remove(0); sendRfidToWriteSent = 0; mRfidToWriteRemoved = true; appendToLog("mmRfidToWrite remove 2");
                        if (DEBUG) appendToLog("Removed after sending count-out.");
                        if (rfidValid == false) {
                            Toast.makeText(context, "Problem in sending data to Rfid Module. Rfid is disabled.", Toast.LENGTH_SHORT).show();
                            rfidFailure = true;
                        } else {
                            Toast.makeText(context, "Problem in Sending Commands to RFID Module.  Bluetooth Disconnected.  Please Reconnect", Toast.LENGTH_SHORT).show();
                            disconnect();
                        }
                        if (DEBUG) appendToLog("done");
                    } else {
                        if (true) appendToLog("size = " + mRfidToWrite.size() + ", PayloadEvents = " + rfidPayloadEvents.toString() + ", data=" + byteArrayToString(mRfidToWrite.get(0).dataValues));
                        boolean retValue = writeRfid(mRfidToWrite.get(0));
                        sendRfidToWriteSent++;
                        if (retValue)   {
                            mRfidToWriteRemoved = false;
                            if (DEBUG) appendToLog("writeRfid() with sendRfidToWriteSent = " + sendRfidToWriteSent);
                            sendFailure = false;
                        } else sendFailure = true;
                    }
                }
                return true;
            }
            return false;
        }

        private boolean isRfidToRead(Cs108ReadData cs108ReadData) {
            boolean found = false;
            if (cs108ReadData.dataValues[0] == (byte) 0x81) {
                Cs108RfidData cs108RfidReadData = new Cs108RfidData();
                byte[] dataValues = new byte[cs108ReadData.dataValues.length - 2];
                System.arraycopy(cs108ReadData.dataValues, 2, dataValues, 0, dataValues.length);
                switch (cs108ReadData.dataValues[1]) {
                    case 0:
                        cs108RfidReadData.rfidPayloadEvent = RfidPayloadEvents.RFID_DATA_READ;
                        cs108RfidReadData.dataValues = dataValues;
                        cs108RfidReadData.invalidSequence = cs108ReadData.invalidSequence;
                        cs108RfidReadData.milliseconds = cs108ReadData.milliseconds;
                        mRfidToRead.add(cs108RfidReadData);
                        found = true;
                        break;
                    default:
                        invalidUpdata++;
                        if (DEBUG) appendToLog("found Invalid Rfid.read data = " + byteArrayToString(dataValues));
                        break;
                }
                if (DEBUG_BTDATA && found) appendToLog("found Rfid.read data = " + byteArrayToString(dataValues));
            }
            return found;
        }
    }

    enum BarcodePayloadEvents {
        BARCODE_NULL,
        BARCODE_POWER_ON, BARCODE_POWER_OFF, BARCODE_SCAN_START, BARCODE_COMMAND, BARCODE_VIBRATE_ON, BARCODE_VIBRATE_OFF,
        BARCODE_DATA_READ, BARCODE_GOOD_READ,
    }
    enum BarcodeCommendTypes {
        COMMAND_COMMON, COMMAND_SETTING, COMMAND_QUERY
    }
    class Cs108BarcodeData {
        boolean waitUplinkResponse = false;
        boolean downlinkResponsed = false;
        BarcodePayloadEvents barcodePayloadEvent;
        byte[] dataValues;
    }
    class BarcodeDevice {
        private boolean onStatus = false; boolean getOnStatus() { return onStatus; }
        private boolean vibrateStatus = false; boolean getVibrateStatus() { return vibrateStatus; }
        private String strVersion, strESN, strSerialNumber, strDate;
        String getVersion() { return strVersion; }
        String getESN() { return strESN; }
        String getSerialNumber() { return strSerialNumber; }
        String getDate() { return strDate; }
        byte[] bytesBarcodePrefix = null;
        byte[] bytesBarcodeSuffix = null;
        byte[] getPrefix() { return bytesBarcodePrefix; }
        byte[] getSuffix() { return bytesBarcodeSuffix; }
        boolean checkPreSuffix(byte[] prefix1, byte[] suffix1) {
            boolean result = false;
            if (prefix1 != null && bytesBarcodePrefix != null && suffix1 != null && bytesBarcodeSuffix != null) {
                result = Arrays.equals(prefix1, bytesBarcodePrefix);
                if (result) result = Arrays.equals(suffix1, bytesBarcodeSuffix);
            }
            return result;
        }

        ArrayList<Cs108BarcodeData> mBarcodeToWrite = new ArrayList<>();
        ArrayList<Cs108BarcodeData> mBarcodeToRead = new ArrayList<>();

        private boolean arrayTypeSet(byte[] dataBuf, int pos, BarcodePayloadEvents event) {
            boolean validEvent = false;
            switch (event) {
                case BARCODE_POWER_ON:
                    validEvent = true;
                    break;
                case BARCODE_POWER_OFF:
                    dataBuf[pos] = 1;
                    validEvent = true;
                    break;
                case BARCODE_SCAN_START:
                    dataBuf[pos] = 2;
                    validEvent = true;
                    break;
                case BARCODE_COMMAND:
                    dataBuf[pos] = 3;
                    validEvent = true;
                    break;
                case BARCODE_VIBRATE_ON:
                    dataBuf[pos] = 4;
                    validEvent = true;
                    break;
                case BARCODE_VIBRATE_OFF:
                    dataBuf[pos] = 5;
                    validEvent = true;
                    break;
            }
            return validEvent;
        }

        private boolean writeBarcode(Cs108BarcodeData data) {
            int datalength = 0;
            if (data.dataValues != null)    datalength = data.dataValues.length;
            byte[] dataOutRef = new byte[] { (byte) 0xA7, (byte) 0xB3, 2, (byte) 0x6A, (byte) 0x82, (byte) 0x37, 0, 0, (byte) 0x90, 0};

            byte[] dataOut = new byte[10 + datalength];
            if (datalength != 0)    {
                System.arraycopy(data.dataValues, 0, dataOut, 10, datalength);
                dataOutRef[2] += datalength;
            }
            System.arraycopy(dataOutRef, 0, dataOut, 0, dataOutRef.length);
            if (arrayTypeSet(dataOut, 9, data.barcodePayloadEvent)) {
                if (true) {
                    appendToLog("BarStreamOut: " + byteArrayToString(dataOut));
                    appendToLogView("BOut: " + byteArrayToString(dataOut));
                }
                return writeData(dataOut, (data.waitUplinkResponse ? 500 : 0));
            }
            return false;
        }

        private boolean isMatchBarcodeToWrite(Cs108ReadData cs108ReadData) {
            boolean match = false;
            if (mBarcodeToWrite.size() != 0) {
                appendToLog("tempDisconnect: icsModel = " + mBluetoothConnector.getCsModel() + ", mBarcodeToWrite.size = " + mBarcodeToWrite.size());
                byte[] dataInCompare = new byte[]{(byte) 0x90, 0};
                if (arrayTypeSet(dataInCompare, 1, mBarcodeToWrite.get(0).barcodePayloadEvent) && (cs108ReadData.dataValues.length == dataInCompare.length + 1)) {
                    if (match = compareArray(cs108ReadData.dataValues, dataInCompare, dataInCompare.length)) {
                        if (DEBUG_BTDATA) appendToLog("found Barcode.read data = " + byteArrayToString(cs108ReadData.dataValues));
                        appendToLog("tempDisconnect: icsModel = " + mBluetoothConnector.getCsModel() + ", isMatchBarcodeToWrite with cs108ReadData = " + byteArrayToString(cs108ReadData.dataValues));
                        if (cs108ReadData.dataValues[2] != 0) {
                            if (DEBUG) appendToLog("Barcode.reply data is found with error");
                        } else if (mBluetoothConnector.getCsModel() == 108) {
                            if (mBarcodeToWrite.get(0).barcodePayloadEvent == BarcodePayloadEvents.BARCODE_POWER_ON) {
                                barcodePowerOnTimeOut = 1000;
                                appendToLog("tempDisconnect: BARCODE_POWER_ON");
                                onStatus = true;
                            } else if (mBarcodeToWrite.get(0).barcodePayloadEvent == BarcodePayloadEvents.BARCODE_POWER_OFF) {
                                appendToLog("tempDisconnect: BARCODE_POWER_OFF");
                                onStatus = false;
                            } else if (mBarcodeToWrite.get(0).barcodePayloadEvent == BarcodePayloadEvents.BARCODE_VIBRATE_ON) {
                                vibrateStatus = true;
                            } else if (mBarcodeToWrite.get(0).barcodePayloadEvent == BarcodePayloadEvents.BARCODE_VIBRATE_OFF) {
                                vibrateStatus = false;
                            } else if (mBarcodeToWrite.get(0).barcodePayloadEvent == BarcodePayloadEvents.BARCODE_COMMAND) {
                                barcodePowerOnTimeOut = 500; if (DEBUG) appendToLog("barcodePowerOnTimeOut is set to 500");
                            }
                            Cs108BarcodeData cs108BarcodeData = mBarcodeToWrite.get(0);
                            if (cs108BarcodeData.waitUplinkResponse) {
                                cs108BarcodeData.downlinkResponsed = true;
                                mBarcodeToWrite.set(0, cs108BarcodeData);
                                return false;
                            }
                            //if (DEBUG)
                            if (DEBUG) appendToLog("matched Barcode.reply data is found with mBarcodeToWrite.size=" + mBarcodeToWrite.size());
                        } else barcodeFailure = true;
                        mBarcodeToWrite.remove(0); sendDataToWriteSent = 0; mDataToWriteRemoved = true;
                    }
                }
            }
            return match;
        }

        private int sendDataToWriteSent = 0; boolean mDataToWriteRemoved = false;
        boolean barcodeFailure = false;
        private boolean sendBarcodeToWrite() {
            if (barcodePowerOnTimeOut != 0) {
                if (DEBUG) appendToLog("barcodePowerOnTimeOut = " + barcodePowerOnTimeOut + ", mBarcodeToWrite.size() = " + mBarcodeToWrite.size());
                return false;
            }
            if (mBarcodeToWrite.size() != 0) {
                if (isBleConnected() == false) {
                    mBarcodeToWrite.clear();
                } else if (System.currentTimeMillis() - btSendTime > btSendTimeOut) {
                    BarcodePayloadEvents barcodePayloadEvents = mBarcodeToWrite.get(0).barcodePayloadEvent;
                    if (DEBUG)  appendToLog("barcodePayloadEvents = " + barcodePayloadEvents.toString());
                    boolean isBarcodeData = false;
                    if (barcodePayloadEvents == BarcodePayloadEvents.BARCODE_SCAN_START || barcodePayloadEvents == BarcodePayloadEvents.BARCODE_COMMAND) isBarcodeData = true;
                    if (barcodeFailure && isBarcodeData) {
                        mBarcodeToWrite.remove(0); sendDataToWriteSent = 0; mDataToWriteRemoved = true;
                    } else if (sendDataToWriteSent >= 2 && isBarcodeData) {
                        mBarcodeToWrite.remove(0); sendDataToWriteSent = 0; mDataToWriteRemoved = true;
                        if (DEBUG) appendToLog("Removed after sending count-out.");
                        if (false) Toast.makeText(context, "Problem in sending data to BarCode Module. Barcode is disabled", Toast.LENGTH_LONG).show();
                        else if (mBluetoothConnector.getCsModel() == 108) Toast.makeText(context, "No barcode present on Reader", Toast.LENGTH_LONG).show();
                        barcodeFailure = true; // disconnect(false);
                    } else {
                        if (DEBUG) appendToLog("size = " + mBarcodeToWrite.size() + ", PayloadEvents = " + mBarcodeToWrite.get(0).barcodePayloadEvent.toString());
                        boolean retValue = writeBarcode(mBarcodeToWrite.get(0));
                        if (retValue) {
                            sendDataToWriteSent++;
                            mDataToWriteRemoved = false;
                        } else {
                            appendToLogView("failure to send " + mBarcodeToWrite.get(0).barcodePayloadEvent.toString());
                            mBarcodeToWrite.remove(0);
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        byte bBarcodeTriggerMode = (byte)0xff;
        private boolean isBarcodeToRead(Cs108ReadData cs108ReadData) {
            boolean found = false, DEBUG = false;

            if (cs108ReadData.dataValues[0] == (byte) 0x91) {
                if (true) appendToLog("BarStream: isBarcodeToRead(): dataValues = " + byteArrayToString(cs108ReadData.dataValues));
                Cs108BarcodeData cs108BarcodeData = new Cs108BarcodeData();
                switch (cs108ReadData.dataValues[1]) {
                    case 0:
                        cs108BarcodeData.barcodePayloadEvent = BarcodePayloadEvents.BARCODE_DATA_READ;
                        byte[] dataValues = new byte[cs108ReadData.dataValues.length - 2];
                        System.arraycopy(cs108ReadData.dataValues, 2, dataValues, 0, dataValues.length);
                        if (true) appendToLog("BarStream: matched Barcode.read data is found with dataValues = " + byteArrayToString(dataValues) + ", mBarcodeToWrite.size() = " + mBarcodeToWrite.size());

                        BarcodeCommendTypes commandType = null;
                        if (mBarcodeToWrite.size() > 0) {
                            if (mBarcodeToWrite.get(0).downlinkResponsed) {
                                int count = 0; boolean matched = true;
                                if (mBarcodeToWrite.get(0).dataValues[0] == 0x1b) {
                                    commandType = BarcodeCommendTypes.COMMAND_COMMON;
                                    count = 1;
                                    if (true) appendToLog("BarStream: 0x1b, Common response with  count = " + count);
                                } else if (mBarcodeToWrite.get(0).dataValues[0] == 0x7E) {
                                    if (true) appendToLog("BarStream: 0x7E, Barcode response with 0x7E mBarcodeToWrite.get(0).dataValues[0] and response data = " + byteArrayToString(dataValues));
                                    matched = true;
                                    commandType = BarcodeCommendTypes.COMMAND_QUERY;
                                    int index = 0;
                                    while (dataValues.length - index >= 5 + 1) {
                                        if (dataValues[index+0] == 2 && dataValues[index+1] == 0 && dataValues[index+4] == 0x34) {
                                            int length = dataValues[index+2] * 256 + dataValues[index+3];
                                            if (dataValues.length - index >= length + 4 + 1) {
                                                matched = true;
                                                if (mBarcodeToWrite.get(0).dataValues[5] == 0x37 && length >= 5) {
                                                    matched = true;
                                                    int prefixLength = dataValues[index+6];
                                                    int suffixLength = 0;
                                                    if (dataValues.length - index >= 5 + 2 + prefixLength + 2 + 1) {
                                                        suffixLength = dataValues[index + 6 + prefixLength + 2];
                                                    }
                                                    if (dataValues.length - index >= 5 + 2 + prefixLength + 2 + suffixLength + 1) {
                                                        bytesBarcodePrefix = null;
                                                        bytesBarcodeSuffix = null;
                                                        if (dataValues[index+5] == 1) {
                                                            bytesBarcodePrefix = new byte[prefixLength];
                                                            System.arraycopy(dataValues, index + 7, bytesBarcodePrefix, 0, bytesBarcodePrefix.length);
                                                        }
                                                        if (dataValues[index + 6 + prefixLength + 1] == 1) {
                                                            bytesBarcodeSuffix = new byte[suffixLength];
                                                            System.arraycopy(dataValues, index + 7 + prefixLength + 2, bytesBarcodeSuffix, 0, bytesBarcodeSuffix.length);
                                                        }
                                                        if (true) appendToLog("BarStream: BarcodePrefix = " + byteArrayToString(bytesBarcodePrefix) + ", BarcodeSuffix = " + byteArrayToString(bytesBarcodeSuffix));
                                                    }
                                                    if (true) appendToLog("BarStream: prefixLength = " + prefixLength + ", suffixLength = " + suffixLength);
                                                } else if (mBarcodeToWrite.get(0).dataValues[5] == 0x47 && length > 1) {
                                                    appendToLog("versionNumber is detected with length = " + length);
                                                    matched = true;
                                                    byte[] byteVersion = new byte[length - 1];
                                                    System.arraycopy(dataValues, index + 5, byteVersion, 0, byteVersion.length);
                                                    String versionNumber;
                                                        try {
                                                            versionNumber = new String(byteVersion, "UTF-8");
                                                            appendToLog("BarStream: versionNumber = " + versionNumber + ", versionNumber.length = " + versionNumber.length());
                                                        } catch (Exception e) {
                                                            versionNumber = null;
                                                        }
                                                        strVersion = versionNumber;
                                                        if (true) appendToLog("BarStream: " + String.format("%02x", dataValues[index+6]) + " versionNumber = " + versionNumber + ", length = " + versionNumber.length());
                                                } else if (mBarcodeToWrite.get(0).dataValues[5] == 0x48 && length >= 5) {
                                                    if (dataValues[index+5] == mBarcodeToWrite.get(0).dataValues[6] && dataValues[index+6] == mBarcodeToWrite.get(0).dataValues[7]) {
                                                        matched = true; //for ESN, S/N or Date
                                                        byte[] byteSN = new byte[length - 3];
                                                        System.arraycopy(dataValues, index + 7, byteSN, 0, byteSN.length);
                                                        String serialNumber;
                                                        try {
                                                            serialNumber = new String(byteSN, "UTF-8");
                                                            int snLength = Integer.parseInt(serialNumber.substring(0, 2));
                                                            if (true)
                                                                appendToLog("BarStream: serialNumber = " + serialNumber + ", snLength = " + snLength + ", serialNumber.length = " + serialNumber.length());
                                                            if (snLength + 2 == serialNumber.length()) {
                                                                serialNumber = serialNumber.substring(2);
                                                            } else serialNumber = null;
                                                        } catch (Exception e) {
                                                            serialNumber = null;
                                                        }
                                                        if (dataValues[index+6] == (byte)0x32) strESN = serialNumber;
                                                        else if (dataValues[index+6] == (byte)0x33) strSerialNumber = serialNumber;
                                                        else if (dataValues[index+6] == (byte)0x34) strDate = serialNumber;
                                                        if (true) appendToLog("BarStream: " + String.format("%02x", dataValues[index+6]) + " serialNumber = " + serialNumber + ", length = " + serialNumber.length());
                                                    }
                                                } else if (mBarcodeToWrite.get(0).dataValues[5] == 0x44 && length >= 3) {
                                                    if (DEBUG) appendToLog("BarStream: dataValue = " + byteArrayToString(dataValues) + ", writeDataValue = " + byteArrayToString(mBarcodeToWrite.get(0).dataValues));
                                                    if (dataValues[index+5] == mBarcodeToWrite.get(0).dataValues[6] && dataValues[index+6] == mBarcodeToWrite.get(0).dataValues[7]) {
                                                        matched = true;
                                                        if (mBarcodeToWrite.get(0).dataValues[6] == 0x30 && mBarcodeToWrite.get(0).dataValues[7] == 0x30  && mBarcodeToWrite.get(0).dataValues[8] == 0x30) {
                                                            bBarcodeTriggerMode = dataValues[7];
                                                            if (dataValues[index + 7] == 0x30) {
                                                                appendToLog("BarStream: Reading mode is TRIGGER");
                                                            } else if (DEBUG)
                                                                appendToLog("BarStream: Reading mode = " + String.valueOf(dataValues[7]));
                                                            appendToLogView("BIn: Correct readingMode query response !!!");
                                                        }
                                                    } else if (DEBUG) {
                                                        matched = true;   //for debugging to skip any wrong response
                                                        appendToLog("BarStream: incorrect response !!!");
                                                        appendToLogView("BIn: incorrect readingMode query response !!!");
                                                    }
                                                   if (DEBUG) appendToLog("matched = " + matched);
                                                }
                                                index += (length + 5);
                                            } else break;
                                        } else index++;
                                    }
                                    if (matched) { if (DEBUG) appendToLog("Matched Query response"); }
                                    else { if (DEBUG) appendToLog("Mis-matched Query response"); }
                                } else {
                                    if (true) appendToLog("BarStream: Barcode response with mBarcodeToWrite.get(0).dataValues[0] =  Others");
                                    String strData = null;
                                    try {
                                        strData = new String(mBarcodeToWrite.get(0).dataValues, "UTF-8");
                                    } catch (Exception ex) {
                                        strData = "";
                                    }
                                    String findStr = "nls";
                                    int lastIndex = 0;
                                    while (lastIndex != -1) {
                                        lastIndex = strData.indexOf(findStr, lastIndex);
                                        if (lastIndex != -1) {
                                            count++;
                                            lastIndex += findStr.length();
                                        }
                                    }
                                    if (DEBUG) appendToLog("Setting strData = " + strData + ", count = " + count);
                                }
                                if (count != 0) {
                                    if (true) appendToLog("BarStream: count = " + count + ", data = " + byteArrayToString(dataValues));
                                    byte[] dataValuesNew = new byte[dataValues.length - count]; matched = false;
                                    int iCount = 0; int iNewIndex = 0;
                                    for (int k = 0; k < dataValues.length; k++) {
                                        boolean match06 = false;
                                        if (matched == false) {
                                            if (dataValues[k] == 0x06 || dataValues[k] == 0x15) { match06 = true; if (++iCount == count) matched = true; if (DEBUG) appendToLog("BarStream: WRONG PREFIX: matched with k = " + k); }
                                        }
                                        if (match06 == false && iNewIndex < dataValuesNew.length) {
                                            dataValuesNew[iNewIndex++] = dataValues[k]; // java.lang.ArrayIndexOutOfBoundsException: length=0; index=0
                                        }
                                    }
                                    if (true) appendToLog("BarStream: matched = " + matched + ", new data = " + byteArrayToString(dataValuesNew));
                                    if (DEBUG) appendToLog("WRONG PREFIX: matched " + matched + ", iNewIndex = " + iNewIndex + ", dataValuesNew = " + byteArrayToString(dataValuesNew));
                                    if (matched == false) cs108BarcodeData.dataValues = dataValues;
                                    else if (iNewIndex != 0) cs108BarcodeData.dataValues = dataValuesNew;
                                    else cs108BarcodeData.dataValues = null;
                                    if (cs108BarcodeData.dataValues != null) {
                                        mBarcodeDevice.mBarcodeToRead.add(cs108BarcodeData);
                                        if (true) appendToLog("BarStream: mBarcodeToRead is added with mBarcodeToWrite.size() = " + mBarcodeToWrite.size() + ", dataValues = " + byteArrayToString(dataValues));
                                    }
                                }
                                if (matched) {
                                    found = true;
                                    mBarcodeToWrite.remove(0);
                                    sendDataToWriteSent = 0;
                                    mDataToWriteRemoved = true;
                                    if (true) appendToLog("BarStream: matched response command");
                                }
                                break;
                            }
                        }
                        for (int i=0; false && commandType == null && i < dataValues.length; i++) {
                            if (dataValues[i] == 0x28 || dataValues[i] == 0x29    //  ( )
                            || dataValues[i] == 0x5B || dataValues[i] == 0x5D || dataValues[i] == 0x5C
                            || dataValues[i] == 0x7B || dataValues[i] == 0x7D
                                    ) dataValues[i] = 0x20;
                        }
                        cs108BarcodeData.dataValues = dataValues;
                        mBarcodeDevice.mBarcodeToRead.add(cs108BarcodeData);
                        if (true) appendToLog("BarStream: mBarcodeToRead is added with dataValues = " + byteArrayToString(dataValues));
                        found = true;
                        break;
                    case 1:
                        if (true) appendToLog("BarStream: matched Barcode.good data is found");
                        cs108BarcodeData.barcodePayloadEvent = BarcodePayloadEvents.BARCODE_GOOD_READ;
                        cs108BarcodeData.dataValues = null;
                        mBarcodeDevice.mBarcodeToRead.add(cs108BarcodeData);
                        found = true;
                        break;
                }
            }
            if (DEBUG_BTDATA && found)  appendToLog("found Barcode.read data = " + byteArrayToString(cs108ReadData.dataValues));
            return found;
        }
    }

    enum NotificationPayloadEvents {
        NOTIFICATION_GET_BATTERY_VOLTAGE, NOTIFICATION_GET_TRIGGER_STATUS,
        NOTIFICATION_AUTO_BATTERY_VOLTAGE, NOTIFICATION_STOPAUTO_BATTERY_VOLTAGE,
        NOTIFICATION_AUTO_RFIDINV_ABORT, NOTIFICATION_GET_AUTO_RFIDINV_ABORT,
        NOTIFICATION_AUTO_BARINV_STARTSTOP, NOTIFICATION_GET_AUTO_BARINV_STARTSTOP,
        NOTIFICATION_AUTO_TRIGGER_REPORT, NOTIFICATION_STOP_TRIGGER_REPORT,

        NOTIFICATION_BATTERY_FAILED, NOTIFICATION_BATTERY_ERROR,
        NOTIFICATION_TRIGGER_PUSHED, NOTIFICATION_TRIGGER_RELEASED
    }
    class Cs108NotificatiionData {
        NotificationPayloadEvents notificationPayloadEvent;
        byte[] dataValues;
    }

    public interface NotificationListener { void onChange(); }
    class NotificationDevice {
        NotificationListener listener;
        void setNotificationListener0(NotificationListener listener) { this.listener = listener; }

        //NotificationListener getListener() { return listener; }
        boolean mTriggerStatus;
        boolean getTriggerStatus() { return mTriggerStatus; }
        void setTriggerStatus(boolean mTriggerStatus) { this.mTriggerStatus = mTriggerStatus; if (listener != null) listener.onChange(); }

        boolean mAutoRfidAbortStatus = true, mAutoRfidAbortStatusUpdate = false;
        boolean getAutoRfidAbortStatus() {
            if (mAutoRfidAbortStatusUpdate == false) {
                Cs108NotificatiionData cs108NotificatiionData = new Cs108NotificatiionData();
                cs108NotificatiionData.notificationPayloadEvent = NotificationPayloadEvents.NOTIFICATION_GET_AUTO_RFIDINV_ABORT;
                mNotificationDevice.mNotificationToWrite.add(cs108NotificatiionData);
            }
            return mAutoRfidAbortStatus;
        }
        void setAutoRfidAbortStatus(boolean mAutoRfidAbortStatus) { this.mAutoRfidAbortStatus = mAutoRfidAbortStatus; mAutoRfidAbortStatusUpdate = true; }

        boolean mAutoBarStartStopStatus = false, mAutoBarStartStopStatusUpdated = false;
        boolean getAutoBarStartStopStatus() {
            if (mAutoBarStartStopStatusUpdated == false) {
                Cs108NotificatiionData cs108NotificatiionData = new Cs108NotificatiionData();
                cs108NotificatiionData.notificationPayloadEvent = NotificationPayloadEvents.NOTIFICATION_GET_AUTO_BARINV_STARTSTOP;
                mNotificationDevice.mNotificationToWrite.add(cs108NotificatiionData);
            }
            return mAutoBarStartStopStatus;
        }
        void setAutoBarStartStopStatus(boolean mAutoBarStartStopStatus) { this.mAutoBarStartStopStatus = mAutoBarStartStopStatus; mAutoBarStartStopStatusUpdated = true; }

        ArrayList<Cs108NotificatiionData> mNotificationToWrite = new ArrayList<>();
        ArrayList<Cs108NotificatiionData> mNotificationToRead = new ArrayList<>();

        private boolean arrayTypeSet(byte[] dataBuf, int pos, NotificationPayloadEvents event) {
            boolean validEvent = false;
            switch (event) {
                case NOTIFICATION_GET_BATTERY_VOLTAGE:
                    validEvent = true;
                    break;
                case NOTIFICATION_GET_TRIGGER_STATUS:
                    dataBuf[pos] = 1;
                    validEvent = true;
                    break;
                case NOTIFICATION_AUTO_BATTERY_VOLTAGE:
                    if (checkHostProcessorVersion(mSiliconLabIcDevice.getSiliconLabIcVersion(), 1, 0, 2) == false) {
                        validEvent = false;
                    } else {
                        dataBuf[pos] = 2;
                        validEvent = true;
                    }
                    break;
                case NOTIFICATION_STOPAUTO_BATTERY_VOLTAGE:
                    if (checkHostProcessorVersion(mSiliconLabIcDevice.getSiliconLabIcVersion(), 1, 0, 2) == false) {
                        validEvent = false;
                    } else {
                        dataBuf[pos] = 3;
                        validEvent = true;
                    }
                    break;
                case NOTIFICATION_AUTO_RFIDINV_ABORT:
                    if (checkHostProcessorVersion(mBluetoothConnector.mBluetoothIcDevice.getBluetoothIcVersion(), 1, 0, 13) == false) {
                        validEvent = false;
                    } else {
                        dataBuf[pos] = 4;
                        validEvent = true;
                    }
                    break;
                case NOTIFICATION_GET_AUTO_RFIDINV_ABORT:
                    if (checkHostProcessorVersion(mBluetoothConnector.mBluetoothIcDevice.getBluetoothIcVersion(), 1, 0, 13) == false) {
                        validEvent = false;
                    } else {
                        dataBuf[pos] = 5;
                        validEvent = true;
                    }
                    break;
                case NOTIFICATION_AUTO_BARINV_STARTSTOP:
                    if (checkHostProcessorVersion(mBluetoothConnector.mBluetoothIcDevice.getBluetoothIcVersion(), 1, 0, 14) == false) {
                        validEvent = false;
                    } else {
                        dataBuf[pos] = 6;
                        validEvent = true;
                    }
                    break;
                case NOTIFICATION_GET_AUTO_BARINV_STARTSTOP:
                    if (false && checkHostProcessorVersion(mBluetoothConnector.mBluetoothIcDevice.getBluetoothIcVersion(), 1, 0, 14) == false) {
                        validEvent = false;
                    } else {
                        dataBuf[pos] = 7;
                        validEvent = true;
                    }
                    break;
                case NOTIFICATION_AUTO_TRIGGER_REPORT:
                    if (checkHostProcessorVersion(mBluetoothConnector.mBluetoothIcDevice.getBluetoothIcVersion(), 1, 0, 16) == false) {
                        validEvent = false;
                    } else {
                        dataBuf[pos] = 8;
                        validEvent = true;
                    }
                    break;
                case NOTIFICATION_STOP_TRIGGER_REPORT:
                    if (checkHostProcessorVersion(mBluetoothConnector.mBluetoothIcDevice.getBluetoothIcVersion(), 1, 0, 16) == false) {
                        validEvent = false;
                    } else {
                        dataBuf[pos] = 9;
                        validEvent = true;
                    }
                    break;
            }
            return validEvent;
        }

        private boolean writeNotification(Cs108NotificatiionData data) {
            int datalength = 0;
            if (data.dataValues != null)    datalength = data.dataValues.length;
            byte[] dataOutRef = new byte[]{(byte) 0xA7, (byte) 0xB3, 2, (byte) 0xD9, (byte) 0x82, (byte) 0x37, 0, 0, (byte) 0xA0, 0};

            byte[] dataOut = new byte[10 + datalength];
            if (DEBUG) appendToLog("event = " + data.notificationPayloadEvent.toString() + ", with datalength = " + datalength);
            if (datalength != 0) {
                System.arraycopy(data.dataValues, 0, dataOut, 10, datalength);
                dataOutRef[2] += datalength;
            }
            System.arraycopy(dataOutRef, 0, dataOut, 0, dataOutRef.length);
            if (arrayTypeSet(dataOut, 9, data.notificationPayloadEvent)) {
                if (DEBUG) appendToLog(byteArrayToString(dataOut));
                return writeData(dataOut, 0);
            }
            return false;
        }

        private boolean isMatchNotificationToWrite(Cs108ReadData cs108ReadData) {
            boolean match = false;
            if (mNotificationToWrite.size() != 0) {
                byte[] dataInCompare = new byte[]{(byte) 0xA0, 0};
                if (arrayTypeSet(dataInCompare, 1, mNotificationToWrite.get(0).notificationPayloadEvent) && (cs108ReadData.dataValues.length >= dataInCompare.length + 1)) {
                    if (match = compareArray(cs108ReadData.dataValues, dataInCompare, dataInCompare.length)) {
                        if (DEBUG_BTDATA) appendToLog("found Notification.read data = " + byteArrayToString(cs108ReadData.dataValues));
                        if (mNotificationToWrite.get(0).notificationPayloadEvent == NotificationPayloadEvents.NOTIFICATION_GET_BATTERY_VOLTAGE) {
                            if (cs108ReadData.dataValues.length >= dataInCompare.length + 2) {
                                mCs108ConnectorData.mVoltageValue = (cs108ReadData.dataValues[2] & 0xFF) * 256 + (cs108ReadData.dataValues[3] & 0xFF);
                                mCs108ConnectorData.mVoltageCount++;
                            }
                        } else if (mNotificationToWrite.get(0).notificationPayloadEvent == NotificationPayloadEvents.NOTIFICATION_GET_TRIGGER_STATUS) {
                            if (cs108ReadData.dataValues[2] != 0) {
                                setTriggerStatus(true); //mTriggerStatus = true;
                                mCs108ConnectorData.triggerButtonStatus = true;
                            } else {
                                setTriggerStatus(false); //mTriggerStatus = false;
                                mCs108ConnectorData.triggerButtonStatus = false;
                            }
                            mCs108ConnectorData.iTriggerCount++;
                            if (DEBUG) appendToLog("BARTRIGGER: isMatchNotificationToWrite finds trigger = " + getTriggerStatus());
                        } else if (mNotificationToWrite.get(0).notificationPayloadEvent == NotificationPayloadEvents.NOTIFICATION_GET_AUTO_RFIDINV_ABORT) {
                            if (cs108ReadData.dataValues[2] != 0) setAutoRfidAbortStatus(true);
                            else setAutoRfidAbortStatus(false);
                            if (DEBUG) appendToLog("AUTORFIDABORT: isMatchNotificationToWrite finds autoRfidAbort = " + getAutoRfidAbortStatus());
                        } else if (mNotificationToWrite.get(0).notificationPayloadEvent == NotificationPayloadEvents.NOTIFICATION_GET_AUTO_BARINV_STARTSTOP) {
                            if (cs108ReadData.dataValues[2] != 0) setAutoBarStartStopStatus(true);
                            else setAutoBarStartStopStatus(false);
                            if (DEBUG) appendToLog("AUTOBARSTARTSTOP: isMatchNotificationToWrite finds autoBarStartStop = " + getAutoBarStartStopStatus());
                        } else {
                        }
                        mNotificationToWrite.remove(0); sendDataToWriteSent = 0;
                    }
                }
            }
            return match;
        }

        private int sendDataToWriteSent = 0;
        private boolean sendNotificationToWrite() {
            if (mNotificationToWrite.size() != 0) {
                if (isBleConnected() == false) {
                    mNotificationToWrite.clear();
                } else if (System.currentTimeMillis() - btSendTime > btSendTimeOut) {
                    if (sendDataToWriteSent >= 5) {
                        int oldSize = mNotificationToWrite.size();
                        mNotificationToWrite.remove(0); sendDataToWriteSent = 0;
                        if (DEBUG) appendToLog("Removed after sending count-out with oldSize = " + oldSize + ", updated mNotificationToWrite.size() = " + mNotificationToWrite.size());
                        if (DEBUG) appendToLog("Removed after sending count-out.");
                        Toast.makeText(context, "Problem in sending data to Notification Module. Removed data sending after count-out", Toast.LENGTH_SHORT).show();
                    } else {
                        if (DEBUG) appendToLog("size = " + mNotificationToWrite.size());
                        boolean retValue = writeNotification(mNotificationToWrite.get(0));
                        if (retValue) {
                            sendDataToWriteSent++;
                        } else {
                            if (DEBUG) appendToLog("failure to send " + mNotificationToWrite.get(0).notificationPayloadEvent.toString());
                            mNotificationToWrite.remove(0);
                        }
                    }
                }
                return true;
            }
            return false;
        }

        long timeTriggerRelease;
        private boolean isNotificationToRead(Cs108ReadData cs108ReadData) {
            boolean found = false;
            if (cs108ReadData.dataValues[0] == (byte) 0xA0 && cs108ReadData.dataValues[1] == (byte) 0x00 && cs108ReadData.dataValues.length >= 4) {
                mCs108ConnectorData.mVoltageValue = (cs108ReadData.dataValues[2] & 0xFF) * 256 + (cs108ReadData.dataValues[3] & 0xFF);
                mCs108ConnectorData.mVoltageCount++;
                found = true;
            } else if (cs108ReadData.dataValues[0] == (byte) 0xA0 && cs108ReadData.dataValues[1] == (byte) 0x01 && cs108ReadData.dataValues.length >= 3) {
                if (cs108ReadData.dataValues[2] == 0) mCs108ConnectorData.triggerButtonStatus = false;
                else mCs108ConnectorData.triggerButtonStatus = true;
                mCs108ConnectorData.iTriggerCount++;
                found = true;
            } else if (cs108ReadData.dataValues[0] == (byte) 0xA1) {
                Cs108NotificatiionData cs108NotificatiionData = new Cs108NotificatiionData();
                switch (cs108ReadData.dataValues[1]) {
                    case 0:
                        if (DEBUG) appendToLog("matched batteryFailed data is found.");
                        cs108NotificatiionData.notificationPayloadEvent = NotificationPayloadEvents.NOTIFICATION_BATTERY_FAILED;
                        cs108NotificatiionData.dataValues = null;
                        if (false) mNotificationDevice.mNotificationToRead.add(cs108NotificatiionData);
                        found = true;
                        break;
                    case 1:
                        if (DEBUG) appendToLog("matched Error data is found, " + byteArrayToString(cs108ReadData.dataValues));
                        cs108NotificatiionData.notificationPayloadEvent = NotificationPayloadEvents.NOTIFICATION_BATTERY_ERROR;
                        byte[] dataValues = new byte[cs108ReadData.dataValues.length - 2];
                        System.arraycopy(cs108ReadData.dataValues, 2, dataValues, 0, dataValues.length);
                        cs108NotificatiionData.dataValues = dataValues;
                        if (true) mNotificationDevice.mNotificationToRead.add(cs108NotificatiionData);
                        appendToLog("endingMessage: found A101");
                        btSendTime = System.currentTimeMillis() - btSendTimeOut + 50;
                        found = true;
                        break;
                    case 2:
                        cs108NotificatiionData.notificationPayloadEvent = NotificationPayloadEvents.NOTIFICATION_TRIGGER_PUSHED;
                        cs108NotificatiionData.dataValues = null;
                        setTriggerStatus(true); //mTriggerStatus = true;
                        if (DEBUG) appendToLog("BARTRIGGER: isNotificationToRead finds trigger = " + getTriggerStatus());
                        if (false) mNotificationDevice.mNotificationToRead.add(cs108NotificatiionData);
                        found = true;
                        break;
                    case 3:
                        cs108NotificatiionData.notificationPayloadEvent = NotificationPayloadEvents.NOTIFICATION_TRIGGER_RELEASED;
                        cs108NotificatiionData.dataValues = null;
                        //if (System.currentTimeMillis() - timeTriggerRelease > 800) {
                        //    timeTriggerRelease = System.currentTimeMillis();
                            setTriggerStatus(false);    //mTriggerStatus = false;
                        //}
                        if (DEBUG) appendToLog("BARTRIGGER: isNotificationToRead finds trigger = " + getTriggerStatus());
                        if (false) mNotificationDevice.mNotificationToRead.add(cs108NotificatiionData);
                        found = true;
                        break;
                }
            }
            if (DEBUG_BTDATA && found) appendToLog("found Notification.read data = " + byteArrayToString(cs108ReadData.dataValues));
            return found;
        }
    }

    enum SiliconLabIcPayloadEvents {
        GET_VERSION, GET_SERIALNUMBER, GET_MODELNAME, RESET
    }
    class Cs108SiliconLabIcReadData {
        SiliconLabIcPayloadEvents siliconLabIcPayloadEvent;
        byte[] dataValues;
    }
    class SiliconLabIcDevice {
        private byte[] mSiliconLabIcVersion = new byte[]{-1, -1, -1};

        String getSiliconLabIcVersion() {
            if (mSiliconLabIcVersion[0] == -1) {
                boolean repeatRequest = false;
                if (mSiliconLabIcToWrite.size() != 0) {
                    if (mSiliconLabIcToWrite.get(mSiliconLabIcToWrite.size() - 1) == SiliconLabIcPayloadEvents.GET_VERSION) {
                        repeatRequest = true;
                    }
                }
                if (repeatRequest == false) {
                    mSiliconLabIcToWrite.add(SiliconLabIcPayloadEvents.GET_VERSION);
                }
                return "";
            } else {
                return String.valueOf(mSiliconLabIcVersion[0]) + "." + String.valueOf(mSiliconLabIcVersion[1]) + "." + String.valueOf(mSiliconLabIcVersion[2]);
            }
        }

        private byte[] serialNumber = new byte[16];
        String getSerialNumber() {
            if (serialNumber[0] == 0) {
                boolean repeatRequest = false;
                if (mSiliconLabIcToWrite.size() != 0) {
                    if (mSiliconLabIcToWrite.get(mSiliconLabIcToWrite.size() - 1) == SiliconLabIcPayloadEvents.GET_SERIALNUMBER) {
                        repeatRequest = true;
                    }
                }
                if (repeatRequest == false) {
                    mSiliconLabIcToWrite.add(SiliconLabIcPayloadEvents.GET_SERIALNUMBER);
                }
                return "";
            } else {
                if (serialNumber[15] == 0) {
                    serialNumber[15] = serialNumber[14];
                    serialNumber[14] = serialNumber[13];
                    serialNumber[13] = 0;
                }
                for (int i = 13; i < 16; i++) {
                    if (serialNumber[i] == 0) serialNumber[i] = 0x30;
                }
                if (true) return byteArray2DisplayString(serialNumber);
                String str = null;
                try {
                    str = new String(serialNumber, "UTF-8");
                    str = str.trim();
                } catch (UnsupportedEncodingException e) {
                    str = null;
                    e.printStackTrace();
                }
                return str;
            }
        }

        private byte[] modelName = new byte[16];
        String getModelName() {
            appendToLog("modelName = " + byteArrayToString(modelName));
            if (modelName[0] == 0) {
                boolean repeatRequest = false;
                if (mSiliconLabIcToWrite.size() != 0) {
                    if (mSiliconLabIcToWrite.get(mSiliconLabIcToWrite.size() - 1) == SiliconLabIcPayloadEvents.GET_MODELNAME) {
                        repeatRequest = true;
                    }
                }
                if (repeatRequest == false) {
                    mSiliconLabIcToWrite.add(SiliconLabIcPayloadEvents.GET_MODELNAME);
                }
                return "";
            } else if (true) return byteArray2DisplayString(modelName);
            else {
                String str = "";
                try {
                    str = new String(modelName, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return str;
            }
        }
        String byteArray2DisplayString(byte[] byteData) {
            if (false) appendToLog("String0 = " + byteArrayToString(byteData));
            String str = "";
            try {
                str = new String(byteData, "UTF-8");
                str = str.replaceAll("[^\\x00-\\x7F]", "");
                str = str.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (false) appendToLog("String1 = " + str);
            return str;
        }

        ArrayList<SiliconLabIcPayloadEvents> mSiliconLabIcToWrite = new ArrayList<>();

        private boolean arrayTypeSet(byte[] dataBuf, int pos, SiliconLabIcPayloadEvents event) {
            boolean validEvent = false;
            switch (event) {
                case GET_VERSION:
                    validEvent = true;
                    break;
                case GET_SERIALNUMBER:
                    dataBuf[pos] = 4;
                    validEvent = true;
                    break;
                case GET_MODELNAME:
                    dataBuf[pos] = 6;
                    validEvent = true;
                    break;
                case RESET:
                    dataBuf[pos] = 12;
                    validEvent = true;
                    break;
            }
            return validEvent;
        }

        private boolean writeSiliconLabIc(SiliconLabIcPayloadEvents event) {
            byte[] dataOut = new byte[]{(byte) 0xA7, (byte) 0xB3, 2, (byte) 0xE8, (byte) 0x82, (byte) 0x37, 0, 0, (byte) 0xB0, 0};
            if (event == SiliconLabIcPayloadEvents.GET_SERIALNUMBER) {
                dataOut = new byte[]{(byte) 0xA7, (byte) 0xB3, 3, (byte) 0xE8, (byte) 0x82, (byte) 0x37, 0, 0, (byte) 0xB0, 4, 0};
            } else if (event == SiliconLabIcPayloadEvents.GET_MODELNAME) {
                dataOut = new byte[]{(byte) 0xA7, (byte) 0xB3, 2, (byte) 0xE8, (byte) 0x82, (byte) 0x37, 0, 0, (byte) 0xB0, 6};
            } else if (event == SiliconLabIcPayloadEvents.RESET) {
                dataOut = new byte[]{(byte) 0xA7, (byte) 0xB3, 2, (byte) 0xE8, (byte) 0x82, (byte) 0x37, 0, 0, (byte) 0xB0, 12};
            }
            if (DEBUG) appendToLog(byteArrayToString(dataOut));
            return writeData(dataOut, 0);
        }

        private boolean isMatchSiliconLabIcToWrite(Cs108ReadData cs108ReadData) {
            boolean match = false;
            if (mSiliconLabIcToWrite.size() != 0) {
                byte[] dataInCompare = new byte[]{(byte) 0xB0, 0};
                if (arrayTypeSet(dataInCompare, 1, mSiliconLabIcToWrite.get(0)) && (cs108ReadData.dataValues.length >= dataInCompare.length + 1)) {
                    if (match = compareArray(cs108ReadData.dataValues, dataInCompare, dataInCompare.length)) {
                        if (true) appendToLog("found SiliconLabIc.read data = " + byteArrayToString(cs108ReadData.dataValues));
                        if (mSiliconLabIcToWrite.get(0) == SiliconLabIcPayloadEvents.GET_VERSION) {
                            if (cs108ReadData.dataValues.length >= 2 + mSiliconLabIcVersion.length) {
                                System.arraycopy(cs108ReadData.dataValues, 2, mSiliconLabIcVersion, 0, mSiliconLabIcVersion.length);
                                if (DEBUG) appendToLog("matched mSiliconLabIc.GetVersion.reply data is found with mSiliconLabIcToWrite.size=" + mSiliconLabIcToWrite.size() + ", version=" + byteArrayToString(mSiliconLabIcVersion));
                            }
                        } else if (mSiliconLabIcToWrite.get(0) == SiliconLabIcPayloadEvents.GET_SERIALNUMBER) {
                            int length = cs108ReadData.dataValues.length - 2;
                            if (length > serialNumber.length) length = serialNumber.length;
                            System.arraycopy(cs108ReadData.dataValues, 2, serialNumber, 0, length);
                            if (DEBUG) appendToLog("matched mSiliconLabIc.GetSerialNumber.reply data is found: " + byteArrayToString(cs108ReadData.dataValues));
                        } else if (mSiliconLabIcToWrite.get(0) == SiliconLabIcPayloadEvents.GET_MODELNAME) {
                            int length = cs108ReadData.dataValues.length - 2;
                            if (length > modelName.length) length = modelName.length;
                            System.arraycopy(cs108ReadData.dataValues, 2, modelName, 0, length);
                            if (DEBUG) appendToLog("matched mSiliconLabIc.GetModelName.reply data is found: " + byteArrayToString(cs108ReadData.dataValues));
                        } else if (mSiliconLabIcToWrite.get(0) == SiliconLabIcPayloadEvents.RESET) {
                            if (cs108ReadData.dataValues[2] != 0) {
                                if (DEBUG) appendToLog("Silicon Lab RESET is found with error");
                            } else if (DEBUG) appendToLog("matched SiliconLab.reply data is found");
                        } else {
                            if (DEBUG) appendToLog("matched mSiliconLabIc.Other.reply data is found.");
                        }
                        mSiliconLabIcToWrite.remove(0); sendDataToWriteSent = 0;
                    }
                }
            }
            return match;
        }

        private int sendDataToWriteSent = 0;
        private boolean sendSiliconLabIcToWrite() {
            if (mSiliconLabIcToWrite.size() != 0) {
                if (isBleConnected() == false) {
                    mSiliconLabIcToWrite.clear();
                } else if (System.currentTimeMillis() - btSendTime > btSendTimeOut) {
                    if (sendDataToWriteSent >= 5) {
                        int oldSize = mSiliconLabIcToWrite.size();
                        mSiliconLabIcToWrite.remove(0); sendDataToWriteSent = 0;
                        if (DEBUG) appendToLog("Removed after sending count-out with oldSize = " + oldSize + ", updated mSiliconLabIcToWrite.size() = " + mSiliconLabIcToWrite.size());
                        if (DEBUG) appendToLog("Removed after sending count-out.");
                        Toast.makeText(context, "Problem in sending data to SiliconLabIc Module. Removed data sending after count-out", Toast.LENGTH_SHORT).show();
                    } else {
                        if (DEBUG) appendToLog("size = " + mSiliconLabIcToWrite.size());
                        boolean retValue = writeSiliconLabIc(mSiliconLabIcToWrite.get(0));
                        if (retValue) {
                            sendDataToWriteSent++;
                        } else {
                            appendToLogView("failure to send " + mSiliconLabIcToWrite.get(0).toString());
                            mSiliconLabIcToWrite.remove(0);
                        }
                    }
                }
                return true;
            }
            return false;
        }
    }

    enum ControlCommands {
        NULL,
        CANCEL, SOFTRESET, ABORT, PAUSE, RESUME, GETSERIALNUMBER, RESETTOBOOTLOADER
    }

    enum HostRegRequests {
        MAC_OPERATION,
        //MAC_VER, MAC_LAST_COMMAND_DURATION,
        //HST_CMNDIAGS,
        //HST_MBP_ADDR, HST_MBP_DATA,
        //HST_OEM_ADDR, HST_OEM_DATA,
        HST_ANT_CYCLES, HST_ANT_DESC_SEL, HST_ANT_DESC_CFG, MAC_ANT_DESC_STAT, HST_ANT_DESC_PORTDEF, HST_ANT_DESC_DWELL, HST_ANT_DESC_RFPOWER, HST_ANT_DESC_INV_CNT,
        HST_TAGMSK_DESC_SEL, HST_TAGMSK_DESC_CFG, HST_TAGMSK_BANK, HST_TAGMSK_PTR, HST_TAGMSK_LEN, HST_TAGMSK_0_3,
        HST_QUERY_CFG, HST_INV_CFG, HST_INV_SEL, HST_INV_ALG_PARM_0, HST_INV_ALG_PARM_1, HST_INV_ALG_PARM_2, HST_INV_ALG_PARM_3, HST_INV_EPC_MATCH_CFG, HST_INV_EPCDAT_0_3,
        HST_TAGACC_DESC_CFG, HST_TAGACC_BANK, HST_TAGACC_PTR, HST_TAGACC_CNT, HST_TAGACC_LOCKCFG, HST_TAGACC_ACCPWD, HST_TAGACC_KILLPWD, HST_TAGWRDAT_SEL, HST_TAGWRDAT_0,
        HST_RFTC_CURRENT_PROFILE,
        HST_RFTC_FRQCH_SEL, HST_RFTC_FRQCH_CFG, HST_RFTC_FRQCH_DESC_PLLDIVMULT, HST_RFTC_FRQCH_DESC_PLLDACCTL, HST_RFTC_FRQCH_CMDSTART,
        HST_AUTHENTICATE_CFG, HST_AUTHENTICATE_MSG, HST_READBUFFER_LEN, HST_UNTRACEABLE_CFG,
        HST_CMD
    }

    public enum HostCommands {
        NULL, CMD_WROEM, CMD_RDOEM, CMD_ENGTEST, CMD_MBPRDREG, CMD_MBPWRREG,
        CMD_18K6CINV, CMD_18K6CREAD, CMD_18K6CWRITE, CMD_18K6CLOCK, CMD_18K6CKILL, CMD_SETPWRMGMTCFG,
        CMD_UPDATELINKPROFILE,
        CMD_18K6CBLOCKWRITE,
        CMD_CHANGEEAS, CMD_GETSENSORDATA,
        CMD_AUTHENTICATE, CMD_READBUFFER, CMD_UNTRACEABLE,
        CMD_FDM_RDMEM, CMD_FDM_WRMEM, CMD_FDM_AUTH, CMD_FDM_GET_TEMPERATURE, CMD_FDM_START_LOGGING, CMD_FDM_STOP_LOGGING,
        CMD_FDM_WRREG, CMD_FDM_RDREG, CMD_FDM_DEEP_SLEEP, CMD_FDM_OPMODE_CHECK, CMD_FDM_INIT_REGFILE, CMD_FDM_LED_CTRL,
    }

    public enum HostCmdResponseTypes {
        NULL,
        TYPE_COMMAND_BEGIN,
        TYPE_COMMAND_END,
        TYPE_18K6C_INVENTORY, TYPE_18K6C_INVENTORY_COMPACT,
        TYPE_18K6C_TAG_ACCESS,
        TYPE_ANTENNA_CYCLE_END,
        TYPE_COMMAND_ACTIVE
    }

    public class Rx000pkgData {
        public HostCmdResponseTypes responseType;
        public int flags;
        public byte[] dataValues;
        public long decodedTime;
        public double decodedRssi;
        public int decodedPhase, decodedChidx, decodedPort;
        public byte[] decodedPc, decodedEpc, decodedCrc, decodedData1, decodedData2;
        public String decodedResult;
        public String decodedError;
    }
    class Rx000Setting {
        Rx000Setting(boolean set_default_setting) {
            if (set_default_setting) {
                macVer = mDefault.macVer;
                diagnosticCfg = mDefault.diagnosticCfg;
                oemAddress = mDefault.oemAddress;

                //RFTC block paramters
                currentProfile = mDefault.currentProfile;

                // Antenna block parameters
                antennaCycle = mDefault.antennaCycle;
                antennaFreqAgile = mDefault.antennaFreqAgile;
                antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect);
            }
            antennaSelectedData = new AntennaSelectedData[ANTSELECT_MAX + 1];
            for (int i = 0; i < antennaSelectedData.length; i++) {
                int default_setting_type = 0;
                if (set_default_setting) {
                    if (i == 0) default_setting_type = 1;
                    else if (i >= 1 && i <= 3)  default_setting_type = 2;
                    else if (i >= 4 && i <= 7)  default_setting_type = 3;
                    else if (i >= 8 && i <= 11) default_setting_type = 4;
                    else    default_setting_type = 5;
                }
                antennaSelectedData[i] = new AntennaSelectedData(set_default_setting, default_setting_type);
            }

            //Tag select block parameters
            if (set_default_setting)    invSelectIndex = 0;
            invSelectData = new InvSelectData[INVSELECT_MAX + 1];
            for (int i = 0; i < invSelectData.length; i++) {
                invSelectData[i] = new InvSelectData(set_default_setting);
            }

            if (set_default_setting) {
                //Inventtory block paraameters
                queryTarget = mDefault.queryTarget;
                querySession = mDefault.querySession;
                querySelect = mDefault.querySelect;
                invAlgo = mDefault.invAlgo; appendToLog("Hello6: invAlgo = " + invAlgo + ", queryTarget = " + queryTarget);
                matchRep = mDefault.matchRep;
                tagSelect = mDefault.tagSelect;
                noInventory = mDefault.noInventory;
                tagDelay = mDefault.tagDelay;
                invModeCompact = mDefault.tagJoin;
                invBrandId = mDefault.brandid;
            }

            if (set_default_setting)    algoSelect = 3;
            algoSelectedData = new AlgoSelectedData[ALGOSELECT_MAX + 1];
            for (int i = 0; i < algoSelectedData.length; i++) {//0 for invalid default,    1 for 0,    2 for 1,     3 for 2,   4 for 3
                int default_setting_type = 0;
                if (set_default_setting) {
                    default_setting_type = i + 1;
                }
                algoSelectedData[i] = new AlgoSelectedData(set_default_setting, default_setting_type);
            }

            if (set_default_setting) {
                matchEnable = mDefault.matchEnable;
                matchType = mDefault.matchType;
                matchLength = mDefault.matchLength;
                matchOffset = mDefault.matchOffset;
                invMatchDataReady = mDefault.invMatchDataReady;

                //Tag access block parameters
                accessRetry = mDefault.accessRetry;
                accessBank = mDefault.accessBank; accessBank2 = mDefault.accessBank2;
                accessOffset = mDefault.accessOffset; accessOffset2 = mDefault.accessOffset2;
                accessCount = mDefault.accessCount; accessCount2 = mDefault.accessCount2;
                accessLockAction = mDefault.accessLockAction;
                accessLockMask = mDefault.accessLockMask;
                //long accessPassword = 0;
                //long killPassword = 0;
                accessWriteDataSelect = mDefault.accessWriteDataSelect;
                accWriteDataReady = mDefault.accWriteDataReady;

                authMatchDataReady = mDefault.authMatchDataReady;
            }

            invMatchData0_63 = new byte[4 * 16];
            accWriteData0_63 = new byte[4 * 16 * 2];
            authMatchData0_63 = new byte[4 * 4];
        }

        class Rx000Setting_default {
            String macVer;
            int diagnosticCfg = 0x210;
            int mbpAddress = 0; // ?
            int mbpData = 0; // ?
            int oemAddress = 4; // ?
            int oemData = 0; // ?

            //RFTC block paramters
            int currentProfile = 1;
            int freqChannelSelect = 0;

            // Antenna block parameters
            int antennaCycle = 1;
            int antennaFreqAgile = 0;
            int antennaSelect = 0;

            //Tag select block parameters
            int invSelectIndex = 0;

            //Inventtory block paraameters
            int queryTarget = 0;
            int querySession = 2;
            int querySelect = 1;
            int invAlgo = 3;
            int matchRep = 0;
            int tagSelect = 0;
            int noInventory = 0;
            int tagRead = 0;
            int tagDelay = 0;
            int tagJoin = 0;
            int brandid = 0;
            int algoSelect = 3;

            int matchEnable = 0;
            int matchType = 0;
            int matchLength = 0;
            int matchOffset = 0;
            byte[] invMatchData0_63; int invMatchDataReady = 0;

            //Tag access block parameters
            int accessRetry = 3;
            int accessBank = 1; int accessBank2 = 0;
            int accessOffset = 2; int accessOffset2 = 0;
            int accessCount = 1; int accessCount2 = 0;
            int accessLockAction = 0;
            int accessLockMask = 0;
            //long accessPassword = 0;
            // long killPassword = 0;
            int accessWriteDataSelect = 0;
            byte[] accWriteData0_63; int accWriteDataReady = 0;

            byte[] authMatchData; int authMatchDataReady = 0;
        }
        Rx000Setting_default mDefault = new Rx000Setting_default();

        boolean readMAC(int address) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 0, 0, 0, 0, 0, 0};
            msgBuffer[2] = (byte) (address % 256);
            msgBuffer[3] = (byte) ((address >> 8) % 256);
            appendToLog("readMac buffer = " + byteArrayToString(msgBuffer));
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.MAC_OPERATION, false, msgBuffer);
        }
        boolean writeMAC(int address, long value) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 0, 0, 0, 0, 0, 0};
            msgBuffer[2] = (byte) (address % 256);
            msgBuffer[3] = (byte) ((address >> 8) % 256);
            msgBuffer[4] = (byte) (value % 256);
            msgBuffer[5] = (byte) ((value >> 8) % 256);
            msgBuffer[6] = (byte) ((value >> 16) % 256);
            msgBuffer[7] = (byte) ((value >> 24) % 256);
            appendToLog("writeMac buffer = " + byteArrayToString(msgBuffer));
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.MAC_OPERATION, true, msgBuffer);
        }

        String macVer = null; int macVerBuild = 0;
        String getMacVer() {
            if (macVer == null) {
                readMAC(0);
                return "";
            } else {
                return macVer;
            }
        }

        long mac_last_command_duration;
        long getMacLastCommandDuration(boolean request) {
            if (request) {
                if (true) readMAC(9);
                //byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 9, 0, 0, 0, 0, 0};
                //mRfidDevice.mRx000Device.sendHostRegRequest(HostRegRequests.MAC_LAST_COMMAND_DURATION, false, msgBuffer);
            }
            return mac_last_command_duration;
        }

        final int DIAGCFG_INVALID = -1; final int DIAGCFG_MIN = 0; final int DIAGCFG_MAX = 0x3FF;
        int diagnosticCfg = DIAGCFG_INVALID;
        int getDiagnosticConfiguration() {
            if (diagnosticCfg < DIAGCFG_MIN || diagnosticCfg > DIAGCFG_MAX) {
                if (true) readMAC(0x201);
                //byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 1, 2, 0, 0, 0, 0};
                //mRfidDevice.mRx000Device.sendHostRegRequest(HostRegRequests.HST_CMNDIAGS, false, msgBuffer);
            }
            return diagnosticCfg;
        }
        boolean setDiagnosticConfiguration(boolean bCommmandActive) {
//            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 1, 2, (byte)0x10, 0, 0, 0};
//            if (bCommmandActive) msgBuffer[5] |= 0x20;
            int diagnosticCfgNew;
            diagnosticCfgNew = 0x10; if (bCommmandActive) diagnosticCfgNew |= 0x20;
            appendToLog("diagnosticCfg = " + diagnosticCfg + ", diagnosticCfgNew = " + diagnosticCfgNew);
            if (diagnosticCfg == diagnosticCfgNew && sameCheck) return true;
            diagnosticCfg = diagnosticCfgNew;
            return writeMAC(0x201, diagnosticCfgNew); //mRfidDevice.mRx000Device.sendHostRegRequest(HostRegRequests.HST_CMNDIAGS, true, msgBuffer);
        }

        int impinjExtensionValue = -1;
        int getImpinjExtension() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                {
                    if (impinjExtensionValue < 0) readMAC(0x203);
                    return impinjExtensionValue;
                }
            }
        }
        boolean setImpinjExtension(boolean tagFocus) {
            int iValue = (tagFocus ? 0x10 : 0);
            boolean bRetValue = writeMAC(0x203, iValue);
            if (bRetValue) impinjExtensionValue = iValue;
            return bRetValue;
        }

        int pwrMgmtStatus = -1;
        void getPwrMgmtStatus() {
            appendToLog("pwrMgmtStatus: getPwrMgmtStatus ");
            pwrMgmtStatus = -1; readMAC(0x204);
        }

        final int MBPADDR_INVALID = -1; final int MBPADDR_MIN = 0; final int MBPADDR_MAX = 0x1FFF;
        long mbpAddress = MBPADDR_INVALID;
        boolean setMBPAddress(long mbpAddress) {
            //byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 0, 4, 0, 0, 0, 0};
            if (mbpAddress < MBPADDR_MIN || mbpAddress > MBPADDR_MAX) return false;
                //mbpAddress = mDefault.mbpAddress;
            if (this.mbpAddress == mbpAddress && sameCheck)  return true;
            //msgBuffer[4] = (byte) (mbpAddress % 256);
            //msgBuffer[5] = (byte) ((mbpAddress >> 8) % 256);
            this.mbpAddress = mbpAddress;
            appendToLog("Going to writeMAC");
            return writeMAC(0x400, (int) mbpAddress); //mRfidDevice.mRx000Device.sendHostRegRequest(HostRegRequests.HST_MBP_ADDR, true, msgBuffer);
        }

        final int MBPDATA_INVALID = -1; final int MBPDATA_MIN = 0; final int MBPDATA_MAX = 0x1FFF;
        long mbpData = MBPDATA_INVALID;
        boolean setMBPData(long mbpData) {
            //byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 1, 4, 0, 0, 0, 0};
            if (mbpData < MBPADDR_MIN || mbpData > MBPADDR_MAX) return false;
                //mbpData = mDefault.mbpData;
            if (this.mbpData == mbpData && sameCheck)  return true;
            //msgBuffer[4] = (byte) (mbpData % 256);
            //msgBuffer[5] = (byte) ((mbpData >> 8) % 256);
            this.mbpData = mbpData;
            return writeMAC(0x401, (int) mbpData); //mRfidDevice.mRx000Device.sendHostRegRequest(HostRegRequests.HST_MBP_DATA, true, msgBuffer);
        }

        final int OEMADDR_INVALID = -1; final int OEMADDR_MIN = 0; final int OEMADDR_MAX = 0x1FFF;
        long oemAddress = OEMADDR_INVALID;
        boolean setOEMAddress(long oemAddress) {
            //byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 0, 5, 0, 0, 0, 0};
            if (oemAddress < OEMADDR_MIN || oemAddress > OEMADDR_MAX) return false;
                //oemAddress = mDefault.oemAddress;
            if (this.oemAddress == oemAddress && sameCheck)  return true;
            //msgBuffer[4] = (byte) (oemAddress % 256);
            //msgBuffer[5] = (byte) ((oemAddress >> 8) % 256);
            this.oemAddress = oemAddress;
            return writeMAC(0x500, (int) oemAddress); //mRfidDevice.mRx000Device.sendHostRegRequest(HostRegRequests.HST_OEM_ADDR, true, msgBuffer);
        }

        final int OEMDATA_INVALID = -1; final int OEMDATA_MIN = 0; final int OEMDATA_MAX = 0x1FFF;
        long oemData = OEMDATA_INVALID;
        boolean setOEMData(long oemData) {
            //byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 1, 5, 0, 0, 0, 0};
            if (oemData < OEMADDR_MIN || oemData > OEMADDR_MAX) return false;
                //oemData = mDefault.oemData;
            if (this.oemData == oemData && sameCheck)  return true;
            //msgBuffer[4] = (byte) (oemData % 256);
            //msgBuffer[5] = (byte) ((oemData >> 8) % 256);
            this.oemData = oemData;
            return writeMAC(0x501, (int) oemData); //mRfidDevice.mRx000Device.sendHostRegRequest(HostRegRequests.HST_OEM_DATA, true, msgBuffer);
        }

        // Antenna block parameters
        final int ANTCYCLE_INVALID = -1; final int ANTCYCLE_MIN = 0; final int ANTCYCLE_MAX = 0xFFFF;
        int antennaCycle = ANTCYCLE_INVALID;
        int getAntennaCycle() {
            if (antennaCycle < ANTCYCLE_MIN || antennaCycle > ANTCYCLE_MAX) getHST_ANT_CYCLES();
            return antennaCycle;
        }
        boolean setAntennaCycle(int antennaCycle) {
            return setAntennaCycle(antennaCycle, antennaFreqAgile);
        }
        boolean setAntennaCycle(int antennaCycle, int antennaFreqAgile) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 0, 7, 0, 0, 0, 0};
            if (antennaCycle < ANTCYCLE_MIN || antennaCycle > ANTCYCLE_MAX) antennaCycle = mDefault.antennaCycle;
            if (antennaFreqAgile < FREQAGILE_MIN || antennaFreqAgile > FREQAGILE_MAX)   antennaFreqAgile = mDefault.antennaFreqAgile;
            if (this.antennaCycle == antennaCycle && this.antennaFreqAgile == antennaFreqAgile  && sameCheck) return true;
            msgBuffer[4] = (byte) (antennaCycle % 256);
            msgBuffer[5] = (byte) ((antennaCycle >> 8) % 256);
            if (antennaFreqAgile != 0) {
                msgBuffer[7] |= 0x01;
            }
            this.antennaCycle = antennaCycle;
            this.antennaFreqAgile = antennaFreqAgile;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_CYCLES, true, msgBuffer);
        }

        final int FREQAGILE_INVALID = -1; final int FREQAGILE_MIN = 0; final int FREQAGILE_MAX = 1;
        int antennaFreqAgile = FREQAGILE_INVALID;
        int getAntennaFreqAgile() {
            if (antennaFreqAgile < FREQAGILE_MIN || antennaFreqAgile > FREQAGILE_MAX)
                getHST_ANT_CYCLES();
            return antennaFreqAgile;
        }
        boolean setAntennaFreqAgile(int freqAgile) {
            return setAntennaCycle(antennaCycle, freqAgile);
        }

        private boolean getHST_ANT_CYCLES() {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 0, 7, 0, 0, 0, 0};
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_CYCLES, false, msgBuffer);
        }

        final int ANTSELECT_INVALID = -1; final int ANTSLECT_MIN = 0; final int ANTSELECT_MAX = 15;
        int antennaSelect = ANTSELECT_INVALID;  //default value = 0
        int getAntennaSelect() {
            appendToLog("AntennaSelect = " + antennaSelect);
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 1, 7, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_DESC_SEL, false, msgBuffer);
            }
            return antennaSelect;
        }
        boolean setAntennaSelect(int antennaSelect) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 1, 7, 0, 0, 0, 0};
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  antennaSelect = mDefault.antennaSelect;
            if (this.antennaSelect == antennaSelect && sameCheck) return true;
            this.antennaSelect = antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect);
            msgBuffer[4] = (byte) (antennaSelect);
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_DESC_SEL, true, msgBuffer);
        }

        AntennaSelectedData[] antennaSelectedData;
        int getAntennaEnable() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaEnable();
            }
        }
        boolean setAntennaEnable(int antennaEnable) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaEnable(antennaEnable);
        }
        boolean setAntennaEnable(int antennaEnable, int antennaInventoryMode, int antennaLocalAlgo, int antennaLocalStartQ,
                                        int antennaProfileMode, int antennaLocalProfile, int antennaFrequencyMode, int antennaLocalFrequency) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaEnable(antennaEnable, antennaInventoryMode, antennaLocalAlgo, antennaLocalStartQ,
                    antennaProfileMode, antennaLocalProfile, antennaFrequencyMode, antennaLocalFrequency);
        }

        int getAntennaInventoryMode() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaInventoryMode();
            }
        }
        boolean setAntennaInventoryMode(int antennaInventoryMode) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaInventoryMode(antennaInventoryMode);
        }

        int getAntennaLocalAlgo() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaLocalAlgo();
            }
        }
        boolean setAntennaLocalAlgo(int antennaLocalAlgo) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaLocalAlgo(antennaLocalAlgo);
        }

        int getAntennaLocalStartQ() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaLocalStartQ();
            }
        }
        boolean setAntennaLocalStartQ(int antennaLocalStartQ) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaLocalStartQ(antennaLocalStartQ);
        }

        int getAntennaProfileMode() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaProfileMode();
            }
        }
        boolean setAntennaProfileMode(int antennaProfileMode) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaProfileMode(antennaProfileMode);
        }

        int getAntennaLocalProfile() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaLocalProfile();
            }
        }
        boolean setAntennaLocalProfile(int antennaLocalProfile) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaLocalProfile(antennaLocalProfile);
        }

        int getAntennaFrequencyMode() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaFrequencyMode();
            }
        }
        boolean setAntennaFrequencyMode(int antennaFrequencyMode) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaFrequencyMode(antennaFrequencyMode);
        }

        int getAntennaLocalFrequency() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaLocalFrequency();
            }
        }
        boolean setAntennaLocalFrequency(int antennaLocalFrequency) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaLocalFrequency(antennaLocalFrequency);
        }

        int getAntennaStatus() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaStatus();
            }
        }

        int getAntennaDefine() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaDefine();
            }
        }

        long getAntennaDwell() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaDwell();
            }
        }
        boolean setAntennaDwell(long antennaDwell) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaDwell(antennaDwell);
        }

        long getAntennaPower(int portNumber) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                if (portNumber < 0 || portNumber > 15) portNumber = antennaSelect;
                long lValue;
                lValue = antennaSelectedData[portNumber].getAntennaPower();
                return lValue;
            }
        }
        boolean setAntennaPower(long antennaPower) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaPower(antennaPower);
        }

        long getAntennaInvCount() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                return antennaSelectedData[antennaSelect].getAntennaInvCount();
            }
        }
        boolean setAntennaInvCount(long antennaInvCount) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX)  { antennaSelect = mDefault.antennaSelect; appendToLog("antennaSelect is set to " + antennaSelect); }
            return antennaSelectedData[antennaSelect].setAntennaInvCount(antennaInvCount);
        }

        //Tag select block parameters
        final int INVSELECT_INVALID = -1; final int INVSELECT_MIN = 0; final int INVSELECT_MAX = 7;
        int invSelectIndex = INVSELECT_INVALID;
        int getInvSelectIndex() {
            if (invSelectIndex < INVSELECT_MIN || invSelectIndex > INVSELECT_MAX) {
                {
                    byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 0, 8, 0, 0, 0, 0};
                    mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_DESC_SEL, false, msgBuffer);
                }
            }
            appendToLog("settingUpdate getInvSelectIndex = " + invSelectIndex);
            return invSelectIndex;
        }
        boolean setInvSelectIndex(int invSelect) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 0, 8, 0, 0, 0, 0};
            if (invSelect < INVSELECT_MIN || invSelect > INVSELECT_MAX) invSelect = mDefault.invSelectIndex;
            if (this.invSelectIndex == invSelect && sameCheck) return true;
            msgBuffer[4] = (byte) (invSelect & 0x07);
            this.invSelectIndex = invSelect;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_DESC_SEL, true, msgBuffer);
        }

        InvSelectData[] invSelectData;
        int getSelectEnable() {
            if (invSelectIndex < INVSELECT_MIN || invSelectIndex > INVSELECT_MAX) invSelectIndex = mDefault.invSelectIndex;
            return invSelectData[invSelectIndex].getSelectEnable();
        }
        boolean setSelectEnable(int enable, int selectTarget, int selectAction, int selectDelay) {
            if (invSelectIndex < INVSELECT_MIN || invSelectIndex > INVSELECT_MAX) invSelectIndex = mDefault.invSelectIndex;
            return invSelectData[invSelectIndex].setRx000HostReg_HST_TAGMSK_DESC_CFG(enable, selectTarget, selectAction, selectDelay);
        }

        int getSelectTarget() {
            if (invSelectIndex < INVSELECT_MIN || invSelectIndex > INVSELECT_MAX) invSelectIndex = mDefault.invSelectIndex;
            return invSelectData[invSelectIndex].getSelectTarget();
        }

        int getSelectAction() {
            if (invSelectIndex < INVSELECT_MIN || invSelectIndex > INVSELECT_MAX) invSelectIndex = mDefault.invSelectIndex;
            return invSelectData[invSelectIndex].getSelectAction();
        }

        int getSelectMaskBank() {
            if (invSelectIndex < INVSELECT_MIN || invSelectIndex > INVSELECT_MAX) invSelectIndex = mDefault.invSelectIndex;
            return invSelectData[invSelectIndex].getSelectMaskBank();
        }
        boolean setSelectMaskBank(int selectMaskBank) {
            if (invSelectIndex < INVSELECT_MIN || invSelectIndex > INVSELECT_MAX) invSelectIndex = mDefault.invSelectIndex;
            return invSelectData[invSelectIndex].setSelectMaskBank(selectMaskBank);
        }

        int getSelectMaskOffset() {
            int dataIndex = invSelectIndex;
            if (dataIndex < INVSELECT_MIN || dataIndex > INVSELECT_MAX) {
                return INVSELECT_INVALID;
            } else {
                return invSelectData[dataIndex].getSelectMaskOffset();
            }
        }
        boolean setSelectMaskOffset(int selectMaskOffset) {
            if (invSelectIndex < INVSELECT_MIN || invSelectIndex > INVSELECT_MAX) invSelectIndex = mDefault.invSelectIndex;
            return invSelectData[invSelectIndex].setSelectMaskOffset(selectMaskOffset);
        }

        int getSelectMaskLength() {
            int dataIndex = invSelectIndex;
            if (dataIndex < INVSELECT_MIN || dataIndex > INVSELECT_MAX) {
                return INVSELECT_INVALID;
            } else {
                return invSelectData[dataIndex].getSelectMaskLength();
            }
        }
        boolean setSelectMaskLength(int selectMaskLength) {
            if (invSelectIndex < INVSELECT_MIN || invSelectIndex > INVSELECT_MAX) invSelectIndex = mDefault.invSelectIndex;
            return invSelectData[invSelectIndex].setSelectMaskLength(selectMaskLength);
        }

        String getSelectMaskData() {
            int dataIndex = invSelectIndex;
            if (dataIndex < INVSELECT_MIN || dataIndex > INVSELECT_MAX) {
                return null;
            } else {
                return invSelectData[dataIndex].getRx000SelectMaskData();
            }
        }
        boolean setSelectMaskData(String maskData) {
            if (maskData == null)   return false;
            if (invSelectIndex < INVSELECT_MIN || invSelectIndex > INVSELECT_MAX) invSelectIndex = mDefault.invSelectIndex;
            if (invSelectData[invSelectIndex].selectMaskDataReady != 0) {
                String maskDataOld = getSelectMaskData();
                if (maskData != null && maskDataOld != null) {
                    if (maskData.matches(maskDataOld) && sameCheck) return true;
                }
            }
            return invSelectData[invSelectIndex].setRx000SelectMaskData(maskData);
        }

        //Inventtory block paraameters
        final int QUERYTARGET_INVALID = -1; final int QUERYTARGET_MIN = 0; final int QUERYTARGET_MAX = 1;
        int queryTarget = QUERYTARGET_INVALID;
        int getQueryTarget() {
            if (queryTarget < QUERYTARGET_MIN || queryTarget > QUERYTARGET_MAX) getHST_QUERY_CFG();
            return queryTarget;
        }
        boolean setQueryTarget(int queryTarget) {
            return setQueryTarget(queryTarget, querySession, querySelect);
        }
        boolean setQueryTarget(int queryTarget, int querySession, int querySelect) {
            if (queryTarget >= 2) { mRfidDevice.mRfidReaderChip.mRx000Setting.setAlgoAbFlip(1); }
            else if (queryTarget >= 0) { mRfidDevice.mRfidReaderChip.mRx000Setting.setAlgoAbFlip(0); }

            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 0, 9, 0, 0, 0, 0};
            if (queryTarget != 2 && (queryTarget < QUERYTARGET_MIN || queryTarget > QUERYTARGET_MAX))
                queryTarget = mDefault.queryTarget;
            if (querySession < QUERYSESSION_MIN || querySession > QUERYSESSION_MAX)
                querySession = mDefault.querySession;
            if (querySelect < QUERYSELECT_MIN || querySelect > QUERYSELECT_MAX)
                querySelect = mDefault.querySelect;
            if (this.queryTarget == queryTarget && this.querySession == querySession && this.querySelect == querySelect && sameCheck) return true;
            msgBuffer[4] |= ((queryTarget == 2 ? 0 : queryTarget) << 4);
            msgBuffer[4] |= (byte) (querySession << 5);
            if ((querySelect & 0x01) != 0) {
                msgBuffer[4] |= (byte) 0x80;
            }
            if ((querySelect & 0x02) != 0) {
                msgBuffer[5] |= (byte) 0x01;
            }
            this.queryTarget = queryTarget;
            this.querySession = querySession;
            this.querySelect = querySelect;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_QUERY_CFG, true, msgBuffer);
        }

        final int QUERYSESSION_INVALID = -1; final int QUERYSESSION_MIN = 0; final int QUERYSESSION_MAX = 3;
        int querySession = QUERYSESSION_INVALID;
        int getQuerySession() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                {
                    if (querySession < QUERYSESSION_MIN || querySession > QUERYSESSION_MAX)
                        getHST_QUERY_CFG();
                    return querySession;
                }
            }

        }
        boolean setQuerySession(int querySession) {
            return setQueryTarget(queryTarget, querySession, querySelect);
        }

        final int QUERYSELECT_INVALID = -1; final int QUERYSELECT_MIN = 0; final int QUERYSELECT_MAX = 3;
        int querySelect = QUERYSELECT_INVALID;
        int getQuerySelect() {
            if (querySelect < QUERYSELECT_MIN || querySelect > QUERYSELECT_MAX) getHST_QUERY_CFG();
            appendToLog("Stream querySelect = " + querySelect);
            return querySelect;
        }
        boolean setQuerySelect(int querySelect) {
            return setQueryTarget(queryTarget, querySession, querySelect);
        }

        private boolean getHST_QUERY_CFG() {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 0, 9, 0, 0, 0, 0};
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_QUERY_CFG, false, msgBuffer);
        }

        final int INVALGO_INVALID = -1; final int INVALGO_MIN = 0; final int INVALGO_MAX = 3;
        int invAlgo = INVALGO_INVALID;
        int getInvAlgo() {
            if (invAlgo < INVALGO_MIN || invAlgo > INVALGO_MAX) getHST_INV_CFG();
            return invAlgo;
        }
        boolean setInvAlgo(int invAlgo) {
            appendToLog("writeBleStreamOut: going to setInvAlgo with invAlgo = " + invAlgo);
            return setInvAlgo(invAlgo, matchRep, tagSelect, noInventory, tagRead, tagDelay, invModeCompact,invBrandId);
        }

        final int MATCHREP_INVALID = -1; final int MATCHREP_MIN = 0; final int MATCHREP_MAX = 255;
        int matchRep = MATCHREP_INVALID;
        int getMatchRep() {
            if (matchRep < MATCHREP_MIN || matchRep > MATCHREP_MAX) getHST_INV_CFG();
            return matchRep;
        }
        boolean setMatchRep(int matchRep) {
            appendToLog("writeBleStreamOut: going to setInvAlgo with invAlgo = " + invAlgo);
            return setInvAlgo(invAlgo, matchRep, tagSelect, noInventory, tagRead, tagDelay, invModeCompact, invBrandId);
        }

        final int TAGSELECT_INVALID = -1; final int TAGSELECT_MIN = 0; final int TAGSELECT_MAX = 1;
        int tagSelect = TAGSELECT_INVALID;
        int getTagSelect() {
            if (tagSelect < TAGSELECT_MIN || tagSelect > TAGSELECT_MAX) getHST_INV_CFG();
            return tagSelect;
        }
        boolean setTagSelect(int tagSelect) {
            appendToLog("writeBleStreamOut: going to setInvAlgo with invAlgo = " + invAlgo);
            return setInvAlgo(invAlgo, matchRep, tagSelect, noInventory, tagRead, tagDelay, invModeCompact, invBrandId);
        }

        final int NOINVENTORY_INVALID = -1; final int NOINVENTORY_MIN = 0; final int NOINVENTORY_MAX = 1;
        int noInventory = NOINVENTORY_INVALID;
        int getNoInventory() {
            if (noInventory < NOINVENTORY_MIN || noInventory > NOINVENTORY_MAX) getHST_INV_CFG();
            return noInventory;
        }
        boolean setNoInventory(int noInventory) {
            appendToLog("writeBleStreamOut: going to setInvAlgo with invAlgo = " + invAlgo);
            return setInvAlgo(invAlgo, matchRep, tagSelect, noInventory, tagRead, tagDelay, invModeCompact, invBrandId);
        }

        final int TAGREAD_INVALID = -1; final int TAGREAD_MIN = 0; final int TAGREAD_MAX = 2;
        int tagRead = TAGREAD_INVALID;
        int getTagRead() {
            if (tagRead < TAGREAD_MIN || tagRead > TAGREAD_MAX) getHST_INV_CFG();
            return tagRead;
        }
        boolean setTagRead(int tagRead) {
            appendToLog("writeBleStreamOut: going to setInvAlgo with invAlgo = " + invAlgo);
            return setInvAlgo(invAlgo, matchRep, tagSelect, noInventory, tagRead, tagDelay, invModeCompact, invBrandId);
        }

        final int TAGDELAY_INVALID = -1; final int TAGDELAY_MIN = 0; final int TAGDELAY_MAX = 63;
        int tagDelay = TAGDELAY_INVALID;
        int getTagDelay() {
            if (tagDelay < TAGDELAY_MIN || tagDelay > TAGDELAY_MAX) getHST_INV_CFG();
            return tagDelay;
        }
        boolean setTagDelay(int tagDelay) {
            appendToLog("writeBleStreamOut: going to setInvAlgo with invAlgo = " + invAlgo);
            return setInvAlgo(invAlgo, matchRep, tagSelect, noInventory, tagRead, tagDelay, invModeCompact, invBrandId);
        }

        long cycleDelay = 0;
        long getCycleDelay() {
            return cycleDelay;
        }
        boolean setCycleDelay(long cycleDelay) {
            if (this.cycleDelay == cycleDelay && sameCheck) return true;

            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, (byte)0x0F, (byte)0x0F, 0, 0, 0, 0};
            msgBuffer[4] |= (cycleDelay & 0xFF);
            msgBuffer[5] |= (byte) ((cycleDelay & 0xFF00) >> 8);
            msgBuffer[6] |= (byte) ((cycleDelay & 0xFF0000) >> 16);
            msgBuffer[7] |= (byte) ((cycleDelay & 0xFF000000) >> 24);
            this.cycleDelay = cycleDelay;
            boolean bResult = mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_CFG, true, msgBuffer);
            //msgBuffer = new byte[]{(byte) 0x70, 0, (byte)0x0F, (byte)0x0F, 0, 0, 0, 0};
            //mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_CFG, false, msgBuffer);
            return bResult;
        }

        final int AUTHENTICATE_CFG_INVALID = -1; final int AUTHENTICATE_CFG_MIN = 0; final int AUTHENTICATE_CFG_MAX = 4095;
        boolean authenticateSendReply;
        boolean authenticateIncReplyLength;
        int authenticateLength = AUTHENTICATE_CFG_INVALID;
        int getAuthenticateReplyLength() {
            if (authenticateLength < AUTHENTICATE_CFG_MIN || authenticateLength > AUTHENTICATE_CFG_MAX) getHST_AUTHENTICATE_CFG();
            return authenticateLength;
        }
        private boolean getHST_AUTHENTICATE_CFG() {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 0, (byte) 0x0F, 0, 0, 0, 0};
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_AUTHENTICATE_CFG, false, msgBuffer);
        }
        boolean setHST_AUTHENTICATE_CFG(boolean sendReply, boolean incReplyLenth, int length) {
            appendToLog("sendReply = " + sendReply + ", incReplyLenth = " + incReplyLenth + ", length = " + length);
            if (length < 0 || length > 0x3FF) return false;

            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 0, (byte) 0x0F, 0, 0, 0, 0};
            if (sendReply) msgBuffer[4] |= 0x01; authenticateSendReply = sendReply;
            if (incReplyLenth) msgBuffer[4] |= 0x02; authenticateIncReplyLength = incReplyLenth;
            msgBuffer[5] |= ((length & 0x3F) << 2);
            msgBuffer[6] |= ((length & 0xFC0) >> 6); authenticateLength = length;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_AUTHENTICATE_CFG, true, msgBuffer);
        }

        byte[] authMatchData0_63; int authMatchDataReady = 0;
        String getAuthMatchData() {
            int length = 96;
            String strValue = "";
            for (int i = 0; i < 3; i++) {
                if (length > 0) {
                    appendToLog("i = " + i + ", authMatchDataReady = " + authMatchDataReady);
                    if ((authMatchDataReady & (0x01 << i)) == 0) {
                        byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 1, (byte)0x0F, 0, 0, 0, 0};
                        msgBuffer[2] += i;
                        mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_AUTHENTICATE_MSG, false, msgBuffer);
                    } else {
                        for (int j = 0; j < 4; j++) {
                            strValue += String.format("%02X", authMatchData0_63[i * 4 + j]);
                        }
                    }
                    length -= 32;
                }
            }
            if (strValue.length() < 16) strValue = null;
            return strValue;
        }
        boolean setAuthMatchData(String matchData) {
            int length = matchData.length();
            for (int i = 0; i < 6; i++) {
                if (length > 0) {
                    length -= 8;

                    byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 1, (byte)0x0F, 0, 0, 0, 0};
                    String hexString = "0123456789ABCDEF";
                    for (int j = 0; j < 8; j++) {
                        if (i * 8 + j + 1 <= matchData.length()) {
                            String subString = matchData.substring(i * 8 + j, i * 8 + j + 1).toUpperCase();
                            int k = 0;
                            for (k = 0; k < 16; k++) {
                                if (subString.matches(hexString.substring(k, k + 1))) {
                                    break;
                                }
                            }
                            if (k == 16) return false;
                            if ((j / 2) * 2 == j) {
                                msgBuffer[7 - j / 2] |= (byte) (k << 4);
                            } else {
                                msgBuffer[7 - j / 2] |= (byte) (k);
                            }
                        }
                    }
                    msgBuffer[2] = (byte) ((msgBuffer[2] & 0xFF) + i);
                    if (mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_AUTHENTICATE_MSG, true, msgBuffer) == false)
                        return false;
                    else {
                        //authMatchDataReady |= (0x01 << i);
                        System.arraycopy(msgBuffer, 4, authMatchData0_63, i * 4, 4); //appendToLog("Data=" + byteArrayToString(mRx000Setting.invMatchData0_63));
//                        appendToLog("invMatchDataReady=" + Integer.toString(mRx000Setting.invMatchDataReady, 16) + ", message=" + byteArrayToString(msgBuffer));
                    }
                }
            }
            return true;
        }

        final int UNTRACEABLE_CFG_INVALID = -1; final int UNTRACEABLE_CFG_MIN = 0; final int UNTRACEABLE_CFG_MAX = 3;
        int untraceableRange = UNTRACEABLE_CFG_INVALID;
        boolean untraceableUser;
        int untraceableTid = UNTRACEABLE_CFG_INVALID;
        int untraceableEpcLength = UNTRACEABLE_CFG_INVALID;
        boolean untraceableEpc;
        boolean untraceableUXpc;
        int getUntraceableEpcLength() {
            if (untraceableRange < UNTRACEABLE_CFG_MIN || untraceableRange > UNTRACEABLE_CFG_MAX) getHST_UNTRACEABLE_CFG();
            return untraceableEpcLength;
        }
        private boolean getHST_UNTRACEABLE_CFG() {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 5, (byte) 0x0F, 0, 0, 0, 0};
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_UNTRACEABLE_CFG, false, msgBuffer);
        }
        boolean setHST_UNTRACEABLE_CFG(int range, boolean user, int tid, int epcLength, boolean epc, boolean uxpc) {
            appendToLog("range = " + range + ", user = " + user + ", tid = " + tid + ", epc = " + epc + ", epcLength = " + epcLength + ", xcpc = " + uxpc);
            if (range < 0 || range > 3) return false;
            if (tid < 0 || tid > 2) return false;
            if (epcLength < 0 || epcLength > 31) return false;

            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 5, (byte) 0x0F, 0, 0, 0, 0};
            msgBuffer[4] |= (range); untraceableRange = range;
            if (user) msgBuffer[4] |= 0x04; untraceableUser = user;
            msgBuffer[4] |= (tid << 3); untraceableTid = tid;
            msgBuffer[4] |= ((epcLength & 0x7) << 5);
            msgBuffer[5] |= ((epcLength & 0x18) >> 3); untraceableEpcLength = epcLength;
            if (epc) msgBuffer[5] |= 0x04; untraceableEpc = epc;
            if (uxpc) msgBuffer[5] |= 0x08; untraceableUXpc = uxpc;
            appendToLog("going to do sendHostRegRequest(HostRegRequests.HST_UNTRACEABLE_CFG,");
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_UNTRACEABLE_CFG, true, msgBuffer);
        }

        final int TAGJOIN_INVALID = -1; final int TAGJOIN_MIN = 0; final int TAGJOIN_MAX = 1;
        int invModeCompact = TAGJOIN_INVALID;
        boolean getInvModeCompact() {
            if (invModeCompact < TAGDELAY_MIN || invModeCompact > TAGDELAY_MAX) { getHST_INV_CFG(); return false; }
            return (invModeCompact == 1 ? true : false);
        }
        boolean setInvModeCompact(boolean invModeCompact) {
            appendToLog("writeBleStreamOut: going to setInvAlgo with invAlgo = " + invAlgo);
            return setInvAlgo(invAlgo, matchRep, tagSelect, noInventory, tagRead, tagDelay, (invModeCompact ? 1 : 0), invBrandId);
        }

        final int BRAND_INVALID = -1; final int BRANDID_MIN = 0; final int BRANDID_MAX = 1;
        int invBrandId = BRAND_INVALID;
        boolean getInvBrandId() {
            if (invBrandId < BRANDID_MIN || invBrandId > BRANDID_MAX) { getHST_INV_CFG(); return false; }
            return (invModeCompact == 1 ? true : false);
        }
        boolean setInvBrandId(boolean invBrandId) {
            return setInvAlgo(invAlgo, matchRep, tagSelect, noInventory, tagRead, tagDelay, invModeCompact, (invBrandId ? 1 : 0));
        }

        private boolean getHST_INV_CFG() {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 1, 9, 0, 0, 0, 0};
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_CFG, false, msgBuffer);
        }
        boolean setInvAlgo(int invAlgo, int matchRep, int tagSelect, int noInventory, int tagRead, int tagDelay, int invModeCompact, int invBrandId) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 1, 9, 0, 0, 0, 0};
            if (invAlgo < INVALGO_MIN || invAlgo > INVALGO_MAX) invAlgo = mDefault.invAlgo;
            if (matchRep < MATCHREP_MIN || matchRep > MATCHREP_MAX) matchRep = mDefault.matchRep;
            if (tagSelect < TAGSELECT_MIN || tagSelect > TAGSELECT_MAX) tagSelect = mDefault.tagSelect;
            if (noInventory < NOINVENTORY_MIN || noInventory > NOINVENTORY_MAX) noInventory = mDefault.noInventory;
            if (tagDelay < TAGDELAY_MIN || tagDelay > TAGDELAY_MAX) tagDelay = mDefault.tagDelay;
            if (invModeCompact < TAGJOIN_MIN || invModeCompact > TAGJOIN_MAX) invModeCompact = mDefault.tagJoin;
            if (invBrandId < BRANDID_MIN || invBrandId > BRANDID_MAX) invBrandId = mDefault.brandid;
            if (tagRead < TAGREAD_MIN || tagRead > TAGREAD_MAX) tagRead = mDefault.tagRead;
            if (DEBUG) appendToLog("Old invAlgo = " + this.invAlgo + ", matchRep = " + this.matchRep + ", tagSelect =" + this.tagSelect + ", noInventory = " + this.noInventory + ", tagRead = " + this.tagRead + ", tagDelay = " + this.tagDelay + ", invModeCompact = " + this.invModeCompact + ", invBrandId = " + this.invBrandId);
            if (DEBUG) appendToLog("New invAlgo = " + invAlgo + ", matchRep = " + matchRep + ", tagSelect =" + tagSelect + ", noInventory = " + noInventory + ", tagRead = " + tagRead + ", tagDelay = " + tagDelay + ", invModeCompact = " + invModeCompact + ", invBrandId = " + invBrandId + ", sameCheck = " + sameCheck);
            if (this.invAlgo == invAlgo && this.matchRep == matchRep && this.tagSelect == tagSelect && this.noInventory == noInventory && this.tagRead == tagRead && this.tagDelay == tagDelay && this.invModeCompact == invModeCompact && this.invBrandId == invBrandId && sameCheck) return true;
            if (DEBUG) appendToLog("There is difference");
            msgBuffer[4] |= invAlgo;
            msgBuffer[4] |= (byte) ((matchRep & 0x03) << 6);
            msgBuffer[5] |= (byte) (matchRep >> 2);
            if (tagSelect != 0) {
                msgBuffer[5] |= 0x40;
            }
            if (noInventory != 0) {
                msgBuffer[5] |= 0x80;
            }
            if ((tagRead & 0x03) != 0) {
                msgBuffer[6] |= (tagRead & 0x03);
            }
            if ((tagDelay & 0x0F) != 0) {
                msgBuffer[6] |= ((tagDelay & 0x0F) << 4);
            }
            if ((tagDelay & 0x30) != 0) {
                msgBuffer[7] |= ((tagDelay & 0x30) >> 4);
            }
            if (invModeCompact == 1) {
                msgBuffer[7] |= 0x04;
            }
            if (invBrandId == 1) {
                msgBuffer[7] |= 0x08;
            }
            this.invAlgo = invAlgo; appendToLog("Hello6: invAlgo = " + invAlgo + ", queryTarget = " + queryTarget);
            this.matchRep = matchRep;
            this.tagSelect = tagSelect;
            this.noInventory = noInventory;
            this.tagRead = tagRead;
            this.tagDelay = tagDelay;
            this.invModeCompact = invModeCompact;
            this.invBrandId = invBrandId;
            if (DEBUG) appendToLog("Stored tagDelay = " + this.tagDelay);
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_CFG, true, msgBuffer);
        }

        final int ALGOSELECT_INVALID = -1; final int ALGOSELECT_MIN = 0; final int ALGOSELECT_MAX = 3;   //DataSheet says Max=1
        int algoSelect = ALGOSELECT_INVALID;
        int getAlgoSelect() {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 2, 9, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_SEL, false, msgBuffer);
            }
            return algoSelect;
        }
        boolean dummyAlgoSelected = false;
        boolean setAlgoSelect(int algoSelect) {
            appendToLog("setTagGroup: algoSelect = " + algoSelect + ", this.algoSelct = " + this.algoSelect + ", dummyAlgoSelected = " + dummyAlgoSelected);
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 2, 9, 0, 0, 0, 0};
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX)
                algoSelect = mDefault.algoSelect;
            if (algoSelect == this.algoSelect && dummyAlgoSelected == false)  return true;
            msgBuffer[4] = (byte) (algoSelect & 0xFF);
            msgBuffer[5] = (byte) ((algoSelect & 0xFF00) >> 8);
            msgBuffer[6] = (byte) ((algoSelect & 0xFF0000) >> 16);
            msgBuffer[7] = (byte) ((algoSelect & 0xFF000000) >> 24);
            this.algoSelect = algoSelect; appendToLog("setTagGroup: Hello6: algoSelect = " + algoSelect);
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_SEL, true, msgBuffer);
        }

        AlgoSelectedData[] algoSelectedData;
        int getAlgoStartQ(int algoSelect) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoStartQ(false);
            }
        }
        int getAlgoStartQ() {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoStartQ(true);
            }
        }
        boolean setAlgoStartQ(int algoStartQ) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) return false;
            return algoSelectedData[algoSelect].setAlgoStartQ(algoStartQ);
        }
        boolean setAlgoStartQ(int startQ, int algoMaxQ, int algoMinQ, int algoMaxRep, int algoHighThres, int algoLowThres) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) return false;
            return algoSelectedData[algoSelect].setAlgoStartQ(startQ, algoMaxQ, algoMinQ, algoMaxRep, algoHighThres, algoLowThres);
        }

        int getAlgoMaxQ(int algoSelect) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoMaxQ();
            }
        }
        int getAlgoMaxQ() {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoMaxQ();
            }
        }
        boolean setAlgoMaxQ(int algoMaxQ) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) return false;
            return algoSelectedData[algoSelect].setAlgoMaxQ(algoMaxQ);
        }

        int getAlgoMinQ(int algoSelect) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoMinQ();
            }
        }
        int getAlgoMinQ() {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoMinQ();
            }
        }
        boolean setAlgoMinQ(int algoMinQ) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) return false;
            return algoSelectedData[algoSelect].setAlgoMinQ(algoMinQ);
        }

        int getAlgoMaxRep() {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoMaxRep();
            }
        }
        boolean setAlgoMaxRep(int algoMaxRep) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) return false;
            return algoSelectedData[algoSelect].setAlgoMaxRep(algoMaxRep);
        }

        int getAlgoHighThres() {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoHighThres();
            }
        }
        boolean setAlgoHighThres(int algoHighThre) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) return false;
            return algoSelectedData[algoSelect].setAlgoHighThres(algoHighThre);
        }

        int getAlgoLowThres() {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoLowThres();
            }
        }
        boolean setAlgoLowThres(int algoLowThre) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) return false;
            return algoSelectedData[algoSelect].setAlgoLowThres(algoLowThre);
        }

        int getAlgoRetry(int algoSelect) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoRetry();
            }
        }
        boolean setAlgoRetry(int algoRetry) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) return false;
            return algoSelectedData[algoSelect].setAlgoRetry(algoRetry);
        }

        int getAlgoAbFlip(int algoSelect) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoAbFlip();
            }
        }
        int getAlgoAbFlip() {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoAbFlip();
            }
        }
        boolean setAlgoAbFlip(int algoAbFlip) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) return false;
            return algoSelectedData[algoSelect].setAlgoAbFlip(algoAbFlip);
        }
        boolean setAlgoAbFlip(int algoAbFlip, int algoRunTilZero) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) return false;
            appendToLog("algoSelect = " + algoSelect + ", algoAbFlip = " + algoAbFlip + ", algoRunTilZero = " + algoRunTilZero);
            return algoSelectedData[algoSelect].setAlgoAbFlip(algoAbFlip, algoRunTilZero);
        }

        int getAlgoRunTilZero(int algoSelect) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoRunTilZero();
            }
        }
        int getAlgoRunTilZero() {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) {
                return ALGOSELECT_INVALID;
            } else {
                return algoSelectedData[algoSelect].getAlgoRunTilZero();
            }
        }
        boolean setAlgoRunTilZero(int algoRunTilZero) {
            if (algoSelect < ALGOSELECT_MIN || algoSelect > ALGOSELECT_MAX) return false;
            return algoSelectedData[algoSelect].setAlgoRunTilZero(algoRunTilZero);
        }

        final int MATCHENABLE_INVALID = -1; final int MATCHENABLE_MIN = 0; final int MATCHENABLE_MAX = 1;
        int matchEnable = MATCHENABLE_INVALID;
        int getInvMatchEnable() {
            getHST_INV_EPC_MATCH_CFG();
            return matchEnable;
        }
        boolean setInvMatchEnable(int matchEnable) {
            return setHST_INV_EPC_MATCH_CFG(matchEnable, this.matchType, this.matchLength, this.matchOffset);
        }
        boolean setInvMatchEnable(int matchEnable, int matchType, int matchLength, int matchOffset) {
            return setHST_INV_EPC_MATCH_CFG(matchEnable, matchType, matchLength, matchOffset);
        }

        final int MATCHTYPE_INVALID = -1; final int MATCHTYPE_MIN = 0; final int MATCHTYPE_MAX = 1;
        int matchType = MATCHTYPE_INVALID;
        int getInvMatchType() {
            getHST_INV_EPC_MATCH_CFG();
            return matchType;
        }

        final int MATCHLENGTH_INVALID = 0; final int MATCHLENGTH_MIN = 0; final int MATCHLENGTH_MAX = 496;
        int matchLength = MATCHLENGTH_INVALID;
        int getInvMatchLength() {
            getHST_INV_EPC_MATCH_CFG();
            return matchLength;
        }

        final int MATCHOFFSET_INVALID = -1; final int MATCHOFFSET_MIN = 0; final int MATCHOFFSET_MAX = 496;
        int matchOffset = MATCHOFFSET_INVALID;
        int getInvMatchOffset() {
            getHST_INV_EPC_MATCH_CFG();
            return matchOffset;
        }

        private boolean getHST_INV_EPC_MATCH_CFG() {
            if (matchEnable < MATCHENABLE_MIN || matchEnable > MATCHENABLE_MAX
                    || matchType < MATCHTYPE_MIN || matchType > MATCHTYPE_MAX
                    || matchLength < MATCHLENGTH_MIN || matchLength > MATCHLENGTH_MAX
                    || matchOffset < MATCHOFFSET_MIN || matchOffset > MATCHOFFSET_MAX
                    ) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 0x11, 9, 0, 0, 0, 0};
                return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_EPC_MATCH_CFG, false, msgBuffer);
            } else {
                return false;
            }
        }
        private boolean setHST_INV_EPC_MATCH_CFG(int matchEnable, int matchType, int matchLength, int matchOffset) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 0x11, 9, 0, 0, 0, 0};
            if (matchEnable < MATCHENABLE_MIN || matchEnable > MATCHENABLE_MAX)
                matchEnable = mDefault.matchEnable;
            if (matchType < MATCHTYPE_MIN || matchType > MATCHTYPE_MAX)
                matchType = mDefault.matchType;
            if (matchLength < MATCHLENGTH_MIN || matchLength > MATCHLENGTH_MAX)
                matchLength = mDefault.matchLength;
            if (matchOffset < MATCHOFFSET_MIN || matchOffset > MATCHOFFSET_MAX)
                matchOffset = mDefault.matchOffset;
            if (this.matchEnable == matchEnable && this.matchType == matchType && this.matchLength == matchLength && this.matchOffset == matchOffset && sameCheck) return true;
            if (matchEnable != 0) {
                msgBuffer[4] |= 0x01;
            }
            if (matchType != 0) {
                msgBuffer[4] |= 0x02;
            }
            msgBuffer[4] |= (byte) ((matchLength % 64) << 2);
            msgBuffer[5] |= (byte) ((matchLength / 64));
            msgBuffer[5] |= (byte) ((matchOffset % 32) << 3);
            msgBuffer[6] |= (byte) (matchOffset / 32);
            this.matchEnable = matchEnable;
            this.matchType = matchType;
            this.matchLength = matchLength;
            this.matchOffset = matchOffset;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_EPC_MATCH_CFG, true, msgBuffer);
        }

        byte[] invMatchData0_63; int invMatchDataReady = 0;
        String getInvMatchData() {
            int length = matchLength;
            String strValue = "";
            for (int i = 0; i < 16; i++) {
                if (length > 0) {
                    if ((invMatchDataReady & (0x01 << i)) == 0) {
                        byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 0x12, 9, 0, 0, 0, 0};
                        msgBuffer[2] += i;
                        mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_EPCDAT_0_3, false, msgBuffer);

                        strValue = null;
                        break;
                    } else {
                        for (int j = 0; j < 4; j++) {
                            strValue += String.format("%02X", invMatchData0_63[i * 4 + j]);
                        }
                    }
                    length -= 32;
                }
            }
            return strValue;
        }
        boolean setInvMatchData(String matchData) {
            int length = matchData.length();
            for (int i = 0; i < 16; i++) {
                if (length > 0) {
                    length -= 8;

                    byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 0x12, 9, 0, 0, 0, 0};
                    String hexString = "0123456789ABCDEF";
                    for (int j = 0; j < 8; j++) {
                        if (i * 8 + j + 1 <= matchData.length()) {
                            String subString = matchData.substring(i * 8 + j, i * 8 + j + 1).toUpperCase();
                            int k = 0;
                            for (k = 0; k < 16; k++) {
                                if (subString.matches(hexString.substring(k, k + 1))) {
                                    break;
                                }
                            }
                            if (k == 16) return false;
                            if ((j / 2) * 2 == j) {
                                msgBuffer[4 + j / 2] |= (byte) (k << 4);
                            } else {
                                msgBuffer[4 + j / 2] |= (byte) (k);
                            }
                        }
                    }
                    msgBuffer[2] = (byte) ((msgBuffer[2] & 0xFF) + i);
                    if (mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_EPCDAT_0_3, true, msgBuffer) == false)
                        return false;
                    else {
                        invMatchDataReady |= (0x01 << i);
                        System.arraycopy(msgBuffer, 4, invMatchData0_63, i * 4, 4); //appendToLog("Data=" + byteArrayToString(mRx000Setting.invMatchData0_63));
//                        appendToLog("invMatchDataReady=" + Integer.toString(mRx000Setting.invMatchDataReady, 16) + ", message=" + byteArrayToString(msgBuffer));
                    }
                }
            }
            return true;
        }

        //Tag access block parameters
        boolean accessVerfiy;
        final int ACCRETRY_INVALID = -1; final int ACCRETRY_MIN = 0; final int ACCRETRY_MAX = 7;
        int accessRetry = ACCRETRY_INVALID;
        int getAccessRetry() {
            if (accessRetry < ACCRETRY_MIN || accessRetry > ACCRETRY_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 1, (byte) 0x0A, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_DESC_CFG, false, msgBuffer);
            }
            return accessRetry;
        }
        boolean setAccessRetry(boolean accessVerfiy, int accessRetry) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 1, 0x0A, 0, 0, 0, 0};
            if (accessRetry < ACCRETRY_MIN || accessRetry > ACCRETRY_MAX)
                accessRetry = mDefault.accessRetry;
            if (this.accessVerfiy == accessVerfiy && this.accessRetry == accessRetry && sameCheck) return true;
            msgBuffer[4] |= (byte) (accessRetry << 1);
            if (accessVerfiy)   msgBuffer[4] |= 0x01;
            this.accessVerfiy = accessVerfiy;
            this.accessRetry = accessRetry;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_DESC_CFG, true, msgBuffer);
        }

        final int ACCBANK_INVALID = -1; final int ACCBANK_MIN = 0; final int ACCBANK_MAX = 3;
        int accessBank = ACCBANK_INVALID; int accessBank2 = ACCBANK_INVALID;
        int getAccessBank() {
            if (accessBank < ACCBANK_MIN || accessBank > ACCBANK_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 2, (byte) 0x0A, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_BANK, false, msgBuffer);
            }
            return accessBank;
        }
        boolean setAccessBank(int accessBank) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 2, 0x0A, 0, 0, 0, 0};
            if (accessBank < ACCBANK_MIN || accessBank > ACCBANK_MAX)
                accessBank = mDefault.accessBank;
            if (this.accessBank == accessBank && this.accessBank2 == 0 && sameCheck) return true;
            msgBuffer[4] = (byte) (accessBank & 0x03);
            this.accessBank = accessBank; this.accessBank2 = 0;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_BANK, true, msgBuffer);
        }
        boolean setAccessBank(int accessBank, int accessBank2) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 2, 0x0A, 0, 0, 0, 0};
            if (DEBUG) appendToLog("setAccessBank(" + accessBank + ", " + accessBank2 + ") with tagRead = " + tagRead);
            if (tagRead != 2)  accessBank2 = 0;
            if (accessBank < ACCBANK_MIN || accessBank > ACCBANK_MAX)
                accessBank = mDefault.accessBank;
            if (accessBank2 < ACCBANK_MIN || accessBank2 > ACCBANK_MAX)
                accessBank2 = mDefault.accessBank2;
            if (this.accessBank == accessBank && this.accessBank2 == accessBank2 && sameCheck) return true;
            msgBuffer[4] = (byte) (accessBank & 0x03);
            msgBuffer[4] |= (byte) ((accessBank2 & 0x03) << 2);
            this.accessBank = accessBank; this.accessBank2 = accessBank2;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_BANK, true, msgBuffer);
        }

        final int ACCOFFSET_INVALID = -1; final int ACCOFFSET_MIN = 0; final int ACCOFFSET_MAX = 0xFFFF;
        int accessOffset = ACCOFFSET_INVALID; int accessOffset2 = ACCOFFSET_INVALID;
        int getAccessOffset() {
            if (accessOffset < ACCOFFSET_MIN || accessOffset > ACCOFFSET_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 3, (byte) 0x0A, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_PTR, false, msgBuffer);
            }
            return accessOffset;
        }
        boolean setAccessOffset(int accessOffset) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 3, 0x0A, 0, 0, 0, 0};
            if (accessOffset < ACCOFFSET_MIN || accessOffset > ACCOFFSET_MAX)
                accessOffset = mDefault.accessOffset;
            if (this.accessOffset == accessOffset && this.accessOffset2 == 0 && sameCheck) return true;
            msgBuffer[4] = (byte) (accessOffset & 0xFF);
            msgBuffer[5] = (byte) ((accessOffset >> 8) & 0xFF);
			msgBuffer[6] = (byte) ((accessOffset >> 16) & 0xFF);
			msgBuffer[7] = (byte) ((accessOffset >> 24) & 0xFF);
            this.accessOffset = accessOffset; this.accessOffset2 = 0;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_PTR, true, msgBuffer);
        }
        boolean setAccessOffset(int accessOffset, int accessOffset2) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 3, 0x0A, 0, 0, 0, 0};
            if (tagRead != 2)   accessOffset2 = 0;
            if (accessOffset < ACCOFFSET_MIN || accessOffset > ACCOFFSET_MAX)
                accessOffset = mDefault.accessOffset;
            if (accessOffset2 < ACCOFFSET_MIN || accessOffset2 > ACCOFFSET_MAX)
                accessOffset2 = mDefault.accessOffset2;
            if (this.accessOffset == accessOffset && this.accessOffset2 == accessOffset2 && sameCheck) return true;
            msgBuffer[4] = (byte) (accessOffset & 0xFF);
            msgBuffer[5] = (byte) ((accessOffset >> 8) & 0xFF);
            msgBuffer[6] = (byte) (accessOffset2 & 0xFF);
            msgBuffer[7] = (byte) ((accessOffset2 >> 8) & 0xFF);
            this.accessOffset = accessOffset; this.accessOffset2 = accessOffset2;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_PTR, true, msgBuffer);
        }

        final int ACCCOUNT_INVALID = -1; final int ACCCOUNT_MIN = 0; final int ACCCOUNT_MAX = 255;
        int accessCount = ACCCOUNT_INVALID; int accessCount2 = ACCCOUNT_INVALID;
        int getAccessCount() {
            if (accessCount < ACCCOUNT_MIN || accessCount > ACCCOUNT_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 4, (byte) 0x0A, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_CNT, false, msgBuffer);
            }
            return accessCount;
        }
        boolean setAccessCount(int accessCount) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 4, 0x0A, 0, 0, 0, 0};
            if (accessCount < ACCCOUNT_MIN || accessCount > ACCCOUNT_MAX)
                accessCount = mDefault.accessCount;
            if (this.accessCount == accessCount && this.accessCount2 == 0 && sameCheck) return true;
            msgBuffer[4] = (byte) (accessCount & 0xFF);
            this.accessCount = accessCount; this.accessCount2 = 0;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_CNT, true, msgBuffer);
        }
        boolean setAccessCount(int accessCount, int accessCount2) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 4, 0x0A, 0, 0, 0, 0};
            if (tagRead != 2)   accessCount2 = 0;
            if (accessCount < ACCCOUNT_MIN || accessCount > ACCCOUNT_MAX)
                accessCount = mDefault.accessCount;
            if (accessCount2 < ACCCOUNT_MIN || accessCount2 > ACCCOUNT_MAX)
                accessCount2 = mDefault.accessCount2;
            if (this.accessCount == accessCount && this.accessCount2 == accessCount2 && sameCheck) return true;
            msgBuffer[4] = (byte) (accessCount & 0xFF);
            msgBuffer[5] = (byte) (accessCount2 & 0xFF);
            this.accessCount = accessCount; this.accessCount2 = accessCount2;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_CNT, true, msgBuffer);
        }

        final int ACCLOCKACTION_INVALID = -1; final int ACCLOCKACTION_MIN = 0; final int ACCLOCKACTION_MAX = 0x3FF;
        int accessLockAction = ACCLOCKACTION_INVALID;
        int getAccessLockAction() {
            if (accessLockAction < ACCLOCKACTION_MIN || accessLockAction > ACCLOCKACTION_MAX)
                getHST_TAGACC_LOCKCFG();
            return accessLockAction;
        }
        boolean setAccessLockAction(int accessLockAction) {
            return setAccessLockAction(accessLockAction, accessLockMask);
        }

        final int ACCLOCKMASK_INVALID = -1; final int ACCLOCKMASK_MIN = 0; final int ACCLOCKMASK_MAX = 0x3FF;
        int accessLockMask = ACCLOCKMASK_INVALID;
        int getAccessLockMask() {
            if (accessLockMask < ACCLOCKMASK_MIN || accessLockMask > ACCLOCKMASK_MAX)
                getHST_TAGACC_LOCKCFG();
            return accessLockMask;
        }
        boolean setAccessLockMask(int accessLockMask) {
            return setAccessLockAction(accessLockAction, accessLockMask);
        }

        boolean getHST_TAGACC_LOCKCFG() {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 5, (byte) 0x0A, 0, 0, 0, 0};
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_LOCKCFG, false, msgBuffer);
        }
        boolean setAccessLockAction(int accessLockAction, int accessLockMask) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 5, 0x0A, 0, 0, 0, 0};
            if (accessLockAction < ACCLOCKACTION_MIN || accessLockAction > ACCLOCKACTION_MAX)
                accessLockAction = mDefault.accessLockAction;
            if (accessLockMask < ACCLOCKMASK_MIN || accessLockMask > ACCLOCKMASK_MAX)
                accessLockMask = mDefault.accessLockMask;
            if (this.accessLockAction == accessLockAction && this.accessLockMask == accessLockMask && sameCheck) return true;
            msgBuffer[4] = (byte) (accessLockAction & 0xFF);
            msgBuffer[5] |= (byte) ((accessLockAction & 0x3FF) >> 8);

            msgBuffer[5] |= (byte) ((accessLockMask & 0x3F) << 2);
            msgBuffer[6] |= (byte) ((accessLockMask & 0x3FF) >> 6);
            this.accessLockAction = accessLockAction;
            this.accessLockMask = accessLockMask;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_LOCKCFG, true, msgBuffer);
        }

        final int ACCPWD_INVALID = 0; final long ACCPWD_MIN = 0; final long ACCPWD_MAX = 0x0FFFFFFFF;
        boolean setRx000AccessPassword(String password) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 6, (byte) 0x0A, 0, 0, 0, 0};
            if (password == null) password = "";
            String hexString = "0123456789ABCDEF";
            for (int j = 0; j < 16; j++) {
                if (j + 1 <= password.length()) {
                    String subString = password.substring(j, j + 1).toUpperCase();
                    int k = 0;
                    for (k = 0; k < 16; k++) {
                        if (subString.matches(hexString.substring(k, k + 1))) {
                            break;
                        }
                    }
                    if (k == 16) return false;
                    if ((j / 2) * 2 == j) {
                        msgBuffer[7 - j / 2] |= (byte) (k << 4);
                    } else {
                        msgBuffer[7 - j / 2] |= (byte) (k);
                    }
                }
            }
            boolean retValue = mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_ACCPWD, true, msgBuffer);
            if (DEBUG) appendToLog("sendHostRegRequest(): retValue = " + retValue);
            return retValue;
        }

        boolean setRx000KillPassword(String password) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 7, (byte) 0x0A, 0, 0, 0, 0};
            String hexString = "0123456789ABCDEF";
            for (int j = 0; j < 16; j++) {
                if (j + 1 <= password.length()) {
                    String subString = password.substring(j, j + 1).toUpperCase();
                    int k = 0;
                    for (k = 0; k < 16; k++) {
                        if (subString.matches(hexString.substring(k, k + 1))) {
                            break;
                        }
                    }
                    if (k == 16) return false;
                    if ((j / 2) * 2 == j) {
                        msgBuffer[7 - j / 2] |= (byte) (k << 4);
                    } else {
                        msgBuffer[7 - j / 2] |= (byte) (k);
                    }
                }
            }
            boolean retValue = mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGACC_KILLPWD, true, msgBuffer);
            if (DEBUG) appendToLog("sendHostRegRequest(): retValue = " + retValue);
            return retValue;
        }

        final int ACCWRITEDATSEL_INVALID = -1; final int ACCWRITEDATSEL_MIN = 0; final int ACCWRITEDATSEL_MAX = 7;
        int accessWriteDataSelect = ACCWRITEDATSEL_INVALID;
        int getAccessWriteDataSelect() {
            if (accessWriteDataSelect < ACCWRITEDATSEL_MIN || accessWriteDataSelect > ACCWRITEDATSEL_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 8, (byte) 0x0A, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGWRDAT_SEL, false, msgBuffer);
            }
            return accessWriteDataSelect;
        }
        boolean setAccessWriteDataSelect(int accessWriteDataSelect) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 8, 0x0A, 0, 0, 0, 0};
            if (accessWriteDataSelect < ACCWRITEDATSEL_MIN || accessWriteDataSelect > ACCWRITEDATSEL_MAX)
                accessWriteDataSelect = mDefault.accessWriteDataSelect;
            if (this.accessWriteDataSelect == accessWriteDataSelect && sameCheck) return true;
            accWriteDataReady = 0;
            msgBuffer[4] = (byte) (accessWriteDataSelect & 0x07);
            this.accessWriteDataSelect = accessWriteDataSelect;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGWRDAT_SEL, true, msgBuffer);
        }

        byte[] accWriteData0_63; int accWriteDataReady = 0;
        String getAccessWriteData() {
            int length = accessCount;
            if (length > 32) {
                length = 32;
            }
            String strValue = "";
            for (int i = 0; i < 32; i++) {
                if (length > 0) {
                    if ((accWriteDataReady & (0x01 << i)) == 0) {
                        byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 9, (byte) 0x0A, 0, 0, 0, 0};
                        msgBuffer[2] += i;
                        mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGWRDAT_0, false, msgBuffer);

                        strValue = null;
                        break;
                    } else {
                        for (int j = 0; j < 4; j++) {
                            strValue += String.format("%02X", accWriteData0_63[i * 4 + j]);
                        }
                    }
                    length -= 2;
                }
            }
            return strValue;
        }
        boolean setAccessWriteData(String dataInput) {
            dataInput = dataInput.trim();
            int writeBufLength = 16 * 2; //16
            int wrieByteSize = 4;   //8
            int length = dataInput.length(); appendToLog("length = " + length);
            if (length > wrieByteSize * writeBufLength) { appendToLog("1"); return false; }
            for (int i = 0; i < writeBufLength; i++) {
                if (length > 0) {
                    length -= wrieByteSize;
                    if ((i / 16) * 16 == i) {
                        byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 8, (byte) 0x0A, 0, 0, 0, 0};
                        msgBuffer[4] = (byte) (i / 16);
                        if (mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGWRDAT_SEL, true, msgBuffer) == false) {
                            appendToLog("23");
                            return false;
                        }
                    }
                    byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 9, (byte) 0x0A, 0, 0, 0, 0};
                    String hexString = "0123456789ABCDEF";
                    for (int j = 0; j < wrieByteSize; j++) {
//                        if (i * wrieByteSize + j + 1 <= dataInput.length()) {
                        appendToLog("dataInput = " + dataInput + ", i = " + i + ", wrieByteSize = " + wrieByteSize + ", j = " + j);
                        if (i * wrieByteSize + j >= dataInput.length()) break;
                            String subString = dataInput.substring(i * wrieByteSize + j, i * wrieByteSize + j + 1).toUpperCase();
                            appendToLog("subString = " + subString);
                            if (DEBUG) appendToLog(subString);
                            int k = 0;
                            for (k = 0; k < 16; k++) {
                                if (subString.matches(hexString.substring(k, k + 1))) {
                                    break;
                                }
                            }
                            if (k == 16) { appendToLog("2: i= " + i + ", j=" + j + ", subString = " + subString); return false; }
                            if ((j / 2) * 2 == j) {
                                msgBuffer[5- j / 2] |= (byte) (k << 4);
                            } else {
                                msgBuffer[5 - j / 2] |= (byte) (k);
                            }
//                        }
                    }
                    appendToLog("complete 4 bytes: " + byteArrayToString(msgBuffer));
                    msgBuffer[2] = (byte) ((msgBuffer[2] & 0xFF) + (i % 16));
                    if (wrieByteSize == 4) {
                        msgBuffer[6] = (byte)(i);
                    }
                    if (mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGWRDAT_0, true, msgBuffer) == false) {
                        appendToLog("3"); return false;
                    } else {
                        mRfidDevice.mRfidReaderChip.mRx000Setting.accWriteDataReady |= (0x01 << i);
                        if (DEBUG) appendToLog("accWriteReady=" + accWriteDataReady);
                        for (int k = 0; k < 4; k++) {
                            accWriteData0_63[i * 4 + k] = msgBuffer[7 - k];
                        }
                        if (DEBUG) appendToLog("Data=" + byteArrayToString(accWriteData0_63));
                    }
                } else break;
            }
            return true;
        }

        //RFTC block paramters
        final int PROFILE_INVALID = -1; final int PROFILE_MIN = 0; final int PROFILE_MAX = 5;   //profile 4 and 5 are custom profiles.
        int currentProfile = PROFILE_INVALID;
        int getCurrentProfile() {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) {
                return ANTSELECT_INVALID;
            } else {
                if (currentProfile < PROFILE_MIN || currentProfile > PROFILE_MAX) {
                    byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 0x60, 0x0B, 0, 0, 0, 0};
                    mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_RFTC_CURRENT_PROFILE, false, msgBuffer);
                }
                return currentProfile;
            }
        }

        boolean setCurrentProfile(int currentProfile) {
            if (antennaSelect < ANTSLECT_MIN || antennaSelect > ANTSELECT_MAX) return false;
            else {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 0x60, 0x0B, 0, 0, 0, 0};
                if (currentProfile < PROFILE_MIN || currentProfile > PROFILE_MAX)
                    currentProfile = mDefault.currentProfile;
                if (this.currentProfile == currentProfile && sameCheck) return true;
                msgBuffer[4] = (byte) (currentProfile);
                this.currentProfile = currentProfile;
                return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_RFTC_CURRENT_PROFILE, true, msgBuffer);
            }
        }

        final int COUNTRYENUM_INVALID = -1; final int COUNTRYENUM_MIN = 1; final int COUNTRYENUM_MAX = 109;
        final int COUNTRYCODE_INVALID = -1; final int COUNTRYCODE_MIN = 1; final int COUNTRYCODE_MAX = 9;
        int countryEnumOem = COUNTRYENUM_INVALID; int countryEnum = COUNTRYENUM_INVALID; int countryCode = COUNTRYCODE_INVALID;   // OemAddress = 0x02
        String modelCode = null;

        final int FREQCHANSEL_INVALID = -1; final int FREQCHANSEL_MIN = 0; final int FREQCHANSEL_MAX = 49;
        int freqChannelSelect = FREQCHANSEL_INVALID;
        int getFreqChannelSelect() {
            appendToLog("freqChannelSelect = " + freqChannelSelect);
            if (freqChannelSelect < FREQCHANSEL_MIN || freqChannelSelect > FREQCHANSEL_MAX) {
                {
                    byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 1, 0x0C, 0, 0, 0, 0};
                    mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_RFTC_FRQCH_SEL, false, msgBuffer);
                }
            }
            return freqChannelSelect;
        }
        boolean setFreqChannelSelect(int freqChannelSelect) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 1, 0x0C, 0, 0, 0, 0};
            if (freqChannelSelect < FREQCHANSEL_MIN || freqChannelSelect > FREQCHANSEL_MAX)   freqChannelSelect = mDefault.freqChannelSelect;
            //if (this.freqChannelSelect == freqChannelSelect && sameCheck)  return true;
            appendToLog("freqChannelSelect = " + freqChannelSelect);
            msgBuffer[4] = (byte) (freqChannelSelect);
            this.freqChannelSelect = freqChannelSelect;
            freqChannelSelect = FREQCHANCONFIG_INVALID; freqPllMultiplier = FREQPLLMULTIPLIER_INVALID;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_RFTC_FRQCH_SEL, true, msgBuffer);
        }

        final int FREQCHANCONFIG_INVALID = -1; final int FREQCHANCONFIG_MIN = 0; final int FREQCHANCONFIG_MAX = 1;
        int freqChannelConfig = FREQCHANCONFIG_INVALID;
        int getFreqChannelConfig() {
            if (freqChannelConfig < FREQCHANCONFIG_MIN || freqChannelConfig > FREQCHANCONFIG_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 2, 0x0C, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_RFTC_FRQCH_CFG, false, msgBuffer);
            }
            return freqChannelConfig;
        }
        boolean setFreqChannelConfig(boolean on) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 2, 0x0C, 0, 0, 0, 0};
            boolean onCurrent = false;
            if (freqChannelConfig != 0) onCurrent = true;
//            if (onCurrent == on && sameCheck)  return true;
            if (on) {
                msgBuffer[4] = 1;
                freqChannelConfig = 1;
            } else {
                freqChannelConfig = 0;
            }
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_RFTC_FRQCH_CFG, true, msgBuffer);
        }

        final int FREQPLLMULTIPLIER_INVALID = -1;
        int freqPllMultiplier = FREQPLLMULTIPLIER_INVALID;
        int getFreqPllMultiplier() {
            if (freqPllMultiplier == FREQPLLMULTIPLIER_INVALID) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 3, 0x0C, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_RFTC_FRQCH_DESC_PLLDIVMULT, false, msgBuffer);
            }
            return freqPllMultiplier;
        }
        boolean setFreqPllMultiplier(int freqPllMultiplier) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 3, 0x0C, 0, 0, 0, 0};
            msgBuffer[4] = (byte)(freqPllMultiplier & 0xFF);
            msgBuffer[5] = (byte)((freqPllMultiplier >> 8) & 0xFF);
            msgBuffer[6] = (byte)((freqPllMultiplier >> 16) & 0xFF);
            msgBuffer[7] = (byte)((freqPllMultiplier >> 24) & 0xFF);
            this.freqPllMultiplier = freqPllMultiplier;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_RFTC_FRQCH_DESC_PLLDIVMULT, true, msgBuffer);
        }

        final int FREQPLLDAC_INVALID = -1;
        int freqPllDac = FREQPLLDAC_INVALID;
        int getFreqPllDac() {
            if (freqPllDac == FREQPLLDAC_INVALID) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 4, 0x0C, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_RFTC_FRQCH_DESC_PLLDACCTL, false, msgBuffer);
            }
            return freqPllDac;
        }

        boolean setFreqChannelOverride(int freqStart) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 8, 0x0C, 0, 0, 0, 0};
            msgBuffer[4] = (byte)(freqStart & 0xFF);
            msgBuffer[5] = (byte)((freqStart >> 8) & 0xFF);
            msgBuffer[6] = (byte)((freqStart >> 16) & 0xFF);
            msgBuffer[7] = (byte)((freqStart >> 24) & 0xFF);
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_RFTC_FRQCH_CMDSTART, true, msgBuffer);
        }
    }

    class AntennaSelectedData {
        AntennaSelectedData(boolean set_default_setting, int default_setting_type) {
            if (default_setting_type < 0)    default_setting_type = 0;
            if (default_setting_type > 5)    default_setting_type = 5;
            mDefault = new AntennaSelectedData_default(default_setting_type);
            if (false && set_default_setting) {
                antennaEnable = mDefault.antennaEnable;
                antennaInventoryMode = mDefault.antennaInventoryMode;
                antennaLocalAlgo = mDefault.antennaLocalAlgo;
                antennaLocalStartQ = mDefault.antennaLocalStartQ;
                antennaProfileMode = mDefault.antennaProfileMode;
                antennaLocalProfile = mDefault.antennaLocalProfile;
                antennaFrequencyMode = mDefault.antennaFrequencyMode;
                antennaLocalFrequency = mDefault.antennaLocalFrequency;
                antennaStatus = mDefault.antennaStatus;
                antennaDefine = mDefault.antennaDefine;
                antennaDwell = mDefault.antennaDwell;
                antennaPower = mDefault.antennaPower; appendToLog("antennaPower is set to default " + antennaPower);
                antennaInvCount = mDefault.antennaInvCount;
            }
        }

        class AntennaSelectedData_default {
            AntennaSelectedData_default(int set_default_setting) {
                antennaEnable = mDefaultArray.antennaEnable[set_default_setting];
                antennaInventoryMode = mDefaultArray.antennaInventoryMode[set_default_setting];
                antennaLocalAlgo = mDefaultArray.antennaLocalAlgo[set_default_setting];
                antennaLocalStartQ = mDefaultArray.antennaLocalStartQ[set_default_setting];
                antennaProfileMode = mDefaultArray.antennaProfileMode[set_default_setting];
                antennaLocalProfile = mDefaultArray.antennaLocalProfile[set_default_setting];
                antennaFrequencyMode = mDefaultArray.antennaFrequencyMode[set_default_setting];
                antennaLocalFrequency = mDefaultArray.antennaLocalFrequency[set_default_setting];
                antennaStatus = mDefaultArray.antennaStatus[set_default_setting];
                antennaDefine = mDefaultArray.antennaDefine[set_default_setting];
                antennaDwell = mDefaultArray.antennaDwell[set_default_setting];
                antennaPower = mDefaultArray.antennaPower[set_default_setting];
                antennaInvCount = mDefaultArray.antennaInvCount[set_default_setting];
            }

            int antennaEnable;
            int antennaInventoryMode;
            int antennaLocalAlgo;
            int antennaLocalStartQ;
            int antennaProfileMode;
            int antennaLocalProfile;
            int antennaFrequencyMode;
            int antennaLocalFrequency;
            int antennaStatus;
            int antennaDefine;
            long antennaDwell;
            long antennaPower;
            long antennaInvCount;
        }
        AntennaSelectedData_default mDefault;

        private class AntennaSelectedData_defaultArray { //0 for invalid default,    1  for 0,       2 for 1 to 3,       3 for 4 to 7,       4 for 8 to   11,        5 for 12 to 15
            int[] antennaEnable =         { -1, 1, 0, 0, 0, 0 };
            int[] antennaInventoryMode = { -1, 0, 0, 0, 0, 0 };
            int[] antennaLocalAlgo =     { -1, 0, 0, 0, 0, 0 };
            int[] antennaLocalStartQ =    { -1, 0, 0, 0, 0, 0 };
            int[] antennaProfileMode =    { -1, 0, 0, 0, 0, 0 };
            int[] antennaLocalProfile =    { -1, 0, 0, 0, 0, 0 };
            int[] antennaFrequencyMode = { -1, 0, 0, 0, 0, 0 };
            int[] antennaLocalFrequency = { -1, 0, 0, 0, 0, 0 };
            int[] antennaStatus =           { -1, 0, 0, 0, 0, 0 };
            int[] antennaDefine =         { -1, 0, 0, 1, 2, 3 };
            long[] antennaDwell =      { -1, 2000, 2000, 2000, 2000, 2000 };
            long[] antennaPower =       { -1, 300, 0, 0, 0, 0 };
            long[] antennaInvCount =   { -1, 8192, 8192, 8192, 8192, 8192 };
        }
        AntennaSelectedData_defaultArray mDefaultArray = new AntennaSelectedData_defaultArray();

        final int ANTENABLE_INVALID = -1; final int ANTENABLE_MIN = 0; final int ANTENABLE_MAX = 1;
        int antennaEnable = ANTENABLE_INVALID;
        int getAntennaEnable() {
            if (antennaEnable < ANTENABLE_MIN || antennaEnable > ANTENABLE_MAX)
                getHST_ANT_DESC_CFG();
            return antennaEnable;
        }
        boolean setAntennaEnable(int antennaEnable) {
            return setAntennaEnable(antennaEnable, antennaInventoryMode, antennaLocalAlgo, antennaLocalStartQ,
                    antennaProfileMode, antennaLocalProfile, antennaFrequencyMode, antennaLocalFrequency);
        }

        final int ANTINVMODE_INVALID = 0; final int ANTINVMODE_MIN = 0; final int ANTINVMODE_MAX = 1;
        int antennaInventoryMode = ANTINVMODE_INVALID;
        int getAntennaInventoryMode() {
            if (antennaInventoryMode < ANTPROFILEMODE_MIN || antennaInventoryMode > ANTPROFILEMODE_MAX)
                getHST_ANT_DESC_CFG();
            return antennaInventoryMode;
        }
        boolean setAntennaInventoryMode(int antennaInventoryMode) {
            return setAntennaEnable(antennaEnable, antennaInventoryMode, antennaLocalAlgo, antennaLocalStartQ, antennaProfileMode,
                    antennaLocalProfile, antennaFrequencyMode, antennaLocalFrequency);
        }

        final int ANTLOCALALGO_INVALID = 0; final int ANTLOCALALGO_MIN = 0; final int ANTLOCALALGO_MAX = 5;
        int antennaLocalAlgo = ANTLOCALALGO_INVALID;
        int getAntennaLocalAlgo() {
            if (antennaLocalAlgo < ANTLOCALALGO_MIN || antennaLocalAlgo > ANTLOCALALGO_MAX)
                getHST_ANT_DESC_CFG();
            return antennaLocalAlgo;
        }
        boolean setAntennaLocalAlgo(int antennaLocalAlgo) {
            return setAntennaEnable(antennaEnable, antennaInventoryMode, antennaLocalAlgo, antennaLocalStartQ,
                    antennaProfileMode, antennaLocalProfile, antennaFrequencyMode, antennaLocalFrequency);
        }

        final int ANTLOCALSTARTQ_INVALID = 0; final int ANTLOCALSTARTQ_MIN = 0; final int ANTLOCALSTARTQ_MAX = 15;
        int antennaLocalStartQ = ANTLOCALSTARTQ_INVALID;
        int getAntennaLocalStartQ() {
            if (antennaLocalStartQ < ANTLOCALSTARTQ_MIN || antennaLocalStartQ > ANTLOCALSTARTQ_MAX)
                getHST_ANT_DESC_CFG();
            return antennaLocalStartQ;
        }
        boolean setAntennaLocalStartQ(int antennaLocalStartQ) {
            return setAntennaEnable(antennaEnable, antennaInventoryMode, antennaLocalAlgo, antennaLocalStartQ,
                    antennaProfileMode, antennaLocalProfile, antennaFrequencyMode, antennaLocalFrequency);
        }

        final int ANTPROFILEMODE_INVALID = 0; final int ANTPROFILEMODE_MIN = 0; final int ANTPROFILEMODE_MAX = 1;
        int antennaProfileMode = ANTPROFILEMODE_INVALID;
        int getAntennaProfileMode() {
            if (antennaProfileMode < ANTPROFILEMODE_MIN || antennaProfileMode > ANTPROFILEMODE_MAX)
                getHST_ANT_DESC_CFG();
            return antennaProfileMode;
        }
        boolean setAntennaProfileMode(int antennaProfileMode) {
            return setAntennaEnable(antennaEnable, antennaInventoryMode, antennaLocalAlgo, antennaLocalStartQ,
                    antennaProfileMode, antennaLocalProfile, antennaFrequencyMode, antennaLocalFrequency);
        }

        final int ANTLOCALPROFILE_INVALID = 0; final int ANTLOCALPROFILE_MIN = 0; final int ANTLOCALPROFILE_MAX = 5;
        int antennaLocalProfile = ANTLOCALPROFILE_INVALID;
        int getAntennaLocalProfile() {
            if (antennaLocalProfile < ANTLOCALPROFILE_MIN || antennaLocalProfile > ANTLOCALPROFILE_MIN)
                getHST_ANT_DESC_CFG();
            return antennaLocalProfile;
        }
        boolean setAntennaLocalProfile(int antennaLocalProfile) {
            return setAntennaEnable(antennaEnable, antennaInventoryMode, antennaLocalAlgo, antennaLocalStartQ,
                    antennaProfileMode, antennaLocalProfile, antennaFrequencyMode, antennaLocalFrequency);
        }

        final int ANTFREQMODE_INVALID = 0; final int ANTFREQMODE_MIN = 0; final int ANTFREQMODE_MAX = 1;
        int antennaFrequencyMode = ANTFREQMODE_INVALID;
        int getAntennaFrequencyMode() {
            if (antennaFrequencyMode < ANTFREQMODE_MIN || antennaFrequencyMode > ANTFREQMODE_MAX)
                getHST_ANT_DESC_CFG();
            return antennaFrequencyMode;
        }
        boolean setAntennaFrequencyMode(int antennaFrequencyMode) {
            return setAntennaEnable(antennaEnable, antennaInventoryMode, antennaLocalAlgo, antennaLocalStartQ,
                    antennaProfileMode, antennaLocalProfile, antennaFrequencyMode, antennaLocalFrequency);
        }

        final int ANTLOCALFREQ_INVALID = 0; final int ANTLOCALFREQ_MIN = 0; final int ANTLOCALFREQ_MAX = 49;
        int antennaLocalFrequency = ANTLOCALFREQ_INVALID;
        int getAntennaLocalFrequency() {
            if (antennaLocalFrequency < ANTLOCALFREQ_MIN || antennaLocalFrequency > ANTLOCALFREQ_MAX)
                getHST_ANT_DESC_CFG();
            return antennaLocalFrequency;
        }
        boolean setAntennaLocalFrequency(int antennaLocalFrequency) {
            return setAntennaEnable(antennaEnable, antennaInventoryMode, antennaLocalAlgo, antennaLocalStartQ,
                    antennaProfileMode, antennaLocalProfile, antennaFrequencyMode, antennaLocalFrequency);
        }

        private boolean getHST_ANT_DESC_CFG() {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 2, 7, 0, 0, 0, 0};
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_DESC_CFG, false, msgBuffer);
        }
        boolean setAntennaEnable(int antennaEnable, int antennaInventoryMode, int antennaLocalAlgo, int antennaLocalStartQ,
                                        int antennaProfileMode, int antennaLocalProfile,
                                        int antennaFrequencyMode, int antennaLocalFrequency) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 2, 7, 0, 0, 0, 0};
            if (antennaEnable < ANTENABLE_MIN || antennaEnable > ANTENABLE_MAX)
                antennaEnable = mDefault.antennaEnable;
            if (antennaInventoryMode < ANTINVMODE_MIN || antennaInventoryMode > ANTINVMODE_MAX)
                antennaInventoryMode = mDefault.antennaInventoryMode;
            if (antennaLocalAlgo < ANTLOCALALGO_MIN || antennaLocalAlgo > ANTLOCALALGO_MAX)
                antennaLocalAlgo = mDefault.antennaLocalAlgo;
            if (antennaLocalStartQ < ANTLOCALSTARTQ_MIN || antennaLocalStartQ > ANTLOCALSTARTQ_MAX)
                antennaLocalStartQ = mDefault.antennaLocalStartQ;
            if (antennaProfileMode < ANTPROFILEMODE_MIN || antennaProfileMode > ANTPROFILEMODE_MAX)
                antennaProfileMode = mDefault.antennaProfileMode;
            if (antennaLocalProfile < ANTLOCALPROFILE_MIN || antennaLocalProfile > ANTLOCALPROFILE_MAX)
                antennaLocalProfile = mDefault.antennaLocalProfile;
            if (antennaFrequencyMode < ANTFREQMODE_MIN || antennaFrequencyMode > ANTFREQMODE_MAX)
                antennaFrequencyMode = mDefault.antennaFrequencyMode;
            if (antennaLocalFrequency < ANTLOCALFREQ_MIN || antennaLocalFrequency > ANTLOCALFREQ_MAX)
                antennaLocalFrequency = mDefault.antennaLocalFrequency;
            if (this.antennaEnable == antennaEnable && this.antennaInventoryMode == antennaInventoryMode && this.antennaLocalAlgo == antennaLocalAlgo
                    && this.antennaLocalStartQ == antennaLocalStartQ && this.antennaProfileMode == antennaProfileMode && this.antennaLocalProfile == antennaLocalProfile
                    && this.antennaFrequencyMode == antennaFrequencyMode && this.antennaLocalFrequency == antennaLocalFrequency
                    && sameCheck)
                return true;
            msgBuffer[4] |= antennaEnable;
            msgBuffer[4] |= (antennaInventoryMode << 1);
            msgBuffer[4] |= (antennaLocalAlgo << 2);
            msgBuffer[4] |= (antennaLocalStartQ << 4);
            msgBuffer[5] |= antennaProfileMode;
            msgBuffer[5] |= (antennaLocalProfile << 1);
            msgBuffer[5] |= (antennaFrequencyMode << 5);
            msgBuffer[5] |= ((antennaLocalFrequency & 0x03) << 6);
            msgBuffer[6] |= (antennaLocalFrequency >> 2);
            this.antennaEnable = antennaEnable;
            this.antennaInventoryMode = antennaInventoryMode;
            this.antennaLocalAlgo = antennaLocalAlgo;
            this.antennaLocalStartQ = antennaLocalStartQ;
            this.antennaProfileMode = antennaProfileMode;
            this.antennaLocalProfile = antennaLocalProfile;
            this.antennaFrequencyMode = antennaFrequencyMode;
            this.antennaLocalFrequency = antennaLocalFrequency;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_DESC_CFG, true, msgBuffer);
        }

        final int ANTSTATUS_INVALID = -1; final int ANTSTATUS_MIN = 0; final int ANTSTATUS_MAX = 0xFFFFF;
        int antennaStatus = ANTSTATUS_INVALID;
        int getAntennaStatus() {
            if (antennaStatus < ANTSTATUS_MIN || antennaStatus > ANTSTATUS_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 3, 7, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.MAC_ANT_DESC_STAT, false, msgBuffer);
            }
            return antennaStatus;
        }

        final int ANTDEFINE_INVALID = -1; final int ANTDEFINE_MIN = 0; final int ANTDEFINE_MAX = 3;
        int antennaDefine = ANTDEFINE_INVALID;
        int getAntennaDefine() {
            if (antennaDefine < ANTDEFINE_MIN || antennaDefine > ANTDEFINE_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 4, 7, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_DESC_PORTDEF, false, msgBuffer);
            }
            return antennaDefine;
        }

        final long ANTDWELL_INVALID = -1; final long ANTDWELL_MIN = 0; final long ANTDWELL_MAX = 0xFFFF;
        long antennaDwell = ANTDWELL_INVALID;
        long getAntennaDwell() {
            if (antennaDwell < ANTDWELL_MIN || antennaDwell > ANTDWELL_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 5, 7, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_DESC_DWELL, false, msgBuffer);
            }
            return antennaDwell;
        }
        boolean setAntennaDwell(long antennaDwell) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 5, 7, 0, 0, 0, 0};
            if (antennaDwell < ANTDWELL_MIN || antennaDwell > ANTDWELL_MAX)
                antennaDwell = mDefault.antennaDwell;
            if (this.antennaDwell == antennaDwell && sameCheck) return true;
            msgBuffer[4] = (byte) (antennaDwell % 256);
            msgBuffer[5] = (byte) ((antennaDwell >> 8) % 256);
            msgBuffer[6] = (byte) ((antennaDwell >> 16) % 256);
            msgBuffer[7] = (byte) ((antennaDwell >> 24) % 256);
            this.antennaDwell = antennaDwell;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_DESC_DWELL, true, msgBuffer);
        }

        final int ANTARGET_INVALID = -1; final int ANTARGET_MIN = 0; final int ANTARGET_MAX = 1;
        int antennaTarget = ANTARGET_INVALID;
        byte[] antennaInventoryRoundControl = null;
        final int ANTOGGLE_INVALID = -1; final int ANTOGGLE_MIN = 0; final int ANTOGGLE_MAX = 100;
        int antennaToggle = ANTOGGLE_INVALID;
        final int ANTRFMODE_INVALID = -1; final int ANTRFMODE_MIN = 1; final int ANTRFMODE_MAX = 15;
        int antennaRfMode = ANTRFMODE_INVALID;

        final long ANTPOWER_INVALID = -1; final long ANTPOWER_MIN = 0; final long ANTPOWER_MAX = 330; //Maximum 330\
        long antennaPower = ANTPOWER_INVALID;   //default value = 300
        long getAntennaPower() {
            if (antennaPower < ANTPOWER_MIN || antennaPower > ANTPOWER_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 6, 7, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_DESC_RFPOWER, false, msgBuffer);
            }
            return antennaPower;
        }
        boolean setAntennaPower(long antennaPower) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 6, 7, 0, 0, 0, 0};
            if (antennaPower < ANTPOWER_MIN || antennaPower > ANTPOWER_MAX)
                antennaPower = mDefault.antennaPower;
            if (this.antennaPower == antennaPower && sameCheck) return true;
            msgBuffer[4] = (byte) (antennaPower % 256);
            msgBuffer[5] = (byte) ((antennaPower >> 8) % 256);
            this.antennaPower = antennaPower;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_DESC_RFPOWER, true, msgBuffer);
        }

        final long ANTINVCOUNT_INVALID = -1; final long ANTINVCOUNT_MIN = 0; final long ANTINVCOUNT_MAX = 0xFFFFFFFFL;
        long antennaInvCount = ANTINVCOUNT_INVALID;
        long getAntennaInvCount() {
            if (antennaInvCount < ANTINVCOUNT_MIN || antennaInvCount > ANTINVCOUNT_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 7, 7, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_DESC_INV_CNT, false, msgBuffer);
            }
            return antennaInvCount;
        }
        boolean setAntennaInvCount(long antennaInvCount) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 7, 7, 0, 0, 0, 0};
            if (antennaInvCount < ANTINVCOUNT_MIN || antennaInvCount > ANTINVCOUNT_MAX)
                antennaInvCount = mDefault.antennaInvCount;
            if (this.antennaInvCount == antennaInvCount && sameCheck) return true;
            msgBuffer[4] = (byte) (antennaInvCount % 256);
            msgBuffer[5] = (byte) ((antennaInvCount >> 8) % 256);
            msgBuffer[6] = (byte) ((antennaInvCount >> 16) % 256);
            msgBuffer[7] = (byte) ((antennaInvCount >> 24) % 256);
            this.antennaInvCount = antennaInvCount;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_ANT_DESC_INV_CNT, true, msgBuffer);
        }
    }

    class InvSelectData {
        InvSelectData(boolean set_default_setting) {
            if (set_default_setting) {
                selectEnable = mDefault.selectEnable;
                selectTarget = mDefault.selectTarget;
                selectAction = mDefault.selectAction;
                selectDelay = mDefault.selectDelay;
                selectMaskBank = mDefault.selectMaskBank;
                selectMaskOffset = mDefault.selectMaskOffset;
                selectMaskLength = mDefault.selectMaskLength; appendToLog("getSelectMaskData with default selectMaskLength = " + selectMaskLength);
                selectMaskDataReady = mDefault.selectMaskDataReady;
            }
        }

        private class InvSelectData_default {
            int selectEnable = 0;
            int selectTarget = 0;
            int selectAction = 0;
            int selectDelay = 0;
            int selectMaskBank = 0;
            int selectMaskOffset = 0;
            int selectMaskLength = 0;
            byte[] selectMaskData0_31 = new byte[4 * 8]; byte selectMaskDataReady = 0;
        }
        InvSelectData_default mDefault = new InvSelectData_default();

        final int INVSELENABLE_INVALID = 0; final int INVSELENABLE_MIN = 0; final int INVSELENABLE_MAX = 1;
        int selectEnable = INVSELENABLE_INVALID;
        int getSelectEnable() {
            getRx000HostReg_HST_TAGMSK_DESC_CFG();
            return selectEnable;
        }
        boolean setSelectEnable(int selectEnable) {
            return setRx000HostReg_HST_TAGMSK_DESC_CFG(selectEnable, this.selectTarget, this.selectAction, this.selectDelay);
        }

        final int INVSELTARGET_INVALID = -1; final int INVSELTARGET_MIN = 0; final int INVSELTARGET_MAX = 7;
        int selectTarget = INVSELTARGET_INVALID;
        int getSelectTarget() {
            getRx000HostReg_HST_TAGMSK_DESC_CFG();
            return selectTarget;
        }

        final int INVSELACTION_INVALID = -1; final int INVSELACTION_MIN = 0; final int INVSELACTION_MAX = 7;
        int selectAction = INVSELACTION_INVALID;
        int getSelectAction() {
            getRx000HostReg_HST_TAGMSK_DESC_CFG();
            return selectAction;
        }

        final int INVSELDELAY_INVALID = -1; final int INVSELDELAY_MIN = 0; final int INVSELDELAY_MAX = 255;
        int selectDelay = INVSELDELAY_INVALID;
        int getSelectDelay() {
            getRx000HostReg_HST_TAGMSK_DESC_CFG();
            return selectDelay;
        }

        boolean getRx000HostReg_HST_TAGMSK_DESC_CFG() {
            if (selectEnable < INVSELENABLE_MIN || selectEnable > INVSELENABLE_MAX
                    || selectTarget < INVSELTARGET_MIN || selectTarget > INVSELTARGET_MAX
                    || selectAction < INVSELACTION_MIN || selectAction > INVSELACTION_MAX
                    || selectDelay < INVSELDELAY_MIN || selectDelay > INVSELDELAY_MAX
                    ) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 1, 8, 0, 0, 0, 0};
                return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_DESC_CFG, false, msgBuffer);
            } else {
                return false;
            }
        }
        boolean setRx000HostReg_HST_TAGMSK_DESC_CFG(int selectEnable, int selectTarget, int selectAction, int selectDelay) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 1, 8, 0, 0, 0, 0};
            if (selectEnable < INVSELENABLE_MIN || selectEnable > INVSELENABLE_MAX)
                selectEnable = mDefault.selectEnable;
            if (selectTarget < INVSELTARGET_MIN || selectTarget > INVSELTARGET_MAX)
                selectTarget = mDefault.selectTarget;
            if (selectAction < INVSELACTION_MIN || selectAction > INVSELACTION_MAX)
                selectAction = mDefault.selectAction;
            int selectDalay0 = selectDelay;
            if (selectDelay < INVSELDELAY_MIN || selectDelay > INVSELDELAY_MAX)
                selectDelay = mDefault.selectDelay;
            if (this.selectEnable == selectEnable && this.selectTarget == selectTarget && this.selectAction == selectAction && this.selectDelay == selectDelay && sameCheck) return true;
            msgBuffer[4] |= (byte) (selectEnable & 0x1);
            msgBuffer[4] |= (byte) ((selectTarget & 0x07) << 1);
            msgBuffer[4] |= (byte) ((selectAction & 0x07) << 4);
            msgBuffer[5] |= (byte) (selectDelay & 0xFF);
            this.selectEnable = selectEnable;
            this.selectTarget = selectTarget;
            this.selectAction = selectAction;
            this.selectDelay = selectDelay;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_DESC_CFG, true, msgBuffer);
        }

        final int INVSELMBANK_INVALID = -1; final int INVSELMBANK_MIN = 0; final int INVSELMBANK_MAX = 3;
        int selectMaskBank = INVSELMBANK_INVALID;
        int getSelectMaskBank() {
            if (selectMaskBank < INVSELMBANK_MIN || selectMaskBank > INVSELMBANK_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 2, 8, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_BANK, false, msgBuffer);
            }
            return selectMaskBank;
        }
        boolean setSelectMaskBank(int selectMaskBank) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 2, 8, 0, 0, 0, 0};
            if (selectMaskBank < INVSELMBANK_MIN || selectMaskBank > INVSELMBANK_MAX)
                selectMaskBank = mDefault.selectMaskBank;
            if (this.selectMaskBank == selectMaskBank && sameCheck) return true;
            msgBuffer[4] |= (byte) (selectMaskBank & 0x3);
            this.selectMaskBank = selectMaskBank;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_BANK, true, msgBuffer);
        }

        final int INVSELMOFFSET_INVALID = -1; final int INVSELMOFFSET_MIN = 0; final int INVSELMOFFSET_MAX = 0xFFFF;
        int selectMaskOffset = INVSELMOFFSET_INVALID;
        int getSelectMaskOffset() {
            if (selectMaskOffset < INVSELMOFFSET_MIN || selectMaskOffset > INVSELMOFFSET_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 3, 8, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_PTR, false, msgBuffer);
            }
            return selectMaskOffset;
        }
        boolean setSelectMaskOffset(int selectMaskOffset) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 3, 8, 0, 0, 0, 0};
            if (selectMaskOffset < INVSELMOFFSET_MIN || selectMaskOffset > INVSELMOFFSET_MAX)
                selectMaskOffset = mDefault.selectMaskOffset;
            if (this.selectMaskOffset == selectMaskOffset && sameCheck) return true;
            msgBuffer[4] |= (byte) (selectMaskOffset & 0xFF);
            msgBuffer[5] |= (byte) ((selectMaskOffset >> 8) & 0xFF);
            this.selectMaskOffset = selectMaskOffset;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_PTR, true, msgBuffer);
        }

        final int INVSELMLENGTH_INVALID = -1; final int INVSELMLENGTH_MIN = 0; final int INVSELMLENGTH_MAX = 255;
        int selectMaskLength = INVSELMLENGTH_INVALID;
        int getSelectMaskLength() {
            appendToLog("getSelectMaskData with selectMaskLength = " + selectMaskLength);
            if (selectMaskLength < INVSELMLENGTH_MIN || selectMaskOffset > INVSELMLENGTH_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 4, 8, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_LEN, false, msgBuffer);
            }
            return selectMaskLength;
        }
        boolean setSelectMaskLength(int selectMaskLength) {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 4, 8, 0, 0, 0, 0};
            if (selectMaskLength < INVSELMLENGTH_MIN) selectMaskLength = INVSELMLENGTH_MIN;
            else if (selectMaskLength > INVSELMLENGTH_MAX) selectMaskLength = INVSELMLENGTH_MAX;
            if (this.selectMaskLength == selectMaskLength && sameCheck) return true;
            msgBuffer[4] |= (byte) (selectMaskLength & 0xFF);
            if (selectMaskLength == INVSELMLENGTH_MAX) msgBuffer[5] = 1;
            this.selectMaskLength = selectMaskLength; appendToLog("getSelectMaskData with saved selectMaskLength = " + selectMaskLength);
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_PTR, true, msgBuffer);
        }

        byte[] selectMaskData0_31 = new byte[4 * 8]; byte selectMaskDataReady = 0;
        String getRx000SelectMaskData() {
            appendToLog("getSelectMaskData with selectMaskData0_31 = " + byteArrayToString(selectMaskData0_31));
            int length = selectMaskLength;
            String strValue = "";
            if (length < 0) {
                getSelectMaskLength();
            } else {
                for (int i = 0; i < 8; i++) {
                    if (length > 0) {
                        if ((selectMaskDataReady & (0x01 << i)) == 0) {
                            byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 5, 8, 0, 0, 0, 0};
                            msgBuffer[2] += i;
                            mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_0_3, false, msgBuffer);

                            strValue = null;
                            break;
                        } else {
                            for (int j = 0; j < 4; j++) {
                                if (DEBUG) appendToLog("i = " + i + ", j = " + j + ", selectMaskData0_31 = " + selectMaskData0_31[i * 4 + j]);
                                strValue += String.format("%02X", selectMaskData0_31[i * 4 + j]);
                            }
                        }
                        length -= 32;
                    }
                }
            }
            return strValue;
        }
        boolean setRx000SelectMaskData(String maskData) {
            int length = maskData.length();
            for (int i = 0; i < 8; i++) {
                if (length > 0) {
                    length -= 8;

                    byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 5, 8, 0, 0, 0, 0};
                    String hexString = "0123456789ABCDEF";
                    for (int j = 0; j < 8; j++) {
                        if (i * 8 + j + 1 <= maskData.length()) {
                            String subString = maskData.substring(i * 8 + j, i * 8 + j + 1).toUpperCase();
                            int k = 0;
                            for (k = 0; k < 16; k++) {
                                if (subString.matches(hexString.substring(k, k + 1))) {
                                    break;
                                }
                            }
                            if (k == 16) return false;
//                                appendToLog("setSelectMaskData(" + maskData +"): i=" + i + ", j=" + j + ", k=" + k);
                            if ((j / 2) * 2 == j) {
                                msgBuffer[4 + j / 2] |= (byte) (k << 4);
                            } else {
                                msgBuffer[4 + j / 2] |= (byte) (k);
                            }
                        }
                    }
                    msgBuffer[2] = (byte) ((msgBuffer[2] & 0xFF) + i);
                    if (mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_TAGMSK_0_3, true, msgBuffer) == false)
                        return false;
                    else {
                        selectMaskDataReady |= (0x01 << i);
                        if (DEBUG) appendToLog("Old selectMaskData0_31 = " + byteArrayToString(selectMaskData0_31));
                        System.arraycopy(msgBuffer, 4, selectMaskData0_31, i * 4, 4);
                        if (DEBUG) appendToLog("New selectMaskData0_31 = " + byteArrayToString(selectMaskData0_31));
                    }
                }
            }
            return true;
        }
    }

    class AlgoSelectedData {
        AlgoSelectedData(boolean set_default_setting, int default_setting_type) {
            if (default_setting_type < 0) default_setting_type = 0;
            if (default_setting_type > 4) default_setting_type = 4;
            mDefault = new AlgoSelectedData_default(default_setting_type);
            if (set_default_setting) {
                algoStartQ = mDefault.algoStartQ;
                algoMaxQ = mDefault.algoMaxQ;
                algoMinQ = mDefault.algoMinQ;
                algoMaxRep = mDefault.algoMaxRep;
                algoHighThres = mDefault.algoHighThres;
                algoLowThres = mDefault.algoLowThres;
                algoRetry = mDefault.algoRetry;
                algoAbFlip = mDefault.algoAbFlip;
                algoRunTilZero = mDefault.algoRunTilZero;
            }
        }

        class AlgoSelectedData_default {
            AlgoSelectedData_default(int set_default_setting) {
                algoStartQ = mDefaultArray.algoStartQ[set_default_setting];
                algoMaxQ = mDefaultArray.algoMaxQ[set_default_setting];
                algoMinQ = mDefaultArray.algoMinQ[set_default_setting];
                algoMaxRep = mDefaultArray.algoMaxRep[set_default_setting];
                algoHighThres = mDefaultArray.algoHighThres[set_default_setting];
                algoLowThres = mDefaultArray.algoLowThres[set_default_setting];
                algoRetry = mDefaultArray.algoRetry[set_default_setting];
                algoAbFlip = mDefaultArray.algoAbFlip[set_default_setting];
                algoRunTilZero = mDefaultArray.algoRunTilZero[set_default_setting];
            }

            int algoStartQ = -1;
            int algoMaxQ = -1;
            int algoMinQ = -1;
            int algoMaxRep = -1;
            int algoHighThres = -1;
            int algoLowThres = -1;
            int algoRetry = -1;
            int algoAbFlip = -1;
            int algoRunTilZero = -1;
        }
        AlgoSelectedData_default mDefault;

        class AlgoSelectedData_defaultArray { //0 for invalid default,    1 for 0,    2 for 1,     3 for 2,   4 for 3
            int[] algoStartQ =     { -1, 0, 0, 0, 4 };
            int[] algoMaxQ =      { -1, 0, 0, 0, 15 };
            int[] algoMinQ =      { -1, 0, 0, 0, 0 };
            int[] algoMaxRep =    { -1, 0, 0, 0, 4 };
            int[] algoHighThres =  { -1, 0, 5, 5, 5 };
            int[] algoLowThres =  { -1, 0, 3, 3, 3 };
            int[] algoRetry =      { -1, 0, 0, 0, 0 };
            int[] algoAbFlip =     { -1, 0, 1, 1, 1 };
            int[] algoRunTilZero = { -1, 0, 0, 0, 0 };
        }
        AlgoSelectedData_defaultArray mDefaultArray = new AlgoSelectedData_defaultArray();

        final int ALGOSTARTQ_INVALID = -1; final int ALGOSTARTQ_MIN = 0; final int ALGOSTARTQ_MAX = 15;
        int algoStartQ = ALGOSTARTQ_INVALID;
        int getAlgoStartQ(boolean getInvalid) {
            if (getInvalid && (algoStartQ < ALGOSTARTQ_MIN || algoStartQ > ALGOSTARTQ_MAX)) getHST_INV_ALG_PARM_0();
            return algoStartQ;
        }
        boolean setAlgoStartQ(int algoStartQ) {
            return setAlgoStartQ(algoStartQ, algoMaxQ, algoMinQ, algoMaxRep, algoHighThres, algoLowThres);
        }

        final int ALGOMAXQ_INVALID = -1; final int ALGOMAXQ_MIN = 0; final int ALGOMAXQ_MAX = 15;
        int algoMaxQ = ALGOMAXQ_INVALID;
        int getAlgoMaxQ() {
            if (algoMaxQ < ALGOMAXQ_MIN || algoMaxQ > ALGOMAXQ_MAX) getHST_INV_ALG_PARM_0();
            return algoMaxQ;
        }
        boolean setAlgoMaxQ(int algoMaxQ) {
            return setAlgoStartQ(algoStartQ, algoMaxQ, algoMinQ, algoMaxRep, algoHighThres, algoLowThres);
        }

        final int ALGOMINQ_INVALID = -1; final int ALGOMINQ_MIN = 0; final int ALGOMINQ_MAX = 15;
        int algoMinQ = ALGOMINQ_INVALID;
        int getAlgoMinQ() {
            if (algoMinQ < ALGOMINQ_MIN || algoMinQ > ALGOMINQ_MAX) getHST_INV_ALG_PARM_0();
            return algoMinQ;
        }
        boolean setAlgoMinQ(int algoMinQ) {
            return setAlgoStartQ(algoStartQ, algoMaxQ, algoMinQ, algoMaxRep, algoHighThres, algoLowThres);
        }

        final int ALGOMAXREP_INVALID = -1; final int ALGOMAXREP_MIN = 0; final int ALGOMAXREP_MAX = 255;
        int algoMaxRep = ALGOMAXREP_INVALID;
        int getAlgoMaxRep() {
            if (algoMaxRep < ALGOMAXREP_MIN || algoMaxRep > ALGOMAXREP_MAX) getHST_INV_ALG_PARM_0();
            return algoMaxRep;
        }
        boolean setAlgoMaxRep(int algoMaxRep) {
            return setAlgoStartQ(algoStartQ, algoMaxQ, algoMinQ, algoMaxRep, algoHighThres, algoLowThres);
        }

        final int ALGOHIGHTHRES_INVALID = -1; final int ALGOHIGHTHRES_MIN = 0; final int ALGOHIGHTHRES_MAX = 15;
        int algoHighThres = ALGOHIGHTHRES_INVALID;
        int getAlgoHighThres() {
            if (algoHighThres < ALGOHIGHTHRES_MIN || algoHighThres > ALGOHIGHTHRES_MAX)
                getHST_INV_ALG_PARM_0();
            return algoHighThres;
        }
        boolean setAlgoHighThres(int algoHighThres) {
            return setAlgoStartQ(algoStartQ, algoMaxQ, algoMinQ, algoMaxRep, algoHighThres, algoLowThres);
        }

        final int ALGOLOWTHRES_INVALID = -1; final int ALGOLOWTHRES_MIN = 0; final int ALGOLOWTHRES_MAX = 15;
        int algoLowThres = ALGOLOWTHRES_INVALID;
        int getAlgoLowThres() {
            if (algoLowThres < ALGOLOWTHRES_MIN || algoLowThres > ALGOLOWTHRES_MAX)
                getHST_INV_ALG_PARM_0();
            return algoLowThres;
        }
        boolean setAlgoLowThres(int algoLowThres) {
            return setAlgoStartQ(algoStartQ, algoMaxQ, algoMinQ, algoMaxRep, algoHighThres, algoLowThres);
        }

        private boolean getHST_INV_ALG_PARM_0() {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 3, 9, 0, 0, 0, 0};
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_ALG_PARM_0, false, msgBuffer);
        }
        boolean setAlgoStartQ(int startQ, int algoMaxQ, int algoMinQ, int algoMaxRep, int algoHighThres, int algoLowThres) {
            appendToLog("startQ = " + startQ + ", algoStartQ = " + this.algoStartQ);
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 3, 9, 0, 0, 0, 0};
            if (startQ < ALGOSTARTQ_MIN || startQ > ALGOSTARTQ_MAX) startQ = mDefault.algoStartQ;
            if (algoMaxQ < ALGOMAXQ_MIN || algoMaxQ > ALGOMAXQ_MAX) algoMaxQ = mDefault.algoMaxQ;
            if (algoMinQ < ALGOMINQ_MIN || algoMinQ > ALGOMINQ_MAX) algoMinQ = mDefault.algoMinQ;
            if (algoMaxRep < ALGOMAXREP_MIN || algoMaxRep > ALGOMAXREP_MAX)
                algoMaxRep = mDefault.algoMaxRep;
            if (algoHighThres < ALGOHIGHTHRES_MIN || algoHighThres > ALGOHIGHTHRES_MAX)
                algoHighThres = mDefault.algoHighThres;
            if (algoLowThres < ALGOLOWTHRES_MIN || algoLowThres > ALGOLOWTHRES_MAX)
                algoLowThres = mDefault.algoLowThres;
            if (false && this.algoStartQ == startQ && this.algoMaxQ == algoMaxQ && this.algoMinQ == algoMinQ
                    && this.algoMaxRep == algoMaxRep && this.algoHighThres == algoHighThres && this.algoLowThres == algoLowThres
                    && sameCheck)
                return true;
            appendToLog("algoMaxRep = " + algoMaxRep + ", algoMaxRep = " + algoMaxRep + ", algoLowThres = " + algoLowThres);
            msgBuffer[4] |= (byte) (startQ & 0x0F);
            msgBuffer[4] |= (byte) ((algoMaxQ & 0x0F) << 4);
            msgBuffer[5] |= (byte) (algoMinQ & 0x0F);
            msgBuffer[5] |= (byte) ((algoMaxRep & 0xF) << 4);
            msgBuffer[6] |= (byte) ((algoMaxRep & 0xF0) >> 4);
            msgBuffer[6] |= (byte) ((algoHighThres & 0x0F) << 4);
            msgBuffer[7] |= (byte) (algoLowThres & 0x0F);
            this.algoStartQ = startQ;
            this.algoMaxQ = algoMaxQ;
            this.algoMinQ = algoMinQ;
            this.algoMaxRep = algoMaxRep;
            this.algoHighThres = algoHighThres;
            this.algoLowThres = algoLowThres;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_ALG_PARM_0, true, msgBuffer);
        }

        final int ALGORETRY_INVALID = -1; final int ALGORETRY_MIN = 0; final int ALGORETRY_MAX = 255;
        int algoRetry = ALGORETRY_INVALID;
        int getAlgoRetry() {
            if (algoRetry < ALGORETRY_MIN || algoRetry > ALGORETRY_MAX) {
                byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 4, 9, 0, 0, 0, 0};
                mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_ALG_PARM_1, false, msgBuffer);
            }
            return algoRetry;
        }
        boolean setAlgoRetry(int algoRetry) {
            if (algoRetry < ALGORETRY_MIN || algoRetry > ALGORETRY_MAX)
                algoRetry = mDefault.algoRetry;
            if (false && this.algoRetry == algoRetry && sameCheck) return true;
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 4, 9, 0, 0, 0, 0};
            msgBuffer[4] = (byte) algoRetry;
            this.algoRetry = algoRetry;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_ALG_PARM_1, true, msgBuffer);
        }

        final int ALGOABFLIP_INVALID = -1; final int ALGOABFLIP_MIN = 0; final int ALGOABFLIP_MAX = 1;
        int algoAbFlip = ALGOABFLIP_INVALID;
        int getAlgoAbFlip() {
            if (algoAbFlip < ALGOABFLIP_MIN || algoAbFlip > ALGOABFLIP_MAX) getHST_INV_ALG_PARM_2();
            return algoAbFlip;
        }
        boolean setAlgoAbFlip(int algoAbFlip) {
            return setAlgoAbFlip(algoAbFlip, algoRunTilZero);
        }

        final int ALGORUNTILZERO_INVALID = -1; final int ALGORUNTILZERO_MIN = 0; final int ALGORUNTILZERO_MAX = 1;
        int algoRunTilZero = ALGORUNTILZERO_INVALID;
        int getAlgoRunTilZero() {
            if (algoRunTilZero < ALGORUNTILZERO_MIN || algoRunTilZero > ALGORUNTILZERO_MAX) getHST_INV_ALG_PARM_2();
            return algoRunTilZero;
        }
        boolean setAlgoRunTilZero(int algoRunTilZero) {
            return setAlgoAbFlip(algoAbFlip, algoRunTilZero);
        }

        private boolean getHST_INV_ALG_PARM_2() {
            byte[] msgBuffer = new byte[]{(byte) 0x70, 0, 5, 9, 0, 0, 0, 0};
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_ALG_PARM_2, false, msgBuffer);
        }
        boolean setAlgoAbFlip(int algoAbFlip, int algoRunTilZero) {
            if (algoAbFlip < ALGOABFLIP_MIN || algoAbFlip > ALGOABFLIP_MAX)
                algoAbFlip = mDefault.algoAbFlip;
            if (algoRunTilZero < ALGORUNTILZERO_MIN || algoRunTilZero > ALGORUNTILZERO_MAX)
                algoRunTilZero = mDefault.algoRunTilZero;
            appendToLog("this.algoAbFlip  = " + this.algoAbFlip + ", algoAbFlip = " + algoAbFlip + ", this.algoRunTilZero = " + this.algoRunTilZero + ", algoRunTilZero = " + algoRunTilZero);
            if (false && this.algoAbFlip == algoAbFlip && this.algoRunTilZero == algoRunTilZero && sameCheck) return true;
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 5, 9, 0, 0, 0, 0};
            if (algoAbFlip != 0) {
                msgBuffer[4] |= 0x01;
            }
            if (algoRunTilZero != 0) {
                msgBuffer[4] |= 0x02;
            }
            this.algoAbFlip = algoAbFlip;
            this.algoRunTilZero = algoRunTilZero;
            return mRfidDevice.mRfidReaderChip.sendHostRegRequest(HostRegRequests.HST_INV_ALG_PARM_2, true, msgBuffer);
        }
    }

    class Rx000EngSetting {
        int narrowRSSI = -1, wideRSSI = -1;
        int getwideRSSI() {
            if (wideRSSI < 0) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                mRfidDevice.mRfidReaderChip.mRx000Setting.writeMAC(0x100, 0x05); //sub-command: 0x05, Arg0: reserved
                mRfidDevice.mRfidReaderChip.mRx000Setting.writeMAC(0x101,  3 + 0x20000); //Arg1: 15-0: number of RSSI sample
                mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(HostCommands.CMD_ENGTEST);
            } else appendToLog("Hello123: wideRSSI = " + wideRSSI);
            return wideRSSI;
        }
        int getnarrowRSSI() {
            if (narrowRSSI < 0) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                mRfidDevice.mRfidReaderChip.mRx000Setting.writeMAC(0x100, 0x05); //sub-command: 0x05, Arg0: reserved
                mRfidDevice.mRfidReaderChip.mRx000Setting.writeMAC(0x101,  3 + 0x20000); //Arg1: 15-0: number of RSSI sample
                mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(HostCommands.CMD_ENGTEST);
            } else appendToLog("Hello123: narrowRSSI = " + wideRSSI);
            return wideRSSI;
        }
        public void resetRSSI() {
            narrowRSSI = -1; wideRSSI = -1;
        }
    }

    class Rx000MbpSetting {
        final int RXGAIN_INVALID = -1; final int RXGAIN_MIN = 0; final int RXGAIN_MAX = 0x1FF;
        int rxGain = RXGAIN_INVALID;
        int getHighCompression() {
            int iRetValue = -1;
            if (rxGain < RXGAIN_MIN || rxGain > RXGAIN_MAX) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                mRfidDevice.mRfidReaderChip.mRx000Setting.setMBPAddress(0x450); appendToLog("70010004: getHighCompression");
                mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_MBPRDREG);
            } else iRetValue = (rxGain >> 8);
            return iRetValue;
        }
        int getRflnaGain() {
            int iRetValue = -1;
            if (rxGain < RXGAIN_MIN || rxGain > RXGAIN_MAX) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                mRfidDevice.mRfidReaderChip.mRx000Setting.setMBPAddress(0x450); appendToLog("70010004: getRflnaGain");
                mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_MBPRDREG);
            } else iRetValue = ((rxGain & 0xC0) >> 6);
            return iRetValue;
        }
        int getIflnaGain() {
            int iRetValue = -1;
            if (rxGain < RXGAIN_MIN || rxGain > RXGAIN_MAX) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                mRfidDevice.mRfidReaderChip.mRx000Setting.setMBPAddress(0x450); appendToLog("70010004: getIflnaGain");
                mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_MBPRDREG);
            } else iRetValue = ((rxGain & 0x38) >> 3);
            return iRetValue;
        }
        int getAgcGain() {
            int iRetValue = -1;
            if (rxGain < RXGAIN_MIN || rxGain > RXGAIN_MAX) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                mRfidDevice.mRfidReaderChip.mRx000Setting.setMBPAddress(0x450); appendToLog("70010004: getAgcGain");
                mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_MBPRDREG);
            } else iRetValue = (rxGain & 0x07);
            return iRetValue;
        }
        int getRxGain() {
            int iRetValue = -1;
            if (rxGain < RXGAIN_MIN || rxGain > RXGAIN_MAX) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                mRfidDevice.mRfidReaderChip.mRx000Setting.setMBPAddress(0x450);
                mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_MBPRDREG);
            } else iRetValue = rxGain;
            return iRetValue;
        }
        boolean setRxGain(int highCompression, int rflnagain, int iflnagain, int agcgain) {
            int rxGain_new = ((highCompression & 0x01) << 8) | ((rflnagain & 0x3) << 6) | ((iflnagain & 0x7) << 3) | (agcgain & 0x7);
            return setRxGain(rxGain_new);
        }
        boolean setRxGain(int rxGain_new) {
            boolean bRetValue = true;
            if ((rxGain_new != rxGain) || (sameCheck == false)) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                bRetValue = mRfidDevice.mRfidReaderChip.mRx000Setting.setMBPAddress(0x450); appendToLog("70010004: setRxGain");
                if (bRetValue != false) bRetValue = mRfidDevice.mRfidReaderChip.mRx000Setting.setMBPData(rxGain_new);
                if (bRetValue != false) bRetValue = mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_MBPWRREG);
                if (bRetValue != false) rxGain = rxGain_new;
            }
            return bRetValue;
        }
    }

    class Rx000OemSetting {
        final int COUNTRYCODE_INVALID = -1; final int COUNTRYCODE_MIN = 1; final int COUNTRYCODE_MAX = 9;
        int countryCode = COUNTRYCODE_INVALID;   // OemAddress = 0x02
        int getCountryCode() {
            if (countryCode < COUNTRYCODE_MIN || countryCode > COUNTRYCODE_MAX) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                mRfidDevice.mRfidReaderChip.mRx000Setting.setOEMAddress(2);
                mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_RDOEM);
            }
            return countryCode;
        }

        final int SERIALCODE_INVALID = -1;
        byte[] serialNumber = new byte[] { SERIALCODE_INVALID, 0, 0, 0, SERIALCODE_INVALID, 0, 0, 0, SERIALCODE_INVALID, 0, 0, 0, SERIALCODE_INVALID, 0, 0, 0 };
        String getSerialNumber() {
            boolean invalid = false;
            int length = serialNumber.length / 4;
            if (serialNumber.length % 4 != 0)   length++;
            for (int i = 0; i < length; i++) {
                if (serialNumber[4 * i] == SERIALCODE_INVALID) {    // OemAddress = 0x04 - 7
                    invalid = true;
                    mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                    mRfidDevice.mRfidReaderChip.mRx000Setting.setOEMAddress(0x04 + i);
                    mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_RDOEM);
                }
            }
            if (invalid)    return null;
            appendToLog("retValue = " + byteArrayToString(serialNumber));
            byte[] retValue = new byte[serialNumber.length];
            for (int i = 0; i < retValue.length; i++) {
                int j = (i/4)*4 + 3 - i%4;
                if (j >= serialNumber.length)   retValue[i] = serialNumber[i];
                else    retValue[i] = serialNumber[j];
                if (retValue[i] == 0) retValue[i] = 0x30;
            }
            appendToLog("retValue = " + byteArrayToString(retValue) + ", String = " + new String(retValue));
            return new String(retValue);
        }

        final int PRODUCT_SERIALCODE_INVALID = -1;
        byte[] productserialNumber = new byte[] { SERIALCODE_INVALID, 0, 0, 0, SERIALCODE_INVALID, 0, 0, 0, SERIALCODE_INVALID, 0, 0, 0, SERIALCODE_INVALID, 0, 0, 0 };
        String getProductSerialNumber() {
            boolean invalid = false;
            int length = productserialNumber.length / 4;
            if (productserialNumber.length % 4 != 0)   length++;
            for (int i = 0; i < length; i++) {
                if (productserialNumber[4 * i] == PRODUCT_SERIALCODE_INVALID) {    // OemAddress = 0x04 - 7
                    invalid = true;
                    mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                    mRfidDevice.mRfidReaderChip.mRx000Setting.setOEMAddress(0x08 + i);
                    mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_RDOEM);
                }
            }
            if (invalid)    return null;
            appendToLog("retValue = " + byteArrayToString(productserialNumber));
            byte[] retValue = new byte[productserialNumber.length];
            for (int i = 0; i < retValue.length; i++) {
                int j = (i/4)*4 + 3 - i%4;
                if (j >= productserialNumber.length)   retValue[i] = productserialNumber[i];
                else    retValue[i] = productserialNumber[j];
                if (retValue[i] == 0) retValue[i] = 0x30;
            }
            appendToLog("retValue = " + byteArrayToString(retValue) + ", String = " + new String(retValue));
            return new String(retValue);
        }

        final int VERSIONCODE_INVALID = -1; final int VERSIONCODE_MIN = 1; final int VERSIONCODE_MAX = 9;
        int versionCode = VERSIONCODE_INVALID;   // OemAddress = 0x02
        int getVersionCode() {
            if (versionCode < VERSIONCODE_MIN || versionCode > VERSIONCODE_MAX) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                mRfidDevice.mRfidReaderChip.mRx000Setting.setOEMAddress(0x0B);
                mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_RDOEM);
            }
            return versionCode;
        }

        String spcialCountryVersion = null;
        String getSpecialCountryVersion() {
            if (spcialCountryVersion == null) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                mRfidDevice.mRfidReaderChip.mRx000Setting.setOEMAddress(0x8E);
                mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_RDOEM);
                return "";
            }
            return spcialCountryVersion.replaceAll("[^A-Za-z0-9]", "");
        }

        final int FREQMODIFYCODE_INVALID = -1; final int FREQMODIFYCODE_MIN = 0; final int FREQMODIFYCODE_MAX = 0xAA;
        int freqModifyCode = FREQMODIFYCODE_INVALID;   // OemAddress = 0x8A
        int getFreqModifyCode() {
            if (freqModifyCode < FREQMODIFYCODE_MIN || freqModifyCode > FREQMODIFYCODE_MAX) {
                mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
                mRfidDevice.mRfidReaderChip.mRx000Setting.setOEMAddress(0x8F);
                mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(Cs108Connector.HostCommands.CMD_RDOEM);
            }
            return freqModifyCode;
        }

        void writeOEM(int address, int value) {
            mRfidDevice.mRfidReaderChip.setPwrManagementMode(false);
            mRfidDevice.mRfidReaderChip.mRx000Setting.setOEMAddress(address);
            mRfidDevice.mRfidReaderChip.mRx000Setting.setOEMData(value);
            mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(HostCommands.CMD_WROEM);
        }
    }

    boolean bFirmware_reset_before = false;
    final int RFID_READING_BUFFERSIZE = 1024;
    public class RfidReaderChip {
        byte[] mRfidToReading = new byte[RFID_READING_BUFFERSIZE];
        int mRfidToReadingOffset = 0;
        ArrayList<Cs108RfidData> mRx000ToWrite = new ArrayList<>();

        Rx000Setting mRx000Setting = new Rx000Setting(true);
        Rx000EngSetting mRx000EngSetting = new Rx000EngSetting();
        Rx000MbpSetting mRx000MbpSetting = new Rx000MbpSetting();
        Rx000OemSetting mRx000OemSetting = new Rx000OemSetting();

        ArrayList<Rx000pkgData> mRx000ToRead = new ArrayList<>();
        private boolean clearTempDataIn_request = false;
        boolean commandOperating;

        double decodeWideBandRSSI(byte byteRSSI) {
            byte mantissa = byteRSSI;
            mantissa &= 0x07;
            byte exponent = byteRSSI;
            exponent >>= 3;
            double dValue = 20 * log10(pow(2, exponent) * (1 + (mantissa / pow(2, 3))));
            return dValue;
        }

        long firmware_ontime_ms = 0; long date_time_ms = 0; boolean bRx000ToReading = false;
        void mRx000UplinkHandler() {
            if (bRx000ToReading) return;
            bRx000ToReading = true;
            int startIndex = 0;
            int startIndexOld = 0;
            int startIndexNew = 0;
            boolean packageFound = false; int packageType = 0;
            long lTime = System.currentTimeMillis();
            boolean bdebugging = false;
            if (mRfidDevice.mRfidToRead.size() != 0) { bdebugging = true; if (DEBUGTHREAD) appendToLog("mRx000UplinkHandler(): START"); }
            else if (DEBUGTHREAD) appendToLog("mRx000UplinkHandler(): START AAA");
            boolean bFirst = true;
            while (mRfidDevice.mRfidToRead.size() != 0) {
                if (isBleConnected() == false) {
                    mRfidDevice.mRfidToRead.clear();
                } else if (System.currentTimeMillis() - lTime > (intervalRx000UplinkHandler/2)) {
                    writeDebug2File("D" + String.valueOf(intervalRx000UplinkHandler) + ", " + System.currentTimeMillis() + ", Timeout");
                    appendToLogView("mRx000UplinkHandler_TIMEOUT !!! mRfidToRead.size() = " + mRfidDevice.mRfidToRead.size());
                    break;
                } else {
                    if (bFirst) { bFirst = false; writeDebug2File("D" + String.valueOf(intervalRx000UplinkHandler) + ", " + System.currentTimeMillis()); }
                    byte[] dataIn = mRfidDevice.mRfidToRead.get(0).dataValues;
                    long tagMilliSeconds = mRfidDevice.mRfidToRead.get(0).milliseconds;
                    boolean invalidSequence = mRfidDevice.mRfidToRead.get(0).invalidSequence;
                    if (false) appendToLog("mRx000UplinkHandler(): invalidSequence = " + invalidSequence + ", Processing data = " + byteArrayToString(dataIn) + ", length=" + dataIn.length + ", mRfidToReading.length=" + mRfidToReading.length + ", startIndex=" + startIndex + ", startIndexNew=" + startIndexNew + ", mRfidToReadingOffset=" + mRfidToReadingOffset);
                    mRfidDevice.mRfidToRead.remove(0);

                    if (dataIn.length >= mRfidToReading.length - mRfidToReadingOffset) {
                        byte[] unhandledBytes = new byte[mRfidToReadingOffset];
                        System.arraycopy(mRfidToReading, 0, unhandledBytes, 0, unhandledBytes.length);
                        if (true) appendToLogView("mRx000UplinkHandler(): ERROR insufficient buffer, mRfidToReadingOffset=" + mRfidToReadingOffset + ", dataIn.length=" + dataIn.length + ", clear mRfidToReading: " + byteArrayToString(unhandledBytes));
                        byte[] mRfidToReadingNew = new byte[RFID_READING_BUFFERSIZE];
                        mRfidToReading = mRfidToReadingNew;
                        mRfidToReadingOffset = 0;
                        invalidUpdata++;
                    }
                    if (mRfidToReadingOffset != 0 && invalidSequence) {
                        byte[] unhandledBytes = new byte[mRfidToReadingOffset];
                        System.arraycopy(mRfidToReading, 0, unhandledBytes, 0, unhandledBytes.length);
                        if (true) appendToLog("mRx000UplinkHandler(): ERROR invalidSequence with nonzero mRfidToReadingOffset=" + mRfidToReadingOffset + ", throw invalid unused data=" + unhandledBytes.length + ", " + byteArrayToString(unhandledBytes));
                        mRfidToReadingOffset = 0;
                        startIndex = 0;
                        startIndexNew = 0;
                    }
                    System.arraycopy(dataIn, 0, mRfidToReading, mRfidToReadingOffset, dataIn.length);
                    mRfidToReadingOffset += dataIn.length;
                    if (true) {
                        byte[] bufferData = new byte[mRfidToReadingOffset];
                        System.arraycopy(mRfidToReading, 0, bufferData, 0, bufferData.length);
                        appendToLog("mRx000UplinkHandler(): mRfidToReadingOffset= " + mRfidToReadingOffset + ", mRfidToReading= " + byteArrayToString(bufferData));
                    }

                    if (true) appendToLog("mRfidToReadingOffset = " + mRfidToReadingOffset + ", startIndex = " + startIndex);
                    int iPayloadSizeMin = 8;
                    while (mRfidToReadingOffset - startIndex >= iPayloadSizeMin) {
                        {
                            int packageLengthRead = (mRfidToReading[startIndex + 5] & 0xFF) * 256 + (mRfidToReading[startIndex + 4] & 0xFF);
                            int expectedLength = 8 + packageLengthRead * 4;
                            if (mRfidToReading[startIndex + 0] == 0x04)
                                expectedLength = 8 + packageLengthRead;
                            if (DEBUG)
                                appendToLog("loop: mRfidToReading.length=" + mRfidToReading.length + ", 1Byte=" + mRfidToReading[startIndex + 0] + ", mRfidToReadingOffset=" + mRfidToReadingOffset + ", startIndex=" + startIndex + ", expectedLength=" + expectedLength);
                            if (mRfidToReadingOffset - startIndex >= 8) {
                                if (mRfidToReading[startIndex + 0] == (byte) 0x40
                                        && (mRfidToReading[startIndex + 1] == 2 || mRfidToReading[startIndex + 1] == 3 || mRfidToReading[startIndex + 1] == 7)) {   //input as Control Command Response
                                    dataIn = mRfidToReading;
                                    if (true) appendToLog("decoding CONTROL data");
                                    if (mRfidDevice.mRfidToWrite.size() == 0) {
                                        if (DEBUG)
                                            appendToLog("Control Response is received with null mRfidToWrite");
                                    } else if (mRfidDevice.mRfidToWrite.get(0) == null) {
                                        if (DEBUG)
                                            appendToLog("Control Response is received with null mRfidToWrite.get(0)");
                                    } else if (mRfidDevice.mRfidToWrite.get(0).dataValues == null) {
                                        mRfidDevice.mRfidToWrite.remove(0); appendToLog("mmRfidToWrite remove 5");
                                        if (DEBUG)
                                            appendToLog("Control Response is received with null mRfidToWrite.dataValues");
                                    } else if (!(mRfidDevice.mRfidToWrite.get(0).dataValues[0] == dataIn[startIndex + 0] && mRfidDevice.mRfidToWrite.get(0).dataValues[1] == dataIn[startIndex + 1])) {
                                        if (DEBUG)
                                            appendToLog("Control Response is received with Mis-matched mRfidToWrite, " + startIndex + ", " + byteArrayToString(dataIn));
                                    } else {
                                        byte[] dataInCompare = null;
                                        switch (mRfidDevice.mRfidToWrite.get(0).dataValues[1]) {
                                            case 2: //SOFTRESET
                                                dataInCompare = new byte[]{0x40, 0x02, (byte) 0xbf, (byte) 0xfd, (byte) 0xbf, (byte) 0xfd, (byte) 0xbf, (byte) 0xfd};
                                                break;
                                            case 3: //ABORT
                                                dataInCompare = new byte[]{0x40, 0x03, (byte) 0xbf, (byte) 0xfc, (byte) 0xbf, (byte) 0xfc, (byte) 0xbf, (byte) 0xfc};
                                                break;
                                            case 7: //RESETTOBOOTLOADER
                                                dataInCompare = new byte[]{0x40, 0x07, (byte) 0xbf, (byte) 0xf8, (byte) 0xbf, (byte) 0xf8, (byte) 0xbf, (byte) 0xf8};
                                                break;
                                        }
                                        byte[] dataIn8 = new byte[8];
                                        System.arraycopy(dataIn, startIndex, dataIn8, 0, dataIn8.length);
                                        if (!(compareArray(dataInCompare, dataIn8, 8))) {
                                            if (DEBUG)
                                                appendToLog("Control response with invalid data: " + byteArrayToString(dataIn8));
                                        } else {
                                            mRfidDevice.mRfidToWrite.remove(0);mRfidDevice.sendRfidToWriteSent = 0; mRfidDevice.mRfidToWriteRemoved = true; appendToLog("mmRfidToWrite remove 6");
                                            if (DEBUG)
                                                appendToLog("matched control command with mRfidToWrite.size=" + mRfidDevice.mRfidToWrite.size());
                                        }
                                    }
                                    packageFound = true;
                                    packageType = 1;
                                    startIndexNew = startIndex + iPayloadSizeMin;
                                }
                                else if ((mRfidToReading[startIndex + 0] == (byte) 0x00 || mRfidToReading[startIndex + 0] == (byte) 0x70)
                                        && mRfidToReading[startIndex + 1] == 0
                                        && mRfidDevice.mRfidToWrite.size() != 0
                                        && mRfidDevice.mRfidToWrite.get(0).dataValues != null
                                        && mRfidDevice.mRfidToWrite.get(0).dataValues[0] == 0x70
                                        && mRfidDevice.mRfidToWrite.get(0).dataValues[1] == 0
                                ) {   //if input as HOST_REG_RESP
                                    if (DEBUG)
                                        appendToLog("loop: decoding HOST_REG_RESP data with startIndex = " + startIndex + ", mRfidToReading=" + byteArrayToString(mRfidToReading));
                                    dataIn = mRfidToReading;
                                    byte[] dataInPayload = new byte[4];
                                    System.arraycopy(dataIn, startIndex + 4, dataInPayload, 0, dataInPayload.length);
                                    //if (mRfidDevice.mRfidToWrite.size() == 0) {
                                    //    if (true) appendToLog("mRx000UplinkHandler(): HOST_REG_RESP is received with null mRfidToWrite: " + byteArrayToString(dataInPayload));
                                    //} else if (mRfidDevice.mRfidToWrite.get(0).dataValues == null) {
                                    //    if (true) appendToLog("mRx000UplinkHandler(): NULL mRfidToWrite.get(0).dataValues"); //.length = " + mRfidDevice.mRfidToWrite.get(0).dataValues.length);
                                    //} else if (!(mRfidDevice.mRfidToWrite.get(0).dataValues[0] == 0x70 && mRfidDevice.mRfidToWrite.get(0).dataValues[1] == 0)) {
                                    //    if (true) appendToLog("mRx000UplinkHandler(): HOST_REG_RESP is received with invalid mRfidDevice.mRfidToWrite.get(0).dataValues=" + byteArrayToString(mRfidDevice.mRfidToWrite.get(0).dataValues));
                                    //} else
                                    {
                                        int addressToWrite = mRfidDevice.mRfidToWrite.get(0).dataValues[2] + mRfidDevice.mRfidToWrite.get(0).dataValues[3] * 256;
                                        int addressToRead = dataIn[startIndex + 2] + dataIn[startIndex + 3] * 256;
                                        if (addressToRead != addressToWrite) {
                                            if (true)
                                                appendToLog("mRx000UplinkHandler(): HOST_REG_RESP is received with misMatch address: addressToRead=" + addressToRead + ", " + startIndex + ", " + byteArrayToString(dataInPayload) + ", addressToWrite=" + addressToWrite);
                                        } else {
                                            switch (addressToRead) {
                                                case 0:
                                                    int patchVersion = dataIn[startIndex + 4] + (dataIn[startIndex + 5] & 0x0F) * 256;
                                                    int minorVersion = (dataIn[startIndex + 5] >> 4) + dataIn[startIndex + 6] * 256;
                                                    int majorVersion = dataIn[startIndex + 7];
                                                    mRx000Setting.macVer = String.valueOf(majorVersion) + "." + String.valueOf(minorVersion) + "." + String.valueOf(patchVersion);
                                                    if (DEBUG)
                                                        appendToLog("found MacVer =" + mRx000Setting.macVer);
                                                    break;
                                                case 9:
                                                    mRx000Setting.mac_last_command_duration = (dataIn[startIndex + 4] & 0xFF)
                                                            + (dataIn[startIndex + 5] & 0xFF) * 256
                                                            + (dataIn[startIndex + 6] & 0xFF) * 256 * 256
                                                            + (dataIn[startIndex + 7] & 0xFF) * 256 * 256 * 256;
                                                    if (DEBUG)
                                                        appendToLog("found mac_last_command_duration =" + mRx000Setting.mac_last_command_duration);
                                                    break;
                                                case 0x0201:
                                                    mRx000Setting.diagnosticCfg = (dataIn[startIndex + 4] & 0x0FF) + ((dataIn[startIndex + 5] & 0x03) * 256);
                                                    if (true)
                                                        appendToLog("found diagnostic configuration: " + byteArrayToString(dataInPayload) + ", diagnosticCfg=" + mRx000Setting.diagnosticCfg);
                                                    break;
                                                case 0x0203:
                                                    mRx000Setting.impinjExtensionValue = (dataIn[startIndex + 4] & 0x03F);
                                                    break;
                                                case 0x204:
                                                    mRx000Setting.pwrMgmtStatus = (dataIn[startIndex + 4] & 0x07);
                                                    appendToLog("pwrMgmtStatus = " + mRx000Setting.pwrMgmtStatus);
                                                    break;
                                                case 0x0700:
                                                    mRx000Setting.antennaCycle = (dataIn[startIndex + 4] & 0xFF) + (dataIn[startIndex + 5] & 0xFF) * 256;
                                                    mRx000Setting.antennaFreqAgile = 0;
                                                    if ((dataIn[startIndex + 7] & 0x01) != 0)
                                                        mRx000Setting.antennaFreqAgile = 1;
                                                    if (DEBUG)
                                                        appendToLog("found antenna cycle: " + byteArrayToString(dataInPayload) + ", cycle=" + mRx000Setting.antennaCycle + ", frequencyAgile=" + mRx000Setting.antennaFreqAgile);
                                                    break;
                                                case 0x0701:
                                                    mRx000Setting.antennaSelect = (dataIn[startIndex + 4] & 0xFF) + (dataIn[startIndex + 5] & 0xFF) * 256 + (dataIn[startIndex + 6] & 0xFF) * 256 * 256 + (dataIn[startIndex + 7] & 0xFF) * 256 * 256 * 256;
                                                    if (DEBUG)
                                                        appendToLog("found antenna select, select=" + mRx000Setting.antennaSelect);
                                                    break;
                                                case 0x0702:
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaEnable = (dataIn[startIndex + 4] & 0x01);
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaInventoryMode = (dataIn[startIndex + 4] & 0x02) >> 1;
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaLocalAlgo = (dataIn[startIndex + 4] & 0x0C) >> 2;
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaLocalStartQ = (dataIn[startIndex + 4] & 0xF0) >> 4;
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaProfileMode = (dataIn[startIndex + 5] & 0x01);
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaLocalProfile = ((dataIn[startIndex + 5] & 0x1E) >> 1);
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaFrequencyMode = ((dataIn[startIndex + 5] & 0x20) >> 5);
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaLocalFrequency = (dataIn[startIndex + 5] & 0x0F) * 4 + ((dataIn[startIndex + 5] & 0xC0) >> 6);
                                                    if (DEBUG)
                                                        appendToLog("found antenna selectEnable: " + byteArrayToString(dataInPayload)
                                                                + ", selectEnable=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaEnable
                                                                + ", inventoryMode=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaInventoryMode
                                                                + ", localAlgo=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaLocalAlgo
                                                                + ", localStartQ=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaLocalStartQ
                                                                + ", profileMode=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaProfileMode
                                                                + ", localProfile=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaLocalProfile
                                                                + ", frequencyMode=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaFrequencyMode
                                                                + ", localFrequency=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaLocalFrequency
                                                        );
                                                    break;
                                                case 0x0703:
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaStatus = (dataIn[startIndex + 4] & 0xFF) + (dataIn[startIndex + 5] & 0xFF) * 256 + (dataIn[startIndex + 6] & 0x0F) * 256 * 256;
                                                    if (DEBUG)
                                                        appendToLog("found antenna status: " + byteArrayToString(dataInPayload) + ", status=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaStatus);
                                                    break;
                                                case 0x0704:
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaDefine = (dataIn[startIndex + 4] & 0x3);
                                                    //      mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaRxDefine = (dataIn[startIndex + 6] & 0x3);
                                                    if (DEBUG)
                                                        appendToLog("found antenna define: " + byteArrayToString(dataInPayload)
                                                                        + ", define=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaDefine
                                                                //        + ", RxDefine=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaRxDefine
                                                        );
                                                    break;
                                                case 0x0705:
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaDwell = (dataIn[startIndex + 4] & 0xFF) + (dataIn[startIndex + 5] & 0xFF) * 256 + (dataIn[startIndex + 6] & 0xFF) * 256 * 256 + (dataIn[startIndex + 7] & 0xFF) * 256 * 256 * 256;
                                                    if (DEBUG)
                                                        appendToLog("found antenna dwell=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaDwell);
                                                    break;
                                                case 0x0706:
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaPower = (dataIn[startIndex + 4] & 0xFF) + (dataIn[startIndex + 5] & 0xFF) * 256 + (dataIn[startIndex + 6] & 0xFF) * 256 * 256 + (dataIn[startIndex + 7] & 0xFF) * 256 * 256 * 256;
                                                    if (DEBUG)
                                                        appendToLog("found antenna power=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaPower);
                                                    break;
                                                case 0x0707:
                                                    mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaInvCount = (dataIn[startIndex + 4] & 0xFF) + (dataIn[startIndex + 5] & 0xFF) * 256 + (dataIn[startIndex + 6] & 0xFF) * 256 * 256 + (dataIn[startIndex + 7] & 0xFF) * 256 * 256 * 256;
                                                    if (DEBUG)
                                                        appendToLog("found antenna InvCount=" + mRx000Setting.antennaSelectedData[mRx000Setting.antennaSelect].antennaInvCount);
                                                    break;
                                                case 0x0800:
                                                    mRx000Setting.invSelectIndex = (dataIn[startIndex + 4] & 0xFF) + (dataIn[startIndex + 5] & 0xFF) * 256 + (dataIn[startIndex + 6] & 0xFF) * 256 * 256 + (dataIn[startIndex + 7] & 0xFF) * 256 * 256 * 256;
                                                    if (DEBUG)
                                                        appendToLog("found inventory select: " + byteArrayToString(dataInPayload) + ", select=" + mRx000Setting.invSelectIndex);
                                                    break;
                                                case 0x0801: {
                                                    int dataIndex = mRx000Setting.invSelectIndex;
                                                    if (dataIndex < mRx000Setting.INVSELECT_MIN || dataIndex > mRx000Setting.INVSELECT_MAX) {
                                                        if (DEBUG)
                                                            appendToLog("found inventory select configuration: " + byteArrayToString(dataInPayload) + ", but invalid index=" + dataIndex);
                                                    } else {
                                                        mRx000Setting.invSelectData[dataIndex].selectEnable = (dataIn[startIndex + 4] & 0x01);
                                                        mRx000Setting.invSelectData[dataIndex].selectTarget = ((dataIn[startIndex + 4] & 0x0E) >> 1);
                                                        mRx000Setting.invSelectData[dataIndex].selectAction = ((dataIn[startIndex + 4] & 0x70) >> 4);
                                                        mRx000Setting.invSelectData[dataIndex].selectDelay = dataIn[startIndex + 5];
                                                        if (DEBUG)
                                                            appendToLog("found inventory select configuration: " + byteArrayToString(dataInPayload)
                                                                    + ", selectEnable=" + mRx000Setting.invSelectData[dataIndex].selectEnable
                                                                    + ", selectTarget=" + mRx000Setting.invSelectData[dataIndex].selectTarget
                                                                    + ", selectAction=" + mRx000Setting.invSelectData[dataIndex].selectAction
                                                                    + ", selectDelay=" + mRx000Setting.invSelectData[dataIndex].selectDelay
                                                            );
                                                    }
                                                    break;
                                                }
                                                case 0x0802: {
                                                    int dataIndex = mRx000Setting.invSelectIndex;
                                                    if (dataIndex < mRx000Setting.INVSELECT_MIN || dataIndex > mRx000Setting.INVSELECT_MAX) {
                                                        if (DEBUG)
                                                            appendToLog("found inventory select mask bank: " + byteArrayToString(dataInPayload) + ", but invalid index=" + dataIndex);
                                                    } else {
                                                        mRx000Setting.invSelectData[dataIndex].selectMaskBank = (dataIn[startIndex + 4] & 0x03);
                                                        if (DEBUG)
                                                            appendToLog("found inventory select mask bank: " + byteArrayToString(dataInPayload)
                                                                    + ", selectMaskBank=" + mRx000Setting.invSelectData[dataIndex].selectMaskBank
                                                            );
                                                    }
                                                    break;
                                                }
                                                case 0x0803: {
                                                    int dataIndex = mRx000Setting.invSelectIndex;
                                                    if (dataIndex < mRx000Setting.INVSELECT_MIN || dataIndex > mRx000Setting.INVSELECT_MAX) {
                                                        if (DEBUG)
                                                            appendToLog("found inventory select mask offset: " + byteArrayToString(dataInPayload) + ", but invalid index=" + dataIndex);
                                                    } else {
                                                        mRx000Setting.invSelectData[dataIndex].selectMaskOffset = (dataIn[startIndex + 4] & 0x0FF) + (dataIn[startIndex + 5] & 0x0FF) * 256 + (dataIn[startIndex + 6] & 0x0FF) * 256 * 256 + (dataIn[startIndex + 7] & 0x0FF) * 256 * 256 * 256;
                                                        if (DEBUG)
                                                            appendToLog("found inventory select mask offset: " + byteArrayToString(dataInPayload)
                                                                    + ", selectMaskOffset=" + mRx000Setting.invSelectData[dataIndex].selectMaskOffset
                                                            );
                                                    }
                                                    break;
                                                }
                                                case 0x0804: {
                                                    int dataIndex = mRx000Setting.invSelectIndex;
                                                    if (dataIndex < mRx000Setting.INVSELECT_MIN || dataIndex > mRx000Setting.INVSELECT_MAX) {
                                                        if (DEBUG)
                                                            appendToLog("found inventory select mask length: " + byteArrayToString(dataInPayload) + ", but invalid index=" + dataIndex);
                                                    } else {
                                                        mRx000Setting.invSelectData[dataIndex].selectMaskLength = (dataIn[startIndex + 4] & 0x0FF);
                                                        appendToLog("getSelectMaskData with read selectMaskLength = " + mRx000Setting.invSelectData[dataIndex].selectMaskLength);
                                                        if (DEBUG)
                                                            appendToLog("found inventory select mask length: " + byteArrayToString(dataInPayload)
                                                                    + ", selectMaskLength=" + mRx000Setting.invSelectData[dataIndex].selectMaskLength
                                                            );
                                                    }
                                                    break;
                                                }
                                                case 0x0805:
                                                case 0x0806:
                                                case 0x0807:
                                                case 0x0808:
                                                case 0x0809:
                                                case 0x080A:
                                                case 0x080B:
                                                case 0x080C: {
                                                    int dataIndex = mRx000Setting.invSelectIndex;
                                                    if (dataIndex < mRx000Setting.INVSELECT_MIN || dataIndex > mRx000Setting.INVSELECT_MAX) {
                                                        if (DEBUG)
                                                            appendToLog("found inventory select mask 0-3: " + byteArrayToString(dataInPayload) + ", but invalid index=" + dataIndex);
                                                    } else {
                                                        int maskDataIndex = addressToRead - 0x0805;
                                                        if (DEBUG)
                                                            appendToLog("Old selectMaskData0_31 = " + byteArrayToString(mRx000Setting.invSelectData[dataIndex].selectMaskData0_31));
                                                        System.arraycopy(dataIn, startIndex + 4, mRx000Setting.invSelectData[dataIndex].selectMaskData0_31, maskDataIndex * 4, 4);
                                                        if (DEBUG)
                                                            appendToLog("Old selectMaskData0_31 = " + byteArrayToString(mRx000Setting.invSelectData[dataIndex].selectMaskData0_31));
                                                        mRx000Setting.invSelectData[dataIndex].selectMaskDataReady |= (0x01 << maskDataIndex);
                                                        if (DEBUG)
                                                            appendToLog("found inventory select mask 0-3: " + byteArrayToString(dataInPayload));
                                                    }
                                                    break;
                                                }
                                                case 0x0900:
                                                    if (mRx000Setting.queryTarget != 2)
                                                        mRx000Setting.queryTarget = (dataIn[startIndex + 4] >> 4) & 0x01;
                                                    mRx000Setting.querySession = (dataIn[startIndex + 4] >> 5) & 0x03;
                                                    mRx000Setting.querySelect = (dataIn[startIndex + 4] >> 7) & 0x01 + ((dataIn[startIndex + 5] & 0x01) * 2);
                                                    if (DEBUG)
                                                        appendToLog("found query configuration: " + byteArrayToString(dataInPayload) + ", target=" + mRx000Setting.queryTarget + ", session=" + mRx000Setting.querySession + ", select=" + mRx000Setting.querySelect);
                                                    break;
                                                case 0x0901:
                                                    mRx000Setting.invAlgo = dataIn[startIndex + 4] & 0x3F;
                                                    mRx000Setting.matchRep = ((dataIn[startIndex + 4] & 0xC0) >> 6) + (dataIn[startIndex + 5] & 0x3F) * 4;
                                                    mRx000Setting.tagSelect = ((dataIn[startIndex + 5] & 0x40) >> 6);
                                                    mRx000Setting.noInventory = ((dataIn[startIndex + 5] & 0x80) >> 7);
                                                    mRx000Setting.tagRead = dataIn[startIndex + 6] & 0x03;
                                                    mRx000Setting.tagDelay = ((dataIn[startIndex + 7] & 0x03) * 16 + ((dataIn[startIndex + 6] & 0xF0) >> 4));
                                                    mRx000Setting.invModeCompact = (dataIn[startIndex + 7] & 0x04);
                                                    if (DEBUG)
                                                        appendToLog("found inventory configuration: " + byteArrayToString(dataInPayload) + ", algorithm=" + mRx000Setting.invAlgo + ", matchRep=" + mRx000Setting.matchRep + ", tagSelect=" + mRx000Setting.tagSelect + ", noInventory=" + mRx000Setting.noInventory + ", tagRead=" + mRx000Setting.tagRead + ", tagDelay=" + mRx000Setting.tagDelay);
                                                    break;
                                                case 0x0902:
                                                    if (dataIn[startIndex + 6] != 0 || dataIn[startIndex + 7] != 0) {
                                                        if (DEBUG)
                                                            appendToLog("found inventory select, but too big: " + byteArrayToString(dataInPayload));
                                                    } else {
                                                        mRx000Setting.algoSelect = (dataIn[startIndex + 4] & 0xFF) + (dataIn[startIndex + 5] & 0xFF) * 256;
                                                        if (DEBUG)
                                                            appendToLog("found inventory algorithm select=" + mRx000Setting.algoSelect);
                                                    }
                                                    break;
                                                case 0x0903: {
                                                    int dataIndex = mRx000Setting.algoSelect;
                                                    if (dataIndex < mRx000Setting.ALGOSELECT_MIN || dataIndex > mRx000Setting.ALGOSELECT_MAX) {
                                                        if (DEBUG)
                                                            appendToLog("found inventory algo parameter 0: " + byteArrayToString(dataInPayload) + ", but invalid index=" + dataIndex);
                                                    } else {
                                                        mRx000Setting.algoSelectedData[dataIndex].algoStartQ = (dataIn[startIndex + 4] & 0x0F);
                                                        mRx000Setting.algoSelectedData[dataIndex].algoMaxQ = ((dataIn[startIndex + 4] & 0xF0) >> 4);
                                                        mRx000Setting.algoSelectedData[dataIndex].algoMinQ = (dataIn[startIndex + 5] & 0x0F);
                                                        mRx000Setting.algoSelectedData[dataIndex].algoMaxRep = ((dataIn[startIndex + 5] & 0xF0) >> 4) + ((dataIn[startIndex + 6] & 0x0F) << 4);
                                                        mRx000Setting.algoSelectedData[dataIndex].algoHighThres = ((dataIn[startIndex + 6] & 0xF0) >> 4);
                                                        mRx000Setting.algoSelectedData[dataIndex].algoLowThres = (dataIn[startIndex + 7] & 0x0F);
                                                        if (DEBUG)
                                                            appendToLog("found inventory algo parameter 0: " + byteArrayToString(dataInPayload)
                                                                    + ", algoStartQ=" + mRx000Setting.algoSelectedData[dataIndex].algoStartQ
                                                                    + ", algoMaxQ=" + mRx000Setting.algoSelectedData[dataIndex].algoMaxQ
                                                                    + ", algoMinQ=" + mRx000Setting.algoSelectedData[dataIndex].algoMinQ
                                                                    + ", algoMaxRep=" + mRx000Setting.algoSelectedData[dataIndex].algoMaxRep
                                                                    + ", algoHighThres=" + mRx000Setting.algoSelectedData[dataIndex].algoHighThres
                                                                    + ", algoLowThres=" + mRx000Setting.algoSelectedData[dataIndex].algoLowThres
                                                            );
                                                    }
                                                    break;
                                                }
                                                case 0x0904: {
                                                    int dataIndex = mRx000Setting.algoSelect;
                                                    if (dataIndex < mRx000Setting.ALGOSELECT_MIN || dataIndex > mRx000Setting.ALGOSELECT_MAX) {
                                                        if (DEBUG)
                                                            appendToLog("found inventory algo parameter 1: " + byteArrayToString(dataInPayload) + ", but invalid index=" + dataIndex);
                                                    } else {
                                                        mRx000Setting.algoSelectedData[dataIndex].algoRetry = dataIn[startIndex + 4] & 0x0FF;
                                                        if (DEBUG)
                                                            appendToLog("found inventory algo parameter 1: " + byteArrayToString(dataInPayload) + ", algoRetry=" + mRx000Setting.algoSelectedData[dataIndex].algoRetry);
                                                    }
                                                    break;
                                                }
                                                case 0x0905: {
                                                    int dataIndex = mRx000Setting.algoSelect;
                                                    if (dataIndex < mRx000Setting.ALGOSELECT_MIN || dataIndex > mRx000Setting.ALGOSELECT_MAX) {
                                                        if (DEBUG)
                                                            appendToLog("found inventory algo parameter 2: " + byteArrayToString(dataInPayload) + ", but invalid index=" + dataIndex);
                                                    } else {
                                                        if (DEBUG)
                                                            appendToLog("found inventory algo parameter 2: " + byteArrayToString(dataInPayload) + ", dataIndex=" + dataIndex + ", algoAbFlip=" + mRx000Setting.algoSelectedData[dataIndex].algoAbFlip + ", algoRunTilZero=" + mRx000Setting.algoSelectedData[dataIndex].algoRunTilZero);
                                                        mRx000Setting.algoSelectedData[dataIndex].algoAbFlip = dataIn[startIndex + 4] & 0x01;
                                                        mRx000Setting.algoSelectedData[dataIndex].algoRunTilZero = (dataIn[startIndex + 4] & 0x02) >> 1;
                                                        if (DEBUG)
                                                            appendToLog("found inventory algo parameter 2: " + byteArrayToString(dataInPayload) + ", algoAbFlip=" + mRx000Setting.algoSelectedData[dataIndex].algoAbFlip + ", algoRunTilZero=" + mRx000Setting.algoSelectedData[dataIndex].algoRunTilZero);
                                                    }
                                                    break;
                                                }
                                                case 0x0911:
                                                    mRx000Setting.matchEnable = dataIn[startIndex + 4] & 0x01;
                                                    mRx000Setting.matchType = ((dataIn[startIndex + 4] & 0x02) >> 1);
                                                    mRx000Setting.matchLength = ((dataIn[startIndex + 4] & 0x0FF) >> 2) + (dataIn[startIndex + 5] & 0x07) * 64;
                                                    mRx000Setting.matchOffset = ((dataIn[startIndex + 5] & 0x0FF) >> 3) + (dataIn[startIndex + 6] & 0x1F) * 32;
                                                    if (DEBUG)
                                                        appendToLog("found inventory match configuration: " + byteArrayToString(dataInPayload) + ", selectEnable=" + mRx000Setting.matchEnable + ", matchType=" + mRx000Setting.matchType + ", matchLength=" + mRx000Setting.matchLength + ", matchOffset=" + mRx000Setting.matchOffset);
                                                    break;
                                                case 0x0912:
                                                case 0x0913:
                                                case 0x0914:
                                                case 0x0915:
                                                case 0x0916:
                                                case 0x0917:
                                                case 0x0918:
                                                case 0x0919:
                                                case 0x091A:
                                                case 0x091B:
                                                case 0x091C:
                                                case 0x091D:
                                                case 0x091E:
                                                case 0x091F:
                                                case 0x0920:
                                                case 0x0921: {
                                                    int maskDataIndex = addressToRead - 0x0912;
                                                    System.arraycopy(dataIn, startIndex + 4, mRx000Setting.invMatchData0_63, maskDataIndex * 4, 4);
                                                    mRx000Setting.invMatchDataReady |= (0x01 << maskDataIndex);
                                                    if (DEBUG)
                                                        appendToLog("found inventory match Data 0-3: " + byteArrayToString(dataInPayload));
                                                    break;
                                                }
                                                case 0x0A01:
                                                    mRx000Setting.accessRetry = (dataIn[startIndex + 4] & 0x0E) >> 1;
                                                    if (DEBUG)
                                                        appendToLog("found access algoRetry: " + byteArrayToString(dataInPayload) + ", accessRetry=" + mRx000Setting.accessRetry);
                                                    break;
                                                case 0x0A02:
                                                    mRx000Setting.accessBank = (dataIn[startIndex + 4] & 0x03);
                                                    mRx000Setting.accessBank2 = ((dataIn[startIndex + 4] >> 2) & 0x03);
                                                    if (DEBUG)
                                                        appendToLog("found access bank: " + byteArrayToString(dataInPayload) + ", accessBank=" + mRx000Setting.accessBank + ", accessBank2=" + mRx000Setting.accessBank2);
                                                    break;
                                                case 0x0A03:
                                                    if (mRx000Setting.tagRead != 0) {
                                                        mRx000Setting.accessOffset = (dataIn[startIndex + 4] & 0x0FF) + (dataIn[startIndex + 5] & 0x0FF) * 256;     // + (dataIn[startIndex + 6] & 0x0FF) * 256 * 256 + (dataIn[startIndex + 7] & 0x0FF) * 256 * 256 * 256;
                                                        mRx000Setting.accessOffset2 = (dataIn[startIndex + 6] & 0x0FF) + (dataIn[startIndex + 7] & 0x0FF) * 256;    // + (dataIn[startIndex + 6] & 0x0FF) * 256 * 256 + (dataIn[startIndex + 7] & 0x0FF) * 256 * 256 * 256;
                                                    } else {
                                                        mRx000Setting.accessOffset = (dataIn[startIndex + 4] & 0x0FF) + (dataIn[startIndex + 5] & 0x0FF) * 256 + (dataIn[startIndex + 6] & 0x0FF) * 256 * 256 + (dataIn[startIndex + 7] & 0x0FF) * 256 * 256 * 256;
                                                    }
                                                    if (DEBUG)
                                                        appendToLog("found access offset: " + byteArrayToString(dataInPayload) + ", accessOffset=" + mRx000Setting.accessOffset + ", accessOffset2=" + mRx000Setting.accessOffset2);
                                                    break;
                                                case 0x0A04:
                                                    mRx000Setting.accessCount = (dataIn[startIndex + 4] & 0x0FF);
                                                    mRx000Setting.accessCount2 = (dataIn[startIndex + 5] & 0x0FF);
                                                    if (DEBUG)
                                                        appendToLog("found access count: " + byteArrayToString(dataInPayload) + ", accessCount=" + mRx000Setting.accessCount + ", accessCount2=" + mRx000Setting.accessCount2);
                                                    break;
                                                case 0x0A05:
                                                    mRx000Setting.accessLockAction = (dataIn[startIndex + 4] & 0x0FF) + ((dataIn[startIndex + 5] & 0x03) * 256);
                                                    mRx000Setting.accessLockMask = ((dataIn[startIndex + 5] & 0x0FF) >> 2) + ((dataIn[startIndex + 6] & 0x0F) * 64);
                                                    if (DEBUG)
                                                        appendToLog("found access lock configuration: " + byteArrayToString(dataInPayload) + ", accessLockAction=" + mRx000Setting.accessLockAction + ", accessLockMask=" + mRx000Setting.accessLockMask);
                                                    break;
                                                case 0x0A08:
                                                    mRx000Setting.accessWriteDataSelect = (dataIn[startIndex + 4] & 0x07);
                                                    if (DEBUG)
                                                        appendToLog("found write data select: " + byteArrayToString(dataInPayload) + ", accessWriteDataSelect=" + mRx000Setting.accessWriteDataSelect);
                                                    break;
                                                case 0x0A09:
                                                case 0x0A0A:
                                                case 0x0A0B:
                                                case 0x0A0C:
                                                case 0x0A0D:
                                                case 0x0A0E:
                                                case 0x0A0F:
                                                case 0x0A10:
                                                case 0x0A11:
                                                case 0x0A12:
                                                case 0x0A13:
                                                case 0x0A14:
                                                case 0x0A15:
                                                case 0x0A16:
                                                case 0x0A17:
                                                case 0x0A18: {
                                                    int maskDataIndex = addressToRead - 0x0A09;
                                                    int maskDataIndexH = 0;
                                                    if (mRx000Setting.accessWriteDataSelect != 0)
                                                        maskDataIndexH = 16;
                                                    for (int k = 0; k < 4; k++) {
                                                        mRx000Setting.accWriteData0_63[(maskDataIndexH + maskDataIndex) * 4 + k] = dataIn[startIndex + 7 - k];
                                                    }
                                                    mRx000Setting.accWriteDataReady |= (0x01 << (maskDataIndexH + maskDataIndex));
                                                    if (DEBUG)
                                                        appendToLog("accessWriteData=" + mRx000Setting.accWriteData0_63);
                                                    if (DEBUG)
                                                        appendToLog("found access write data 0-3: " + byteArrayToString(dataInPayload));
                                                    break;
                                                }
                                                case 0x0b60:
                                                    mRx000Setting.currentProfile = dataIn[startIndex + 4];
                                                    if (DEBUG)
                                                        appendToLog("found current profile: " + byteArrayToString(dataInPayload) + ", profile=" + mRx000Setting.currentProfile);
                                                    break;
                                                case 0x0c01:
                                                    mRx000Setting.freqChannelSelect = dataIn[startIndex + 4];
                                                    if (true)
                                                        appendToLog("setFreqChannelSelect: found frequency channel select: " + byteArrayToString(dataInPayload) + ", freqChannelSelect=" + mRx000Setting.freqChannelSelect);
                                                    break;
                                                case 0x0c02:
                                                    mRx000Setting.freqChannelConfig = dataIn[startIndex + 4] & 0x01;
                                                    if (DEBUG)
                                                        appendToLog("found frequency channel configuration: " + byteArrayToString(dataInPayload) + ", channelConfig=" + mRx000Setting.freqChannelConfig);
                                                    break;
                                                case 0x0f00:
                                                    mRx000Setting.authenticateSendReply = ((dataIn[startIndex + 4] & 1) != 0) ? true : false;
                                                    mRx000Setting.authenticateIncReplyLength = ((dataIn[startIndex + 4] & 2) != 0) ? true : false;
                                                    mRx000Setting.authenticateLength = ((dataIn[startIndex + 5] & 0xFC) >> 3) + (dataIn[startIndex + 6] & 0x3F);
                                                    if (DEBUG)
                                                        appendToLog("found authenticate configuration: " + byteArrayToString(dataInPayload));
                                                    break;
                                                case 0x0f01:
                                                case 0x0f02:
                                                case 0x0f03:
                                                case 0x0f04: {
                                                    int maskDataIndex = addressToRead - 0x0f01;
                                                    System.arraycopy(dataIn, startIndex + 4, mRx000Setting.authMatchData0_63, maskDataIndex * 4, 4);
                                                    //mRx000Setting.authMatchDataReady |= (0x01 << maskDataIndex);
                                                    if (DEBUG)
                                                        appendToLog("found authenticate match Data 0-3: " + byteArrayToString(dataInPayload));
                                                    break;
                                                }
                                                case 0x0f05:
                                                    mRx000Setting.untraceableRange = dataIn[startIndex + 4] & 0x03;
                                                    mRx000Setting.untraceableUser = ((dataIn[startIndex + 4] & 0x04) != 0) ? true : false;
                                                    mRx000Setting.untraceableTid = ((dataIn[startIndex + 4] & 0x18) >> 3);
                                                    mRx000Setting.untraceableEpcLength = ((dataIn[startIndex + 4] & 0xE0) >> 5) + ((dataIn[startIndex + 5] & 0x3) << 3);
                                                    mRx000Setting.untraceableEpc = ((dataIn[startIndex + 5] & 4) != 0) ? true : false;
                                                    mRx000Setting.untraceableUXpc = ((dataIn[startIndex + 5] & 8) != 0) ? true : false;
                                                    if (DEBUG)
                                                        appendToLog("found untraceable configuration: " + byteArrayToString(dataInPayload));
                                                    break;
                                                default:
                                                    if (true)
                                                        appendToLog("found OTHERS with addressToWrite=" + addressToWrite + ", addressToRead=" + addressToRead + ", " + byteArrayToString(dataInPayload));
                                                    break;
                                            }
                                            mRfidDevice.mRfidToWrite.remove(0); mRfidDevice.sendRfidToWriteSent = 0; mRfidDevice.mRfidToWriteRemoved = true; appendToLog("mmRfidToWrite remove 7");
                                        }
                                    }
                                    packageFound = true;
                                    packageType = 2;
                                    startIndexNew = startIndex + 8;
                                } else if ((mRfidToReading[startIndex + 0] >= 1 && mRfidToReading[startIndex + 0] <= 4) //02 for begin and end, 03 for inventory, 01 for access
                                        && (expectedLength >= 0 && expectedLength < mRfidToReading.length)
                                        && (mRfidToReading[startIndex + 2] == 0 || mRfidToReading[startIndex + 2] == 1 || (mRfidToReading[startIndex + 2] >= 5 && mRfidToReading[startIndex + 2] <= 14))
                                        && (mRfidToReading[startIndex + 3] == 0 || mRfidToReading[startIndex + 3] == 0x30 || mRfidToReading[startIndex + 3] == (byte) 0x80)
//                                    && mRfidToReading[startIndex + 6] == 0    //for packageTypeRead = 0x3007, this byte is 0x20. Others are 0
                                        && mRfidToReading[startIndex + 7] == 0) {  //if input as command response
                                    {
                                        if (DEBUG) appendToLog("loop: decoding 1_4 data");
                                        if (mRfidToReadingOffset - startIndex < expectedLength)
                                            break;
                                        dataIn = mRfidToReading;
                                        byte[] dataInPayload = new byte[expectedLength - 4];
                                        System.arraycopy(dataIn, startIndex + 4, dataInPayload, 0, dataInPayload.length);
                                        //if ((dataIn[startIndex + 3] == (byte) 0x80 && dataIn[startIndex + 6] == 0 && dataIn[startIndex + 7] == 0) == false) {
                                        //    appendToLog("mRx000UplinkHandler(): invalid command response is received with incorrect byte3= " + dataIn[startIndex + 3] + ", byte6=" + dataIn[startIndex + 6] + ", byte7=" + dataIn[startIndex + 7]);
                                        //}

                                        int packageTypeRead = dataIn[startIndex + 2] + (dataIn[startIndex + 3] & 0xFF) * 256;
//                                    appendToLog("AAA packageTypeRead=" + Integer.toString(packageTypeRead, 16) + ", startIndex=" + startIndex + ", dataIn=" + byteArrayToString(dataIn));
                                        Rx000pkgData dataA = new Rx000pkgData();
                                        if (packageTypeRead == 6 && (dataIn[startIndex + 1] & 0x02) != 0 && dataIn[startIndex + 13] == 0) {
                                            dataIn[startIndex + 13] = (byte) 0xFF;
                                        }
                                        int padCount = ((dataIn[startIndex + 1] & 0x0FF) >> 6);
                                        if (packageTypeRead == 6) {
                                            dataA.dataValues = new byte[8 + packageLengthRead * 4 - padCount];
                                            System.arraycopy(dataIn, startIndex, dataA.dataValues, 0, dataA.dataValues.length);
                                        } else if (packageTypeRead == 0x8005 || packageTypeRead == 5) {
                                            if (dataIn[startIndex + 0] == 0x04) {
                                                dataA.dataValues = new byte[packageLengthRead];
                                                dataA.decodedPort = dataIn[startIndex + 6];
                                            } else
                                                dataA.dataValues = new byte[packageLengthRead * 4 - padCount];
                                            System.arraycopy(dataIn, startIndex + 8, dataA.dataValues, 0, dataA.dataValues.length);
                                        } else {
                                            dataA.dataValues = new byte[packageLengthRead * 4];
                                            System.arraycopy(dataIn, startIndex + 8, dataA.dataValues, 0, dataA.dataValues.length);
                                        }
                                        dataA.flags = (dataIn[startIndex + 1] & 0xFF);
                                        switch (packageTypeRead) {
                                            case 0x0000:
                                            case 0x8000: //RFID_PACKET_TYPE_COMMAND_BEGIN  //original 0
                                                if (dataIn[startIndex + 0] != 1 && dataIn[startIndex + 0] != 2) {
                                                    if (DEBUG)
                                                        appendToLog("command COMMAND_BEGIN is found without first byte as 0x01 or 0x02, " + byteArrayToString(dataInPayload));
                                                } else if (mRfidDevice.mRfidToWrite.size() == 0) {
                                                    if (DEBUG)
                                                        appendToLog("command COMMAND_BEGIN is found without mRfidToWrite");
                                                } else {
                                                    byte[] dataWritten = mRfidDevice.mRfidToWrite.get(0).dataValues;
                                                    if (dataWritten == null) {
                                                    } else if (!(dataWritten[0] == (byte) 0x70 && dataWritten[1] == 1 && dataWritten[2] == 0 && dataWritten[3] == (byte) 0xF0)) {
                                                        if (true)
                                                            appendToLog("command COMMAND_BEGIN is found with invalid mRfidToWrite: " + byteArrayToString(dataWritten));
                                                    } else {
                                                        boolean matched = true;
                                                        for (int i = 0; i < 4; i++) {
                                                            if (dataWritten[4 + i] != dataIn[startIndex + 8 + i]) {
                                                                matched = false;
                                                                break;
                                                            }
                                                        }
                                                        long lValue = 0;
                                                        int multipler = 1;
                                                        for (int i = 0; i < 4; i++) {
                                                            lValue += (dataIn[startIndex + 12 + i] & 0xFF) * multipler;
                                                            multipler *= 256;
                                                        }
                                                        if (matched == false) {
                                                            if (true)
                                                                appendToLog("command COMMAND_BEGIN is found with mis-matched command:" + byteArrayToString(dataWritten));
                                                        } else {
                                                            mRfidDevice.mRfidToWrite.remove(0); mRfidDevice.sendRfidToWriteSent = 0; mRfidDevice.mRfidToWriteRemoved = true; appendToLog("mmRfidToWrite remove 8");
                                                            mRfidDevice.setInventoring(true);
                                                            Date date = new Date();
                                                            long date_time = date.getTime();
                                                            long expected_firmware_ontime_ms = firmware_ontime_ms;
                                                            if (date_time_ms != 0) {
                                                                long firmware_ontime_ms_difference = date_time - date_time_ms;
                                                                if (firmware_ontime_ms_difference > 2000) {
                                                                    expected_firmware_ontime_ms += (firmware_ontime_ms_difference - 2000);
                                                                }
                                                            }
                                                            if (lValue < expected_firmware_ontime_ms) {
                                                                bFirmware_reset_before = true;
                                                                appendToLogView("command COMMAND_BEGIN --- Firmware reset before !!!");
                                                            }
                                                            firmware_ontime_ms = lValue;
                                                            date_time_ms = date_time;
                                                            if (true)
                                                                appendToLog("command COMMAND_BEGIN is found with packageLength=" + packageLengthRead + ", with firmware count=" + lValue + ", date_time=" + date_time + ", expected firmware count=" + expected_firmware_ontime_ms);
                                                            //if (mUsbConnector != null) mUsbConnector.inventorRunning = true;
                                                        }
                                                    }
                                                }
                                                break;
                                            case 0x0001:
                                            case 0x8001:    //RFID_PACKET_TYPE_COMMAND_END  //original 1
                                                if (dataIn[startIndex + 0] != 1 && dataIn[startIndex + 0] != 2) {
                                                    if (DEBUG)
                                                        appendToLog("command COMMAND_END is found without first byte as 0x01 or 0x02, " + byteArrayToString(dataInPayload));
                                                    break;
                                                } else {
                                                    dataA.responseType = HostCmdResponseTypes.TYPE_COMMAND_END;
                                                    mRfidDevice.setInventoring(false);
                                                    if (true)
                                                        appendToLog("command COMMAND_END is found with packageLength=" + packageLengthRead + ", length = " + dataA.dataValues.length + ", dataValues=" + byteArrayToString(dataA.dataValues));
                                                    if (dataA.dataValues.length >= 8) {
                                                        int status = dataA.dataValues[12 - 8] + dataA.dataValues[13 - 8] * 256;
                                                        if (status != 0)
                                                            dataA.decodedError = "Received COMMAND_END with status=" + String.format("0x%X", status) + ", error_port=" + dataA.dataValues[14 - 8];
                                                        if (dataA.decodedError != null)
                                                            appendToLog(dataA.decodedError);
                                                    }
                                                }
                                                int oldSize = mRx000ToRead.size(); mRx000ToRead.add(dataA);  appendToLog("oldSize = " + oldSize + ", after adding 8001 mRx000ToRead.size = " + mRx000ToRead.size());
                                                commandOperating = false;
                                                break;
                                            case 0x0005:
                                            case 0x8005:    //RFID_PACKET_TYPE_18K6C_INVENTORY  //original 5
                                                if (dataIn[startIndex + 0] != 3 && dataIn[startIndex + 0] != 4) {
                                                    if (DEBUG)
                                                        appendToLog("command 18K6C_INVENTORY is found without first byte as 0x03, 0x04, " + byteArrayToString(dataInPayload));
                                                    break;
                                                } else {
                                                    if (dataIn[startIndex + 0] == 3) {
                                                        dataA.responseType = HostCmdResponseTypes.TYPE_18K6C_INVENTORY;
                                                        if (true) {
                                                            boolean crcError;
                                                            if (dataA.dataValues.length < 12 + 4)
                                                                dataA.decodedError = "Received TYPE_18K6C_INVENTORY with length = " + String.valueOf(dataA.dataValues.length) + ", data = " + byteArrayToString(dataA.dataValues);
                                                            else {
                                                                int epcLength = (dataA.dataValues[12] >> 3) * 2;
                                                                if (dataA.dataValues.length < 12 + 2 + epcLength + 2)
                                                                    dataA.decodedError = "Received TYPE_18K6C_INVENTORY with length = " + String.valueOf(dataA.dataValues.length) + ", data = " + byteArrayToString(dataA.dataValues);
                                                                else {
                                                                    long time1 = dataA.dataValues[3] & 0x00FF;
                                                                    time1 = time1 << 8;
                                                                    time1 |= dataA.dataValues[2] & 0x00FF;
                                                                    time1 = time1 = time1 << 8;
                                                                    time1 |= dataA.dataValues[1] & 0x00FF;
                                                                    time1 = time1 = time1 << 8;
                                                                    time1 |= dataA.dataValues[0] & 0x00FF;
                                                                    dataA.decodedTime = time1;

                                                                    if (true)
                                                                        dataA.decodedRssi = decodeWideBandRSSI(dataA.dataValues[13 - 8]);
                                                                    else {
                                                                        byte nbRssi = dataA.dataValues[13 - 8];
                                                                        byte mantissa = nbRssi;
                                                                        mantissa &= 0x07;
                                                                        byte exponent = nbRssi;
                                                                        exponent >>= 3;
                                                                        dataA.decodedRssi = 20 * log10(pow(2, exponent) * (1 + (mantissa / pow(2, 3))));
                                                                    }

                                                                    byte bValue = dataA.dataValues[14 - 8];
                                                                    bValue &= 0x7F;
                                                                    if ((bValue & 0x40) != 0)
                                                                        bValue |= 0x80;
                                                                    dataA.decodedPhase = bValue;
                                                                    if (true) {
                                                                        int iValue = dataA.dataValues[14 - 8] & 0x3F; //0x7F;
                                                                        boolean b7 = false;
                                                                        if ((dataA.dataValues[14 - 8] & 0x80) != 0)
                                                                            b7 = true;
                                                                        iValue *= 90;
                                                                        iValue /= 32;
                                                                        dataA.decodedPhase = iValue;
                                                                    }

                                                                    dataA.decodedChidx = dataA.dataValues[15 - 8];
                                                                    dataA.decodedPort = dataA.dataValues[18 - 8];
                                                                    int data1_count = (dataA.dataValues[16 - 8] & 0xFF);
                                                                    data1_count *= 2;
                                                                    int data2_count = (dataA.dataValues[17 - 8] & 0xFF);
                                                                    data2_count *= 2;

                                                                    if (dataA.dataValues.length >= 12 + 2) {
                                                                        dataA.decodedPc = new byte[2];
                                                                        System.arraycopy(dataA.dataValues, 12, dataA.decodedPc, 0, dataA.decodedPc.length);
                                                                    }
                                                                    if (dataA.dataValues.length >= 12 + 2 + 2) {
                                                                        dataA.decodedEpc = new byte[dataA.dataValues.length - 12 - 4];
                                                                        System.arraycopy(dataA.dataValues, 12 + 2, dataA.decodedEpc, 0, dataA.decodedEpc.length);
                                                                        dataA.decodedCrc = new byte[2];
                                                                        System.arraycopy(dataA.dataValues, dataA.dataValues.length - 2, dataA.decodedCrc, 0, dataA.decodedCrc.length);
                                                                    }
                                                                    if (data1_count != 0 && dataA.dataValues.length - 2 - data1_count - data2_count >= 0) {
                                                                        dataA.decodedData1 = new byte[data1_count];
                                                                        System.arraycopy(dataA.dataValues, dataA.dataValues.length - 2 - data1_count - data2_count, dataA.decodedData1, 0, dataA.decodedData1.length);
                                                                    }
                                                                    if (data2_count != 0 && dataA.dataValues.length - 2 - data2_count >= 0) {
                                                                        dataA.decodedData2 = new byte[data2_count];
                                                                        System.arraycopy(dataA.dataValues, dataA.dataValues.length - 2 - data2_count, dataA.decodedData2, 0, dataA.decodedData2.length);
                                                                    }
                                                                    if (DEBUG)
                                                                        appendToLog("dataValues = " + byteArrayToString(dataA.dataValues) + ", 1 decodedRssi = " + dataA.decodedRssi + ", decodedPhase = " + dataA.decodedPhase + ", decodedChidx = " + dataA.decodedChidx + ", decodedPort = " + dataA.decodedPort + ", decodedPc = " + byteArrayToString(dataA.decodedPc)
                                                                                + ", decodedCrc = " + byteArrayToString(dataA.decodedCrc) + ", decodedEpc = " + byteArrayToString(dataA.decodedEpc) + ", decodedData1 = " + byteArrayToString(dataA.decodedData1) + ", decodedData2 = " + byteArrayToString(dataA.decodedData2));
                                                                }
                                                            }
                                                        }
                                                        int oldSize2 = mRx000ToRead.size(); mRx000ToRead.add(dataA);  appendToLog("oldSize = " + oldSize2 + ", after adding 8005 mRx000ToRead.size = " + mRx000ToRead.size());
                                                    } else {
                                                        dataA.responseType = HostCmdResponseTypes.TYPE_18K6C_INVENTORY_COMPACT;
                                                        if (true) {
                                                            if (dataA.dataValues.length < 3)
                                                                dataA.decodedError = "Received TYPE_18K6C_INVENTORY with length = " + String.valueOf(dataA.dataValues.length) + ", data = " + byteArrayToString(dataA.dataValues);
                                                            else {
                                                                int index = 0;
                                                                byte[] dataValuesFull = dataA.dataValues;
                                                                while (index < dataValuesFull.length) {
                                                                    dataA.decodedTime = System.currentTimeMillis();
                                                                    if (dataValuesFull.length >= index + 2) {
                                                                        dataA.decodedPc = new byte[2];
                                                                        System.arraycopy(dataValuesFull, index, dataA.decodedPc, 0, dataA.decodedPc.length);
                                                                        index += 2;
                                                                    } else {
                                                                        break;
                                                                    }
                                                                    int epcLength = ((dataA.decodedPc[0] & 0xFF) >> 3) * 2;
                                                                    if (dataValuesFull.length >= index + epcLength) {
                                                                        dataA.decodedEpc = new byte[epcLength];
                                                                        System.arraycopy(dataValuesFull, index, dataA.decodedEpc, 0, epcLength);
                                                                        index += epcLength;
                                                                    }
                                                                    if (dataValuesFull.length >= index + 1) {
                                                                        if (true)
                                                                            dataA.decodedRssi = decodeWideBandRSSI(dataValuesFull[index]);
                                                                        else {
                                                                            byte nbRssi = dataValuesFull[index];
                                                                            byte mantissa = nbRssi;
                                                                            mantissa &= 0x07;
                                                                            byte exponent = nbRssi;
                                                                            exponent >>= 3;
                                                                            dataA.decodedRssi = 20 * log10(pow(2, exponent) * (1 + (mantissa / pow(2, 3))));
                                                                        }
                                                                        index++;
                                                                    }
                                                                    if (false)
                                                                        appendToLog((dataA.dataValues != null ? "mRfidToRead.size() = " + mRfidDevice.mRfidToRead.size() + ", dataValues = " + byteArrayToString(dataA.dataValues) + ", " : "") + "2 decodedRssi = " + dataA.decodedRssi + ", decodedPc = " + byteArrayToString(dataA.decodedPc) + ", decodedEpc = " + byteArrayToString(dataA.decodedEpc));
                                                                    if (dataValuesFull.length > index) {
                                                                        mRx000ToRead.add(dataA);

                                                                        int iDecodedPortOld = dataA.decodedPort;
                                                                        dataA = new Rx000pkgData();
                                                                        dataA.decodedPort = iDecodedPortOld;
                                                                        dataA.responseType = HostCmdResponseTypes.TYPE_18K6C_INVENTORY_COMPACT;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        int oldSize3 = mRx000ToRead.size(); mRx000ToRead.add(dataA);  appendToLog("oldSize = " + oldSize3 + ", after adding 8005 mRx000ToRead.size = " + mRx000ToRead.size());
                                                    }
                                                    if (DEBUG)
                                                        appendToLog("command 18K6C_INVENTORY is found with data=" + byteArrayToString(dataA.dataValues));
                                                }
                                                break;
                                            case 6: //RFID_PACKET_TYPE_18K6C_TAG_ACCESS
                                                if (dataIn[startIndex + 0] != 1) {
                                                    if (DEBUG)
                                                        appendToLog("command 18K6C_TAG_ACCESS is found without first byte as 0x02, " + byteArrayToString(dataInPayload));
                                                    break;
                                                } else {
                                                    dataA.responseType = HostCmdResponseTypes.TYPE_18K6C_TAG_ACCESS;
                                                    if (true) {
                                                        byte[] dataInPayload_full = new byte[expectedLength];
                                                        System.arraycopy(dataIn, startIndex, dataInPayload_full, 0, dataInPayload_full.length);
                                                        appendToLog("command TYPE_18K6C_TAG_ACCESS is found with packageLength=" + packageLengthRead + ", " + byteArrayToString(dataInPayload_full));
                                                    }
                                                    if (true) {
                                                        int accessError, backscatterError;
                                                        boolean timeoutError, crcError;
                                                        if (dataA.dataValues.length >= 8 + 12) {
                                                            backscatterError = 0;
                                                            accessError = 0;
                                                            timeoutError = false;
                                                            crcError = false;
                                                            if ((dataA.dataValues[1] & 8) != 0)
                                                                crcError = true;
                                                            else if ((dataA.dataValues[1] & 4) != 0)
                                                                timeoutError = true;
                                                            else if ((dataA.dataValues[1] & 2) != 0)
                                                                backscatterError = (dataA.dataValues[13] & 0xFF);
                                                            else if ((dataA.dataValues[1] & 1) != 0 && dataA.dataValues.length >= 8 + 12 + 4)
                                                                accessError = (dataA.dataValues[20] & 0xFF) + (dataA.dataValues[21] & 0xFF) * 256;

                                                            byte[] dataRead = new byte[dataA.dataValues.length - 20];
                                                            System.arraycopy(dataA.dataValues, 20, dataRead, 0, dataRead.length);
                                                            if (backscatterError == 0 && accessError == 0 && timeoutError == false && crcError == false) {
                                                                if ((dataA.dataValues[12] == (byte) 0xC3) || (dataA.dataValues[12] == (byte) 0xC4) || (dataA.dataValues[12] == (byte) 0xC5)
                                                                        || (dataA.dataValues[12] == (byte) 0xD5) || (dataA.dataValues[12] == (byte) 0xE2))
                                                                    dataA.decodedResult = "";
                                                                else if ((dataA.dataValues[12] == (byte) 0xC2) || (dataA.dataValues[12] == (byte) 0xE0))
                                                                    dataA.decodedResult = byteArrayToString(dataRead);
                                                                else
                                                                    dataA.decodedError = "Received TYPE_18K6C_TAG_ACCESS with unhandled command = " + String.valueOf(dataA.dataValues[12]) + ", data = " + byteArrayToString(dataA.dataValues);
                                                            } else {
                                                                dataA.decodedError = "Received TYPE_18K6C_TAG_ACCESS with Error ";
                                                                if (crcError)
                                                                    dataA.decodedError += "crcError=" + crcError + ", ";
                                                                if (timeoutError)
                                                                    dataA.decodedError += "timeoutError=" + timeoutError + ", ";
                                                                if (backscatterError != 0) {
                                                                    dataA.decodedError += "backscatterError:";
                                                                    String strErrorMessage = String.valueOf(backscatterError);
                                                                    switch (backscatterError) {
                                                                        case 3:
                                                                            strErrorMessage = "Specified memory location does not exist or the PC value is not supported by the tag";
                                                                            break;
                                                                        case 4:
                                                                            strErrorMessage = "Specified memory location is locked and/or permalocked and is not writeable";
                                                                            break;
                                                                        case 0x0B:
                                                                            strErrorMessage = "Tag has insufficient power to perform the memory write";
                                                                            break;
                                                                        case 0x0F:
                                                                            strErrorMessage = "Tag does not support error-specific codes";
                                                                        default:
                                                                            break;
                                                                    }
                                                                    dataA.decodedError += strErrorMessage + ", ";
                                                                }
                                                                if (accessError != 0) {
                                                                    dataA.decodedError += "accessError: ";
                                                                    String strErrorMessage = String.valueOf(accessError);
                                                                    switch (accessError) {
                                                                        case 0x01:
                                                                            strErrorMessage = "Read after write verify failed";
                                                                            break;
                                                                        case 0x02:
                                                                            strErrorMessage = "Problem transmitting tag command";
                                                                            break;
                                                                        case 0x03:
                                                                            strErrorMessage = "CRC error on tag response to a write";
                                                                            break;
                                                                        case 0x04:
                                                                            strErrorMessage = "CRC error on the read packet when verifying the write";
                                                                            break;
                                                                        case 0x05:
                                                                            strErrorMessage = "Maximum retries on the write exceeded";
                                                                            break;
                                                                        case 0x06:
                                                                            strErrorMessage = "Failed waiting for read data from tag, possible timeout";
                                                                            break;
                                                                        case 0x07:
                                                                            strErrorMessage = "Failure requesting a new tag handle";
                                                                            break;
                                                                        case 0x09:
                                                                            strErrorMessage = "Out of retries";
                                                                            break;
                                                                        case 0x0A:
                                                                            strErrorMessage = "Error waiting for tag response, possible timeout";
                                                                            break;
                                                                        case 0x0B:
                                                                            strErrorMessage = "CRC error on tag response to a kill";
                                                                            break;
                                                                        case 0x0C:
                                                                            strErrorMessage = "Problem transmitting 2nd half of tag kill";
                                                                            break;
                                                                        case 0x0D:
                                                                            strErrorMessage = "Tag responded with an invalid handle on first kill command";
                                                                            break;
                                                                        default:
                                                                            break;
                                                                    }
                                                                    dataA.decodedError += strErrorMessage + ", ";
                                                                }
                                                                dataA.decodedError += "data = " + byteArrayToString(dataA.dataValues);
                                                            }
                                                        } else {
                                                            dataA.decodedError = "Received TYPE_18K6C_TAG_ACCESS with length = " + String.valueOf(dataA.dataValues.length) + ", data = " + byteArrayToString(dataA.dataValues);
                                                        }
                                                        //appendToLog("Received TYPE_18K6C_TAG_ACCESS with length = " + String.valueOf(dataA.dataValues.length) + ", data = " + byteArrayToString(dataA.dataValues));
                                                    }
                                                }
                                                int oldSize4 = mRx000ToRead.size(); mRx000ToRead.add(dataA);  appendToLog("oldSize = " + oldSize4 + ", after adding 0006 mRx000ToRead.size = " + mRx000ToRead.size());
                                                if (true) {
                                                    appendToLog("mRx000UplinkHandler(): package read = " + byteArrayToString(dataA.dataValues));
                                                }
                                                break;
                                            case 0x0007:
                                            case 0x8007:    //RFID_PACKET_TYPE_ANTENNA_CYCLE_END    //original 7
                                                if (dataIn[startIndex + 0] != 1 && dataIn[startIndex + 0] != 2) {
                                                    if (DEBUG)
                                                        appendToLog("command TYPE_ANTENNA_CYCLE_END is found without first byte as 0x01 or 0x02, " + byteArrayToString(dataInPayload));
                                                    break;
                                                } else {
                                                    dataA.responseType = HostCmdResponseTypes.TYPE_ANTENNA_CYCLE_END;
                                                    if (DEBUG)
                                                        appendToLog("command TYPE_ANTENNA_CYCLE_END is found with packageLength=" + packageLengthRead + ", " + byteArrayToString(dataInPayload));
                                                }
                                                mRx000ToRead.add(dataA);
                                                break;
                                            case 0x000E:
                                                if (dataIn[startIndex + 0] != 1 && dataIn[startIndex + 0] != 2) {
                                                    if (DEBUG)
                                                        appendToLog("command TYPE_COMMAND_ACTIVE is found without first byte as 0x01 or 0x02, " + byteArrayToString(dataInPayload));
                                                    break;
                                                } else {
                                                    dataA.responseType = HostCmdResponseTypes.TYPE_COMMAND_ACTIVE;
                                                    if (DEBUG)
                                                        appendToLog("command TYPE_COMMAND_ACTIVE is found with packageLength=" + packageLengthRead + ", " + byteArrayToString(dataInPayload));
                                                }
                                                mRx000ToRead.add(dataA);
                                                break;
                                            case 0x3005:    //RFID_PACKET_TYPE_MBP_READ
                                                int address = (dataIn[startIndex + 8] & 0xFF) + (dataIn[startIndex + 9] & 0xFF) * 256;
                                                switch (address) {
                                                    case 0x450:
                                                        mRx000MbpSetting.rxGain = (dataIn[startIndex + 10] & 0xFF) + (dataIn[startIndex + 11] & 0xFF) * 256;
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            case 0x3007:    //RFID_PACKET_TYPE_OEMCFG_READ
                                                address = (dataIn[startIndex + 8] & 0xFF) + (dataIn[startIndex + 9] & 0xFF) * 256 + (dataIn[startIndex + 10] & 0xFF) * 256 * 256 + (dataIn[startIndex + 11] & 0xFF) * 256 * 256 * 256;
                                                switch (address) {
                                                    case 0x02:
//                                                    dataIn[startIndex + 12] = 3;
                                                        mRx000OemSetting.countryCode = (dataIn[startIndex + 12] & 0xFF) + (dataIn[startIndex + 13] & 0xFF) * 256 + (dataIn[startIndex + 14] & 0xFF) * 256 * 256 + (dataIn[startIndex + 15] & 0xFF) * 256 * 256 * 256;
                                                        if (DEBUG)
                                                            appendToLog("countryCode = " + mRx000OemSetting.countryCode);
                                                        break;
                                                    case 0x04:
                                                    case 0x05:
                                                    case 0x06:
                                                    case 0x07:
                                                        System.arraycopy(dataIn, startIndex + 12, mRx000OemSetting.serialNumber, 4 * (address - 4), 4);
                                                        break;
                                                    case 0x08:
                                                    case 0x09:
                                                    case 0x0A:
                                                        System.arraycopy(dataIn, startIndex + 12, mRx000OemSetting.productserialNumber, 4 * (address - 8), 4);
                                                        break;
                                                    case 0x0B:  //VERSIONCODE_MAX
                                                        System.arraycopy(dataIn, startIndex + 12, mRx000OemSetting.productserialNumber, 4 * (address - 8), 4);
                                                        if (dataIn[startIndex + 12] == 0 && dataIn[startIndex + 13] == 0 && dataIn[startIndex + 14] == 0 && dataIn[startIndex + 15] == 0) {
                                                            mRx000OemSetting.versionCode = 0;
                                                        } else if (dataIn[startIndex + 12] == 0x20 && dataIn[startIndex + 13] == 0x17 && dataIn[startIndex + 14] == 0) {
                                                            mRx000OemSetting.versionCode = (dataIn[startIndex + 14] & 0xFF) + (dataIn[startIndex + 15] & 0xFF) * 256;
                                                        }
                                                        if (true)
                                                            appendToLog("versionCode = " + mRx000OemSetting.versionCode);
                                                        break;
                                                    case 0x8E:
                                                /*dataIn[startIndex + 12] = 0x2A; //0x4F;
                                                dataIn[startIndex + 13] = 0x2A; //0x46;
                                                dataIn[startIndex + 14] = 0x4E; //0x41; //0x43;
                                                dataIn[startIndex + 15] = 0x5A; //0x53; //0x41; */
                                                        if (dataIn[startIndex + 12] == 0 || dataIn[startIndex + 13] == 0 || dataIn[startIndex + 14] == 0 || dataIn[startIndex + 15] == 0) {
                                                            mRx000OemSetting.spcialCountryVersion = "";
                                                        } else {
                                                            mRx000OemSetting.spcialCountryVersion = String.valueOf((char) dataIn[startIndex + 15])
                                                                    + String.valueOf((char) dataIn[startIndex + 14])
                                                                    + String.valueOf((char) dataIn[startIndex + 13])
                                                                    + String.valueOf((char) dataIn[startIndex + 12]);
                                                        }
                                                        byte[] dataInPart = new byte[4];
                                                        System.arraycopy(dataIn, startIndex + 12, dataInPart, 0, dataInPart.length);
                                                        if (DEBUG)
                                                            appendToLog("spcialCountryVersion = " + mRx000OemSetting.spcialCountryVersion + ", data = " + byteArrayToString(dataInPart));
                                                        break;
                                                    case 0x8F:
                                                        //dataIn[startIndex + 12] = (byte)0xAA;
                                                        mRx000OemSetting.freqModifyCode = (dataIn[startIndex + 12] & 0xFF) + (dataIn[startIndex + 13] & 0xFF) * 256 + (dataIn[startIndex + 14] & 0xFF) * 256 * 256 + (dataIn[startIndex + 15] & 0xFF) * 256 * 256 * 256;
                                                        if (DEBUG)
                                                            appendToLog("freqModifyCode = " + mRx000OemSetting.freqModifyCode);
                                                        break;
                                                    default:
                                                        break;
                                                }
/*                                            if (address >= 4 && address <= 7) {
                                            for (int i = 0; i < 4; i++) {
                                                mRx000OemSetting.serialNumber[(address - 4) * 4 + i] = dataIn[startIndex + 12 + i];
                                            }
                                        }*/
                                                if (true)
                                                    appendToLog("command OEMCFG_READ is found with address = " + address + ", packageLength=" + packageLengthRead + ", " + byteArrayToString(dataInPayload));
                                                break;
                                            case 0x3008:    //RFID_PACKET_TYPE_ENG_RSSI
                                                appendToLog("Hello123: RFID_PACKET_TYPE_ENG_RSSI S is found: " + byteArrayToString(dataInPayload));
                                                if ((dataIn[startIndex + 8] & 0x02) != 0) {
                                                    mRx000EngSetting.narrowRSSI = (dataIn[startIndex + 28] & 0xFF) + (dataIn[startIndex + 29] & 0xFF) * 256;
                                                    mRx000EngSetting.wideRSSI = (dataIn[startIndex + 30] & 0xFF) + (dataIn[startIndex + 31] & 0xFF) * 256;
                                                    appendToLog("Hello123: narrorRSSI = " + String.format("%04X", mRx000EngSetting.narrowRSSI) + ", wideRSSI = " + String.format("%04X", mRx000EngSetting.wideRSSI));
                                                }
                                                break;
                                            default:
                                                if (DEBUG)
                                                    appendToLog("command OTHERS is found: " + byteArrayToString(dataInPayload) + ", with packagelength=" + packageLengthRead + ", packageTypeRead=" + packageTypeRead);
                                                break;
                                        }
                                        packageFound = true;
                                        packageType = 3;
                                        startIndexNew = startIndex + expectedLength;
                                    }
                                }
                            }
                        }

                        if (packageFound) {
                            packageFound = false;
                            if (false)
                                appendToLog("mRx000UplinkHandler(): packageFound " + packageType + " with mRfidToReadingOffset=" + mRfidToReadingOffset + ", startIndexOld= " + startIndexOld + ", startIndex= " + startIndex + ", startIndexNew=" + startIndexNew);
                            if (startIndex != startIndexOld) {
                                byte[] unhandledBytes = new byte[startIndex - startIndexOld];
                                System.arraycopy(mRfidToReading, startIndexOld, unhandledBytes, 0, unhandledBytes.length);
                                if (true)
                                    appendToLog("mRx000UplinkHandler(): packageFound with invalid unused data: " + unhandledBytes.length + ", " + byteArrayToString(unhandledBytes));
                                invalidUpdata++;
                            }
                            if (false) {
                                byte[] usedBytes = new byte[startIndexNew - startIndex];
                                System.arraycopy(mRfidToReading, startIndex, usedBytes, 0, usedBytes.length);
                                appendToLog("mRx000UplinkHandler(): used data = " + usedBytes.length + ", " + byteArrayToString(usedBytes));
                            }
                            byte[] mRfidToReadingNew = new byte[RFID_READING_BUFFERSIZE];
                            System.arraycopy(mRfidToReading, startIndexNew, mRfidToReadingNew, 0, mRfidToReadingOffset - startIndexNew);
                            mRfidToReading = mRfidToReadingNew;
                            mRfidToReadingOffset -= startIndexNew;
                            startIndex = 0;
                            startIndexNew = 0;
                            startIndexOld = 0;
                            if (mRfidToReadingOffset != 0) {
                                byte[] remainedBytes = new byte[mRfidToReadingOffset];
                                System.arraycopy(mRfidToReading, 0, remainedBytes, 0, remainedBytes.length);
                                if (false) appendToLog("mRx000UplinkHandler(): moved with remained bytes=" + byteArrayToString(remainedBytes));
                            }
                            //}
                        } else {
                            startIndex++;
                        }
                    }
                    if (startIndex != 0 && mRfidToReadingOffset != 0) appendToLog("mRx000UplinkHandler(): exit while(-8) loop with startIndex = " + startIndex + ( startIndex == 0 ? "" : "(NON-ZERO)" ) + ", mRfidToReadingOffset=" + mRfidToReadingOffset);
                }
            }
            if (mRfidToReadingOffset == startIndexNew && mRfidToReadingOffset != 0) {
                byte[] unusedData = new byte[mRfidToReadingOffset];
                System.arraycopy(mRfidToReading, 0, unusedData, 0, unusedData.length);
                appendToLog("mRx000UplinkHandler(): Ending with invaid unused data: " + mRfidToReadingOffset + ", " + byteArrayToString(unusedData));
                mRfidToReading = new byte[RFID_READING_BUFFERSIZE];
                mRfidToReadingOffset = 0;
            }
            if (DEBUGTHREAD) appendToLog("mRx000UplinkHandler(): END");
//            if (DEBUG) appendToLog("mRx000UplinkHandler(): END, mRfidToRead.size = " + mRfidDevice.mRfidToRead.size() + ", mCs108DataRead.size= " + mCs108DataRead.size() + ", streamInBufferSize = " + streamInBufferSize);
            bRx000ToReading = false;
        }

        boolean turnOn(boolean onStatus) {
            Cs108RfidData cs108RfidData = new Cs108RfidData();
            if (onStatus) {
                cs108RfidData.rfidPayloadEvent = RfidPayloadEvents.RFID_POWER_ON;
                cs108RfidData.waitUplinkResponse = false;
                clearTempDataIn_request = true;
                addRfidToWrite(cs108RfidData);
                return true;
            } else if (onStatus == false) {
                cs108RfidData.rfidPayloadEvent = RfidPayloadEvents.RFID_POWER_OFF;
                cs108RfidData.waitUplinkResponse = false;
                clearTempDataIn_request = true;
                addRfidToWrite(cs108RfidData);
                return true;
            } else {
                return false;
            }
        }

        boolean sendControlCommand(ControlCommands controlCommands) {
            byte[] msgBuffer = new byte[]{(byte) 0x40, 6, 0, 0, 0, 0, 0, 0};
            boolean needResponse = false;
            if (isBleConnected() == false) return false;
            switch (controlCommands) {
                default:
                    msgBuffer = null;
                case CANCEL:
                    msgBuffer[1] = 1;
                    commandOperating = false;
                    break;
                case SOFTRESET:
                    msgBuffer[1] = 2;
                    needResponse = true;
                    break;
                case ABORT:
                    msgBuffer[1] = 3;
                    needResponse = true;
                    commandOperating = false;
                    break;
                case PAUSE:
                    msgBuffer[1] = 4;
                    break;
                case RESUME:
                    msgBuffer[1] = 5;
                    break;
                case GETSERIALNUMBER:
                    msgBuffer = new byte[]{(byte) 0xC0, 0x06, 0, 0, 0, 0, 0, 0};
                    needResponse = true;
                    break;
                case RESETTOBOOTLOADER:
                    msgBuffer[1] = 7;
                    needResponse = true;
                    break;
            }

            if (msgBuffer == null) {
                if (DEBUG) appendToLog("Invalid control commands");
                return false;
            } else {
                clearTempDataIn_request = true;

                Cs108RfidData cs108RfidData = new Cs108RfidData();
                cs108RfidData.rfidPayloadEvent = Cs108Connector.RfidPayloadEvents.RFID_COMMAND;
                cs108RfidData.dataValues = msgBuffer;
                if (needResponse) {
//                    if (DEBUG) appendToLog("sendControlCommand() adds to mRx000ToWrite");
                    cs108RfidData.waitUplinkResponse = needResponse;
                    addRfidToWrite(cs108RfidData);
//                    mRx000ToWrite.add(cs108RfidData);
                } else {
//                    if (DEBUG) appendToLog("sendControlCommand() adds to mRfidToWrite");
                    cs108RfidData.waitUplinkResponse = needResponse;
                    addRfidToWrite(cs108RfidData);
                }
                if (controlCommands == Cs108Connector.ControlCommands.ABORT) aborting = true;
                return true;
            }
        }

        boolean sendHostRegRequestHST_RFTC_FRQCH_DESC_PLLDIVMULT(int freqChannel) {
            long fccFreqTable[] = new long[]{
                    0x00180E4F, //915.75 MHz
                    0x00180E4D, //915.25 MHz
                    0x00180E1D, //903.25 MHz
                    0x00180E7B, //926.75 MHz
                    0x00180E79, //926.25 MHz
                    0x00180E21, //904.25 MHz
                    0x00180E7D, //927.25 MHz
                    0x00180E61, //920.25 MHz
                    0x00180E5D, //919.25 MHz
                    0x00180E35, //909.25 MHz
                    0x00180E5B, //918.75 MHz
                    0x00180E57, //917.75 MHz
                    0x00180E25, //905.25 MHz
                    0x00180E23, //904.75 MHz
                    0x00180E75, //925.25 MHz
                    0x00180E67, //921.75 MHz
                    0x00180E4B, //914.75 MHz
                    0x00180E2B, //906.75 MHz
                    0x00180E47, //913.75 MHz
                    0x00180E69, //922.25 MHz
                    0x00180E3D, //911.25 MHz
                    0x00180E3F, //911.75 MHz
                    0x00180E1F, //903.75 MHz
                    0x00180E33, //908.75 MHz
                    0x00180E27, //905.75 MHz
                    0x00180E41, //912.25 MHz
                    0x00180E29, //906.25 MHz
                    0x00180E55, //917.25 MHz
                    0x00180E49, //914.25 MHz
                    0x00180E2D, //907.25 MHz
                    0x00180E59, //918.25 MHz
                    0x00180E51, //916.25 MHz
                    0x00180E39, //910.25 MHz
                    0x00180E3B, //910.75 MHz
                    0x00180E2F, //907.75 MHz
                    0x00180E73, //924.75 MHz
                    0x00180E37, //909.75 MHz
                    0x00180E5F, //919.75 MHz
                    0x00180E53, //916.75 MHz
                    0x00180E45, //913.25 MHz
                    0x00180E6F, //923.75 MHz
                    0x00180E31, //908.25 MHz
                    0x00180E77, //925.75 MHz
                    0x00180E43, //912.75 MHz
                    0x00180E71, //924.25 MHz
                    0x00180E65, //921.25 MHz
                    0x00180E63, //920.75 MHz
                    0x00180E6B, //922.75 MHz
                    0x00180E1B, //902.75 MHz
                    0x00180E6D, //923.25 MHz
            };
            byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 3, 0x0C, 0, 0, 0, 0};
            if (freqChannel >= 50) {
                freqChannel = 49;
            }
            long freqData = fccFreqTable[freqChannel];
            msgBuffer[4] = (byte) (freqData % 256);
            msgBuffer[5] = (byte) ((freqData >> 8) % 256);
            msgBuffer[6] = (byte) ((freqData >> 16) % 256);
            msgBuffer[7] = (byte) ((freqData >> 24) % 256);
            return sendHostRegRequest(HostRegRequests.HST_RFTC_FRQCH_DESC_PLLDIVMULT, true, msgBuffer);
        }

        boolean bLowPowerStandby = false;
        public boolean setPwrManagementMode(boolean bLowPowerStandby) {
            appendToLog("pwrMgmtStatus: setPwrManagementMode(" + bLowPowerStandby + ")");
            if (bLowPowerStandby == false) return true;     //for testing if setPwrManagementMode(false) is needed
            if (this.bLowPowerStandby == bLowPowerStandby) return true;
            boolean result = mRfidDevice.mRfidReaderChip.mRx000Setting.writeMAC(0x200, (bLowPowerStandby ? 1 : 0));
            if (result) {
                result = mRfidDevice.mRfidReaderChip.sendHostRegRequestHST_CMD(HostCommands.CMD_SETPWRMGMTCFG);
                this.bLowPowerStandby = bLowPowerStandby;
                mRfidDevice.mRfidReaderChip.mRx000Setting.getPwrMgmtStatus();
            }
            return result;
        }

        boolean sendHostRegRequestHST_CMD(HostCommands hostCommand) {
            long hostCommandData = -1;
            switch (hostCommand) {
                case CMD_WROEM:
                    hostCommandData = 0x02;
                    break;
                case CMD_RDOEM:
                    hostCommandData = 0x03;
                    break;
                case CMD_ENGTEST:
                    hostCommandData = 0x04;
                    break;
                case CMD_MBPRDREG:
                    hostCommandData = 0x05;
                    break;
                case CMD_MBPWRREG:
                    hostCommandData = 0x06;
                    break;
                case CMD_18K6CINV:
                    hostCommandData = 0x0F;
                    break;
                case CMD_18K6CREAD:
                    hostCommandData = 0x10;
                    break;
                case CMD_18K6CWRITE:
                    hostCommandData = 0x11;
                    break;
                case CMD_18K6CLOCK:
                    hostCommandData = 0x12;
                    break;
                case CMD_18K6CKILL:
                    hostCommandData = 0x13;
                    break;
                case CMD_SETPWRMGMTCFG:
                    hostCommandData = 0x14;
                    break;
                case CMD_UPDATELINKPROFILE:
                    hostCommandData = 0x19;
                    break;
                case CMD_18K6CBLOCKWRITE:
                    hostCommandData = 0x1F;
                    break;
                case CMD_CHANGEEAS:
                    hostCommandData = 0x26;
                    break;
                case CMD_GETSENSORDATA:
                    hostCommandData = 0x3b;
                    break;
                case CMD_AUTHENTICATE:
                    hostCommandData = 0x50;
                    break;
                case CMD_READBUFFER:
                    hostCommandData = 0x51;
                    break;
                case CMD_UNTRACEABLE:
                    hostCommandData = 0x52;
                    break;
                case CMD_FDM_RDMEM:
                    hostCommandData = 0x53; break;
                case CMD_FDM_WRMEM:
                    hostCommandData = 0x54; break;
                case CMD_FDM_AUTH:
                    hostCommandData = 0x55; break;
                case CMD_FDM_GET_TEMPERATURE:
                    hostCommandData = 0x56; break;
                case CMD_FDM_START_LOGGING:
                    hostCommandData = 0x57; break;
                case CMD_FDM_STOP_LOGGING:
                    hostCommandData = 0x58; break;
                case CMD_FDM_WRREG:
                    hostCommandData = 0x59; break;
                case CMD_FDM_RDREG:
                    hostCommandData = 0x5A; break;
                case CMD_FDM_DEEP_SLEEP:
                    hostCommandData = 0x5B; break;
                case CMD_FDM_OPMODE_CHECK:
                    hostCommandData = 0x5C; break;
                case CMD_FDM_INIT_REGFILE:
                    hostCommandData = 0x5d; break;
                case CMD_FDM_LED_CTRL:
                    hostCommandData = 0x5e; break;
            }
            if (hostCommandData == -1) {
                return false;
            } else {
                commandOperating = true;
                byte[] msgBuffer = new byte[]{(byte) 0x70, 1, 0, (byte) 0xf0, 0, 0, 0, 0};
                msgBuffer[4] = (byte) (hostCommandData % 256);
                msgBuffer[5] = (byte) ((hostCommandData >> 8) % 256);
                msgBuffer[6] = (byte) ((hostCommandData >> 16) % 256);
                msgBuffer[7] = (byte) ((hostCommandData >> 24) % 256);
                return sendHostRegRequest(HostRegRequests.HST_CMD, true, msgBuffer);
            }
        }

        ArrayList<byte[]> macAccessHistory = new ArrayList<>();
        boolean bifMacAccessHistoryData(byte[] msgBuffer) {
            if (sameCheck == false) return false;
            if (msgBuffer.length != 8) return false;
            if (msgBuffer[0] != (byte)0x70 || msgBuffer[1] != 1) return false;
            if (msgBuffer[2] == 0 && msgBuffer[3] == (byte)0xF0) return false;
            return true;
        }
        int findMacAccessHistory(byte[] msgBuffer) {
            int i = -1;
            for (i = 0; i < macAccessHistory.size(); i++) {
//                appendToLog("macAccessHistory(" + i + ")=" + byteArrayToString(macAccessHistory.get(i)));
                if (Arrays.equals(macAccessHistory.get(i), msgBuffer)) break;
            }
            if (i == macAccessHistory.size()) i = -1;
            if (i >= 0) appendToLog("macAccessHistory: returnValue = " + i + ", msgBuffer=" + byteArrayToString(msgBuffer));
            return i;
        }
        void addMacAccessHistory(byte[] msgBuffer) {
            byte[] msgBuffer4 = Arrays.copyOf(msgBuffer, 4);
            for (int i = 0; i < macAccessHistory.size(); i++) {
                byte[] macAccessHistory4 = Arrays.copyOf(macAccessHistory.get(i), 4);
                if (Arrays.equals(msgBuffer4, macAccessHistory4)) {
                    appendToLog("macAccessHistory: deleted old record=" + byteArrayToString(macAccessHistory4));
                    macAccessHistory.remove(i);
                    break;
                }
            }
            appendToLog("macAccessHistory: added msgbuffer=" + byteArrayToString(msgBuffer));
            macAccessHistory.add(msgBuffer);
        }

        boolean sendHostRegRequest(HostRegRequests hostRegRequests, boolean writeOperation, byte[] msgBuffer) {
            boolean needResponse = false;
            boolean validRequest = false;

            if (isBleConnected() == false) return false;
            if (false && bifMacAccessHistoryData(msgBuffer)) {
                if (findMacAccessHistory(msgBuffer) >= 0) {
                    appendToLog("setAlgoRetry: No sending as same data = " + byteArrayToString(msgBuffer));
                    return true;
                }
            }
            addMacAccessHistory(msgBuffer);
            switch (hostRegRequests) {
                case MAC_OPERATION:
//                case MAC_VER:
//                case MAC_LAST_COMMAND_DURATION:
                    //needResponse = true;
//                    validRequest = true;
//                    break;
//                case HST_CMNDIAGS:
//                case HST_MBP_ADDR:
//                case HST_MBP_DATA:
//                case HST_OEM_ADDR:
//                case HST_OEM_DATA:
//                    validRequest = true;
//                    break;
                case HST_ANT_CYCLES:
                case HST_ANT_DESC_SEL:
                case HST_ANT_DESC_CFG:
                case MAC_ANT_DESC_STAT:
                case HST_ANT_DESC_PORTDEF:
                case HST_ANT_DESC_DWELL:
                case HST_ANT_DESC_RFPOWER:
                case HST_ANT_DESC_INV_CNT:
                    validRequest = true;
                    break;
                case HST_TAGMSK_DESC_SEL:
                case HST_TAGMSK_DESC_CFG:
                case HST_TAGMSK_BANK:
                case HST_TAGMSK_PTR:
                case HST_TAGMSK_LEN:
                case HST_TAGMSK_0_3:
                    validRequest = true;
                    break;
                case HST_QUERY_CFG:
                case HST_INV_CFG:
                case HST_INV_SEL:
                case HST_INV_ALG_PARM_0:
                case HST_INV_ALG_PARM_1:
                case HST_INV_ALG_PARM_2:
                case HST_INV_ALG_PARM_3:
//                    validRequest = true;
//                    break;
                case HST_INV_EPC_MATCH_CFG:
                case HST_INV_EPCDAT_0_3:
                    validRequest = true;
                    break;
                case HST_TAGACC_DESC_CFG:
                case HST_TAGACC_BANK:
                case HST_TAGACC_PTR:
                case HST_TAGACC_CNT:
                case HST_TAGACC_LOCKCFG:
                case HST_TAGACC_ACCPWD:
                case HST_TAGACC_KILLPWD:
                case HST_TAGWRDAT_SEL:
                case HST_TAGWRDAT_0:
                    validRequest = true;
                    break;
                case HST_RFTC_CURRENT_PROFILE:
                case HST_RFTC_FRQCH_SEL:
                case HST_RFTC_FRQCH_CFG:
                case HST_RFTC_FRQCH_DESC_PLLDIVMULT:
                case HST_RFTC_FRQCH_DESC_PLLDACCTL:
                case HST_RFTC_FRQCH_CMDSTART:
                    validRequest = true;
                    break;
                case HST_AUTHENTICATE_CFG:
                case HST_AUTHENTICATE_MSG:
                case HST_READBUFFER_LEN:
                case HST_UNTRACEABLE_CFG:
                    validRequest = true;
                    break;
                case HST_CMD:
                    validRequest = true;
                    needResponse = true;
                    break;
            }

            appendToLog("checking msgbuffer = " + (msgBuffer == null ? "NULL" : "Valid") + ", validRequst = " + validRequest);
            if (msgBuffer == null || validRequest == false) {
                if (true) appendToLog("Invalid HST_REQ_REQ or null message");
                return false;
            } else {
                appendToLog("True Ending 0");
                Cs108RfidData cs108RfidData = new Cs108RfidData();
                cs108RfidData.rfidPayloadEvent = Cs108Connector.RfidPayloadEvents.RFID_COMMAND;
                cs108RfidData.dataValues = msgBuffer;
                if (needResponse || writeOperation == false) {
                    cs108RfidData.waitUplinkResponse = (needResponse || writeOperation == false);
//                    mRx000ToWrite.add(cs108RfidData);
                    addRfidToWrite(cs108RfidData);
                } else {
                    cs108RfidData.waitUplinkResponse = (needResponse || writeOperation == false);
                    addRfidToWrite(cs108RfidData);
                }
                appendToLog("True Ending");
                return true;
            }
        }

        void addRfidToWrite(Cs108RfidData cs108RfidData) {
            boolean repeatRequest = false;
            if (mRfidDevice.mRfidToWrite.size() != 0) {
                Cs108RfidData cs108RfidData1 = mRfidDevice.mRfidToWrite.get(mRfidDevice.mRfidToWrite.size() - 1);
                if (cs108RfidData.rfidPayloadEvent == cs108RfidData1.rfidPayloadEvent) {
                    if (cs108RfidData.dataValues == null && cs108RfidData1.dataValues == null) {
                        repeatRequest = true;
                    } else if (cs108RfidData.dataValues != null && cs108RfidData1.dataValues != null) {
                        if (cs108RfidData.dataValues.length == cs108RfidData1.dataValues.length) {
                            if (compareArray(cs108RfidData.dataValues, cs108RfidData1.dataValues, cs108RfidData.dataValues.length)) {
                                repeatRequest = true;
                            }
                        }
                    }
                }
            }
            if (repeatRequest == false) {
                appendToLog("add cs108RfidData to mRfidToWrite with rfidPayloadEvent = " + cs108RfidData.rfidPayloadEvent);
                mRfidDevice.mRfidToWrite.add(cs108RfidData);
            }
        }
    }
}

