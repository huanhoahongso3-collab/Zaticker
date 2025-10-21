package dhp.thl.tpl.ntt

import android.app.AlertDialog
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import dhp.thl.tpl.ntt.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(), StickerAdapter.StickerListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: StickerAdapter
    private val prefs by lazy { getSharedPreferences("stickers", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = StickerAdapter(loadStickers(), this)
        binding.recycler.layoutManager = GridLayoutManager(this, 3)
        binding.recycler.adapter = adapter

        binding.addButton.setOnClickListener { openSystemImagePicker() }

        // ✅ Handle import when opened via "Share → Zaticker"
        handleShareIntent(intent)
    }

    /** ✅ Allow picking multiple images from gallery */
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
                            importToAppData(clipData.getItemAt(i).uri)
                        }
                    }
                    uri != null -> importToAppData(uri)
                    else -> Toast.makeText(this, "No images selected", Toast.LENGTH_SHORT).show()
                }
            }
        }

    /** ✅ Import and save image into app-private files */
    private fun importToAppData(src: Uri) {
        try {
            val input = contentResolver.openInputStream(src) ?: return
            val name = "zaticker_${System.currentTimeMillis()}.png"
            val file = File(filesDir, name)
            FileOutputStream(file).use { out -> input.copyTo(out) }

            val uri = Uri.fromFile(file)

            // ✅ Save and refresh
            saveSticker(uri)
            adapter.addStickerAtTop(uri)
            binding.recycler.scrollToPosition(0)
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** ✅ Save sticker list in SharedPreferences */
    private fun saveSticker(uri: Uri) {
        val set = prefs.getStringSet("uris", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        val newList = mutableListOf(uri.toString())
        newList.addAll(set)
        prefs.edit().putStringSet("uris", newList.toSet()).apply()
    }

    /** ✅ Load saved stickers (newest first) */
    private fun loadStickers(): MutableList<Uri> {
        val set = prefs.getStringSet("uris", emptySet()) ?: emptySet()
        return set.map { Uri.parse(it) }.toMutableList()
    }

    /** ✅ Share sticker to Zalo (real sticker intent) */
    override fun onStickerClick(uri: Uri) {
        try {
            val file = File(uri.path!!)
            val contentUri = FileProvider.getUriForFile(this, "$packageName.provider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Only real sticker keys
                putExtra("is_sticker", true)
                putExtra("type", 3)

                setClassName("com.zing.zalo", "com.zing.zalo.ui.TempShareViaActivity")
            }

            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Zalo not installed or share failed", Toast.LENGTH_SHORT).show()
        }
    }

    /** ✅ Delete sticker and remove file from /data */
    override fun onStickerLongClick(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Delete Sticker")
            .setMessage("Do you want to delete this sticker?")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    val file = File(uri.path ?: "")
                    if (file.exists()) file.delete()

                    adapter.removeSticker(uri)
                    val set = prefs.getStringSet("uris", mutableSetOf())?.toMutableSet()
                    set?.remove(uri.toString())
                    prefs.edit().putStringSet("uris", set).apply()

                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** ✅ Handle share intents (import shared stickers) */
    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) importToAppData(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (!uris.isNullOrEmpty()) {
                    for (uri in uris) importToAppData(uri)
                }
            }
        }
    }
}
