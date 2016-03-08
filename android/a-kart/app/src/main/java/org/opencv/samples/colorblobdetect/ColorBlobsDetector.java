package org.opencv.samples.colorblobdetect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.frogdesign.akart.util.UtilsKt;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import rx.functions.Func1;

public class ColorBlobsDetector implements Func1<Bitmap, Boolean> {
    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.1;
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(25, 50, 50, 0);
    private Mat mSpectrum = new Mat();
    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();

    // Cache
    Mat mPyrDownMat = new Mat();
    Mat mHsvMat = new Mat();
    Mat mMask = new Mat();
    Mat mDilatedMask = new Mat();
    Mat mHierarchy = new Mat();

    public void setColorRadius(Scalar radius) {
        mColorRadius = radius;
    }


    public static class NamedColorBlob {
        private static Scalar colorRadius = new Scalar(50, 50, 50, 0);

        public final String id;
        public final Scalar lowerBound = new Scalar(0);
        public final Scalar upperBound = new Scalar(0);

        public NamedColorBlob(String id, Scalar hsvColor) {
            this.id = id;
            double minH = hsvColor.val[0] - colorRadius.val[0];
            double maxH = hsvColor.val[0] + colorRadius.val[0];

            lowerBound.val[0] = minH;
            upperBound.val[0] = maxH + 1;

            lowerBound.val[1] = UtilsKt.clamp(hsvColor.val[1] - colorRadius.val[1], 0, 255);
            upperBound.val[1] = UtilsKt.clamp(hsvColor.val[1] + colorRadius.val[1], 0, 255) + 1;

            lowerBound.val[2] = UtilsKt.clamp(hsvColor.val[2] - colorRadius.val[2], 0, 255);
            upperBound.val[2] = UtilsKt.clamp(hsvColor.val[2] + colorRadius.val[2], 0, 255) + 1;

            lowerBound.val[3] = 0;
            upperBound.val[3] = 255 + 1;
        }

        public final Point centroid = new Point();
    }

    private List<NamedColorBlob> registered = new ArrayList<>();
    public CopyOnWriteArrayList<NamedColorBlob> detected = new CopyOnWriteArrayList<>();

    public void addDetectedColor(String id, Scalar hsvColor) {
        registered.add(new NamedColorBlob(id, hsvColor));
    }

    public Mat getSpectrum() {
        return mSpectrum;
    }

    public void setMinContourArea(double area) {
        mMinContourArea = area;
    }

    private final Mat kernel = new Mat();

    List<MatOfPoint> contours = new ArrayList<>();
    public List<Point> centroids = new ArrayList<>();

    private Mat imgMAT = null;
    private Bitmap testImg = loadImage();

    @Override
    public Boolean call(Bitmap bmp) {
        if (imgMAT == null) {
            imgMAT = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC4);
        }
        Utils.bitmapToMat(bmp, imgMAT, true);
        process(imgMAT);
//        if (testImg == null) {
//            saveImage(bmp);
//            Utils.bitmapToMat(bmp, imgMAT);
//        }
        return Boolean.TRUE;
    }

    public void process(Mat rgbaImage) {

//        if (testImg != null) {
//            if (imgMAT == null) {
//                imgMAT = new Mat(testImg.getHeight(), testImg.getWidth(), CvType.CV_8UC4);
//            }
//            Utils.bitmapToMat(testImg, imgMAT, true);
//            mPyrDownMat = imgMAT;
//            Mat copied = rgbaImage.submat(0, imgMAT.rows(), 0, imgMAT.cols());
//            imgMAT.copyTo(copied);

//            Scalar red = getAvgHSVFromRgba(imgMAT, 256,200, 278,220);
//            Scalar green = getAvgHSVFromRgba(imgMAT, 305,199, 327,220);
//            Scalar blue = getAvgHSVFromRgba(imgMAT, 256,247, 277,271);
//            Scalar violet = getAvgHSVFromRgba(imgMAT, 306,247, 327,269);
//
//            Log.i("CONTOUR", "red: (" + red.val[0] + ", " + red.val[1] +", " + red.val[2] + ", " + red.val[3] +")");
//            Log.i("CONTOUR", "green: (" + green.val[0] + ", " + green.val[1] +", " + green.val[2] + ", " + green.val[3] +")");
//            Log.i("CONTOUR", "blue: (" + blue.val[0] + ", " + blue.val[1] +", " + blue.val[2] + ", " + blue.val[3] +")");
//            Log.i("CONTOUR", "violet: (" + violet.val[0] + ", " + violet.val[1] +", " + violet.val[2] + ", " + violet.val[3] +")");
//        }
        //Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        //Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
        mPyrDownMat = rgbaImage;
        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);
