import React, { useEffect, useState, useRef } from 'react';

import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  FlatList,
} from 'react-native';
import { Cs108Manager } from 'react-native-csl-cs108';

let bleManger = new Cs108Manager();

const format2Digit = (value: string | number) => ('0' + value).slice(-2);

const getTime = () => {
  const newDate = new Date();
  return `[${format2Digit(newDate.getHours())}:${format2Digit(
    newDate.getMinutes()
  )}:${format2Digit(newDate.getSeconds())}]:    `;
};

const App = () => {
  const [log, setLog] = useState(getTime() + 'Starting ...\n');
  const [scanning, setScanning] = useState(false);
  const [connectedDevice, setConnectedDevice] = useState<any | null>(null);
  const [list, setList] = useState<any | null>(null);
  const [showPopup, setShowPopup] = useState<boolean>(false);

  const refScroll = useRef<ScrollView>(null);

  const startScan = () => {
    setShowPopup(false);
    setScanning(true);
    setList(null);
    screenLog('Scanning ...');
    bleManger.startDeviceScan((_, ble) => {
      if (ble) {
        // stopScan();
        // screenLog(` FOUND: ${ble.device.address} - ${ble.device.name}`, false);
        // bleManger.clearCache();
        // setTimeout(() => {
        //   setConnectedDevice(ble);
        //   connect(ble.device?.address);
        // }, 100);

        // if (ble.device?.address.toLowerCase().includes('64:e7:d8')) {
        //   stopScan();
        //   screenLog(` FOUND ${ble.device.address} - ${ble.device.name}`, false);
        //   bleManger.clearCache();
        //   setTimeout(() => {
        //     setConnectedDevice(ble);
        //     connect(ble.device?.address);
        //   }, 0);
        //   return;
        // }
        screenLog(` • ${ble.device.address} - ${ble.device.name}`, false);
      }
    });
  };

  const stopScan = () => {
    setScanning(false);
    setTimeout(() => {
      screenLog('Scanning End ...');
    }, 0);
    bleManger.stopDeviceScan();
  };

  const connect = (ble: any) => {
    screenLog('CONNECTING ==> ' + ble.address);
    bleManger.connectDevice(ble.address, (err, _) => {
      if (!err) {
        screenLog('CONNECTED ==> ' + ble.address);
        screenLog('Listening on ==> ' + ble.address + '\n');
        setShowPopup(false);
        setConnectedDevice(ble);
        startReadData();
        refScroll.current?.scrollToEnd({ animated: false });
      } else {
        setConnectedDevice(null);
        screenLog('CONNECT TO ' + ble.address + ' FAIL');
      }
    });
  };

  const toggleConnect = () => {
    if (connectedDevice) {
      setConnectedDevice(false);
      bleManger.stopReadRFID();
      bleManger.disconnectDevice();
    } else {
      setList(bleManger.getListDeviceScanned());
    }
    setShowPopup((prev) => !prev);
  };

  const startReadData = () => {
    bleManger.startReadRFID((err, dataRfid) => {
      if (err) return;
      try {
        screenLog('RFID read: ' + JSON.stringify(dataRfid));
      } catch (error) {
        screenLog('ERROR RFID ' + JSON.stringify(error));
      }
    });
  };

  const screenLog = (data: any, time = true) => {
    setLog((s) => (s += (time ? getTime() : '') + data.toString() + '\n'));
    refScroll.current?.scrollToEnd({ animated: false });
  };

  const clearLog = () => {
    setLog('');
  };

  useEffect(() => {
    bleManger.stopReadRFID();
    bleManger.stopDeviceScan();
    setScanning(false);
    screenLog('Started - No Devices Connected!');
    return () => {
      setConnectedDevice(null);
      bleManger.stopReadRFID();
      bleManger.disconnectDevice();
    };
  }, []);

  const renderItem = ({ item, index }) => {
    return (
      <TouchableOpacity
        style={{ backgroundColor: '#ccc', margin: 7, padding: 5 }}
        onPress={() => connect(item.device)}
      >
        <Text>
          {index + 1 + '/ ' + item.device?.address + ' - ' + item.device?.name}
        </Text>
      </TouchableOpacity>
    );
  };

  return (
    <View style={styles.container}>
      {!showPopup ? (
        <ScrollView
          ref={refScroll}
          style={styles.wrapText}
          contentContainerStyle={{ paddingBottom: 5 }}
        >
          <Text style={styles.text}>{log || '_'}</Text>
        </ScrollView>
      ) : (
        <FlatList
          style={{ width: '100%' }}
          data={list}
          extraData={list}
          renderItem={renderItem}
          keyExtractor={(item) => item?.address}
        />
      )}
      <View style={styles.tools}>
        <View style={styles.row}>
          <TouchableOpacity style={styles.button} onPress={toggleConnect}>
            <Text
              style={[styles.buttonText, connectedDevice && { color: '#0d0' }]}
            >
              {connectedDevice
                ? 'Listening on =>> ' +
                  (connectedDevice?.name ?? connectedDevice?.address)
                : '[ NO DEVICE - press to connect! ]'}
            </Text>
          </TouchableOpacity>
        </View>
        <View style={styles.row}>
          <TouchableOpacity
            style={[styles.button, scanning && styles.disabled]}
            onPress={startScan}
            disabled={scanning}
          >
            <Text style={styles.buttonText}>
              {!scanning ? 'Scan' : 'Scanning'}
            </Text>
            {scanning && (
              <ActivityIndicator
                style={styles.activityIndicator}
                size={'small'}
                color={'#fff'}
              />
            )}
          </TouchableOpacity>
          <TouchableOpacity style={styles.button} onPress={stopScan}>
            <Text style={styles.buttonText}>Stop Scan</Text>
          </TouchableOpacity>
          <TouchableOpacity style={[styles.button]} onPress={clearLog}>
            <Text style={[styles.buttonText, { color: '#ff8080' }]}>
              Clear Log
            </Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
};

export default App;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#151515',
  },
  wrapText: {
    flex: 1,
    width: '100%',
    paddingHorizontal: 5,
  },
  text: {
    flex: 1,
    width: '100%',
    lineHeight: 20,
    fontSize: 14,
    color: '#eee',
  },
  tools: {
    width: '100%',
    backgroundColor: '#000',
    paddingBottom: 10,
  },
  row: {
    flexDirection: 'row',
  },
  button: {
    height: 40,
    flex: 1,
    margin: 5,
    borderRadius: 5,
    padding: 5,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#808080',
  },
  buttonText: {
    fontSize: 15,
    color: '#eee',
  },
  activityIndicator: {
    position: 'absolute',
    right: 5,
  },
  disabled: {
    backgroundColor: '#00994d',
  },
  popup: {
    backgroundColor: '#ccc',
  },
});

// 00:1B:66:E0:D6:BE
// D7:04:A6:47:90
