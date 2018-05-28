package com.example.luanhajzeraj.SensorFusion_Kalman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import geodesy.Ellipsoid;
import geodesy.GeodeticCalculator;
import geodesy.GeodeticMeasurement;
import geodesy.GlobalCoordinates;
import geodesy.GlobalPosition;
import model.Coordinates;
import model.FilterThread;
import model.Pair;

/**
 * Created by Luan Hajzeraj on 31.03.2018.
 */
class Service {
    /**
     * Liste mit realen GPS Koordinaten auf Basis von geodesy
     */
    private static List<GlobalPosition> listOfPositions = new ArrayList<>();
    //private static List<Pair> listOfPoints = new ArrayList<>();
    private static LinkedList<Pair> listOfPoints = new LinkedList<>();
    private static LinkedList<Pair> estimatedPoints = new LinkedList<>();
    private static LinkedList<Pair> oldEstimatedPoints = new LinkedList<>();
    private static List<Pair> listOfOldPoints = new ArrayList<>();
    private static GlobalPosition firstGlobalPositionOfList;
    private static Pair initialPoint;
    private static boolean canSave = true;
    private static boolean semaphore = true;
    private static float[] linVeloc = new float[]{0,0};
    private static float locationAccurancy;
    private static float accel_x_wgs;
    private static float accel_y_wgs;
    private static Pair firstCartasianPoint = null;
    private static boolean isDrawing = false;
    private static FilterThread thread = new FilterThread();
    private static LinkedList<Pair> estimatedVelocity = new LinkedList<>();
    private static double dt =0;
    private static double oldDt =0;
    private static HashMap<Pair, List<Double>> angleDistancePairMap = new HashMap<>();
    private static LinkedList<Coordinates> listOfWGSDestinationPoints = new LinkedList<>();

    /**
     * Berechnet auf Basis der Liste von GlobalPositions die kartesischen Koordinaten und speichert
     * diese in listOfPoints.
     *
     * @return
     */
    static boolean calculateCartesianCoordinats() {
        //listOfPoints.clear();
        if (listOfPositions.size() >= 2) {

            // Berechne für den Rest die x- und y-Koordinaten
            for (GlobalPosition gp : listOfPositions) {
                // Berechne Abstand der jeweiligen position zur ersten Position
                // Der Abstand wird in Metern angegeben; Der Winkel wird im Winkelmaß (deg) berechnet,
                // ist immer am Nordpol ausgerichtet und bewegt sich entgegen des Uhrzeigersinns
                // Beispiel: Punkt 1 ist irgendwo im Raum. Punkt 2 ist rechts daneben, dann ist der
                // Winkel etwa 90 Grad
                double distance = coordinateDistanceBetweenTwoPoints(firstGlobalPositionOfList, gp);
                double angle = coordinateAngleBetweenTwoPoints(firstGlobalPositionOfList, gp);

                // Berechne auf Basis von Distanz und Winkel die x- und y-Komponente des zweiten Punktes
                // Formeln (SEHR WICHTIG: Eingabe MUSS in rad sein):
                // x = dist. * sin(rad (angle) )
                // y = dist. * cos(rad (angle) )
                Pair point = new Pair(distance * Math.sin(Math.toRadians(angle)), distance * Math.cos(Math.toRadians(angle)));

                Service.getListOfPoints().add(point);


                // MERKE: Dies muss eigentlich für die geschätzten Punkte erfolgen!!!
                // Jedoch ist noch unklar, wie Winkel und Abstand der geschätzten, KARTESISCHEN Pkt.
                // zum Ausgangspunkt (GLOBALPOSITION) erfolgen soll...

                // Füge der Map den Punkt mit seinem Absatnd und Winkel hinzu, um Später die Punkte
                //wie zurück (in Kugelkoordinaten) rechnen zu können
                List<Double> list = new ArrayList<>();
                list.add(distance);
                list.add(angle);
                Service.getAngleDistancePairMap().put(point,list);

                // Setze den ersten berechneten Punkt
                if(Service.getFirstCartasianPoint() == null){
                    Service.setFirstCartasianPoint(new Pair(
                            distance * Math.sin(Math.toRadians(angle)),
                            distance * Math.cos(Math.toRadians(angle))));
                }
            }

            // Setze den initialPoint mit seinen Koordinaten genau einmal: Wähle dafür den ersten
            // berechneten Punkt auf Basis der "firstGlobalPosition", da dieser x- & y-Koordinaten.
            // firstGlobalPosition ist eben eine GlobalPosition
            if(semaphore) {
                initialPoint = new Pair(getListOfPoints().get(0).getX(), getListOfPoints().get(0).getY());
                semaphore = false;
            }
            return true;
        }
        return false;
    }

