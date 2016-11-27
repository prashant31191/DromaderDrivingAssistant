package com.skobbler.sdkdemo.fatigue;

import java.io.IOException;

/**
 * Created by marcinsendera on 24.11.2016.
 */

public class FatigueComputations {

    // instance of FuzzyLogicClass
    private FuzzyLogicClass fuzzyLogic;

    // my factors/arguments that have been received from upper FatigueAlgorithm instance
    //private String myLocalTime;
    private double myLocalTime;

    private String myWeather;

    // changing received data

    private double myExecutionTime;


    public FatigueComputations() {

        // handling exception while creating a new instance of a FuzzyLogicClass
        try {
            this.fuzzyLogic = new FuzzyLogicClass();
        } catch (FCLFileCannotBeOpenedException e) {

            e.printStackTrace();

        }

    }


    public void onCompute(double localTime, double executionTime, String weather){

        this.myLocalTime = localTime;

        this.myExecutionTime = executionTime;

        this.myWeather = weather;


    }


}