//        double[] hsv = mHsvMat.get(263,256);
//        Log.i("CONTOUR", "Touched hsv color: (" + hsv[0] + ", " + hsv[1] +
//                ", " + hsv[2] + ")");
        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        detected.clear();

        for (int i = 0; i < registered.size(); i++) {

            NamedColorBlob ncb = registered.get(i);

            Core.inRange(mHsvMat, ncb.lowerBound, ncb.upperBound, mMask);
            Imgproc.dilate(mMask, mDilatedMask, kernel);

            List<MatOfPoint> contours = new ArrayList<>();

            Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

           // Imgproc.drawContours(rgbaImage, contours, -1,  new Scalar( 0,255, 0, 255));
            // Find max contour area
            double maxArea = 0;
            Iterator<MatOfPoint> each = contours.iterator();
            while (each.hasNext()) {
                MatOfPoint wrapper = each.next();
                if (isProbablyAMarker(wrapper, -1)) {
                    double area = Imgproc.contourArea(wrapper);
                    if (area > maxArea)
                        maxArea = area;
                }
            }
            each = contours.iterator();
            while (each.hasNext()) {
                MatOfPoint contour = each.next();
                if (isProbablyAMarker(contour, maxArea)) {
                    //Core.multiply(contour, new Scalar(2,2), contour);
                    mContours.add(contour);
                    Moments mu = Imgproc.moments(contour, false);
                    ncb.centroid.x = mu.get_m10() / mu.get_m00();
                    ncb.centroid.y = mu.get_m01() / mu.get_m00();
                    detected.add(ncb);
                }
            }
        }
    }

    private void saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        String fname = "Image-Drone.png";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Bitmap loadImage() {
        String root = Environment.getExternalStorageDirectory().toString();
        File file = new File(root + "/saved_images/Image-Drone.png");
        try {
            return BitmapFactory.decodeStream(new FileInputStream(file)).copy(Bitmap.Config.ARGB_8888, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private float squarenessRolerance = 0.1f;

    private boolean isProbablyAMarker(MatOfPoint contour, double maxArea) {
         //check if roughly square
        Rect rect = Imgproc.boundingRect(contour);
        float low_h = rect.width * (1.0f - squarenessRolerance);
        float high_h = rect.width * (1.0f + squarenessRolerance);
        if (rect.height < low_h || rect.height > high_h) return false;

        //check if area is ok
        if (maxArea >= 0 && Imgproc.contourArea(contour) < mMinContourArea * maxArea) return false;
        return true;
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }

    private static Scalar getAvgHSVFromRgba(Mat whole, int x_topleft, int y_topleft, int x_botright, int y_botright) {
        Mat hsv = new Mat();
        int width = x_botright - x_topleft;
        int height = y_botright - y_topleft;
        Mat rgba = whole.submat(y_topleft, y_botright, x_topleft, x_botright);
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        Scalar value = Core.sumElems(hsv);
        int pointCount = width * height;
        for (int i = 0; i < value.val.length; i++)
            value.val[i] /= pointCount;
        return value;
    }
}
