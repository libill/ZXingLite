package com.king.zxing.camera

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.king.zxing.Preferences

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
 */ /**
 * Enumerates settings of the preference controlling the front light.
 */
enum class FrontLightMode {
    /** Always on.  */
    ON,

    /** On only when ambient light is low.  */
    AUTO,

    /** Always off.  */
    OFF;

    companion object {
        private fun parse(modeString: String?): FrontLightMode {
            return modeString?.let { valueOf(it) } ?: AUTO
        }

        fun readPref(sharedPrefs: SharedPreferences): FrontLightMode {
            return parse(
                sharedPrefs.getString(Preferences.KEY_FRONT_LIGHT_MODE, AUTO.toString()))
        }

        fun put(context: Context?, mode: FrontLightMode) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putString(Preferences.KEY_FRONT_LIGHT_MODE, mode.toString()).commit()
        }
    }
}