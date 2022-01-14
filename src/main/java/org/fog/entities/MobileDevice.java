package org.fog.entities;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import java.util.Queue;
import java.util.Set;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.NetworkTopology;

import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.localization.*;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.Logger;
import org.fog.utils.ModuleLaunchConfig;
import org.fog.vmmigration.MyStatistics;
import org.fog.vmmigration.VmMigrationTechnique;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.Vm;
import org.fog.application.Application;
import org.fog.cmfog.CMFog;
import org.fog.cmfog.helpers.CMFogHelper;
import org.fog.cmfog.CMFogMobileController;
import org.fog.cmfog.MonitoringAgent;
import org.fog.cmfog.helpers.DecisionHelper;
import static org.fog.cmfog.helpers.DecisionHelper.chooseHandoffAP2;
import org.fog.cmfog.helpers.LogHelper;
import org.fog.cmfog.SimulationParameters;
import org.fog.mfmf.simulator.TraceRecord;
import org.fog.cmfog.enumerate.Algorithm;
import org.fog.cmfog.enumerate.Technology;
import org.fog.vmmigration.BeforeMigration;
import org.fog.vmmigration.DecisionMigration;
import org.fog.vmmigration.Service;
import org.fog.vmmobile.LogMobile;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.MobileEvents;
import org.fog.vmmobile.constants.Policies;

public class MobileDevice extends FogDevice {

    private java.util.Map<Integer, List<TraceRecord>> mobilityTrace = new HashMap<>();
    private java.util.Map<String, Integer> totalTranstion2 = new HashMap<>();
    private java.util.Map<String, Integer> totalTime = new HashMap<>();
    private int totalTransition[];
    private FogDevice lastFN = null;
    private List<FogDevice> mobilityTrack = new ArrayList<>();
    private List<FogDevice> migrationTrack = new ArrayList<>();
    private Algorithm algorithm;
    private Technology technology;
    private int migrations = 0;
    private long totalTuples = 0;
    private long totalLostTuples = 0;
    private double latencyAverage = 0;
    private boolean handoffArea = false;
    private boolean verticalMigration = false;
    private double nextHandoff = 0;
    private double nextMigration = 0;

    public boolean isVerticalMigration() {
        return verticalMigration;
    }

    public void setVerticalMigration(boolean verticalMigration) {
        this.verticalMigration = verticalMigration;
    }

    public List<FogDevice> getMigrationTrack() {
        return migrationTrack;
    }

    public void setMigrationTrack(List<FogDevice> migrationTrack) {
        this.migrationTrack = migrationTrack;
    }

    public List<FogDevice> getMobilityTrack() {
        return mobilityTrack;
    }

    public void setMobilityTrack(List<FogDevice> mobilityTrack) {
        this.mobilityTrack = mobilityTrack;
    }

    public double getLatencyAverage() {
        return latencyAverage;
    }

    public void setLatencyAverage(double latencyAverage) {
        this.latencyAverage = latencyAverage;
    }

    public void addTotalTuples(long tuples) {
        totalTuples += tuples;
    }

    public void addTotalLostTuples(long tuples) {
        totalLostTuples += tuples;
    }

    public long getTotalTuples() {
        return totalTuples;
    }

    public long getTotalLostTuples() {
        return totalLostTuples;
    }

    public int getMigrations() {
        return migrations;
    }

