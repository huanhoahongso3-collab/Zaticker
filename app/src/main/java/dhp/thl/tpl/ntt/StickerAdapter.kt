package dhp.thl.tpl.ntt

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dhp.thl.tpl.ntt.databinding.ItemStickerBinding

class StickerAdapter(
    private val stickers: MutableList<Uri>,
    private val listener: StickerListener
) : RecyclerView.Adapter<StickerAdapter.VH>() {

    interface StickerListener {
        fun onStickerClick(uri: Uri)
        fun onStickerLongClick(uri: Uri)
    }

    inner class VH(val bind: ItemStickerBinding) : RecyclerView.ViewHolder(bind.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = stickers.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val uri = stickers[position]
        holder.bind.image.setImageURI(uri)
        holder.bind.image.setOnClickListener { listener.onStickerClick(uri) }
        holder.bind.image.setOnLongClickListener {
            listener.onStickerLongClick(uri)
            true
        }
    }

    fun addSticker(uri: Uri) {
        stickers.add(uri)
        notifyItemInserted(stickers.size - 1)
    }

    fun removeSticker(uri: Uri) {
        val index = stickers.indexOf(uri)
        if (index != -1) {
            stickers.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
