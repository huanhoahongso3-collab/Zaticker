package dhp.thl.tpl.ntt

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import dhp.thl.tpl.ntt.databinding.ItemStickerBinding
import java.io.File

class StickerAdapter(
    private val stickers: MutableList<Uri>,
    private val listener: StickerListener
) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {

    interface StickerListener {
        fun onStickerClick(uri: Uri)
        fun onStickerLongClick(uri: Uri)
    }

    class StickerViewHolder(val binding: ItemStickerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val binding = ItemStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        val uri = stickers[position]
        holder.binding.stickerImage.setImageURI(uri)

        holder.binding.root.setOnClickListener { listener.onStickerClick(uri) }
        holder.binding.root.setOnLongClickListener {
            listener.onStickerLongClick(uri)
            true
        }
    }

    override fun getItemCount(): Int = stickers.size

    /** Add sticker to top */
    fun addStickerAtTop(context: Context, uri: Uri) {
        stickers.add(0, uri)
        notifyItemInserted(0)
    }

    /** Remove sticker */
    fun removeSticker(context: Context, uri: Uri) {
        val index = stickers.indexOf(uri)
        if (index != -1) {
            stickers.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    companion object {

        /** Load stickers from app private storage in sorted order */
        fun loadOrdered(context: Context): MutableList<Uri> {
            val dir = context.filesDir
            val list = dir.listFiles()?.filter { it.extension.lowercase() == "png" } ?: emptyList()
            val sorted = list.sortedByDescending { it.lastModified() }
            return sorted.map { Uri.fromFile(it) }.toMutableList()
        }
    }
}
