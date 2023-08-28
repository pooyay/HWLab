package edu.sharif.hwlab

//import com.google.android.material.snackbar.Snackbar
//import androidx.navigation.findNavController
//import androidx.navigation.ui.AppBarConfiguration
//import androidx.navigation.ui.navigateUp
//import androidx.navigation.ui.setupActionBarWithNavController
import java.nio.ByteBuffer

import android.content.Context
import android.net.DhcpInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import edu.sharif.hwlab.databinding.ActivityWifiBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URL
import java.net.UnknownHostException
import kotlin.experimental.and


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

    fun setupHotspot() {
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


    fun inetAddressToInt(inetAddress: InetAddress): Int {
        val ipBytes = inetAddress.address
        return ByteBuffer.wrap(ipBytes).int
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
//            postRequest()

            val apiUrl = "http://192.168.4.1/wifi"
            performApiCall(apiUrl)
        }

//        val apiUrl = "http://your.api.endpoint.com"
//        performApiCall(apiUrl)
    }

    private fun performApiCall(apiUrl: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = makeApiCall(apiUrl)
            // Handle the API response on the main thread if needed
            launch(Dispatchers.Main) {
                handleApiResponse(result)
            }
        }
    }

    private fun getBroadcastIpAddress(): String {
        try {
            val enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (enumNetworkInterfaces.hasMoreElements()) {
                val networkInterface = enumNetworkInterfaces.nextElement()
                if (networkInterface.isLoopback)
                    continue
                val interfaceAddress = networkInterface.interfaceAddresses
                return interfaceAddress.find { x -> x.broadcast != null }?.broadcast?.hostAddress
                    ?: ""
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return ""
    }

    fun find_ip() {
        try {
            val bip = getBroadcastIpAddress()
            println("Broadcast IP Address: $bip")

            val socket = DatagramSocket()
            socket.broadcast = true
            val sendData = "GET_LOCAL_IP".toByteArray()
            val sendPacket = DatagramPacket(
                sendData,
                sendData.size,
                InetAddress.getByName(bip),
                12345
            )
            socket.send(sendPacket)
            val receiveData = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            socket.receive(receivePacket)
            val receivedMessage =
                String(receivePacket.data).slice(IntRange(0, receivePacket.length - 1))
            println("Received IP Address: $receivedMessage")
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private suspend fun makeApiCall(apiUrl: String): String {
        find_ip()
        return ""
        /*val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true

        try {
            val outputStream = connection.outputStream
            val postData =
                "ssid=" + binding.ssidText.text + "&password=" + binding.passwordText.text
            outputStream.write(postData.toByteArray())
            outputStream.close()

            val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (inputStream.readLine().also { line = it } != null) {
                response.append(line)
            }
            return response.toString()
        } catch (e: Throwable) {
            //val Log = Logger.getLogger(WifiActivity::class.java.name)
            e.message?.let { Log.e("Api call", it) }
            throw e
        } finally {
            connection.disconnect()
        }
        return ""*/
    }

    private fun handleApiResponse(result: String) {
        Toast.makeText(applicationContext, result, Toast.LENGTH_LONG).show()
        // Handle the API response here
    }


//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_wifi)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }
}