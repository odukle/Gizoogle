package com.odukle.gizoogle

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.activity_dialog_translate.*

var dtShowing = false

class DialogTranslate : AppCompatActivity() {

    companion object {
        lateinit var instance: DialogTranslate
        lateinit var functions: FirebaseFunctions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_dialog_translate)
        setSupportActionBar(toolbar_dt)

        dtShowing = true
        instance = this
        functions = FirebaseFunctions.getInstance("asia-south1")

        setFinishOnTouchOutside(false)

        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        adView_dialog.loadAd(adRequest)

        val viewModel: TTViewModel = ViewModelProvider(this).get(TTViewModel::class.java)
        viewModel.thugText.observe(this, {
            thug_text_dialog.setText(it)
        })

        if (intent.action == Intent.ACTION_SEND) {
            original_text_dialog.setText(intent.getStringExtra(Intent.EXTRA_TEXT))
            if (isOnline(this)) {
                viewModel.translate()
            } else {
                Toast.makeText(this, "check yo' internet connection", Toast.LENGTH_SHORT).show()
            }
        } else if (intent.action == Intent.ACTION_PROCESS_TEXT) {
            original_text_dialog.setText(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT))
            if (isOnline(this)) {
                viewModel.translate()
            } else {
                Toast.makeText(this, "check yo' internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        btn_copy_dialog.setOnClickListener {
            if (thug_text_dialog.text.isNullOrEmpty()) {
                Toast.makeText(this, "Nothing to copy 🙄", Toast.LENGTH_SHORT).show()
            } else {
                val clipBoard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(CLIP_DATA, thug_text_dialog.text.toString())
                clipBoard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to clipboard 🤘", Toast.LENGTH_SHORT).show()
            }
        }

        btn_share_dialog.setOnClickListener {

            if (thug_text_dialog.text.isNullOrEmpty()) {
                Toast.makeText(this, "Nothing to share 🙄", Toast.LENGTH_SHORT).show()
            } else {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, thug_text_dialog.text.toString())
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, "Thug Text"))
                finish()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dt, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.dismiss_dialog) {
            finish()
        }

        return true
    }

    override fun onPause() {
        super.onPause()
        dtShowing = false
    }

    override fun onResume() {
        super.onResume()
        dtShowing = true
    }
}