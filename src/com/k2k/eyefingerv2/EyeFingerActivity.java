package com.k2k.eyefingerv2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

public class EyeFingerActivity extends Activity implements CvCameraViewListener2 {

	private static final String TAG = "EyeTrackerActivity";
	private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
	public static final int JAVA_DETECTOR = 0;
	private static final int TM_SQDIFF = 0;
	private static final int TM_SQDIFF_NORMED = 1;
	private static final int TM_CCOEFF = 2;
	private static final int TM_CCOEFF_NORMED = 3;
	private static final int TM_CCORR = 4;
	private static final int TM_CCORR_NORMED = 5;

	private int learn_frames = 0;
	private Mat teplateR;
	private Mat teplateL;
	int method = 0;

	private MenuItem mItemFace50;
	private MenuItem mItemFace40;
	private MenuItem mItemFace30;
	private MenuItem mItemFace20;
	private MenuItem mItemType;

	private Mat mRgba;
	private Mat mGray;
	// matrix for zooming
	private Mat mZoomWindow;
	private Mat mZoomWindow2;

	private File mCascadeFile;
	private CascadeClassifier mJavaDetector;
	private CascadeClassifier mJavaDetectorEye;
	private CascadeClassifier mJavaDetectorEyeLeft;

	private int mDetectorType = JAVA_DETECTOR;
	private String[] mDetectorName;

	private float mRelativeFaceSize = 0.2f;
	private int mAbsoluteFaceSize = 0;

	private CameraBridgeViewBase mOpenCvCameraView;

	private SeekBar mMethodSeekbar;
	private TextView mValue;

	double xCenter = -1;
	double yCenter = -1;
	int mOrientationMode = Configuration.ORIENTATION_PORTRAIT;

	SensorManager m_sensor_manager;
	Sensor m_light_sensor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_eye_finger);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.javaCameraViewEyeFinger);
		mOpenCvCameraView.setCvCameraViewListener(this);

//		mMethodSeekbar = (SeekBar) findViewById(R.id.methodSeekBar);
//		mValue = (TextView) findViewById(R.id.method);

//		mMethodSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
//
//			@Override
//			public void onStopTrackingTouch(SeekBar seekBar) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void onStartTrackingTouch(SeekBar seekBar) {
//				// TODO Auto-generated method stub
//
//			}
//
//			@Override
//			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//				method = progress;
//				switch (method) {
//				case 0:
//					mValue.setText("TM_SQDIFF");
//					break;
//				case 1:
//					mValue.setText("TM_SQDIFF_NORMED");
//					break;
//				case 2:
//					mValue.setText("TM_CCOEFF");
//					break;
//				case 3:
//					mValue.setText("TM_CCOEFF_NORMED");
//					break;
//				case 4:
//					mValue.setText("TM_CCORR");
//					break;
//				case 5:
//					mValue.setText("TM_CCORR_NORMED");
//					break;
//				}
//
//			}
//		});
		
