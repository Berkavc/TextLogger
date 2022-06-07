package com.obss.textlogger.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.obss.textlogger.R
import com.obss.textlogger.databinding.CustomSnackbarBinding
import kotlinx.coroutines.*

class CustomSnackBar : ConstraintLayout {

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


    private lateinit var binding: CustomSnackbarBinding
    private var view: View? = null
    private val layoutInflater = LayoutInflater.from(context)

    private var coroutineCallCountDown: CoroutineScope? = CoroutineScope(Dispatchers.IO)

    private var countDown: Long = 7000L

    internal fun initSnackBar(view: View, title: String? = null, animationResource: Int? = null) {
        view.let {
            this.view = it
            binding = CustomSnackbarBinding.inflate(layoutInflater, it as ViewGroup, false)
            it.addView(binding.root)
            binding.textViewSnackBarMessage.text =
                title ?: resources.getString(R.string.custom_snackbar_title)
            animationResource?.let {
                binding.lottieSnackBarWarning.setAnimation(animationResource)
            }
            coroutineCallCountDown?.launch {
                async {
                    delay(countDown)
                    withContext(Dispatchers.Main) {
                        clearView()
                    }
                }
            }
        }
    }

    internal fun clearView() {
        coroutineCallCountDown?.let {
            if(it.isActive){
                it.cancel()
            }
        }
        coroutineCallCountDown = null
        (view as ViewGroup).removeView(binding.root)
    }


    override fun onDetachedFromWindow() {
        clearView()
        super.onDetachedFromWindow()
    }

}
