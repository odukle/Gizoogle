package com.odukle.gizoogle

import android.content.*
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.get
import androidx.lifecycle.ViewModelProvider
import com.android.billingclient.api.*
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlin.properties.Delegates


private const val TAG = "MainActivity"
const val INTERSTITIAL_UNIT_ID = "ca-app-pub-9193191601772541/6901507623"
const val SKU_AD_REMOVAL = "remove_ads"
const val MY_PREF = "myPref"
const val REMOVE_ADS = "removeAds"
const val CLIP_DATA = "clipData"
const val AD_COUNT = "adCount"

var maShowing = false

class MainActivity : AppCompatActivity(), PurchasesUpdatedListener {

    lateinit var mInterstitialAd: InterstitialAd
    var randomBtnClicked = false
    private lateinit var skuDetails: SkuDetails
    lateinit var billingClient: BillingClient
    var count by Delegates.notNull<Int>()
    lateinit var sharedPref: SharedPreferences
    var adFree by Delegates.notNull<Boolean>()

    companion object {
        lateinit var instance: MainActivity
        lateinit var functions: FirebaseFunctions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
//        Log.d(TAG, "onCreate starts")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        maShowing = true
        sharedPref = getSharedPreferences(MY_PREF, Context.MODE_PRIVATE)
        functions = FirebaseFunctions.getInstance("asia-south1")
        instance = this
        count = sharedPref.getInt(AD_COUNT, 0)

        original_text.hint = "Enter your text here"

        val viewModel: TTViewModel = ViewModelProvider(this).get(TTViewModel::class.java)
//        viewModel.loadQuotesPage()
        viewModel.thugText.observe(this, {
            thug_text.setText(it)
        })

        if (intent.action == Intent.ACTION_SEND) {
            original_text.setText(intent.getStringExtra(Intent.EXTRA_TEXT))
            if (isOnline(this)) {
                viewModel.translate()
            } else {
                Toast.makeText(this, "check yo' internet connection", Toast.LENGTH_SHORT).show()
            }
        } else if (intent.action == Intent.ACTION_PROCESS_TEXT) {
            original_text.setText(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT))
            if (isOnline(this)) {
                viewModel.translate()
            } else {
                Toast.makeText(this, "check yo' internet connection", Toast.LENGTH_SHORT).show()
            }
        }

        billingClient =
            BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "connection ok")
                    val skuList = arrayListOf(SKU_AD_REMOVAL)
                    val params = SkuDetailsParams.newBuilder()
                    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
                    querySkuDetails(params)
                }
            }

            override fun onBillingServiceDisconnected() {
                if (isOnline(this@MainActivity)) {
                    billingClient.startConnection(this)
                }
            }
        })


        btn_thug_talk.setOnClickListener {
            if (isOnline(this)) {
                if (original_text.text.isNullOrEmpty()) {
                    Toast.makeText(
                        this,
                        "Put some text up in tha box yo! 🙄",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this, "Wait, I'm talking to Snoop Dogg! 🗣", Toast.LENGTH_SHORT)
                        .show()
                    viewModel.translate()
                }
            } else {
                Toast.makeText(this, "check yo' internet connection 🙄", Toast.LENGTH_SHORT).show()
            }
        }

        btn_random_text.setOnClickListener {
            viewModel.generateRandomQuote()
        }

        btn_copy.setOnClickListener {
            if (thug_text.text.isNullOrEmpty()) {
                Toast.makeText(this, "Nothing to copy 🙄", Toast.LENGTH_SHORT).show()
            } else {
                val clipBoard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(CLIP_DATA, thug_text.text.toString())
                clipBoard.setPrimaryClip(clip)
                Toast.makeText(this, "Copied to clipboard 🤘", Toast.LENGTH_SHORT).show()
            }
        }

        btn_share.setOnClickListener {

            if (thug_text.text.isNullOrEmpty()) {
                Toast.makeText(this, "Nothing to share 🙄", Toast.LENGTH_SHORT).show()
            } else {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, thug_text.text.toString())
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(sendIntent, "Thug Text"))
            }
        }

        adFree = sharedPref.getBoolean(REMOVE_ADS, false)
        if (!adFree) {
            MobileAds.initialize(this)
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

            mInterstitialAd = InterstitialAd(this)
            mInterstitialAd.adUnitId = INTERSTITIAL_UNIT_ID
            mInterstitialAd.loadAd(adRequest)
            mInterstitialAd.adListener = object : AdListener() {

                override fun onAdOpened() {
                    maShowing = true
                    super.onAdOpened()
                }

                override fun onAdClosed() {
                    mInterstitialAd.loadAd(adRequest)
                }
            }
        } else {
            adView.visibility = View.GONE
        }
