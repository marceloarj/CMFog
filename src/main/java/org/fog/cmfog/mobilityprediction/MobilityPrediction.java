package org.fog.cmfog.mobilityprediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.fog.cmfog.CMFog;
import org.fog.cmfog.helpers.CMFogHelper;
import org.fog.cmfog.SimulationParameters;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.mfmf.simulator.TraceRecord;

public class MobilityPrediction {

    private List<FogDevice> fognodes;
    private List<MobileDevice> mobiledevices;
    private Map<Integer, Map<String, double[]>> probabilityMatrix2;
    private Map<Integer, Map<String, Integer>> timeMatrix;
    private Integer[] idMapping;

    public static Integer predictedTimeByLayer(Integer aMobileDeviceID, String toSearch) {
        Integer slotTime = null;
        if (SimulationParameters.getLayer() == 1) {
            slotTime = CMFog.mobilityPredictionL1.predictedTime(aMobileDeviceID, toSearch);
        } else if (SimulationParameters.getLayer() == 2) {
            slotTime = CMFog.mobilityPredictionL2.predictedTime(aMobileDeviceID, toSearch);
        }
        return slotTime;
    }

    public int getTotalFN() {
        return fognodes.size();
    }

    public Integer predictedTime(Integer mdID, String state) {
        System.out.println("Predicting time for " + mdID + " with search tag " + state);
        //System.out.println(timeMatrix.get(mdID));
        return timeMatrix.get(mdID).get(state);
    }

    public MobilityPrediction(List<FogDevice> fognodes) {

        this.fognodes = fognodes;
        this.probabilityMatrix2 = new HashMap<>();
    }

    public MobilityPrediction(List<FogDevice> fognodes, List<MobileDevice> mobiledevices) {
        this.fognodes = fognodes;
        this.mobiledevices = mobiledevices;
        this.probabilityMatrix2 = new HashMap<>();
        this.timeMatrix = new HashMap<>();
        this.idMapping = new Integer[this.fognodes.size()];
        System.out.println("--------------- here --------------");
        for (int anIndex = 0; anIndex < idMapping.length; anIndex++) {
            idMapping[anIndex] = fognodes.get(anIndex).getMyId();
            System.out.println(anIndex + " -> " + fognodes.get(anIndex).getMyId());
        }
        System.out.println("--------------- here --------------");
    }

    public Integer mapping(Integer aCloudletId) {

        for (int anIndex = 0; anIndex < idMapping.length; anIndex++) {
            // System.out.println(aCloudletId + " - " + idMapping[anIndex]);
            if (Objects.equals(idMapping[anIndex], aCloudletId)) {

                return anIndex;
            }
        }
        System.out.println("--------------Mapping is null----------------------");
        return null;
    }

    public void generateTransictionMatrix() {

        matrixInstantiation();
        clusterization();
//        for (MobileDevice aMobileDevice : mobiledevices) {
//            Map<String, double[]> matrix = probabilityMatrix2.get(aMobileDevice.getMyId());
//            for (Map.Entry<String, double[]> line : matrix.entrySet()) {
//                System.out.print(line.getKey() + " | ");
//                for (int i = 0; i < line.getValue().length; i++) {
//                    System.out.print(matrix.get(line.getKey())[i] + " ");
//                }
//                System.out.println(" ");
//            }
//        }
        System.out.println("-------------------------------------------------------------------------------------------" + fognodes.size());
        markovChain();
        normalization();

        for (MobileDevice aMobileDevice : mobiledevices) {
            Map<String, double[]> matrix = probabilityMatrix2.get(aMobileDevice.getMyId());
            for (Map.Entry<String, double[]> line : matrix.entrySet()) {
                System.out.print(line.getKey() + " | ");
                for (int i = 0; i < line.getValue().length; i++) {
                    System.out.print(matrix.get(line.getKey())[i] + " ");
                }
                System.out.println(" ");
            }
        }
        System.out.println("==========================================================");
        for (MobileDevice aMobileDevice : mobiledevices) {
            Map<String, Integer> matrix = timeMatrix.get(aMobileDevice.getMyId());
            for (Map.Entry<String, Integer> line : matrix.entrySet()) {
                System.out.println(line.getKey() + " | " + line.getValue());

            }
        }
    }

