package edu.sharif.hwlab

import android.content.Context
import android.net.DhcpInfo
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import edu.sharif.hwlab.databinding.ActivityWifiBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import kotlin.experimental.or


class WifiActivity : AppCompatActivity() {
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

    private lateinit var binding: ActivityWifiBinding
    private lateinit var espIp: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWifiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GlobalScope.launch(Dispatchers.IO) {
            espIp = findIp()
            binding.ipAddressTextView.text = "Esp IP Address: $espIp"
        }
        setSupportActionBar(binding.toolbar)
        binding.setupButton.setOnClickListener {
            performApiCall("/wifi")
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun performApiCall(route: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = makeApiCall(route)
            // Handle the API response on the main thread if needed
            launch(Dispatchers.Main) {
                handleApiResponse(result)
            }
        }
    }

    private fun getBroadcastIpAddressHotspot(): String? {
        try {
            val enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (enumNetworkInterfaces.hasMoreElements()) {
                val networkInterface = enumNetworkInterfaces.nextElement()
                if (networkInterface.isLoopback)
                    continue
                val interfaceAddress = networkInterface.interfaceAddresses
                return interfaceAddress.find { x -> x.broadcast != null }?.broadcast?.hostAddress
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return null
    }

    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
    }

    fun getBroadcastIpAddressWifi(): String {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val netMask = wifiManager.dhcpInfo.netmask
        return intToIp(wifiManager.connectionInfo?.ipAddress?.or(netMask.inv()) ?: 0)
    }


    private fun getBroadcastIpAddress(): String {
        return getBroadcastIpAddressHotspot() ?: getBroadcastIpAddressWifi()
    }

    private fun findIp(): String {
        var tries = 0
        while (tries < 200) {
            tries++
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
                socket.soTimeout = 300;   // set the timeout in millisecounds.

                socket.send(sendPacket)
                val receiveData = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveData, receiveData.size)
                socket.receive(receivePacket)
                val receivedMessage =
                    String(receivePacket.data).slice(IntRange(0, receivePacket.length - 1))
                println("Received IP Address: $receivedMessage")
                socket.close()
                return receivedMessage
            } catch (e: Throwable) {
                println("FAILED")
                println(e.message)
                e.printStackTrace()
            }
        }
        return "192.168.4.1"
    }


    private suspend fun makeApiCall(route: String): String {
        val url = URL("http://$espIp$route")
        val connection = withContext(Dispatchers.IO) {
            url.openConnection()
        } as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true

        try {
            val outputStream = connection.outputStream
            var postData =
                "ssid=" + binding.ssidText.text + "&password=" + binding.passwordText.text
            if (binding.serverText.text.isNotEmpty()) {
                postData += "&server=" + binding.serverText.text
            }
            withContext(Dispatchers.IO) {
                outputStream.write(postData.toByteArray())
            }
            withContext(Dispatchers.IO) {
                outputStream.close()
            }

            val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (withContext(Dispatchers.IO) {
                    inputStream.readLine()
                }.also { line = it } != null) {
                response.append(line)
            }
            return response.toString()
        } catch (e: Throwable) {
            e.message?.let { Log.e("Api call", it) }
            throw e
        } finally {
            connection.disconnect()
        }
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