//        Log.d(TAG, "onCreate ends")
    }

    private fun querySkuDetails(params: SkuDetailsParams.Builder) {
        Log.d(TAG, "querySkuDetails called")
        CoroutineScope(IO).launch {
            billingClient.querySkuDetailsAsync(
                params.build()
            ) { result, skuDetailsList ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                    Log.d(TAG, "response: ${result.responseCode}, skuDetailsList: $skuDetailsList")
                    for (skuObject in skuDetailsList) {
                        Log.d(TAG, skuObject.sku)
                        skuDetails = skuObject
                    }
                } else {
                    Log.d(TAG, "something went wrong ${result.responseCode}")
                }
            }
        }
        Log.d(TAG, "querySkuDetails ends")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        Log.d(TAG, "create options menu called")
        menuInflater.inflate(R.menu.menu_main, menu)
        if (isDarkTheme()) {
            menu[0].setIcon(R.drawable.ic_baseline_wb_sunny_24)
            if (!adFree) {
                menu[1].setIcon(R.drawable.ic_remove_ads_light)
            } else {
                menu[1].setIcon(R.drawable.ic_ad_block)
            }
            menu[2].setIcon(R.drawable.ic_round_info_24)
        } else {
            menu[0].setIcon(R.drawable.ic_baseline_nights_stay_24)
            if (!adFree) {
                menu[1].setIcon(R.drawable.ic_remove_ads_dark)
            } else {
                menu[1].setIcon(R.drawable.ic_ad_block_dark)
            }
            menu[2].setIcon(R.drawable.ic_round_info_24_black)
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.theme -> {
                if (isDarkTheme()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }

            R.id.about_app -> {
                startActivity(Intent(this, AboutApp::class.java))
            }

            R.id.remove_ads -> {
                if (!adFree) {
                    val flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetails)
                        .build()

                    val responseCode = billingClient.launchBillingFlow(this, flowParams)
                } else {
                    Toast.makeText(
                        this,
                        "No need, you already have the ad-free version 🤑",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }
        return true
    }

    private fun isDarkTheme(): Boolean {
        return resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun createUri(searchString: String): String {
        return Uri.parse("http://www.gizoogle.net/tranzizzle.php")
            .buildUpon()
            .appendQueryParameter("search", searchString)
            .appendQueryParameter("se", "Go+Git+Dis+Shiznit")
            .toString()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {

        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (p in purchases) {
                handlePurchase(p)
            }
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
        } else if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Toast.makeText(this, "Already Purchased", Toast.LENGTH_SHORT).show()
            sharedPref.edit().putBoolean(REMOVE_ADS, true).apply()
            setAdFree()
        } else {
            Log.d(TAG, "other error ${result.responseCode}")
        }
    }


    private fun setAdFree() {
        this.recreate()
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.sku == SKU_AD_REMOVAL) {
            sharedPref.edit().putBoolean(REMOVE_ADS, true).apply()
            setAdFree()
        }

        val acknowledgePurchaseResponseListener =
            AcknowledgePurchaseResponseListener { Log.d(TAG, "Purchase acknowledged") }

        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                CoroutineScope(IO).launch {
                    billingClient.acknowledgePurchase(
                        acknowledgeParams.build(),
                        acknowledgePurchaseResponseListener
                    )
                }
            }
        } else {
            sharedPref.edit().putBoolean(REMOVE_ADS, false).apply()
            adView.visibility = View.VISIBLE
            this.recreate()
        }

    }

    override fun onPause() {
        super.onPause()
        maShowing = false
    }

    override fun onResume() {
        super.onResume()
        maShowing = true
    }
}

@RequiresApi(Build.VERSION_CODES.M)
fun isOnline(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities =
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities != null) {
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
            return true
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
            return true
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
            return true
        }
    }
    return false
}