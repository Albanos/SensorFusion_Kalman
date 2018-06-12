package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.content.Context;
import android.os.Environment;

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

import model.Pair;

// Von: https://stackoverflow.com/a/16475889

public class ExcelFileCreator {

    public void createExcelFile(Context context) {


        // ======================Grundstruktur für den Excel-Export erzeugen
        HSSFWorkbook workbook = new HSSFWorkbook();

        HSSFSheet firstSheet = workbook.createSheet("Original");
        HSSFSheet secondSheet = workbook.createSheet("Estimated");

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

        // Zeichne originale Punkte
        int i = 2;
        for(Pair p : Service.getListOfPoints()){
            String timestamp = p.getTimestamp().toString();
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
        for(Pair p : Service.getEstimatedPoints()){
            String timestamp = p.getTimestamp().toString();
            Double x = p.getX();
            Double y = p.getY();

            HSSFRow currentRow = secondSheet.createRow(i);
            HSSFCell estimatedTimestamp = currentRow.createCell(0);
            HSSFCell estimatedX = currentRow.createCell(1);
            HSSFCell estimatedY = currentRow.createCell(2);

            estimatedTimestamp.setCellValue(timestamp);
            estimatedX.setCellValue(x);
            estimatedY.setCellValue(y);
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