    private void clusterization() {
        for (MobileDevice aMobileDevice : mobiledevices) {
            for (Map.Entry<Integer, List<TraceRecord>> aDayTrace : aMobileDevice.getMobilityTrace().entrySet()) {
                List<TraceRecord> aTrace = aDayTrace.getValue();
                List<TraceRecord> toRemove = new ArrayList<>();
                for (TraceRecord aRecord : aTrace) {
                    aRecord.setNode(findConnectedFG(aRecord.getX(), aRecord.getY()));
                    if (aRecord.getNode() == null) {
                        toRemove.add(aRecord);
                        // System.out.println("NULL: " + aRecord.getX() + " - " + aRecord.getY());
                        continue;
                    }
                    //System.out.println(aRecord.getX() + " - " + aRecord.getY() + " / " + aRecord.getNode().getMyId());
                }
                for (TraceRecord aRecord : toRemove) {
                    aTrace.remove(aRecord);
                }
                //System.out.println("TRACE SIZE END: " + aTrace.size());
            }
        }

    }

    private FogDevice findConnectedFG(double x, double y) {
        FogDevice aFogNode = null;
        double minDistance = Double.MAX_VALUE;
        for (FogDevice temp : fognodes) {
            double firstTerm = Math.pow(x - temp.getCoord().getCoordX(), 2);
            double secondTerm = Math.pow(y - temp.getCoord().getCoordY(), 2);
            double distance = Math.sqrt(firstTerm + secondTerm);
            if (distance < minDistance) {
                minDistance = distance;
                aFogNode = temp;
            }
        }
        return aFogNode;
    }

//    private FogDevice findConnectedFG(double x, double y) {
//        FogDevice aFogNode = null;
//        for (FogDevice temp : fognodes) {
//            double coordX = x;
//            double coordY = y;
//            double FDcoordX = temp.getCoord().getCoordX();
//            double FDcoordY = temp.getCoord().getCoordY();
//            // System.out.println(FDcoordX + " - " + FDcoordY);
//            double startX = FDcoordX - MaxAndMin.AP_COVERAGE / 2;
//            double endX = FDcoordX + MaxAndMin.AP_COVERAGE / 2;
//            double startY = FDcoordY - MaxAndMin.AP_COVERAGE / 2;
//            double endY = FDcoordY + MaxAndMin.AP_COVERAGE / 2;
//            if ((startX <= coordX && coordX <= endX) && (startY <= coordY && coordY <= endY)) {
//                //  System.out.println("X:" + startX + " / "+endX + "  Y:" + startY + " / "+endY);
//                aFogNode = temp;
//                break;
//            }
//        }
//        return aFogNode;
//    }
    private void markovChain() {
        for (MobileDevice aMobileDevice : mobiledevices) {
            for (Map.Entry<Integer, List<TraceRecord>> aDayTrace : aMobileDevice.getMobilityTrace().entrySet()) {
                updateMarkovChain(aDayTrace.getValue(), aMobileDevice);
            }
        }
    }

//    private void updateMarkovChain(List<TraceRecord> traces, MobileDevice aMobileDevice) {
//        int lastFN = -1;
//        for (int i = 0; i < traces.size() - 1; i++) {
//            int actualFN = traces.get(i).getNode().getMyId();
//            int nextFN = traces.get(i + 1).getNode().getMyId();
//            if (actualFN == nextFN) {
//                continue;
//            }
//            // this.probabilityMatrix.get(aMobileDevice.getMyId())[actualFN][nextFN] = this.probabilityMatrix.get(aMobileDevice.getMyId())[actualFN][nextFN] + 1;
//            //
//            Map<String, double[]> tranM = this.probabilityMatrix2.get(aMobileDevice.getMyId());
//            tranM.get(String.valueOf(actualFN))[nextFN] = tranM.get(String.valueOf(actualFN))[nextFN] + 1;
//            //
//            aMobileDevice.getTotalTransition()[actualFN] = aMobileDevice.getTotalTransition()[actualFN] + 1;
//        }
//    }
    private void updateMarkovChain(List<TraceRecord> traces, MobileDevice aMobileDevice) {
        int lastFN = -1;
        int count = 0;
        for (int i = 0; i < traces.size() - 1; i++) {
            int actualFN = traces.get(i).getNode().getMyId();
            int nextFN = traces.get(i + 1).getNode().getMyId();
            count++;
            if (actualFN == nextFN || lastFN == actualFN) {
                continue;
            }
            String key = "";
            if (lastFN == -1) {
                key = String.valueOf(actualFN);
            } else {
                key = String.valueOf(lastFN).concat("/").concat(String.valueOf(actualFN));
            }
            // System.out.println(lastFN + " - " + actualFN + " - " + nextFN);
            Map<String, double[]> tranM = this.probabilityMatrix2.get(aMobileDevice.getMyId());
            if (!tranM.containsKey(key)) {
                double[] mobilityMatrix2 = new double[fognodes.size()];
                tranM.put(key, mobilityMatrix2);
            }
            System.out.println(key + " - " + nextFN);
//            if (fognodes.size() == 36) {
//                tranM.get(key)[nextFN - 121] = tranM.get(key)[nextFN - 121] + 1;
//            } else {
//                tranM.get(key)[nextFN] = tranM.get(key)[nextFN] + 1;
//            }
            tranM.get(key)[mapping(nextFN)] = tranM.get(key)[mapping(nextFN)] + 1;
            //
            //////////////////
            Map<String, Integer> timeM = this.timeMatrix.get(aMobileDevice.getMyId());
            if (!timeM.containsKey(key)) {
                timeM.put(key, count);
            } else {
                timeM.put(key, timeM.get(key) + count);
            }
            count = 0;
            /////////////////

            Map<String, Integer> totalTransition = aMobileDevice.getTotalTranstion2();
            if (!totalTransition.containsKey(key)) {
                totalTransition.put(key, 1);
            } else {
                int partial = totalTransition.get(key);
                totalTransition.put(key, partial + 1);
            }
            ////////////////////////////
            Map<String, Integer> totalTime = aMobileDevice.getTotalTime();
            if (!totalTime.containsKey(key)) {
                totalTime.put(key, 1);
            } else {
                int partial = totalTime.get(key);
                totalTime.put(key, partial + 1);
            }
            ////////////////////////////
            lastFN = actualFN;
        }
    }

//    private void updateMarkovChain(List<TraceRecord> traces, MobileDevice aMobileDevice) {
//        int lastFN = traces.get(0).getNode().getMyId();
//        for (int i = 1; i < traces.size() - 1; i++) {
//            int actualFN = traces.get(i).getNode().getMyId();
//            int nextFN = traces.get(i + 1).getNode().getMyId();
//            if (actualFN == nextFN || lastFN == actualFN) {
//                continue;
//            }
//            String key = String.valueOf(lastFN).concat(String.valueOf(actualFN));
//            System.out.println(lastFN + " - " + actualFN + " - " + nextFN);
//            Map<String, double[]> tranM = this.probabilityMatrix2.get(aMobileDevice.getMyId());
//            if (!tranM.containsKey(key)) {
//                double[] mobilityMatrix2 = new double[fognodes.size()];
//                tranM.put(key, mobilityMatrix2);
//            }
//            tranM.get(key)[nextFN] = tranM.get(key)[nextFN] + 1;
//            //
//            Map<String, Integer> totalTransition = aMobileDevice.getTotalTranstion2();
//            if (!totalTransition.containsKey(key)) {
//                totalTransition.put(key, 1);
//            } else {
//                int partial = totalTransition.get(key);
//                totalTransition.put(key, partial + 1);
//            }
//            lastFN = actualFN;
//        }
//    }
//    private void normalization() {
//        for (MobileDevice aMobileDevice : mobiledevices) {
//            for (int i = 0; i < fognodes.size(); i++) {
//                for (int j = 0; j < fognodes.size(); j++) {
//                    //System.out.print(this.probabilityMatrix[i][j] + "  ");
//                    if (aMobileDevice.getTotalTransition()[i] > 0) {
//                        //       this.probabilityMatrix.get(aMobileDevice.getMyId())[i][j] = (100 * this.probabilityMatrix.get(aMobileDevice.getMyId())[i][j]) / aMobileDevice.getTotalTransition()[i];
//                        //
//                        Map<String, double[]> tranM = this.probabilityMatrix2.get(aMobileDevice.getMyId());
//                        tranM.get(String.valueOf(i))[j] = (100 * tranM.get(String.valueOf(i))[j]) / aMobileDevice.getTotalTransition()[i];
//                    }
//                }
//                //	System.out.println( " / "+totalTransition[i]);
//            }
//        }
//
//    }
    private void normalization() {
        for (MobileDevice aMobileDevice : mobiledevices) {
            Map<String, double[]> matrix = probabilityMatrix2.get(aMobileDevice.getMyId());
            /////////////////////////////////////////////////////////////////////////////////////////////////
            Map<String, Integer> timeCount = timeMatrix.get(aMobileDevice.getMyId());
            for (Map.Entry<String, Integer> state : timeCount.entrySet()) {

                int count = aMobileDevice.getTotalTime().get(state.getKey());
                // System.out.println(state.getKey() + " | " + timeCount.get(state.getKey()) + " - " + count + " = " + (timeCount.get(state.getKey()) / count));
                timeCount.put(state.getKey(), (timeCount.get(state.getKey()) / count));
            }
            //////////////////////////////////////////////////////////////////////////////////////////////////////
            for (Map.Entry<String, double[]> line : matrix.entrySet()) {
                for (int i = 0; i < line.getValue().length; i++) {
                    matrix.get(line.getKey())[i] = (100 * matrix.get(line.getKey())[i]) / aMobileDevice.getTotalTranstion2().get(line.getKey());
                }
            }
        }
    }

//    public double[] getMobilityProbability(MobileDevice aMobileDevice, int actualFG) {
//        // escolhe a matrix do user mdID
//        //return this.probabilityMatrix.get(aMobileDevice.getMyId())[actualFG];
//        if (aMobileDevice.getLastFN() == null) {
//            System.out.println("SEARCHING FOR: " + String.valueOf(actualFG));
//            return this.probabilityMatrix2.get(aMobileDevice.getMyId()).get(String.valueOf(String.valueOf(actualFG)));
//        } else {
//            System.out.println("SEARCHING FOR: " + String.valueOf(aMobileDevice.getLastFN().getMyId()).concat(String.valueOf(actualFG)));
//            // if (this.probabilityMatrix2.get(aMobileDevice.getMyId()).get(String.valueOf(aMobileDevice.getLastFN().getMyId()).concat(String.valueOf(actualFG))) == null) {
//            //  return this.probabilityMatrix2.get(aMobileDevice.getMyId()).get(String.valueOf(aMobileDevice.getLastlastFN().getMyId()).concat(String.valueOf(actualFG)));
//            //  }
//            return this.probabilityMatrix2.get(aMobileDevice.getMyId()).get(String.valueOf(aMobileDevice.getLastFN().getMyId()).concat(String.valueOf(actualFG)));
//        }
//
//    }
    public double[] getMobilityProbability(MobileDevice aMobileDevice, String search) {
//        System.out.println("Translated postion : " + mapping(aMobileDevice.getMyId()));
//        Map<String, double[]> get = this.probabilityMatrix2.get(mapping(aMobileDevice.getMyId()));
//        if (get != null) {
//            for (String a : get.keySet()) {
//                System.out.print(a + " - ");
//            }
//        }

        return this.probabilityMatrix2.get(aMobileDevice.getMyId()).get(search);//15/02
    }

