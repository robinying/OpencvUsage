package com.robin.opencvusage.ui.activity

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.robin.opencvusage.R
import com.robin.opencvusage.databinding.ActivityMainBinding
import org.opencv.android.CameraBridgeViewBase
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

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private var mJavaDetector: CascadeClassifier? = null
    private var mRgba: Mat? = null
    private var mGray: Mat? = null
    private val FACE_RECT_COLOR = Scalar(0.0, 255.0, 0.0, 255.0)
    private var mCascadeFile: File? = null
    private var mAbsoluteFaceSize = 0
    private val mRelativeFaceSize = 0.2f
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val isSuccess = OpenCVLoader.initDebug()
        if (isSuccess) {
            Toast.makeText(this, "opencv init success", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "opencv init failed", Toast.LENGTH_SHORT).show()
        }
        binding.cameraView.setCameraPermissionGranted()
        binding.cameraView.visibility = CameraBridgeViewBase.VISIBLE
        binding.cameraView.setCvCameraViewListener(this)
        startCamera()
    }

    private fun startCamera() {
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

            cascadeDir.delete()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        binding.cameraView.enableView()
    }


    override fun onDestroy() {
        super.onDestroy()
        binding.cameraView.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mGray = Mat()
        mRgba = Mat()
    }

    override fun onCameraViewStopped() {
        mGray?.release()
        mRgba?.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        mRgba = inputFrame.rgba()
        mGray = inputFrame.gray()

        if (mAbsoluteFaceSize == 0) {
            val height = mGray!!.rows()
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize).toInt()
            }
        }

        val faces = MatOfRect()
        mJavaDetector?.detectMultiScale(
            mGray, faces, 1.1, 2, 2,
            Size(mAbsoluteFaceSize.toDouble(), mAbsoluteFaceSize.toDouble()), Size()
        )


        val facesArray = faces.toArray()
        for (i in facesArray.indices) {
            Imgproc.rectangle(
                mRgba,
                facesArray[i].tl(),
                facesArray[i].br(),
                FACE_RECT_COLOR,
                3
            )
        }

        return mRgba!!
    }
}