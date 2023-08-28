package edu.sharif.hwlab

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
//import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
//import androidx.navigation.findNavController
//import androidx.navigation.ui.AppBarConfiguration
//import androidx.navigation.ui.navigateUp
//import androidx.navigation.ui.setupActionBarWithNavController
import edu.sharif.hwlab.databinding.ActivityWifiBinding
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class WifiActivity : AppCompatActivity() {

    fun postRequest() {
//        val values = mapOf("ssid" to binding.ssidText.text, "password" to binding.passwordText.text)
        val url = URL("http://192.168.4.1/wifi")
        val postData = "ssid=" + binding.ssidText.text + "&password=" + binding.passwordText.text

        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("Content-Length", postData.length.toString())
        conn.useCaches = false

        DataOutputStream(conn.outputStream).use { it.writeBytes(postData) }
        BufferedReader(InputStreamReader(conn.inputStream)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                println(line)
            }
        }
    }

    fun setupHotspot(){
        val esp_ssid = "sakht"
        val esp_password = "12345678"
        val wifiConfig = WifiConfiguration()
        wifiConfig.SSID = java.lang.String.format("\"%s\"", esp_ssid)
        wifiConfig.preSharedKey = java.lang.String.format("\"%s\"", esp_password)
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val netId = wifiManager.addNetwork(wifiConfig)
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()
    }


    //    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityWifiBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

//        val navController = findNavController(R.id.nav_host_fragment_content_wifi)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.setupButton.setOnClickListener {
            postRequest()
        }

    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_wifi)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }
}