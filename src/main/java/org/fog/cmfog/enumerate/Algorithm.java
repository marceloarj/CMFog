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
public enum Algorithm {
    LL(0), CCMFOG(1), TCMFOG(2);

    private int number;

    private Algorithm(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }

    static public Algorithm setValue(int number) {
        switch (number) {
            case 1:
                return CCMFOG;
            case 2:
                return TCMFOG;
            default:
                return null;
        }

    }
}