//		m_sensor_manager.registerListener(this, m_light_sensor, SensorManager.SENSOR_DELAY_UI);
//		Sensor m_light_sensor = m_sensor_manager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.eye_finger, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");

				try {
					// load cascade file from application resources
					InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
					File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
					mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
					FileOutputStream os = new FileOutputStream(mCascadeFile);

					byte[] buffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						os.write(buffer, 0, bytesRead);
					}
					is.close();
					os.close();

					InputStream iser = getResources().openRawResource(R.raw.haarcascade_righteye_2splits);
					File cascadeDirER = getDir("cascadeER", Context.MODE_PRIVATE);
					File cascadeFileER = new File(cascadeDirER, "haarcascade_eye_right.xml");
					FileOutputStream oser = new FileOutputStream(cascadeFileER);

					byte[] bufferER = new byte[4096];
					int bytesReadER;
					while ((bytesReadER = iser.read(bufferER)) != -1) {
						oser.write(bufferER, 0, bytesReadER);
					}
					iser.close();
					oser.close();

					iser = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
					File cascadeDirEL = getDir("cascadeER", Context.MODE_PRIVATE);
					File cascadeFileEL = new File(cascadeDirEL, "haarcascade_eye_left.xml");
					oser = new FileOutputStream(cascadeFileEL);

					byte[] bufferEL = new byte[4096];
					int bytesReadEL;
					while ((bytesReadEL = iser.read(bufferEL)) != -1) {
						oser.write(bufferEL, 0, bytesReadEL);
					}
					iser.close();
					oser.close();
					
					mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
					if (mJavaDetector.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetector = null;
					} else {
						Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
					}

					mJavaDetectorEye = new CascadeClassifier(cascadeFileER.getAbsolutePath());
					if (mJavaDetectorEye.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetectorEye = null;
					} else {
						Log.i(TAG, "Loaded cascade classifier from " + cascadeFileER.getAbsolutePath());
					}
					
					mJavaDetectorEyeLeft = new CascadeClassifier(cascadeFileEL.getAbsolutePath());
					if (mJavaDetectorEyeLeft.empty()) {
						Log.e(TAG, "Failed to load cascade classifier");
						mJavaDetectorEyeLeft = null;
					} else {
						Log.i(TAG, "Loaded cascade classifier from " + cascadeFileEL.getAbsolutePath());
					}

					cascadeFileEL.delete();
					cascadeFileER.delete();
					cascadeDir.delete();

				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
				}
				mOpenCvCameraView.setCameraIndex(1);
				mOpenCvCameraView.enableFpsMeter();
				mOpenCvCameraView.enableView();

			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};
	
	public EyeFingerActivity() {
		mDetectorName = new String[2];
		mDetectorName[JAVA_DETECTOR] = "Java";

		Log.i(TAG, "Instantiated new " + this.getClass());
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		mOpenCvCameraView.disableView();
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
		mRgba = new Mat();
	}

	@Override
	public void onCameraViewStopped() {
		mGray.release();
		mRgba.release();
		mZoomWindow.release();
		mZoomWindow2.release();
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();
		mGray = inputFrame.gray();
		Core.flip(mRgba, mRgba, 1);
		Core.flip(mGray, mGray, 1);

		
		// Potation Mode
		{
			if (mAbsoluteFaceSize == 0) {
				int height = mGray.rows();
				if (Math.round(height * mRelativeFaceSize) > 0) {
					mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
				}
			}

			if (mZoomWindow == null || mZoomWindow2 == null) {
				CreateAuxiliaryMats();
			}

			MatOfRect faces = new MatOfRect();

			if (mJavaDetector != null) {
				mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
						new Size());
			}
			
			Rect[] facesArray = faces.toArray();
			for (int i = 0; i < facesArray.length; i++) {
				Core.putText(mRgba, "[Rect No:" + i + "/" + facesArray.length + "]", new Point(100 , 100),
						Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255, 255));
				
				Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
				// 센터 좌표 출력
				xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
				yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
				Point center = new Point(xCenter, yCenter);
	
				Core.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);
	
				Core.putText(mRgba, "[" + center.x + "," + center.y + "]", new Point(center.x + 20, center.y + 20),
						Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255, 255));
	
				// 눈 주위 사각형 표시
				Rect r = facesArray[i];
