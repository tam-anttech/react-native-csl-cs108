import React, { useEffect, useState, useRef } from 'react';

import {
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
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

  const refScroll = useRef<ScrollView>(null);

  const startScan = () => {
    setScanning(true);
    screenLog('Scanning ...');
    bleManger.startDeviceScan((_, ble) => {
      if (ble) {
        console.log(ble);

        if (
          ble.device?.address.toLowerCase().includes('fc:a8:9a') ||
          ble.device?.address.toLowerCase().includes('00:1b:66')
        ) {
          stopScan();
          screenLog(` FOUND ${ble.device.address} - ${ble.device.name}`, false);
          bleManger.clearCache();
          setTimeout(() => {
            connect(ble.device?.address);
          }, 0);
          return;
        }
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

  const connect = (address: string) => {
    screenLog('CONNECTING ==> ' + address);
    bleManger.connectDevice(address, (err, deviceConnected) => {
      console.log('CONNECTED =>> ', { err }, { deviceConnected });
      setConnectedDevice(deviceConnected);
      screenLog('CONNECTED ==> ' + address + '\n');
    });
  };

  const disconnect = () => {
    setConnectedDevice(null);
    bleManger.disconnectDevice();
  };

  const screenLog = (data: any, time = true) => {
    setLog((s) => (s += (time ? getTime() : '') + data.toString() + '\n'));
    refScroll.current?.scrollToEnd({ animated: false });
  };

  const clearLog = () => {
    setLog('');
  };

  useEffect(() => {
    bleManger.stopDeviceScan();
    setScanning(false);
    screenLog('Started - No Devices Connected!');
    return () => {
      stopScan();
    };
  }, []);

  return (
    <View style={styles.container}>
      <ScrollView
        ref={refScroll}
        style={styles.wrapText}
        contentContainerStyle={{ paddingBottom: 5 }}
      >
        <Text style={styles.text}>{log || '_'}</Text>
      </ScrollView>
      <View style={styles.tools}>
        <View style={styles.row}>
          <TouchableOpacity style={styles.button} onPress={disconnect}>
            <Text
              style={[styles.buttonText, connectedDevice && { color: '#0d0' }]}
            >
              {connectedDevice
                ? 'Listening on =>> [ ' + connectedDevice + ' ]'
                : '[ NO DEVICE CONNECTED ]'}
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
    paddingVertical: 10,
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
});

// 00:1B:66:E0:D6:BE
