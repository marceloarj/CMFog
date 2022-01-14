package org.fog.vmmigration;

import java.util.List;

import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.DiscoverLocalization;

public class LowestLatency implements DecisionMigration {

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

    public LowestLatency(List<FogDevice> serverCloudlets,
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
        System.out.println("-------------------------------------------------------> SHOULD MIG LOWEST LATENCY");
        setCorrentAP(smartThing.getSourceAp());
        setSmartThingPosition(DiscoverLocalization.discoverLocal(getCorrentAP().getCoord(),smartThing.getCoord()));
        smartThing.getMigrationTechnique().verifyPoints(smartThing, getSmartThingPosition());

        if (!(smartThing.isMigPoint())) {
            System.out.println("-------------------------------------------------------> NOT IN MIG POINT");
            return false;//no migration
        } else {
            System.out.println("-------------------------------------------------------> FINDING CLOUDLET DEST");
            setNextServerClouletId(Migration.lowestLatencyCostServerCloudlet(serverCloudlets, apDevices, smartThing));
            System.out.println("-------------------------------------------------------> FOUND: " + getNextServerClouletId());
            if (getNextServerClouletId() < 0) {
                //				System.out.println("Does not exist nextServerCloulet");
                return false;
            } else {
                setNextApId(Migration.nextAp(apDevices, smartThing));//tempListAps, smartThing));		
                if (getNextApId() < 0) {//index is negative -> A migração não deve ocorrer, pois caso o st faça um handoff, não será para nenhum ap deste ServerCloudlet
                    //	System.out.println("Does not exist nextAp");
//						return false;//no migration
                } else {
                    if (!Migration.isEdgeAp(apDevices.get(getNextApId()), smartThing)) {// verify if the next Ap is edge (return false if the ServerCloudlet destination is the same ServerCloud source)
                        System.out.println("----------------------------------------------------------------> N ERA PR ENTRAR AQUI");
//						System.out.println("#########Ap is not Edge######");
                        return false;//no migration
                    }
                }
            }
        }
        System.out.println("---------------------------------------------------------> " + ServiceAgreement.serviceAgreement(serverCloudlets.get(getNextServerClouletId()), smartThing));
        return ServiceAgreement.serviceAgreement(serverCloudlets.get(getNextServerClouletId()), smartThing);
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
