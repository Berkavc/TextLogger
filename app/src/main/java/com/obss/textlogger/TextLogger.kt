package com.obss.textlogger

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import com.obss.textlogger.listener.ShakeDetector
import com.obss.textlogger.view.CustomSnackBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class TextLogger(private val activity: Activity) : ShakeDetector.Listener {

    private var controlViewAttached = false
    private lateinit var customSnackBar: CustomSnackBar

    private var lottieAnimationResource: Int? = null
    private var title: String? = null

    private var stringBuilderBuild: StringBuilder = StringBuilder()

    fun init(filePathName: String? = null) {
        customSnackBar = CustomSnackBar(activity)
        val coroutineCallLogger = CoroutineScope(Dispatchers.IO)
        coroutineCallLogger.launch {
            async {
                val fileDirectory = activity.filesDir
                val filePath: File
                if (filePathName != null) {
                    filePath = File(fileDirectory, "$filePathName.txt")
                    if (filePath.exists()) {
                        filePath.delete()
                    }
                } else {
                    filePath = File(fileDirectory, "$TEXT_LOGGER.txt")
                    if (filePath.exists()) {
                        filePath.delete()
                    }
                }
                saveDefaultFileDetails(filePath , activity)
            }
        }
        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sd = ShakeDetector(this)
        sd.start(sensorManager)
        val view = activity.window.decorView.rootView
        customSnackBar.initSnackBar(view, title, lottieAnimationResource)
    }

    fun setSnackBarCustomization(title: String? = null, lottieAnimationResource: Int? = null) {
        this.title = title
        this.lottieAnimationResource = lottieAnimationResource
    }

    override fun hearShake() {
        if (!controlViewAttached) {
            controlViewAttached = true
        } else {
            controlViewAttached = false

        }
    }

    private fun saveDefaultFileDetails(filePath: File , context: Context) {
        if (!filePath.exists()) {
            filePath.createNewFile()
            takeDeviceInformationDetails(context)
            filePath.appendText(
                stringBuilderBuild.toString()
            )
            val process = Runtime.getRuntime().exec("logcat")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilderBuild.append(line)
            }
        }
    }

    private fun takeDeviceInformationDetails(context: Context) {
        val deviceId = Build.ID
        val deviceSerial = Build.FINGERPRINT
        val device = Build.DEVICE
        val deviceModel = Build.MODEL
        val deviceType = Build.TYPE
        val deviceUser = Build.USER
        val sdkVersion = Build.VERSION.SDK_INT
        val manufacturer = Build.MANUFACTURER
        val host = Build.HOST
        val hardware = Build.HARDWARE
        val deviceBrand = Build.BRAND
        val product = Build.PRODUCT
        stringBuilderBuild = StringBuilder()
        stringBuilderBuild.append(
            "Device Information:" + "\n"
                    + "ID:" + deviceId + "\n"
                    + "SERIAL: " + deviceSerial + "\n"
                    + "DEVICE:" + device + "\n"
                    + "DEVICE MODEL:" + deviceModel + "\n"
                    + "DEVICE TYPE:" + deviceType + "\n"
                    + "USER:" + deviceUser + "\n"
                    + "SDK VERSION:" + sdkVersion + "\n"
                    + "MANUFACTURER:" + manufacturer + "\n"
                    + "HOST:" + host + "\n"
                    + "HARDWARE:" + hardware + "\n"
                    + "BRAND:" + deviceBrand + "\n"
                    + "PRODUCT:" + product + "\n"
        )
        takeDevicePerformanceDetails(context)
    }

    private fun takeDevicePerformanceDetails(context : Context) {
        try {
            val stringBuilderCpuAbi: StringBuilder = StringBuilder()
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager: ActivityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
            val runtime: Runtime = Runtime.getRuntime()
            val availableMemory = memoryInfo.availMem / 1048576L
            val totalMemory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                memoryInfo.totalMem / 1048576L
            } else {
                null
            }
            val lowMemory = memoryInfo.lowMemory
            val runtimeMaxMemory = runtime.maxMemory() / 1048576L
            val runtimeTotalMemory = runtime.totalMemory() / 1048576L
            val runtimeFreeMemory = runtime.freeMemory() / 1048576L
            val availableProcessors = runtime.availableProcessors()
            val usedMemorySize = (runtimeTotalMemory - runtimeFreeMemory)
            val cpuAbi: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (cpuAbiItem in Build.SUPPORTED_ABIS.iterator()) {
                    stringBuilderCpuAbi.append(cpuAbiItem + "\n")
                }
                stringBuilderCpuAbi.toString()
            } else {
                Build.CPU_ABI.toString()
            }
            val sendNetworkUsage = android.net.TrafficStats.getMobileTxBytes()
            val receivedNetworkUsage = android.net.TrafficStats.getMobileRxBytes()
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            var batteryLevel = -1
            var batteryScale = 1
            if (batteryStatus != null) {
                batteryLevel =
                    batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, batteryLevel)
                batteryScale =
                    batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, batteryScale)
            }
            val battery = batteryLevel / batteryScale.toFloat() * 100
            stringBuilderBuild.append(
                "Available Memory:$availableMemory MB\nTotal Memory:$totalMemory MB\nRuntime Max Memory: $runtimeMaxMemory MB \n" +
                        "Runtime Total Memory:$runtimeTotalMemory MB\nRuntime Free Memory:$runtimeFreeMemory MB\nLow Memory: ${lowMemory.toString().trim()}\nAvailable Processors:$availableProcessors\n"
                        + "Used Memory Size:$usedMemorySize MB\nCPU ABI:${cpuAbi.trim()}\nNetwork Usage(Send):$sendNetworkUsage Bytes\nNetwork Usage(Received):$receivedNetworkUsage Bytes\n"
                        + "Battery:${battery.toString().trim()}\n "
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}