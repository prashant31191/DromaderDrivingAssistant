package com.skobbler.sdkdemo.navigationui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;
import com.skobbler.ngx.R;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.map.SKAnimationSettings;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.map.SKMapCustomPOI;
import com.skobbler.ngx.map.SKMapPOI;
import com.skobbler.ngx.map.SKMapScreenCaptureListener;
import com.skobbler.ngx.map.SKMapScreenCaptureManager;
import com.skobbler.ngx.map.SKMapSettings;
import com.skobbler.ngx.map.SKMapSurfaceListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapViewHolder;
import com.skobbler.ngx.map.SKMapViewStyle;
import com.skobbler.ngx.map.SKPOICluster;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.navigation.SKNavigationListener;
import com.skobbler.ngx.navigation.SKNavigationManager;
import com.skobbler.ngx.navigation.SKNavigationSettings;
import com.skobbler.ngx.navigation.SKNavigationState;
import com.skobbler.ngx.positioner.SKCurrentPositionListener;
import com.skobbler.ngx.positioner.SKCurrentPositionProvider;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.positioner.SKPositionerManager;
import com.skobbler.ngx.reversegeocode.SKReverseGeocoderManager;
import com.skobbler.ngx.routing.SKExtendedRoutePosition;
import com.skobbler.ngx.routing.SKRouteAdvice;
import com.skobbler.ngx.routing.SKRouteInfo;
import com.skobbler.ngx.routing.SKRouteJsonAnswer;
import com.skobbler.ngx.routing.SKRouteListener;
import com.skobbler.ngx.routing.SKRouteManager;
import com.skobbler.ngx.routing.SKRouteSettings;
import com.skobbler.ngx.routing.SKViaPoint;
import com.skobbler.ngx.util.SKGeoUtils;
import com.skobbler.sdkdemo.activity.DialogMessage;
import com.skobbler.sdkdemo.costs.CostCalculator;
import com.skobbler.sdkdemo.fatigue.FatigueAlgorithm;
import com.skobbler.sdkdemo.fatigue.HotelSearch;
import com.skobbler.sdkdemo.fatigue.ParkingSearch;
import com.skobbler.sdkdemo.navigationui.autonight.SKToolsAutoNightManager;
import com.skobbler.ngx.search.SKSearchResult;
import com.skobbler.ngx.util.SKLogging;
import com.skobbler.sdkdemo.petrolstations.FillStationStructure;
import com.skobbler.sdkdemo.util.PreferenceTypes;
import com.skobbler.sdkdemo.util.Utils;
import com.skobbler.sdkdemo.weather.WeatherPrediction;

import static com.skobbler.sdkdemo.activity.MapActivity.VIA_POINT_ICON_ID;

/**
 * This class handles the logic related to the navigation and route calculation.
 */
