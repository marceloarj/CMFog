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
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.NetworkTopology;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.cmfog.CMFog;
import org.fog.cmfog.helpers.CMFogHelper;
import org.fog.cmfog.CMFogMobileController;
import org.fog.cmfog.MonitoringAgent;
import org.fog.cmfog.helpers.DecisionHelper;
import org.fog.cmfog.helpers.LogHelper;
import org.fog.cmfog.helpers.NetworkHelper;
import org.fog.cmfog.SimulationParameters;
import org.fog.localization.Coordinate;//myiFogSim
import org.fog.localization.Distances;
import org.fog.cmfog.mobilityprediction.MobilityPrediction;
import org.fog.cmfog.enumerate.Algorithm;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.ModuleLaunchConfig;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.vmmigration.BeforeMigration;
import org.fog.vmmigration.DecisionMigration;
import org.fog.vmmigration.MyStatistics;
import org.fog.vmmigration.Service;

import org.fog.vmmobile.LogMobile;
import org.fog.vmmobile.constants.MaxAndMin;
import org.fog.vmmobile.constants.MobileEvents;
import org.fog.vmmobile.constants.Policies;

public class FogDevice extends PowerDatacenter {
    
    protected Queue<Tuple> northTupleQueue;
    protected Queue<Pair<Tuple, Integer>> southTupleQueue;
    
    protected List<String> activeApplications;
    protected ArrayList<String[]> path;
    
    protected Map<String, Application> applicationMap;
    protected Map<String, List<String>> appToModulesMap;
    protected Map<Integer, Double> childToLatencyMap;
    
    protected Map<Integer, Integer> cloudTrafficMap;
    
    protected double lockTime;

    /**
     * ID of the parent Fog Device
     */
    protected int parentId;
    protected int volatilParentId;

    /**
     * ID of the Controller
     */
    protected int controllerId;
    /**
     * IDs of the children Fog devices
     */
    protected List<Integer> childrenIds;
    
    protected Map<Integer, List<String>> childToOperatorsMap;

    /**
     * Flag denoting whether the link southwards from this FogDevice is busy
     */
    protected boolean isSouthLinkBusy;

    /**
     * Flag denoting whether the link northwards from this FogDevice is busy
     */
    protected boolean isNorthLinkBusy;
    
    protected double uplinkBandwidth;
    protected double downlinkBandwidth;
    protected double uplinkLatency;
    protected List<Pair<Integer, Double>> associatedActuatorIds;
    
    protected double energyConsumption;
    protected double lastUtilizationUpdateTime;
    protected double lastUtilization;
    private int level;
    
    protected double ratePerMips;
    
    protected double totalCost;
    
    protected Map<String, Map<String, Integer>> moduleInstanceCount;
    
    protected Coordinate coord;// = new Coordinate();//myiFogSim
    protected Set<ApDevice> apDevices;//myiFogSim
    protected Set<MobileDevice> smartThings;
    protected Set<MobileDevice> smartThingsWithVm;
    protected Set<FogDevice> serverCloudlets;
    protected boolean available;
    protected Service service;
    //	protected NetworkBwServerCloulets networkBwServerCloulets;
    private HashMap<FogDevice, Double> netServerCloudlets = new HashMap<>();
    protected DecisionMigration migrationStrategy;
    protected int policyReplicaVM;
    private FogDevice serverCloudletToVmMigrate;
    protected BeforeMigration beforeMigration;
    protected int startTravelTime;
    protected int travelTimeId;
    protected int travelPredicTime;
    protected int mobilityPrecitionError;
    
    protected int myId;
    protected int layer = 1;
    protected double migrationRate;
    protected List<FogDevice> childrenCloudlets = new ArrayList<>();
    
    public double getMigrationRate() {
        return migrationRate;
    }
    
    public void setMigrationRate(double migrationRate) {
        this.migrationRate = migrationRate;
    }
    
    public List<FogDevice> getChildrenCloudlets() {
        return childrenCloudlets;
    }
    
    public void setChildrenCloudlets(List<FogDevice> childrenCloudlets) {
        this.childrenCloudlets = childrenCloudlets;
    }
    
    public int getLayer() {
        return layer;
    }
    
    public void setLayer(int layer) {
        this.layer = layer;
        if (layer == 1) {
            setMigrationRate(1.5);
        } else if (layer == 2) {
            setMigrationRate(1.0);
        } else {
            setMigrationRate(0.0);
        }
    }
    
    public int getMyId() {
        return myId;
    }
    
    public void setMyId(int myId) {
        this.myId = myId;
    }
    
    public HashMap<FogDevice, Double> getNetServerCloudlets() {
        return netServerCloudlets;
    }
    
    public void setNetServerCloudlets(HashMap<FogDevice, Double> netServerCloudlets) {
        this.netServerCloudlets = netServerCloudlets;
    }
    
    public Service getService() {
        return service;
    }
    
    public void setService(Service service) {
        this.service = service;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    public Set<MobileDevice> getSmartThings() {//myiFogSim
        return smartThings;
    }
    
    public void setSmartThings(MobileDevice st, int action) {//myiFogSim
        if (action == Policies.ADD) {
            this.smartThings.add(st);
        } else {
            this.smartThings.remove(st);
        }
    }
    
    public Set<ApDevice> getApDevices() { //myiFogSim
        return apDevices;
    }
    
    public void setApDevices(ApDevice ap, int action) {//myiFogSim
        if (action == Policies.ADD) {
            this.apDevices.add(ap);
        } else {
            this.apDevices.remove(ap);
        }
    }
    
    public Coordinate getCoord() {//myiFogSim
        return coord;
    }
    
    public void setCoord(int coordX, int coordY) { //myiFogSim
        this.coord.setCoordX(coordX);
        this.coord.setCoordY(coordY);
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
    
    public void setMobilityPredictionError(int mobilityPrecitionError) {
        this.mobilityPrecitionError = mobilityPrecitionError;
    }
    
    public FogDevice() {//myiFogSim

    }
    
    public FogDevice(String name, int coordX, int coordY, int id) { //myiFogSim
        //	public FogDevice(String name, Coordinate coord, int coordX, int coordY, int id) { //myiFogSim
        // TODO Auto-generated constructor stub
        super(name);
        this.coord = new Coordinate();

        //	this.setName(name);
        //	coord.setPositions(this.getName(),coordX, coordY);
        this.setCoord(coordX, coordY);
        this.setMyId(id);
        smartThings = new HashSet<>();
        apDevices = new HashSet<>();
        netServerCloudlets = new HashMap<>();
        serverCloudlets = new HashSet<>();
        this.setAvailable(true);
        
    }
    
    public FogDevice(String name) {
        super(name);
    }
    
    public FogDevice( //myiFogSim - for ServerCloulet -> it addition Service in the Construction
            String name,
            FogDeviceCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval,
            double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips,
            int coordX, int coordY, int id, Service service,
            DecisionMigration migrationStrategy,
            int policyReplicaVM,
            BeforeMigration beforeMigration
    ) throws Exception {
        
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
        
        this.coord = new Coordinate();
        //	this.setName(name);
        //	coord.setPositions(this.getName(),coordX, coordY);
        this.setCoord(coordX, coordY);
        this.setMyId(id);
        smartThings = new HashSet<>();
        smartThingsWithVm = new HashSet<>();
        apDevices = new HashSet<>();
        netServerCloudlets = new HashMap<>();
        setVolatilParentId(-1);
        this.setAvailable(true);
        this.setService(service);
        
        setBeforeMigrate(beforeMigration);
        setPolicyReplicaVM(policyReplicaVM);
        setMigrationStrategy(migrationStrategy);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setServerCloudletToVmMigrate(null);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        setPath(new ArrayList<String[]>());
        setTravelTimeId(-1);
        setTravelPredicTime(0);
        setMobilityPredictionError(0);
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());
        
        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);
        
        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());
        
        this.cloudTrafficMap = new HashMap<Integer, Integer>();
        
