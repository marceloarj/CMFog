package org.fog.mfmf.simulator;

import org.fog.vmmigration.*;
import java.util.List;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.cmfog.CMFog;
import org.fog.cmfog.helpers.CMFogHelper;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.DiscoverLocalization;
import org.fog.cmfog.mobilityprediction.Manager;
import org.fog.vmmobile.AppExemplo2;

public class CMFogDecision implements DecisionMigration {

    private List<FogDevice> serverCloudlets;
    private List<ApDevice> apDevices;
    private int migPointPolicy;
    private ApDevice correntAP;
    private int nextApId;
    private int nextServerClouletId;
    private int policyReplicaVM;

    private int smartThingPosition;
    private boolean migZone;
    private boolean migPoint;

    public CMFogDecision(List<FogDevice> serverCloudlets,
            List<ApDevice> apDevices, int migPointPolicy, int policyReplicaVM) {
        super();
        setServerCloudlets(serverCloudlets);
        setApDevices(apDevices);
        setMigPointPolicy(migPointPolicy);
        setPolicyReplicaVM(policyReplicaVM);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean shouldMigrate(MobileDevice smartThing) {
        System.out.println("================================= SHOULD MIGRATE =================================");
        setCorrentAP(smartThing.getSourceAp()); //AP atual
        setSmartThingPosition(DiscoverLocalization.discoverLocal(getCorrentAP().getCoord(), smartThing.getCoord()));
        smartThing.getMigrationTechnique().verifyPoints(smartThing, getSmartThingPosition());
        int fogNodeId = -1;
        if (smartThing.isVerticalMigration() == true) {
            System.out.println("Decide for vertical migration from L1->L2 - WRONG");
            fogNodeId = ((FogDevice) CloudSim.getEntity(smartThing.getSourceAp().getServerCloudlet().getParentId())).getMyId();
            Integer estimatedPermTime = Manager.syncForVerticalMigration(smartThing);
            smartThing.setEstimatedPermTime(estimatedPermTime);
            System.out.println("Vertical Migrating to L2, Next migration after" + smartThing.getEstimatedPermTime());
        } else if (!(smartThing.isMigPoint())) {
            //System.out.println("################ NOT IN MIGRATION POINT ################");
            return false;//no migration
        } else {
            System.out.println("-----> " + smartThing.getName() + " meets the trigger requirements for migration <<<<< EDITED");
            if (smartThing.getVmLocalServerCloudlet().getLayer() == 1) {
                System.out.println("Decide for layer 1");
                fogNodeId = Manager.predictMobility(CMFog.mobilityPredictionL1, smartThing, serverCloudlets);
            } else if (smartThing.getVmLocalServerCloudlet().getLayer() == 2) {
                System.out.println("Decide for layer 2");
                fogNodeId = Manager.predictMobility(CMFog.mobilityPredictionL2, smartThing, serverCloudlets);
            }

            setNextServerClouletId(fogNodeId);
            try {
                //System.out.println("PREDICTED NODE" + AppExemplo2.getFogDeviceByMyId(fogNodeId, serverCloudlets).getMyId() + " | SELECT: " + fogNodeId);
                // AppExemplo2.log.add(CloudSim.clock() + " | " + smartThing.getSourceAp().getName() + " - " + smartThing.getVmLocalServerCloudlet().getName() + " - " + AppExemplo2.getFogDeviceByMyId(fogNodeId, serverCloudlets).getName() + " - " + smartThing.getMigTime() + " - " + (smartThing.getMigTime() + CloudSim.clock()));
            } catch (NullPointerException a) {
                AppExemplo2.log.add("$$$$$$$$$$$$ NULL IN SHOULD MIGRATE $$$$$$$$$$$$");
            }
            if (getNextServerClouletId() < 0) {
                System.out.println("Does not exist nextServerCloulet");
                return false;
            } else {
                //smartThing.setMobilityTrack(new ArrayList<>());
                //setNextApId(fogNodeId);	
                //AppExemplo2.log.add("AP ID: " + getNextApId());
            }
        }
        //FogDevice next= null;
        return ServiceAgreement.serviceAgreement(CMFogHelper.getCloudletByMyId(getNextServerClouletId(), serverCloudlets), smartThing);
    }

    public ApDevice getCorrentAP() {
        return correntAP;
    }

    public List<FogDevice> getServerCloudlets() {
        return serverCloudlets;
    }

    public void setServerCloudlets(List<FogDevice> serverCloudlets) {
        this.serverCloudlets = serverCloudlets;
    }

    public List<ApDevice> getApDevices() {
        return apDevices;
    }

    public void setApDevices(List<ApDevice> apDevices) {
        this.apDevices = apDevices;
    }

    public int getMigPointPolicy() {
        return migPointPolicy;
    }

    public void setMigPointPolicy(int migPointPolicy) {
        this.migPointPolicy = migPointPolicy;
    }

    public int getNextApId() {
        return nextApId;
    }

    public void setNextApId(int nextApId) {
        this.nextApId = nextApId;
    }

    public int getNextServerClouletId() {
        return nextServerClouletId;
    }

    public void setNextServerClouletId(int nextServerClouletId) {
        this.nextServerClouletId = nextServerClouletId;
    }

    public int getSmartThingPosition() {
        return smartThingPosition;
    }

    public void setSmartThingPosition(int smartThingPosition) {
        this.smartThingPosition = smartThingPosition;
    }

    public boolean isMigZone() {
        return migZone;
    }

    public void setMigZone(boolean migZone) {
        this.migZone = migZone;
    }

    public boolean isMigPoint() {
        return migPoint;
    }

    public void setMigPoint(boolean migPoint) {
        this.migPoint = migPoint;
    }

    public void setCorrentAP(ApDevice correntAP) {
        this.correntAP = correntAP;
    }

    public int getPolicyReplicaVM() {
        return policyReplicaVM;
    }

    public void setPolicyReplicaVM(int policyReplicaVM) {
        this.policyReplicaVM = policyReplicaVM;
    }
}
