package com.robin.opencvusage.ui.activity

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import com.robin.opencvusage.R
import com.robin.opencvusage.facedetect.DetectionBasedTracker
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private var mJavaDetector: CascadeClassifier? = null
    private var mNativeDetector:DetectionBasedTracker?=null
    private var mCascadeFile: File? = null
    private val mRelativeFaceSize = 0.2f
    private var mAbsoluteFaceSize = 0
    private val FACE_RECT_COLOR = Scalar(0.0, 255.0, 0.0, 255.0)

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker")

                    try {
                        // load cascade file from application resources
                        val `is` = resources.openRawResource(R.raw.lbpcascade_frontalface)
                        val cascadeDir = getDir("cascade", MODE_PRIVATE)
                        mCascadeFile = File(cascadeDir, "lbpcascade_frontalface.xml")
                        val os: FileOutputStream = FileOutputStream(mCascadeFile)
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (`is`.read(buffer).also { bytesRead = it } != -1) {
                            os.write(buffer, 0, bytesRead)
                        }
                        `is`.close()
                        os.close()
                        mJavaDetector = CascadeClassifier(mCascadeFile!!.absolutePath)
                        if (mJavaDetector!!.empty()) {
                            Log.e(
                                TAG,
                                "Failed to load cascade classifier"
                            )
                            mJavaDetector = null
                        } else {
                            Log.i(
                                TAG,
                                "Loaded cascade classifier from " + mCascadeFile!!.absolutePath
                            )
                        }
                        mNativeDetector = DetectionBasedTracker(mCascadeFile!!.absolutePath, 0)
                        mNativeDetector?.start()
                        cascadeDir.delete()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e(TAG, "Failed to load cascade. Exception thrown: $e")
                    }

                    mOpenCvCameraView!!.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Permissions for Android 6+
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )

        setContentView(R.layout.activity_main)

        mOpenCvCameraView = findViewById(R.id.main_surface)

        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE

        mOpenCvCameraView!!.setCvCameraViewListener(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mOpenCvCameraView!!.setCameraPermissionGranted()
                } else {
                    val message = "Camera permission was not granted"
                    Log.e(TAG, message)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                Log.e(TAG, "Unexpected permission request")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}

    override fun onCameraViewStopped() {}

    override fun onCameraFrame(frame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        // get current camera frame as OpenCV Mat object
        val mat = frame.rgba()

//        // native call to process current camera frame
//        adaptiveThresholdFromJNI(mat.nativeObjAddr)

        // return processed frame for live preview

        val mRgba = frame.rgba()
        val mGray = frame.gray()

        if (mAbsoluteFaceSize == 0) {
            val height: Int = mGray.rows()
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize)
            }
        }

        val faces = MatOfRect()

//        mJavaDetector?.detectMultiScale(
//            mGray, faces, 1.1, 2, 2,  // TODO: objdetect.CV_HAAR_SCALE_IMAGE
//            Size(mAbsoluteFaceSize.toDouble(), mAbsoluteFaceSize.toDouble()), Size()
//        )
        mNativeDetector?.detect(mGray, faces)


        val facesArray = faces.toArray()
        Log.d(TAG, "face size:" + facesArray.size)
        for (i in facesArray.indices) {
            Imgproc.rectangle(
                mRgba,
                facesArray[i].tl(),
                facesArray[i].br(),
                FACE_RECT_COLOR,
                3
            )
        }
        return mat
    }

//    private external fun adaptiveThresholdFromJNI(matAddr: Long)

    companion object {

        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_REQUEST = 1
    }
}