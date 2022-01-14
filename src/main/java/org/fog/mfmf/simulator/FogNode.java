package org.fog.mfmf.simulator;

public class FogNode extends Node{
	private int startX;
	private int endX;
	private int startY;
	private int endY;
	private Node cloud;
	public int getStartX() {
		return startX;
	}
	public void setStartX(int startX) {
		this.startX = startX;
	}
	public int getEndX() {
		return endX;
	}
	public void setEndX(int endX) {
		this.endX = endX;
	}
	public int getStartY() {
		return startY;
	}
	public void setStartY(int startY) {
		this.startY = startY;
	}
	public int getEndY() {
		return endY;
	}
	public void setEndY(int endY) {
		this.endY = endY;
	}
	public Node getCloud() {
		return cloud;
	}
	public void setCloud(Node cloud) {
		this.cloud = cloud;
	}
	
	
}
