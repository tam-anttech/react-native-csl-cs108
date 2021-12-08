import { Cs108Module } from './Cs108Module';

type ListerInterface = (error?: any, scannedDevice?: any) => void;
export class Cs108Manager {
  _getDevicesTaskInterval: any;

  _listDeviceScanned: any[];

  constructor() {
    Cs108Module.createClient();
    this._listDeviceScanned = [];
  }

  stopDeviceScan = () => {
    clearInterval(this._getDevicesTaskInterval);
    this.clearCache();
    Cs108Module.stopDeviceScan();
  };

  startDeviceScan = (listener: ListerInterface) => {
    Cs108Module.stopDeviceScan();
    this._getDevicesTaskInterval = setInterval(() => {
      Cs108Module.getNewDeviceScanned((data: any) => {
        if (!data.includes('ERROR')) {
          const newDevice = JSON.parse(data);
          if (
            !this._listDeviceScanned.some(
              (d: any) => d.device.address === newDevice?.device.address
            )
          ) {
            listener(null, newDevice);
            this._listDeviceScanned.push(newDevice);
          }
        } else {
          listener(`[ERROR_SCAN_NULL]`, null);
        }
      });
    }, 200);
    Cs108Module.startDeviceScan();
  };

  connectDevice = (address: string, listener: ListerInterface) => {
    Cs108Module.connectDevice(address, (deviceConnected: any) => {
      if (!deviceConnected.includes('ERROR')) {
        console.log({ deviceConnected });
        listener(null, deviceConnected);
      } else {
        listener('[ERROR_CONNECT]', null);
      }
    });
  };

  disconnectDevice = () => {
    Cs108Module.disconnectDevice();
  };

  clearCache = () => {
    this._listDeviceScanned.length = 0;
  };
}
