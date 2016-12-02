package com.skobbler.sdkdemo.petrolstations;

import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.search.SKSearchResultParent;

/**
 * Created by marcinsendera on 02.12.2016.
 */

public class PetrolStationStructure {

    // initializing my
    private int positionNumber;

    private SKCoordinate coordinates;


    // prices for fuels
    private double petrol = 10.0;
    private double diesel = 10.0;
    private double lpg = 10.0;

    private String countryCode;


    public PetrolStationStructure(int position, SKCoordinate coordinates){
        this.positionNumber = position;
        this.coordinates = coordinates;

        String countryName = "";
        SKSearchResult result = SKReverseGeocoderManager.getInstance().reverseGeocodePosition(coordinates);
        if (result != null && result.getParentsList() != null) {

            for (SKSearchResultParent parent : result.getParentsList()) {
                countryName = parent.getParentName();
            }

        }

        this.countryCode = countryName;

    }

    //return true if the petrol station is the same, else return false
    public boolean compareCoordinates(double longitude, double latitude){

        if(this.coordinates.getLongitude() == longitude && this.coordinates.getLatitude() == latitude){
            return true;
        }

        return false;
    }



    public void setPetrolCost(double price){
        this.petrol = price;
    }

    public void setDieselCost(double price){
        this.petrol = price;
    }

    public void setLpgCost(double price){
        this.petrol = price;
    }
}