    public void addMigration() {
        this.migrations = migrations + 1;
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public Technology getTechnology() {
        return technology;
    }

    public void setTechnology(Technology technology) {
        this.technology = technology;
    }

    public double getNextMigration() {
        return nextMigration;
    }

    public void setNextMigration(double nextMigration) {
        this.nextMigration = nextMigration;
    }

    public double getNextHandoff() {
        return nextHandoff;
    }

    public void setNextHandoff(double nextHandoff) {
        this.nextHandoff = nextHandoff;
    }

    public Queue<Tuple> getNorthTupleQueue() {
        return northTupleQueue;
    }

    public void setNorthTupleQueue(Queue<Tuple> northTupleQueue) {
        this.northTupleQueue = northTupleQueue;
    }

    public Queue<Pair<Tuple, Integer>> getSouthTupleQueue() {
        return southTupleQueue;
    }

    public void setSouthTupleQueue(Queue<Pair<Tuple, Integer>> southTupleQueue) {
        this.southTupleQueue = southTupleQueue;
    }

    public List<String> getActiveApplications() {
        return activeApplications;
    }

    public void setActiveApplications(List<String> activeApplications) {
        this.activeApplications = activeApplications;
    }

    public ArrayList<String[]> getPath() {
        return path;
    }

    public void setPath(ArrayList<String[]> path) {
        this.path = path;
    }

    public java.util.Map<String, Application> getApplicationMap() {
        return applicationMap;
    }

    public void setApplicationMap(java.util.Map<String, Application> applicationMap) {
        this.applicationMap = applicationMap;
    }

    public java.util.Map<String, List<String>> getAppToModulesMap() {
        return appToModulesMap;
    }

    public void setAppToModulesMap(java.util.Map<String, List<String>> appToModulesMap) {
        this.appToModulesMap = appToModulesMap;
    }

    public java.util.Map<Integer, Double> getChildToLatencyMap() {
        return childToLatencyMap;
    }

    public void setChildToLatencyMap(java.util.Map<Integer, Double> childToLatencyMap) {
        this.childToLatencyMap = childToLatencyMap;
    }

    public java.util.Map<Integer, Integer> getCloudTrafficMap() {
        return cloudTrafficMap;
    }

    public void setCloudTrafficMap(java.util.Map<Integer, Integer> cloudTrafficMap) {
        this.cloudTrafficMap = cloudTrafficMap;
    }

    public double getLockTime() {
        return lockTime;
    }

    public void setLockTime(double lockTime) {
        this.lockTime = lockTime;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public int getVolatilParentId() {
        return volatilParentId;
    }

    public void setVolatilParentId(int volatilParentId) {
        this.volatilParentId = volatilParentId;
    }

    public int getControllerId() {
        return controllerId;
    }

    public void setControllerId(int controllerId) {
        this.controllerId = controllerId;
    }

    public List<Integer> getChildrenIds() {
        return childrenIds;
    }

    public void setChildrenIds(List<Integer> childrenIds) {
        this.childrenIds = childrenIds;
    }

    public java.util.Map<Integer, List<String>> getChildToOperatorsMap() {
        return childToOperatorsMap;
    }

    public void setChildToOperatorsMap(java.util.Map<Integer, List<String>> childToOperatorsMap) {
        this.childToOperatorsMap = childToOperatorsMap;
    }

    public boolean isIsSouthLinkBusy() {
        return isSouthLinkBusy;
    }

    public void setIsSouthLinkBusy(boolean isSouthLinkBusy) {
        this.isSouthLinkBusy = isSouthLinkBusy;
    }

    public boolean isIsNorthLinkBusy() {
        return isNorthLinkBusy;
    }

    public void setIsNorthLinkBusy(boolean isNorthLinkBusy) {
        this.isNorthLinkBusy = isNorthLinkBusy;
    }

    public double getUplinkBandwidth() {
        return uplinkBandwidth;
    }

    public void setUplinkBandwidth(double uplinkBandwidth) {
        this.uplinkBandwidth = uplinkBandwidth;
    }

    public double getDownlinkBandwidth() {
        return downlinkBandwidth;
    }

    public void setDownlinkBandwidth(double downlinkBandwidth) {
        this.downlinkBandwidth = downlinkBandwidth;
    }

    public double getUplinkLatency() {
        return uplinkLatency;
    }

    public void setUplinkLatency(double uplinkLatency) {
        this.uplinkLatency = uplinkLatency;
    }

    public List<Pair<Integer, Double>> getAssociatedActuatorIds() {
        return associatedActuatorIds;
    }

    public void setAssociatedActuatorIds(List<Pair<Integer, Double>> associatedActuatorIds) {
        this.associatedActuatorIds = associatedActuatorIds;
    }

    public double getEnergyConsumption() {
        return energyConsumption;
    }

    public void setEnergyConsumption(double energyConsumption) {
        this.energyConsumption = energyConsumption;
    }

    public double getLastUtilizationUpdateTime() {
        return lastUtilizationUpdateTime;
    }

    public void setLastUtilizationUpdateTime(double lastUtilizationUpdateTime) {
        this.lastUtilizationUpdateTime = lastUtilizationUpdateTime;
    }

    public double getLastUtilization() {
        return lastUtilization;
    }

    public void setLastUtilization(double lastUtilization) {
        this.lastUtilization = lastUtilization;
    }

    public double getRatePerMips() {
        return ratePerMips;
    }

    public void setRatePerMips(double ratePerMips) {
        this.ratePerMips = ratePerMips;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public java.util.Map<String, java.util.Map<String, Integer>> getModuleInstanceCount() {
        return moduleInstanceCount;
    }

    public void setModuleInstanceCount(java.util.Map<String, java.util.Map<String, Integer>> moduleInstanceCount) {
        this.moduleInstanceCount = moduleInstanceCount;
    }

    public Coordinate getCoord() {
        return coord;
    }

    public void setCoord(Coordinate coord) {
        this.coord = coord;
    }

    public Set<ApDevice> getApDevices() {
        return apDevices;
    }

    public void setApDevices(Set<ApDevice> apDevices) {
        this.apDevices = apDevices;
    }

    public Set<MobileDevice> getSmartThings() {
        return smartThings;
    }

    public void setSmartThings(Set<MobileDevice> smartThings) {
        this.smartThings = smartThings;
    }

    public Set<MobileDevice> getSmartThingsWithVm() {
        return smartThingsWithVm;
    }

    public void setSmartThingsWithVm(Set<MobileDevice> smartThingsWithVm) {
        this.smartThingsWithVm = smartThingsWithVm;
    }

    public Set<FogDevice> getServerCloudlets() {
        return serverCloudlets;
    }

    public void setServerCloudlets(Set<FogDevice> serverCloudlets) {
        this.serverCloudlets = serverCloudlets;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public DecisionMigration getMigrationStrategy() {
        return migrationStrategy;
    }

    public void setMigrationStrategy(DecisionMigration migrationStrategy) {
        this.migrationStrategy = migrationStrategy;
    }

    public int getPolicyReplicaVM() {
        return policyReplicaVM;
    }

    public void setPolicyReplicaVM(int policyReplicaVM) {
        this.policyReplicaVM = policyReplicaVM;
    }

    public BeforeMigration getBeforeMigration() {
        return beforeMigration;
    }

    public void setBeforeMigration(BeforeMigration beforeMigration) {
        this.beforeMigration = beforeMigration;
    }

    public int getStartTravelTime() {
        return startTravelTime;
    }

    public void setStartTravelTime(int startTravelTime) {
        this.startTravelTime = startTravelTime;
    }

    public int getTravelTimeId() {
        return travelTimeId;
    }

    public void setTravelTimeId(int travelTimeId) {
        this.travelTimeId = travelTimeId;
    }

    public int getTravelPredicTime() {
        return travelPredicTime;
    }

    public void setTravelPredicTime(int travelPredicTime) {
        this.travelPredicTime = travelPredicTime;
    }

    public int getMobilityPrecitionError() {
        return mobilityPrecitionError;
    }

    public void setMobilityPrecitionError(int mobilityPrecitionError) {
        this.mobilityPrecitionError = mobilityPrecitionError;
    }

    public int getMyId() {
        return myId;
    }

    public void setMyId(int myId) {
        this.myId = myId;
    }

    public int getLayer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public int getNumClients() {
        return numClients;
    }

    public void setNumClients(int numClients) {
        this.numClients = numClients;
    }

    public boolean isHandoffArea() {
        return handoffArea;
    }

    public void setHandoffArea(boolean handoffArea) {
        this.handoffArea = handoffArea;
    }

    public java.util.Map<String, Integer> getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(java.util.Map<String, Integer> totalTime) {
        this.totalTime = totalTime;
    }

    public FogDevice getLastFN() {
        return lastFN;
    }

    public void setLastFN(FogDevice lastFN) {
        this.lastFN = lastFN;
    }

    private Integer estimatedPermTime;

    public Integer getEstimatedPermTime() {
        return estimatedPermTime;
    }

    public void setEstimatedPermTime(Integer estimatedPermTime) {
        this.estimatedPermTime = estimatedPermTime;
    }

    public java.util.Map<String, Integer> getTotalTranstion2() {
        return totalTranstion2;
    }

    public void setTotalTranstion2(java.util.Map<String, Integer> totalTranstion2) {
        this.totalTranstion2 = totalTranstion2;
    }

    public int[] getTotalTransition() {
        return totalTransition;
    }

    public void setTotalTransition(int[] totalTransition) {
        this.totalTransition = totalTransition;
    }

    public java.util.Map<Integer, List<TraceRecord>> getMobilityTrace() {
        return mobilityTrace;
    }

    public void setMobilityTrace(java.util.Map<Integer, List<TraceRecord>> mobilityTrace) {
        this.mobilityTrace = mobilityTrace;
    }

    public void addTrace(List<TraceRecord> aTraceRecord) {
        mobilityTrace.put(mobilityTrace.size(), aTraceRecord);

    }
    /////////////////////////////////
    private int direction; //NONE, NORTH, SOUTH, ...
    private int speed; // in m/s
    protected Coordinate futureCoord;// = new Coordinate();//myiFogSim
    //private double distanceAp;
    private FogDevice sourceServerCloudlet;
    private FogDevice destinationServerCloudlet;
    private FogDevice vmLocalServerCloudlet;
    private ApDevice sourceAp;
    private ApDevice destinationAp;
    private Vm vmMobileDevice;
    private double migTime;
    private boolean migPoint;
    private boolean migZone;
    private Set<MobileSensor> sensors;//Set of Sensors
    private Set<MobileActuator> actuators;//Set of Actuators
    private float maxServiceValue;
    private boolean migStatus;
    private boolean postCopyStatus;
    private boolean handoffStatus;
    private boolean lockedToHandoff;
    private boolean lockedToMigration;
    private boolean abortMigration;
    private double vmSize;
    private double tempSimulation;
    private double timeFinishHandoff = 0;
    private double timeFinishDeliveryVm = 0;
    private double timeStartLiveMigration = 0;
    private boolean status;
    private boolean migStatusLive;
    protected VmMigrationTechnique migrationTechnique;

    //ivate String nome;
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.getName() == null) ? 0 : this.getName().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MobileDevice other = (MobileDevice) obj;
        if (this.getName() == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!this.getName().equals(other.getName())) {
            return false;
        }
        return true;
    }

    public MobileDevice() {
        // TODO Auto-generated constructor stub
    }

    public MobileDevice(String name, Coordinate coord, int coordX, int coordY, int id) {
        // TODO Auto-generated constructor stub
        super(name, coordX, coordY, id);

        setDirection(0);
        setSpeed(0);
        //	setDistanceAp(0);
        setSourceServerCloudlet(null);
        setDestinationServerCloudlet(null);
        setVmLocalServerCloudlet(null);
        setSourceAp(null);
        setDestinationAp(null);
        setVmMobileDevice(null);
        setMigTime(0);
        setMigStatus(false);
        setMigStatusLive(false);
        setPostCopyStatus(false);
        setHandoffStatus(false);
        setLockedToHandoff(false);
        setLockedToMigration(false);
        setAbortMigration(false);
        setMigPoint(false);
        setMigZone(false);
        actuators = new HashSet<>();
        sensors = new HashSet<>();
        setStatus(true);
        this.futureCoord = new Coordinate();
        setFutureCoord(-1, -1);

    }

    public MobileDevice(String name, int coordX, int coordY, int id, int dir, int sp) {
        //			public MobileDevice(String name, Coordinate coord, int coordX, int coordY, int id, int dir, int sp) {

        // TODO Auto-generated constructor stub
        //		super(name, coord, coordX, coordY, id);
        super(name, coordX, coordY, id);
        setDirection(dir);
        setSpeed(sp);
        //	setDistanceAp(0);
        setSourceServerCloudlet(null);
        setDestinationServerCloudlet(null);
        setVmLocalServerCloudlet(null);
        setSourceAp(null);
        setDestinationAp(null);
        setVmMobileDevice(null);
        setMigTime(0);
        setMigStatus(false);
        setMigStatusLive(false);
        setPostCopyStatus(false);

        setHandoffStatus(false);
        setLockedToHandoff(false);
        setLockedToMigration(false);
        setStatus(true);
        setAbortMigration(false);
        this.futureCoord = new Coordinate();
        setFutureCoord(-1, -1);

        actuators = new HashSet<>();
        sensors = new HashSet<>();

    }
    //	@Override
    //	public void startEntity() {
    //		sendNow(getId(),MobileEvents.MAKE_DECISION_MIGRATION,this);
    //	}

    public MobileDevice(String name,
            FogDeviceCharacteristics characteristics,
            AppModuleAllocationPolicy vmAllocationPolicy,
            LinkedList<Storage> storageList, double schedulingInterval, double uplinkBandwidth,
            double downlinkBandwidth, double uplinkLatency,
            double d, int coordX, int coordY, int id, int dir, int sp, float maxServiceValue, double vmSize,
            VmMigrationTechnique migrationTechnique) throws Exception {
        // TODO Auto-generated constructor stub
        super(name, characteristics, vmAllocationPolicy,
                storageList, schedulingInterval,
                uplinkBandwidth,
                downlinkBandwidth,
                uplinkLatency, sp, coordX, coordY, id);
        setDirection(dir);
        setSpeed(sp);
        //		setDistanceAp(0);
        setSourceServerCloudlet(null);
        setDestinationServerCloudlet(null);
        setVmLocalServerCloudlet(null);
        setSourceAp(null);
        setDestinationAp(null);
        setVmMobileDevice(null);
        setMigTime(0);
        setMigStatus(false);
        setMigStatusLive(false);
        setPostCopyStatus(false);

        setVmSize(vmSize);
        setHandoffStatus(false);
        setLockedToHandoff(false);
        setStatus(true);
        setAbortMigration(false);
        setMigrationTechnique(migrationTechnique);
        this.futureCoord = new Coordinate();
        setFutureCoord(-1, -1);
        actuators = new HashSet<>();
        sensors = new HashSet<>();
        setMaxServiceValue(maxServiceValue);

    }

    @Override
    public String toString() {
        return this.getName();
//        return this.getName(); + "[coordX=" + this.getCoord().getCoordX()
//                + ", coordY=" + this.getCoord().getCoordY()
//                + ", direction=" + direction + ", speed=" + speed
//                + /*", distanceAp=" + distanceAp + */ ", sourceCloudletServer="
//                + sourceServerCloudlet + ", destinationCloudletServer="
//                + destinationServerCloudlet + ", sourceAp=" + sourceAp
//                + ", destinationAp=" + destinationAp + ", vmMobileDevice="
//                + vmMobileDevice + ", migTime=" + migTime + ", sensors="
//                + sensors + ", actuators=" + actuators + "]";
    }

    @Override
    protected void processOtherEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case FogEvents.TUPLE_ARRIVAL:
                processTupleArrival(ev);
                break;
            case FogEvents.LAUNCH_MODULE:
                processModuleArrival(ev);
                break;
            case FogEvents.RELEASE_OPERATOR:
                processOperatorRelease(ev);
                break;
            case FogEvents.SENSOR_JOINED:
                processSensorJoining(ev);
                break;
            case FogEvents.SEND_PERIODIC_TUPLE:
                sendPeriodicTuple(ev);
                break;
            case FogEvents.APP_SUBMIT:
                processAppSubmit(ev);
                break;
            case FogEvents.UPDATE_NORTH_TUPLE_QUEUE:
                updateNorthTupleQueue();
                break;
            case FogEvents.UPDATE_SOUTH_TUPLE_QUEUE:
                updateSouthTupleQueue();
                break;
            case FogEvents.ACTIVE_APP_UPDATE:
                updateActiveApplications(ev);
                break;
            case FogEvents.ACTUATOR_JOINED:
                processActuatorJoined(ev);
                break;
            case FogEvents.LAUNCH_MODULE_INSTANCE:
                updateModuleInstanceCount(ev);
                break;
            case FogEvents.RESOURCE_MGMT:
                manageResources(ev);
                break;
            case MobileEvents.MAKE_DECISION_HANDOFF:
                invokeDecisionHandoff(ev);
                break;
            default:
                break;
        }
    }

