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

    static boolean calculateCartesianCoordinats() {
        if (listOfPositions.size() >= 2) {
            EstimationFilter filter = EstimationFilter.getInstance();

            // Berechne für den Rest die x- und y-Koordinaten
            for (GlobalPosition gp : listOfPositions) {
                // Berechne Abstand der jeweiligen position zur ersten Position
                double distance = filter.coordinateDistanceBetweenTwoPoints(firstGlobalPositionOfList, gp);
                double angle = filter.coordinateAngleBetweenTwoPoints(firstGlobalPositionOfList, gp);
                // Berechne auf Basis von Distanz und Winkel die x- und y-Komponente des zweiten Punktes
                // Formeln:
                // x = dist. * sin(angle)
                // y = dist. * cos(angle)
                Service.getListOfPoints().add(new Pair(distance * Math.sin(angle), distance * Math.cos(angle)));
            }
            return true;
        }
        return false;
    }

    public static GlobalPosition getFirstGlobalPositionOfList() {
        return firstGlobalPositionOfList;
    }

    private static boolean canSave = true;
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
}