public class SKToolsLogicManager implements SKMapSurfaceListener, SKNavigationListener, SKRouteListener,
        SKCurrentPositionListener, SKMapScreenCaptureListener {

    private boolean rerouting = false;
    private List<FillStationStructure> fillStations0 = null;
    private List<FillStationStructure> fillStations1 = null;
    private List<FillStationStructure> fillStations2 = null;
    private List<FillStationStructure> fillStations = null;
    private List<Boolean> fillStationsUsed = null;

    private double endVolume;
    private double endVolume0;
    private double endVolume1;
    private double endVolume2;

    private int fillStationNumber0;
    private int fillStationNumber1;
    private int fillStationNumber2;
    private int fillStationNumber;
    private int currentStationNr;

    private int fillStationResponse = 0;
    private static final int ANNOTATION_ID_DEFAULT = 20;
    private static int annotationIDroute1 = ANNOTATION_ID_DEFAULT;
    private static int annotationIDroute2 = ANNOTATION_ID_DEFAULT;
    private static int annotationIDroute3 = ANNOTATION_ID_DEFAULT;

    /**
     * Singleton instance for current class
     */
    private static volatile SKToolsLogicManager instance = null;

    private SKMapSurfaceView mapView;

    /**
     * the view that holds the map view
     */
    private SKMapViewHolder mapHolder;

    private Activity currentActivity;

    private SKCurrentPositionProvider currentPositionProvider;

    private SKNavigationManager naviManager;

    /**
     * the initial configuration for calculating route and navigating
     */
    private SKToolsNavigationConfiguration configuration;

    /**
     * number of options pressed (in navigation settings).
     */
    public int numberOfSettingsOptionsPressed;

    /**
     * last audio advices that needs to be played when the visual advice is
     * pressed
     */
    private String[] lastAudioAdvices;

    /**
     * the distance left to the destination after every route update
     */
    private long navigationCurrentDistance;

    /**
     * boolean value which shows if there are blocks on the route or not
     */
    private boolean roadBlocked;

    /**
     * flag for when a re-routing was done, which is set to true only until the
     * next update of the navigation state
     */
    private boolean reRoutingInProgress = false;

    public static volatile SKPosition lastUserPosition;

    private boolean navigationStopped;

    private SKMapSurfaceListener previousMapSurfaceListener;

    private List<SKRouteInfo> skRouteInfoList = new ArrayList<SKRouteInfo>();

    private SKToolsNavigationListener navigationListener;

    private SKMapViewStyle currentMapStyle;

    private SKMapSettings.SKMapDisplayMode currentUserDisplayMode;

    private FatigueAlgorithm fatigueAlgorithm;

    private SKCoordinate hotelCoordinates;
    private SKCoordinate parkingCoordinates;

    private volatile boolean[] routeCalculationsEnded = new boolean[3];

    public boolean startPedestrian = false;

    /**
     * Creates a single instance of {@link SKToolsNavigationUIManager}
     *
     * @return
     */
    public static SKToolsLogicManager getInstance() {
        if (instance == null) {
            synchronized (SKToolsLogicManager.class) {
                if (instance == null) {
                    instance = new SKToolsLogicManager();
                }
            }
        }
        return instance;
    }

    private SKToolsLogicManager() {
        naviManager = SKNavigationManager.getInstance();
    }

    /**
     * Sets the current activity.
     *
     * @param activity
     * @param rootId
     */
    protected void setActivity(Activity activity, int rootId) {
        this.currentActivity = activity;
        currentPositionProvider = new SKCurrentPositionProvider(currentActivity);
        if (SKToolsUtils.hasGpsModule(currentActivity)) {
            currentPositionProvider.requestLocationUpdates(true, false, true);
        } else if (SKToolsUtils.hasNetworkModule(currentActivity)) {
            currentPositionProvider.requestLocationUpdates(false, true, true);
        }
        currentPositionProvider.setCurrentPositionListener(this);
        SKToolsNavigationUIManager.getInstance().setActivity(currentActivity, rootId);
    }

    /**
     * Sets the listener.
     *
     * @param navigationListener
     */
    public void setNavigationListener(SKToolsNavigationListener navigationListener) {
        this.navigationListener = navigationListener;
    }

    /**
     * Starts a route calculation.
     *
     * @param configuration
     * @param mapHolder
     */
    public void calculateRoute(SKToolsNavigationConfiguration configuration, SKMapViewHolder mapHolder) {
        this.mapHolder = mapHolder;
        this.mapView = mapHolder.getMapSurfaceView();
        this.configuration = configuration;
        SKToolsMapOperationsManager.getInstance().setMapView(mapView);
        currentPositionProvider.requestUpdateFromLastPosition();
        currentMapStyle = mapView.getMapSettings().getMapStyle();
        SKRouteSettings route = new SKRouteSettings();
        route.setStartCoordinate(configuration.getStartCoordinate());
        route.setDestinationCoordinate(configuration.getDestinationCoordinate());
        SKToolsMapOperationsManager.getInstance().drawDestinationNavigationFlag(configuration
                        .getDestinationCoordinate().getLongitude(), configuration.getDestinationCoordinate().getLatitude());
        List<SKViaPoint> viaPointList;
        viaPointList = configuration.getViaPointCoordinateList();
        if (viaPointList != null) {
            route.setViaPoints(viaPointList);
        }

        SKToolsNavigationUIManager.getInstance().setRouteType(configuration.getRouteType());
        if (configuration.getRouteType() == SKRouteSettings.SKRouteMode.CAR_SHORTEST) {
            route.setMaximumReturnedRoutes(1);
        } else {
            route.setMaximumReturnedRoutes(3);
        }

        route.setRouteMode(configuration.getRouteType());
        route.setRouteExposed(true);
        route.getRouteRestrictions().setTollRoadsAvoided(configuration.isTollRoadsAvoided());
        route.getRouteRestrictions().setFerriesAvoided(configuration.isFerriesAvoided());
        route.getRouteRestrictions().setHighWaysAvoided(configuration.isHighWaysAvoided());
        route.setRequestAdvices(true);
        route.setRequestCountryCodes(true);
        route.setRequestExtendedPoints(true);
        SKRouteManager.getInstance().setRouteListener(this);

        SKRouteManager.getInstance().calculateRoute(route);
        SKToolsNavigationUIManager.getInstance().showPreNavigationScreen();

        if (configuration.isAutomaticDayNight() && lastUserPosition != null) {
            SKToolsAutoNightManager.getInstance().setAutoNightAlarmAccordingToUserPosition
                                                (lastUserPosition.getCoordinate(), currentActivity);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                SKToolsAutoNightManager.getInstance().setAlarmForHourlyNotificationAfterKitKat(currentActivity, true);
            } else {
                SKToolsAutoNightManager.getInstance().setAlarmForHourlyNotification(currentActivity);
            }

            checkCorrectMapStyle();
        }
        navigationStopped = false;

        if (navigationListener != null) {
            navigationListener.onRouteCalculationStarted();
        }
    }

    /**
     * Starts a navigation with the specified configuration.
     *
     * @param configuration
     * @param mapHolder
     * @param isFreeDrive
     */
    public void startNavigation(SKToolsNavigationConfiguration configuration, SKMapViewHolder mapHolder, boolean isFreeDrive) {

        SKNavigationSettings navigationSettings = new SKNavigationSettings();
        reRoutingInProgress = false;
        this.configuration = configuration;
        this.mapHolder = mapHolder;
        mapView = mapHolder.getMapSurfaceView();
        SKToolsNavigationUIManager.getInstance().setRouteType(configuration.getRouteType());
        currentUserDisplayMode = SKMapSettings.SKMapDisplayMode.MODE_3D;
        mapView.getMapSettings().setFollowPositions(false);
        mapView.getMapSettings().setHeadingMode(SKMapSettings.SKHeadingMode.ROUTE);
        mapView.getMapSettings().setMapDisplayMode(currentUserDisplayMode);
        mapView.getMapSettings().setStreetNamePopupsShown(true);
        mapView.getMapSettings().setMapZoomingEnabled(false);
        previousMapSurfaceListener = mapView.getMapSurfaceListener();
        mapHolder.setMapSurfaceListener(this);
        SKToolsMapOperationsManager.getInstance().setMapView(mapView);

        navigationSettings.setNavigationType(configuration.getNavigationType());
        navigationSettings.setPositionerVerticalAlignment(-0.25f);
        navigationSettings.setShowRealGPSPositions(false);
        navigationSettings.setDistanceUnit(configuration.getDistanceUnitType());
        navigationSettings.setSpeedWarningThresholdInCity(configuration.getSpeedWarningThresholdInCity());
        navigationSettings.setSpeedWarningThresholdOutsideCity(configuration.getSpeedWarningThresholdOutsideCity());
        if (configuration.getNavigationType().equals(SKNavigationSettings.SKNavigationType.FILE)) {
            navigationSettings.setFileNavigationPath(configuration.getFreeDriveNavigationFilePath());
        }

        naviManager.setNavigationListener(this);
        naviManager.setMapView(mapView);
        naviManager.startNavigation(navigationSettings);

        SKToolsNavigationUIManager.getInstance().inflateNavigationViews(currentActivity);
        SKToolsNavigationUIManager.getInstance().reset(configuration.getDistanceUnitType());
        SKToolsNavigationUIManager.getInstance().setFollowerMode();
        if (configuration.getNavigationType() == SKNavigationSettings.SKNavigationType.SIMULATION) {
            SKToolsNavigationUIManager.getInstance().inflateSimulationViews();
        }
        if (isFreeDrive) {
            SKToolsNavigationUIManager.getInstance().setFreeDriveMode();
            currentMapStyle = mapView.getMapSettings().getMapStyle();
        }
        currentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.d("", "lastUserPosition = " + lastUserPosition);
        if (configuration.isAutomaticDayNight() && lastUserPosition != null) {
            if (isFreeDrive) {
                SKToolsAutoNightManager.getInstance().setAutoNightAlarmAccordingToUserPosition
                                                    (lastUserPosition.getCoordinate(), currentActivity);

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    SKToolsAutoNightManager.getInstance().setAlarmForHourlyNotificationAfterKitKat(currentActivity, true);
                } else {
                    SKToolsAutoNightManager.getInstance().setAlarmForHourlyNotification(currentActivity);
                }

                checkCorrectMapStyle();
            }
        }
        SKToolsNavigationUIManager.getInstance().switchDayNightStyle(SKToolsMapOperationsManager
                                                                .getInstance().getCurrentMapStyle());
        navigationStopped = false;

        if (navigationListener != null) {
            navigationListener.onNavigationStarted();
        }

        // sprawdzenie ustawień aplikacji: czy włączać algorytm zmęczenia i czy włączać powiadomienia o stacjach benzynowych
        // (jeśli nie to: fillStationsNumber = -2)
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences
                                                            (getCurrentActivity().getApplicationContext());
        boolean fatiguePreferences = sharedPreferences.getBoolean(PreferenceTypes.K_FATIGUE_ALERTS, true);
        boolean fuelStationPreferences = sharedPreferences.getBoolean(PreferenceTypes.K_PETROL_STATIONS_ALERTS, true);

        fatigueAlgorithm = new FatigueAlgorithm(currentActivity.getApplicationContext());
        if(fatiguePreferences){
            fatigueAlgorithm.startMeasurement();
        }

        if(!fuelStationPreferences){
            fillStationNumber = -2;
        }

        for (int i = 0; i < fillStations.size(); i++) {
            fillStationsUsed.add(false);
        }
    }

    /**
     * Stops the navigation.
     */
    protected void stopNavigation() {
        SKToolsMapOperationsManager.getInstance().startPanningMode();
        mapView.getMapSettings().setMapStyle(currentMapStyle);
        mapView.getMapSettings().setCompassShown(false);
        SKRouteManager.getInstance().clearCurrentRoute();
        naviManager.stopNavigation();
        currentPositionProvider.stopLocationUpdates();
        mapHolder.setMapSurfaceListener(previousMapSurfaceListener);
        navigationStopped = true;
        startPedestrian = false;
        if (configuration.getDestinationCoordinate() != null) {
            SKToolsMapOperationsManager.getInstance().drawDestinationPoint(configuration
                    .getDestinationCoordinate().getLongitude(), configuration.getDestinationCoordinate().getLatitude());
        }
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SKToolsNavigationUIManager.getInstance().removeNavigationViews();
                currentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        if (navigationListener != null) {
            navigationListener.onNavigationEnded();
        }

        SKToolsAdvicePlayer.getInstance().stop();
        SKMapScreenCaptureManager.getInstance().disableMapScreenCapture();
    }

    /**
     * Checks the correct map style, taking into consideration auto night configuration settings.
     */
    private void checkCorrectMapStyle() {
        int currentMapStyle = SKToolsMapOperationsManager.getInstance().getCurrentMapStyle();
        int correctMapStyle = SKToolsMapOperationsManager.getInstance().getMapStyleBeforeStartDriveMode
                                                                    (configuration.isAutomaticDayNight());
        if (currentMapStyle != correctMapStyle) {
            SKToolsMapOperationsManager.getInstance().switchDayNightStyle(configuration, correctMapStyle);
            SKToolsNavigationUIManager.getInstance().changePanelsBackgroundAndTextViewsColour
                                        (SKToolsMapOperationsManager.getInstance().getCurrentMapStyle());
        }
    }

    /**
     * Checks if the navigation is stopped.
     *
     * @return
     */
    public boolean isNavigationStopped() {
        return navigationStopped;
    }

    /**
     * Gets the current activity.
     *
     * @return
     */
    public Activity getCurrentActivity() {
        return currentActivity;
    }

    /**
     * Handles orientation changed.
     */
    public void notifyOrientationChanged() {
        int mapStyle = SKToolsMapOperationsManager.getInstance().getCurrentMapStyle();
        SKMapSettings.SKMapDisplayMode displayMode = mapView.getMapSettings().getMapDisplayMode();
        SKToolsNavigationUIManager.getInstance().handleOrientationChanged(mapStyle, displayMode);
    }

    /**
     * Handles the block roads list items click.
     *
     * @param parent
     * @param position
     */
    protected void handleBlockRoadsItemsClick(AdapterView<?> parent, int position) {

        SKToolsNavigationUIManager.getInstance().setFollowerMode();
        SKToolsNavigationUIManager.getInstance().showFollowerModePanels
                (configuration.getNavigationType() == SKNavigationSettings.SKNavigationType.SIMULATION);

        String item = (String) parent.getItemAtPosition(position);
        if (item.equals(currentActivity.getResources().getString(R.string.unblock_all))) {
            naviManager.unblockAllRoads();
            roadBlocked = false;
        } else {
            // blockedDistance[0] - value, blockedDistance[1] - unit
            String[] blockedDistance = item.split(" ");
            int distance;
            try {
                distance = Integer.parseInt(blockedDistance[0]);
            } catch (NumberFormatException e) {
                distance = -1;
            }
            // set unit type based on blockDistance[1]
            int type = -1;
            if ("ft".equals(blockedDistance[1])) {
                type = 0;
            } else if ("yd".equals(blockedDistance[1])) {
                type = 1;
            } else if ("mi".equals(blockedDistance[1])) {
                type = 2;
            } else if ("km".equals(blockedDistance[1])) {
                type = 3;
            }

            naviManager.blockRoad(SKToolsUtils
                    .distanceInMeters(distance, type));
            roadBlocked = true;
        }
    }

    /**
     * Handles the items click.
     *
     * @param v
     */
    protected void handleItemsClick(View v) {
        int id = v.getId();

        this.fillStations = fillStations0;
        this.fillStationNumber = fillStationNumber0;
        this.endVolume = endVolume0;
        
        if (id == R.id.first_route || id == R.id.second_route || id == R.id.third_route) {

            int routeIndex = 0;

            if (id == R.id.first_route) {
                routeIndex = 0;
                this.fillStations = fillStations0;
                this.fillStationNumber = fillStationNumber0;
                this.endVolume = endVolume0;
                Log.d("endvolumechange",""+endVolume0);
            } else if (id == R.id.second_route) {
                routeIndex = 1;
                this.fillStations = fillStations1;
                this.fillStationNumber = fillStationNumber1;
                this.endVolume = endVolume1;
                Log.d("endvolumechange",""+endVolume1);
            } else if (id == R.id.third_route) {
                routeIndex = 2;
                this.fillStations = fillStations2;
                this.fillStationNumber = fillStationNumber2;
                this.endVolume = endVolume2;
                Log.d("endvolumechange",""+endVolume2);
            }

            SKToolsMapOperationsManager.getInstance().zoomToRoute(currentActivity);
            if (skRouteInfoList.size() > routeIndex) {
                int routeId = skRouteInfoList.get(routeIndex).getRouteID();
                SKRouteManager.getInstance().setCurrentRouteByUniqueId(routeId);
                SKToolsNavigationUIManager.getInstance().selectAlternativeRoute(routeIndex);
            }
        } else if (id == R.id.start_navigation_button) {
            SKToolsNavigationUIManager.getInstance().removePreNavigationViews();
            SKRouteManager.getInstance().clearRouteAlternatives();
            skRouteInfoList.clear();
            startNavigation(configuration, mapHolder, false);
        } else if (id == R.id.navigation_top_back_button) {
            SKToolsMapOperationsManager.getInstance().setMapInNavigationMode();
            SKToolsNavigationUIManager.getInstance().setFollowerMode();
            SKToolsNavigationUIManager.getInstance().showFollowerModePanels
                    (configuration.getNavigationType() == SKNavigationSettings.SKNavigationType.SIMULATION);
            mapView.getMapSettings().setCompassShown(false);
            mapView.getMapSettings().setMapZoomingEnabled(false);
            if (currentUserDisplayMode != null) {
                SKToolsMapOperationsManager.getInstance().switchMapDisplayMode(currentUserDisplayMode);
            }
        } else if (id == R.id.cancel_pre_navigation_button) {
            removeRouteCalculationScreen();
        } else if (id == R.id.menu_back_prenavigation_button) {
            SKToolsNavigationUIManager.getInstance().handleNavigationBackButton();
        } else if (id == R.id.navigation_increase_speed) {
            SKNavigationManager.getInstance().increaseSimulationSpeed(3);
        } else if (id == R.id.navigation_decrease_speed) {
            SKNavigationManager.getInstance().decreaseSimulationSpeed(3);
        } else if (id == R.id.menu_back_follower_mode_button) {
            SKToolsNavigationUIManager.getInstance().handleNavigationBackButton();
        } else if (id == R.id.navigation_bottom_right_estimated_panel || id == R.id.navigation_bottom_right_arriving_panel) {
            SKToolsNavigationUIManager.getInstance().switchEstimatedTime();
        } else if (id == R.id.pedestrian_compass_panel_layout) {
            SKToolsNavigationUIManager.getInstance().setTheCorrespondingImageForCompassPanel(mapView.getMapSettings());
        } else if (id == R.id.position_me_real_navigation_button) {
            if (lastUserPosition != null) {
                mapView.centerOnCurrentPosition(15,true,1000);
            } else {
                Toast.makeText(currentActivity, currentActivity.getResources()
                        .getString(R.string.no_position_available), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.current_advice_image_holder || id == R.id.current_advice_text_holder) {
            playLastAdvice();
        }
    }

    /**
     * Removes the pre navigation screen.
     */
    protected void removeRouteCalculationScreen() {
        SKToolsNavigationUIManager.getInstance().removePreNavigationViews();
        SKRouteManager.getInstance().clearCurrentRoute();
        SKRouteManager.getInstance().clearRouteAlternatives();
        skRouteInfoList.clear();
        mapView.getMapSettings().setMapStyle(currentMapStyle);
        SKToolsAutoNightManager.getInstance().cancelAlarmForForHourlyNotification();
        SKToolsMapOperationsManager.getInstance().drawDestinationPoint(configuration
                .getDestinationCoordinate().getLongitude(), configuration.getDestinationCoordinate().getLatitude());
        if (navigationListener != null) {
            navigationListener.onRouteCalculationCanceled();
        }
    }

    /**
     * handles the click on different views
     *
     * @param v the current view on which the click is detected
     */
    protected void handleSettingsItemsClick(View v) {
        boolean naviScreenSet = false;

        int id = v.getId();
        if (id == R.id.navigation_settings_audio_button) {
            numberOfSettingsOptionsPressed++;
            if (numberOfSettingsOptionsPressed == 1) {
                SKToolsNavigationUIManager.getInstance().loadAudioSettings();
            }
        } else if (id == R.id.navigation_settings_day_night_mode_button) {
            numberOfSettingsOptionsPressed++;
            if (numberOfSettingsOptionsPressed == 1) {
                loadDayNightSettings(configuration);
            }
        } else if (id == R.id.navigation_settings_overview_button) {
            final SKSearchResult destination = SKReverseGeocoderManager.getInstance()
                        .reverseGeocodePosition(configuration.getDestinationCoordinate());
            if (destination != null) {
                SKToolsMapOperationsManager.getInstance().switchToOverViewMode(currentActivity, configuration);
                SKToolsNavigationUIManager.getInstance()
                        .showOverviewMode(SKToolsUtils.getFormattedAddress(destination.getParentsList()));
                naviScreenSet = true;
            }
        } else if (id == R.id.navigation_settings_route_info_button) {
            numberOfSettingsOptionsPressed++;
            if (numberOfSettingsOptionsPressed == 1) {
                final SKSearchResult startCoord = SKReverseGeocoderManager.getInstance()
                                .reverseGeocodePosition(configuration.getStartCoordinate());
                final SKSearchResult destCoord = SKReverseGeocoderManager.getInstance()
                            .reverseGeocodePosition(configuration.getDestinationCoordinate());
                String startAdd = SKToolsUtils.getFormattedAddress(startCoord.getParentsList());
                String destAdd = SKToolsUtils.getFormattedAddress(destCoord.getParentsList());
                SKToolsNavigationUIManager.getInstance().showRouteInfoScreen(startAdd, destAdd);
                naviScreenSet = true;
            }
        } else if (id == R.id.navigation_settings_roadblock_info_button) {
            naviScreenSet = true;

            if (!SKToolsNavigationUIManager.getInstance().isFreeDriveMode()) {
                SKToolsNavigationUIManager.getInstance()
                        .showRoadBlockMode(configuration.getDistanceUnitType(), navigationCurrentDistance);
            } else {
                SKToolsNavigationUIManager.getInstance().showRouteInfoFreeDriveScreen();
            }

        } else if (id == R.id.navigation_settings_panning_button) {
            SKToolsMapOperationsManager.getInstance().startPanningMode();
            SKToolsNavigationUIManager.getInstance()
                    .showPanningMode(configuration.getNavigationType() == SKNavigationSettings.SKNavigationType.REAL);
            naviScreenSet = true;
        } else if (id == R.id.navigation_settings_view_mode_button) {
            loadMapDisplayMode();
        } else if (id == R.id.navigation_settings_quit_button) {
            SKToolsNavigationUIManager.getInstance().showExitNavigationDialog();
        } else if (id == R.id.navigation_settings_back_button) {
            if (currentUserDisplayMode != null) {
                SKToolsMapOperationsManager.getInstance().switchMapDisplayMode(currentUserDisplayMode);
            }
        }

        SKToolsNavigationUIManager.getInstance().hideSettingsPanel();
        numberOfSettingsOptionsPressed = 0;

        if (!naviScreenSet) {
            SKToolsNavigationUIManager.getInstance().setFollowerMode();
            SKToolsNavigationUIManager.getInstance().showFollowerModePanels
                    (configuration.getNavigationType() == SKNavigationSettings.SKNavigationType.SIMULATION);
        }
    }

    private void switchToPanningMode() {
        if (!SKToolsNavigationUIManager.getInstance().isPanningMode()) {
            SKToolsMapOperationsManager.getInstance().startPanningMode();
            SKToolsNavigationUIManager.getInstance()
                    .showPanningMode(configuration.getNavigationType() == SKNavigationSettings.SKNavigationType.REAL);
        }
    }

    /**
     * play the last advice
     */
    protected void playLastAdvice() {
        SKToolsAdvicePlayer.getInstance().playAdvice(lastAudioAdvices, SKToolsAdvicePlayer.PRIORITY_USER);
    }

    /**
     * Checks if the roads are blocked.
     *
     * @return
     */
    protected boolean isRoadBlocked() {
        return roadBlocked;
    }

    /**
     * Changes the map style from day -> night or night-> day
     */
    private void loadDayNightSettings(SKToolsNavigationConfiguration configuration) {
        int mapStyle = SKToolsMapOperationsManager.getInstance().getCurrentMapStyle();
        int newStyle;
        if (mapStyle == SKToolsMapOperationsManager.DAY_STYLE) {
            newStyle = SKToolsMapOperationsManager.NIGHT_STYLE;
        } else {
            newStyle = SKToolsMapOperationsManager.DAY_STYLE;
        }

        SKToolsNavigationUIManager.getInstance().switchDayNightStyle(newStyle);
        SKToolsMapOperationsManager.getInstance().switchDayNightStyle(configuration, newStyle);
    }

    /**
     * Decides the style in which the map needs to be changed next.
     */
    public void computeMapStyle(boolean isDaytime) {
        Log.d("", "Update the map style after receiving the broadcast");
        int mapStyle;
        if (isDaytime) {
            mapStyle = SKToolsMapOperationsManager.DAY_STYLE;
        } else {
            mapStyle = SKToolsMapOperationsManager.NIGHT_STYLE;
        }
        SKToolsNavigationUIManager.getInstance().switchDayNightStyle(mapStyle);
        SKToolsMapOperationsManager.getInstance().switchDayNightStyle(configuration, mapStyle);
    }

    /**
     * Changes the map display from 3d-> 2d and vice versa
     */
    private void loadMapDisplayMode() {
        SKMapSettings.SKMapDisplayMode displayMode = mapView.getMapSettings().getMapDisplayMode();
        SKMapSettings.SKMapDisplayMode newDisplayMode;
        if (displayMode == SKMapSettings.SKMapDisplayMode.MODE_3D) {
            newDisplayMode = SKMapSettings.SKMapDisplayMode.MODE_2D;
        } else {
            newDisplayMode = SKMapSettings.SKMapDisplayMode.MODE_3D;
        }

        currentUserDisplayMode = newDisplayMode;
        SKToolsNavigationUIManager.getInstance().switchMapMode(newDisplayMode);
        SKToolsMapOperationsManager.getInstance().switchMapDisplayMode(newDisplayMode);
    }

    @Override
    public void onActionPan() {
        switchToPanningMode();
    }

    @Override
    public void onActionZoom() {
        float currentZoom = mapView.getZoomLevel();
        if (currentZoom < 5) {
            // do not show the blue dot
            mapView.getMapSettings().setCurrentPositionShown(false);
        } else {
            mapView.getMapSettings().setCurrentPositionShown(true);
        }
        switchToPanningMode();
    }

    @Override
    public void onSurfaceCreated(SKMapViewHolder holder) {
    }

    @Override
    public void onMapRegionChanged(SKCoordinateRegion skCoordinateRegion) {
    }

    @Override
    public void onMapRegionChangeStarted(SKCoordinateRegion skCoordinateRegion) {
    }

    @Override
    public void onMapRegionChangeEnded(SKCoordinateRegion skCoordinateRegion) {
    }

    @Override
    public void onDoubleTap(SKScreenPoint skScreenPoint) {
    }

    @Override
    public void onSingleTap(SKScreenPoint skScreenPoint) {
        if (SKToolsNavigationUIManager.getInstance().isFollowerMode()) {
            SKToolsNavigationUIManager.getInstance().showSettingsMode();
        }
    }

    @Override
    public void onRotateMap() {
        switchToPanningMode();
    }

    @Override
    public void onLongPress(SKScreenPoint skScreenPoint) {
        SKAnnotation annotation = new SKAnnotation(19);
        annotation.setUniqueID(19);
        annotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_PURPLE);
        SKViaPoint viaPoint = new SKViaPoint(19, mapView.pointToCoordinate(skScreenPoint));
        SKRouteManager.getInstance().addViaPoint(viaPoint, -1);
    }

    @Override
    public void onInternetConnectionNeeded() {
    }

    @Override
    public void onMapActionDown(SKScreenPoint skScreenPoint) {
    }

    @Override
    public void onMapActionUp(SKScreenPoint skScreenPoint) {
    }

    @Override
    public void onPOIClusterSelected(SKPOICluster skpoiCluster) {
    }

    @Override
    public void onMapPOISelected(SKMapPOI skMapPOI) {
    }

    @Override
    public void onAnnotationSelected(SKAnnotation skAnnotation) {
    }

    @Override
    public void onCustomPOISelected(SKMapCustomPOI skMapCustomPOI) {
    }

    @Override
    public void onCompassSelected() {
    }

    @Override
    public void onCurrentPositionSelected() {
    }

    @Override
    public void onObjectSelected(int i) {
    }

    @Override
    public void onInternationalisationCalled(int i) {
    }

    @Override
    public void onBoundingBoxImageRendered(int i) {
    }

    @Override
    public void onGLInitializationError(String messsage) {
    }

    @Override
    public void onScreenshotReady(Bitmap bitmap) {
    }

    @Override
    public void onDestinationReached() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getCurrentActivity().getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferenceTypes.K_FUEL_LEVEL, String.valueOf(endVolume+5.0));
        editor.commit();

        if (configuration.getNavigationType() == SKNavigationSettings.SKNavigationType.REAL
                                    && configuration.isContinueFreeDriveAfterNavigationEnd()) {
            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SKRouteManager.getInstance().clearCurrentRoute();
                    SKToolsMapOperationsManager.getInstance().deleteDestinationPoint();
                    SKToolsNavigationUIManager.getInstance().setFreeDriveMode();
                }
            });
        } else {
            stopNavigation();
        }
    }

    @Override
    public void onSignalNewAdviceWithInstruction(String instruction) {
    }

    @Override
    public void onSignalNewAdviceWithAudioFiles(String[] audioFiles, boolean specialSoundFile) {
        SKToolsAdvicePlayer.getInstance().playAdvice(audioFiles, SKToolsAdvicePlayer.PRIORITY_NAVIGATION);
    }

    @Override
    public void onSpeedExceededWithAudioFiles(String[] adviceList, boolean speedExceeded) {
        playSoundWhenSpeedIsExceeded(adviceList, speedExceeded);
    }

    /**
     * play sound when the speed is exceeded
     *
     * @param adviceList    - the advices that needs to be played
     * @param speedExceeded - true if speed is exceeded, false otherwise
     */
    private void playSoundWhenSpeedIsExceeded(final String[] adviceList, final boolean speedExceeded) {
        if (!navigationStopped) {
            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (speedExceeded) {
                        SKToolsAdvicePlayer.getInstance().playAdvice(adviceList, SKToolsAdvicePlayer.PRIORITY_SPEED_WARNING);
                    }
                    SKToolsNavigationUIManager.getInstance().handleSpeedExceeded(speedExceeded);
                }
            });
        }
    }

    @Override
    public void onSpeedExceededWithInstruction(String instruction, boolean speedExceeded) {
    }

    public void setFillStations(List<FillStationStructure> list, int number){
        switch (number) {
            case 0: this.fillStations0 = list;
                    if(!list.isEmpty()) {
                        this.fillStationNumber0 = 0;
                    } else {
                        this.fillStationNumber0 = -2;
                    }
                break;
            case 1: this.fillStations1 = list;
                    if(!list.isEmpty()) {
                        this.fillStationNumber1 = 0;
                    } else {
                        this.fillStationNumber1 = -2;
                    }
                break;
            case 2: this.fillStations2 = list;
                    if(!list.isEmpty()) {
                        this.fillStationNumber2 = 0;
                    } else {
                        this.fillStationNumber2 = -2;
                    }
                break;
        }
    }

    public void setEndVolume(double endVolume, int number){
        switch (number) {
            case 0: this.endVolume0 = endVolume;
                break;
            case 1: this.endVolume1 = endVolume;
                break;
            case 2: this.endVolume2 = endVolume;
                break;
        }
    }

    public void setHotelCoordinates(SKCoordinate coordinates){
        this.hotelCoordinates = coordinates;
    }

    public void setParkingCoordinates(SKCoordinate coordinates){
        this.parkingCoordinates = coordinates;
    }

    private void fatigueMessage(){
        DialogMessage dm = new DialogMessage(currentActivity);
        dm.setMessage("You're probably tired. Where do you want to go?",
                com.skobbler.sdkdemo.R.string.go_to_hotel,
                new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        searchHotel();
                        fatigueAlgorithm.takeBreak();
                    }
                },
                com.skobbler.sdkdemo.R.string.go_on_parking,
                new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        searchParking();
                        fatigueAlgorithm.takeBreak();
                    }
                },
                com.skobbler.sdkdemo.R.string.dismiss,
                new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        fatigueAlgorithm.dismiss();
                    }
                });
        dm.showWithTimeout(15000);
    }

    private void fillStationMessage(FillStationStructure fillStationStructure){
        DialogMessage dm = new DialogMessage(currentActivity);
        String s = String.format(Locale.US, "%.2f", fillStationStructure.getAppCost());
        dm.setMessage("Fill your car with: " + fillStationStructure.getFuelToFill() + " l (about: " + s + " EUR)",
                com.skobbler.sdkdemo.R.string.go_on_station,
                new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        fillStationResponse = 1;
                    }
                },
                com.skobbler.sdkdemo.R.string.dismiss,
                new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        fillStationResponse = 2;
                    }
                });
        dm.showWithTimeout(15000);
    }

    private void searchParking() {
        parkingCoordinates = null;
        ParkingSearch parkingSearch = new ParkingSearch();
        parkingSearch.startSearch();
    }

    private void searchHotel() {
        hotelCoordinates = null;
        HotelSearch hotelSearch = new HotelSearch();
        hotelSearch.startSearch();
    }

    public void goViaHotel() {
        SKAnnotation hotelAnnotation = new SKAnnotation(30);
        hotelAnnotation.setLocation(hotelCoordinates);
        hotelAnnotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_PURPLE);
        mapView.addAnnotation(hotelAnnotation, SKAnimationSettings.ANIMATION_NONE);
        SKViaPoint viaPoint = new SKViaPoint(VIA_POINT_ICON_ID, hotelCoordinates);
        SKRouteManager.getInstance().addViaPoint(viaPoint, -1);
        rerouting = true;
    }

    public void goViaParking() {
        SKAnnotation parkingAnnotation = new SKAnnotation(31);
        parkingAnnotation.setLocation(parkingCoordinates);
        parkingAnnotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_PURPLE);
        mapView.addAnnotation(parkingAnnotation, SKAnimationSettings.ANIMATION_NONE);
        SKViaPoint viaPoint = new SKViaPoint(VIA_POINT_ICON_ID, parkingCoordinates);
        SKRouteManager.getInstance().addViaPoint(viaPoint, -1);
        rerouting = true;
    }

    public void goViaFuelStation(FillStationStructure fillStationStructure){
        SKAnnotation fillStationAnnotation = new SKAnnotation(30);
        fillStationAnnotation.setLocation(fillStationStructure.getCoordinates());
        fillStationAnnotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_PURPLE);
        mapView.addAnnotation(fillStationAnnotation, SKAnimationSettings.ANIMATION_NONE);
        SKViaPoint viaPoint = new SKViaPoint(VIA_POINT_ICON_ID, fillStationStructure.getCoordinates());
        SKRouteManager.getInstance().addViaPoint(viaPoint, -1);
        rerouting = true;
        if((fillStationNumber + 1)<fillStations.size()) {
            fillStationNumber++;
        } else{
            fillStationNumber = -1;
        }
        fillStationResponse = 0;
    }

    @Override
    public void onUpdateNavigationState(SKNavigationState skNavigationState) {
        currentStationNr = 0;

        if(fillStationNumber != -2) {
            if (fillStationResponse == 1) {
                currentStationNr = fillStationNumber;
                goViaFuelStation(fillStations.get(fillStationNumber));
            }
            if (fillStationResponse == 2) {
                if ((fillStationNumber + 1) < fillStations.size()) {
                    fillStationNumber++;
                } else {
                    fillStationNumber = -1;
                }
                fillStationResponse = 0;
            }
            if (fillStationNumber != -1) {
                if (fillStations != null && fillStationResponse == 0) {
                    if (fillStationNumber < fillStations.size()) {
                        if (!fillStationsUsed.get(currentStationNr) && SKGeoUtils.calculateAirDistanceBetweenCoordinates
                                (fillStations.get(fillStationNumber).getCoordinates(),
                                        SKPositionerManager.getInstance().getCurrentGPSPosition(true).getCoordinate()) < 2000) {
                            fillStationsUsed.set(currentStationNr, true);
                            fillStationMessage(fillStations.get(fillStationNumber));
                        }
                    }
                }
            }
        }

        if(this.fatigueAlgorithm.getResponse()){
            fatigueMessage();
        }

        SKLogging.writeLog("SKToolsLogicManager", "NAVIGATION STATE " + skNavigationState.toString(), SKLogging.LOG_DEBUG);
        if(currentUserDisplayMode == SKMapSettings.SKMapDisplayMode.MODE_3D){
            mapView.getMapSettings().setStreetNamePopupsShown(true);
        }
        lastAudioAdvices = skNavigationState.getCurrentAdviceAudioAdvices();
        navigationCurrentDistance = (int) Math.round(skNavigationState.getDistanceToDestination());

        if (reRoutingInProgress) {
            reRoutingInProgress = false;
            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean followerMode = SKToolsNavigationUIManager.getInstance().isFollowerMode();
                    if (followerMode) {
                        SKToolsNavigationUIManager.getInstance().showFollowerModePanels(configuration
                                .getNavigationType() == SKNavigationSettings.SKNavigationType.SIMULATION);
                    }
                }
            });
        }
        int mapStyle = SKToolsMapOperationsManager.getInstance().getCurrentMapStyle();
        SKToolsNavigationUIManager.getInstance().handleNavigationState(skNavigationState, mapStyle);
    }

    @Override
    public void onReRoutingStarted() {
        rerouting = true;
        if (SKToolsNavigationUIManager.getInstance().isFollowerMode()) {
            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SKToolsNavigationUIManager.getInstance().hideTopPanels();
                    SKToolsNavigationUIManager.getInstance().hideBottomAndLeftPanels();
                    SKToolsNavigationUIManager.getInstance().showReroutingPanel();
                    reRoutingInProgress = true;
                }
            });
        }
    }

    @Override
    public void onFreeDriveUpdated(String countryCode, String streetName, String referenceName,
                                   SKNavigationState.SKStreetType streetType, double currentSpeed, double speedLimit) {
        if (SKToolsNavigationUIManager.getInstance().isFollowerMode()) {
            int mapStyle = SKToolsMapOperationsManager.getInstance().getCurrentMapStyle();
            SKToolsNavigationUIManager.getInstance().handleFreeDriveUpdated(countryCode, streetName, currentSpeed,
                                                        speedLimit, configuration.getDistanceUnitType(), mapStyle);
        }
    }

    @Override
    public void onVisualAdviceChanged(final boolean firstVisualAdviceChanged, final boolean secondVisualAdviceChanged,
                                                                            final SKNavigationState skNavigationState) {
        final int mapStyle = SKToolsMapOperationsManager.getInstance().getCurrentMapStyle();
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SKToolsNavigationUIManager.getInstance().setTopPanelsBackgroundColour(mapStyle,
                                                            firstVisualAdviceChanged, secondVisualAdviceChanged);
                SKNavigationManager.getInstance().renderVisualAdviceImage(skNavigationState.getFirstCrossingDescriptor(),
                    skNavigationState.getCurrentAdviceVisualAdviceFile(),
                    SKToolsNavigationUIManager.getInstance().getVisualAdviceColorAccordingToBackgroundsDrawableColor(false));
            }
        });
    }

    @Override
    public void onTunnelEvent(boolean b) {
    }

    @Override
    public void onRouteCalculationCompleted(final SKRouteInfo skRouteInfo) {
        if (!skRouteInfo.isCorridorDownloaded()) {
            return;
        }
        skRouteInfoList.add(skRouteInfo);
        if (SKToolsNavigationUIManager.getInstance().isPreNavigationMode()) {
            SKToolsMapOperationsManager.getInstance().zoomToRoute(currentActivity);
        }

        final List<SKRouteAdvice> advices = SKRouteManager.getInstance().getAdviceListForRouteByUniqueId
                            (skRouteInfo.getRouteID(), SKMaps.SKDistanceUnitType.DISTANCE_UNIT_KILOMETER_METERS);
        if (advices != null){
            for (SKRouteAdvice advice : advices) {
                SKLogging.writeLog("SKToolsLogicManager", " Route advice is " + advice.toString(), SKLogging.LOG_DEBUG);
            }
        }

        final String[] routeSummary = skRouteInfo.getRouteSummary();
        if (routeSummary != null) {
            for (String street :routeSummary) {
                SKLogging.writeLog("SKToolsLogicManager" , " Route Summary street = " + street , SKLogging.LOG_ERROR);
            }
        } else {
            SKLogging.writeLog("SKToolsLogicManager", "Route summary is null " , SKLogging.LOG_ERROR);
        }
    }

    @Override
    public void onRouteCalculationFailed(SKRoutingErrorCode skRoutingErrorCode) {
        SKToolsNavigationUIManager.getInstance().showRouteCalculationFailedDialog(skRoutingErrorCode);
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SKToolsNavigationUIManager.getInstance().removePreNavigationViews();
            }
        });
    }

    private void setRouteCalculationsEnded(int i, boolean ended) {
        this.routeCalculationsEnded[i] = ended;
    }

    private void displayRouteInfo(int i) {
        List <SKCoordinate> coordinateList = getCoordinatesForWeather(skRouteInfoList.get(i).getRouteID());
        int annotationIdStart = 0;
        switch (i){
            case 0:
                annotationIdStart = ANNOTATION_ID_DEFAULT;
                setAnnotationIDroute1(getAnnotationIDroute1() + coordinateList.size());
                setAnnotationIDroute2(getAnnotationIDroute1());
                break;
            case 1:
                annotationIdStart = getAnnotationIDroute1();
                setAnnotationIDroute2(getAnnotationIDroute2() + coordinateList.size());
                setAnnotationIDroute3(getAnnotationIDroute2());
                break;
            case 2:
                annotationIdStart = getAnnotationIDroute2();
                setAnnotationIDroute3(getAnnotationIDroute3() + coordinateList.size());
                break;
        }
        final String time = SKToolsUtils.formatTime(skRouteInfoList.get(i).getEstimatedTime());
        final String distance = SKToolsUtils.convertAndFormatDistance(skRouteInfoList.get(i)
                .getDistance(), configuration.getDistanceUnitType(), currentActivity);
        CostCalculator costCalculator = new CostCalculator();
        final String cost = String.format("%.2f", costCalculator.getCost(
                skRouteInfoList.get(i), getCurrentActivity().getApplicationContext(), i));
        Log.d("onallroutes", "cost");
        SKToolsNavigationUIManager.getInstance().sePreNavigationButtons(i, time, distance, cost);
        LayoutInflater inflater = (LayoutInflater) currentActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = currentActivity.findViewById(com.skobbler.sdkdemo.R.id.customView);
        if (Utils.isInternetAvailable(getCurrentActivity().getApplicationContext())) {
            new WeatherPrediction().execute(skRouteInfoList.get(i).getRouteID(), mapView, inflater, view,
                    currentActivity.getResources(), currentActivity.getPackageName(), coordinateList, annotationIdStart);
        }
        setRouteCalculationsEnded(i, true);
    }

    @Override
    public void onAllRoutesCompleted() {
        Log.d("onallroutes", "completed");

        for (int j = 0; j < 3; j++) {
            routeCalculationsEnded[j] = false;
        }

        if (!skRouteInfoList.isEmpty()) {
            currentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = ANNOTATION_ID_DEFAULT; i < annotationIDroute1 || i < annotationIDroute2
                                                                                || i < annotationIDroute3; i++){
                        mapView.deleteAnnotation(i);
                    }

                    if (SKToolsNavigationUIManager.getInstance().isPreNavigationMode()) {
                        SKToolsNavigationUIManager.getInstance().showStartNavigationPanel();
                    }
                    if (!rerouting) {
                        if (skRouteInfoList.size() > 0) {
                            Thread route0 = new Thread() {
                                @Override
                                public void run() {
                                    Log.d("LOGIC_MANAGER ", "0");
                                    displayRouteInfo(0);
                                }
                            };
                            route0.start();
                        }
                        if (skRouteInfoList.size() > 1) {
                            Thread route1 = new Thread() {
                                @Override
                                public void run() {
                                    while (!routeCalculationsEnded[0]) {
                                        try {
                                            Log.d("LOGIC_MANAGER ", "1");
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    displayRouteInfo(1);
                                }
                            };
                            route1.start();
                        }
                        if (skRouteInfoList.size() > 2) {
                            Thread route2 = new Thread() {
                                @Override
                                public void run() {
                                    while (!routeCalculationsEnded[1]) {
                                        try {
                                            Log.d("LOGIC_MANAGER ", "2");
                                            Thread.sleep(1000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    displayRouteInfo(2);
                                }
                            };
                            route2.start();
                        }
                    }

                    int routeId = skRouteInfoList.get(0).getRouteID();
                    SKRouteManager.getInstance().setCurrentRouteByUniqueId(routeId);
                    SKToolsNavigationUIManager.getInstance().selectAlternativeRoute(0);
                    if (SKToolsNavigationUIManager.getInstance().isPreNavigationMode()) {
                        SKToolsMapOperationsManager.getInstance().zoomToRoute(currentActivity);
                    }
                }
            });
        }

        if (navigationListener != null) {
            navigationListener.onRouteCalculationCompleted();
        }

        rerouting = false;
    }

    @Override
    public void onServerLikeRouteCalculationCompleted(SKRouteJsonAnswer skRouteJsonAnswer) {
    }

    @Override
    public void onOnlineRouteComputationHanging(int i) {
    }

    @Override
    public void onCurrentPositionUpdate(SKPosition skPosition) {
        lastUserPosition = skPosition;
        if (configuration.getNavigationType() == SKNavigationSettings.SKNavigationType.REAL) {
            SKPositionerManager.getInstance().reportNewGPSPosition(skPosition);
        }
    }

    @Override
    public void onViaPointReached(int index) {
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SKToolsNavigationUIManager.getInstance().showViaPointPanel();
            }
        });
    }

    @Override
    public void onNewMapScreenAvailable(int width, int height, String dirPath) {
    }

    private static List<SKCoordinate> getCoordinatesForWeather (int routeID) {
        List<SKCoordinate> coordinates = new ArrayList<SKCoordinate>();
        int i = 0;
        List<SKExtendedRoutePosition> positions = SKRouteManager.getInstance()
                                                    .getExtendedRoutePointsForRouteByUniqueId(routeID);
        for (SKExtendedRoutePosition pos : positions) {
            if (i % 2500 == 0) {
                coordinates.add(new SKCoordinate(pos.getCoordinate().getLongitude(), pos.getCoordinate().getLatitude()));
            }
            i++;
        }
        i--;
        if (i % 2500 != 0) {
            coordinates.add(new SKCoordinate(positions.get(i).getCoordinate().getLongitude(),
                                                positions.get(i).getCoordinate().getLatitude()));
        }
        return coordinates;
    }

    private static int getAnnotationIDroute1() {
        return annotationIDroute1;
    }

    private static void setAnnotationIDroute1(int annotationIDroute1) {
        SKToolsLogicManager.annotationIDroute1 = annotationIDroute1;
    }

    private static int getAnnotationIDroute2() {
        return annotationIDroute2;
    }

    private static void setAnnotationIDroute2(int annotationIDroute2) {
        SKToolsLogicManager.annotationIDroute2 = annotationIDroute2;
    }

    private static int getAnnotationIDroute3() {
        return annotationIDroute3;
    }

    private static void setAnnotationIDroute3(int annotationIDroute3) {
        SKToolsLogicManager.annotationIDroute3 = annotationIDroute3;
    }

}