    public void performHandoff() {
        MobileDevice aMobileDevice = this;
        int apToHandoff = chooseHandoffAP2(CMFog.accessPoints, aMobileDevice);
        if (apToHandoff >= 0 && apToHandoff != aMobileDevice.getSourceAp().getMyId() && !aMobileDevice.isHandoffStatus()) {
            aMobileDevice.setDestinationAp(CMFog.accessPoints.get(apToHandoff));
            aMobileDevice.setHandoffStatus(true);
            aMobileDevice.setLockedToHandoff(true);
            System.out.println("Performing handoff");
            System.out.println("--> Setting " + aMobileDevice.getName() + " to handoff from " + aMobileDevice.getSourceAp().getName() + " to " + aMobileDevice.getDestinationAp().getName());
            double handoffTime = MaxAndMin.MIN_HANDOFF_TIME + (MaxAndMin.MAX_HANDOFF_TIME - MaxAndMin.MIN_HANDOFF_TIME) * SimulationParameters.getRand().nextDouble();
            float handoffLocked = (float) (handoffTime * 10);
            int delayConnection = 100;
            //Handoff will be always between cloudlets, so you have to disconect the smarthing every handoff
            sendNow(aMobileDevice.getSourceServerCloudlet().getId(), MobileEvents.DESCONNECT_ST_TO_SC, aMobileDevice);
            send(aMobileDevice.getDestinationAp().getServerCloudlet().getId(), handoffTime + delayConnection, MobileEvents.CONNECT_ST_TO_SC, aMobileDevice);
            LogMobile.debug("MobileController.java", aMobileDevice.getName() + " will be desconnected from " + aMobileDevice.getSourceServerCloudlet().getName() + " by handoff");
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            send(aMobileDevice.getSourceAp().getId(), handoffTime, MobileEvents.START_HANDOFF, aMobileDevice);
            send(aMobileDevice.getDestinationAp().getId(), handoffLocked, MobileEvents.UNLOCKED_HANDOFF, aMobileDevice);
            MyStatistics.getInstance().setTotalHandoff(1);
            //saveHandOff(aMobileDevice);
            //FogDevice aDestination = CMFogHelper.getCloudletById(aMobileDevice.getDestinationAp().getParentId(), CMFog.cloudletsL1);--
            //aMobileDevice.getMobilityTrack().add(aDestination);
            aMobileDevice.setNextHandoff(-1);
            LogMobile.debug("MobileController.java", aMobileDevice.getName() + " handoff was scheduled! " + "SourceAp: " + aMobileDevice.getSourceAp().getName() + " NextAp: " + aMobileDevice.getDestinationAp().getName() + "\n");
            LogMobile.debug("MobileController.java", "Distance between " + aMobileDevice.getName() + " and " + aMobileDevice.getSourceAp().getName() + ": " + Distances.checkDistance(aMobileDevice.getCoord(), aMobileDevice.getSourceAp().getCoord()));
        }
    }