    public Integer[] getIdMapping() {
        return idMapping;
    }

    private void matrixInstantiation() {
        for (MobileDevice aMobileDevice : mobiledevices) {
            //double[][] mobilityMatrix = new double[fognodes.size()][fognodes.size()];
            //  probabilityMatrix.put(aMobileDevice.getMyId(), mobilityMatrix);
            //
            // double[] mobilityMatrix2;
            Map<String, double[]> probM = new HashMap<>();
//            for (FogDevice aFog : this.fognodes) {
//                mobilityMatrix2 = new double[fognodes.size()];
//                probM.put(String.valueOf(aFog.getMyId()), mobilityMatrix2);
//            }
            probabilityMatrix2.put(aMobileDevice.getMyId(), probM);
            Map<String, Integer> timeM = new HashMap<>();
            timeMatrix.put(aMobileDevice.getMyId(), timeM);
            //
            aMobileDevice.setTotalTransition(new int[this.fognodes.size()]);
        }
    }

    public void parseTraces(MobileDevice aMobileDevice, ArrayList<String[]> trace) {
        List<TraceRecord> aTrace = new ArrayList<>();
        for (String[] record : trace) {
            TraceRecord aTraceRecord = new TraceRecord(Double.valueOf(record[2]), Double.valueOf(record[3]), aMobileDevice); // 0
            aTrace.add(aTraceRecord);
        }
        aMobileDevice.addTrace(aTrace);
    }

}
