package camera;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import mingming.research.Utils;

public class Camerafragment extends Fragment implements SurfaceHolder.Callback{

	private static final int PICTURE_SIZE_MAX_WIDTH = 1280;
    private static final int PREVIEW_SIZE_MAX_WIDTH = 640;

    private int cameraId;
    private Camera camera;
    private SurfaceHolder surfaceHolder;
    private CameraFragmentListener listener;
	
    
    /**
     * On activity getting attached.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof CameraFragmentListener)) {
            throw new IllegalArgumentException(
                "Activity has to implement CameraFragmentListener interface"
            );
        }

        listener = (CameraFragmentListener) activity;
    }
    
    
    /**
     * On creating view for fragment.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CameraPreview previewView = new CameraPreview(getActivity());

        previewView.getHolder().addCallback(this);

        return previewView;
    }
    
    /**
     * On fragment getting resumed.
     */
    @Override
    public void onResume() {
        super.onResume();

        try {
            camera = Camera.open(cameraId);
        } catch (Exception exception) {
            Log.e(Utils.TAG, "Can't open camera with id " + cameraId, exception);

            listener.onCameraError();
            return;
        }
    }

    /**
     * On fragment getting paused.
     */
    @Override
    public void onPause() {
        super.onPause();

        stopCameraPreview();
        camera.release();
    }

    /**
     * Start the camera preview.
     */
    private synchronized void startCameraPreview() {
        determineDisplayOrientation();
        setupCamera();

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException exception) {
            Log.e(Utils.TAG, "Can't start camera preview due to IOException", exception);

            listener.onCameraError();
        }
    }
    
    
    /**
     * Determine the current display orientation and rotate the camera preview
     * accordingly.
     */
    public void determineDisplayOrientation() {
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraId, cameraInfo);

        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees  = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int displayOrientation;

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            displayOrientation = (cameraInfo.orientation + degrees) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        } else {
            displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
        }

        camera.setDisplayOrientation(displayOrientation);
    }

    /**
     * Setup the camera parameters.
     */
    public void setupCamera() {
        Camera.Parameters parameters = camera.getParameters();

        Size bestPreviewSize = determineBestPreviewSize(parameters);
        Size bestPictureSize = determineBestPictureSize(parameters);

        parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);

        camera.setParameters(parameters);
    }

    private Size determineBestPreviewSize(Camera.Parameters parameters) {
        List<Size> sizes = parameters.getSupportedPreviewSizes();

        return determineBestSize(sizes, PREVIEW_SIZE_MAX_WIDTH);
    }
    
    
    private Size determineBestPictureSize(Camera.Parameters parameters) {
        List<Size> sizes = parameters.getSupportedPictureSizes();

        return determineBestSize(sizes, PICTURE_SIZE_MAX_WIDTH);
    }

    protected Size determineBestSize(List<Size> sizes, int widthThreshold) {
        Size bestSize = null;

        for (Size currentSize : sizes) {
            boolean isDesiredRatio = (currentSize.width / 4) == (currentSize.height / 3);
            boolean isBetterSize = (bestSize == null || currentSize.width > bestSize.width);
            boolean isInBounds = currentSize.width <= PICTURE_SIZE_MAX_WIDTH;

            if (isDesiredRatio && isInBounds && isBetterSize) {
                bestSize = currentSize;
            }
        }

        if (bestSize == null) {
            listener.onCameraError();

            return sizes.get(0);
        }

        return bestSize;
    }
    
    /**
     * Stop the camera preview.
     */
    private synchronized void stopCameraPreview() {
        try {
            camera.stopPreview();
        } catch (Exception exception) {
            Log.i(Utils.TAG, "Exception during stopping camera preview");
        }
    }

    
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
	    this.surfaceHolder = holder;

        startCameraPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		
	}

}
