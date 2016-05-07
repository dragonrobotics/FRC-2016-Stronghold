package org.usfirst.frc.team5002.robot.commands;

import com.ni.vision.NIVision;
import com.ni.vision.NIVision.Image;
import edu.wpi.first.wpilibj.CameraServer;

public class BipolarCamera{
	private final int camCenter;
	private final int camRight;
	private int desiredCam;
	private int curCam;
	private Image frame;
	private CameraServer server;
	
	public BipolarCamera(int desiredCam){
		this.desiredCam = desiredCam;
        // Get camera ids by supplying camera name ex 'cam0', found on roborio web interface
        camCenter = NIVision.IMAQdxOpenCamera("cam0", NIVision.IMAQdxCameraControlMode.CameraControlModeController);
        camRight = NIVision.IMAQdxOpenCamera("cam1", NIVision.IMAQdxCameraControlMode.CameraControlModeController);
        // Img that will contain camera img
        frame = NIVision.imaqCreateImage(NIVision.ImageType.IMAGE_RGB, 0);
        // Server that we'll give the img to
        server = CameraServer.getInstance();
        server.setQuality(50);
        curCam = camCenter;
		}
	
	public void init(){
	}
	
	public void run(){
		if(desiredCam != camCenter){
			changeCam(camCenter);
		}
		
		else if(desiredCam != camRight){
			changeCam(camRight);
		}
		this.updateCam();
		return;
	}
	
	/**
	 * Stop aka close camera stream
	 */
	public void end(){
	}
	
	/**
	 * Change the camera to get imgs from to a different one
	 * @param newId for camera
	 */
	public void changeCam(int newId){
		NIVision.IMAQdxStopAcquisition(curCam);
    	NIVision.IMAQdxConfigureGrab(newId);
    	NIVision.IMAQdxStartAcquisition(newId);
    	curCam = newId;
    }
    
	/**
	 * Get the img from current camera and give it to the server
	 */
    public void updateCam(){
    	NIVision.IMAQdxGrab(curCam, frame, 1);
        server.setImage(frame);
    }
}