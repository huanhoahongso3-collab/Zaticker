package dhp.thl.tpl.ntt

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import dhp.thl.tpl.ntt.databinding.ActivityMainBinding

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

        binding.addButton.setOnClickListener { pickImage.launch("image/*") }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importToMediaStore(it) }
    }

    private fun importToMediaStore(src: Uri) {
        val input = contentResolver.openInputStream(src) ?: return
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.DISPLAY_NAME, "zaticker_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Zaticker")
        }
        val dst = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        dst?.let {
            contentResolver.openOutputStream(it)?.use { out -> input.copyTo(out) }
            saveSticker(it)
            adapter.addSticker(it, toTop = true)
            Toast.makeText(this, "Imported!", Toast.LENGTH_SHORT).show()
        }
    }

    /** ✅ Safe save (keeps all stickers and preserves order) */
    private fun saveSticker(uri: Uri) {
        val existing = prefs.getStringSet("uris", emptySet()) ?: emptySet()
        val updated = existing.toMutableList().apply {
            remove(uri.toString()) // avoid duplicates
            add(uri.toString())    // newest last
        }
        prefs.edit().putStringSet("uris", updated.toSet()).apply()
    }

    /** ✅ Newest stickers shown first (top-left) */
    private fun loadStickers(): MutableList<Uri> {
        val saved = prefs.getStringSet("uris", emptySet()) ?: emptySet()
        return saved.map { Uri.parse(it) }.reversed().toMutableList()
    }

    /** ✅ Proper Zalo sticker intent (not photo share) */
    override fun onStickerClick(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("is_sticker", true)
            putExtra("type", 3)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.zing.zalo")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Zalo not installed", Toast.LENGTH_SHORT).show()
        }
    }

    /** ✅ Confirmation dialog before deletion */
    override fun onStickerLongClick(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Delete sticker?")
            .setMessage("Are you sure you want to delete this sticker?")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    contentResolver.delete(uri, null, null)
                    adapter.removeSticker(uri)
                    val saved = prefs.getStringSet("uris", emptySet()) ?: emptySet()
                    val updated = saved.toMutableSet().apply { remove(uri.toString()) }
                    prefs.edit().putStringSet("uris", updated).apply()
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
