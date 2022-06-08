package com.obss.textlogger.view

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.obss.textlogger.R
import com.obss.textlogger.TEXT_LOGGER
import com.obss.textlogger.databinding.FilePreviewerBinding
import com.obss.textlogger.utils.clickWithThrottle
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class FilePreviewer : ConstraintLayout {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(
        context,
        attrs
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)


    private lateinit var binding: FilePreviewerBinding
    private var view: View? = null
    private val layoutInflater = LayoutInflater.from(context)

    private var stringBuilderLog: StringBuilder = saveLog()

    private var stringBuilderDefault: StringBuilder = StringBuilder()


    internal fun initFilePreviewer(view: View , activity:Activity , filePathName:String? = null) {
        view.let {
            this.view = it
            binding = FilePreviewerBinding.inflate(layoutInflater, it as ViewGroup, false)
            it.addView(binding.root)

            binding.constraintLayoutFilePreviewerProgressBar.visibility = View.GONE

            binding.textViewFilePreviewerLog.text = stringBuilderLog.toString()

            binding.imageViewFilePreviewerClose.clickWithThrottle {
                clearView()
            }

            binding.imageViewFilePreviewerSave.clickWithThrottle {
                binding.constraintLayoutFilePreviewerProgressBar.visibility = View.VISIBLE
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
                        withContext(Dispatchers.Main){
                            binding.constraintLayoutFilePreviewerProgressBar.visibility = View.GONE
                            Toast.makeText(activity , resources.getString(R.string.file_previewer_save_success , filePath.path) , Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    internal fun clearView() {
        (view as ViewGroup).removeView(binding.root)
    }


    override fun onDetachedFromWindow() {
        clearView()
        super.onDetachedFromWindow()
    }



    private fun saveDefaultFileDetails(filePath: File, context: Context) {
        if (!filePath.exists()) {
            filePath.createNewFile()
            takeDeviceInformationDetails(context)
            stringBuilderDefault.append(stringBuilderLog)
            filePath.appendText(
                stringBuilderDefault.toString()
            )
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
        stringBuilderDefault = StringBuilder()
        stringBuilderDefault.append(
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
            stringBuilderDefault.append(
                "Available Memory:$availableMemory MB\nTotal Memory:$totalMemory MB\nRuntime Max Memory: $runtimeMaxMemory MB \n" +
                        "Runtime Total Memory:$runtimeTotalMemory MB\nRuntime Free Memory:$runtimeFreeMemory MB\nLow Memory: ${lowMemory.toString().trim()}\nAvailable Processors:$availableProcessors\n"
                        + "Used Memory Size:$usedMemorySize MB\nCPU ABI:${cpuAbi.trim()}\nNetwork Usage(Send):$sendNetworkUsage Bytes\nNetwork Usage(Received):$receivedNetworkUsage Bytes\n"
                        + "Battery:${battery.toString().trim()}\n "
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun saveLog() : StringBuilder{
        stringBuilderLog = StringBuilder()
        val command = String.format("logcat -e threadtime *:*")
        val process = Runtime.getRuntime().exec(command)
        val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            stringBuilderLog.append(line).append("\n")
        }
        return stringBuilderLog
    }

}