        this.lockTime = 0;
        
        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());
    }
    
    public FogDevice( //myiFogSim - for ServerCloulet -> it addition Service in the Construction
            String name,
            FogDeviceCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval,
            double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips,
            int coordX, int coordY, int id, Service service,
            DecisionMigration migrationStrategy,
            int policyReplicaVM,
            BeforeMigration beforeMigration, int layer
    ) throws Exception {
        
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
        setLayer(layer);
        this.coord = new Coordinate();
        //	this.setName(name);
        //	coord.setPositions(this.getName(),coordX, coordY);
        this.setCoord(coordX, coordY);
        this.setMyId(id);
        smartThings = new HashSet<>();
        smartThingsWithVm = new HashSet<>();
        apDevices = new HashSet<>();
        netServerCloudlets = new HashMap<>();
        setVolatilParentId(-1);
        this.setAvailable(true);
        this.setService(service);
        
        setBeforeMigrate(beforeMigration);
        setPolicyReplicaVM(policyReplicaVM);
        setMigrationStrategy(migrationStrategy);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setServerCloudletToVmMigrate(null);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        setPath(new ArrayList<String[]>());
        setTravelTimeId(-1);
        setTravelPredicTime(0);
        setMobilityPredictionError(0);
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());
        
        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);
        
        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());
        
        this.cloudTrafficMap = new HashMap<Integer, Integer>();
        
        this.lockTime = 0;
        
        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());
    }
    
    public FogDevice( //myiFogSim - for MobileDevice
            String name,
            FogDeviceCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval,
            double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips,
            int coordX, int coordY, int id
    ) throws Exception {
        
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
        
        this.coord = new Coordinate();
        //	this.setName(name);
        //	coord.setPositions(this.getName(),coordX, coordY);
        this.setCoord(coordX, coordY);
        this.setMyId(id);
        smartThings = new HashSet<>();
        smartThingsWithVm = new HashSet<>();
        
        apDevices = new HashSet<>();
        netServerCloudlets = new HashMap<>();
        setVolatilParentId(-1);
        
        this.setAvailable(true);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setServerCloudletToVmMigrate(null);
        
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        setPath(new ArrayList<String[]>());
        setTravelTimeId(-1);
        setTravelPredicTime(0);
        setMobilityPredictionError(0);
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());
        
        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);
        
        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());
        
        this.cloudTrafficMap = new HashMap<Integer, Integer>();
        
        this.lockTime = 0;
        
        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());
    }
    
    public FogDevice(
            String name,
            FogDeviceCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval,
            double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setServerCloudletToVmMigrate(null);
        
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        setPath(new ArrayList<String[]>());
        setTravelTimeId(-1);
        setTravelPredicTime(0);
        setMobilityPredictionError(0);
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());
        
        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);
        
        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());
        
        this.cloudTrafficMap = new HashMap<Integer, Integer>();
        
        this.lockTime = 0;
        
        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());
    }
    
    public FogDevice(
            String name, long mips, int ram,
            double uplinkBandwidth, double downlinkBandwidth, double ratePerMips, PowerModel powerModel) throws Exception {
        super(name, null, null, new LinkedList<Storage>(), 0);
        
        List<Pe> peList = new ArrayList<Pe>();

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000; // host storage
        int bw = 10000;
        
        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                powerModel
        );
        
        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);
        
        setVmAllocationPolicy(new AppModuleAllocationPolicy(hostList));
        
        String arch = Config.FOG_DEVICE_ARCH;
        String os = Config.FOG_DEVICE_OS;
        String vmm = Config.FOG_DEVICE_VMM;
        double time_zone = Config.FOG_DEVICE_TIMEZONE;
        double cost = Config.FOG_DEVICE_COST;
        double costPerMem = Config.FOG_DEVICE_COST_PER_MEMORY;
        double costPerStorage = Config.FOG_DEVICE_COST_PER_STORAGE;
        double costPerBw = Config.FOG_DEVICE_COST_PER_BW;
        
        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);
        
        setCharacteristics(characteristics);
        
        setLastProcessTime(0.0);
        setVmList(new ArrayList<Vm>());
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host1 : getCharacteristics().getHostList()) {
            host1.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        setPath(new ArrayList<String[]>());
        setTravelTimeId(-1);
        setTravelPredicTime(0);
        setMobilityPredictionError(0);
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        
        getCharacteristics().setId(super.getId());
        
        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);
        
        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());
        
        this.cloudTrafficMap = new HashMap<Integer, Integer>();
        
        this.lockTime = 0;
        
        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setChildToLatencyMap(new HashMap<Integer, Double>());
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
    }

    /**
     * Overrides this method when making a new and different type of resource.
     * <br>
     * <b>NOTE:</b> You do not need to override {@link #body()} method, if you
     * use this method.
     *
     * @pre $none
     * @post $none
     */
    @Override
    protected void registerOtherEntity() {
        
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
            case MobileEvents.MAKE_DECISION_MIGRATION:
                invokeDecisionMigration(ev);
                break;
            case MobileEvents.TO_MIGRATION:
                invokeBeforeMigration(ev);
                break;
            case MobileEvents.NO_MIGRATION:
                invokeNoMigration(ev);
                break;
            case MobileEvents.START_MIGRATION:
                invokeStartMigration(ev);
                break;
            case MobileEvents.ABORT_MIGRATION:
                invokeAbortMigration(ev);
                break;
            case MobileEvents.REMOVE_VM_OLD_CLOUDLET:
                removeVmOldServerCloudlet(ev);
                break;
            case MobileEvents.ADD_VM_NEW_CLOUDLET:
                addVmNewServerCloudlet(ev);
                break;
            case MobileEvents.DELIVERY_VM:
                deliveryVM(ev);
                break;
            case MobileEvents.CONNECT_ST_TO_SC:
                connectServerCloudletSmartThing(ev);
                break;
            case MobileEvents.DESCONNECT_ST_TO_SC:
                desconnectServerCloudletSmartThing(ev);
                break;
            case MobileEvents.UNLOCKED_MIGRATION:
                unLockedMigration(ev);
                break;
            case MobileEvents.VM_MIGRATE:
                myVmMigrate(ev);
                break;
            case MobileEvents.SET_MIG_STATUS_TRUE:
                migStatusToLiveMigration(ev);
                break;
            
            default:
                break;
        }
    }
    
    private void myVmMigrate(SimEvent ev) {
        MobileDevice aMobileDevice = (MobileDevice) ev.getData();
        Application app = aMobileDevice.getVmLocalServerCloudlet().applicationMap.get("MyApp_vr_game" + aMobileDevice.getMyId());
        if (app == null) {
            System.out.println("Clock: " + CloudSim.clock() + " - FogDevice.java - App == Null");
            System.exit(0);
        }
        getApplicationMap().put(app.getAppId(), app);
        if (aMobileDevice.getVmLocalServerCloudlet().getApplicationMap().remove(app.getAppId()) == null) {
            System.out.println("FogDevice.java - applicationMap did not remove. return == null");
            System.exit(0);
        }
        
        CMFogMobileController mobileController = (CMFogMobileController) CloudSim.getEntity("MobileController");
        
        mobileController.getModuleMapping().addModuleToDevice(((AppModule) aMobileDevice.getVmMobileDevice()).getName(), getName(), 1);
        mobileController.getModuleMapping().getModuleMapping().remove(aMobileDevice.getVmLocalServerCloudlet().getName());
        if (!mobileController.getModuleMapping().getModuleMapping().containsKey(getName())) {
            mobileController.getModuleMapping().getModuleMapping().put(getName(), new HashMap<String, Integer>());
            mobileController.getModuleMapping().getModuleMapping().get(getName()).put("AppModuleVm_" + aMobileDevice.getName(), 1);
        }
        mobileController.submitApplicationMigration(aMobileDevice, app, 1);
        sendNow(mobileController.getId(), MobileEvents.APP_SUBMIT_MIGRATE, app);
    }
    
    private void unLockedMigration(SimEvent ev) {
        // TODO Auto-generated method stub
        MobileDevice smartThing = (MobileDevice) ev.getData();
        smartThing.setLockedToMigration(false);
        // smartThing.setTimeFinishDeliveryVm(-1);
        LogMobile.debug("FogDevice.java", smartThing.getName() + " had the migration unlocked");
    }
    
    private void saveConnectionCloudletSmartThing(MobileDevice st, String conType) {
        
        try ( FileWriter fw = new FileWriter(st.getMyId() + "ConClSmTh.txt", true);  BufferedWriter bw = new BufferedWriter(fw);  PrintWriter out = new PrintWriter(bw)) {
            out.println(CloudSim.clock() + "\t" + st.getMyId() + "\t" + conType);
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
    
    private void desconnectServerCloudletSmartThing(SimEvent ev) {
        MobileDevice aMobileDevice = (MobileDevice) ev.getData();
        desconnectServerCloudletSmartThing(aMobileDevice);
        //MyStatistics.getInstance().startWithoutConnetion(aMobileDevice.getMyId(), CloudSim.clock());
        saveConnectionCloudletSmartThing(aMobileDevice, "desconnectServerCloudletSmartThing");
    }
    
    private void connectServerCloudletSmartThing(SimEvent ev) {
        // TODO Auto-generated method stub
        MobileDevice smartThing = (MobileDevice) ev.getData();
        connectServerCloudletSmartThing(smartThing);
        //MyStatistics.getInstance().finalWithoutConnection(smartThing.getMyId(), CloudSim.clock());
        saveConnectionCloudletSmartThing(smartThing, "connectServerCloudletSmartThing");
        
        CMFog.modulesLocation.put("client" + smartThing.getMyId(), smartThing.getSourceServerCloudlet());
    }
    
    private void addVmNewServerCloudlet(SimEvent ev) {
        // TODO Auto-generated method stub

    }
    
    private void removeVmOldServerCloudlet(SimEvent ev) {
        // TODO Auto-generated method stub

    }
    
    private void invokeAbortMigration(SimEvent ev) {
        MobileDevice aMobileDevice = (MobileDevice) ev.getData();
        System.out.println("*_*_*_*_*_*_*_*_*_*_*_*_*_ABORT MIGRATION -> beforeMigration*_*_*_*_*_*_*_*_*_*_*_*: " + aMobileDevice.getName());
        MyStatistics.getInstance().getInitialWithoutVmTime().remove(aMobileDevice.getMyId());
        MyStatistics.getInstance().getInitialTimeDelayAfterNewConnection().remove(aMobileDevice.getMyId());
        MyStatistics.getInstance().getInitialTimeWithoutConnection().remove(aMobileDevice.getMyId());
        aMobileDevice.setMigStatus(false);
        aMobileDevice.setPostCopyStatus(false);
        aMobileDevice.setMigStatusLive(false);
        aMobileDevice.setLockedToMigration(false);
        //aMobileDevice.setTimeFinishDeliveryVm(-1.0);
        aMobileDevice.setAbortMigration(true);
        aMobileDevice.setDestinationServerCloudlet(aMobileDevice.getVmLocalServerCloudlet());
    }
    
    public boolean connectServerCloudletSmartThing(MobileDevice st) {
        st.setSourceServerCloudlet(this);

        //System.out.println("check: " + st.getSourceServerCloudlet().getName());
        setSmartThings(st, Policies.ADD);
        //		st.setVmLocalServerCloudlet(this);
        st.setParentId(getId());
        double latency = st.getUplinkLatency();
        System.out.println(st.getName() + " to " + getName() + " with latency = " + latency);
        getChildToLatencyMap().put(st.getId(), latency);
        addChild(st.getId());
        //		for(MobileSensor s: st.getSensors()){
        //			addChild(s.getId());
        //			latency = s.getLatency();
        //			getChildToLatencyMap().put(s.getId(), latency);
        //		}
        //		for(MobileActuator a: st.getActuators()){
        //			addChild(a.getId());
        //			latency = a.getLatency();
        //			getChildToLatencyMap().put(a.getId(), latency);
        //		}
        setUplinkLatency(getUplinkLatency() + 0.123812950236);//
        //		this.getChildrenIds().add(st.getId());
        //		NetworkTopology.addLink(this.getId(), st.getId(), st.getDownlinkBandwidth(), 0.05);//0.02+0.03
        LogMobile.debug("FogDevice.java", st.getName() + " was connected to " + getName());
        
        return true;
    }
    
    public boolean desconnectServerCloudletSmartThing(MobileDevice st) {
        //		for(MobileSensor s: st.getSensors()){
        //			st.getSourceServerCloudlet().getChildrenIds().remove((Integer)s.getId());
        //		}
        //		for(MobileActuator a: st.getActuators()){
        //			st.getSourceServerCloudlet().getChildrenIds().remove((Integer)a.getId());
        //		}
        setSmartThings(st, Policies.REMOVE); //it'll remove the smartThing from serverCloudlets-smartThing's set
        System.out.println("-----> Removing " + st.getName() + " from " + getName());
        st.setSourceServerCloudlet(null);
//		NetworkTopology.addLink(this.getId(), st.getId(), 0.0, 0.0);
        setUplinkLatency(getUplinkLatency() - 0.123812950236);
        removeChild(st.getId());
        LogMobile.debug("FogDevice.java", st.getName() + " was desconnected to " + getName());
        return true;
        
    }
    
    private void invokeStartMigration(SimEvent ev) {
        MobileDevice aMobileDevice = (MobileDevice) ev.getData();
        if (CMFogMobileController.getMobileDevices().contains(aMobileDevice)) {
            if (!aMobileDevice.isAbortMigration()) {
                if (aMobileDevice.getSourceAp() != null) {
                    int sourceCloudlet = getId();
                    int destionationCloudlet = aMobileDevice.getDestinationServerCloudlet().getId();
                    System.out.println("-----> Migrating " + aMobileDevice.getName() + "'s VM");
                    Double delay = 1.0 + getNetworkDelay(sourceCloudlet, destionationCloudlet);
                    send(aMobileDevice.getVmLocalServerCloudlet().getId(), delay, MobileEvents.DELIVERY_VM, aMobileDevice);
                    LogMobile.debug("FogDevice.java", aMobileDevice.getName() + " was scheduled the DELIVERY_VM  from "
                            + aMobileDevice.getVmLocalServerCloudlet().getName() + " to "
                            + aMobileDevice.getDestinationServerCloudlet().getName());
                    sendNow(aMobileDevice.getDestinationServerCloudlet().getId(), MobileEvents.VM_MIGRATE, aMobileDevice);
                    Map<String, Object> ma = new HashMap<String, Object>();
                    ma.put("vm", aMobileDevice.getVmMobileDevice());
                    ma.put("host", aMobileDevice.getDestinationServerCloudlet().getHost());
                    sendNow(aMobileDevice.getVmLocalServerCloudlet().getId(), CloudSimTags.VM_MIGRATE, ma);
                    LogMobile.debug("FogDevice.java", "CloudSim.VM_MIGRATE was scheduled  to VM#: " + aMobileDevice.getVmMobileDevice().getId() + " HOST#: "
                            + aMobileDevice.getDestinationServerCloudlet().getHost().getId());
                } else {
                    System.out.println("------> No AP to finish START MIGRATION");
                    sendNow(aMobileDevice.getVmLocalServerCloudlet().getId(), MobileEvents.ABORT_MIGRATION, aMobileDevice);
                }
            } else {
                aMobileDevice.setAbortMigration(false);
            }
        } else {
            LogMobile.debug("FogDevice.java", aMobileDevice.getName() + " was excluded from List of SmartThings!");
        }
    }
    
    private void deliveryVM(SimEvent ev) {// pode ser um bom ponto de medição
        MobileDevice aMobileDevice = (MobileDevice) ev.getData();
        if (CMFogMobileController.getMobileDevices().contains(aMobileDevice)) {
            
            LogMobile.debug("FogDevice.java", "DELIVERY VM: " + aMobileDevice.getName() + " (id: " + aMobileDevice.getId() + ") from " + aMobileDevice.getVmLocalServerCloudlet().getName()
                    + " to " + aMobileDevice.getDestinationServerCloudlet().getName());
            System.out.println("----> Delivering " + aMobileDevice.getName() + "' VM from " + aMobileDevice.getVmLocalServerCloudlet().getName() + " to " + aMobileDevice.getDestinationServerCloudlet().getName());
            
            aMobileDevice.getVmLocalServerCloudlet().setSmartThingsWithVm(aMobileDevice, Policies.REMOVE);
            aMobileDevice.setVmLocalServerCloudlet(aMobileDevice.getDestinationServerCloudlet());
            aMobileDevice.setDestinationServerCloudlet(null);
            aMobileDevice.getVmLocalServerCloudlet().setSmartThingsWithVm(aMobileDevice, Policies.ADD);
            CMFog.modulesLocation.put("AppModuleVm_" + aMobileDevice.getName(), aMobileDevice.getVmLocalServerCloudlet());
            
            aMobileDevice.setMigStatus(false);
            aMobileDevice.setPostCopyStatus(false);
            aMobileDevice.setMigStatusLive(false);
            
            if (aMobileDevice.getSourceServerCloudlet() == null) {
                //aMobileDevice.setSourceServerCloudlet(aMobileDevice.getVmLocalServerCloudlet());
                System.out.println("CRASH " + aMobileDevice.getMyId() + "\t source c " + aMobileDevice.getSourceServerCloudlet()
                        + "\t local server " + aMobileDevice.getVmLocalServerCloudlet());
            }

            //float migrationLocked = (aMobileDevice.getVmMobileDevice().getSize() * (aMobileDevice.getSpeed() + 1)) + 20000;
//            if (migrationLocked < aMobileDevice.getTravelPredicTime() * 1000) {
//                migrationLocked = aMobileDevice.getTravelPredicTime() * 1000;
//            }
            send(aMobileDevice.getVmLocalServerCloudlet().getId(), 3000, MobileEvents.UNLOCKED_MIGRATION, aMobileDevice);
            MyStatistics.getInstance().countMigration();
            MyStatistics.getInstance().historyMigrationTime(aMobileDevice.getMyId(), aMobileDevice.getMigTime());
            
            aMobileDevice.addMigration();
            
            if (aMobileDevice.getAlgorithm() == Algorithm.TCMFOG) {
                System.out.println(aMobileDevice.getSearchForLastMigration());
                if (aMobileDevice.getSearchForLastMigration().equals("1/0") || aMobileDevice.getSearchForLastMigration().equals("105/100")) {
                    System.out.println(">>>>>>>>>>>>>>>>>> NO ESTIMATED TIME FOR NEXT MIGRATION <<<<<<<<<<<<<<<<<<<<<<<<<<");
                    aMobileDevice.setNextMigration(CloudSim.clock() + 500 * 1000);
                    
                } else {
                    if (aMobileDevice.isVerticalMigration()) {
                        aMobileDevice.setNextMigration(MaxAndMin.MAX_SIMULATION_TIME);
                        aMobileDevice.setAfterVerticalMig(true);
                    } else {
                        double estimatedTime = aMobileDevice.getEstimatedPermTime() * 1000;
                        //double estimatedTime = (aMobileDevice.getEstimatedPermTime() - 10) * 1000;
                        double estimatedMigTime = NetworkHelper.calculateInitialMigTime(aMobileDevice);
                        estimatedMigTime += estimatedMigTime * 0.25;
                        aMobileDevice.setNextMigration(CloudSim.clock() + estimatedTime - estimatedMigTime);
                        System.out.println("SETTING TIME FOR NEXT MIGRATION: " + estimatedTime + " / " + estimatedMigTime);
                        
                    }
                }
            }
            if (aMobileDevice.isVerticalMigration()) {
                aMobileDevice.setVerticalMigration(false);
                aMobileDevice.setLayer(2);
                SimulationParameters.setLayer(2);
            }
            aMobileDevice.performHandoff();
            //sendNow(aMobileDevice.getId(), MobileEvents.MAKE_DECISION_HANDOFF, aMobileDevice);
            aMobileDevice.getMigrationTrack().add(aMobileDevice.getVmLocalServerCloudlet());
        } else {
            LogMobile.debug("FogDevice.java", aMobileDevice.getName() + " was excluded by List of SmartThings! (inside Delivery Vm)");
        }
    }

//VERIFICAR O MIGRATION_TIME
    private void invokeNoMigration(SimEvent ev) {
        // TODO Auto-generated method stub
        MobileDevice smartThing = (MobileDevice) ev.getData();
        
        if (smartThing.isLockedToMigration()) {//isMigStatus()){
            LogMobile.debug("FogDevice.java", "NO MIGRATE: " + smartThing.getName() + " already is in migration Process or the migration is locked");
        } else {
            LogMobile.debug("FogDevice.java", "NO MIGRATE: " + smartThing.getName() + " is not in Migrate");
        }
    }
    
    private void invokeBeforeMigration(SimEvent ev) {
        MobileDevice aMobileDevice = (MobileDevice) ev.getData();
        if (CMFogMobileController.getMobileDevices().contains(aMobileDevice)) {
            double delayProcess = getBeforeMigrate().dataprepare(aMobileDevice);
            double migrationTime = NetworkHelper.calculateMigrationTime(aMobileDevice);
            if (delayProcess >= 0) {
                aMobileDevice.setMigStatus(true);

                //aMobileDevice.setTimeFinishDeliveryVm(-1.0);
                send(aMobileDevice.getVmLocalServerCloudlet().getId(), migrationTime + delayProcess, MobileEvents.START_MIGRATION, aMobileDevice);
                aMobileDevice.setLockedToMigration(true);
            }
        } else {
            sendNow(aMobileDevice.getVmLocalServerCloudlet().getId(), MobileEvents.ABORT_MIGRATION, aMobileDevice);
        }
    }
    
    private void migStatusToLiveMigration(SimEvent ev) {
        // TODO Auto-generated method stub
        MobileDevice smartThing = (MobileDevice) ev.getData();
        sendNow(smartThing.getVmLocalServerCloudlet().getId(), MobileEvents.START_MIGRATION, smartThing);//It'll happen according the Migration Time
    }
    
    private double migrationTimeToLiveMigration(MobileDevice smartThing) {
        // TODO Auto-generated method stub
        double runTime = CloudSim.clock() - smartThing.getTimeStartLiveMigration();
        if (smartThing.getMigTime() > runTime) {
            runTime = smartThing.getMigTime() - runTime;
            return runTime;
        } else {
            return 0;
        }
        
    }
    
    private void invokeDecisionMigration(SimEvent ev) {
        MobileDevice aMobileDevice = (MobileDevice) ev.getData();
        if (aMobileDevice.getSourceAp() != null && DecisionHelper.shouldMigrate(aMobileDevice)) {
            //if (false) {
            LogMobile.debug("FogDevice.java", "Made the decisionMigration for " + aMobileDevice.getName());
            LogMobile.debug("FogDevice.java", "from " + aMobileDevice.getVmLocalServerCloudlet().getName() + " to " + aMobileDevice.getDestinationServerCloudlet().getName()
                    + " -> Connected by: " + aMobileDevice.getSourceAp().getServerCloudlet().getName());
            sendNow(aMobileDevice.getVmLocalServerCloudlet().getId(), MobileEvents.TO_MIGRATION, aMobileDevice);
            aMobileDevice.setLockedToMigration(true);
            //aMobileDevice.setTimeFinishDeliveryVm(-1.0);
            saveMigration(aMobileDevice);
        } else {
            sendNow(getId(), MobileEvents.NO_MIGRATION, aMobileDevice);
        }
    }
    
    public void invokeDecisionMigration(MobileDevice aMobileDevice) {
        if (DecisionHelper.shouldMigrate(aMobileDevice)) {
            LogMobile.debug("FogDevice.java", "Made the decisionMigration for " + aMobileDevice.getName());
            LogMobile.debug("FogDevice.java", "from " + aMobileDevice.getVmLocalServerCloudlet().getName() + " to " + aMobileDevice.getDestinationServerCloudlet().getName()
                    + " -> Connected by: " + aMobileDevice.getSourceAp().getServerCloudlet().getName());
            sendNow(aMobileDevice.getVmLocalServerCloudlet().getId(), MobileEvents.TO_MIGRATION, aMobileDevice);
            aMobileDevice.setLockedToMigration(true);
            saveMigration(aMobileDevice);
        } else {
            sendNow(getId(), MobileEvents.NO_MIGRATION, aMobileDevice);
        }
    }
    
    private static void saveMigration(MobileDevice st) {
        //System.out.println("MIGRATION " + st.getMyId() + " Position: " + st.getCoord().getCoordX() + ", " + st.getCoord().getCoordY() + " Direction: " + st.getDirection() + " Speed: " + st.getSpeed());
        //System.out.println("Distance between " + st.getName() + " and " + st.getSourceAp().getName() + ": "
        //      + Distances.checkDistance(st.getCoord(), st.getSourceAp().getCoord()) + " Migration time: " + st.getMigTime());
        try ( FileWriter fw = new FileWriter(st.getMyId() + "migration.txt", true);  BufferedWriter bw = new BufferedWriter(fw);  PrintWriter out = new PrintWriter(bw)) {
            out.println(st.getMyId() + "\t" + st.getCoord().getCoordX() + "\t"
                    + st.getCoord().getCoordY() + "\t" + st.getDirection() + "\t"
                    + st.getSpeed() + "\t" + st.getVmLocalServerCloudlet().getName() + "\t"
                    + st.getDestinationServerCloudlet().getName() + "\t"
                    + CloudSim.clock() + "\t" + st.getMigTime() + "\t" + (CloudSim.clock() + st.getMigTime()));
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

    /**
     * Perform miscellaneous resource management tasks
     *
     * @param ev
     */
    private void manageResources(SimEvent ev) {
        updateEnergyConsumption();
        send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
    }
    
    @Override
    public String toString() {
        return String.valueOf(getMyId());
    }

    /**
     * Updating the number of modules of an application module on this device
     *
     * @param ev instance of SimEvent containing the module and no of instances
     */
    private void updateModuleInstanceCount(SimEvent ev) {
        ModuleLaunchConfig config = (ModuleLaunchConfig) ev.getData();
        String appId = config.getModule().getAppId();
        if (!moduleInstanceCount.containsKey(appId)) {
            moduleInstanceCount.put(appId, new HashMap<String, Integer>());
        }
        moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
        //System.out.println(getName() + " Creating " + config.getInstanceCount() + " instances of module " + config.getModule().getName());
    }

    /**
     * Sending periodic tuple for an application edge. Note that for multiple
     * instances of a single source module, only one tuple is sent DOWN while
     * instanceCount number of tuples are sent UP.
     *
     * @param ev SimEvent instance containing the edge to send tuple on
     */
    private void sendPeriodicTuple(SimEvent ev) {
        AppEdge edge = (AppEdge) ev.getData();
        String srcModule = edge.getSource();
        AppModule module = null;
        for (Vm vm : getHost().getVmList()) {
            //if(vm.getVmm().equals("Xen")){
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
            if (applicationMap.isEmpty()) {
                continue;
            } else {
                Application app = getApplicationMap().get(module.getAppId());
                if (app == null) {
//					System.out.println("*sendPeriodicTuple*");
//					System.out.println("Clock: "+CloudSim.clock()+" - "+ getName());
//					System.out.println("FogDevice.java - App == null");
//					System.out.println("FogDevice.java - module.getAppId: "+module.getAppId());
//					System.out.println("FogDevice.java - getApplicationMap: "+getApplicationMap().entrySet());
//					for(MobileDevice st: MobileController.getSmartThings()){
//						if(module.getVmm().equals(st.getVmMobileDevice().getVmm())){
//							Tuple tuple = st.getVmLocalServerCloudlet().applicationMap.get(module.getAppId()).createTuple(edge, st.getVmLocalServerCloudlet().getId());
//							st.getVmLocalServerCloudlet().updateTimingsOnSending(tuple);
//							st.getVmLocalServerCloudlet().sendToSelf(tuple);
//							send(st.getVmLocalServerCloudlet().getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
//							System.out.println("Resend "+ module.getAppId()+ " to "+st.getVmLocalServerCloudlet().getName());
//							break;
//						}
//					}
                    continue;
                }
                Tuple tuple = applicationMap.get(module.getAppId()).createTuple(edge, getId());
                updateTimingsOnSending(tuple);
                sendToSelf(tuple);
            }
        }
        if (applicationMap.isEmpty()) {
            return;
        } else {
            send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
        }
    }
    
    protected void processActuatorJoined(SimEvent ev) {
        int actuatorId = ev.getSource();
        double delay = (double) ev.getData();
        getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
    }
    
    protected void updateActiveApplications(SimEvent ev) {
        Application app = (Application) ev.getData();
        // System.out.print("FogDevice " + this.getName() + " Apps " + getActiveApplications() + " Adding app " + app.getAppId());
        if (!getActiveApplications().contains(app.getAppId())) {
            getActiveApplications().add(app.getAppId());
        }
//        System.out.println(" Apps " + getActiveApplications());
    }
    
    public String getOperatorName(int vmId) {
        for (Vm vm : this.getHost().getVmList()) {
            if (vm.getId() == vmId) {
                return ((AppModule) vm).getName();
            }
        }
        return null;
    }

    /**
     * Update cloudet processing without scheduling future events.
     *
     * @return the double
     */
    @Override
    protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;
        
        for (PowerHost host : this.<PowerHost>getHostList()) {
            Log.printLine();
            
            double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
            if (time < minTime) {
                minTime = time;
            }
            
            Log.formatLine(
                    "%.2f: [Host #%d] utilization is %.2f%%",
                    currentTime,
                    host.getId(),
                    host.getUtilizationOfCpu() * 100);
        }
        
        if (timeDiff > 0) {
            Log.formatLine(
                    "\nEnergy consumption for the last time frame from %.2f to %.2f:",
                    getLastProcessTime(),
                    currentTime);
            
            for (PowerHost host : this.<PowerHost>getHostList()) {
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                if (utilizationOfCpu < 0 || utilizationOfCpu > 1) {
                    System.out.println("utilizationOfCpu: " + utilizationOfCpu);
                }
                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
                        previousUtilizationOfCpu,
                        utilizationOfCpu,
                        timeDiff);
                timeFrameDatacenterEnergy += timeFrameHostEnergy;
                
                Log.printLine();
                Log.formatLine(
                        "%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
                        currentTime,
                        host.getId(),
                        getLastProcessTime(),
                        previousUtilizationOfCpu * 100,
                        utilizationOfCpu * 100);
                Log.formatLine(
                        "%.2f: [Host #%d] energy is %.2f W*sec",
                        currentTime,
                        host.getId(),
                        timeFrameHostEnergy);
            }
            
            Log.formatLine(
                    "\n%.2f: Data center's energy is %.2f W*sec\n",
                    currentTime,
                    timeFrameDatacenterEnergy);
        }
        
        setPower(getPower() + timeFrameDatacenterEnergy);
        
        checkCloudletCompletion();

        /**
         * Remove completed VMs *
         */
        /**
         * Change made by HARSHIT GUPTA
         */
        /*for (PowerHost host : this.<PowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}*/
        Log.printLine();
        
        setLastProcessTime(currentTime);
        return minTime;
    }
    
    @Override
    protected void checkCloudletCompletion() {
        boolean cloudletCompleted = false;
        List<Vm> removeVmList = new ArrayList<>();
        List<? extends Host> list = getVmAllocationPolicy().getHostList();
        for (int i = 0; i < list.size(); i++) {
            Host host = list.get(i);
            for (Vm vm : host.getVmList()) {
                while (vm.getCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
                    if (cl != null) {
                        cloudletCompleted = true;
                        Tuple tuple = (Tuple) cl;
                        TimeKeeper.getInstance().tupleEndedExecution(tuple);
                        Application application = getApplicationMap().get(tuple.getAppId());
                        if (application == null) {
//							System.out.println("*checkCloudletCompletion*");
//							System.out.println("Clock: "+CloudSim.clock()+" - "+ getName());
//							System.out.println("FogDevice.java - Application == null");
//							System.out.println("FogDevice.java - tuple.getAppId: "+tuple.getAppId());
//							System.out.println("FogDevice.java - getApplicationMap: "+getApplicationMap().entrySet());
                            removeVmList.add(vm);
//							for(MobileDevice st: MobileController.getSmartThings()){
//								for(Sensor s: st.getSensors()){
//									if(tuple.getAppId().equals(s.getAppId())){
//										st.getVmLocalServerCloudlet().checkCloudletCompletion();
//										application = st.getVmLocalServerCloudlet().getApplicationMap().get(tuple.getAppId());
//										List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId());
//										for(Tuple resTuple : resultantTuples){
//											resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
//											resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
//											st.getVmLocalServerCloudlet().updateTimingsOnSending(resTuple);
//											st.getVmLocalServerCloudlet().sendToSelf(resTuple);
//										}
//										sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
//										Logger.debug(st.getVmLocalServerCloudlet().getName(), "Completed execution of tuple "+tuple.getCloudletId()+" on "+tuple.getDestModuleName());
//										LogMobile.debug(st.getVmLocalServerCloudlet().getName(), "Completed execution of tuple "+tuple.getCloudletId()+" on "+tuple.getDestModuleName());
//
//									}
//
//								}
//							}
                            continue;
                        }
                        //	Logger.ENABLED=true;
                        //						System.out.println("FogDevice.java - tuple.getAppId: "+tuple.getAppId());

                        Logger.debug(getName(), "Completed execution of tuple " + tuple.getCloudletId() + " on " + tuple.getDestModuleName());
                        //	Logger.ENABLED=false;

                        List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId());
                        for (Tuple resTuple : resultantTuples) {
                            resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
                            resTuple.getModuleCopyMap().put(((AppModule) vm).getName(), vm.getId());
                            updateTimingsOnSending(resTuple);
                            sendToSelf(resTuple);
                        }
                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                    }
                }
            }
            for (Vm vm : removeVmList) {
                host.getVmList().remove(vm);
            }
            removeVmList.clear();
        }
        
        if (cloudletCompleted) {
            updateAllocatedMips(null);
        }
        //		Logger.ENABLED=false;

    }
    
    protected void updateTimingsOnSending(Tuple resTuple) {
        // TODO ADD CODE FOR UPDATING TIMINGS WHEN A TUPLE IS GENERATED FROM A PREVIOUSLY RECIEVED TUPLE.
        // WILL NEED TO CHECK IF A NEW LOOP STARTS AND INSERT A UNIQUE TUPLE ID TO IT.
        String srcModule = resTuple.getSrcModuleName();
        String destModule = resTuple.getDestModuleName();
        for (AppLoop loop : getApplicationMap().get(resTuple.getAppId()).getLoops()) {
            if (loop.hasEdge(srcModule, destModule) && loop.isStartModule(srcModule)) {
                int tupleId = TimeKeeper.getInstance().getUniqueId();
                resTuple.setActualTupleId(tupleId);
                if (!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId())) {
                    TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
                }
                TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
                TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());

                //Logger.debug(getName(), "\tSENDING\t"+tuple.getActualTupleId()+"\tSrc:"+srcModule+"\tDest:"+destModule);
            }
        }
    }
    
    protected int getChildIdWithRouteTo(int targetDeviceId) {
        for (Integer childId : getChildrenIds()) {
            if (targetDeviceId == childId) {
                return childId;
            }
            if (((FogDevice) CloudSim.getEntity(childId)).getChildIdWithRouteTo(targetDeviceId) != -1) {
                return childId;
            }
        }
        return -1;
    }
    
    protected int getChildIdForTuple(Tuple tuple) {
        if (tuple.getDirection() == Tuple.ACTUATOR) {
            int gatewayId = ((Actuator) CloudSim.getEntity(tuple.getActuatorId())).getGatewayDeviceId();
            return getChildIdWithRouteTo(gatewayId);
        }
        return -1;
    }
    
    protected void updateAllocatedMips(String incomingOperator) {
        getHost().getVmScheduler().deallocatePesForAllVms();
        for (final Vm vm : getHost().getVmList()) {
            //if(vm.getVmm().equals("Xen")){
            if (vm.getCloudletScheduler().runningCloudlets() > 0 || ((AppModule) vm).getName().equals(incomingOperator)) {
                getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>() {
                    protected static final long serialVersionUID = 1L;
                    
                    {
                        add((double) getHost().getTotalMips());
                    }
                });
            } else {
                getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>() {
                    protected static final long serialVersionUID = 1L;
                    
                    {
                        add(0.0);
                    }
                });
            }
            //}
        }
        
        updateEnergyConsumption();
        
    }
    
    private void updateEnergyConsumption() {
        double totalMipsAllocated = 0;
        for (final Vm vm : getHost().getVmList()) {
            //			AppModule operator = (AppModule)vm;
            //			operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler()
            //					.getAllocatedMipsForVm(operator));
            totalMipsAllocated += getHost().getTotalAllocatedMipsForVm(vm);
        }
        
        double timeNow = CloudSim.clock();
        double currentEnergyConsumption = getEnergyConsumption();
        double newEnergyConsumption = currentEnergyConsumption + (timeNow - lastUtilizationUpdateTime) * getHost().getPowerModel().getPower(lastUtilization);
        setEnergyConsumption(newEnergyConsumption);

        /*if(getName().equals("d-0")){
			System.out.println("------------------------");
			System.out.println("Utilization = "+lastUtilization);
			System.out.println("Power = "+getHost().getPowerModel().getPower(lastUtilization));
			System.out.println(timeNow-lastUtilizationUpdateTime);
		}*/
        double currentCost = getTotalCost();
        double newcost = currentCost + (timeNow - lastUtilizationUpdateTime) * getRatePerMips() * lastUtilization * getHost().getTotalMips();
        setTotalCost(newcost);
        
        lastUtilization = Math.min(1, totalMipsAllocated / getHost().getTotalMips());
        lastUtilizationUpdateTime = timeNow;
    }
    
    protected void processAppSubmit(SimEvent ev) {
        Application app = (Application) ev.getData();
        //		System.out.println("*************FogDevice.java********* app.getAppId: "+app.getAppId());
        applicationMap.put(app.getAppId(), app);
    }
    
    protected void addChild(int childId) {
        if (CloudSim.getEntityName(childId).toLowerCase().contains("sensor")) {
            return;
        }
        if (!getChildrenIds().contains(childId) && childId != getId()) {
            getChildrenIds().add(childId);
        }
        if (!getChildToOperatorsMap().containsKey(childId)) {
            getChildToOperatorsMap().put(childId, new ArrayList<String>());
        }
    }
    
    protected void removeChild(int childId) {
        getChildrenIds().remove(CloudSim.getEntity(childId));
        getChildToOperatorsMap().remove(childId);
    }
    
    protected void updateCloudTraffic() {
        int time = (int) CloudSim.clock() / 1000;
        if (!cloudTrafficMap.containsKey(time)) {
            cloudTrafficMap.put(time, 0);
        }
        cloudTrafficMap.put(time, cloudTrafficMap.get(time) + 1);
    }
    
    protected void sendTupleToActuator(Tuple tuple) {
        /*for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			if(actuatorId == tuple.getActuatorId()){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		int childId = getChildIdForTuple(tuple);
		if(childId != -1)
			sendDown(tuple, childId);*/
        for (Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()) {
            int actuatorId = actuatorAssociation.getFirst();
            double delay = actuatorAssociation.getSecond();
            String actuatorType = ((Actuator) CloudSim.getEntity(actuatorId)).getActuatorType();
            if (tuple.getDestModuleName().equals(actuatorType)) {
                send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
                return;
            }
        }
        for (int childId : getChildrenIds()) {
            sendDown(tuple, childId);
        }
    }
    int numClients = 0;
    
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
        LogHelper.addTotalTuples(aTuple.getAppId(), 1);
        if (aTuple.getInitialTime() == -1) {
            MyStatistics.getInstance().getTupleLatency().put(aTuple.getMyTupleId(), CloudSim.clock() - getUplinkLatency());
            aTuple.setInitialTime(CloudSim.clock() - getUplinkLatency());
        }

        //Identify lost tuple
        for (MobileDevice st : getSmartThings()) {
            if (st.getId() == ev.getSource()) {
                if ((!st.isHandoffStatus() && !st.isMigStatus())) {
                    break;
                } else {
                    MyStatistics.getInstance().setMyCountLostTuple(1);
                    st.addTotalLostTuples(1);
                    // System.out.println(st.isHandoffStatus() + " <- (FD) -> " + st.isMigStatus());
                    LogHelper.addLostTuples(aTuple.getAppId(), 1);
                    MonitoringAgent.addlostTuple();
                    saveLostTupple(String.valueOf(CloudSim.clock()), st.getId() + "fdlostTupple.txt");
                    if (st.isMigStatus()) {
                        LogMobile.debug("FogDevice.java", st.getName() + " is in Migration");
                        return;
                    } else {
                        return;
                    }
                }
            }
            
        }
        //
        if (getName().equals("cloud")) {
            updateCloudTraffic();
        }
        Logger.debug(getName(), "Received tuple " + aTuple.getCloudletId() + " with tupleType = " + aTuple.getTupleType() + "\t| Source : "
                + CloudSim.getEntityName(ev.getSource()) + "|Dest : " + CloudSim.getEntityName(ev.getDestination()));

