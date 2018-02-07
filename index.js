
import { NativeModules } from 'react-native';

const { ReactLocator } = NativeModules;
let listeners = {};

export default {
  ...ReactLocator,
  stopTracking: () => {
    DeviceEventEmitter.removeAllListeners(ReactLocator.LOCATION_EVENT);
    ReactLocator.stopTracking();
  },
  onLocation: (callback) => {
    if (typeof callbackFn !== 'function') {
      throw 'callback function must be provided';
    }
    return DeviceEventEmitter.addListener(ReactLocator.LOCATION_EVENT, callback);
  }
};
