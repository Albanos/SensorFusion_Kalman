package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.content.Context;
import android.os.Environment;

import org.apache.commons.math3.linear.RealVector;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.LinkedList;

import model.Coordinates;
import model.Pair;

// Von: https://stackoverflow.com/a/16475889

public class ExcelFileCreator {

    public void createExcelFile() {


        // ======================Grundstruktur für den Excel-Export erzeugen
        HSSFWorkbook workbook = new HSSFWorkbook();

        HSSFSheet firstSheet = workbook.createSheet("Original");
        HSSFSheet secondSheet = workbook.createSheet("Estimated");
        HSSFSheet thirdSheet = workbook.createSheet("Original_WGS");

        // Die echten Punkte in sheet1 --> Header erzeugen
        HSSFRow sheet1_row1 = firstSheet.createRow(0);
        HSSFCell cell_1A = sheet1_row1.createCell(0);
        cell_1A.setCellValue(new HSSFRichTextString("Original"));

        HSSFRow sheet1_row2 = firstSheet.createRow(1);
        HSSFCell cell_2A = sheet1_row2.createCell(0);
        cell_2A.setCellValue(new HSSFRichTextString("Timestamp"));

        HSSFCell cell_2B = sheet1_row2.createCell(1);
        cell_2B.setCellValue(new HSSFRichTextString("X"));

        HSSFCell cell_2C = sheet1_row2.createCell(2);
        cell_2C.setCellValue(new HSSFRichTextString("Y"));

        // Die geschätzten Punkte in sheet2 --> Header erzeugen
        HSSFRow sheet2_row1 = secondSheet.createRow(0);
        HSSFCell cell_1A_sheet2 = sheet2_row1.createCell(0);
        cell_1A_sheet2.setCellValue(new HSSFRichTextString("Estimated"));

        HSSFRow sheet2_row2 = secondSheet.createRow(1);
        HSSFCell cell_2A_sheet2 = sheet2_row2.createCell(0);
        cell_2A_sheet2.setCellValue(new HSSFRichTextString("Timestamp"));

        HSSFCell cell_2B_sheet2 = sheet2_row2.createCell(1);
        cell_2B_sheet2.setCellValue(new HSSFRichTextString("X"));

        HSSFCell cell_2C_sheet2 = sheet2_row2.createCell(2);
        cell_2C_sheet2.setCellValue(new HSSFRichTextString("Y"));

        HSSFCell cell_2D_sheet2 = sheet2_row2.createCell(3);
        cell_2D_sheet2.setCellValue(new HSSFRichTextString("VectorU"));

        HSSFCell cell_2E_sheet2 = sheet2_row2.createCell(4);
        cell_2E_sheet2.setCellValue(new HSSFRichTextString("Velocity_X"));

        HSSFCell cell_2F_sheet2 = sheet2_row2.createCell(5);
        cell_2F_sheet2.setCellValue(new HSSFRichTextString("Velocity_Y"));

        HSSFRow sheet3_row1 = thirdSheet.createRow(0);
        HSSFCell cell_1A_sheet3 = sheet3_row1.createCell(0);
        cell_1A_sheet3.setCellValue(new HSSFRichTextString("Original_WGS"));

        HSSFRow sheet3_row2 = thirdSheet.createRow(1);
        HSSFCell cell_2A_sheet3 = sheet3_row2.createCell(0);
        cell_2A_sheet3.setCellValue(new HSSFRichTextString("Timestamp"));

        HSSFCell cell_2B_sheet3 = sheet3_row2.createCell(1);
        cell_2B_sheet3.setCellValue(new HSSFRichTextString("Latitude"));

        HSSFCell cell_2C_sheet3 = sheet3_row2.createCell(2);
        cell_2C_sheet3.setCellValue(new HSSFRichTextString("Longitude"));

        HSSFCell cell_2D_sheet3 = sheet3_row2.createCell(3);
        cell_2D_sheet3.setCellValue(new HSSFRichTextString("Altitude"));

        // Zeichne originalen kartesischen Punkte
        int i = 2;
        for(Pair p : Service.getListOfPoints()){
            //String timestamp = p.getTimestamp().toString();
            long timestamp = p.getTimestamp();
            Double x = p.getX();
            Double y = p.getY();

            HSSFRow currentRow = firstSheet.createRow(i);
            HSSFCell originalTimestamp = currentRow.createCell(0);
            HSSFCell originalX = currentRow.createCell(1);
            HSSFCell originalY = currentRow.createCell(2);

            originalTimestamp.setCellValue(timestamp);
            originalX.setCellValue(x);
            originalY.setCellValue(y);
            i++;
        }

        // Zeichne die geschätzten Punkte
        i = 2;
        int j=0;
        for(Pair p : Service.getEstimatedPoints()){
            //String timestamp = p.getTimestamp().toString();
            long timestamp = p.getTimestamp();
            Double x = p.getX();
            Double y = p.getY();
            RealVector currentU = Service.getThread().getFilter().getU();
            double estimatedVelocityX = Service.getEstimatedVelocity().get(j).getX();
            double estimatedVelocityY = Service.getEstimatedVelocity().get(j).getY();

            // Zeichne ausserdem noch die Globalen Koordinaten des jeweils geschätzten Punktes
            double latitudeOfP = Service.getPointToWGSMap().get(p).getLatitude();
            double longitudeOfP = Service.getPointToWGSMap().get(p).getLongitude();


            HSSFRow currentRow = secondSheet.createRow(i);
            HSSFCell estimatedTimestamp = currentRow.createCell(0);
            HSSFCell estimatedX = currentRow.createCell(1);
            HSSFCell estimatedY = currentRow.createCell(2);
            HSSFCell vectorU = currentRow.createCell(3);
            HSSFCell velocityX = currentRow.createCell(4);
            HSSFCell velocityY = currentRow.createCell(5);
            HSSFCell latitudeOfPoint = currentRow.createCell(6);
            HSSFCell longitudeOfPoint = currentRow.createCell(7);

            estimatedTimestamp.setCellValue(timestamp);
            estimatedX.setCellValue(x);
            estimatedY.setCellValue(y);
            vectorU.setCellValue(currentU.toString());
            velocityX.setCellValue(estimatedVelocityX);
            velocityY.setCellValue(estimatedVelocityY);
            latitudeOfPoint.setCellValue(latitudeOfP);
            longitudeOfPoint.setCellValue(longitudeOfP);

            i++;
            j++;
        }

        i = 2;
        // Zeichne die originalen WGS Punkte
        for(Coordinates c : Service.getListOfWGSCoordinates()){
            double latitude = c.getLatitude();
            double longitude = c.getLongitude();
            double altitude = c.getAltitude();
            long timestamp = c.getTimestamp();

            HSSFRow currentRow = thirdSheet.createRow(i);
            HSSFCell originalWGS_timestamp = currentRow.createCell(0);
            HSSFCell originalWGS_latitude = currentRow.createCell(1);
            HSSFCell originalWGS_longitude = currentRow.createCell(2);
            HSSFCell originalWGS_altitude = currentRow.createCell(3);

            originalWGS_timestamp.setCellValue(timestamp);
            originalWGS_latitude.setCellValue(latitude);
            originalWGS_longitude.setCellValue(longitude);
            originalWGS_altitude.setCellValue(altitude);

            i++;
        }

        // ======================Schreibe alles in ein file
        FileOutputStream fos = null;
        try {
            String str_path = Environment.getExternalStorageDirectory().toString();
            File file;
            //file = new File(str_path, "exportMeasurement_" + Calendar.getInstance().getTime().toString() + ".xls");
            file = new File(str_path, "exportMeasurement_" + new Timestamp(System.currentTimeMillis()) + ".xls");
            fos = new FileOutputStream(file);
            workbook.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Weiter mit dem thread
        Service.getThread().start();
    }
}
