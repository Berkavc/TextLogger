package com.obss.textlogger

import android.app.Activity
import android.content.Context
import android.hardware.SensorManager
import com.obss.textlogger.listener.ShakeDetector
import com.obss.textlogger.view.CustomSnackBar
import com.obss.textlogger.view.FilePreviewer

class TextLogger(private val activity: Activity) : ShakeDetector.Listener {

    private var controlViewAttached = false
    private lateinit var customSnackBar: CustomSnackBar
    private lateinit var filePreviewer: FilePreviewer

    private var lottieAnimationResource: Int? = null
    private var title: String? = null

    private var filePathName: String? = null

    private val view = activity.window.decorView.rootView

    fun setSnackBarCustomization(title: String? = null, lottieAnimationResource: Int? = null) {
        this.title = title
        this.lottieAnimationResource = lottieAnimationResource
    }

    fun init(filePathName: String? = null) {
        this.filePathName = filePathName
        customSnackBar = CustomSnackBar(activity)
        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sd = ShakeDetector(this)
        sd.start(sensorManager)
        customSnackBar.initSnackBar(view, title, lottieAnimationResource)
        filePreviewer = FilePreviewer(activity)
    }

    override fun hearShake() {
        if (!controlViewAttached) {
            controlViewAttached = true
            filePreviewer.initFilePreviewer(view, activity, filePathName)
        } else {
            controlViewAttached = false
            filePreviewer.clearView()
        }
    }

}