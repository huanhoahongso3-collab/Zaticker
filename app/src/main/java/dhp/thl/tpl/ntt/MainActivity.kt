package dhp.thl.tpl.ntt

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
import androidx.recyclerview.widget.GridLayoutManager
import dhp.thl.tpl.ntt.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(), StickerAdapter.StickerListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: StickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = StickerAdapter(StickerAdapter.loadOrdered(this), this)
        binding.recycler.layoutManager = GridLayoutManager(this, 3)
        binding.recycler.adapter = adapter

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) requestLegacyPermissions()

        binding.addButton.setOnClickListener { openSystemImagePicker() }

        handleShareIntent(intent)
    }

    private fun requestLegacyPermissions() {
        val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), 999)
        }
    }

    private fun openSystemImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickImages.launch(intent)
    }

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val clipData: ClipData? = result.data?.clipData
                val uri: Uri? = result.data?.data

                when {
                    clipData != null -> {
                        for (i in 0 until clipData.itemCount) {
                            importToAppOrExternal(clipData.getItemAt(i).uri, askRB = false)
                        }
                    }
                    uri != null -> promptRemoveBackground(uri)
                    else -> Toast.makeText(this, getString(R.string.no_images_selected), Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun promptRemoveBackground(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rb_prompt_title))
            .setMessage(getString(R.string.rb_prompt_message))
            .setPositiveButton(getString(R.string.rb_yes)) { _, _ -> removeBGAndImport(uri) }
            .setNegativeButton(getString(R.string.rb_no)) { _, _ -> importToAppOrExternal(uri, askRB = false) }
            .show()
    }

    private fun importToAppOrExternal(src: Uri, askRB: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) importToAppData(src)
        else importToExternalStorage(src)
    }

    private fun importToAppData(src: Uri) {
        try {
            val input = contentResolver.openInputStream(src) ?: return
            val name = "zaticker_${System.currentTimeMillis()}.png"
            val file = File(filesDir, name)
            FileOutputStream(file).use { out -> input.copyTo(out) }
            val uri = Uri.fromFile(file)

            adapter.addStickerAtTop(this, uri)
            binding.recycler.scrollToPosition(0)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun importToExternalStorage(src: Uri) {
        try {
            val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val folder = File(baseDir, "Zaticker")
            if (!folder.exists()) folder.mkdirs()

            val input = contentResolver.openInputStream(src) ?: return
            val name = "zaticker_${System.currentTimeMillis()}.png"
            val file = File(folder, name)
            FileOutputStream(file).use { out -> input.copyTo(out) }

            val uri = Uri.fromFile(file)

            adapter.addStickerAtTop(this, uri)
            binding.recycler.scrollToPosition(0)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.legacy_import_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeBGAndImport(src: Uri) {
        Thread {
            try {
                val input = contentResolver.openInputStream(src) ?: return@Thread
                val url = URL("https://briarmbg20.vercel.app/api/rmbg")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/octet-stream")
                conn.doOutput = true

                conn.outputStream.use { output -> input.copyTo(output) }

                val outputFile = File(filesDir, "zaticker_rb_${System.currentTimeMillis()}.png")
                conn.inputStream.use { inputNet -> FileOutputStream(outputFile).use { out -> inputNet.copyTo(out) } }

                runOnUiThread {
                    val uri = Uri.fromFile(outputFile)
                    adapter.addStickerAtTop(this, uri)
                    binding.recycler.scrollToPosition(0)
                    Toast.makeText(this, getString(R.string.rb_success), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.rb_failed, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onStickerClick(uri: Uri) {
        // unchanged
    }

    override fun onStickerLongClick(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.sticker_options_title))
            .setMessage(getString(R.string.sticker_options_message))
            .setPositiveButton(getString(R.string.export)) { _, _ -> exportSticker(uri) }
            .setNegativeButton(getString(R.string.delete)) { _, _ -> deleteSticker(uri) }
            .setNeutralButton(getString(R.string.rb_option)) { _, _ -> removeBGAndReimport(uri) }
            .show()
    }

    private fun removeBGAndReimport(uri: Uri) {
        removeBGAndImport(uri)
    }

    private fun deleteSticker(uri: Uri) {
        // unchanged
    }

    private fun exportSticker(uri: Uri) {
        // unchanged
    }

    private fun handleShareIntent(intent: Intent?) {
        // unchanged
    }
}