//        String direction = "UP";
//        if (aTuple.getDirection() == 2) {
//            direction = "DOWN";
//        }
//        System.out.println("Received tuple " + aTuple.getCloudletId() + " [ " + this.getName() + "] with tupleType = " + aTuple.getTupleType() + "\t| DIREC = " + direction + "\t| Source : "
//                + aTuple.getSrcModuleName() + "| Dest : " + aTuple.getDestModuleName() + " | Took: " + (CloudSim.clock() - TimeKeeper.getInstance().getEmitTimes().get(aTuple.getActualTupleId())));
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
        
        if (aTuple.getDirection() == Tuple.ACTUATOR) {
            sendTupleToActuator(aTuple);
            return;
        }
        
        for (Vm vm : getHost().getVmList()) {
            final AppModule operator = (AppModule) vm;
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
        
        if (getName().equals("cloud") && aTuple.getDestModuleName() == null) {
            sendNow(getControllerId(), FogEvents.TUPLE_FINISHED, null);
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
                //System.out.println("Finishing TUPLE @FD: " + aTuple.getMyTupleId());
                executeTuple(ev, aTuple.getDestModuleName());
                CMFog.msgQueue.remove(Integer.valueOf(aTuple.getMyTupleId()));
                return;
            }
        }
        //Not for this cloudlet, foward the tuple from event
        if (aTuple.getDestModuleName() != null) {
            if (aTuple.getDirection() == Tuple.DOWN) {
                if (this.layer == 1) {
                    //System.out.println("Choosing a MD to send");
                    for (int childId : getChildrenIds()) {
                        MobileDevice tempSt = (MobileDevice) CloudSim.getEntity(childId);
                        if (aTuple.getAppId().equals(((AppModule) tempSt.getVmMobileDevice()).getAppId())) {
                            sendDown(aTuple, childId);
                        }
                    }
                } else {
                    //System.out.println("Sending DOWN to L1");
                    sendDown(aTuple, CMFog.modulesLocation.get(aTuple.getDestModuleName()).getId());
                }
            } else if (aTuple.getDirection() == Tuple.UP) {
//                if (this.layer == 1) {
//                    System.out.println("Sending UP to L2");
//                    sendUp(aTuple);
//                } else {
                //System.out.println("Sending UP to L2/CLOUD or DOWN to L1/L2");
                List<Integer> originTrace = CMFogHelper.generateCloudletTrace(this);
                List<Integer> destinationTrace = CMFogHelper.generateCloudletTrace(CMFog.modulesLocation.get(aTuple.getDestModuleName()));
                //System.out.println(originTrace);
                //System.out.println(destinationTrace);
                //System.out.println("SRC:" + aTuple.getSrcModuleName() + " - " + CMFog.modulesLocation.get(aTuple.getSrcModuleName()).getName() + " DEST: " + aTuple.getDestModuleName() + " - " + CMFog.modulesLocation.get(aTuple.getDestModuleName()).getName());
                int convergenceIndex = CMFogHelper.traceConvergence(originTrace, destinationTrace);
                //System.out.println(getId() + " <--- CI: " + convergenceIndex + "[" + destinationTrace.get(convergenceIndex) + "]");
                if (destinationTrace.get(convergenceIndex) != getId()) {
                    sendUp(aTuple);
                } else {
                    if (this.layer == 1) {
                        //System.out.println("Choosing a MD to send");
                        for (int childId : getChildrenIds()) {
                            MobileDevice tempSt = (MobileDevice) CloudSim.getEntity(childId);
                            if (aTuple.getAppId().equals(((AppModule) tempSt.getVmMobileDevice()).getAppId())) {
                                aTuple.setDirection(Tuple.DOWN);
                                sendDown(aTuple, childId);
                                return;
                            }
                        }
                    }
                    FogDevice convergenceCloudlet = CMFogHelper.getCloudletById(destinationTrace.get(convergenceIndex - 1));
                    if (childrenCloudlets.contains(convergenceCloudlet)) {
                        //System.out.println("Convergence point, inverting direction");
                        aTuple.setDirection(Tuple.DOWN);
                        sendDown(aTuple, destinationTrace.get(convergenceIndex - 1));
                    } else {
                        System.out.println("No valid child");
                        System.exit(0);
                    }
                }
                //   }
            }
        } else {
            System.out.println("No destination, send up to resolve");
            sendUp(aTuple);
        }
        ////////////////////////////////// OLD

