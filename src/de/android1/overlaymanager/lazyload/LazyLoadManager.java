package de.android1.overlaymanager.lazyload;

import java.util.List;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.Projection;

import de.android1.overlaymanager.ManagedOverlay;
import de.android1.overlaymanager.ManagedOverlayItem;

public class LazyLoadManager {

    private static final java.lang.String LOG_TAG = "Maps_LazyLoadHandler";

    protected static final int REFRESH_ITEMS = 1;
    protected static final int ON_BEGIN = 10;
    protected static final int ON_SUCCESS = 11;
    protected static final int ON_ERROR = 12;

    protected ManagedOverlay overlay;
    protected Boolean threadRunning = true;
    private int threadPauseTime = 1000;
    protected LazyLoadAnimation lazyLoadAnimation;
    protected LazyLoadCallback lazyLoadCallback;
    protected LazyLoadListener lazyLoadListener;

    public LazyLoadManager(ManagedOverlay overlay) {
	this.overlay = overlay;
	this.lazyloadHandler.setOverlay(overlay);
    }

    public LazyLoadAnimation enableLazyLoadAnimation(ImageView imageView) {
	lazyLoadAnimation = new LazyLoadAnimation(imageView);
	return lazyLoadAnimation;
    }

    protected LazyLoadHandler lazyloadHandler = new LazyLoadHandler() {
	@Override
	public void handleMessage(Message message) {
	    switch (message.what) {
	    case REFRESH_ITEMS:
		overlay.addAll((List<ManagedOverlayItem>) message.obj);
		overlay.getManager().getMapView().postInvalidate();
		break;
	    case ON_BEGIN:
		if (lazyLoadAnimation != null)
		    lazyLoadAnimation.start();
		if (lazyLoadListener != null) {
		    lazyLoadListener.onBegin(overlay);
		}
		break;
	    case ON_SUCCESS:
		if (lazyLoadListener != null) {
		    lazyLoadListener.onSuccess(overlay);
		}
		if (lazyLoadAnimation != null)
		    lazyLoadAnimation.stop();
		break;
	    case ON_ERROR:
		if (lazyLoadListener != null) {
		    lazyLoadListener.onError((LazyLoadException) message.obj, overlay);
		}
		if (lazyLoadAnimation != null)
		    lazyLoadAnimation.stop();
		break;
	    }
	}
    };

    private volatile long delay = 0;
    private volatile boolean run = false;

    public synchronized void call(long delay) {
	this.run = true;
	this.delay = delay;
    }

    private synchronized void reset() {
	this.run = false;
	this.delay = 0;
    }

    public synchronized void close() {
	this.threadRunning = false;
    }
    
    public synchronized void start() {
	this.threadRunning = true;
	invoke();
    }

    public void invoke() {
	Thread t = new Thread(new Runnable() {
	    @Override
	    public void run() {
		while (threadRunning) {
		    try {
			Thread.sleep(getThreadPauseTime());
			if (run) {
			    Log.d(LOG_TAG, "Lazy Loading...");
			    long mdelay = delay;
			    reset();
			    if (mdelay > 0)
				try {
				    Log.d(LOG_TAG, "Waiting " + mdelay);
				    Thread.sleep(mdelay);
				} catch (InterruptedException e) {
				    e.printStackTrace();
				}
			    if (lazyLoadListener != null || lazyLoadAnimation != null) {
				lazyloadHandler.sendEmptyMessage(ON_BEGIN);
			    }

			    try {
				Projection p = overlay.getManager().getMapView().getProjection();
				GeoPoint topleft = p.fromPixels(0, 0);
				GeoPoint bottomright = p.fromPixels(overlay.getManager().getMapView().getWidth(), overlay.getManager().getMapView().getHeight());
				List<ManagedOverlayItem> newitems = overlay.getLazyLoadCallback().lazyload(topleft, bottomright, overlay);
				Message.obtain(lazyloadHandler, REFRESH_ITEMS, newitems).sendToTarget();

				if (overlay.getLazyLoadListener() != null || lazyLoadAnimation != null)
				    lazyloadHandler.sendEmptyMessage(ON_SUCCESS);
				int size = 0;
				if (newitems != null)
				    size = newitems.size();
				Log.d(LOG_TAG, "LazyLoad - Success (" + size + ") items loaded.");
			    } catch (LazyLoadException e) {
				if (lazyLoadListener != null || lazyLoadAnimation != null) {
				    Message.obtain(lazyloadHandler, ON_ERROR, e).sendToTarget();
				}
				Log.w(LOG_TAG, "LazyLoad - Exception:" + e);
			    }

			}
		    } catch (InterruptedException e1) {

		    }
		}
	    }
	});
	t.start();
    }

    private static class LazyLoadHandler extends Handler {

	ManagedOverlay overlay;

	public void setOverlay(ManagedOverlay overlay) {
	    this.overlay = overlay;
	}
    }

    public LazyLoadCallback getLazyLoadCallback() {
	return lazyLoadCallback;
    }

    public void setLazyLoadCallback(LazyLoadCallback lazyLoadCallback) {
	this.lazyLoadCallback = lazyLoadCallback;
    }

    public LazyLoadListener getLazyLoadListener() {
	return lazyLoadListener;
    }

    public void setLazyLoadListener(LazyLoadListener lazyLoadListener) {
	this.lazyLoadListener = lazyLoadListener;
    }

    public LazyLoadAnimation getLazyLoadAnimation() {
	return lazyLoadAnimation;
    }

    public void setThreadPauseTime(int threadPauseTime) {
	this.threadPauseTime = threadPauseTime;
    }

    public int getThreadPauseTime() {
	return threadPauseTime;
    }
}
