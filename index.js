
import { NativeModules, DeviceEventEmitter } from 'react-native';

const { RNGeoLocator } = NativeModules;
let listeners = {};

export default RNGeoLocator;
