package dhp.thl.tpl.ntt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return

        val shareType = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
            // Detect which share target by meta-data
            intent.metaDataShareType() ?: "IMPORT"
        } ?: "IMPORT"

        val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        if (uri != null) {
            when (shareType) {
                "IMPORT" -> importToAppData(uri)
                "QUICK_ZALO" -> shareDirectToZalo(uri)
                "IMPORT_AND_ZALO" -> {
                    importToAppData(uri)
                    shareDirectToZalo(uri)
                }
            }
        }
    }

    /** Extension function to read meta-data from intent */
    private fun Intent.metaDataShareType(): String? {
        val component = resolveActivity(packageManager) ?: return null
        val info = packageManager.getActivityInfo(component, 128)
        val metaData = info.metaData
        return metaData?.getString("EXTRA_SHARE_TYPE")
    }

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
            Toast.makeText(
                this,
                getString(R.string.import_failed, e.message ?: ""),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun shareDirectToZalo(src: Uri) {
        try {
            val input = contentResolver.openInputStream(src) ?: return
            val temp = File(cacheDir, "temp_zshare_${System.currentTimeMillis()}.png")
            FileOutputStream(temp).use { out -> input.copyTo(out) }

            val contentUri = FileProvider.getUriForFile(this, "$packageName.provider", temp)
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

    override fun onStickerClick(uri: Uri) {
        shareDirectToZalo(uri)
    }

    override fun onStickerLongClick(uri: Uri) {
        adapter.removeSticker(this, uri)
    }

    private fun openSystemImagePicker() {
        // Keep your existing multi-image picker logic
    }
}
