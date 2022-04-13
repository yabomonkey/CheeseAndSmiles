package yabomonkey.example.saycheesepicturetaker

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.lifecycle.ProcessCameraProvider
import yabomonkey.example.saycheesepicturetaker.facedetector.FaceDetectorProcessor
import com.google.mlkit.vision.face.FaceDetectorOptions
import yabomonkey.example.saycheesepicturetaker.OpenShutterActivity.Companion.aspectRatio
import yabomonkey.example.saycheesepicturetaker.databinding.ActivitySmileOverlayBinding
import yabomonkey.example.saycheesepicturetaker.facedetector.VisionImageProcessor
import yabomonkey.example.saycheesepicturetaker.utils.GraphicOverlay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OpenSmileOverlayActivity : AppCompatActivity()  {
    private lateinit var viewBinding: ActivitySmileOverlayBinding

    private lateinit var cameraExecutor: ExecutorService

    private var drawingOverlay: GraphicOverlay? = null

    private var imageProcessor: VisionImageProcessor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySmileOverlayBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        startSmileOverlayCamera()

        drawingOverlay = findViewById( R.id.cameraDrawingOverlay )

        val realTimeOpts = FaceDetectorOptions.Builder()
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
        imageProcessor = FaceDetectorProcessor(this, realTimeOpts)

    }

    private fun startSmileOverlayCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Get screen metrics used to setup camera for full screen resolution
        val screenAspectRatio: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics.bounds
            aspectRatio(metrics.width(), metrics.height())
        } else {
            AspectRatio.RATIO_4_3
        }

        val rotation = viewBinding.overlayViewFinder.display.rotation
    }
}