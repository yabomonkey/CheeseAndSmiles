package yabomonkey.example.saycheesepicturetaker

import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import yabomonkey.example.saycheesepicturetaker.databinding.ActivityGalleryBinding
import yabomonkey.example.saycheesepicturetaker.utils.padWithDisplayCutout
import yabomonkey.example.saycheesepicturetaker.utils.showImmersive
import java.io.File
import java.util.*

private const val TAG = "OpenGalleryActivity"

class OpenGalleryActivity : AppCompatActivity() {

    /** Adapter class used to present a fragment containing one photo or video as a page */
    inner class MediaPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int = mediaList.size
        override fun getItem(position: Int): Fragment = PhotoFragment.create(mediaList[position])
        override fun getItemPosition(obj: Any): Int = POSITION_NONE
    }

    private lateinit var viewBinding: ActivityGalleryBinding

    private var mediaList: MutableList<File> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.Media._ID,
            MediaStore.Images.ImageColumns.DATE_ADDED,
            MediaStore.Images.ImageColumns.MIME_TYPE,
            MediaStore.Images.Media.DATA
        )

        contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
            null, MediaStore.Images.ImageColumns.DATE_ADDED + " DESC"
        )!!.use {
            while (it.moveToNext()) {
                mediaList.add(it.position, File(it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))))
            }
        }

        viewBinding.backButton.setOnClickListener {
            finish()
        }

        viewBinding.shareButton.setOnClickListener {
            mediaList.getOrNull(viewBinding.photoViewPager.currentItem)?.let { mediaFile ->

                // Create a sharing intent
                val intent = Intent().apply {
                    // Infer media type from file extension
                    val mediaType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(mediaFile.extension)
                    // Get URI from our FileProvider implementation
                    val uri = FileProvider.getUriForFile(
                        applicationContext, BuildConfig.APPLICATION_ID + ".provider", mediaFile)
                    // Set the appropriate intent extra, type, action and flags
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = mediaType
                    action = Intent.ACTION_SEND
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                // Launch the intent letting the user choose which app to share with
                startActivity(Intent.createChooser(intent, getString(R.string.share_hint)))
            }
        }

        // Handle delete button press
        viewBinding.deleteButton.setOnClickListener {

            mediaList.getOrNull(viewBinding.photoViewPager.currentItem)?.let { mediaFile ->

                AlertDialog.Builder(this, R.style.Theme_SayCheesePictureTaker_AlertDialogCustom)
                    .setTitle(getString(R.string.delete_title))
                    .setMessage(getString(R.string.delete_dialog))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes) { _, _ ->

                        // Delete current photo
                        mediaFile.delete()

                        // Send relevant broadcast to notify other apps of deletion
                        MediaScannerConnection.scanFile(
                            applicationContext, arrayOf(mediaFile.absolutePath), null, null)

                        // Notify our view pager
                        mediaList.removeAt(viewBinding.photoViewPager.currentItem)
                        viewBinding.photoViewPager.adapter?.notifyDataSetChanged()

                        // If all photos have been deleted, return to camera
                        if (mediaList.isEmpty()) {
                            finish()
                        }

                    }

                    .setNegativeButton(android.R.string.no, null)
                    .create().showImmersive()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (mediaList.isEmpty()) {
            viewBinding.deleteButton.isEnabled = false
            viewBinding.shareButton.isEnabled = false
            Log.d(TAG, "The mediaList is empty")
        }

        viewBinding.photoViewPager.apply {
            offscreenPageLimit = 2
            adapter = MediaPagerAdapter(supportFragmentManager)
        }

        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            viewBinding.cutoutSafeArea.padWithDisplayCutout()
        }
    }
}