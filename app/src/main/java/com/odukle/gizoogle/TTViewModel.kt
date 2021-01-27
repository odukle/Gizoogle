package com.odukle.gizoogle

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_dialog_translate.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch

private const val TAG = "ViewModel"

class TTViewModel : ViewModel() {

    private val _thugText = MutableLiveData<String>().apply {
        value = ""
    }

    val thugText: LiveData<String> = _thugText

    fun translate() {

        val activity = if (maShowing) {
            MainActivity.instance
        } else {
            DialogTranslate.instance
        }

        if (maShowing) {
            (activity as MainActivity).count++
            activity.sharedPref.edit().putInt(AD_COUNT, activity.count).apply()
            if (activity.count > 3) {
                if (!activity.adFree) {
                    if (activity.mInterstitialAd.isLoaded) {
                        activity.mInterstitialAd.show()
                        activity.count = 0
                        activity.sharedPref.edit().putInt(AD_COUNT, activity.count).apply()
                    } else {
                        Log.d(TAG, "The interstitial wasn't loaded yet.")
                    }
                }
            }
        } else {
            activity as DialogTranslate
        }

        if (maShowing) {
            activity.progress_bar.visibility = View.VISIBLE
        } else if (dtShowing) {
            activity.progress_bar_dialog.visibility = View.VISIBLE
        }

        try {

            val text = if (maShowing) {
                activity.original_text?.text.toString()
            } else if (dtShowing) {
                activity.original_text_dialog?.text.toString()
            } else {
                ""
            }
            thugTranslate(text).addOnCompleteListener {
                if (it.isSuccessful) {
                    val thugText = it.result["value"] as String

                    _thugText.postValue(thugText)
                    Log.d(TAG, thugText)
                } else {
                    if (maShowing) {
                        Toast.makeText(activity, "Error: " +  it.exception?.message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(activity, "Error: " +  it.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }

                if (maShowing) {
                    activity.progress_bar.visibility = View.GONE
                } else if (dtShowing) {
                    activity.progress_bar_dialog.visibility = View.GONE
                }
            }

        } catch (e: Exception) {
            Log.d(TAG, e.stackTraceToString())
            if (maShowing) {
                Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, e.message, Toast.LENGTH_SHORT).show()
            }
        }

    }


    fun generateRandomQuote() {
        val ma = MainActivity.instance
        CoroutineScope(IO).launch {
            ma.runOnUiThread {
                ma.original_text.setText("")
                ma.original_text.hint = "wait, cooking random text..."
            }
            try {

                generateQuote().addOnCompleteListener {
                    if (it.isSuccessful) {
                        val quote = it.result["quote"] as String
                        ma.runOnUiThread {
                            ma.original_text.hint = ""
                            ma.original_text.setText(quote)
                        }

                    } else {
                        if (maShowing) {
                            Toast.makeText(ma, "Error: " +  it.exception?.message, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ma, "Error: " +  it.exception?.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }


            } catch (e: Exception) {
                Log.e(TAG, e.stackTraceToString())
                ma.runOnUiThread {
                    Toast.makeText(ma, e.message, Toast.LENGTH_SHORT).show()
                }
            }

        }

    }

    private fun thugTranslate(text: String): Task<Map<String, Any>> {
        val data = hashMapOf(
            "text" to text
        )

        if (dtShowing) {
            return DialogTranslate.functions
                .getHttpsCallable("thugTranslate")
                .call(data)
                .continueWith { task ->
                    task.result?.data as Map<String, Any>?
                }
        } else {
            return MainActivity.functions
                .getHttpsCallable("thugTranslate")
                .call(data)
                .continueWith { task ->
                    task.result?.data as Map<String, Any>?
                }
        }
    }

    private fun generateQuote(): Task<Map<String, Any>> {

        val data = hashMapOf<String, String>()

        if (dtShowing) {
            return DialogTranslate.functions
                .getHttpsCallable("generateQuote")
                .call(data)
                .continueWith { task ->
                    task.result?.data as Map<String, Any>?
                }
        } else {
            return MainActivity.functions
                .getHttpsCallable("generateQuote")
                .call(data)
                .continueWith { task ->
                    task.result?.data as Map<String, Any>?
                }
        }
    }

}