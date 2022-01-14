package org.fog.mfmf.simulator;

public class Node {
	protected Link uplink;
	protected int id;
	//Link downlink;
	public Link getUplink() {
		return uplink;
	}
	public void setUplink(Link uplink) {
		this.uplink = uplink;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	
	
	
}
