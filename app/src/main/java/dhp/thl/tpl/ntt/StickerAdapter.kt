package dhp.thl.tpl.ntt

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import dhp.thl.tpl.ntt.databinding.ItemStickerBinding

class StickerAdapter(
    private val stickers: MutableList<Uri>,
    private val listener: StickerListener
) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {

    interface StickerListener {
        fun onStickerClick(uri: Uri)
        fun onStickerLongClick(uri: Uri)
    }

    inner class StickerViewHolder(val binding: ItemStickerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
        val binding = ItemStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StickerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        val uri = stickers[position]

        // Load sticker image efficiently
        Glide.with(holder.binding.image.context)
            .load(uri)
            .centerCrop()
            .into(holder.binding.image)

        // Click → share
        holder.binding.image.setOnClickListener {
            listener.onStickerClick(uri)
        }

        // Long press → confirm delete
        holder.binding.image.setOnLongClickListener {
            listener.onStickerLongClick(uri)
            true
        }
    }

    override fun getItemCount(): Int = stickers.size

    /** ✅ Add a new sticker, optionally at the top-left */
    fun addSticker(uri: Uri, toTop: Boolean = false) {
        if (toTop) {
            stickers.add(0, uri)
            notifyItemInserted(0)
        } else {
            stickers.add(uri)
            notifyItemInserted(stickers.size - 1)
        }
    }

    /** ✅ Remove a sticker safely */
    fun removeSticker(uri: Uri) {
        val index = stickers.indexOf(uri)
        if (index != -1) {
            stickers.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