//				// compute the eye area
//				Rect eyearea = new Rect(r.x + r.width / 8, (int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
//						(int) (r.height / 3.0));
				
				// split it
				Rect eyearea_right = new Rect(r.x + r.width / 16, (int) (r.y + (r.height / 4.5)),
						(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
				Rect eyearea_left = new Rect(r.x + r.width / 16 + (r.width - 2 * r.width / 16) / 2,
						(int) (r.y + (r.height / 4.5)), (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
	
//				Core.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(), new Scalar(255, 0, 0, 255), 2);
//				Core.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(), new Scalar(255, 0, 0, 255), 2);
	
				Core.putText(mRgba, "[Learn CNT:" + learn_frames + "]", new Point(100, 200),
						Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255, 255));
				
				if (learn_frames < 5) {
					teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
					teplateL = get_template(mJavaDetectorEyeLeft, eyearea_left, 24);
					learn_frames++;
				} else {
					match_eye(eyearea_right, teplateR, method);
					match_eye(eyearea_left, teplateL, method);
				}
//	
//				Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2, mZoomWindow2.size());
//				Imgproc.resize(mRgba.submat(eyearea_right), mZoomWindow, mZoomWindow.size());
	
			}
		}

/*		// Landscape Mode
		{
				if (mAbsoluteFaceSize == 0) {
			int height = mGray.rows();
			if (Math.round(height * mRelativeFaceSize) > 0) {
				mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
			}
		}

		if (mZoomWindow == null || mZoomWindow2 == null) {
			CreateAuxiliaryMats();
		}

		MatOfRect faces = new MatOfRect();

		if (mJavaDetector != null) {
			mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize),
					new Size());
		}
		
			Rect[] facesArray = faces.toArray();
			for (int i = 0; i < facesArray.length; i++) {
				Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
				xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
				yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
				Point center = new Point(xCenter, yCenter);
	
				Core.circle(mRgba, center, 10, new Scalar(255, 0, 0, 255), 3);
	
				Core.putText(mRgba, "[" + center.x + "," + center.y + "]", new Point(center.x + 20, center.y + 20),
						Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255, 255));
	
				Rect r = facesArray[i];
				// compute the eye area
				Rect eyearea = new Rect(r.x + r.width / 8, (int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
						(int) (r.height / 3.0));
				// split it
				Rect eyearea_right = new Rect(r.x + r.width / 16, (int) (r.y + (r.height / 4.5)),
						(r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
				Rect eyearea_left = new Rect(r.x + r.width / 16 + (r.width - 2 * r.width / 16) / 2,
						(int) (r.y + (r.height / 4.5)), (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
	
				Core.rectangle(mRgba, eyearea_left.tl(), eyearea_left.br(), new Scalar(255, 0, 0, 255), 2);
				Core.rectangle(mRgba, eyearea_right.tl(), eyearea_right.br(), new Scalar(255, 0, 0, 255), 2);
	
				if (learn_frames < 5) {
					teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
					teplateL = get_template(mJavaDetectorEye, eyearea_left, 24);
					learn_frames++;
				} else {
					match_eye(eyearea_right, teplateR, method);
					match_eye(eyearea_left, teplateL, method);
				}
	
				Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2, mZoomWindow2.size());
				Imgproc.resize(mRgba.submat(eyearea_right), mZoomWindow, mZoomWindow.size());
	
			}
		}
*/
		return mRgba;
	}
	
	private void CreateAuxiliaryMats() {
		if (mGray.empty())
			return;

		int rows = mGray.rows();
		int cols = mGray.cols();

		if (mZoomWindow == null) {
			mZoomWindow = mRgba.submat(rows / 2 + rows / 10, rows, cols / 2 + cols / 10, cols);
			mZoomWindow2 = mRgba.submat(0, rows / 2 - rows / 10, cols / 2 + cols / 10, cols);
		}

	}

	private void match_eye(Rect area, Mat mTemplate, int type) {
		Point matchLoc;
		Mat mROI = mGray.submat(area);
		int result_cols = mROI.cols() - mTemplate.cols() + 1;
		int result_rows = mROI.rows() - mTemplate.rows() + 1;
		
		Core.putText(mRgba, "[mROI.cols()" + mROI.cols() + "] [mTemplate.cols()=" + mTemplate.cols() + "]}", new Point(100, 240),
				Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255, 255));
		
		Core.putText(mRgba, "[mROI.cols()" + mROI.rows() + "] [mTemplate.cols()=" + mTemplate.rows() + "]}", new Point(100, 260),
				Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255, 255));
		
		// Check for bad template size
		if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
			learn_frames = 0;
			return;
		}
		Mat mResult = new Mat(result_cols, result_rows, CvType.CV_8U);

		switch (type) {
		case TM_SQDIFF:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
			break;
		case TM_SQDIFF_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF_NORMED);
			break;
		case TM_CCOEFF:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
			break;
		case TM_CCOEFF_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF_NORMED);
			break;
		case TM_CCORR:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
			break;
		case TM_CCORR_NORMED:
			Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR_NORMED);
			break;
		}

		Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
		// there is difference in matching methods - best match is max/min value
		if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
			matchLoc = mmres.minLoc;
		} else {
			matchLoc = mmres.maxLoc;
		}

		Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
		Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x, matchLoc.y + mTemplate.rows() + area.y);

		Core.rectangle(mRgba, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0, 255));
		//Rect rec = new Rect(matchLoc_tx, matchLoc_ty);
//		if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
//			return mmres.maxVal;
//		} else {
//			return mmres.minVal;
//		}
	}

	private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {
		Mat template = new Mat();
		Mat mROI = mGray.submat(area);
		MatOfRect eyes = new MatOfRect();
		Point iris = new Point();
		Rect eye_template = new Rect();
		clasificator.detectMultiScale(mROI, eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT
				| Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30), new Size());

		Rect[] eyesArray = eyes.toArray();
		for (int i = 0; i < eyesArray.length; i++) {
			Rect e = eyesArray[i];
			e.x = area.x + e.x;
			e.y = area.y + e.y;
			Rect eye_only_rectangle = new Rect((int) e.tl().x, (int) (e.tl().y + e.height * 0.4), (int) e.width,
					(int) (e.height * 0.6));
			// reduce ROI
			mROI = mGray.submat(eye_only_rectangle);
			Mat vyrez = mRgba.submat(eye_only_rectangle);
			// find the darkness point
			Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);
			// draw point to visualise pupil
			Core.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
			iris.x = mmG.minLoc.x + eye_only_rectangle.x;
			iris.y = mmG.minLoc.y + eye_only_rectangle.y;
			eye_template = new Rect((int) iris.x - size / 2, (int) iris.y - size / 2, size, size);
			Core.rectangle(mRgba, eye_template.tl(), eye_template.br(), new Scalar(255, 0, 0, 255), 2);
			// copy area to template
			template = (mGray.submat(eye_template)).clone();
			return template;
		}
		return template;
	}
}
