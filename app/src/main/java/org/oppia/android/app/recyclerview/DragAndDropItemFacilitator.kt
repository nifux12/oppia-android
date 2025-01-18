package org.oppia.android.app.recyclerview

import android.view.MotionEvent
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

private const val ALPHA_FULL = 1.0f
private const val ALPHA_DRAGGING = 0.5f
// Reduce the long press timeout (default is 500ms)
private const val DRAG_START_DELAY = 150L

/**
 * A custom [ItemTouchHelper.Callback] that provides optimized drag & drop functionality with reduced
 * activation delay.
 */
class DragAndDropItemFacilitator(
  private val onItemDragListener: OnItemDragListener,
  private val onDragEndedListener: OnDragEndedListener
) : ItemTouchHelper.Callback() {

  private var dragStarted = false
  private var initialTouchPos = 0f
  private lateinit var itemTouchHelper: ItemTouchHelper

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

  override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    // Swiping not supported
  }

  override fun isLongPressDragEnabled(): Boolean = false

  override fun isItemViewSwipeEnabled(): Boolean = false

  override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
    if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
      viewHolder?.itemView?.alpha = ALPHA_DRAGGING
    }
    super.onSelectedChanged(viewHolder, actionState)
  }

  override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
    viewHolder.itemView.alpha = ALPHA_FULL
    if (dragStarted) {
      onDragEndedListener.onDragEnded(recyclerView.adapter!!)
      dragStarted = false
    }
    super.clearView(recyclerView, viewHolder)
  }

  fun attachToRecyclerView(recyclerView: RecyclerView) {
    itemTouchHelper = ItemTouchHelper(this)
    itemTouchHelper.attachToRecyclerView(recyclerView)

    recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
      private var dragStartTime = 0L

      override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val holder = rv.findChildViewUnder(e.x, e.y)?.let { rv.getChildViewHolder(it) }

        when (e.actionMasked) {
          MotionEvent.ACTION_DOWN -> {
            dragStartTime = System.currentTimeMillis()
            initialTouchPos = e.y
          }
          MotionEvent.ACTION_MOVE -> {
            if (!dragStarted && holder != null) {
              val dragDuration = System.currentTimeMillis() - dragStartTime
              val dragDistance = Math.abs(e.y - initialTouchPos)

              // Start drag if either:
              // 1. User has held for the minimum time
              // 2. User has moved significantly while holding
              if (dragDuration >= DRAG_START_DELAY || dragDistance > 20) {
                dragStarted = true
                itemTouchHelper.startDrag(holder)
                return true
              }
            }
          }
          MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            dragStartTime = 0
          }
        }
        return false
      }
    })
  }
}
