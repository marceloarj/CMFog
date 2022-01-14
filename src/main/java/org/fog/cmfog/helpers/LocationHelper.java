/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fog.cmfog.helpers;

import org.fog.entities.FogDevice;
import org.fog.entities.MobileDevice;
import org.fog.localization.Coordinate;
import org.fog.vmmobile.constants.MaxAndMin;

/**
 *
 * @author marce
 */
public class LocationHelper {

    public static boolean isCovered(FogDevice aFogDevice, MobileDevice aMobileDevice) {
        int radius = MaxAndMin.CLOUDLET_COVERAGE / 2;
        int entityX = aFogDevice.getCoord().getCoordX();
        int entityY = aFogDevice.getCoord().getCoordY();
        int coordX = aMobileDevice.getCoord().getCoordX();
        int coordY = aMobileDevice.getCoord().getCoordY();
        boolean xCovered = false, yCovered = false;
        // System.out.println("Fog DEV: " + entityX + " / " + entityY);
        // System.out.println("MD: " + coordX + " / " + coordY);
        // System.out.println("RADIUS " + radius);
        if (coordX >= entityX - radius && entityX + radius >= coordX) {

            xCovered = true;
        }
        if (coordY >= entityY - radius && entityY + radius >= coordY) {

            yCovered = true;
        }
        if (xCovered == true && yCovered == true) {
            // System.out.println("#### is covered");
            return true;
        } else {
            // System.out.println("#### is NOT covered");
            return false;
        }
    }

    public static boolean isInZone(FogDevice aFogDevice, MobileDevice aMobileDevice, int zone) {
        if (isBordeline(aFogDevice.getCoord(), aMobileDevice.getCoord(), zone)) {
            return false;
        }
        int radius = MaxAndMin.CLOUDLET_COVERAGE / 2;
        int entityX = aFogDevice.getCoord().getCoordX();
        int entityY = aFogDevice.getCoord().getCoordY();
        int coordX = aMobileDevice.getCoord().getCoordX();
        int coordY = aMobileDevice.getCoord().getCoordY();
        boolean xNucled = false, yNucled = false;
        //System.out.println(aFogDevice.getName() + ": " + entityX + " / " + entityY);
        // System.out.println("MD: " + coordX + " / " + coordY);
        //System.out.println("RADIUS " + radius);
        if (coordX >= entityX - radius + zone && entityX + radius - zone >= coordX) {
            //  System.out.println("x nucled");
            xNucled = true;
        }
        if (coordY >= entityY - radius + zone && entityY + radius - zone >= coordY) {
            //System.out.println("y nucled");
            yNucled = true;
        }
        if ((xNucled == false || yNucled == false) && isCovered(aFogDevice, aMobileDevice)) {
            return true;
        } else {
            return false;
        }
    }

    private static boolean isBordeline(Coordinate FDCoord, Coordinate MDCoord, int zone) {
        if (isCorner(MDCoord)) {
            return true;
        }
        if (isInGridBord(MDCoord)) {
            int radius = MaxAndMin.CLOUDLET_COVERAGE / 2;
            int entityX = FDCoord.getCoordX();
            int entityY = FDCoord.getCoordY();
            int coordX = MDCoord.getCoordX();
            int coordY = MDCoord.getCoordY();
            boolean xNucled = false, yNucled = false;
            if (coordX >= entityX - radius + zone && entityX + radius - zone >= coordX) {
                xNucled = true;
            }
            if (coordY >= entityY - radius + zone && entityY + radius - zone >= coordY) {
                yNucled = true;
            }
            if (xNucled == false && yNucled == false) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private static boolean isInGridBord(Coordinate MDCoord) {
        if (MDCoord.getCoordX() >= 0 && MDCoord.getCoordX() <= MaxAndMin.MIG_POINT) {
            return true;
        }
        if (MDCoord.getCoordX() >= (MaxAndMin.MAX_X - MaxAndMin.MIG_POINT) && MDCoord.getCoordX() <= MaxAndMin.MAX_X) {
            return true;
        }
        if (MDCoord.getCoordY() >= 0 && MDCoord.getCoordY() <= MaxAndMin.MIG_POINT) {
            return true;
        }
        if (MDCoord.getCoordY() >= (MaxAndMin.MAX_Y - MaxAndMin.MIG_POINT) && MDCoord.getCoordY() <= MaxAndMin.MAX_Y) {
            return true;
        }
        return false;
    }

    private static boolean isCorner(Coordinate MDCoord) {
        if (MDCoord.getCoordX() >= 0 && MDCoord.getCoordX() <= MaxAndMin.MIG_POINT) {
            if (MDCoord.getCoordY() >= 0 && MDCoord.getCoordY() <= MaxAndMin.MIG_POINT) {
                return true;
            }
            if (MDCoord.getCoordY() >= (MaxAndMin.MAX_Y - MaxAndMin.MIG_POINT) && MDCoord.getCoordY() <= MaxAndMin.MAX_Y) {
                return true;
            }
        }
        if (MDCoord.getCoordX() >= (MaxAndMin.MAX_X - MaxAndMin.MIG_POINT) && MDCoord.getCoordX() <= MaxAndMin.MAX_X) {
            if (MDCoord.getCoordY() >= 0 && MDCoord.getCoordY() <= MaxAndMin.MIG_POINT) {
                return true;
            }
            if (MDCoord.getCoordY() >= (MaxAndMin.MAX_Y - MaxAndMin.MIG_POINT) && MDCoord.getCoordY() <= MaxAndMin.MAX_Y) {
                return true;
            }
        }
        return false;
    }
}
