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
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import dhp.thl.tpl.ntt.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(), StickerAdapter.StickerListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: StickerAdapter
    private val client = OkHttpClient()
    private val apiUrl = "https://briarmbg20.vercel.app/api/rmbg"

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
                    clipData != null -> (0 until clipData.itemCount).forEach {
                        importToAppOrExternal(clipData.getItemAt(it).uri)
                    }
                    uri != null -> importToAppOrExternal(uri)
                    else -> Toast.makeText(this, getString(R.string.no_images_selected), Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun importToAppOrExternal(src: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) importToAppData(src)
        else importToExternalStorage(src)
    }

    private fun importToAppData(src: Uri) {
        try {
            val input = contentResolver.openInputStream(src) ?: return
            val name = "zaticker_${System.currentTimeMillis()}.png"
            val file = File(filesDir, name)
            FileOutputStream(file).use { out -> input.copyTo(out) }
            adapter.addStickerAtTop(this, Uri.fromFile(file))
            binding.recycler.scrollToPosition(0)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private fun importToExternalStorage(src: Uri) {
        try {
            val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val folder = File(baseDir, "Zaticker").apply { if (!exists()) mkdirs() }
            val input = contentResolver.openInputStream(src) ?: return
            val name = "zaticker_${System.currentTimeMillis()}.png"
            val file = File(folder, name)
            FileOutputStream(file).use { out -> input.copyTo(out) }
            adapter.addStickerAtTop(this, Uri.fromFile(file))
            binding.recycler.scrollToPosition(0)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.legacy_import_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStickerClick(uri: Uri) {
        try {
            val file = File(uri.path!!)
            val contentUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
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
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.sticker_options_title))
            .setMessage(getString(R.string.sticker_options_message))
            .setPositiveButton(getString(R.string.rb_option)) { _, _ -> removeBackground(uri) }
            .setNegativeButton(getString(R.string.delete)) { _, _ -> deleteSticker(uri) }
            .setNeutralButton(getString(R.string.cancel), null)
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
            val folder = File(baseDir, "Zaticker").apply { if (!exists()) mkdirs() }
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
            Intent.ACTION_SEND -> intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { importToAppOrExternal(it) }
            Intent.ACTION_SEND_MULTIPLE -> intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.forEach { importToAppOrExternal(it) }
        }
    }

    /** --------------------- Background Removal --------------------- */

    private fun removeBackground(uri: Uri) {
        try {
            val file = File(uri.path ?: return)
            binding.progressBar.visibility = View.VISIBLE

            lifecycleScope.launch {
                val success = callRemoveBgNewApi(file)
                binding.progressBar.visibility = View.GONE

                if (success) {
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this@MainActivity, getString(R.string.rb_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, getString(R.string.rb_failed, "API lỗi"), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, getString(R.string.rb_failed, e.message), Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun callRemoveBgNewApi(imageFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBody = imageFile.asRequestBody("application/octet-stream".toMediaType())
            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val bytes = response.body?.bytes() ?: return@withContext false
                imageFile.writeBytes(bytes)
                return@withContext true
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext false
        }
    }
}

