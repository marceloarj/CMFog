package org.fog.mfmf.simulator;

public class Link {
	private double bandwith;
	private double delay;
	private Node parent;
	
	public Link(double bandwith, double delay) {
		super();
		this.bandwith = bandwith;
		this.delay = delay;
	}
	public double getBandwith() {
		return bandwith;
	}
	public void setBandwith(double bandwith) {
		this.bandwith = bandwith;
	}
	public double getDelay() {
		return delay;
	}
	public void setDelay(double delay) {
		this.delay = delay;
	}
	public Node getParent() {
		return parent;
	}
	public void setParent(Node parent) {
		this.parent = parent;
	}
	
	
}
