package edu.sharif.hwlab

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import edu.sharif.hwlab.databinding.ActivityWifiBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URL
import kotlin.system.exitProcess


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
            if (espIp.isEmpty()) {
                espIp = "37.32.12.22:8000"
                launch(Dispatchers.Main) {
                    binding.ipAddressTextView.text =
                        "Esp IP Address not found in local network, please add server url"
                    binding.serverText.setText(espIp)
                }
            } else {
                launch(Dispatchers.Main) {
                    binding.ipAddressTextView.text = "Esp IP Address: $espIp"
                }
            }

            val mainHandler = Handler(Looper.getMainLooper())

            mainHandler.post(object : Runnable {
                override fun run() {
                    performApiCall("/state", "GET", null)
                    mainHandler.postDelayed(this, 1000)
                }
            })
        }
        setSupportActionBar(binding.toolbar)

        binding.relaySwitch.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            var path = "/off";
            if (b) {
                path = "/on"
            }
            performApiCall(path, "GET", null)
        }
        binding.setupButton.setOnClickListener {
            var postData =
                "ssid=" + binding.ssidText.text + "&password=" + binding.passwordText.text
            if (binding.serverText.text.isNotEmpty()) {
                postData += "&server=" + binding.serverText.text
            }
            performApiCall("/wifi", "POST", postData)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun performApiCall(route: String, method: String, data: String?) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = makeApiCall(route, method, data)
            // Handle the API response on the main thread if needed
            launch(Dispatchers.Main) {
                handleApiResponse(route, result)
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
        while (tries < 30) {
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
        return ""
    }


    private suspend fun makeApiCall(
        route: String,
        method: String = "POST",
        data: String? = null
    ): String {
        val url = URL("http://$espIp$route")
        val connection = withContext(Dispatchers.IO) {
            url.openConnection()
        } as HttpURLConnection
        connection.requestMethod = method

        try {
            if (!data.isNullOrEmpty()) {
                connection.doOutput = true
                val outputStream = connection.outputStream
                withContext(Dispatchers.IO) {
                    outputStream.write(data.toByteArray())
                }
                withContext(Dispatchers.IO) {
                    outputStream.close()
                }
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

    private fun handleApiResponse(route: String, result: String) {
        if (route == "/wifi") {
            Toast.makeText(applicationContext, result, Toast.LENGTH_LONG).show()
        } else if (route == "/state") {
            val obj = JSONObject(result)
            if(obj.get("result") !="ok"){
                Toast.makeText(applicationContext, obj.get("error").toString(), Toast.LENGTH_LONG).show()
                exitProcess(0)
            }
            binding.relaySwitch.isChecked = obj.get("state") as Boolean
        }
    }


//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_wifi)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }
}