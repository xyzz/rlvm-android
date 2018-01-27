package `is`.xyz.rlvm

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.system.Os


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun buttonClicked(view: View) {
        Os.setenv("LIBGL_ES", "2", true)
        Os.setenv("HOME", "/sdcard/rlvm/", true)

        val myIntent = Intent(this, GameActivity::class.java)
        startActivity(myIntent)
    }
}
