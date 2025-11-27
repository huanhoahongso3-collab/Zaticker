package dhp.thl.tpl.ntt

import android.app.AlertDialog
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = StickerAdapter(StickerAdapter.loadOrdered(this), this)
        binding.recycler.layoutManager = GridLayoutManager(this, 3)
        binding.recycler.adapter = adapter

        binding.addButton.setOnClickListener { openSystemImagePicker() }

        handleShareIntent(intent)
    }

    /** Pick multiple images from gallery */
    private fun openSystemImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        pickImages.launch(intent)
    }

    private val pickImages = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val clipData = result.data?.clipData
            val uri = result.data?.data

            when {
                clipData != null -> {
                    for (i in 0 until clipData.itemCount) {
                        importToAppData(clipData.getItemAt(i).uri)
                    }
                }
                uri != null -> importToAppData(uri)
                else -> Toast.makeText(this, getString(R.string.no_images_selected), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Import and save image into app storage */
    private fun importToAppData(src: Uri) {
        try {
            val input = contentResolver.openInputStream(src) ?: return
            val name = "${getString(R.string.file_prefix)}${System.currentTimeMillis()}.png"
            val file = File(filesDir, name)
            FileOutputStream(file).use { out -> input.copyTo(out) }

            val uri = Uri.fromFile(file)
            adapter.addStickerAtTop(this, uri)
            binding.recycler.scrollToPosition(0)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }

    /** Share image directly to Zalo */
    private fun shareDirectToZalo(src: Uri) {
        try {
            val input = contentResolver.openInputStream(src) ?: return
            val tempFile = File(cacheDir, "temp_zshare_${System.currentTimeMillis()}.png")
            FileOutputStream(tempFile).use { out -> input.copyTo(out) }

            val contentUri = FileProvider.getUriForFile(this, "$packageName.provider", tempFile)
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
            Toast.makeText(this, getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
        }
    }

    /** Sticker click → share to Zalo */
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
            Toast.makeText(this, getString(R.string.zalo_not_installed), Toast.LENGTH_SHORT).show()
        }
    }

    /** Long press → delete sticker */
    override fun onStickerLongClick(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_sticker))
            .setMessage(getString(R.string.delete_sticker_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                try {
                    val file = File(uri.path ?: "")
                    if (file.exists()) file.delete()
                    adapter.removeSticker(this, uri)
                    Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, getString(R.string.delete_failed), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /** Handle incoming share intents */
    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {

            // Single image → show 3 share targets
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return
                val options = arrayOf(
                    getString(R.string.share_import),
                    getString(R.string.share_quick_zalo),
                    getString(R.string.share_import_and_zalo)
                )

                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.choose_action))
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> importToAppData(uri)
                            1 -> shareDirectToZalo(uri)
                            2 -> {
                                importToAppData(uri)
                                shareDirectToZalo(uri)
                            }
                        }
                    }.show()
            }

            // Multiple images → import only
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (!uris.isNullOrEmpty()) {
                    uris.forEach { importToAppData(it) }
                }
            }
        }
    }
}
