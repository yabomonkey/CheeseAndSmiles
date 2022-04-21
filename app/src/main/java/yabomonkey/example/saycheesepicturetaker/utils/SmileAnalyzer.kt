package yabomonkey.example.saycheesepicturetaker.utils

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

private const val TAG = "SmileAnalyzer"

/** Helper type alias used for analysis use case callbacks */
typealias SmileListener = (smiling: Boolean) -> Unit


class SmileAnalyzer(private val smilePercentage: Int, private val listener: SmileListener? = null) : ImageAnalysis.Analyzer {

    @Override
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val detector = FaceDetection.getClient(realTimeOpts)
            var numberOfSmiles = 0
            var allSmiling = false
            detector.process(image)
                .addOnSuccessListener { faces ->
                    for (face in faces) {
                        if (face.smilingProbability != null) {
                            if (face.smilingProbability!! > smilePercentage.toFloat()/100) {
                                numberOfSmiles++

                            }
                        }
                    }
                    if (faces.size != 0 && faces.size == numberOfSmiles) {
                        allSmiling = true
                    }
                    listener?.invoke(allSmiling)
                }
                .addOnFailureListener {
                    Log.e(TAG, "detector.process failed")
                }
            imageProxy.close()
        }
    }
}