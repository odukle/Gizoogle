package com.odukle.gizoogle

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ThugSearch : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thug_search)
        val url = createUri("Something")
    }


    private fun createUri(searchString: String): String {
        return Uri.parse("http://www.gizoogle.net/tranzizzle.php")
            .buildUpon()
            .appendQueryParameter("search", searchString)
            .appendQueryParameter("se", "Go+Git+Dis+Shiznit")
            .toString()
    }
}