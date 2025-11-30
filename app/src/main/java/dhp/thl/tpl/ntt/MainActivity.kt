package dhp.thl.tpl.ntt

import android.Manifest
import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dhp.thl.tpl.ntt.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(), StickerAdapter.StickerListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: StickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load stickers
        adapter = StickerAdapter(StickerAdapter.loadOrdered(this), this)
        binding.recycler.layoutManager = GridLayoutManager(this, 3)
        binding.recycler.adapter = adapter

        // Request legacy storage for Android 9 and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestLegacyPermissions()
        }

        binding.addButton.setOnClickListener { openSystemImagePicker() }

        // Handle external share intents
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

    private val pickImages = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val clipData: ClipData? = result.data?.clipData
            val uri: Uri? = result.data?.data
            when {
                clipData != null -> {
                    for (i in 0 until clipData.itemCount) {
                        importToAppOrExternal(clipData.getItemAt(i).uri)
                    }
                }
                uri != null -> importToAppOrExternal(uri)
                else -> Toast.makeText(this, getString(R.string.no_images_selected), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importToAppOrExternal(src: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            importToAppData(src)
        } else {
            importToExternalStorage(src)
        }
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

    override fun onStickerClick(uri: Uri) {
        try {
            val file = File(uri.path!!)
            val contentUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra("is_sticker", true)
                putExtra("type", 3)
                setClassName("com.zing.zalo", "com.zing.zalo.ui.TempShareViaActivity")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.zalo_share_failed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStickerLongClick(uri: Uri) {
        val options = arrayOf(
            getString(R.string.export),
            getString(R.string.delete),
            getString(R.string.rb_option),
            getString(R.string.cancel)
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.sticker_options_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportSticker(uri)
                    1 -> deleteSticker(uri)
                    2 -> removeBackground(uri)
                    3 -> {} // Cancel
                }
            }
            .show()
    }

    private fun deleteSticker(uri: Uri) {
        try {
            val file = File(uri.path ?: "")
            if (file.exists()) file.delete()
            adapter.removeSticker(this, uri)
            Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.delete_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportSticker(uri: Uri) {
        try {
            val input = contentResolver.openInputStream(uri) ?: return
            val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val folder = File(baseDir, "Zaticker")
            if (!folder.exists()) folder.mkdirs()
            val name = "zaticker_export_${System.currentTimeMillis()}.png"
            val file = File(folder, name)
            FileOutputStream(file).use { out -> input.copyTo(out) }
            Toast.makeText(this, getString(R.string.sticker_exported, file.absolutePath), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.export_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) importToAppOrExternal(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                uris?.forEach { importToAppOrExternal(it) }
            }
        }
    }

    // ================= RB Function ==================
    private fun removeBackground(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        BackgroundRemovalTask(uri).execute()
    }

    private inner class BackgroundRemovalTask(private val uri: Uri) : AsyncTask<Void, Void, Uri?>() {

        private var errorMessage: String? = null

        override fun doInBackground(vararg params: Void?): Uri? {
            return try {
                val inputStream = contentResolver.openInputStream(uri) ?: return null
                val url = URL("https://briarmbg20.vercel.app/api/rmbg")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/octet-stream")

                connection.outputStream.use { out: OutputStream ->
                    inputStream.copyTo(out)
                }

                if (connection.responseCode == 200) {
                    val name = "zaticker_rb_${System.currentTimeMillis()}.png"
                    val file = File(filesDir, name)
                    connection.inputStream.use { input ->
                        FileOutputStream(file).use { out -> input.copyTo(out) }
                    }
                    Uri.fromFile(file)
                } else {
                    errorMessage = "HTTP ${connection.responseCode}"
                    null
                }
            } catch (e: Exception) {
                errorMessage = e.message
                null
            }
        }

        override fun onPostExecute(result: Uri?) {
            binding.progressBar.visibility = View.GONE
            if (result != null) {
                adapter.addStickerAtTop(this@MainActivity, result)
                binding.recycler.scrollToPosition(0)
                Toast.makeText(this@MainActivity, getString(R.string.rb_success), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.rb_failed, errorMessage), Toast.LENGTH_LONG).show()
            }
        }
    }
}

