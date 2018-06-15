package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.math3.linear.RealVector;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;
import model.Pair;

public class CsvFileCreator {

    public void exportOriginalCartesianPoints(Context context) {
        Toast.makeText(context,"Anzahl, estimatedPoints:  " + Service.getEstimatedPoints().size() + "\n" + "Time:  " + new Timestamp(System.currentTimeMillis()),Toast.LENGTH_LONG).show();

        String pathToFolder = generateSpecificFolder();

        File file = new File(pathToFolder + "/exportMeasurement_originalCartesian" + new Timestamp(System.currentTimeMillis()) + ".csv");
        CsvWriter csvWriter = new CsvWriter();

        try (CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)) {
            // Beschriftung des Kopfes
            csvAppender.appendLine("Timestamp", "X", "Y");

            // Hole Daten und schreibe
            for(Pair p : Service.getListOfPoints()){
                String timestamp = String.valueOf(p.getTimestamp());
                String x = String.valueOf(p.getX());
                String y = String.valueOf(p.getY());

                csvAppender.appendLine(timestamp,x,y);
                //csvAppender.endLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        exportEstimatedPoints(pathToFolder);

//        try (CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)) {
//            // header
//            csvAppender.appendLine("header1", "header2");
//
//            // 1st line in one operation
//            csvAppender.appendLine("value1", "value2");
//
//            // 2nd line in split operations
//            csvAppender.appendField("value3");
//            csvAppender.appendField("value4");
//            csvAppender.endLine();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    private void exportEstimatedPoints(String pathToFolder) {
        File file = new File(pathToFolder + "/exportMeasurement_estimatedPoints" + new Timestamp(System.currentTimeMillis()) + ".csv");
        CsvWriter csvWriter = new CsvWriter();

        try (CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)) {
            // Beschriftung des Kopfes
            csvAppender.appendLine("Timestamp", "X", "Y", "VectorU", "Velocity_X", "Velocity_Y");

            Collection<String[]> data = new ArrayList<>();
            data.add(new String[] {"Timestamp", "X", "Y", "VectorU", "Velocity_X", "Velocity_Y"});

//            for(int i =0; i < Service.getEstimatedPoints().size(); i++) {
//                Log.d("HI", "Iteration in exportMeasurement:  " + i);
//                String timestamp = String.valueOf(Service.getEstimatedPoints().get(i).getTimestamp());
//                String x = String.valueOf(Service.getEstimatedPoints().get(i).getX());
//                String y = String.valueOf(Service.getEstimatedPoints().get(i).getY());
//                String vectorU = Service.getThread().getFilter().getU().toString();
//                String velocityX = String.valueOf(Service.getEstimatedVelocity().get(i).getX());
//                String velocityY = String.valueOf(Service.getEstimatedVelocity().get(i).getY());
//
//                data.add(new String[] {timestamp,x,y,vectorU,velocityX,velocityY});
//            }
//
            //csvWriter.write(file, StandardCharsets.UTF_8, data);
            // Hole Daten und schreibe
            //for(int i =0; i < Service.getEstimatedPoints().size(); i++){
            for(int i =0; i < 50000; i++){
                Log.d("HI", "Iteration in exportMeasurement:  " + i);
                String timestamp = String.valueOf(Service.getEstimatedPoints().get(i).getTimestamp());
                String x = String.valueOf(Service.getEstimatedPoints().get(i).getX());
                String y = String.valueOf(Service.getEstimatedPoints().get(i).getY());
                String vectorU = Service.getThread().getFilter().getU().toString();
                String velocityX = String.valueOf(Service.getEstimatedVelocity().get(i).getX());
                String velocityY = String.valueOf(Service.getEstimatedVelocity().get(i).getY());

                csvAppender.appendLine(timestamp,x,y,vectorU,velocityX,velocityY);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateSpecificFolder() {
        // Erzeuge einen Ordner mit aktuellem timestamp
        String folder_main = "exportMeasurement_LH_" + new Timestamp(System.currentTimeMillis());

        File f = new File(Environment.getExternalStorageDirectory(), folder_main);
        if (!f.exists()) {
            f.mkdirs();
        }

        return f.getAbsolutePath();
    }
}
