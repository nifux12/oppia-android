package org.oppia.android.app.recyclerview

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

private const val ALPHA_FULL = 1.0f
private const val ALPHA_DRAGGING = 0.5f
private const val LONG_PRESS_DELAY = 20L // Reduced from default ~500ms

/**
 * A custom [ItemTouchHelper.Callback] that provides optimized drag & drop functionality with reduced
 * activation delay.
 */
class DragAndDropItemFacilitator(
  private val onItemDragListener: OnItemDragListener,
  private val onDragEndedListener: OnDragEndedListener
) {
  private val callback = object : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
      recyclerView: RecyclerView,
      viewHolder: RecyclerView.ViewHolder
    ): Int {
      val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
      return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
      recyclerView: RecyclerView,
      source: RecyclerView.ViewHolder,
      target: RecyclerView.ViewHolder
    ): Boolean {
      onItemDragListener.onItemDragged(
        source.adapterPosition,
        target.adapterPosition,
        recyclerView.adapter!!
      )
      return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun getAnimationDuration(
      recyclerView: RecyclerView,
      animationType: Int,
      animateDx: Float,
      animateDy: Float
    ): Long = LONG_PRESS_DELAY

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
      if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
        viewHolder?.itemView?.alpha = ALPHA_DRAGGING
      }
      super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
      viewHolder.itemView.alpha = ALPHA_FULL
      onDragEndedListener.onDragEnded(recyclerView.adapter!!)
      super.clearView(recyclerView, viewHolder)
    }
  }

  private val itemTouchHelper = ItemTouchHelper(callback)

  fun attachToRecyclerView(recyclerView: RecyclerView) {
    itemTouchHelper.attachToRecyclerView(recyclerView)
  }
}
