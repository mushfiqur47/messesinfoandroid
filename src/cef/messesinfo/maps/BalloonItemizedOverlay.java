package cef.messesinfo.maps;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import cef.messesinfo.R;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

import de.android1.overlaymanager.ManagedOverlay;
import de.android1.overlaymanager.ManagedOverlayItem;
import de.android1.overlaymanager.OverlayManager;

public abstract class BalloonItemizedOverlay extends ManagedOverlay implements OnCreateContextMenuListener {

	private MapView mapView;
	private BalloonOverlayView<ManagedOverlayItem> balloonView;
	private View clickRegion;
	private int viewOffset;
	final MapController mc;
	private ManagedOverlayItem currentFocussedItem;
	private int currentFocussedIndex;

	/**
	 * Create a new BalloonItemizedOverlay
	 * 
	 * @param defaultMarker - A bounded Drawable to be drawn on the map for each item in the overlay.
	 * @param mapView - The view upon which the overlay items are to be drawn.
	 */
	public BalloonItemizedOverlay(OverlayManager manager, String name, Drawable drawable) {
		super(manager, name, drawable);
		this.mapView = manager.getMapView();
		viewOffset = 0;
		mc = mapView.getController();
	}

	/**
	 * Set the horizontal distance between the marker and the bottom of the information
	 * balloon. The default is 0 which works well for center bounded markers. If your
	 * marker is center-bottom bounded, call this before adding overlay items to ensure
	 * the balloon hovers exactly above the marker. 
	 * 
	 * @param pixels - The padding between the center point and the bottom of the
	 * information balloon.
	 */
	public void setBalloonBottomOffset(int pixels) {
		viewOffset = pixels;
	}
	public int getBalloonBottomOffset() {
		return viewOffset;
	}

	/**
	 * Override this method to handle a "tap" on a balloon. By default, does nothing 
	 * and returns false.
	 * 
	 * @param index - The index of the item whose balloon is tapped.
	 * @param item - The item whose balloon is tapped.
	 * @return true if you handled the tap, otherwise false.
	 */
	protected boolean onBalloonTap(int index, ManagedOverlayItem item) {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.ItemizedOverlay#onTap(int)
	 */
	@Override
	protected boolean onTap(int index) {

		currentFocussedIndex = index;
		currentFocussedItem = createItem(index);

		boolean isRecycled;
		if (balloonView == null) {
			balloonView = createBalloonOverlayView();
			clickRegion = (View) balloonView.findViewById(R.id.balloon_inner_layout);
			clickRegion.setOnTouchListener(createBalloonTouchListener());
			clickRegion.setOnCreateContextMenuListener(this);
			isRecycled = false;
		} else {
			isRecycled = true;
		}

		balloonView.setVisibility(View.GONE);

		List<Overlay> mapOverlays = mapView.getOverlays();
		if (mapOverlays.size() > 1) {
			hideOtherBalloons(mapOverlays);
		}

		balloonView.setData(currentFocussedItem);

		GeoPoint point = currentFocussedItem.getPoint();
		MapView.LayoutParams params = new MapView.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, point,
				MapView.LayoutParams.BOTTOM_CENTER);
		params.mode = MapView.LayoutParams.MODE_MAP;

		balloonView.setVisibility(View.VISIBLE);

		if (isRecycled) {
			balloonView.setLayoutParams(params);
		} else {
			mapView.addView(balloonView, params);
		}

		mc.animateTo(point);

		return true;
	}

	/**
	 * Creates the balloon view. Override to create a sub-classed view that
	 * can populate additional sub-views.
	 */
	protected BalloonOverlayView<ManagedOverlayItem> createBalloonOverlayView() {
		return new BalloonOverlayView<ManagedOverlayItem>(getMapView().getContext(), getBalloonBottomOffset());
	}

	/**
	 * Sets the visibility of this overlay's balloon view to GONE. 
	 */
	protected void hideBalloon() {
		if (balloonView != null) {
			balloonView.setVisibility(View.GONE);
		}
	}

	/**
	 * Hides the balloon view for any other BalloonItemizedOverlay instances
	 * that might be present on the MapView.
	 * 
	 * @param overlays - list of overlays (including this) on the MapView.
	 */
	private void hideOtherBalloons(List<Overlay> overlays) {

		for (Overlay overlay : overlays) {
			if (overlay instanceof BalloonItemizedOverlay && overlay != this) {
				((BalloonItemizedOverlay) overlay).hideBalloon();
			}
		}

	}

	/**
	 * Sets the onTouchListener for the balloon being displayed, calling the
	 * overridden {@link #onBalloonTap} method.
	 */
	private OnTouchListener createBalloonTouchListener() {
		return new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {

				View l =  ((View) v.getParent()).findViewById(R.id.balloon_main_layout);
				Drawable d = l.getBackground();

				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					int[] states = {android.R.attr.state_pressed};
					if (d.setState(states)) {
						d.invalidateSelf();
					}
					return true;
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					int newStates[] = {};
					if (d.setState(newStates)) {
						d.invalidateSelf();
					}
					// call overridden method
					onBalloonTap(currentFocussedIndex, currentFocussedItem);
					return true;
				} else {
					return false;
				}

			}
		};
	}
	
	public ManagedOverlayItem getCurrentFocussedItem() {
	    return currentFocussedItem;
	}

}
