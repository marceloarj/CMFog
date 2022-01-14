package org.fog.vmmigration;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.cloudbus.cloudsim.NetworkTopology;
import org.fog.entities.*;
import org.fog.localization.Coordinate;
import org.fog.localization.DiscoverLocalization;
import org.fog.localization.Distances;
import org.fog.vmmobile.AppExemplo2;
import org.fog.vmmobile.constants.*;

public class Migration {

    private static boolean migrationPoint;
    private static boolean migrationZone;
    private int location;
    private ApDevice correntAP;
    private FogDevice correntServerCloudlet;
    private MobileDevice correntSmartThing;
    private ApDevice apAvailable;
    private FogDevice serverCloudletAvailable;
    private int flowDirection;
    private static List<ApDevice> apsAvailable;
    private static List<FogDevice> serverCloudletsAvailable;
    private static int policyReplicaVM;
    private static Random rand;

    /**
     * @param args
     * @author Marcio Moraes Lopes
     */
    public static List<ApDevice> apAvailableList(List<ApDevice> oldApList,
            MobileDevice smartThing) {//It looks to cone and return the Aps available list
        List<ApDevice> newApList = new ArrayList<>();
        for (ApDevice ap : oldApList) {
            if (!smartThing.getSourceAp().equals(ap)) {
                newApList.add(ap);
            }
        }

        return newApList;
    }
////////////////////////////////////////////////////////////////////////////

    public static int nextAp(List<ApDevice> apDevices, MobileDevice smartThing, boolean byLatency) {//Policy: the closest Ap 
        return theClosestAp(apDevices, smartThing);//return the closest ap's id or -1 if it doesn't exist
    }

    public static int theClosestAp(List<ApDevice> apDevices, MobileDevice smartThing) {//verify what return type is better (int or ApDevice)	
        List<ApDevice> apDevicesCP = new ArrayList<>(apDevices);
        ApDevice choose = null;
        double min = Double.MAX_VALUE;
        while (!apDevicesCP.isEmpty()) {
            for (ApDevice ap : apDevicesCP) {
                int destinationToCompare = smartThing.getVmLocalServerCloudlet().getId();
                if (smartThing.getDestinationServerCloudlet() != null) {
                    destinationToCompare = smartThing.getDestinationServerCloudlet().getId();
                }
                double latency = ap.getUplinkLatency() + NetworkTopology.getDelay(ap.getServerCloudlet().getId(), destinationToCompare);
                if (latency < min) {
                    min = latency;
                    choose = ap;
                }
            }
            if (Distances.checkDistance(choose.getCoord(), smartThing.getCoord()) > MaxAndMin.AP_COVERAGE) {
                apDevicesCP.remove(choose);
                System.out.println("removind ap: " + choose.getName());
                min = Double.MAX_VALUE;
            } else {
                System.out.println("-> Handoff decision: " + choose.getName());
                return choose.getMyId();
            }
        }
        return -1;
    }

///////////////////////////////////////////////////////////////////////////////
    public static int nextAp(List<ApDevice> apDevices, MobileDevice smartThing) {//Policy: the closest Ap 

        setApsAvailable(apAvailableList(apDevices, smartThing));//It return apDevice list without the smartThing's sourceAp 
        if (getApsAvailable().size() == 0) {
            return -1;
        }
        return Distances.theClosestAp(getApsAvailable(), smartThing);//return the closest ap's id or -1 if it doesn't exist
    }

    public int nextApFromCloudlet(Set<ApDevice> apDevices, MobileDevice smartThing) {

        return 0;
    }

    public static boolean insideCone(int smartThingDirection, int zoneDirection) {//
        int ajust1, ajust2;

        if (smartThingDirection == Directions.EAST) {
            ajust1 = Directions.SOUTHEAST;
            ajust2 = Directions.EAST + 1;
        } else if (smartThingDirection == Directions.SOUTHEAST) {
            ajust1 = Directions.SOUTHEAST - 1;
            ajust2 = Directions.EAST;
        } else {
            ajust1 = smartThingDirection - 1;
            /*plus 45 degree*/
            ajust2 = smartThingDirection + 1;
        }

        if (zoneDirection == smartThingDirection
                || zoneDirection == ajust1
                || zoneDirection == ajust2) /*Define Migration Zone -> it looks for 135 degree = 45 way + 45 way1 +45 way2*/ {
            return true;
        } else {
            return false;
        }
    }

