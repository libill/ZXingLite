package com.king.zxing.camera

import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.AutoFocusCallback
import android.os.AsyncTask
import android.preference.PreferenceManager
import com.king.zxing.Preferences
import com.king.zxing.util.LogUtils
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.concurrent.RejectedExecutionException

/*
 * Copyright (C) 2012 ZXing authors
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
 */internal class AutoFocusManager(context: Context?, private val camera: Camera?) :
    AutoFocusCallback {
    companion object {
        private const val AUTO_FOCUS_INTERVAL_MS = 1200L
        private var FOCUS_MODES_CALLING_AF: MutableCollection<String> = ArrayList(2)

        init {
            FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_AUTO)
            FOCUS_MODES_CALLING_AF.add(Camera.Parameters.FOCUS_MODE_MACRO)
        }
    }

    private var stopped = false
    private var focusing = false
    private val useAutoFocus: Boolean
    private var outstandingTask: AsyncTask<*, *, *>? = null

    @Synchronized
    override fun onAutoFocus(success: Boolean, theCamera: Camera) {
        focusing = false
        autoFocusAgainLater()
    }

    @Synchronized
    private fun autoFocusAgainLater() {
        if (!stopped && outstandingTask == null) {
            val newTask = AutoFocusTask(this)
            try {
                newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                outstandingTask = newTask
            } catch (ree: RejectedExecutionException) {
                LogUtils.Companion.w("Could not request auto focus", ree)
            }
        }
    }

    @Synchronized
    fun start() {
        if (useAutoFocus) {
            outstandingTask = null
            if (!stopped && !focusing) {
                try {
                    camera!!.autoFocus(this)
                    focusing = true
                } catch (re: RuntimeException) {
                    // Have heard RuntimeException reported in Android 4.0.x+; continue?
                    LogUtils.Companion.w("Unexpected exception while focusing", re)
                    // Try again later to keep cycle going
                    autoFocusAgainLater()
                }
            }
        }
    }

    @Synchronized
    private fun cancelOutstandingTask() {
        if (outstandingTask != null) {
            if (outstandingTask!!.status != AsyncTask.Status.FINISHED) {
                outstandingTask!!.cancel(true)
            }
            outstandingTask = null
        }
    }

    @Synchronized
    fun stop() {
        stopped = true
        if (useAutoFocus) {
            cancelOutstandingTask()
            // Doesn't hurt to call this even if not focusing
            try {
                camera!!.cancelAutoFocus()
            } catch (re: RuntimeException) {
                // Have heard RuntimeException reported in Android 4.0.x+; continue?
                LogUtils.Companion.w("Unexpected exception while cancelling focusing", re)
            }
        }
    }

    private class AutoFocusTask(manager: AutoFocusManager) :
        AsyncTask<Any?, Any?, Any?>() {
        private val weakReference: WeakReference<AutoFocusManager>
        override fun doInBackground(vararg params: Any?): Any? {
            try {
                Thread.sleep(AUTO_FOCUS_INTERVAL_MS)
            } catch (e: InterruptedException) {
                // continue
            }
            val manager = weakReference.get()
            manager?.start()
            return null
        }

        init {
            weakReference = WeakReference(manager)
        }
    }

    init {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val currentFocusMode = camera!!.parameters.focusMode
        useAutoFocus = sharedPrefs.getBoolean(Preferences.KEY_AUTO_FOCUS, true) &&
            FOCUS_MODES_CALLING_AF!!.contains(currentFocusMode)
        LogUtils.Companion.i("Current focus mode '$currentFocusMode'; use auto focus? $useAutoFocus")
        start()
    }
}