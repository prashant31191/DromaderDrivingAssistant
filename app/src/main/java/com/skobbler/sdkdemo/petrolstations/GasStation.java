package com.skobbler.sdkdemo.petrolstations;

import com.skobbler.ngx.SKCoordinate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by marcinsendera on 06.12.2016.
 */

public class GasStation {

    private double fuelcost;
    private double position;

    public SKCoordinate coordinate;
    public List<GVTuple> GV = new ArrayList<GVTuple>();

    public GasStation(double pos, double cost, SKCoordinate coord){
        this.fuelcost = cost;
        this.position = pos;
        this.coordinate = coord;
    }

    public void addGVTuple(double fuelLevel, int previousStationPosition){
        this.GV.add(new GVTuple(fuelLevel, previousStationPosition));
    }

    public void scalePosition(double scaleValue){
        this.position +=scaleValue;
    }

    public double getFuelCost(){
        return this.fuelcost;
    }

    public double getPosition(){
        return this.position;
    }

    public SKCoordinate getCoordinate() {return this.coordinate;}

}
