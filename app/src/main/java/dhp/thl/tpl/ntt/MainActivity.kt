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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.core.content.FileProvider
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
    }

    /** ✅ Open system picker (multi-select) */
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

    /** ✅ Store inside private app storage (/data/data/.../files) */
    private fun importToAppData(src: Uri) {
        try {
            val input = contentResolver.openInputStream(src) ?: return
            val name = "zaticker_${System.currentTimeMillis()}.png"
            val file = File(filesDir, name)
            FileOutputStream(file).use { out -> input.copyTo(out) }

            val uri = Uri.fromFile(file)
            saveSticker(uri)
            adapter.addSticker(uri)
            Toast.makeText(this, "Imported!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /** ✅ Persist sticker URIs */
    private fun saveSticker(uri: Uri) {
        val set = prefs.getStringSet("uris", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set.add(uri.toString())
        prefs.edit().putStringSet("uris", set).apply()
    }

    /** ✅ Load stickers (newest first) */
    private fun loadStickers(): MutableList<Uri> {
        val set = prefs.getStringSet("uris", emptySet()) ?: emptySet()
        return set.map { Uri.parse(it) }
            .sortedByDescending { File(it.path ?: "").lastModified() }
            .toMutableList()
    }

    /** ✅ Share as Zalo sticker using FileProvider */
    override fun onStickerClick(uri: Uri) {
        val file = File(uri.path ?: return)
        val contentUri = FileProvider.getUriForFile(this, "$packageName.provider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            setPackage("com.zing.zalo")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("is_sticker", true)
            putExtra("type", 3)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Zalo not installed", Toast.LENGTH_SHORT).show()
        }
    }

    /** ✅ Confirm delete */
    override fun onStickerLongClick(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Delete Sticker")
            .setMessage("Do you want to delete this sticker?")
            .setPositiveButton("Delete") { _, _ ->
                try {
                    File(uri.path ?: "").delete()
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
}
