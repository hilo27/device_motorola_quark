/*
 * Copyright (c) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lineageos.settings.device;

import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class CameraActivationSensor implements SensorEventListener, UpdatedStateNotifier {
    private static final String TAG = "LineageActions-CameraSensor";

    private static final int DELAY_BETWEEN_ACTION_IN_MS = 1500;
    private static final int TURN_SCREEN_ON_WAKE_LOCK_MS = 500;

    private final LineageActionsSettings mLineageActionsSettings;
    private final SensorAction mAction;
    private final SensorHelper mSensorHelper;

    private final Sensor mSensor;
    private final Sensor mProx;

    private long mLastAction;

    private boolean mIsEnabled;
    private boolean mProxIsCovered;

    public CameraActivationSensor(LineageActionsSettings cmActionsSettings, SensorAction action,
        SensorHelper sensorHelper) {
        mLineageActionsSettings = cmActionsSettings;
        mAction = action;
        mSensorHelper = sensorHelper;

        mSensor = sensorHelper.getCameraActivationSensor();
        mProx = sensorHelper.getProximitySensor();
        mLastAction = System.currentTimeMillis();
    }

    @Override
    public synchronized void updateState() {
        if (mLineageActionsSettings.isCameraGestureEnabled() && !mIsEnabled) {
            Log.d(TAG, "Enabling");
            mSensorHelper.registerListener(mSensor, this);
            mSensorHelper.registerListener(mProx, mProxListener);
            mIsEnabled = true;
        } else if (!mLineageActionsSettings.isCameraGestureEnabled() && mIsEnabled) {
            Log.d(TAG, "Disabling");
            mSensorHelper.unregisterListener(this);
            mSensorHelper.unregisterListener(mProxListener);
            mIsEnabled = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (mProxIsCovered) {
            Log.d(TAG, "proximity sensor covered, ignoring activate camera");
            return;
        }

        long now = System.currentTimeMillis();

        if (now - mLastAction > DELAY_BETWEEN_ACTION_IN_MS) {
            Log.d(TAG, "activate camera");
            mLastAction = now;
            mAction.action();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private SensorEventListener mProxListener = new SensorEventListener() {
        @Override
        public synchronized void onSensorChanged(SensorEvent event) {
            mProxIsCovered = event.values[0] < mProx.getMaximumRange();
        }

        @Override
        public void onAccuracyChanged(Sensor mSensor, int accuracy) {}
    };
}