    private void invokeDecisionHandoff(SimEvent ev) {
        MobileDevice aMobileDevice = (MobileDevice) ev.getData();
        //  if (false) {
        if (aMobileDevice.getSourceAp() != null && DecisionHelper.shouldHandoff(aMobileDevice)) {
            aMobileDevice.setHandoffStatus(true);
            aMobileDevice.setLockedToHandoff(true);
            System.out.println("--> Setting " + aMobileDevice.getName() + " to handoff from " + aMobileDevice.getSourceAp().getName() + " to " + aMobileDevice.getDestinationAp().getName());
            double handoffTime = MaxAndMin.MIN_HANDOFF_TIME + (MaxAndMin.MAX_HANDOFF_TIME - MaxAndMin.MIN_HANDOFF_TIME) * SimulationParameters.getRand().nextDouble();
            float handoffLocked = (float) (handoffTime * 10);
            int delayConnection = 100;
            //Handoff will be always between cloudlets, so you have to disconect the smarthing every handoff
            sendNow(aMobileDevice.getSourceServerCloudlet().getId(), MobileEvents.DESCONNECT_ST_TO_SC, aMobileDevice);
            send(aMobileDevice.getDestinationAp().getServerCloudlet().getId(), handoffTime + delayConnection, MobileEvents.CONNECT_ST_TO_SC, aMobileDevice);
            LogMobile.debug("MobileController.java", aMobileDevice.getName() + " will be desconnected from " + aMobileDevice.getSourceServerCloudlet().getName() + " by handoff");
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            send(aMobileDevice.getSourceAp().getId(), handoffTime, MobileEvents.START_HANDOFF, aMobileDevice);
            send(aMobileDevice.getDestinationAp().getId(), handoffLocked, MobileEvents.UNLOCKED_HANDOFF, aMobileDevice);
            MyStatistics.getInstance().setTotalHandoff(1);
            //saveHandOff(aMobileDevice);

//            FogDevice aDestination = CMFogHelper.getCloudletById(aMobileDevice.getDestinationAp().getParentId(), CMFog.cloudletsL1);--
//            aMobileDevice.getMobilityTrack().add(aDestination);
            aMobileDevice.setNextHandoff(-1);
            LogMobile.debug("MobileController.java", aMobileDevice.getName() + " handoff was scheduled! " + "SourceAp: " + aMobileDevice.getSourceAp().getName() + " NextAp: " + aMobileDevice.getDestinationAp().getName() + "\n");
            LogMobile.debug("MobileController.java", "Distance between " + aMobileDevice.getName() + " and " + aMobileDevice.getSourceAp().getName() + ": " + Distances.checkDistance(aMobileDevice.getCoord(), aMobileDevice.getSourceAp().getCoord()));
        }
    }

