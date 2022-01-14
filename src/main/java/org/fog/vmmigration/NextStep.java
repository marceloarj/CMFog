package org.fog.vmmigration;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.ApDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.Coordinate;
import org.fog.vmmobile.AppExemplo2;
import org.fog.vmmobile.LogMobile;
import org.fog.vmmobile.constants.Directions;
import org.fog.vmmobile.constants.Policies;

public class NextStep {
//	public  int getContNextStep() {
//		return contNextStep;
//	}
//
//	public  void setContNextStep(int contNextStep) {
//		this.contNextStep = contNextStep;
//	}

//	public  int contNextStep = 0;
    private static void saveMobility(MobileDevice st) {
//		System.out.println(st.getMyId() + " Position: " + st.getCoord().getCoordX() + ", " + st.getCoord().getCoordY() + " Direction: " + st.getDirection() + " Speed: " + st.getSpeed());
//		System.out.println("Source AP: " + st.getSourceAp() + " Dest AP: " + st.getDestinationAp() + " Host: " + st.getHost().getId());
//		System.out.println("Local server: " + st.getVmLocalServerCloudlet().getName() + " Apps " + st.getVmLocalServerCloudlet().getActiveApplications() + " Map " + st.getVmLocalServerCloudlet().getApplicationMap());
//		if(st.getDestinationServerCloudlet() == null){
//			System.out.println("Dest server: null Apps: null Map: null");
//		}
//		else{
//			System.out.println("Dest server: " + st.getDestinationServerCloudlet().getName() + " Apps: " + st.getDestinationServerCloudlet().getActiveApplications() +  " Map " + st.getDestinationServerCloudlet().getApplicationMap());
//		}
        try (FileWriter fw1 = new FileWriter(st.getMyId() + "out.txt", true);
                BufferedWriter bw1 = new BufferedWriter(fw1);
                PrintWriter out1 = new PrintWriter(bw1)) {
            out1.println(CloudSim.clock() + " " + st.getMyId() + " Position: " + st.getCoord().getCoordX() + ", " + st.getCoord().getCoordY() + " Direction: " + st.getDirection() + " Speed: " + st.getSpeed());
            out1.println("Source AP: " + st.getSourceAp() + " Dest AP: " + st.getDestinationAp() + " Host: " + st.getHost().getId());
            out1.println("Local server: " + st.getVmLocalServerCloudlet().getName() + " Apps " + st.getVmLocalServerCloudlet().getActiveApplications() + " Map " + st.getVmLocalServerCloudlet().getApplicationMap());
            if (st.getSourceServerCloudlet() == null) {
                out1.println("Source server: null Apps: null Map: null");
            } else {
                out1.println("Source server: " + st.getSourceServerCloudlet().getName() + " Apps: " + st.getSourceServerCloudlet().getActiveApplications() + " Map " + st.getSourceServerCloudlet().getApplicationMap());
            }
            if (st.getDestinationServerCloudlet() == null) {
                out1.println("Dest server: null Apps: null Map: null");
            } else {
                out1.println("Dest server: " + st.getDestinationServerCloudlet().getName() + " Apps: " + st.getDestinationServerCloudlet().getActiveApplications() + " Map " + st.getDestinationServerCloudlet().getApplicationMap());
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

        try (FileWriter fw = new FileWriter(st.getMyId() + "route.txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            out.println(st.getMyId() + "\t" + st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY() + "\t" + st.getDirection() + "\t" + st.getSpeed() + "\t" + CloudSim.clock());
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

        try (FileWriter fw = new FileWriter(st.getMyId() + "migrationPos.txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            if (st.getSourceServerCloudlet() == null) {
                out.println(st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY()
                        + "\t" + CloudSim.clock() + "\t" + st.getMigTime() + "\t" + (CloudSim.clock() + st.getMigTime()));
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

        try (FileWriter fw = new FileWriter(st.getMyId() + "handoffPos.txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            if (st.isLockedToHandoff()) {
                out.println(st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY()
                        + "\t" + CloudSim.clock());
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

        try (FileWriter fw = new FileWriter(st.getMyId() + "sourceAp.txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            if (st.getSourceAp() == null) {
                out.println(st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY()
                        + "\t" + CloudSim.clock());
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

        try (FileWriter fw = new FileWriter(st.getMyId() + "sourceServerCloudlet.txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            if (st.getSourceServerCloudlet() == null) {
                out.println(st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY()
                        + "\t" + CloudSim.clock());
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

        if (MyStatistics.getInstance().getInitialWithoutVmTime().get(st.getMyId()) != null) {
            try (FileWriter fw = new FileWriter(st.getMyId() + "withoutVmTime.txt", true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    PrintWriter out = new PrintWriter(bw)) {
                if (st.getSourceServerCloudlet() == null) {
                    out.println(st.getCoord().getCoordX() + "\t" + st.getCoord().getCoordY()
                            + "\t" + CloudSim.clock());
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

    public static void CMFog(List<MobileDevice> theMobileDevices) {
        Coordinate coordinate = new Coordinate();
        for (MobileDevice aMobileDevice : theMobileDevices) {
            if (aMobileDevice.getTravelTimeId() == -1) {
                continue;
            }
            coordinate.newCoordinate(aMobileDevice);
        }
    }

    public static void nextStep(List<FogDevice> serverCloudlets, List<ApDevice> apDevices, List<MobileDevice> smartThings, int stepPolicy) {
        Coordinate coordinate = new Coordinate();
        MobileDevice aMobileDevice = null;

        for (int aTempMD = 0; aTempMD < smartThings.size(); aTempMD++) {//It makes the new position according direction and speed
            aMobileDevice = smartThings.get(aTempMD);
            if (aMobileDevice.getTravelTimeId() == -1) {
                continue;
            }
            if ((aMobileDevice.getDirection() != Directions.NONE)) {
                coordinate.newCoordinate(aMobileDevice);
            }
            if (aMobileDevice.getCoord().getCoordX() == -1) {
                if (aMobileDevice.getSourceServerCloudlet() != null) {
                    int j = 0, indexCloud = 0;
                    for (FogDevice sc : serverCloudlets) {
                        if (aMobileDevice.getSourceServerCloudlet().equals(sc)) {
                            indexCloud = j;
                            break;
                        }
                        j++;
                    }
                    //serverCloudlets.get(st.getSourceServerCloudlet().getId()).getSmartThings().remove(st);
                    serverCloudlets.get(indexCloud).getSmartThings().remove(aMobileDevice);
                    j = 0;
                    int indexAp = 0;
                    for (ApDevice ap : apDevices) {
                        if (aMobileDevice.getSourceAp().equals(ap)) {
                            indexAp = j;
                            break;
                        }
                        j++;
                    }
//					apDevices.get(st.getSourceAp().getId()).getSmartThing().remove(st);
                    apDevices.get(indexAp).getSmartThings().remove(aMobileDevice);
                    aMobileDevice.setSourceAp(null);
                    System.out.println("NextStep.java setted CLOUDLET=NULL");
                    aMobileDevice.setSourceServerCloudlet(null);
                    aMobileDevice.setMigStatus(false);
                }
                if (aMobileDevice.getSourceAp() == null) {
                    smartThings.remove(aMobileDevice);
                    LogMobile.debug("NextStep.java", aMobileDevice.getName() + " was removed!");
                } else {
                    //caso termine logo apos um handoff. A st tera se conectado novamente ao ap mas ainda nao ocorreu a conexao st cloudlet
                    if (aMobileDevice.getSourceServerCloudlet() != null) {
                        aMobileDevice.getSourceServerCloudlet().setSmartThings(aMobileDevice, Policies.REMOVE); //it'll remove the smartThing from serverCloudlets-smartThing's set
                    }
                    aMobileDevice.getSourceAp().setSmartThings(aMobileDevice, Policies.REMOVE);//it'll remove the smartThing from ap-smartThing's set
                    LogMobile.debug("NextStep.java", aMobileDevice.getName() + " was removed!");
                    smartThings.remove(aMobileDevice);
                }
            } else {
                //System.out.println(aMobileDevice.getMyId() + "\t" + aMobileDevice.getCoord().getCoordX() + "\t" + aMobileDevice.getCoord().getCoordY() + "\t" + CloudSim.clock() + "\t" + Calendar.getInstance().getTime());
                saveMobility(aMobileDevice);
            }
            if (aMobileDevice.getDestinationAp() != null && aMobileDevice.getVmLocalServerCloudlet() != null) {
                AppExemplo2.temLog = AppExemplo2.temLog + aMobileDevice.getDestinationAp().getName() + " - " + aMobileDevice.getVmLocalServerCloudlet().getName() + " - ";
            }
        }
    }

    public static void nextStep(List<FogDevice> serverCloudlets, List<ApDevice> apDevices, List<MobileDevice> smartThings,
            Coordinate coordDevices, int stepPolicy, int seed) {
        MobileDevice aMobileDevice = null;
        Coordinate coordinate = new Coordinate();
        for (int aTempMD = 0; aTempMD < smartThings.size(); aTempMD++) {//It makes the new position according direction and speed
            aMobileDevice = smartThings.get(aTempMD);
            if (aMobileDevice.getTravelTimeId() == -1) {
                continue;
            }
            if ((aMobileDevice.getDirection() != Directions.NONE)) {
                coordinate.newCoordinate(aMobileDevice);
            }
            if (aMobileDevice.getCoord().getCoordX() == -1) {
                if (aMobileDevice.getSourceServerCloudlet() != null) {
                    int j = 0, indexCloud = 0;
                    for (FogDevice sc : serverCloudlets) {
                        if (aMobileDevice.getSourceServerCloudlet().equals(sc)) {
                            indexCloud = j;
                            break;
                        }
                        j++;
                    }
                    //serverCloudlets.get(st.getSourceServerCloudlet().getId()).getSmartThings().remove(st);
                    serverCloudlets.get(indexCloud).getSmartThings().remove(aMobileDevice);
                    j = 0;
                    int indexAp = 0;
                    for (ApDevice ap : apDevices) {
                        if (aMobileDevice.getSourceAp().equals(ap)) {
                            indexAp = j;
                            break;
                        }
                        j++;
                    }
//					apDevices.get(st.getSourceAp().getId()).getSmartThing().remove(st);
                    apDevices.get(indexAp).getSmartThings().remove(aMobileDevice);
                    aMobileDevice.setSourceAp(null);
                    System.out.println("NextStep.java setted CLOUDLET=NULL");
                    aMobileDevice.setSourceServerCloudlet(null);
                    aMobileDevice.setMigStatus(false);
                }
                if (aMobileDevice.getSourceAp() == null) {
                    smartThings.remove(aMobileDevice);
                    LogMobile.debug("NextStep.java", aMobileDevice.getName() + " was removed!");
                } else {
                    //caso termine logo apos um handoff. A st tera se conectado novamente ao ap mas ainda nao ocorreu a conexao st cloudlet
                    if (aMobileDevice.getSourceServerCloudlet() != null) {
                        aMobileDevice.getSourceServerCloudlet().setSmartThings(aMobileDevice, Policies.REMOVE); //it'll remove the smartThing from serverCloudlets-smartThing's set
                    }
                    aMobileDevice.getSourceAp().setSmartThings(aMobileDevice, Policies.REMOVE);//it'll remove the smartThing from ap-smartThing's set
                    LogMobile.debug("NextStep.java", aMobileDevice.getName() + " was removed!");
                    smartThings.remove(aMobileDevice);
                }
            } else {
                //System.out.println(aMobileDevice.getMyId() + "\t" + aMobileDevice.getCoord().getCoordX() + "\t" + aMobileDevice.getCoord().getCoordY() + "\t" + CloudSim.clock() + "\t" + Calendar.getInstance().getTime());
                saveMobility(aMobileDevice);
            }
            if (aMobileDevice.getDestinationAp() != null && aMobileDevice.getVmLocalServerCloudlet() != null) {
                AppExemplo2.temLog = AppExemplo2.temLog + aMobileDevice.getDestinationAp().getName() + " - " + aMobileDevice.getVmLocalServerCloudlet().getName() + " - ";
            }
        }
    }

}
