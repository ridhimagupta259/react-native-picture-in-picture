import { NativeModules, NativeEventEmitter } from 'react-native';

const { PictureInPicture } = NativeModules;
const eventEmitter = new NativeEventEmitter(PictureInPicture);

const eventMap = {
    pipActivityState: 'EVENT_ACTIVITY_STATE',
};

const _subscriptions = new Map();

const addEventListener = (event, handler) => {
    const mappedEvent = eventMap[event];
    if (mappedEvent) {
        let listener;
        listener = eventEmitter.addListener(mappedEvent, handler);

        _subscriptions.set(handler, listener);
        return {
            remove: () => removeEventListener(event, handler),
        };
    } else {
        return {
            remove: () => { },
        };
    }
};

const removeEventListener = (type, handler) => {
    const listener = _subscriptions.get(handler);
    if (!listener) {
        return;
    }
    listener.remove();
    _subscriptions.delete(handler);
};

const removeAllListeners = () => {
    _subscriptions.forEach((listener, key, map) => {
        listener.remove();
        map.delete(key);
    });
};

export const RNPictureInPicture = {
    ...PictureInPicture,
    addEventListener,
    removeEventListener,
    removeAllListeners
}

export default RNPictureInPicture;