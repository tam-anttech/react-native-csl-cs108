import { Cs108Module } from './Cs108Module';

type ListerInterface = (error?: any, scannedDevice?: any) => void;
export class Cs108Manager {
  _getDevicesTaskInterval: any;
  _getRfidTaskInterval: any;

  _listDeviceScanned: any[];

  constructor() {
    Cs108Module.createClient();
    this._listDeviceScanned = [];
  }

  getListDeviceScanned = () => this._listDeviceScanned;

  stopDeviceScan = () => {
    clearInterval(this._getDevicesTaskInterval);
    Cs108Module.stopDeviceScan();
  };

  startDeviceScan = (listener: ListerInterface) => {
    this._listDeviceScanned.length = 0;
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

  startReadRFID = (listener: ListerInterface) => {
    Cs108Module.abortOperation();
    this._getRfidTaskInterval = setInterval(() => {
      Cs108Module.getRfidData((data: any) => {
        if (!data.includes('ERROR')) {
          const newData = JSON.parse(data);
          listener(null, newData);
        } else {
          listener(`[ERROR_READ_RFID_NULL]`, null);
        }
      });
    }, 1000);
    Cs108Module.startOperation();
  };

  stopReadRFID = () => {
    Cs108Module.abortOperation();
    clearInterval(this._getRfidTaskInterval);
  };

  clearCache = () => {
    this._listDeviceScanned.length = 0;
  };
}
