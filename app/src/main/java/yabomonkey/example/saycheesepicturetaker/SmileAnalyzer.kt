package yabomonkey.example.saycheesepicturetaker

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

private const val TAG = "SmileAnalyzer"

class SmileAnalyzer(private val listener: (Any) -> Int) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        val realTimeOpts = FaceDetectorOptions.Builder()
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val detector = FaceDetection.getClient(realTimeOpts)
            var numberOfSmiles = 0
            val result = detector.process(image)
                .addOnSuccessListener { faces ->
                    for (face in faces) {
                        if (face.smilingProbability != null) {
                            val smileProb = face.smilingProbability
                            if (smileProb > 0.5) {
                                numberOfSmiles++
                            }
                        }
                        Log.d(TAG, "Detected a face: $face")
                    }
                }
                .addOnFailureListener { e ->
                    // Task failed with an exception
                    // ...
                }

            listener(numberOfSmiles)

            imageProxy.close()
        }

    }
}