//        if (appToModulesMap.containsKey(aTuple.getAppId())) {
//            
//            if (appToModulesMap.get(aTuple.getAppId()).contains(aTuple.getDestModuleName())) {
//                int vmId = -1;
//                for (Vm vm : getHost().getVmList()) {
//                    //if(vm.getVmm().equals("Xen")){
//                    if (((AppModule) vm).getName().equals(aTuple.getDestModuleName())) {
//                        vmId = vm.getId();
//                    }
//                    //}
//                }
//                if (vmId < 0
//                        || (aTuple.getModuleCopyMap().containsKey(aTuple.getDestModuleName())
//                        && aTuple.getModuleCopyMap().get(aTuple.getDestModuleName()) != vmId)) {
//                    return;
//                }
//                aTuple.setVmId(vmId);
//                updateTimingsOnReceipt(aTuple);
//                System.out.println("processing");
//                executeTuple(ev, aTuple.getDestModuleName());
//            } else {
//                System.out.println("REDIR");
//                
//            }
//        } else {
//            System.out.println("????????????????????????????");
//            if (aTuple.getDirection() == Tuple.UP) {
//                sendUp(aTuple);
//            } else if (aTuple.getDirection() == Tuple.DOWN) {
//                for (int childId : getChildrenIds()) {
//                    MobileDevice tempSt = (MobileDevice) CloudSim.getEntity(childId);
//                    if (aTuple.getAppId().equals(((AppModule) tempSt.getVmMobileDevice()).getAppId())) {
//                        //						System.out.println("FogDevice: "+CloudSim.getEntityName(getId())+" ChildId: "+CloudSim.getEntityName(childId)+" "+tuple.getTupleType());
//                        sendDown(aTuple, childId);
//                    }
//                }
//                
//            }
//        }
    }
    
    public void printResults(String a, String filename) {
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
    
    protected void updateTimingsOnReceipt(Tuple tuple) {
        Application app = getApplicationMap().get(tuple.getAppId());
        if (app == null) {
            return;
        }
        String srcModule = tuple.getSrcModuleName();
        String destModule = tuple.getDestModuleName();
        List<AppLoop> loops = app.getLoops();
        for (AppLoop loop : loops) {
            if (loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)) {
                Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
                if (startTime == null) {
                    break;
                }
                if (!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())) {
                    TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
                    TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
                    TimeKeeper.getInstance().getMaxLoopExecutionTime().put(loop.getLoopId(), 0.0);
                    printResults(String.valueOf(0), loop.getLoopId() + "LoopId.txt");
                    printResults(String.valueOf(0), loop.getLoopId() + "LoopMaxId.txt");
                }
                double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
                int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
                double delay = CloudSim.clock() - TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());//+plusLatency);
                //System.out.println(">>>>>> AVG: " + tuple.getActualTupleId() + " - " + delay);
                if (delay > TimeKeeper.getInstance().getMaxLoopExecutionTime().get(loop.getLoopId())) {
                    TimeKeeper.getInstance().getMaxLoopExecutionTime().put(loop.getLoopId(), delay);
                    printResults(String.valueOf(delay), loop.getLoopId() + "LoopMaxId.txt");
                }
                TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
                double newAverage = (currentAverage * currentCount + delay) / (currentCount + 1);
                TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
                TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount + 1);
                printResults(String.valueOf(newAverage), loop.getLoopId() + "LoopId.txt");
                break;
            }
        }
    }
    
    protected void processSensorJoining(SimEvent ev) {
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
    }
    
    protected void executeTuple(SimEvent ev, String operatorId) {
        //TODO Power funda
        Tuple tuple = (Tuple) ev.getData();
        
        boolean flagContinue = false;
        for (MobileDevice st : CMFogMobileController.getMobileDevices()) {// verifica se o smartthing ainda esta na lista
            for (Sensor s : st.getSensors()) {
                if (tuple.getAppId().equals(s.getAppId())) {
                    flagContinue = true;
                    break;
                }
            }
        }
        if (!flagContinue) {
            return;
        }
        
        Logger.debug(getName(), "Executing tuple " + tuple.getCloudletId() + " on module " + operatorId);
        
        if (MyStatistics.getInstance().getTupleLatency().get(tuple.getMyTupleId()) != null) {//if(tuple.getTupleType().contains("EEG")){
            tuple.setFinalTime(CloudSim.clock() + getUplinkLatency());
            //			MyStatistics.getInstance().putLatencyFileValue(tuple.getFinalTime()- MyStatistics.getInstance().getTupleLatency().get(tuple.getMyTupleId())//tuple.getInitialTime()+(2*getUplinkLatency())
            //					, CloudSim.clock(),tuple.getTupleType(),tuple.getMyTupleId());
        }
        
        Application app = getApplicationMap().get(tuple.getAppId());
        if (app == null) {
//			System.out.println("*executeTuple*");
//			System.out.println("Clock: "+CloudSim.clock()+" - "+ getName());
//			System.out.println("FogDevice.java - App == null");
//			System.out.println("FogDevice.java - tuple.getAppId: "+tuple.getAppId());
//			System.out.println("FogDevice.java - getApplicationMap: "+getApplicationMap().entrySet());
//						for(MobileDevice st: MobileController.getSmartThings()){
//							for(Sensor s: st.getSensors()){
//								if(tuple.getAppId().equals(s.getAppId())){
//									st.getVmLocalServerCloudlet().executeTuple(ev, operatorId);
//								}
//							}
//						}
            return;
        }
        
        TimeKeeper.getInstance().tupleStartedExecution(tuple);
        updateAllocatedMips(operatorId);
        processCloudletSubmit(ev, false);
        updateAllocatedMips(operatorId);
        /*for(Vm vm : getHost().getVmList()){
			Logger.error(getName(), "MIPS allocated to "+((AppModule)vm).getName()+" = "+getHost().getTotalAllocatedMipsForVm(vm));
		}*/
    }
    
    protected void processModuleArrival(SimEvent ev) {
        AppModule module = (AppModule) ev.getData();
        String appId = module.getAppId();
        if (!appToModulesMap.containsKey(appId)) {
            appToModulesMap.put(appId, new ArrayList<String>());
        }
        appToModulesMap.get(appId).add(module.getName());
        processVmCreate(ev, false);
        if (module.isBeingInstantiated()) {
            module.setBeingInstantiated(false);
        }

        //initializePeriodicTuples(module);
        module.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(module).getVmScheduler()
                .getAllocatedMipsForVm(module));
    }
    
    private void initializePeriodicTuples(AppModule module) {
        String appId = module.getAppId();
        Application app = getApplicationMap().get(appId);
        List<AppEdge> periodicEdges = app.getPeriodicEdges(module.getName());
        for (AppEdge edge : periodicEdges) {
            send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
        }
    }
    
    protected void processOperatorRelease(SimEvent ev) {
        this.processVmMigrate(ev, false);
    }
    
    protected void updateNorthTupleQueue() {
        if (!getNorthTupleQueue().isEmpty()) {
            Tuple tuple = getNorthTupleQueue().poll();
            sendUpFreeLink(tuple);
        } else {
            setNorthLinkBusy(false);
        }
    }
    
    protected void sendDownFreeLink(Tuple tuple, int childId) {
        double networkDelay = tuple.getCloudletFileSize() / getUplinkBandwidth();
        //Logger.debug(getName(), "Sending tuple with tupleType = "+tuple.getTupleType()+" DOWN");
        setSouthLinkBusy(true);
        send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE);
