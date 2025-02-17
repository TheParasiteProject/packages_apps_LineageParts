/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod project
 * SPDX-FileCopyrightText: 2017-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.gestures;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.Manifest;
import android.media.AudioManager;
import android.media.session.MediaSessionLegacyHelper;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;

import com.android.internal.os.DeviceKeyHandler;

import lineageos.providers.LineageSettings;

public class KeyHandler implements DeviceKeyHandler {

    private static final String TAG = KeyHandler.class.getSimpleName();

    private static final String GESTURE_WAKEUP_REASON = "lineageparts-gesture-wakeup";
    private static final String PULSE_ACTION = "com.android.systemui.doze.pulse";
    private static final int GESTURE_REQUEST = 0;
    private static final int GESTURE_WAKELOCK_DURATION = 3000;
    private static final int EVENT_PROCESS_WAKELOCK_DURATION = 500;

    private final Context mContext;
    private final AudioManager mAudioManager;
    private final PowerManager mPowerManager;
    private final WakeLock mGestureWakeLock;
    private final EventHandler mEventHandler;
    private final CameraManager mCameraManager;
    private final Vibrator mVibrator;

    private final SparseIntArray mActionMapping = new SparseIntArray();
    private final boolean mProximityWakeSupported;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private WakeLock mProximityWakeLock;
    private boolean mDefaultProximity;
    private int mProximityTimeOut;

    private String mRearCameraId;
    private boolean mTorchEnabled;

