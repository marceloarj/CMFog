/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fog.cmfog.enumerate;

/**
 *
 * @author marce
 */
public enum Technology {
    TEC_4G(1), TEC_5G(2), TEC_WIFI(3);

    private int number;

    private Technology(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    static public Technology setValue(int number) {
        switch (number) {
            case 1:
                return TEC_4G;
            case 2:
                return TEC_5G;
            case 3:
                return TEC_WIFI;
            default:
                return null;
        }

    }
}
