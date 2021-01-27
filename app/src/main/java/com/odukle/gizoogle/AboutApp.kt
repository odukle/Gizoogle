package com.odukle.gizoogle

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_about_app.*

class AboutApp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)

        setSupportActionBar(toolbar_aa)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_24)
    }

    fun openInsta(view: View) {
        //Get url from tag
        val url = view.tag as String
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addCategory(Intent.CATEGORY_BROWSABLE)

        //pass the url to intent data
        intent.data = Uri.parse(url)
        startActivity(intent)
    }

    fun openEmail(view: View) {
        val id = view.tag as String

        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.data = Uri.parse("mailto:$id")
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return true
    }
}