    private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int[] keycodes = intent.getIntArrayExtra(
                    TouchscreenGestureConstants.UPDATE_EXTRA_KEYCODE_MAPPING);
            int[] actions = intent.getIntArrayExtra(
                    TouchscreenGestureConstants.UPDATE_EXTRA_ACTION_MAPPING);
            mActionMapping.clear();
            if (keycodes != null && actions != null && keycodes.length == actions.length) {
                for (int i = 0; i < keycodes.length; i++) {
                    mActionMapping.put(keycodes[i], actions[i]);
                }
            }
        }
    };

    public KeyHandler(final Context context) {
        mContext = context;

        mAudioManager = mContext.getSystemService(AudioManager.class);

        mPowerManager = context.getSystemService(PowerManager.class);
        mGestureWakeLock = mPowerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "LineageParts:GestureWakeLock");

        mEventHandler = new EventHandler(Looper.getMainLooper());

        mCameraManager = mContext.getSystemService(CameraManager.class);
        mCameraManager.registerTorchCallback(new TorchModeCallback(), mEventHandler);

        mVibrator = context.getSystemService(Vibrator.class);

        final Resources resources = mContext.getResources();
        mProximityWakeSupported = resources.getBoolean(
                org.lineageos.platform.internal.R.bool.config_proximityCheckOnWake);

        if (mProximityWakeSupported) {
            mProximityTimeOut = resources.getInteger(
                    org.lineageos.platform.internal.R.integer.config_proximityCheckTimeout);
            mDefaultProximity = mContext.getResources().getBoolean(
                    org.lineageos.platform.internal.R.bool.
                            config_proximityCheckOnWakeEnabledByDefault);

            mSensorManager = context.getSystemService(SensorManager.class);
            mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mProximityWakeLock = mPowerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, "LineageParts:ProximityWakeLock");
        }
        mContext.registerReceiver(mUpdateReceiver,
                new IntentFilter(TouchscreenGestureConstants.UPDATE_PREFS_ACTION),
                Context.RECEIVER_NOT_EXPORTED);
    }

    private class TorchModeCallback extends CameraManager.TorchCallback {
        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (!cameraId.equals(mRearCameraId)) return;
            mTorchEnabled = enabled;
        }

        @Override
        public void onTorchModeUnavailable(String cameraId) {
            if (!cameraId.equals(mRearCameraId)) return;
            mTorchEnabled = false;
        }
    }

    public KeyEvent handleKeyEvent(final KeyEvent event) {
        final int action = mActionMapping.get(event.getScanCode(), -1);
        if (action < 0 || event.getAction() != KeyEvent.ACTION_UP || !hasSetupCompleted()) {
            return event;
        }

        if (action != 0 && !mEventHandler.hasMessages(GESTURE_REQUEST)) {
            final Message msg = getMessageForAction(action);
            final boolean proxWakeEnabled = LineageSettings.System.getInt(
                    mContext.getContentResolver(),
                    LineageSettings.System.PROXIMITY_ON_WAKE, mDefaultProximity ? 1 : 0) == 1;
            if (mProximityWakeSupported && proxWakeEnabled && mProximitySensor != null) {
                mGestureWakeLock.acquire(2L * mProximityTimeOut);
                mEventHandler.sendMessageDelayed(msg, mProximityTimeOut);
                processEvent(action);
            } else {
                mGestureWakeLock.acquire(EVENT_PROCESS_WAKELOCK_DURATION);
                mEventHandler.sendMessage(msg);
            }
        }

        return null;
    }

    private boolean hasSetupCompleted() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 0) != 0;
    }

    private void processEvent(final int action) {
        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (mProximityWakeLock.isHeld()) {
                    mProximityWakeLock.release();
                }
                mSensorManager.unregisterListener(this);
                if (!mEventHandler.hasMessages(GESTURE_REQUEST)) {
                    // The sensor took too long; ignoring
                    return;
                }
                mEventHandler.removeMessages(GESTURE_REQUEST);
                if (event.values[0] >= mProximitySensor.getMaximumRange()) {
                    Message msg = getMessageForAction(action);
                    mEventHandler.sendMessage(msg);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Ignore
            }

        }, mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private Message getMessageForAction(final int action) {
        Message msg = mEventHandler.obtainMessage(GESTURE_REQUEST);
        msg.arg1 = action;
        return msg;
    }

    private class EventHandler extends Handler {

        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.arg1) {
                case TouchscreenGestureConstants.ACTION_CAMERA:
                    launchCamera();
                    break;
                case TouchscreenGestureConstants.ACTION_FLASHLIGHT:
                    toggleFlashlight();
                    break;
                case TouchscreenGestureConstants.ACTION_PLAY_PAUSE_MUSIC:
                    playPauseMusic();
                    break;
                case TouchscreenGestureConstants.ACTION_PREVIOUS_TRACK:
                    previousTrack();
                    break;
                case TouchscreenGestureConstants.ACTION_NEXT_TRACK:
                    nextTrack();
                    break;
                case TouchscreenGestureConstants.ACTION_VOLUME_DOWN:
                    volumeDown();
                    break;
                case TouchscreenGestureConstants.ACTION_VOLUME_UP:
                    volumeUp();
                    break;
                case TouchscreenGestureConstants.ACTION_AMBIENT_DISPLAY:
                    launchDozePulse();
                    break;
                case TouchscreenGestureConstants.ACTION_WAKE_DEVICE:
                    wakeDevice();
                    break;
                case TouchscreenGestureConstants.ACTION_BROWSER:
                case TouchscreenGestureConstants.ACTION_DIALER:
                case TouchscreenGestureConstants.ACTION_EMAIL:
                case TouchscreenGestureConstants.ACTION_MESSAGES:
                case TouchscreenGestureConstants.ACTION_WECHAT_PAY:
                case TouchscreenGestureConstants.ACTION_ALIPAY_PAY:
                case TouchscreenGestureConstants.ACTION_WECHAT_SCAN:
                case TouchscreenGestureConstants.ACTION_ALIPAY_SCAN:
                case TouchscreenGestureConstants.ACTION_ALIPAY_TRIP:
                case TouchscreenGestureConstants.ACTION_WALLET_TRIP:
                case TouchscreenGestureConstants.ACTION_GPAY:
                case TouchscreenGestureConstants.ACTION_PAYTM:
                case TouchscreenGestureConstants.ACTION_PAYTM_SCAN_PAY:
                case TouchscreenGestureConstants.ACTION_PAYTM_QR_CODE:
                case TouchscreenGestureConstants.ACTION_PHONEPE:
                case TouchscreenGestureConstants.ACTION_GOOGLE_MAPS:
                case TouchscreenGestureConstants.ACTION_GOOGLE_SEARCH:
                    launchActivity(msg.arg1);
                    break;
            }
        }
    }

    private void launchCamera() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
        final Intent intent = new Intent(lineageos.content.Intent.ACTION_SCREEN_CAMERA_GESTURE);
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT,
                Manifest.permission.STATUS_BAR_SERVICE);
        doHapticFeedback();
    }

    private void launchActivity(int action) {
        performWakeUp();
        ActionUtils.triggerAction(mContext, action);
        doHapticFeedback();
    }

    private void performWakeUp() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), PowerManager.WAKE_REASON_GESTURE,
                GESTURE_WAKEUP_REASON);
    }

    private void toggleFlashlight() {
        String rearCameraId = getRearCameraId();
        if (rearCameraId != null) {
            mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
            try {
                mCameraManager.setTorchMode(rearCameraId, !mTorchEnabled);
                mTorchEnabled = !mTorchEnabled;
            } catch (CameraAccessException e) {
                // Ignore
            }
            doHapticFeedback();
        }
    }

    private void playPauseMusic() {
        dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        doHapticFeedback();
    }

    private void previousTrack() {
        dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        doHapticFeedback();
    }

    private void nextTrack() {
        dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_NEXT);
        doHapticFeedback();
    }

    private void volumeDown() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
        doHapticFeedback();
    }

    private void volumeUp() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
        doHapticFeedback();
    }

    private void launchDozePulse() {
        final boolean dozeEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.DOZE_ENABLED, 1) != 0;
        if (dozeEnabled) {
            mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
            final Intent intent = new Intent(PULSE_ACTION);
            mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            doHapticFeedback();
        }
    }

    private void wakeDevice() {
        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), GESTURE_WAKEUP_REASON);
    }

    private void dispatchMediaKeyWithWakeLockToMediaSession(final int keycode) {
        final MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(mContext);
        if (helper == null) {
            Log.w(TAG, "Unable to send media key event");
            return;
        }
        KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keycode, 0);
        helper.sendMediaButtonEvent(event, true);
        event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
        helper.sendMediaButtonEvent(event, true);
    }


    private void doHapticFeedback() {
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            return;
        }

        final boolean enabled = LineageSettings.System.getInt(mContext.getContentResolver(),
                LineageSettings.System.TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, 1) != 0;
        if (enabled) {
            mVibrator.vibrate(VibrationEffect.get(VibrationEffect.EFFECT_CLICK));
        }
    }

    private String getRearCameraId() {
        if (mRearCameraId == null) {
            try {
                for (final String cameraId : mCameraManager.getCameraIdList()) {
                    final CameraCharacteristics characteristics =
                            mCameraManager.getCameraCharacteristics(cameraId);
                    final int orientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (orientation == CameraCharacteristics.LENS_FACING_BACK) {
                        mRearCameraId = cameraId;
                        break;
                    }
                }
            } catch (CameraAccessException e) {
                // Ignore
            }
        }
        return mRearCameraId;
    }

}
