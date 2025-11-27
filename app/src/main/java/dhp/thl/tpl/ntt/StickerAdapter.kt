package dhp.thl.tpl.ntt

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class StickerAdapter(
    private val stickers: MutableList<Uri>,
    private val listener: StickerListener
) : RecyclerView.Adapter<StickerAdapter.ViewHolder>() {

    interface StickerListener {
        fun onStickerClick(uri: Uri)
        fun onStickerLongClick(uri: Uri)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sticker, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = stickers.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = stickers[position]
        holder.bind(uri)
    }

    fun addStickerAtTop(context: Context, uri: Uri) {
        stickers.add(0, uri)
        notifyItemInserted(0)
        Toast.makeText(context, context.getString(R.string.importing), Toast.LENGTH_SHORT).show()
    }

    fun removeSticker(context: Context, uri: Uri) {
        val index = stickers.indexOf(uri)
        if (index != -1) {
            stickers.removeAt(index)
            notifyItemRemoved(index)
            Toast.makeText(context, context.getString(R.string.deleted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, context.getString(R.string.delete_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun getStickerAt(position: Int): Uri = stickers[position]

    fun addAll(newStickers: List<Uri>) {
        stickers.addAll(newStickers)
        notifyDataSetChanged()
    }

    fun clear() {
        stickers.clear()
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.sticker_image)

        fun bind(uri: Uri) {
            Glide.with(itemView.context)
                .load(uri)
                .centerCrop()
                .into(imageView)

            itemView.setOnClickListener {
                (itemView.context as? StickerListener)?.onStickerClick(uri)
            }

            itemView.setOnLongClickListener {
                showDeleteDialog(uri)
                true
            }
        }

        private fun showDeleteDialog(uri: Uri) {
            val context = itemView.context
            if (context !is StickerListener) return

            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.delete_sticker))
                .setMessage(context.getString(R.string.delete_sticker_confirm))
                .setPositiveButton(context.getString(R.string.delete)) { _, _ ->
                    context.onStickerLongClick(uri)
                }
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show()
        }
    }

    companion object {
        fun loadOrdered(context: Context): MutableList<Uri> {
            // Load stickers from internal storage or database
            // For simplicity, return empty list here
            return mutableListOf()
        }
    }
}
