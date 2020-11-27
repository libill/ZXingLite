package com.king.zxing.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.king.zxing.CaptureFragment
import com.king.zxing.app.util.immersiveStatusBar

/**
 * Fragment扫码
 * @author [Jenly](mailto:jenly1314@gmail.com)
 */
class CaptureFragmentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_activity)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        immersiveStatusBar(this, toolbar, 0.2f)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = intent.getStringExtra(MainActivity.Companion.KEY_TITLE)
        replaceFragment(CaptureFragment.newInstance())
    }

    private fun replaceFragment(fragment: Fragment?) {
        replaceFragment(R.id.fragmentContent, fragment)
    }

    private fun replaceFragment(@IdRes id: Int, fragment: Fragment?) {
        supportFragmentManager.beginTransaction().replace(id, fragment!!).commit()
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.ivLeft -> onBackPressed()
        }
    }
}