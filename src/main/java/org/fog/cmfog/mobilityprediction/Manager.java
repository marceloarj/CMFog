package org.fog.cmfog.mobilityprediction;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.cmfog.CMFog;
import org.fog.cmfog.CMFogMobileController;
import org.fog.cmfog.helpers.CMFogHelper;
import org.fog.cmfog.helpers.NetworkHelper;
import org.fog.cmfog.SimulationParameters;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.cmfog.rmodule.RMiddleware;

public class Manager {

    public static int predictMobility(MobilityPrediction mobilityPrediction, MobileDevice aMobileDevice, List<FogDevice> theFognodes) {
        double mBenef[][] = null;
        String search = "";
        FogDevice actualCloudlet = aMobileDevice.getSourceAp().getServerCloudlet();
        FogDevice cloudletToPair = null;
        List<FogDevice> mobilityTrack = new ArrayList<>(aMobileDevice.getMobilityTrack());
        //if (SimulationParameters.getLayer() == 1) {
        System.out.println("User mobility L1: " + mobilityTrack);
        //} else {
        List<FogDevice> temp = new ArrayList<>();
        FogDevice lastAdded = null;
        for (FogDevice iterator : mobilityTrack) {
            FogDevice aParent = ((FogDevice) CloudSim.getEntity(iterator.getParentId()));
            if (aParent != lastAdded) {
                temp.add(aParent);
                lastAdded = aParent;
            }
        }
        System.out.println("User mobility L2: " + temp);
        //}
        System.out.println("VM's mobility: " + aMobileDevice.getMigrationTrack());

        if (mobilityTrack.isEmpty()) {
            search = CMFogHelper.generateSearchTag(actualCloudlet, null);
            System.out.println("Searching for (empty track): " + search);
            mBenef = generateBenefits(mobilityPrediction.getMobilityProbability(aMobileDevice, search));
        } else {
            List<FogDevice> mobilityTrackCP = new ArrayList<>(mobilityTrack);
            while (mBenef == null && !mobilityTrackCP.isEmpty()) {
                cloudletToPair = mobilityTrackCP.get(mobilityTrackCP.size() - 1);
                mobilityTrackCP.remove(cloudletToPair);
                System.out.println(cloudletToPair.getName() + " - " + actualCloudlet.getName());
                search = CMFogHelper.generateSearchTag(actualCloudlet, cloudletToPair);
                System.out.println("Searching for: " + search);
                mBenef = generateBenefits(mobilityPrediction.getMobilityProbability(aMobileDevice, search));
            }
        }

        if (mBenef == null) {
            System.out.println("----> No mobility prediction can be made right now");
            return -1;
        }

        double mCost[][] = generateCosts(mobilityPrediction.getTotalFN(), aMobileDevice, NetworkHelper.getUplinkBW(aMobileDevice), theFognodes, mobilityPrediction);
        for (int i = 0; i < mBenef.length; i++) {
            if (mBenef[i][1] == 0.0) {
                mCost[i][1] = Double.MAX_VALUE;
            }
        }
        double[][] MADMResult = executeMADM_T(mBenef, mCost);
        double minDistance = Double.MAX_VALUE;
        int selectedFN = (int) MADMResult[0][0];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < MADMResult[0].length; j++) {
                System.out.print(String.format("%.04f", MADMResult[i][j]) + " ");
            }
            FogDevice cloudlet = CMFogHelper.getCloudletByMyId(mobilityPrediction.getIdMapping()[(int) MADMResult[i][0]]);
            if ((MADMResult[i][1] != MADMResult[MADMResult.length - 1][1]) && CMFogHelper.calculateDistance(aMobileDevice.getCoord(), cloudlet.getCoord()) < minDistance) {
                minDistance = CMFogHelper.calculateDistance(aMobileDevice.getCoord(), cloudlet.getCoord());
                selectedFN = (int) MADMResult[i][0];
                //System.out.println(">>>>>>> CHOOOSING OPTION " + (i + 1) + "<<<<<<<<<<<<<<<<");
            }
            System.out.println(" - " + String.format("%.04f", CMFogHelper.calculateDistance(aMobileDevice.getCoord(), cloudlet.getCoord())));// + " / " + cloudlet.getCoord());
        }
        //int selectedFN = executeMADM(mBenef, mCost);
        System.out.println("comparing: " + mobilityPrediction.getIdMapping()[selectedFN] + " - " + aMobileDevice.getVmLocalServerCloudlet().getMyId());
        if (mobilityPrediction.getIdMapping()[selectedFN] == aMobileDevice.getVmLocalServerCloudlet().getMyId()) {
            System.out.println("----> Migration for same cloudlet not allowed");
            return -1;
        }
        //?
        //String searchPermTime = String.valueOf(aMobileDevice.getVmLocalServerCloudlet().getMyId()).concat(String.valueOf(mobilityPrediction.getIdMapping()[selectedFN]));
        FogDevice leaf = CMFogHelper.getCloudletByMyId(mobilityPrediction.getIdMapping()[selectedFN]);
        actualCloudlet = aMobileDevice.getSourceAp().getServerCloudlet(); // BIG CHANGE HERE
        if (aMobileDevice.getLayer() == 2) {
            System.out.println(leaf.getChildrenCloudlets().size());
            System.out.println(leaf.getChildrenCloudlets());
            leaf = leaf.getChildrenCloudlets().get(0);
        }

        //System.out.println("AC: " + actualCloudlet.getName());
        String searchPermTime = CMFogHelper.generateSearchTag(leaf, actualCloudlet);
        //System.out.println("SFN: " + selectedFN + " - LEAF:" + leaf);
        System.out.println("Search for permtime: " + searchPermTime);
