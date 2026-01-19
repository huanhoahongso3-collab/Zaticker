package dhp.thl.tpl.ndv

import android.Manifest
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import dhp.thl.tpl.ntt.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(), StickerAdapter.StickerListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: StickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        [cite_start]binding = ActivityMainBinding.inflate(layoutInflater) [cite: 120]
        [cite_start]setContentView(binding.root) [cite: 120]

        // Load stickers from storage
        [cite_start]adapter = StickerAdapter(StickerAdapter.loadOrdered(this), this) [cite: 120]
        [cite_start]binding.recycler.layoutManager = GridLayoutManager(this, 3) [cite: 120]
        [cite_start]binding.recycler.adapter = adapter [cite: 120]

        // Request legacy storage permission for Android 9 and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            [cite_start]requestLegacyPermissions() [cite: 121]
        }

        [cite_start]binding.addButton.setOnClickListener { openSystemImagePicker() } [cite: 121]

        // Handle external share intents
        [cite_start]handleShareIntent(intent) [cite: 121]
    }

    private fun requestLegacyPermissions() {
        [cite_start]val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE [cite: 121]
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            [cite_start]ActivityCompat.requestPermissions(this, arrayOf(perm), 999) [cite: 121]
        }
    [cite_start]} [cite: 122]

    /** Allow picking multiple images */
    private fun openSystemImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        [cite_start]} [cite: 122]
        [cite_start]pickImages.launch(intent) [cite: 122]
    }

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            [cite_start]if (result.resultCode == RESULT_OK) { [cite: 123]
                val clipData: ClipData? [cite_start]= result.data?.clipData [cite: 124]
                val uri: Uri? [cite_start]= result.data?.data [cite: 125]

                when {
                    clipData != null -> {
                        for (i in 0 until clipData.itemCount) {
                            [cite_start]importToAppOrExternal(clipData.getItemAt(i).uri) [cite: 126]
                        }
                    }
                    [cite_start]uri != null -> importToAppOrExternal(uri) [cite: 126]
                    [cite_start]else -> Toast.makeText(this, getString(R.string.no_images_selected), Toast.LENGTH_SHORT).show() [cite: 126]
                [cite_start]} [cite: 127]
            }
        }

    /** Save based on API level */
    private fun importToAppOrExternal(src: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            [cite_start]importToAppData(src) [cite: 127]
        } else {
            [cite_start]importToExternalStorage(src) [cite: 127]
        [cite_start]} [cite: 128]
    }

    /** Scoped storage for Android 10+ */
    private fun importToAppData(src: Uri) {
        try {
            [cite_start]val input = contentResolver.openInputStream(src) ?: return [cite: 128]
            [cite_start]val name = "zaticker_${System.currentTimeMillis()}.png" [cite: 128]
            [cite_start]val file = File(filesDir, name) [cite: 128]
            [cite_start]FileOutputStream(file).use { out -> input.copyTo(out) } [cite: 128]
            
            [cite_start]val uri = Uri.fromFile(file) [cite: 129]
            [cite_start]adapter.addStickerAtTop(this, uri) [cite: 129]
            [cite_start]binding.recycler.scrollToPosition(0) [cite: 129]
        } catch (e: Exception) {
            [cite_start]Toast.makeText(this, getString(R.string.import_failed, e.message), Toast.LENGTH_SHORT).show() [cite: 129]
        }
    }

    /** Legacy storage for Android 9 and below */
    private fun importToExternalStorage(src: Uri) {
        [cite_start]try { [cite: 130]
            [cite_start]val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) [cite: 130]
            [cite_start]val folder = File(baseDir, "Zaticker") [cite: 130]
            [cite_start]if (!folder.exists()) folder.mkdirs() [cite: 130]

            [cite_start]val input = contentResolver.openInputStream(src) ?: return [cite: 130]
            [cite_start]val name = "zaticker_${System.currentTimeMillis()}.png" [cite: 130]
            [cite_start]val file = File(folder, name) [cite: 130]
            [cite_start]FileOutputStream(file).use { out -> input.copyTo(out) } [cite: 131]

            [cite_start]val uri = Uri.fromFile(file) [cite: 131]
            [cite_start]adapter.addStickerAtTop(this, uri) [cite: 131]
            [cite_start]binding.recycler.scrollToPosition(0) [cite: 131]
        } catch (e: Exception) {
            [cite_start]Toast.makeText(this, getString(R.string.legacy_import_failed, e.message), Toast.LENGTH_SHORT).show() [cite: 131]
        }
    }

    [cite_start]/** Share sticker to Zalo */ [cite: 131, 132]
    override fun onStickerClick(uri: Uri) {
        try {
            [cite_start]val file = File(uri.path!!) [cite: 132]
            [cite_start]val contentUri = FileProvider.getUriForFile(this, "$packageName.provider", file) [cite: 132]

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                [cite_start]putExtra(Intent.EXTRA_STREAM, contentUri) [cite: 133]
                [cite_start]addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) [cite: 133]
                [cite_start]putExtra("is_sticker", true) [cite: 133]
                [cite_start]putExtra("type", 3) [cite: 133]
                [cite_start]setClassName("com.zing.zalo", "com.zing.zalo.ui.TempShareViaActivity") [cite: 133]
            }

            [cite_start]startActivity(intent) [cite: 133]
        [cite_start]} catch (e: Exception) { [cite: 134]
            [cite_start]Toast.makeText(this, getString(R.string.zalo_share_failed), Toast.LENGTH_SHORT).show() [cite: 134]
        }
    }

    /** Long press: 3-option dialog (Export / Delete / Cancel) */
    override fun onStickerLongClick(uri: Uri) {
        AlertDialog.Builder(this)
            [cite_start].setTitle(getString(R.string.sticker_options_title)) [cite: 134]
            [cite_start].setMessage(getString(R.string.sticker_options_message)) [cite: 134]
            [cite_start].setPositiveButton(getString(R.string.export)) { _, _ -> exportSticker(uri) } [cite: 134, 135]
            [cite_start].setNegativeButton(getString(R.string.delete)) { _, _ -> deleteSticker(uri) } [cite: 135]
            [cite_start].setNeutralButton(getString(R.string.cancel), null) [cite: 135]
            [cite_start].show() [cite: 135]
    }

    /** Delete sticker immediately */
    private fun deleteSticker(uri: Uri) {
        try {
            [cite_start]val file = File(uri.path ?: "") [cite: 135]
            [cite_start]if (file.exists()) file.delete() [cite: 136]
            [cite_start]adapter.removeSticker(this, uri) [cite: 136]
            [cite_start]Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show() [cite: 136]
        } catch (e: Exception) {
            [cite_start]Toast.makeText(this, getString(R.string.delete_failed), Toast.LENGTH_SHORT).show() [cite: 136]
        }
    }

    /** Export sticker to Pictures/Zaticker folder */
    private fun exportSticker(uri: Uri) {
        try {
            [cite_start]val input = contentResolver.openInputStream(uri) ?: return [cite: 137]
            [cite_start]val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) [cite: 137]
            [cite_start]val folder = File(baseDir, "Zaticker") [cite: 137]
            [cite_start]if (!folder.exists()) folder.mkdirs() [cite: 137]

            [cite_start]val name = "zaticker_export_${System.currentTimeMillis()}.png" [cite: 137]
            [cite_start]val file = File(folder, name) [cite: 137]
            [cite_start]FileOutputStream(file).use { out -> input.copyTo(out) } [cite: 138]

            [cite_start]Toast.makeText(this, getString(R.string.sticker_exported, file.absolutePath), Toast.LENGTH_LONG).show() [cite: 138]
        } catch (e: Exception) {
            [cite_start]Toast.makeText(this, getString(R.string.export_failed, e.message), Toast.LENGTH_SHORT).show() [cite: 138]
        }
    }

    /** Import stickers via external share intents */
    private fun handleShareIntent(intent: Intent?) {
        [cite_start]if (intent == null) return [cite: 138]

        [cite_start]when (intent.action) { [cite: 139]
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                [cite_start]if (uri != null) importToAppOrExternal(uri) [cite: 139]
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                [cite_start]val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) [cite: 139, 140]
                [cite_start]uris?.forEach { importToAppOrExternal(it) } [cite: 140]
            }
        }
    }
}