    /**
     * Berechnet die neuen Koordinaten für einen spezifischen Zielpunkt, ausgehend vom
     * Start-Punkt. Die Breite und länge wird in einer Map zurückgegeben
     *
     * @param pair
     * @return
     */
    static HashMap<String, Double> calculateWGSCoordiantesOnCartesianCoordinates(Pair pair){
        double distanceOfPair = Service.getAngleDistancePairMap().get(pair).get(0);
        double angleOfPair = Service.getAngleDistancePairMap().get(pair).get(1);

        GeodeticCalculator calculator = new GeodeticCalculator();
        GlobalCoordinates globalCoordinates = calculator.calculateEndingGlobalCoordinates(Ellipsoid.WGS84,
                Service.getFirstGlobalPositionOfList(), distanceOfPair, angleOfPair);

        HashMap<String, Double> returnMap = new HashMap<>();

        returnMap.put("Latitude", globalCoordinates.getLatitude());
        returnMap.put("Longitude", globalCoordinates.getLongitude());

        return returnMap;
    }

    static void calculateWGSCoordinatesForAllCartesianPoints(){
        for(Pair p : Service.getEstimatedPoints()){
            double distance = Service.getAngleDistancePairMap().get(p).get(0);
            double angle = Service.getAngleDistancePairMap().get(p).get(1);

            GeodeticCalculator calculator = new GeodeticCalculator();
            GlobalCoordinates globalCoordinates = calculator.calculateEndingGlobalCoordinates(Ellipsoid.WGS84,
                    Service.getFirstGlobalPositionOfList(), distance, angle);

            Coordinates coordinates = new Coordinates(globalCoordinates.getLatitude(), globalCoordinates.getLongitude());

            if(! Service.getListOfWGSDestinationPoints().contains(coordinates)) {
                Service.getListOfWGSDestinationPoints().add(coordinates);
            }
        }
    }

    /**
     * Berechnet die Basisvektoren der GNSS-Geschwindigkeit (Geschwindigkeit in x- & y-Richtung).
     *
     * @param gnssVelocity
     * @param position
     * @return
     */
    static List<Double> calculateBaseVectorsOfGNSSVelocity(float gnssVelocity, GlobalPosition position){
        List<Double> returnList = new ArrayList<>();

        // Winkel zwischen allererstem Datum und position (im Gradmaß)
        double angle = coordinateAngleBetweenTwoPoints(firstGlobalPositionOfList, position);

        // Berechne die Geschwindigkeit in x- & y-Richtung (gnssVelocity stellt Betrag dar):
        // vX = gnssVelocity * sin(rad(angle))
        // vY = gnssVelocity * cos(rad(angle))
        double velocity_x = gnssVelocity * Math.sin(Math.toRadians(angle));
        double velocity_y = gnssVelocity * Math.cos(Math.toRadians(angle));

        returnList.add(velocity_x);
        returnList.add(velocity_y);

        return returnList;
    }

    /**
     * Berechnet die Distanz zwischen zwei globalPositions
     * @param g1
     * @param g2
     * @return
     */
    static double coordinateDistanceBetweenTwoPoints(GlobalPosition g1, GlobalPosition g2){
        if(g1 != null && g2 != null){
            GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

            GeodeticMeasurement gm = geodeticCalculator
                    .calculateGeodeticMeasurement(Ellipsoid.WGS84, g1, g2);

            return gm.getEllipsoidalDistance();
        }
        return 0;
    }

