package yabomonkey.example.saycheesepicturetaker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import yabomonkey.example.saycheesepicturetaker.databinding.ActivitySmileOverlayBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OpenOverlayActivity : AppCompatActivity()  {
    private lateinit var viewBinding: ActivitySmileOverlayBinding

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySmileOverlayBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
    }
}