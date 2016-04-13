package org.opencv.samples.colorblobdetect
//
//import android.app.Activity
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.Matrix
//import android.os.Bundle
//import android.os.Environment
//import android.util.Log
//import com.frogdesign.akart.MarkerDetector
//import com.frogdesign.akart.model.Car
//import com.frogdesign.akart.util.clamp
//import com.frogdesign.akart.util.inrange
//import com.frogdesign.akart.view.AimView
//import org.opencv.android.BaseLoaderCallback
//import org.opencv.android.LoaderCallbackInterface
//import org.opencv.android.OpenCVLoader
//import org.opencv.android.Utils
//import org.opencv.core.*
//import org.opencv.imgproc.Imgproc
//import timber.log.Timber
//import java.io.File
//import java.io.FileInputStream
//import java.io.FileNotFoundException
//import java.io.FileOutputStream
//import java.util.*
//import java.util.concurrent.CopyOnWriteArrayList
//
//class ColorBlobsDetector : MarkerDetector {
//
//    private val mContours = ArrayList<MatOfPoint>()
//
//    // Cache
//    internal var mPyrDownMat = Mat()
//    internal var mHsvMat = Mat()
//    internal var mMask = Mat()
//    internal var mDilatedMask = Mat()
//    internal var mHierarchy = Mat()
//
//    private var mLoaderCallback: BaseLoaderCallback? = null
//
//    override fun setup(ctx: Context, cars: List<Car>) {
//        for (a in cars) {
//            addDetectedColor(a.id, a.color)
//        }
//    }
//
//    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle) {
//        super.onActivityCreated(activity, savedInstanceState)
//        mLoaderCallback = object : BaseLoaderCallback(activity) {
//            override fun onManagerConnected(status: Int) {
//                when (status) {
//                    LoaderCallbackInterface.SUCCESS -> Timber.i("CENTROID", "OpenCV loaded successfully")
//                    else -> super.onManagerConnected(status)
//                }
//            }
//        }
//
//        if (!OpenCVLoader.initDebug()) {
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, activity, mLoaderCallback)
//        } else {
//            Timber.i("OpenCV library found inside package. Using it!")
//            mLoaderCallback?.onManagerConnected(LoaderCallbackInterface.SUCCESS)
//        }
//    }
//
//    override fun onActivityDestroyed(activity: Activity) {
//        super.onActivityDestroyed(activity)
//        mLoaderCallback = null
//    }
//
//    override fun process(inBitmap: Bitmap?) {
//        if (imgMAT == null) imgMAT = Mat(inBitmap!!.height, inBitmap.width, CvType.CV_8UC4)
//        Utils.bitmapToMat(inBitmap, imgMAT, true)
//        process(imgMAT!!)
//    }
//
//    override fun setTarget(forCar: Car, webcamToScreenTransf: Matrix, targets: AimView) {
//
//        for (i in detected.indices) {
//            try {
//                val a = detected[i]
//                if (a.id == forCar.id) {
//                    Timber.i("CENTROID", "centroid " + a.id + ", " + a.centroid)
//                    val array = floatArrayOf(a.centroid.x.toFloat(), a.centroid.y.toFloat())
//                    webcamToScreenTransf.mapPoints(array)
//                    Timber.i("CENTROID", "mapped " + a.id + ", " + array[0] + ":" + array[1])
//                    targets.setTarget(a.id, array[0], array[1])
//                    break
//                }
//            } catch(e: ArrayIndexOutOfBoundsException) {
//                //just because I suck
//            }
//        }
//    }
//
//
//    class NamedColorBlob(val id: String, hsvColor: Scalar) {
//        val lowerBound = Scalar(0.0)
//        val upperBound = Scalar(0.0)
//
//        init {
//            val minH = hsvColor.`val`[0] - colorRadius.`val`[0]
//            val maxH = hsvColor.`val`[0] + colorRadius.`val`[0]
//
//            lowerBound.`val`[0] = clamp(minH, 0.0, 255.0)
//            upperBound.`val`[0] = clamp(maxH, 0.0, 255.0) + 1
//
//            lowerBound.`val`[1] = clamp(hsvColor.`val`[1] - colorRadius.`val`[1], 0.0, 255.0)
//            upperBound.`val`[1] = clamp(hsvColor.`val`[1] + colorRadius.`val`[1], 0.0, 255.0) + 1
//
//            lowerBound.`val`[2] = clamp(hsvColor.`val`[2] - colorRadius.`val`[2], 0.0, 255.0)
//            upperBound.`val`[2] = clamp(hsvColor.`val`[2] + colorRadius.`val`[2], 0.0, 255.0) + 1
//
//            lowerBound.`val`[3] = 0.0
//            upperBound.`val`[3] = (255 + 1).toDouble()
//        }
//
//        val centroid = Point()
//
//        override fun equals(other: Any?): Boolean {
//            if (this === other) return true
//            if (other == null || javaClass != other.javaClass) return false
//
//            val that = other as NamedColorBlob?
//
//            return id == that?.id
//
//        }
//
//        override fun hashCode(): Int {
//            return id.hashCode()
//        }
//
//        companion object {
//            private val colorRadius = Scalar(25.0, 50.0, 75.0, 0.0)
//        }
//    }
//
//    private val registered = ArrayList<NamedColorBlob>()
//    var detected = CopyOnWriteArrayList<NamedColorBlob>()
//
//    fun addDetectedColor(id: String, hsvColor: Scalar) {
//        registered.add(NamedColorBlob(id, hsvColor))
//    }
//
//    private val kernel = Mat()
//
//    private var imgMAT: Mat? = null
//    //    private val testImg = loadImage()
//
//    fun process(rgba: Mat) {
//
//        val focusedSquare = 275;
//        val x = (rgba.cols() - focusedSquare) / 2
//        val y = (rgba.rows() - focusedSquare) / 2
//        val rgbaImage = rgba.submat(y, y + focusedSquare, x, x + focusedSquare)
//
//        //        if (testImg != null) {
//        //            if (imgMAT == null) {
//        //                imgMAT = new Mat(testImg.getHeight(), testImg.getWidth(), CvType.CV_8UC4);
//        //            }
//        //            Utils.bitmapToMat(testImg, imgMAT, true);
//        //            mPyrDownMat = imgMAT;
//        //            Mat copied = rgbaImage.submat(0, imgMAT.rows(), 0, imgMAT.cols());
//        //            imgMAT.copyTo(copied);
//
//        //            Scalar red = getAvgHSVFromRgba(imgMAT, 256,200, 278,220);
//        //            Scalar green = getAvgHSVFromRgba(imgMAT, 305,199, 327,220);
//        //            Scalar blue = getAvgHSVFromRgba(imgMAT, 256,247, 277,271);
//        //            Scalar violet = getAvgHSVFromRgba(imgMAT, 306,247, 327,269);
//        //
//        //            Timber.i("CONTOUR", "red: (" + red.val[0] + ", " + red.val[1] +", " + red.val[2] + ", " + red.val[3] +")");
//        //            Timber.i("CONTOUR", "green: (" + green.val[0] + ", " + green.val[1] +", " + green.val[2] + ", " + green.val[3] +")");
//        //            Timber.i("CONTOUR", "blue: (" + blue.val[0] + ", " + blue.val[1] +", " + blue.val[2] + ", " + blue.val[3] +")");
//        //            Timber.i("CONTOUR", "violet: (" + violet.val[0] + ", " + violet.val[1] +", " + violet.val[2] + ", " + violet.val[3] +")");
//        //        }
//        mPyrDownMat = rgbaImage
//        Imgproc.pyrDown(rgbaImage, mPyrDownMat)
//        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL)
//        // Filter contours by area and resize to fit the original image size
//        mContours.clear()
//        detected.clear()
//
//        for (i in registered.indices) {
//
//            val ncb = registered[i]
//
//            Core.inRange(mHsvMat, ncb.lowerBound, ncb.upperBound, mMask)
//
//            Imgproc.dilate(mMask, mDilatedMask, kernel)
//
//            val contours = ArrayList<MatOfPoint>()
//
//            Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
//
//            // Imgproc.drawContours(rgbaImage, contours, -1,  new Scalar( 0,255, 0, 255));
//            // Find max contour area
//            var maxArea = 0.0
//            var each = contours.iterator()
//            while (each.hasNext()) {
//                val wrapper = each.next()
//                if (isProbablyAMarker(wrapper, -1.0)) {
//                    val area = Imgproc.contourArea(wrapper)
//                    if (area > maxArea) {
//                        maxArea = area
//                    }
//                }
//            }
//            each = contours.iterator()
//            while (each.hasNext()) {
//                val contour = each.next()
//                if (isProbablyAMarker(contour, maxArea)) {
//                    Core.multiply(contour, Scalar(2.0, 2.0), contour)
//
//                    val area = Imgproc.contourArea(contour)
//                    Timber.i("AREA", ncb.id + "AREA " + area)
//                    mContours.add(contour)
//                    val mu = Imgproc.moments(contour, false)
//                    ncb.centroid.x = x + mu._m10 / mu._m00
//                    ncb.centroid.y = y + mu._m01 / mu._m00
//                    if (detected.contains(ncb))
//                        detected.remove(ncb)
//                    detected.add(ncb)
//                }
//            }
//        }
//    }
//
//    private fun isProbablyAMarker(contour: MatOfPoint, maxArea: Double): Boolean {
//        //check if roughly square
//        val rect = Imgproc.boundingRect(contour)
//        val low_h = rect.width * (1.0f - SQUARENESS_TOLERANCE)
//        val high_h = rect.width * (1.0f + SQUARENESS_TOLERANCE)
//        if (rect.height < low_h || rect.height > high_h) return false
//
//        //check if area is ok
//        val contourArea = Imgproc.contourArea(contour)
//        if (maxArea < 0) {
//            if (!inrange(contourArea, AREA_LOWERBOUND, AREA_UPPERBOUND)) return false
//        } else if (contourArea < MIN_COUNTOUR_AREA_RATIO * maxArea) return false
//        return true
//    }
//
//    fun getContours(): List<MatOfPoint> {
//        return mContours
//    }
//
//    companion object {
//        // Minimum contour area in percent for contours filtering
//        private val MIN_COUNTOUR_AREA_RATIO = 0.1
//
//        private val AREA_LOWERBOUND = 20.0
//        private val AREA_UPPERBOUND = java.lang.Double.MAX_VALUE
//        private val SQUARENESS_TOLERANCE = 0.2f
//
//        fun loadImage(): Bitmap? {
//            val root = Environment.getExternalStorageDirectory().toString()
//            val file = File(root + "/saved_images/Image-Drone.png")
//            try {
//                return BitmapFactory.decodeStream(FileInputStream(file)).copy(Bitmap.Config.ARGB_8888, true)
//            } catch (e: FileNotFoundException) {
//                e.printStackTrace()
//            }
//
//            return null
//        }
//
//        fun saveImage(finalBitmap: Bitmap) {
//
//            val root = Environment.getExternalStorageDirectory().toString()
//            val myDir = File(root + "/saved_images")
//            myDir.mkdirs()
//            val fname = "Image-Drone.png"
//            val file = File(myDir, fname)
//            if (file.exists()) file.delete()
//            try {
//                val out = FileOutputStream(file)
//                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
//                out.flush()
//                out.close()
//
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//
//        }
//
//        private fun getAvgHSVFromRgba(whole: Mat, x_topleft: Int, y_topleft: Int, x_botright: Int, y_botright: Int): Scalar {
//            val hsv = Mat()
//            val width = x_botright - x_topleft
//            val height = y_botright - y_topleft
//            val rgba = whole.submat(y_topleft, y_botright, x_topleft, x_botright)
//            Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV_FULL)
//
//            // Calculate average color of touched region
//            val value = Core.sumElems(hsv)
//            val pointCount = width * height
//            for (i in value.`val`.indices)
//                value.`val`[i] /= pointCount.toDouble()
//            return value
//        }
//    }
//}
