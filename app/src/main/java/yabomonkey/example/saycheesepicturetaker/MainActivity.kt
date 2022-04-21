package yabomonkey.example.saycheesepicturetaker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.setPadding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import yabomonkey.example.saycheesepicturetaker.databinding.ActivityMainBinding
import yabomonkey.example.saycheesepicturetaker.utils.showImmersive

const val APP_TAG = "Smile-Finder"

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var cameraLabelString: String

    private var imagesInGallery: Boolean = false

    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        activateToolbar(false)

        // Request camera permissions
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        val delaySeekBar: SeekBar = binding.mainContainer.delaySeekBar

        binding.mainContainer.delayTextView.text = "Delay before photo shoot (in seconds): ${delaySeekBar.progress}"

        delaySeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.mainContainer.delayTextView.text = "Delay before photo shoot (in seconds): $progress"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        val exposureSeekBar: SeekBar = findViewById(R.id.exposureSeekBar)


        binding.mainContainer.exposureTextView.text =
            "Length of Photo Session (in seconds): ${exposureSeekBar.progress}"

        exposureSeekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                "Length of Photo Shoot (in seconds): $progress".also { binding.mainContainer.exposureTextView.text = it }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.mainContainer.cameraButtonGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (checkedId == R.id.backCamMaterialButton) {
                cameraLabelString = getString(R.string.cameraSelectedLabelText) + " " + binding.mainContainer.backCamMaterialButton.text
                binding.mainContainer.cameraTextView.text = cameraLabelString
            }
            else if (checkedId == R.id.frontCamMaterialButton) {
                cameraLabelString = getString(R.string.cameraSelectedLabelText) + " " + binding.mainContainer.frontCamMaterialButton.text
                binding.mainContainer.cameraTextView.text = cameraLabelString
            }
        }

        checkAvailableCameras()

        if (binding.mainContainer.cameraButtonGroup.checkedButtonId == R.id.backCamMaterialButton) {
            cameraLabelString = getString(R.string.cameraSelectedLabelText) + " " + binding.mainContainer.backCamMaterialButton.text
            binding.mainContainer.cameraTextView.text = cameraLabelString
        } else if (binding.mainContainer.cameraButtonGroup.checkedButtonId == R.id.frontCamMaterialButton) {
            cameraLabelString = getString(R.string.cameraSelectedLabelText) + " " + binding.mainContainer.frontCamMaterialButton.text
            binding.mainContainer.cameraTextView.text = cameraLabelString
        }

        val maxSlider = 100
        val minSlider = 0
        val totalSlider = maxSlider - minSlider

        val smileSlider = binding.mainContainer.smileFluidSlider
        smileSlider.position = 0.5f
        smileSlider.startText ="Barely Smiling"
        smileSlider.endText = "Big Cheesy Smile"
        var smileSliderPosition = "${minSlider + (totalSlider  * smileSlider.position).toInt()}"
        var smileConfidenceString: String = getString(R.string.smileConfidenceLabelText) + " " + smileSliderPosition
        binding.mainContainer.smileConfidenceTextView.text = smileConfidenceString

        smileSlider.positionListener = {
            pos -> smileSliderPosition = "${minSlider + (totalSlider  * pos).toInt()}"
            smileConfidenceString = getString(R.string.smileConfidenceLabelText) + " " + smileSliderPosition
            binding.mainContainer.smileConfidenceTextView.text = smileConfidenceString
            smileSlider.bubbleText = smileSliderPosition
        }

        binding.mainContainer.smileOverlayButton.setOnClickListener {
            val intent = Intent(this, OpenSmileOverlayActivity::class.java)
            startActivity(intent)
        }

        binding.mainContainer.openShutterButton.setOnClickListener {
            val intent = Intent(this, OpenShutterActivity::class.java)
            val selectedCamera = if (binding.mainContainer.cameraButtonGroup.checkedButtonId == R.id.backCamMaterialButton) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            intent.putExtra(DELAY_LENGTH, delaySeekBar.progress)
            intent.putExtra(EXPOSURE_LENGTH, exposureSeekBar.progress)
            intent.putExtra(SELECTED_CAMERA, selectedCamera)
            intent.putExtra(SMILE_PERCENTAGE, smileSliderPosition.toInt())
            startActivity(intent)
        }

        binding.mainContainer.photoViewButton.setOnClickListener {
            if (imagesInGallery) {
                val intent = Intent(this, OpenGalleryActivity::class.java)
                startActivity(intent)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        setGalleryThumbnail()

        binding.root.postDelayed({
            hideSystemUI()
        }, IMMERSIVE_FLAG_TIMEOUT)
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
        return if (item.itemId == R.id.action_about_me) {

            AlertDialog.Builder(this, R.style.Theme_SayCheesePictureTaker_AlertDialogCustom)
                .setTitle(getString(R.string.about_title))
                .setMessage(getString(R.string.about_dialog))
                .setIcon(android.R.drawable.ic_menu_camera)
                .setNeutralButton(android.R.string.yes, null)
                .create().showImmersive()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
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
        val cursor: Cursor = contentResolver.query(
            uriExternal, projection, null,
            null, MediaStore.Images.ImageColumns.DATE_ADDED + " DESC"
        )!!

        if (cursor.moveToFirst()) {
            val columnIndexID = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val imageId: Long = cursor.getLong(columnIndexID)
            val imageURI = Uri.withAppendedPath(uriExternal, "" + imageId)

            imagesInGallery = true

            // Run the operations in the view's thread
            binding.mainContainer.photoViewButton.let { photoViewButton ->
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

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun checkAvailableCameras() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            binding.mainContainer.backCamMaterialButton.isClickable = hasBackCamera()
            if (!binding.mainContainer.backCamMaterialButton.isClickable) {
                binding.mainContainer.backCamMaterialButton.visibility = View.INVISIBLE
                binding.mainContainer.cameraButtonGroup.check(R.id.frontCamMaterialButton)
            }

            binding.mainContainer.frontCamMaterialButton.isClickable = hasFrontCamera()
            if (!binding.mainContainer.frontCamMaterialButton.isClickable) binding.mainContainer.frontCamMaterialButton.visibility = View.INVISIBLE

            if (!binding.mainContainer.backCamMaterialButton.isClickable && !binding.mainContainer.frontCamMaterialButton.isClickable) throw IllegalStateException("Back and front camera are unavailable")


        }, ContextCompat.getMainExecutor(this))
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}