    /**
     * Berechnet den Winkel zwischen zwei globalPositions im Gradmaß (Winkel ist am Noden
     * ausgerichtet ; Winkelrichtung ist im Uhrzeigersinn)
     * @param g1
     * @param g2
     * @return
     */
    static double coordinateAngleBetweenTwoPoints(GlobalPosition g1, GlobalPosition g2){
        if(g1 != null && g2 != null){
            GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

            GeodeticMeasurement gm = geodeticCalculator
                    .calculateGeodeticMeasurement(Ellipsoid.WGS84, g1, g2);

            return gm.getAzimuth();
        }
        return 0;
    }

    /**
     * Berechnet die lineare Geschwindigkeit auf Basis von linearer Beschleunigung näherungsweise.
     * Problem bei der Berechnung sind die stark-verrauschten Beschleunigungsdaten, welche für
     * starke Sprünge in der Geschwindigkeitsberechnung sorgen...
     *
     * @param accelerometer_x
     * @param accelerometer_y
     * @param dt
     */
    static void calculateLinearVelocity(float accelerometer_x, float accelerometer_y, float dt) {
        // Geschwindigkeit berechnen
        // Berechnung von linearer Geschwindigkeit, siehe wikipedia: "Gleichmässig beschleunigte Bew."
        linVeloc[0] = linVeloc[0] + (accelerometer_x * dt);
        linVeloc[1] = linVeloc[1] + (accelerometer_y * dt);
    }

    public static GlobalPosition getFirstGlobalPositionOfList() {
        return firstGlobalPositionOfList;
    }

    public static void setFirstGlobalPositionOfList(GlobalPosition firstGlobalPositionOfList) {
        if(canSave) {
            Service.firstGlobalPositionOfList = firstGlobalPositionOfList;
            listOfPositions.remove(firstGlobalPositionOfList);
            canSave = false;
        }
    }

    static List<Pair> getListOfOldPoints() {
        return listOfOldPoints;
    }

    public static LinkedList<Pair> getOldEstimatedPoints() {
        return oldEstimatedPoints;
    }

    //    static List<Pair> getListOfPoints() {
//        return listOfPoints;
//    }

    static LinkedList<Pair> getListOfPoints() {
        return listOfPoints;
    }
    static List<GlobalPosition> getListOfPositions() {
        return listOfPositions;
    }

    public static Pair getInitialPoint() {
        return initialPoint;
    }

    public static float[] getLinVeloc() {
        return linVeloc;
    }

    public static void setLinVeloc(float[] linVeloc) {
        Service.linVeloc = linVeloc;
    }

    public static float getLocationAccurancy() {
        return locationAccurancy;
    }

    public static void setLocationAccurancy(float locationAccurancy) {
        Service.locationAccurancy = locationAccurancy;
    }

    public static float getAccel_x_wgs() {
        return accel_x_wgs;
    }

    public static void setAccel_x_wgs(float accel_x_wgs) {
        Service.accel_x_wgs = accel_x_wgs;
    }

    public static float getAccel_y_wgs() {
        return accel_y_wgs;
    }

    public static void setAccel_y_wgs(float accel_y_wgs) {
        Service.accel_y_wgs = accel_y_wgs;
    }

    public static LinkedList<Pair> getEstimatedPoints() {
        return estimatedPoints;
    }

    public static Pair getFirstCartasianPoint() {
        return firstCartasianPoint;
    }

    public static void setFirstCartasianPoint(Pair firstCartasianPoint) {
        Service.firstCartasianPoint = firstCartasianPoint;
    }

    public static boolean isIsDrawing() {
        return isDrawing;
    }

    public static void setIsDrawing(boolean isDrawing) {
        Service.isDrawing = isDrawing;
    }

    public static FilterThread getThread() {
        return thread;
    }

    public static LinkedList<Pair> getEstimatedVelocity() {
        return estimatedVelocity;
    }

    public static double getDt() {
        return dt;
    }

    public static void setDt(double dt) {
        Service.dt = dt;
    }

    public static double getOldDt() {
        return oldDt;
    }

    public static void setOldDt(double oldDt) {
        Service.oldDt = oldDt;
    }

    public static HashMap<Pair, List<Double>> getAngleDistancePairMap() {
        return angleDistancePairMap;
    }

    public static LinkedList<Coordinates> getListOfWGSDestinationPoints() {
        return listOfWGSDestinationPoints;
    }
}