//        double latency = 0;
//        if (this.layer == 1) {
//            latency = getChildToLatencyMap().get(childId);
//        } else {
//            latency = NetworkTopology.getDelay(getId(), childId);
//        }
        //System.out.println(getName() + " - " + networkDelay);// + " + " + getUplinkLatency() + " = " + delay);
        send(childId, networkDelay, FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());
    }
    
    protected void sendUpFreeLink(Tuple tuple) {
        //double networkDelay = (tuple.getCloudletFileSize() / 1000) / getUplinkBandwidth();
        double networkDelay = tuple.getCloudletFileSize() / getUplinkBandwidth();
        //System.out.println(getName() + " " + tuple.getMyTupleId() + " : " + tuple.getCloudletFileSize() + " / " + getUplinkBandwidth() + " = " + networkDelay);
        setNorthLinkBusy(true);
        send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
        //double delay = networkDelay;// + getUplinkLatency();
        // System.out.println(getName() + " - " + networkDelay);// + " + " + getUplinkLatency() + " = " + delay);
        send(parentId, networkDelay, FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());
    }
    
    protected void sendUp(Tuple tuple) {
        if (parentId > 0) {
            if (!isNorthLinkBusy()) {
                sendUpFreeLink(tuple);
            } else {
                northTupleQueue.add(tuple);
            }
        }
    }
    
    protected void updateSouthTupleQueue() {
        if (!getSouthTupleQueue().isEmpty()) {
            Pair<Tuple, Integer> pair = getSouthTupleQueue().poll();
            sendDownFreeLink(pair.getFirst(), pair.getSecond());
        } else {
            setSouthLinkBusy(false);
        }
    }
    
    protected void sendDown(Tuple tuple, int childId) {
        //if (getChildrenIds().contains(childId)) {
        if (!isSouthLinkBusy()) {
            sendDownFreeLink(tuple, childId);
        } else {
            southTupleQueue.add(new Pair<Tuple, Integer>(tuple, childId));
        }
        //}
    }
    
    protected void sendToSelf(Tuple tuple) {
        send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
    }
    
    public PowerHost getHost() {
        return (PowerHost) getHostList().get(0);
    }
    
    public int getParentId() {
        return parentId;
    }
    
    public void setParentId(int parentId) {
        this.parentId = parentId;
    }
    
    public List<Integer> getChildrenIds() {
        return childrenIds;
    }
    
    public void setChildrenIds(List<Integer> childrenIds) {
        this.childrenIds = childrenIds;
    }
    
    public double getUplinkBandwidth() {
        return uplinkBandwidth;
    }
    
    public void setUplinkBandwidth(double uplinkBandwidth) {
        this.uplinkBandwidth = uplinkBandwidth;
    }
    
    public double getUplinkLatency() {
        return uplinkLatency;
    }
    
    public void setUplinkLatency(double uplinkLatency) {
        this.uplinkLatency = uplinkLatency;
    }
    
    public boolean isSouthLinkBusy() {
        return isSouthLinkBusy;
    }
    
    public boolean isNorthLinkBusy() {
        return isNorthLinkBusy;
    }
    
    public void setSouthLinkBusy(boolean isSouthLinkBusy) {
        this.isSouthLinkBusy = isSouthLinkBusy;
    }
    
    public void setNorthLinkBusy(boolean isNorthLinkBusy) {
        this.isNorthLinkBusy = isNorthLinkBusy;
    }
    
    public int getControllerId() {
        return controllerId;
    }
    
    public void setControllerId(int controllerId) {
        this.controllerId = controllerId;
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
    
    public Map<Integer, List<String>> getChildToOperatorsMap() {
        return childToOperatorsMap;
    }
    
    public void setChildToOperatorsMap(Map<Integer, List<String>> childToOperatorsMap) {
        this.childToOperatorsMap = childToOperatorsMap;
    }
    
    public Map<String, Application> getApplicationMap() {
        return applicationMap;
    }
    
    public void setApplicationMap(Map<String, Application> applicationMap) {
        this.applicationMap = applicationMap;
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
    
    public double getDownlinkBandwidth() {
        return downlinkBandwidth;
    }
    
    public void setDownlinkBandwidth(double downlinkBandwidth) {
        this.downlinkBandwidth = downlinkBandwidth;
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
    
    public Map<Integer, Double> getChildToLatencyMap() {
        return childToLatencyMap;
    }
    
    public void setChildToLatencyMap(Map<Integer, Double> childToLatencyMap) {
        this.childToLatencyMap = childToLatencyMap;
    }
    
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
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
    
    public Map<String, Map<String, Integer>> getModuleInstanceCount() {
        return moduleInstanceCount;
    }
    
    public void setModuleInstanceCount(
            Map<String, Map<String, Integer>> moduleInstanceCount) {
        this.moduleInstanceCount = moduleInstanceCount;
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
    
    public Set<FogDevice> getServerCloudlets() {
        return serverCloudlets;
    }
    
    public void setServerCloudlets(FogDevice sc, int action) {//myiFogSim
        if (action == Policies.ADD) {
            this.serverCloudlets.add(sc);
        } else {
            this.serverCloudlets.remove(sc);
        }
    }
    
    public FogDevice getServerCloudletToVmMigrate() {
        return serverCloudletToVmMigrate;
    }
    
    public void setServerCloudletToVmMigrate(FogDevice serverCloudletToVmMigrate) {
        this.serverCloudletToVmMigrate = serverCloudletToVmMigrate;
    }
    
    public Set<MobileDevice> getSmartThingsWithVm() {
        return smartThingsWithVm;
    }
    
    public void setSmartThingsWithVm(MobileDevice st, int action) {//myiFogSim
        if (action == Policies.ADD) {
            this.smartThingsWithVm.add(st);
        } else {
            this.smartThingsWithVm.remove(st);
        }
    }
    
    public int getVolatilParentId() {
        return volatilParentId;
    }
    
    public void setVolatilParentId(int volatilParentId) {
        this.volatilParentId = volatilParentId;
    }
    
    public BeforeMigration getBeforeMigrate() {
        return beforeMigration;
    }
    
    public void setBeforeMigrate(BeforeMigration beforeMigration) {
        this.beforeMigration = beforeMigration;
    }
}
