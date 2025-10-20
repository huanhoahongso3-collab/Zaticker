package dhp.thl.tpl.ntt

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import dhp.thl.tpl.ntt.databinding.ActivityMainBinding
import java.io.File

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
            adapter.addSticker(it)
            Toast.makeText(this, "Imported!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSticker(uri: Uri) {
        val set = prefs.getStringSet("uris", mutableSetOf()) ?: mutableSetOf()
        set.add(uri.toString())
        prefs.edit().putStringSet("uris", set).apply()
    }

    private fun loadStickers(): MutableList<Uri> {
        val set = prefs.getStringSet("uris", emptySet()) ?: emptySet()
        return set.map { Uri.parse(it) }.toMutableList()
    }

    override fun onStickerClick(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("type", 3)
            putExtra("is_sticker", true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
            addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP)
            setClassName("com.zing.zalo", "com.zing.zalo.ui.TempShareViaActivity")
        }

        try {
            // Grant URI permission to Zalo
            val resInfoList = packageManager.queryIntentActivities(intent, 0)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Debugging log: compare to adb intent
            Log.d("ZaloIntent", intent.toUri(Intent.URI_INTENT_SCHEME))

            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Zalo not installed or sharing failed", Toast.LENGTH_SHORT).show()

            // fallback to normal share chooser if Zalo not found
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(fallbackIntent, "Share Image"))
            } catch (ignored: Exception) {
                Toast.makeText(this, "No app can handle sharing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStickerLongClick(uri: Uri) {
        try {
            contentResolver.delete(uri, null, null)
            adapter.removeSticker(uri)
            val set = prefs.getStringSet("uris", mutableSetOf())?.toMutableSet()
            set?.remove(uri.toString())
            prefs.edit().putStringSet("uris", set).apply()
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
        }
    }
}
