package dhp.thl.tpl.ntt

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.MultipartBuilder
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private val apiUrl = "https://briaai-bria-rmbg-1-4.hf.space/--replicas/5jrnx/predict"

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter
    private val images = mutableListOf<ImageModel>()
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        adapter = ImageAdapter(images, ::onImageLongPress)
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        recyclerView.adapter = adapter

        findViewById<View>(R.id.btnImport).setOnClickListener {
            pickImages()
        }
    }

    /** Pick images silently (single or multiple) */
    private fun pickImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(Intent.createChooser(intent, "Chọn hình"), 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            if (data.clipData != null) {
                for (i in 0 until data.clipData!!.itemCount) {
                    val uri = data.clipData!!.getItemAt(i).uri
                    addImage(uri)
                }
            } else {
                data.data?.let { uri -> addImage(uri) }
            }
        }
    }

    private fun addImage(uri: Uri) {
        val path = FileUtils.getPath(this, uri) ?: return
        images.add(ImageModel(path))
        adapter.notifyItemInserted(images.size - 1)
    }

    /** Long-press menu: Xóa nền / Xóa / Xuất / Hủy */
    private fun onImageLongPress(view: View, image: ImageModel) {
        val popup = androidx.appcompat.widget.PopupMenu(this, view)
        popup.menu.add(getString(R.string.rb_option)) // Xóa nền
        popup.menu.add(getString(R.string.delete))    // Xóa
        popup.menu.add(getString(R.string.export))    // Xuất
        popup.menu.add(getString(R.string.cancel))    // Hủy

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                getString(R.string.rb_option) -> removeBackgroundWithLoading(image)
                getString(R.string.delete) -> deleteImage(image)
                getString(R.string.export) -> exportImage(image)
                getString(R.string.cancel) -> {}
            }
            true
        }
        popup.show()
    }

    /** Delete image from list and storage */
    private fun deleteImage(image: ImageModel) {
        val index = images.indexOf(image)
        if (index != -1) {
            images.removeAt(index)
            adapter.notifyItemRemoved(index)
        }
        val file = File(image.path)
        if (file.exists()) file.delete()
        Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show()
    }

    /** Export image using FileProvider */
    private fun exportImage(image: ImageModel) {
        val file = File(image.path)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Xuất hình"))
    }

    /** Remove background with circular loading */
    private fun removeBackgroundWithLoading(image: ImageModel) {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val base64 = callRemoveBgApi(File(image.path))
            progressBar.visibility = View.GONE

            if (base64 != null) {
                val decoded = Base64.decode(base64, Base64.DEFAULT)
                File(image.path).writeBytes(decoded)
                adapter.notifyItemChanged(images.indexOf(image))
                Toast.makeText(this@MainActivity, getString(R.string.rb_success), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, getString(R.string.rb_failed, "API lỗi"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Call Bria AI API */
    private suspend fun callRemoveBgApi(imageFile: File): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBuilder()
                .type(MultipartBuilder.FORM)
                .addFormDataPart(
                    "data[]",
                    imageFile.name,
                    com.squareup.okhttp.RequestBody.create(MediaType.parse("image/png"), imageFile)
                )
                .build()

            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = response.body()?.string() ?: return@withContext null
                return@withContext json.substringAfter("\"data\":[\"").substringBefore("\"]")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return@withContext null
        }
    }
}

