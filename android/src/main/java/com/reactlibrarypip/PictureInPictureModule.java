package com.reactlibrarypip;

import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Rational;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;

public class PictureInPictureModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private final ReactApplicationContext reactContext;

    private static final int ASPECT_WIDTH = 3;
    private static final int ASPECT_HEIGHT = 4;
    private boolean isPipSupported = false;
    private boolean isCustomAspectRatioSupported = false;
    private boolean isPipListenerEnabled = false;
    private Rational aspectRatio;
    private boolean isPlaying = true;

    /**
     * Intent action for media controls from Picture-in-Picture mode.
     */
    private String ACTION_MEDIA_CONTROL = "media_control";

    /**
     * Intent extra for media controls from Picture-in-Picture mode.
     */
    private String EXTRA_CONTROL_TYPE = "control_type";

    /**
     * The request code for play action PendingIntent.
     */
    private int REQUEST_PLAY = 1;

    /**
     * The request code for pause action PendingIntent.
     */
    private int REQUEST_PAUSE = 2;

    /**
     * The request code for info action PendingIntent.
     */
    private int REQUEST_INFO = 3;

    /**
     * The intent extra value for play action.
     */
    private final int CONTROL_TYPE_PLAY = 1;

    /**
     * The intent extra value for pause action.
     */
    private final int CONTROL_TYPE_PAUSE = 2;

    PictureInPictureParams.Builder params = new PictureInPictureParams.Builder();

    public PictureInPictureModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        reactContext.addLifecycleEventListener(this);
        isPipSupported = true;
        isCustomAspectRatioSupported = true;
        aspectRatio = new Rational(ASPECT_WIDTH, ASPECT_HEIGHT);
    }

    @Override
    public String getName() {
        return "PictureInPicture";
    }

    @ReactMethod
    public void showAlertToast(String text) {
        Context context = getReactApplicationContext();
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @ReactMethod
    public void start() {
        if (isPipSupported) {
            if (isCustomAspectRatioSupported) {
                getReactApplicationContext().registerReceiver(mReceiver, new IntentFilter(ACTION_MEDIA_CONTROL));
                params.setAspectRatio(this.aspectRatio);
                getCurrentActivity().enterPictureInPictureMode(params.build());
            } else
                getCurrentActivity().enterPictureInPictureMode();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @ReactMethod
    public void changeIconToPlay() {
        updatePictureInPictureActions(R.drawable.ic_play_arrow_24dp, "Play",
                CONTROL_TYPE_PLAY, REQUEST_PLAY);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @ReactMethod
    public void changeIconToPause() {
        updatePictureInPictureActions(R.drawable.ic_pause_24dp, "Pause",
                CONTROL_TYPE_PAUSE, REQUEST_PAUSE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @ReactMethod
    public void updatePlayerState(boolean isPaused) {

        isPlaying = !isPaused;
        if (isPlaying) changeIconToPause();
        else changeIconToPlay();

    }

    /**
     * This method will be synced and called with appropriate parameters according to the
     * play pause callbacks of the player.
     * This must be done before switching the video to pip mode.
     *
     * @param iconResource
     * @param title
     * @param controlType
     * @param requestCode
     */

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void updatePictureInPictureActions(
            @DrawableRes int iconResource,
            String title,
            int controlType,
            int requestCode
    ) {
        ArrayList<RemoteAction> actions = new ArrayList<>();
        PendingIntent intent = PendingIntent.getBroadcast(
                reactContext,
                requestCode,
                new Intent(ACTION_MEDIA_CONTROL).putExtra(EXTRA_CONTROL_TYPE, controlType),
                0
        );
        actions.add(new RemoteAction(Icon.createWithResource(reactContext,
                iconResource)
                , title, "Info about the video", intent));
        params.setActions(actions);
        getCurrentActivity().setPictureInPictureParams(params.build());
    }

    /**
     * A [BroadcastReceiver] to receive action item events from Picture-in-Picture mode.
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (!intent.getAction().equals(ACTION_MEDIA_CONTROL)) {
                    return;
                }

                // This is where we are called back from Picture-in-Picture action items.
                switch (intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)) {
                    case CONTROL_TYPE_PLAY: {
                        isPlayPauseState(true);
                        //Toast.makeText(context, "Play", Toast.LENGTH_SHORT).show();
                    }
                    break;
                    case CONTROL_TYPE_PAUSE: {
                        isPlayPauseState(false);
                        //Toast.makeText(context, "Pause", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };


    public boolean hasSystemFeature(String featureName) {
//        FeatureInfo.getSystemAvailableFeatures();
        return true;
    }

    public void isPlayPauseState(boolean play) {
        WritableMap event = Arguments.createMap();
        WritableMap error = Arguments.createMap();
        event.putBoolean("state", play);
        sendEvent("EVENT_ACTIVITY_STATE", event);
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }


    @ReactMethod
    public void configureAspectRatio(Integer width, Integer height) {
        aspectRatio = new Rational(width, height);
    }

    @ReactMethod
    public void enableAutoPipSwitch() {
        isPipListenerEnabled = true;
    }

    @ReactMethod
    public void disableAutoPipSwitch() {
        isPipListenerEnabled = false;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onHostResume() {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onHostPause() {
        if (isPipSupported && isPipListenerEnabled) {
            start();
        }
    }

    @Override
    public void onHostDestroy() {
        getReactApplicationContext().unregisterReceiver(mReceiver);
    }

}