    private static void saveDistance(int travelTimeId, Coordinate coord_atual, Coordinate coord_prev, Coordinate coord_erro, Double dist_atual_prev, Double dist_atual_erro, Double dist_prev_erro, int velocidade, String filename) {

        try (FileWriter fw1 = new FileWriter(filename, true);
                BufferedWriter bw1 = new BufferedWriter(fw1);
                PrintWriter out1 = new PrintWriter(bw1)) {
            out1.println(travelTimeId + "\t" + coord_atual.getCoordX() + "\t" + coord_atual.getCoordY() + "\t" + coord_prev.getCoordX() + "\t" + coord_prev.getCoordY() + "\t" + coord_erro.getCoordX() + "\t" + coord_erro.getCoordY() + "\t" + dist_atual_prev + "\t" + dist_atual_erro + "\t" + dist_prev_erro + "\t" + velocidade);
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

    public static List<FogDevice> serverClouletsAvailableList(List<FogDevice> oldServerCloudlets, MobileDevice smartThing) {

        Coordinate coord_atual = smartThing.getCoord();

        ArrayList<String[]> path = smartThing.getPath();
        int travelTimeId = smartThing.getTravelTimeId() + smartThing.getTravelPredicTime();
        if (travelTimeId >= path.size()) {
            travelTimeId = path.size() - 1;
        }

        String[] coodinates = path.get(travelTimeId);

        int x = (int) Double.parseDouble(coodinates[2]);
        int y = (int) Double.parseDouble(coodinates[3]);
        Coordinate coord_prev = new Coordinate();
        coord_prev.setCoordX(x);
        coord_prev.setCoordY(y);

        int directionMPError = AppExemplo2.getRand().nextInt(8) + 1;

        Coordinate coord_erro = Coordinate.newCoordinateWithError(coord_prev, smartThing.getMobilityPrecitionError(), directionMPError);

        saveDistance(smartThing.getTravelTimeId(), coord_atual, coord_prev, coord_erro, Distances.checkDistance(coord_atual, coord_prev), Distances.checkDistance(coord_atual, coord_erro), Distances.checkDistance(coord_prev, coord_erro), smartThing.getSpeed(), "distancias_migracao.txt");

        smartThing.setFutureCoord(coord_erro.getCoordX(), coord_erro.getCoordY());

        List<FogDevice> newServerCloudlets = new ArrayList<>();

        int localServerCloudlet;
        boolean cone;
        for (FogDevice sc : oldServerCloudlets) {
            localServerCloudlet = DiscoverLocalization.discoverLocal(
                    smartThing.getFutureCoord(), sc.getCoord());//return the relative position between Server Cloudlet and smart thing -> set this value
            cone = insideCone(localServerCloudlet, directionMPError);
            //, localAp);
            if (cone && (sc.getMyId() != smartThing.getSourceServerCloudlet().getMyId())) {
                newServerCloudlets.add(sc);
            }
        }
        return newServerCloudlets;

        //		
        //
        //		for(FogDevice sc:oldServerCloudlets){
        //			if(sc.isAvailable()){				
        //
        //				scCoordX = sc.getCoord().getCoordX();
        //				scCoordY = sc.getCoord().getCoordY();
        //				if(smartThing.getDirection()==Directions.EAST){
        //					if(scCoordX > stCoordX){ //180 degrees
        //						newServerCloudlets.add(sc);
        //					}
        //					continue;
        //				}
        //				else if(smartThing.getDirection()==Directions.NORTH){
        //					if(scCoordY > stCoordY){ //180 degrees
        //						newServerCloudlets.add(sc);
        //					}
        //					continue;
        //				}
        //				else if(smartThing.getDirection()==Directions.WEST){
        //					if(scCoordX < stCoordX){ //180 degrees
        //						newServerCloudlets.add(sc);
        //					}
        //					continue;
        //				}
        //				else if(smartThing.getDirection()==Directions.SOUTH){
        //					if(scCoordY < stCoordY){ //180 degrees
        //						newServerCloudlets.add(sc);
        //					}
        //					continue;
        //				}
        //				else if(smartThing.getDirection()==Directions.NORTHEAST){
        //					if(scCoordX > stCoordX && scCoordY > stCoordY){//90 degrees 
        //						newServerCloudlets.add(sc);
        //					}
        //					continue;
        //				}
        //				else if(smartThing.getDirection()==Directions.NORTHWEST){
        //					if(scCoordX < stCoordX && scCoordY > stCoordY){ //90 degrees
        //						newServerCloudlets.add(sc);					
        //					}
        //
        //					continue;
        //				}
        //				else if(smartThing.getDirection()==Directions.SOUTHWEST){
        //					if(scCoordX < stCoordX && scCoordY < stCoordY ){ //90 degrees
        //						newServerCloudlets.add(sc);					
        //					}
        //					continue;
        //				}
        //				else if(smartThing.getDirection()==Directions.SOUTHEAST){
        //					if(scCoordX > stCoordX && scCoordY < stCoordY ){ //90 degrees
        //						newServerCloudlets.add(sc);					
        //					}
        //					continue;
        //				}
        //
        //			}
        //		}
        //		return newServerCloudlets;
        //
    }

    public static int nextServerCloudlet(List<FogDevice> serverCloudlets, MobileDevice smartThing) {//Policy: the closest serverCloudlet

        setServerCloudletsAvailable(serverClouletsAvailableList(serverCloudlets, smartThing));
        if (getServerCloudletsAvailable().size() == 0) {
            return -1;
        } else {
            return Distances.theClosestServerCloudlet(getServerCloudletsAvailable(), smartThing);
        }
    }

    public static boolean isEdgeAp(ApDevice apDevice, MobileDevice smartThing) {
        if (apDevice.getServerCloudlet().getMyId() == smartThing.getSourceServerCloudlet().getMyId())// verify if the next Ap is edge
        {
            return false;
        } else {
            return true;
        }
    }

    public static int lowestLatencyCostServerCloudlet(List<FogDevice> oldServerCloudlets, List<ApDevice> oldApDevices, MobileDevice smartThing) {
        System.out.println("--------------------------> SEARCHING LOW LATENCY CLOUDLET");
        List<FogDevice> newServerCloudlets = new ArrayList<>();
        List<FogDevice> numServerCloudlets = new ArrayList<>();
        List<Double> costList = new ArrayList<>();

        for (FogDevice sc : oldServerCloudlets) { //adiciona todos os cloudlets na lita temp
            newServerCloudlets.add(sc);
        }

        for (int i = 0; i < 9; i++) { //seleciona 10 server mais proximos
            int destinationServerCloudlet = nextServerCloudlet(newServerCloudlets, smartThing);
            if (destinationServerCloudlet >= 0) {
                for (FogDevice sc1 : newServerCloudlets) {
                    if (sc1.getMyId() == destinationServerCloudlet) {
                        numServerCloudlets.add(sc1); //se o mais prox ecistir em tempo adicionar na temp2 o selecionado
                        break;
                    }
                }

                FogDevice sc = null;
                //remove o cloudlet para n ser mais escolhido
                for (int j = 0; j < newServerCloudlets.size(); j++) {
                    sc = newServerCloudlets.get(j);
                    if (sc.getMyId() == destinationServerCloudlet) {
                        newServerCloudlets.remove(sc);
                        break;
                    }
                }
            } else {
                break;
            }
        }

        if (numServerCloudlets.size() == 0) {
            return -1;
        }
        //this point numServerCloudlets has + than 0 and - than 10 sc
        double sumCost;
        int choose = -1;
        double minCost = -1;
        int idNextAp = nextAp(oldApDevices, smartThing);

        if (idNextAp < 0) {
            return -1;
        }

        for (FogDevice sc : oldServerCloudlets) {
            System.out.println("sum cost func: " + sc.getMyId() + " | " + sumCostFunction(sc, oldApDevices.get(idNextAp), smartThing));
        }
        for (int i = 0; i < numServerCloudlets.size(); i++) {
            minCost = sumCostFunction(numServerCloudlets.get(i), oldApDevices.get(idNextAp), smartThing);
            System.out.println("min cost: [" + numServerCloudlets.get(i).getMyId() + "]" + minCost);
            if (minCost >= 0) {
                System.out.println("selecionado " + numServerCloudlets.get(i).getMyId());
                choose = numServerCloudlets.get(i).getMyId();
                break;
            }
        }
        if (minCost < 0) {
            return -1;
        }

        for (FogDevice sc : numServerCloudlets) {
            sumCost = sumCostFunction(sc, oldApDevices.get(idNextAp), smartThing);
            costList.add(sumCost);
            System.out.println(sumCost);
            if (sumCost < 0) {
                continue;
            }
            if (sumCost < minCost) {
                System.out.println("change :" + choose + " / " + sc.getMyId());
                minCost = sumCost;
                choose = sc.getMyId();
            }
        }
        return choose;
    }

    public static void lowestLatencyCostServerCloudletILP(List<FogDevice> oldServerCloudlets, List<ApDevice> oldApDevices, MobileDevice smartThing) {
        List<FogDevice> clusterOfCloudlets = new ArrayList<>();
        List<Double> costList = new ArrayList<>();

        Double sumCost;
        int idNextAp = nextAp(oldApDevices, smartThing);

        clusterOfCloudlets = serverClouletsAvailableList(oldServerCloudlets, smartThing);
        for (FogDevice sc : clusterOfCloudlets) {
            sumCost = sumCostFunction(sc, oldApDevices.get(idNextAp), smartThing);
            costList.add(sumCost);
        }
        List< List<Double>> latencyMatrix = getLatencyMatrix(smartThing.getFutureCoord());
        latencyMatrix.add(costList);
        setLatencyMatrix(findCluster(smartThing.getFutureCoord()), latencyMatrix);
    }

    public static double sumCostFunction(FogDevice serverCloudlet, ApDevice nextAp, MobileDevice smartThing) {
        double sum = -1;
        if (nextAp.getServerCloudlet().equals(serverCloudlet)) {
            sum = NetworkTopology.getDelay(smartThing.getId(), nextAp.getId())
                    + NetworkTopology.getDelay(nextAp.getId(), nextAp.getServerCloudlet().getId())
                    + (1.0 / nextAp.getServerCloudlet().getHost().getAvailableMips())
                    + LatencyByDistance.latencyConnection(nextAp.getServerCloudlet(), smartThing);
        } else {
            sum = NetworkTopology.getDelay(smartThing.getId(), nextAp.getId())
                    + NetworkTopology.getDelay(nextAp.getId(), nextAp.getServerCloudlet().getId())
                    + 1.0 //router
                    + NetworkTopology.getDelay(nextAp.getServerCloudlet().getId(), serverCloudlet.getId())
                    + (1.0 / serverCloudlet.getHost().getAvailableMips())
                    + LatencyByDistance.latencyConnection(serverCloudlet, smartThing);
        }
        return sum;
    }

    private static List<List<Double>> getLatencyMatrix(Coordinate futureCoord) {
        // TODO Auto-generated method stub
        return null;
    }

    static void setLatencyMatrix(int cluster, List< List<Double>> latencyMatrix) {

    }

    static int findCluster(Coordinate stCoord) {
        return 1;
    }

    static List<FogDevice> getCluster(int i) {
        return null;
    }

    public static boolean isMigrationPoint() {
        return migrationPoint;
    }

    public static void setMigrationPoint(boolean migrationPoint) {
        Migration.migrationPoint = migrationPoint;
    }

    public static boolean isMigrationZone() {
        return migrationZone;
    }

    public static void setMigrationZone(boolean migrationZone) {
        Migration.migrationZone = migrationZone;
    }

    public int getLocation() {
        return location;
    }

    public void setLocation(int location) {
        this.location = location;
    }

    public ApDevice getCorrentAP() {
        return correntAP;
    }

    public void setCorrentAP(ApDevice correntAP) {
        this.correntAP = correntAP;
    }

    public FogDevice getCorrentServerCloudlet() {
        return correntServerCloudlet;
    }

    public void setCorrentServerCloudlet(FogDevice correntServerCloudlet) {
        this.correntServerCloudlet = correntServerCloudlet;
    }

    public MobileDevice getCorrentSmartThing() {
        return correntSmartThing;
    }

    public void setCorrentSmartThing(MobileDevice correntSmartThing) {
        this.correntSmartThing = correntSmartThing;
    }

    public ApDevice getApAvailable() {
        return apAvailable;
    }

    public void setApAvailable(ApDevice apAvailable) {
        this.apAvailable = apAvailable;
    }

    public FogDevice getServerCloudletAvailable() {
        return serverCloudletAvailable;
    }

    public void setServerCloudletAvailable(FogDevice serverCloudletAvailable) {
        this.serverCloudletAvailable = serverCloudletAvailable;
    }

    public int getFlowDirection() {
        return flowDirection;
    }

    public void setFlowDirection(int flowDirection) {
        this.flowDirection = flowDirection;
    }

    public static List<ApDevice> getApsAvailable() {
        return apsAvailable;
    }

    public static void setApsAvailable(List<ApDevice> apsAvailable) {
        Migration.apsAvailable = apsAvailable;
    }

    public static List<FogDevice> getServerCloudletsAvailable() {
        return serverCloudletsAvailable;
    }

    public static void setServerCloudletsAvailable(List<FogDevice> serverCloudletsAvailable) {
        Migration.serverCloudletsAvailable = serverCloudletsAvailable;
    }

    public static int getPolicyReplicaVM() {
        return policyReplicaVM;
    }

    public static void setPolicyReplicaVM(int policyReplicaVM) {
        Migration.policyReplicaVM = policyReplicaVM;
    }

    public static Random getRand() {
        return rand;
    }

    /**
     *
     * @param distance
     * @return boolean about if is or isn't in migration point
     */
}