    public void saveLostTupple(String a, String filename) {
        try ( FileWriter fw1 = new FileWriter(filename, true);  BufferedWriter bw1 = new BufferedWriter(fw1);  PrintWriter out1 = new PrintWriter(bw1)) {
            out1.println(a);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void processTupleArrival(SimEvent ev) {

        Tuple aTuple = (Tuple) ev.getData();
        MyStatistics.getInstance().setMyCountTotalTuple(1);
        addTotalTuples(1);
        LogHelper.addTotalTuples(aTuple.getAppId(), 1);
        if (!CMFogMobileController.getMobileDevices().contains(this)) {
            return;
        }

        if (aTuple.getInitialTime() == -1) {
            MyStatistics.getInstance().getTupleLatency().put(aTuple.getMyTupleId(), CloudSim.clock() - getUplinkLatency());
            aTuple.setInitialTime(CloudSim.clock() - getUplinkLatency());
        }

        Logger.debug(getName(), "Received tuple " + aTuple.getCloudletId() + " with tupleType = " + aTuple.getTupleType() + "\t| Source : "
                + CloudSim.getEntityName(ev.getSource()) + "|Dest : " + CloudSim.getEntityName(ev.getDestination()));
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);

//        String direction = "UP";
//        if (aTuple.getDirection() == 2) {
//            direction = "DOWN";
//        }
//        System.out.println("Received tuple " + aTuple.getCloudletId() + " [ " + this.getName() + "] with tupleType = " + aTuple.getTupleType() + "\t| DIREC = " + direction + "\t| Source : "
//                + aTuple.getSrcModuleName() + "| Dest : " + aTuple.getDestModuleName() + " | Took: " + (CloudSim.clock() - TimeKeeper.getInstance().getEmitTimes().get(aTuple.getActualTupleId())));
        if (aTuple.getDirection() == Tuple.ACTUATOR) {
            sendTupleToActuator(aTuple);
            return;
        }

        if ((isMigStatus() || isHandoffStatus())) {
            MyStatistics.getInstance().setMyCountLostTuple(1);
            addTotalLostTuples(1);
            //System.out.println(isHandoffStatus() + " <- (MD) -> " + isMigStatus());
            MonitoringAgent.addlostTuple();
            LogHelper.addLostTuples(aTuple.getAppId(), 1);
            saveLostTupple(String.valueOf(CloudSim.clock()), aTuple.getUserId() + "mdlostTupple.txt");
        } else {
            if (getHost().getVmList().size() > 0) {
                for (Vm vm : getHost().getVmList()) {
                    final AppModule operator = (AppModule) vm;//getHost().getVmList().get(index);
                    if (CloudSim.clock() > 0) {
                        getHost().getVmScheduler().deallocatePesForVm(operator);
                        getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>() {
                            protected static final long serialVersionUID = 1L;

                            {
                                add((double) getHost().getTotalMips());
                            }
                        });
                    }

                    break;
                }

            }

            if (appToModulesMap.containsKey(aTuple.getAppId())) {
                if (appToModulesMap.get(aTuple.getAppId()).contains(aTuple.getDestModuleName())) {
                    int vmId = -1;
                    for (Vm vm : getHost().getVmList()) {
                        if (((AppModule) vm).getName().equals(aTuple.getDestModuleName())) {
                            vmId = vm.getId();
                        }
                    }
                    if (vmId < 0
                            || (aTuple.getModuleCopyMap().containsKey(aTuple.getDestModuleName())
                            && aTuple.getModuleCopyMap().get(aTuple.getDestModuleName()) != vmId)) {
                        return;
                    }
                    aTuple.setVmId(vmId);
                    updateTimingsOnReceipt(aTuple);
                    executeTuple(ev, aTuple.getDestModuleName());
                    // System.out.println("Finishing TUPLE @MD: " + aTuple.getMyTupleId());
                    CMFog.msgQueue.remove(Integer.valueOf(aTuple.getMyTupleId()));
                    return;
                }
            }
            if (aTuple.getDirection() == Tuple.UP) {
                sendUp(aTuple);
            } else {
                MyStatistics.getInstance().setMyCountLostTuple(1);
                addTotalLostTuples(1);
                MonitoringAgent.addlostTuple();
                saveLostTupple(String.valueOf(CloudSim.clock()), aTuple.getUserId() + "mdlostTupple.txt");
                System.out.println("MOBILE DEVICE GETTING WRONG MESSAGE - SEVERE ROUTE PROBLEM");
                System.exit(0);
            }
        }
    }

