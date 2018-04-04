package com.example.luanhajzeraj.SensorFusion_Kalman;

import java.util.ArrayList;
import java.util.List;

import geodesy.GlobalPosition;
import model.Pair;

/**
 * Created by Luan Hajzeraj on 31.03.2018.
 */
class Service {
    /**
     * Liste mit realen GPS Koordinaten auf Basis von geodesy
     */
    private static List<GlobalPosition> listOfPositions = new ArrayList<>();
    private static List<Pair> listOfPoints = new ArrayList<>();
    private static List<Pair> listOfOldPoints = new ArrayList<>();
    private static GlobalPosition firstGlobalPositionOfList;
    private static Pair initialPoint;
    private static boolean canSave = true;
    private static boolean semaphore = true;

    static boolean calculateCartesianCoordinats() {
        if (listOfPositions.size() >= 2) {
            EstimationFilter filter = EstimationFilter.getInstance();

            // Berechne für den Rest die x- und y-Koordinaten
            for (GlobalPosition gp : listOfPositions) {
                // Berechne Abstand der jeweiligen position zur ersten Position
                // Der Abstand wird in Metern angegeben; Der Winkel wird im Winkelmaß (deg) berechnet,
                // ist immer am Nordpol ausgerichtet und bewegt sich entgegen des Uhrzeigersinns
                // Beispiel: Punkt 1 ist irgendwo im Raum. Punkt 2 ist rechts daneben, dann ist der
                // Winkel etwa 90 Grad
                double distance = filter.coordinateDistanceBetweenTwoPoints(firstGlobalPositionOfList, gp);
                double angle = filter.coordinateAngleBetweenTwoPoints(firstGlobalPositionOfList, gp);

                // Berechne auf Basis von Distanz und Winkel die x- und y-Komponente des zweiten Punktes
                // Formeln (SEHR WICHTIG: Eingabe MUSS in rad sein):
                // x = dist. * sin(rad (angle) )
                // y = dist. * cos(rad (angle) )
                Service.getListOfPoints().add(new Pair(distance * Math.sin(Math.toRadians(angle)), distance * Math.cos(Math.toRadians(angle))));

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

    static List<Pair> getListOfPoints() {
        return listOfPoints;
    }
    static List<GlobalPosition> getListOfPositions() {
        return listOfPositions;
    }

    public static Pair getInitialPoint() {
        return initialPoint;
    }
}
