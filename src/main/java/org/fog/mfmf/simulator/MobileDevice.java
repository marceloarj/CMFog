package org.fog.mfmf.simulator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobileDevice {

	private int id;
	private FogNode accessPoint;
	private FogNode predictedAP;
	private int mobilityIndex;
	private Link accessLink;
	private Map<Integer, List<Coordinate>> traces = new HashMap<>();
	private double contentSize;

	public FogNode getPredictedAP() {
		return predictedAP;
	}

	public void setPredictedAP(FogNode predictedAP) {
		this.predictedAP = predictedAP;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public FogNode getAccessPoint() {
		return accessPoint;
	}

	public void setAccessPoint(FogNode accessPoint) {
		this.accessPoint = accessPoint;
	}

	public int getMobilityIndex() {
		return mobilityIndex;
	}

	public void setMobilityIndex(int mobilityIndex) {
		this.mobilityIndex = mobilityIndex;
	}

	public Link getAccessLink() {
		return accessLink;
	}

	public void setAccessLink(Link accessLink) {
		this.accessLink = accessLink;
	}

	public void addTrace(List<Coordinate> aTrace) {
		int idtrace = traces.size();
		traces.put(idtrace, aTrace);
	}

	public Map<Integer, List<Coordinate>> getTraces() {
		return traces;
	}

	public Coordinate getActualPosition(int pertrace, int steps) {
		return traces.get(pertrace).get(steps);
	}

	public double getContentSize() {
		return contentSize;
	}

	public void setContentSize(double contentSize) {
		this.contentSize = contentSize;
	}

	public void setTraces(Map<Integer, List<Coordinate>> traces) {
		this.traces = traces;
	}

}
