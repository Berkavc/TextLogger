package com.obss.textlogger.listener
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.*

/**
 * This class is used for detecting shake while phone is shaking.
 */
internal class ShakeDetector(private val listener: Listener) : SensorEventListener {

    private var accelerationThreshold = DEFAULT_ACCELERATION_THRESHOLD
    private var lastTime: Long = 0

    companion object {
        const val SENSITIVITY_LIGHT = 11
        const val SENSITIVITY_MEDIUM = 13
        const val SENSITIVITY_HARD = 15
        private const val DEFAULT_ACCELERATION_THRESHOLD = SENSITIVITY_LIGHT
    }

    /** This interface listens for shakes, called on the main thread when the device is shaken.  */
    interface Listener {
        fun hearShake()
    }

    private val queue = SampleQueue()
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    /**
     * This method is used for listening for shakes on devices with appropriate hardware.
     * @return true if the device supports shake detection.
     */
    fun start(sensorManager: SensorManager): Boolean {
        if (accelerometer != null) {
            return true
        }
        accelerometer = sensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER
        )
        if (accelerometer != null) {
            this.sensorManager = sensorManager
            sensorManager.registerListener(
                this, accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
        return accelerometer != null
    }

    /**
     * This method is used to stop listening.
     * Ignored on devices without appropriate hardware.
     */
    fun stop() {
        if (accelerometer != null) {
            queue.clear()
            sensorManager!!.unregisterListener(this, accelerometer)
            sensorManager = null
            accelerometer = null
        }
    }

    /**
     * This method is used to stop listening.
     * Ignored on devices without appropriate hardware.
     */
    override fun onSensorChanged(event: SensorEvent) {
        val accelerating = isAccelerating(event)
        val timestamp = event.timestamp
        queue.add(timestamp, accelerating)
        if (queue.isShaking) {
            val current = System.currentTimeMillis()
            if ((current - lastTime) > 2500L) {
                queue.clear()
                listener.hearShake()
                lastTime = current
            }
        }
    }

    /**
     * This method returns true if the device is currently accelerating.
     * @return magnitudeSquared to detect acceleration
     */
    private fun isAccelerating(event: SensorEvent): Boolean {
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val magnitudeSquared = ax * ax + ay * ay + (az * az).toDouble()
        return magnitudeSquared > accelerationThreshold * accelerationThreshold
    }

    /**
     * This method sets the acceleration of threshold sensitivity.
     */
    fun setSensitivity(accelerationThreshold: Int) {
        this.accelerationThreshold = accelerationThreshold
    }

    /**
     * This class defines queue of samples. Keeps a running average while shaking.
     */
    internal class SampleQueue {
        private val pool = SamplePool()
        private var oldest: Sample? = null
        private var newest: Sample? = null
        private var sampleCount = 0
        private var acceleratingCount = 0

        /**
         * Adds a sample.
         * @param timestamp    in nanoseconds of sample
         * @param accelerating true if > [.accelerationThreshold].
         */
        fun add(timestamp: Long, accelerating: Boolean) {
            purge(timestamp - MAX_WINDOW_SIZE)
            val added = pool.acquire()
            added.timestamp = timestamp
            added.accelerating = accelerating
            added.next = null
            if (newest != null) {
                newest!!.next = added
            }
            newest = added
            if (oldest == null) {
                oldest = added
            }
            sampleCount++
            if (accelerating) {
                acceleratingCount++
            }
        }

        /**
         * Removes all shake samples from this queue.
         */
        fun clear() {
            while (oldest != null) {
                val removed: Sample = oldest as Sample
                oldest = removed.next
                pool.release(removed)
            }
            newest = null
            sampleCount = 0
            acceleratingCount = 0
        }

        /**
         * This method purges shake samples with timestamps older than cutoff.
         */
        fun purge(cutoff: Long) {
            while (sampleCount >= MIN_QUEUE_SIZE && oldest != null && cutoff - oldest!!.timestamp > 0
            ) {
                // Remove sample.
                val removed: Sample = oldest as Sample
                if (removed.accelerating) {
                    acceleratingCount--
                }
                sampleCount--
                oldest = removed.next
                if (oldest == null) {
                    newest = null
                }
                pool.release(removed)
            }
        }

        /**
         * This method copies the shake samples into a list, with the oldest entry at index 0.
         * @return list of shake samples
         */
        fun asList(): List<Sample> {
            val list: MutableList<Sample> =
                ArrayList()
            var s = oldest
            while (s != null) {
                list.add(s)
                s = s.next
            }
            return list
        }

        val isShaking: Boolean
            get() = newest != null && oldest != null && newest!!.timestamp - oldest!!.timestamp >= MIN_WINDOW_SIZE && acceleratingCount >= (sampleCount shr 1) + (sampleCount shr 2)
        companion object {
            /** Window size in ns. Used to compute the average.  */
            private const val MAX_WINDOW_SIZE: Long = 500000000 // 0.5s
            private const val MIN_WINDOW_SIZE = MAX_WINDOW_SIZE shr 1 // 0.25s
            private const val MIN_QUEUE_SIZE = 4
        }
    }


    /**
     * This class is An accelerometer shake sample.
     */
    internal class Sample {
        var timestamp: Long = 0
        var accelerating = false
        var next: Sample? = null
    }

    /**
     * This class is used for pooling samples.
     */
    internal class SamplePool {
        private var head: Sample? = null

        /**
         * This method acquires a sample from the pool.
         * @return acquire samples.
         */
        fun acquire(): Sample {
            var acquired = head
            if (acquired == null) {
                acquired = Sample()
            } else {
                // Remove instance from pool.
                head = acquired.next
            }
            return acquired
        }

        /**
         * This method gives a sample to the pool.
         */
        fun release(sample: Sample) {
            sample.next = head
            head = sample
        }
    }

    /**
     * This method detects changes whether accuracy is changed.
     */
    override fun onAccuracyChanged(
        sensor: Sensor,
        accuracy: Int) {
    }
}