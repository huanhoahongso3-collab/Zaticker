package dhp.thl.tpl.ndv // ✅ Updated Package

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.json.JSONArray

class StickerAdapter(
    private val list: MutableList<Uri>,
    private val listener: StickerListener
) : RecyclerView.Adapter<StickerAdapter.ViewHolder>() {

    interface StickerListener {
        fun onStickerClick(uri: Uri)
        fun onStickerLongClick(uri: Uri)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.image)
        
        init {
            view.setOnClickListener { listener.onStickerClick(list[adapterPosition]) }
            view.setOnLongClickListener {
                listener.onStickerLongClick(list[adapterPosition])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_sticker, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.image.context)
            .load(list[position])
            .centerInside()
            .into(holder.image)
    }

    override fun getItemCount() = list.size

    fun addStickerAtTop(context: Context, uri: Uri) {
        list.add(0, uri)
        notifyItemInserted(0)
        saveOrdered(context)
    }

    fun removeSticker(context: Context, uri: Uri) {
        val pos = list.indexOf(uri)
        if (pos != -1) {
            list.removeAt(pos)
            notifyItemRemoved(pos)
            saveOrdered(context)
        }
    }

    private fun saveOrdered(context: Context) {
        val prefs = context.getSharedPreferences("stickers_prefs", Context.MODE_PRIVATE)
        val array = JSONArray()
        list.forEach { array.put(it.toString()) }
        prefs.edit().putString("order_json", array.toString()).apply()
    }

    companion object {
        fun loadOrdered(context: Context): MutableList<Uri> {
            val prefs = context.getSharedPreferences("stickers_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString("order_json", null)
            val result = mutableListOf<Uri>()
            
            if (json != null) {
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    result.add(Uri.parse(array.getString(i)))
                }
            } else {
                // Fallback to scanning directory if no JSON exists
                val dir = context.filesDir
                dir.listFiles()?.forEach { 
                    if (it.name.startsWith("zaticker_")) result.add(Uri.fromFile(it)) 
                }
            }
            return result
        }
    }
}
