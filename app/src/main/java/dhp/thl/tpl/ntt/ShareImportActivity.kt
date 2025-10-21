package dhp.thl.tpl.ntt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

/**
 * Handles "Share to Zaticker" intents (image/*).
 * Supports single or multiple images.
 */
class ShareImportActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("stickers", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            when (intent?.action) {
                Intent.ACTION_SEND -> {
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (uri != null) importSticker(uri)
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    if (!uris.isNullOrEmpty()) uris.forEach { importSticker(it) }
                }
                else -> Toast.makeText(this, "Unsupported share type", Toast.LENGTH_SHORT).show()
            }

            Toast.makeText(this, "Sticker(s) imported!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // Return to main library view after import
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        finish()
    }

    /** Copy a shared image into app-private folder and save URI */
    private fun importSticker(src: Uri) {
        try {
            val input = contentResolver.openInputStream(src) ?: return
            val name = "zaticker_${System.currentTimeMillis()}.png"
            val file = File(filesDir, name)
            FileOutputStream(file).use { out -> input.copyTo(out) }

            val uri = Uri.fromFile(file)

            // Save into shared prefs
            val set = prefs.getStringSet("uris", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            val newSet = mutableSetOf(uri.toString())
            newSet.addAll(set)
            prefs.edit().putStringSet("uris", newSet).apply()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
