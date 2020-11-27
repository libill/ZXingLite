/*
 Copyright © 2015, 2016 Jenly Yu <a href="mailto:jenly1314@gmail.com">Jenly</a>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 	http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package com.king.zxing.util

import android.util.Log

/**
 * @author Jenly [Jenly](mailto:jenly1314@gmail.com)
 */
class LogUtils private constructor() {
    companion object {
        const val TAG = "ZXingLite"
        const val COLON = ":"
        const val VERTICAL = "|"

        /** 是否显示Log日志  */
        private var isShowLog = true

        /** Log日志优先权  */
        private var priority = 1

        /**
         * Priority constant for the println method;use System.out.println
         */
        const val PRINTLN = 1

        /**
         * Priority constant for the println method; use Log.v.
         */
        const val VERBOSE = 2

        /**
         * Priority constant for the println method; use Log.d.
         */
        const val DEBUG = 3

        /**
         * Priority constant for the println method; use Log.i.
         */
        const val INFO = 4

        /**
         * Priority constant for the println method; use Log.w.
         */
        const val WARN = 5

        /**
         * Priority constant for the println method; use Log.e.
         */
        const val ERROR = 6

        /**
         * Priority constant for the println method.use Log.wtf.
         */
        const val ASSERT = 7
        const val TAG_FORMAT = "%s.%s(L:%d)"
        fun setShowLog(isShowLog: Boolean) {
            Companion.isShowLog = isShowLog
        }

        fun isShowLog(): Boolean {
            return isShowLog
        }

        fun getPriority(): Int {
            return priority
        }

        fun setPriority(priority: Int) {
            Companion.priority = priority
        }

        /**
         * 根据堆栈生成TAG
         * @return TAG|className.methodName(L:lineNumber)
         */
        private fun generateTag(caller: StackTraceElement): String {
            var tag = TAG_FORMAT
            var callerClazzName = caller.className
            callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1)
            tag = String.format(tag, *arrayOf(callerClazzName, caller.methodName,
                Integer.valueOf(caller.lineNumber)))
            return StringBuilder().append(TAG).append(VERTICAL)
                .append(tag).toString()
        }

        /**
         * 获取堆栈
         * @param n
         * n=0		VMStack
         * n=1		Thread
         * n=3		CurrentStack
         * n=4		CallerStack
         * ...
         * @return
         */
        fun getStackTraceElement(n: Int): StackTraceElement {
            return Thread.currentThread().stackTrace[n]
        }

        /**
         * 获取调用方的堆栈TAG
         * @return
         */
        private val callerStackLogTag: String
            private get() = generateTag(getStackTraceElement(5))

        /**
         *
         * @param t
         * @return
         */
        private fun getStackTraceString(t: Throwable): String {
            return Log.getStackTraceString(t)
        }
        // -----------------------------------Log.v
        /**
         * Log.v
         * @param msg
         */
        fun v(msg: String) {
            if (isShowLog && priority <= VERBOSE) Log.v(
                callerStackLogTag, msg)
        }

        fun v(t: Throwable) {
            if (isShowLog && priority <= VERBOSE) Log.v(
                callerStackLogTag, getStackTraceString(t))
        }

        fun v(msg: String, t: Throwable?) {
            if (isShowLog && priority <= VERBOSE) Log.v(
                callerStackLogTag, msg, t)
        }
        // -----------------------------------Log.d
        /**
         * Log.d
         * @param msg
         */
        fun d(msg: String) {
            if (isShowLog && priority <= DEBUG) Log.d(
                callerStackLogTag, msg)
        }

        fun d(t: Throwable) {
            if (isShowLog && priority <= DEBUG) Log.d(
                callerStackLogTag, getStackTraceString(t))
        }

        fun d(msg: String, t: Throwable?) {
            if (isShowLog && priority <= DEBUG) Log.d(
                callerStackLogTag, msg, t)
        }
        // -----------------------------------Log.i
        /**
         * Log.i
         * @param msg
         */
        fun i(msg: String) {
            if (isShowLog && priority <= INFO) Log.i(
                callerStackLogTag, msg)
        }

        fun i(t: Throwable) {
            if (isShowLog && priority <= INFO) Log.i(
                callerStackLogTag, getStackTraceString(t))
        }

        fun i(msg: String, t: Throwable?) {
            if (isShowLog && priority <= INFO) Log.i(
                callerStackLogTag, msg, t)
        }
        // -----------------------------------Log.w
        /**
         * Log.w
         * @param msg
         */
        fun w(msg: String?) {
            if (isShowLog && priority <= WARN) Log.w(
                callerStackLogTag, msg.toString())
        }

        fun w(t: Throwable) {
            if (isShowLog && priority <= WARN) Log.w(
                callerStackLogTag, getStackTraceString(t))
        }

        fun w(msg: String, t: Throwable?) {
            if (isShowLog && priority <= WARN) Log.w(
                callerStackLogTag, msg, t)
        }
        // -----------------------------------Log.e
        /**
         * Log.e
         * @param msg
         */
        fun e(msg: String) {
            if (isShowLog && priority <= ERROR) Log.e(
                callerStackLogTag, msg)
        }

        fun e(t: Throwable) {
            if (isShowLog && priority <= ERROR) Log.e(
                callerStackLogTag, getStackTraceString(t))
        }

        fun e(msg: String, t: Throwable?) {
            if (isShowLog && priority <= ERROR) Log.e(
                callerStackLogTag, msg, t)
        }
        // -----------------------------------Log.wtf
        /**
         * Log.wtf
         * @param msg
         */
        fun wtf(msg: String) {
            if (isShowLog && priority <= ASSERT) Log.wtf(
                callerStackLogTag, msg)
        }

        fun wtf(t: Throwable) {
            if (isShowLog && priority <= ASSERT) Log.wtf(
                callerStackLogTag, getStackTraceString(t))
        }

        fun wtf(msg: String, t: Throwable?) {
            if (isShowLog && priority <= ASSERT) Log.wtf(
                callerStackLogTag, msg, t)
        }
        // -----------------------------------System.out.print
        /**
         * System.out.print
         *
         * @param msg
         */
        fun print(msg: String?) {
            if (isShowLog && priority <= PRINTLN) print(msg)
        }

        fun print(obj: Any?) {
            if (isShowLog && priority <= PRINTLN) print(obj)
        }
        // -----------------------------------System.out.printf
        /**
         * System.out.printf
         *
         * @param msg
         */
        fun printf(msg: String?) {
            if (isShowLog && priority <= PRINTLN) System.out.printf(
                msg)
        }
        // -----------------------------------System.out.println
        /**
         * System.out.println
         *
         * @param msg
         */
        fun println(msg: String?) {
            if (isShowLog && priority <= PRINTLN) println(msg)
        }

        fun println(obj: Any?) {
            if (isShowLog && priority <= PRINTLN) println(obj)
        }
    }

    init {
        throw AssertionError()
    }
}