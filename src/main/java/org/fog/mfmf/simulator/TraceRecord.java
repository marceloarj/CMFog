package org.fog.mfmf.simulator;

import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;

public class TraceRecord {

    private double x;
    private double y;
    private FogDevice node;
    private MobileDevice mobileDevice;

    public TraceRecord(double latitude, double longitude, MobileDevice mobileDevice) {
        this.x = latitude;
        this.y = longitude;
        this.mobileDevice = mobileDevice;
    }

    public double getX() {
        return x;
    }

    public void setX(double latitude) {
        this.x = latitude;
    }

    public double getY() {
        return y;
    }

    public void setY(double longitude) {
        this.y = longitude;
    }

    public FogDevice getNode() {
        return node;
    }

    public void setNode(FogDevice node) {
        this.node = node;
    }

    public MobileDevice getMobileDevice() {
        return mobileDevice;
    }

    public void setMobileDevice(MobileDevice mobileDevice) {
        this.mobileDevice = mobileDevice;
    }

}
