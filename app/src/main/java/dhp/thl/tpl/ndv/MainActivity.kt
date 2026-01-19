package dhp.thl.tpl.ndv // ✅ Updated Package

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
import com.google.android.material.color.DynamicColors
import dhp.thl.tpl.ndv.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(), StickerAdapter.StickerListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: StickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Apply Material 3 Dynamic Colors before super.onCreate
        DynamicColors.applyToActivityIfAvailable(this)
        
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater) [cite: 120]
        setContentView(binding.root)

        // Load stickers and setup UI 
        adapter = StickerAdapter(StickerAdapter.loadOrdered(this), this)
        binding.recycler.layoutManager = GridLayoutManager(this, 3)
        binding.recycler.adapter = adapter

        // Legacy permissions check [cite: 121]
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestLegacyPermissions()
        }

        binding.addButton.setOnClickListener { openSystemImagePicker() } [cite: 121]

        handleShareIntent(intent) [cite: 121]
    }

    private fun requestLegacyPermissions() {
        val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), 999)
        }
    } [cite: 122]

    private fun openSystemImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickImages.launch(intent)
    } [cite: 122]

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) { [cite: 123]
                val clipData: ClipData? = result.data?.clipData [cite: 124]
                val uri: Uri? = result.data?.data [cite: 125]

                when {
                    clipData != null -> {
                        for (i in 0 until clipData.itemCount) {
                            importToAppOrExternal(clipData.getItemAt(i).uri) [cite: 126]
                        }
                    }
                    uri != null -> importToAppOrExternal(uri) [cite: 126]
                    else -> Toast.makeText(this, getString(R.string.no_images_selected), Toast.LENGTH_SHORT).show() [cite: 126]
                }
            }
        } [cite: 127]

    private fun importToAppOrExternal(src: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            importToAppData(src) [cite: 127]
        } else {
            importToExternalStorage(src) [cite: 127]
        }
    } [cite: 128]

    private fun importToAppData(src: Uri) {
        try {
            val input = contentResolver.openInputStream(src) ?: return [cite: 128]
            val name = "zaticker_${System.currentTimeMillis()}.png" [cite: 128]
            val file = File(filesDir, name) [cite: 128]
            FileOutputStream(file).use { out -> input.copyTo(out) } [cite: 128]
            val uri = Uri.fromFile(file) [cite: 129]

            adapter.addStickerAtTop(this, uri) [cite: 129]
            binding.recycler.scrollToPosition(0) [cite: 129]
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_failed, e.message), Toast.LENGTH_SHORT).show() [cite: 129]
        }
    }

    private fun importToExternalStorage(src: Uri) {
        try { [cite: 130]
            val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) [cite: 130]
            val folder = File(baseDir, "Zaticker") [cite: 130]
            if (!folder.exists()) folder.mkdirs() [cite: 130]

            val input = contentResolver.openInputStream(src) ?: return [cite: 130]
            val name = "zaticker_${System.currentTimeMillis()}.png" [cite: 130]
            val file = File(folder, name) [cite: 130]
            FileOutputStream(file).use { out -> input.copyTo(out) } [cite: 131]

            val uri = Uri.fromFile(file) [cite: 131]
            adapter.addStickerAtTop(this, uri) [cite: 131]
            binding.recycler.scrollToPosition(0) [cite: 131]
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.legacy_import_failed, e.message), Toast.LENGTH_SHORT).show() [cite: 131]
        }
    }

    override fun onStickerClick(uri: Uri) {
        try { [cite: 132]
            val file = File(uri.path!!) [cite: 132]
            // ✅ Updated authority for new package ID
            val contentUri = FileProvider.getUriForFile(this, "dhp.thl.tpl.ndv.provider", file) [cite: 132]

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri) [cite: 133]
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) [cite: 133]
                putExtra("is_sticker", true) [cite: 133]
                putExtra("type", 3) [cite: 133]
                setClassName("com.zing.zalo", "com.zing.zalo.ui.TempShareViaActivity") [cite: 133]
            }
            startActivity(intent) [cite: 133]
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.zalo_share_failed), Toast.LENGTH_SHORT).show() [cite: 134]
        }
    }

    override fun onStickerLongClick(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.sticker_options_title)) [cite: 134]
            .setMessage(getString(R.string.sticker_options_message)) [cite: 134]
            .setPositiveButton(getString(R.string.export)) { _, _ -> exportSticker(uri) } [cite: 135]
            .setNegativeButton(getString(R.string.delete)) { _, _ -> deleteSticker(uri) } [cite: 135]
            .setNeutralButton(getString(R.string.cancel), null) [cite: 135]
            .show()
    }

    private fun deleteSticker(uri: Uri) {
        try {
            val file = File(uri.path ?: "") [cite: 135]
            if (file.exists()) file.delete() [cite: 136]
            adapter.removeSticker(this, uri) [cite: 136]
            Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show() [cite: 136]
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.delete_failed), Toast.LENGTH_SHORT).show() [cite: 136]
        }
    }

    private fun exportSticker(uri: Uri) {
        try { [cite: 137]
            val input = contentResolver.openInputStream(uri) ?: return [cite: 137]
            val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) [cite: 137]
            val folder = File(baseDir, "Zaticker") [cite: 137]
            if (!folder.exists()) folder.mkdirs() [cite: 137]

            val name = "zaticker_export_${System.currentTimeMillis()}.png" [cite: 137]
            val file = File(folder, name) [cite: 137]
            FileOutputStream(file).use { out -> input.copyTo(out) } [cite: 138]

            Toast.makeText(this, getString(R.string.sticker_exported, file.absolutePath), Toast.LENGTH_LONG).show() [cite: 138]
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.export_failed, e.message), Toast.LENGTH_SHORT).show() [cite: 138]
        }
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return [cite: 139]
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) importToAppOrExternal(uri) [cite: 139]
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) [cite: 140]
                uris?.forEach { importToAppOrExternal(it) } [cite: 140]
            }
        }
    }
}
