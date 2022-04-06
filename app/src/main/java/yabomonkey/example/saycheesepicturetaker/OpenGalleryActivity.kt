package yabomonkey.example.saycheesepicturetaker

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import yabomonkey.example.saycheesepicturetaker.databinding.ActivityGalleryBinding

private const val TAG = "OpenGalleryActivity"

class OpenGalleryActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityGalleryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        Log.d(TAG, "onCreate called")
    }


}