package yabomonkey.example.saycheesepicturetaker

import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

private val TAG = "BaseActivity"
internal const val  DELAY_LENGTH = "DELAY_LENGTH"
internal const val  EXPOSURE_LENGTH = "EXPOSURE_LENGTH"
internal const val  SELECTED_CAMERA = "SELECTED_CAMERA"
internal const val  SMILE_PERCENTAGE = "SMILE_PERCENTAGE"
internal const val  DELAY_HOLDER = "DELAY_HOLDER"
internal const val  EXPOSURE_HOLDER = "EXPOSURE_HOLDER"
internal const val IMMERSIVE_FLAG_TIMEOUT = 500L

open class BaseActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    internal fun activateToolbar(enableHome: Boolean) {
        val toolbar = findViewById<View>(R.id.toolbar) as androidx.appcompat.widget.Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(enableHome)
    }
}