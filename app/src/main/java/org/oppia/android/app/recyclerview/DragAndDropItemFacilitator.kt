package org.oppia.android.app.recyclerview

import android.graphics.Canvas
import android.view.MotionEvent
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

private const val ALPHA_FULL = 1.0f
private const val ALPHA_DRAGGING = 0.5f

/** A [ItemTouchHelper.SimpleCallback] that provides drag & drop functionality to [RecyclerView]s. */
class DragAndDropItemFacilitator(
  private val onItemDragListener: OnItemDragListener,
  private val onDragEndedListener: OnDragEndedListener
) : ItemTouchHelper.Callback() {

  private var itemTouchHelper: ItemTouchHelper? = null

  fun attachToRecyclerView(recyclerView: RecyclerView) {
    ItemTouchHelper(this).also { helper ->
      itemTouchHelper = helper
      helper.attachToRecyclerView(recyclerView)

      recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
          if (e.action == MotionEvent.ACTION_DOWN) {
            rv.findChildViewUnder(e.x, e.y)?.let { childView ->
              rv.getChildViewHolder(childView)?.let { viewHolder ->
                itemTouchHelper?.startDrag(viewHolder)
              }
            }
          }
          return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
      })
    }
  }

  override fun getMovementFlags(
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder
  ): Int = makeMovementFlags(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
    /* swipeFlags= */ 0
  )

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

  // Disable default long press behavior
  override fun isLongPressDragEnabled(): Boolean = false

  override fun isItemViewSwipeEnabled(): Boolean = false

  override fun onChildDraw(
    canvas: Canvas,
    recyclerView: RecyclerView,
    viewHolder: RecyclerView.ViewHolder,
    dX: Float,
    dY: Float,
    actionState: Int,
    isCurrentlyActive: Boolean
  ) {
    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
      viewHolder.itemView.alpha = if (isCurrentlyActive) ALPHA_DRAGGING else ALPHA_FULL
      viewHolder.itemView.elevation = if (isCurrentlyActive) 8f else 0f
    }
    super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
  }

  override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
    super.clearView(recyclerView, viewHolder)
    // Reset view properties after drag
    viewHolder.itemView.alpha = ALPHA_FULL
    viewHolder.itemView.elevation = 0f
    onDragEndedListener.onDragEnded(recyclerView.adapter!!)
  }
}
