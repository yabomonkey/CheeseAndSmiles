package yabomonkey.example.saycheesepicturetaker

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import yabomonkey.example.saycheesepicturetaker.databinding.ActivityOpenShutterBinding
import yabomonkey.example.saycheesepicturetaker.utils.SmileAnalyzer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.schedule
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class OpenShutterActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityOpenShutterBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var uiTimer = Timer("uiTimer")
    private var sysTimer = Timer("sysTimer")
    private val handler = Handler(Looper.getMainLooper())
    private var activityRestored: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityOpenShutterBinding.inflate(layoutInflater)

        setContentView(viewBinding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (savedInstanceState?.getString(DELAY_HOLDER) == null && savedInstanceState?.getString(EXPOSURE_HOLDER) == null) {
            //Start a timer based on the  and then launch the camera after the time has run
            startCountdownTimer(intent.getIntExtra(DELAY_LENGTH, 0))
            handler.postDelayed(updateDelayTimer, ((intent.getIntExtra(DELAY_LENGTH, 0)) * 1000).toLong())
        } else if (savedInstanceState.getString(DELAY_HOLDER) != null && savedInstanceState.getString(EXPOSURE_HOLDER) == null) {
            startCountdownTimer(savedInstanceState.getString(DELAY_HOLDER)!!.toInt())
            handler.postDelayed(updateDelayTimer, ((savedInstanceState.getString(DELAY_HOLDER)!!.toInt()) * 1000).toLong())
        } else if (savedInstanceState.getString(DELAY_HOLDER) == null && savedInstanceState.getString(EXPOSURE_HOLDER) != null) {
            activityRestored = true
            startCamera()
            startCountdownTimer(savedInstanceState.getString(EXPOSURE_HOLDER)!!.toInt())
            handler.postDelayed(
                updateExposureTimer,
                (savedInstanceState.getString(EXPOSURE_HOLDER)!!.toInt() * 1000).toLong()
            )
        }
    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Get screen metrics used to setup camera for full screen resolution
        val screenAspectRatio: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics.bounds
            aspectRatio(metrics.width(), metrics.height())
        } else {
            AspectRatio.RATIO_4_3
        }

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            val rotation = viewBinding.viewFinder.display.rotation

            imageCapture = ImageCapture.Builder()
                // We request aspect ratio but no resolution to match preview config, but letting
                // CameraX optimize for whatever specific resolution best fits our use cases
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation, we will have to call this again if rotation changes
                // during the lifecycle of this use case
                .setTargetRotation(rotation)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, SmileAnalyzer(intent.getIntExtra(SMILE_PERCENTAGE, 0)) { allSmiling ->
                        if (allSmiling) takePhoto()
                    }
                    )
                }


            // Select back camera as a default
            val cameraSelector = CameraSelector.Builder().requireLensFacing(intent.getIntExtra(
                SELECTED_CAMERA, 0)).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

        //Start a timer based on the exposure slider setting and then launch the camera after the time has run
        if (!activityRestored) {
            startCountdownTimer(intent.getIntExtra(EXPOSURE_LENGTH, 0))
            handler.postDelayed(
                updateExposureTimer,
                ((intent.getIntExtra(EXPOSURE_LENGTH, 0)) * 1000).toLong()
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        uiTimer.cancel()
        sysTimer.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (handler.hasCallbacks(updateDelayTimer)){
            outState.putString(DELAY_HOLDER, viewBinding.timerCountdown.text.toString())
        }
        handler.removeCallbacks(updateDelayTimer)

        if (handler.hasCallbacks(updateExposureTimer)) {
            outState.putString(EXPOSURE_HOLDER, viewBinding.timerCountdown.text.toString())
        }
        handler.removeCallbacks(updateExposureTimer)
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$APP_TAG")
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun startCountdownTimer(secondsToCount: Int) {
        var counter = secondsToCount

        uiTimer.schedule(1000, 1000) {
            runOnUiThread {
                if (counter > 0) {
                    viewBinding.timerCountdown.text = counter.toString()
                    counter--
                } else {
                    viewBinding.timerCountdown.text = ""
                }
            }
        }
    }

    private val updateDelayTimer = Runnable { startCamera() }

    private val updateExposureTimer = Runnable { finish() }

    companion object {
        private const val TAG = "OpenShutterActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        /**
         *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
         *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
         *
         *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
         *  of preview ratio to one of the provided values.
         *
         *  @param width - preview width
         *  @param height - preview height
         *  @return suitable aspect ratio
         */
        fun aspectRatio(width: Int, height: Int): Int {
            val previewRatio = max(width, height).toDouble() / min(width, height)
            if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
                return AspectRatio.RATIO_4_3
            }
            return AspectRatio.RATIO_16_9
        }
    }
}