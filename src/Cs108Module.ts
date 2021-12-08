import {
  NativeModules,
  Platform,
  NativeEventEmitter,
  EventSubscriptionVendor,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-csl-cs108' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const CslCs108 = NativeModules.CslCs108
  ? NativeModules.CslCs108
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export interface Cs108ModuleInterface {
  createClient(restoreIdentifierKey?: string): void;

  startDeviceScan(): void;

  stopDeviceScan(): void;

  getNewDeviceScanned(callback?: any): void;

  connectDevice(address: string, callback?: any): void;

  disconnectDevice(): void;

  startOperation(): void;

  abortOperation(): void;

  getRfidData(callback?: any): void;

  ScanEvent: string;
}

export const Cs108Module: Cs108ModuleInterface & EventSubscriptionVendor =
  CslCs108;
export const EventEmitter = NativeEventEmitter;
