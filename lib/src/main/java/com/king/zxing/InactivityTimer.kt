package com.king.zxing

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.BatteryManager
import com.king.zxing.util.LogUtils
import java.lang.ref.WeakReference
import java.util.concurrent.RejectedExecutionException

/*
 * Copyright (C) 2010 ZXing authors
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
 */ /**
 * Finishes an activity after a period of inactivity if the device is on battery power.
 */
class InactivityTimer(private val activity: Activity?) {
    private val powerStatusReceiver: BroadcastReceiver
    private var registered: Boolean
    private var inactivityTask: AsyncTask<Any?, Any?, Any?>? = null
    fun onActivity() {
        cancel()
        try {
            InactivityAsyncTask(activity).also {
                it.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                inactivityTask = it
            }
        } catch (ree: RejectedExecutionException) {
            LogUtils.Companion.w("Couldn't schedule inactivity task; ignoring")
        }
    }

    fun onPause() {
        cancel()
        if (registered) {
            activity!!.unregisterReceiver(powerStatusReceiver)
            registered = false
        } else {
            LogUtils.Companion.w("PowerStatusReceiver was never registered?")
        }
    }

    fun onResume() {
        if (registered) {
            LogUtils.Companion.w("PowerStatusReceiver was already registered?")
        } else {
            activity!!.registerReceiver(powerStatusReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            registered = true
        }
        onActivity()
    }

    private fun cancel() {
        val task: AsyncTask<*, *, *>? = inactivityTask
        if (task != null) {
            task.cancel(true)
            inactivityTask = null
        }
    }

    fun shutdown() {
        cancel()
    }

    private class PowerStatusReceiver(inactivityTimer: InactivityTimer) : BroadcastReceiver() {
        private val weakReference: WeakReference<InactivityTimer>
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_BATTERY_CHANGED == intent.action) {
                // 0 indicates that we're on battery
                val inactivityTimer = weakReference.get()
                if (inactivityTimer != null) {
                    val onBatteryNow = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) <= 0
                    if (onBatteryNow) {
                        inactivityTimer.onActivity()
                    } else {
                        inactivityTimer.cancel()
                    }
                }
            }
        }

        init {
            weakReference = WeakReference(inactivityTimer)
        }
    }

    private class InactivityAsyncTask(activity: Activity?) :
        AsyncTask<Any?, Any?, Any?>() {
        private val weakReference: WeakReference<Activity?> = WeakReference(activity)
        override fun doInBackground(vararg params: Any?): Any? {
            try {
                Thread.sleep(INACTIVITY_DELAY_MS)
                LogUtils.Companion.i("Finishing activity due to inactivity")
                val activity = weakReference.get()
                activity?.finish()
            } catch (e: InterruptedException) {
                // continue without killing
            }
            return null
        }
    }

    companion object {
        private val TAG = InactivityTimer::class.java.simpleName
        private const val INACTIVITY_DELAY_MS = 5 * 60 * 1000L
    }

    init {
        powerStatusReceiver = PowerStatusReceiver(this)
        registered = false
        onActivity()
    }
}