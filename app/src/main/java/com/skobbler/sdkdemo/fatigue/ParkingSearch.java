package com.skobbler.sdkdemo.fatigue;

import android.util.Log;

import com.skobbler.ngx.SKCategories;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.positioner.SKPositionerManager;
import com.skobbler.ngx.sdktools.onebox.utils.SKToolsUtils;
import com.skobbler.ngx.search.SKNearbySearchSettings;
import com.skobbler.ngx.search.SKSearchListener;
import com.skobbler.ngx.search.SKSearchManager;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.search.SKSearchStatus;
import com.skobbler.ngx.util.SKGeoUtils;
import com.skobbler.ngx.util.SKLogging;
import com.skobbler.sdkdemo.navigationui.SKToolsLogicManager;

import java.util.List;

/**
 * Created by Krzysiek on 21.12.2016.
 */

public class ParkingSearch implements SKSearchListener {
    private static final int[] searchCategories = new int[] {
            SKCategories.SKPOICategory.SKPOI_CATEGORY_PARKING.getValue()
    };

    short radius = 32000;   // 32 km

    SKCoordinate parkingCoordinate = null;
    SKSearchManager searchManager;
    SKNearbySearchSettings searchObject;
    SKPosition currentPosition;
    SKCoordinate currentCoordinate;
    SKSearchStatus status;

    public void startSearch() {
        searchManager = new SKSearchManager(this);
        searchObject = new SKNearbySearchSettings();
        currentPosition = SKPositionerManager.getInstance().getCurrentGPSPosition(true);
        //SKCoordinate currentCoordinate1 = new SKCoordinate(currentPosition.getCoordinate().getLatitude(), currentPosition.getCoordinate().getLongitude());
        //Log.d("myTag", "current coord"+ currentPosition.toString());
        currentCoordinate = currentPosition.getCoordinate();
        searchObject.setLocation(currentCoordinate);
        searchObject.setRadius(radius);
        searchObject.setSearchResultsNumber(100);
        searchObject.setSearchCategories(searchCategories);
        searchObject.setSearchTerm(""); // all
        searchObject.setSearchMode(SKSearchManager.SKSearchMode.OFFLINE);
        status = searchManager.nearbySearch(searchObject);
        if (status != SKSearchStatus.SK_SEARCH_NO_ERROR) {
            SKLogging.writeLog("SKSearchStatus: ", status.toString(), 0);
        }
    }

    @Override
    public void onReceivedSearchResults(final List<SKSearchResult> searchResults) {
        int closest = 32000;
        int closestNr = 0;
        Log.d("myTag", "current coord"+ currentCoordinate.toString());
        if (searchResults.size() > 0) {
            for (int i = 0; i < searchResults.size(); i++) {
                Log.d("myTag", "current coord"+ currentCoordinate.toString());
                Log.d("myTag","result coord" + searchResults.get(i).getLocation().toString());
                int distance = (int) SKToolsUtils.distanceBetween(currentCoordinate, searchResults.get(i).getLocation());
                Log.d("myTag","result coord distance " + distance);
                if (distance < closest) {
                    closest = distance;
                    closestNr = i;
                }
            }
            parkingCoordinate = searchResults.get(closestNr).getLocation();
            Log.d("myTag","parking coord" + parkingCoordinate.toString());
            Log.d("myTag", "current coord"+ currentCoordinate.toString());
            SKToolsLogicManager skToolsLogicManager = SKToolsLogicManager.getInstance();
            skToolsLogicManager.setParkingCoordinates(parkingCoordinate);
            skToolsLogicManager.goViaParking();
        }
    }

}
