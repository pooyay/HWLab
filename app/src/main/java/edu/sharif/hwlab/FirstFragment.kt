package edu.sharif.hwlab

//import java.net.http.HttpClient
//import java.net.http.HttpRequest
//import java.net.http.HttpResponse

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import edu.sharif.hwlab.databinding.FragmentFirstBinding
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

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
        val wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val netId = wifiManager.addNetwork(wifiConfig)
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.setupButton.setOnClickListener {
            postRequest()
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}