    private void sendPeriodicTuple(SimEvent ev) {
        AppEdge edge = (AppEdge) ev.getData();
        String srcModule = edge.getSource();
        AppModule module = null;
        for (Vm vm : getHost().getVmList()) {
            //	if(vm.getVmm().equals("Xen")){
            if (((AppModule) vm).getName().equals(srcModule)) {
                module = (AppModule) vm;
                break;
            }
            //}
        }
        if (module == null) {
            return;
        }

        int instanceCount = getModuleInstanceCount().get(module.getAppId()).get(srcModule);

        /*
		 * Since tuples sent through a DOWN application edge are anyways broadcasted, only UP tuples are replicated
         */
        for (int i = 0; i < ((edge.getDirection() == Tuple.UP) ? instanceCount : 1); i++) {
            Tuple tuple = applicationMap.get(module.getAppId()).createTuple(edge, getId());
            updateTimingsOnSending(tuple);
            sendToSelf(tuple);
        }
        send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
    }

    private void updateModuleInstanceCount(SimEvent ev) {
        ModuleLaunchConfig config = (ModuleLaunchConfig) ev.getData();
        String appId = config.getModule().getAppId();
        if (!moduleInstanceCount.containsKey(appId)) {
            moduleInstanceCount.put(appId, new HashMap<String, Integer>());
        }
        moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
        // System.out.println(getName() + " Creating " + config.getInstanceCount() + " instances of module " + config.getModule().getName());
    }

    private void manageResources(SimEvent ev) {
        //		updateEnergyConsumption();
        send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
    }

    public float getMaxServiceValue() {
        return maxServiceValue;
    }

