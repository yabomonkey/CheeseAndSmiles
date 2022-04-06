package yabomonkey.example.saycheesepicturetaker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.AppBarConfiguration
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yabomonkey.example.saycheesepicturetaker.databinding.ActivityMainBinding

private const val TAG = "MainActivity"
const val APP_TAG = "Smile-Finder"


class MainActivity : BaseActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var delayProgressLabel: TextView
    private lateinit var exposureProgressLabel: TextView

    private lateinit var galleryThumbnail: ImageButton

   override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        activateToolbar(false)

        Log.d(TAG, "onCreate: Starts")

        // Request camera permissions
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        
        val delaySeekBar: SeekBar = findViewById(R.id.delaySeekBar)

        delayProgressLabel = findViewById(R.id.delayTextView)
        delayProgressLabel.text = "Delay (in seconds): ${delaySeekBar.progress}"

        delaySeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                delayProgressLabel.text = "Delay (in seconds): $progress"
//                Log.d(TAG, "delaySeekBar: onProgressChanged")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        val exposureSeekBar: SeekBar = findViewById(R.id.exposureSeekBar)

        exposureProgressLabel = findViewById(R.id.exposureTextView)
        exposureProgressLabel.text = "Length of Photo Session (in seconds): ${exposureSeekBar.progress}"

        exposureSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                exposureProgressLabel.text = "Length of Photo Session (in seconds): $progress"
//                Log.d(TAG, "exposureSeekBar: onProgressChanged")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        var openShutterButton: Button = findViewById(R.id.openShutterButton)

        openShutterButton.setOnClickListener {
            val intent = Intent(this, OpenShutterActivity::class.java)
            intent.putExtra(DELAY_LENGTH, delaySeekBar.progress)
            intent.putExtra(EXPOSURE_LENGTH, exposureSeekBar.progress)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // In the background, load latest photo taken (if any) for gallery thumbnail
        lifecycleScope.launch(Dispatchers.IO) {
            setGalleryThumbnail()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {

            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setGalleryThumbnail() {
        val uriExternal: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.Media._ID,
            MediaStore.Images.ImageColumns.DATE_ADDED,
            MediaStore.Images.ImageColumns.MIME_TYPE
        )
        val cursor: Cursor = contentResolver.query(uriExternal, projection, null,
            null, MediaStore.Images.ImageColumns.DATE_ADDED + " DESC"
        )!!

        if (cursor.moveToFirst()) {
            val columnIndexID = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val imageId: Long = cursor.getLong(columnIndexID)
            val imageURI = Uri.withAppendedPath(uriExternal, "" + imageId)

            // Run the operations in the view's thread
            galleryThumbnail = findViewById(R.id.photo_view_button)
            galleryThumbnail?.let { photoViewButton ->
                photoViewButton.post {
                    // Remove thumbnail padding
                    photoViewButton.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())

                    // Load thumbnail into circular button using Glide
                    Glide.with(photoViewButton)
                        .load(imageURI)
                        .apply(RequestOptions.circleCropTransform())
                        .into(photoViewButton)
                }
            }
        }
        cursor.close()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}