//        if (mobilityPrediction.predictedTime(aMobileDevice.getMyId(), searchPermTime) != null && mobilityPrediction.predictedTime(aMobileDevice.getMyId(), searchPermTime) <= 50) {
//            System.out.println(">>>Too Short permtime<<<");
//            return -1;
//        }
        aMobileDevice.setEstimatedPermTime(mobilityPrediction.predictedTime(aMobileDevice.getMyId(), searchPermTime));

        aMobileDevice.setSearchForLastMigration(searchPermTime);

        System.out.println(aMobileDevice.getSearchForLastMigration() + " - " + aMobileDevice.getEstimatedPermTime());
        System.out.println("Selected Cloudlet: " + mobilityPrediction.getIdMapping()[selectedFN]);
        return mobilityPrediction.getIdMapping()[selectedFN];
    }

    private static double[][] generateCosts(int totalFN, MobileDevice aMobileDevice, double myBW, List<FogDevice> theFognodes, MobilityPrediction aMobilityPrediction) {
        double mCost[][] = new double[totalFN][2];
        for (int i = 0; i < totalFN; i++) {
            mCost[i][0] = i;
            if (aMobilityPrediction.mapping(aMobileDevice.getVmLocalServerCloudlet().getMyId()) == i) {
                //if (aMobileDevice.getParentId() == i) {
                mCost[i][1] = Integer.MAX_VALUE;
            } else {
                double predictionBW = identifyMBW(aMobileDevice.getVmLocalServerCloudlet().getMyId(), theFognodes, aMobilityPrediction.getIdMapping()[i]);
                double migrationRate = NetworkHelper.multiplicativeMigrationDelay(aMobileDevice.getVmLocalServerCloudlet().getMyId(), aMobilityPrediction.getIdMapping()[i]);
                mCost[i][1] = migrationCost(myBW, predictionBW, aMobileDevice.getVmMobileDevice().getSize(), migrationRate); // custo

                // mCost[i][2] = quantidade de nó no destino
                // mCost[i][3] = tempo de acesso destino
            }
        }
        return mCost;
    }

    private static double identifyMBW(int cloudletId, List<FogDevice> theFognodes, int possibleCloudlet) {
        FogDevice connectedNode = CMFogHelper.getCloudletByMyId(cloudletId, theFognodes);
        FogDevice alternativeNode = CMFogHelper.getCloudletByMyId(possibleCloudlet, theFognodes);
        return connectedNode.getUplinkBandwidth() <= alternativeNode.getUplinkBandwidth() ? connectedNode.getUplinkBandwidth() : alternativeNode.getUplinkBandwidth();
    }

    private static double migrationCost(double originBW, double destinyBW, double contentSize, double migrationRate) {
        double minBW = originBW >= destinyBW ? destinyBW : originBW;
        double migrationCost = contentSize / minBW;
        migrationCost = migrationCost + (migrationCost * migrationRate * 0.5);
        //System.out.println(minBW + " - " + contentSize + " - " + migrationRate + " - " + migrationCost);
        return migrationCost;
    }

    private static double[][] generateBenefits(double[] mobilityProbability) {
        if (mobilityProbability == null) {

            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>problme here");
            return null;
        }
        double mBenef[][] = new double[mobilityProbability.length][2];
        for (int i = 0; i < mobilityProbability.length; i++) {
            mBenef[i][0] = i;
            mBenef[i][1] = mobilityProbability[i];
        }
        return mBenef;
    }

    private static int executeMADM(double mBenef[][], double mCost[][]) {
        double ranking[][] = RMiddleware.callMethodical(mBenef, mCost);
        /*
		 * double max = 0; int selectedFN = -1; for (int i = 0; i <= ranking[0].length;
		 * i++) { if (ranking[i][1] > max) { max = ranking[i][1]; selectedFN = (int)
		 * ranking[i][0]; } }
         */
        System.out.println("MADM Result " + ranking[0][0]);
        for (int i = 0; i < ranking.length; i++) {
            for (int j = 0; j < ranking[0].length; j++) {
                System.out.print(ranking[i][j] + " ");
            }
            System.out.println(" ");
        }
        return (int) ranking[0][0];
    }

    private static double[][] executeMADM_T(double mBenef[][], double mCost[][]) {
        for (int x = 0; x < mBenef.length; x++) {
            if (mBenef[x][1] != 0) {
                System.out.println("[" + mBenef[x][0] + "] = " + mBenef[x][1] + " | " + mCost[x][1]);
            }
        }
        double ranking[][] = RMiddleware.callMethodical(mBenef, mCost);
        /*
		 * double max = 0; int selectedFN = -1; for (int i = 0; i <= ranking[0].length;
		 * i++) { if (ranking[i][1] > max) { max = ranking[i][1]; selectedFN = (int)
		 * ranking[i][0]; } }
         */
        System.out.println("MADM Result " + ranking[0][0]);
//        for (int i = 0; i < ranking.length; i++) {
//            for (int j = 0; j < ranking[0].length; j++) {
//                System.out.print(ranking[i][j] + " ");
//            }
//            System.out.println(" ");
//        }
        return ranking;
    }

    public static Integer syncForVerticalMigration(MobileDevice aMobileDevice) {
        Double timeLeft = aMobileDevice.getNextMigration() - CloudSim.clock();
        Integer migrationTime = aMobileDevice.getEstimatedPermTime();
        System.out.println("TL: " + timeLeft + " NextM: " + aMobileDevice.getNextMigration() + " PermT: " + migrationTime);
        //System.out.println("MT SIZE: " + aMobileDevice.getMobilityTrack().size());
        FogDevice last = aMobileDevice.getMobilityTrack().get(aMobileDevice.getMobilityTrack().size() - 1);
        FogDevice actual = aMobileDevice.getSourceAp().getServerCloudlet();
        System.out.println("Search String: " + last.getMyId() + "/" + actual.getMyId());
        ///////////////////////////////////////
        double mBenef[][] = generateBenefits(CMFog.mobilityPredictionL1.getMobilityProbability(aMobileDevice, last.getMyId() + "/" + actual.getMyId()));
        double mCost[][] = generateCosts(CMFog.mobilityPredictionL1.getTotalFN(), aMobileDevice, NetworkHelper.getUplinkBW(aMobileDevice), CMFogMobileController.getCloudletsL1(), CMFog.mobilityPredictionL1);
        for (int i = 0; i < mBenef.length; i++) {
            if (mBenef[i][1] == 0.0) {
                mCost[i][1] = Double.MAX_VALUE;
            }
        }
        int selectedFN = executeMADM(mBenef, mCost);
        FogDevice next = CMFogHelper.getCloudletByMyId(selectedFN);
        ///////////////////////////////////////
        FogDevice actualParent = ((FogDevice) CloudSim.getEntity(actual.getParentId()));
        FogDevice nextParent = ((FogDevice) CloudSim.getEntity(next.getParentId()));
        ///
        System.out.println(last.getName() + " - " + actual.getName() + " - " + next.getName());
        System.out.println(((FogDevice) CloudSim.getEntity(last.getParentId())).getName() + " - " + actualParent.getName() + " - " + nextParent.getName());
        ///
        System.out.println("Search String(P): " + ((FogDevice) CloudSim.getEntity(last.getParentId())).getMyId() + "/" + actualParent.getMyId());
        System.out.println("Search String(P[A,N]) " + actualParent.getMyId() + "/" + nextParent.getMyId());
        while (actualParent.getMyId() == nextParent.getMyId()) {
            String searchTag = actual.getMyId() + "/" + next.getMyId();
            System.out.println("Search String: " + searchTag);
            System.out.println("Search String(P): " + actualParent.getMyId() + "/" + nextParent.getMyId());
            migrationTime += CMFog.mobilityPredictionL1.predictedTime(aMobileDevice.getMyId(), searchTag);
            mBenef = generateBenefits(CMFog.mobilityPredictionL1.getMobilityProbability(aMobileDevice, searchTag));
            mCost = generateCosts(CMFog.mobilityPredictionL1.getTotalFN(), aMobileDevice, NetworkHelper.getUplinkBW(aMobileDevice), CMFogMobileController.getCloudletsL1(), CMFog.mobilityPredictionL1);
            for (int i = 0; i < mBenef.length; i++) {
                if (mBenef[i][1] == 0.0) {
                    mCost[i][1] = Double.MAX_VALUE;
                }
            }
            selectedFN = executeMADM(mBenef, mCost);
            actualParent = nextParent;
            nextParent = CMFogHelper.getCloudletByMyId(selectedFN);
        }
        return migrationTime;
    }
}