    public void setMaxServiceValue(float maxServiceValue) {
        this.maxServiceValue = maxServiceValue;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public Coordinate getFutureCoord() {//myiFogSim
        return futureCoord;
    }

    public void setFutureCoord(int coordX, int coordY) { //myiFogSim
        this.futureCoord.setCoordX(coordX);
        this.futureCoord.setCoordY(coordY);
    }

    //	public double getDistanceAp() {
    //		return distanceAp;
    //	}
    //
    //	public void setDistanceAp(double distanceAp) {
    //		this.distanceAp = distanceAp;
    //	}
    public FogDevice getSourceServerCloudlet() {
        return sourceServerCloudlet;
    }

    public void setSourceServerCloudlet(FogDevice sourceServerCloudlet) {
        if (sourceServerCloudlet != null) {
            System.out.println("-----> Setting " + getName() + " ServerCloudlet to " + sourceServerCloudlet.getName());
        } else {
            System.out.println("-----> Setting " + getName() + " ServerCloudlet to {NULL}");
        }
        this.sourceServerCloudlet = sourceServerCloudlet;
    }

    public FogDevice getDestinationServerCloudlet() {
        return destinationServerCloudlet;
    }

    public void setDestinationServerCloudlet(FogDevice destinationServerCloudlet) {
        this.destinationServerCloudlet = destinationServerCloudlet;
    }

    public ApDevice getSourceAp() {
        return sourceAp;
    }

    public void setSourceAp(ApDevice sourceAp) {
        this.sourceAp = sourceAp;
    }

    public ApDevice getDestinationAp() {
        return destinationAp;
    }

    public void setDestinationAp(ApDevice destinationAp) {
        this.destinationAp = destinationAp;
    }

    public Vm getVmMobileDevice() {
        return vmMobileDevice;
    }

    public void setVmMobileDevice(Vm vmMobileDevice) {
        this.vmMobileDevice = vmMobileDevice;
    }

    public double getVmSize() {
        return vmSize;
    }

    public void setVmSize(double vmSize) {
        this.vmSize = vmSize;
    }

    public double getMigTime() {
        return migTime;
    }

    public void setMigTime(double d) {
        this.migTime = d;
    }

    public Set<MobileSensor> getSensors() {
        return sensors;
    }

    public void setSensors(Set<MobileSensor> sensors) {
        this.sensors = sensors;
    }

    public Set<MobileActuator> getActuators() {
        return actuators;
    }

    public void setActuators(Set<MobileActuator> actuators) {
        this.actuators = actuators;
    }

    public boolean isMigStatus() {
        return migStatus;
    }

    public void setMigStatus(boolean migStatus) {
        this.migStatus = migStatus;
    }

    public boolean isMigStatusLive() {
        return migStatusLive;
    }

    public void setMigStatusLive(boolean migStatusLive) {
        this.migStatusLive = migStatusLive;
    }

    public double getTempSimulation() {
        return tempSimulation;
    }

    public void setTempSimulation(double tempSimulation) {
        this.tempSimulation = tempSimulation;
    }

    public boolean isHandoffStatus() {
        return handoffStatus;
    }

    public void setHandoffStatus(boolean handoffStatus) {
        System.out.println("Setting handoff status to: " + handoffStatus);
        this.handoffStatus = handoffStatus;
    }

    public double getTimeFinishHandoff() {
        return timeFinishHandoff;
    }

    public void setTimeFinishHandoff(double timeFinishHandoff) {
        this.timeFinishHandoff = timeFinishHandoff;
    }

    public double getTimeFinishDeliveryVm() {
        return timeFinishDeliveryVm;
    }

    public void setTimeFinishDeliveryVm(double timeFinishDeliveryVm) {
        this.timeFinishDeliveryVm = timeFinishDeliveryVm;
    }

    public FogDevice getVmLocalServerCloudlet() {
        return vmLocalServerCloudlet;
    }

    protected String searchForLastMigration = null;

    public String getSearchForLastMigration() {
        return searchForLastMigration;
    }

    public void setSearchForLastMigration(String searchForLastMigration) {
        this.searchForLastMigration = searchForLastMigration;
    }

    public void setVmLocalServerCloudlet(FogDevice vmLocalServerCloudlet) {
        if (vmLocalServerCloudlet != null) {
            System.out.println("setting VMlocalSC (" + myId + ") to " + vmLocalServerCloudlet.getMyId());
        } else {
            System.out.println("setting VMlocalSC to null");
        }
        this.vmLocalServerCloudlet = vmLocalServerCloudlet;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean isLockedToHandoff() {
        return lockedToHandoff;
    }

    public void setLockedToHandoff(boolean LockedToHandoff) {
        this.lockedToHandoff = LockedToHandoff;
    }

    public boolean isLockedToMigration() {
        return lockedToMigration;
    }

    public void setLockedToMigration(boolean lockedToMigration) {
        this.lockedToMigration = lockedToMigration;
    }

    public boolean isAbortMigration() {
        return abortMigration;
    }

    public void setAbortMigration(boolean abortMigration) {
        this.abortMigration = abortMigration;
    }

    public VmMigrationTechnique getMigrationTechnique() {
        return migrationTechnique;
    }

    public void setMigrationTechnique(VmMigrationTechnique migrationTechnique) {
        this.migrationTechnique = migrationTechnique;
    }

    public boolean isMigPoint() {
        return migPoint;
    }

    public void setMigPoint(boolean migPoint) {
        this.migPoint = migPoint;
    }

    public boolean isMigZone() {
        return migZone;
    }

    public void setMigZone(boolean migZone) {
        this.migZone = migZone;
    }

    public boolean isPostCopyStatus() {
        return postCopyStatus;
    }

    public void setPostCopyStatus(boolean postCopyStatus) {
        this.postCopyStatus = postCopyStatus;
    }

    public double getTimeStartLiveMigration() {
        return timeStartLiveMigration;
    }

    public void setTimeStartLiveMigration(double timeStartLiveMigration) {
        this.timeStartLiveMigration = timeStartLiveMigration;
    }

    public void setNextServerClouletId(int i) {
        // TODO Auto-generated method stub
    }

    public int getNextServerClouletId() {
        // TODO Auto-generated method stub
        return 1;
    }

    public void checkStatusAP() {
        MobileDevice aMobileDevice = this;
        if (!CMFogHelper.isConnectedToNetwork(aMobileDevice)) {
            connectToAP();
        } else {
            checkAPCoverage();
            //if (!CMFogHelper.isConnectedToNetwork(aMobileDevice)) {
            //  connectToAP();
            //}
//            if (!CMFogHelper.isConnectedToNetwork(aMobileDevice)) {
//                connectToNearestAP();
//            }
        }
    }

    private void connectToAP() {
        MobileDevice aMobileDevice = this;
        int anAPtoConnect = DecisionHelper.chooseHandoffAP2(CMFog.accessPoints, this);
        ApDevice anAcessPoint = CMFog.accessPoints.get(anAPtoConnect);

        if (anAcessPoint.getMaxSmartThing() > anAcessPoint.getSmartThings().size()) {
            double delay = SimulationParameters.getRand().nextDouble();
            aMobileDevice.setSourceAp(anAcessPoint);
            anAcessPoint.setSmartThings(aMobileDevice, Policies.ADD);
            NetworkTopology.addLink(anAcessPoint.getId(), aMobileDevice.getId(), aMobileDevice.getUplinkBandwidth(), delay);
            LogMobile.debug("ApDevice.java", aMobileDevice.getName() + " was connected to " + aMobileDevice.getSourceAp().getName());
            anAcessPoint.setUplinkLatency(anAcessPoint.getUplinkLatency() + delay);
            System.out.println("================================= NEW CONNECTION TO MOBILE DEVICE =================================");
            //L2POI
            aMobileDevice.getSourceAp().getServerCloudlet().connectServerCloudletSmartThing(aMobileDevice);
            CMFog.modulesLocation.put("client" + aMobileDevice.getMyId(), aMobileDevice.getSourceServerCloudlet());
            MyStatistics.getInstance().setTotalHandoff(1);
            saveHandOff(aMobileDevice);
            //CMFogHelper.emitTuple(aMobileDevice);
        }
    }

    private boolean earlyMigration = false;
    private boolean afterVerticalMig = false;

    public boolean isEarlyMigration() {
        return earlyMigration;
    }

    public void setEarlyMigration(boolean earlyMigration) {
        this.earlyMigration = earlyMigration;
    }

    public boolean isAfterVerticalMig() {
        return afterVerticalMig;
    }

    public void setAfterVerticalMig(boolean afterVerticalMig) {
        this.afterVerticalMig = afterVerticalMig;
    }

    public void checkAPCoverage() {
        MobileDevice aMobileDevice = this;
        if (!CMFogHelper.isInsideCoverage(aMobileDevice.getCoord(), aMobileDevice.getSourceAp().getCoord())) {
            System.out.println("================================= OUT OF AP's COVERAGE -> HANDOFFING " + getName() + "=================================");
            System.out.println(aMobileDevice.getNextMigration() - CloudSim.clock());
            if (!aMobileDevice.isLockedToMigration()) {
                if (aMobileDevice.getVmLocalServerCloudlet().getLayer() == 1) {
                    System.out.println("=================== EARLY MIG CHECK L1" + getName() + "===================");
                    earlyMigration = true;
                    aMobileDevice.getVmLocalServerCloudlet().invokeDecisionMigration(aMobileDevice);
                } else if (aMobileDevice.getVmLocalServerCloudlet().getLayer() == 2) {
                    System.out.println("=================== EARLY MIG CHECK L2" + getName() + "===================");
                    Integer actualCloudlet = aMobileDevice.getVmLocalServerCloudlet().getId();
                    ApDevice apDestination = CMFog.accessPoints.get(chooseHandoffAP2(CMFog.accessPoints, aMobileDevice));
                    Integer destinationCloudlet = CMFogHelper.getCloudletById(apDestination.getParentId()).getParentId();
                    System.out.println("comparing: " + aMobileDevice.getVmLocalServerCloudlet().getName() + " and " + CMFogHelper.getCloudletById(destinationCloudlet).getName());
                    if (!actualCloudlet.equals(destinationCloudlet)) {
                        System.out.println("=================== CHECK PASSED L2" + getName() + "===================");
                        earlyMigration = true;
                        aMobileDevice.getVmLocalServerCloudlet().invokeDecisionMigration(aMobileDevice);
                    }
                }
            }
            earlyMigration = false;
            aMobileDevice.performHandoff();
        }

    }

    private static void saveHandOff(MobileDevice st) {
        //System.out.println("HANDOFF " + st.getMyId() + " Position: " + st.getCoord().getCoordX() + ", " + st.getCoord().getCoordY() + " Direction: " + st.getDirection() + " Speed: " + st.getSpeed());
        try ( FileWriter fw = new FileWriter(st.getMyId() + "handoff.txt", true);  BufferedWriter bw = new BufferedWriter(fw);  PrintWriter out = new PrintWriter(bw)) {
            if (st.getDestinationAp() == null) {
                out.println(st.getMyId() + "\t" + CloudSim.clock() + "\t" + st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY() + "\t" + st.getDirection() + "\t" + st.getSpeed() + "\t" + st.getSourceAp() + "\t" + " - FROM DC");
            } else {
                out.println(st.getMyId() + "\t" + CloudSim.clock() + "\t" + st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY() + "\t" + st.getDirection() + "\t" + st.getSpeed() + "\t" + st.getSourceAp() + "\t" + st.getDestinationAp());
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
