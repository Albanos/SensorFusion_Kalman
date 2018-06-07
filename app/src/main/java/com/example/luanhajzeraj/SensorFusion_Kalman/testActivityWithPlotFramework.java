package com.example.luanhajzeraj.SensorFusion_Kalman;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.RectRegion;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import model.Pair;

public class testActivityWithPlotFramework extends AppCompatActivity {
    private static boolean setScaling = false;

    private static int minXValue =0;
    private static int maxXValue =0;
    private static int minYValue =0;
    private static int maxYValue =0;

    private XYPlot plot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_with_plot_framework);

        Toast.makeText(this, "Anzahl Punkte:  " + Service.getListOfPoints().size(),
                    Toast.LENGTH_LONG).show();

        firstDrawFramework();
        //secondDrawFramework();
        //anderesBeispielMitSecondDrawFramework();
    }

//    private void anderesBeispielMitSecondDrawFramework() {
//        // initialize our XYPlot reference:
//        plot = (XYPlot) findViewById(R.id.plot);
//
//        XYSeries series1 = generateScatter("series1", 80, new RectRegion(10, 50, 10, 50));
//
//        plot.setDomainBoundaries(0, 80, BoundaryMode.FIXED);
//        plot.setRangeBoundaries(0, 80, BoundaryMode.FIXED);
//
//        // create formatters to use for drawing a series using LineAndPointRenderer
//        // and configure them from xml:
//        LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.RED, Color.GREEN, null, null);
//
//
//        // add each series to the xyplot:
//        plot.addSeries(series1, series1Format);
//
//        // reduce the number of range labels
//        plot.setLinesPerRangeLabel(3);
//    }
//
//    private XYSeries generateScatter(String title, int numPoints, RectRegion region) {
//        SimpleXYSeries series = new SimpleXYSeries(title);
//        for(int i = 0; i < numPoints; i++) {
//            series.addLast(
//                    region.getMinX().doubleValue() + (Math.random() * region.getWidth().doubleValue()),
//                    region.getMinY().doubleValue() + (Math.random() * region.getHeight().doubleValue())
//            );
//        }
//        return series;
//    }
//
//    private void secondDrawFramework() {
//        // initialize our XYPlot reference:
//        plot = (XYPlot) findViewById(R.id.plot);
//
//        // create a couple arrays of y-values to plot:
//        final Number[] domainLabels = {-5,-4,-3,-2,-1,0,1, 2, 3, 4, 5, 6, 7, 8, 9, 14};
//        Number[] series1Numbers = {-8,-1,-6,-8,0,1, 4, 2, 8, 4, 16, 8, 32, 16, 64};
//
//        XYSeries series1 = new SimpleXYSeries(
//                Arrays.asList(series1Numbers), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, "Erste Sammlung");
//
//        LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.RED, Color.GREEN, null, null);
//
//        plot.addSeries(series1, series1Format);
//
//
//        //?????
//        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(new Format() {
//            @Override
//            public StringBuffer format(Object obj, @NonNull StringBuffer toAppendTo, @NonNull FieldPosition pos) {
//                int i = Math.round(((Number) obj).floatValue());
//                return toAppendTo.append(domainLabels[i]);
//            }
//            @Override
//            public Object parseObject(String source, @NonNull ParsePosition pos) {
//                return null;
//            }
//        });
//    }

    private void firstDrawFramework() {
        Service.getThread().stop();

        GraphView graph = (GraphView) findViewById(R.id.graph);
        DataPoint[] points = new DataPoint[Service.getListOfPoints().size()];
        //DataPoint[] points = new DataPoint[2];
        DataPoint[] points2 = new DataPoint[Service.getEstimatedPoints().size()];

        // Bestimme die echten Punkte
        for(int i =0; i < points.length; i++){
            if(i == 0){
                points[i] = new DataPoint(0,0);
                continue;
            }
            points[i] = new DataPoint(Service.getListOfPoints().get(i).getX(), Service.getListOfPoints().get(i).getY());
        }

        // Test-Punkte, um den Graph-Typ zu beurteilen. WICHTIG: Data-point-größe muss zwei sein!!!
//        points[0] = new DataPoint(10,10);
//        points[1] = new DataPoint(40,70);

        // Bestimme die geschätzten Punkte
//        for(int i =0; i < points2.length; i++){
//            if(i == 0){
//                points2[i] = new DataPoint(0,0);
//                continue;
//            }
//            points2[i] = new DataPoint(Service.getEstimatedPoints().get(i).getX(), Service.getEstimatedPoints().get(i).getY());
//        }


        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(points);
        //PointsGraphSeries<DataPoint> series = new PointsGraphSeries<>(points);
        LineGraphSeries<DataPoint> series2 = new LineGraphSeries<>(points2);
        series2.setColor(Color.RED);

        // Zeichne für beide Garphen die Punkte und verbinde sie mit Linien: lege Größe der
        // Punkte
        series.setDrawDataPoints(true);
        //series.setDataPointsRadius(15);
        series.setDataPointsRadius(10);
        series.setThickness(8);

        series2.setDrawDataPoints(true);
        series2.setDataPointsRadius(5);
        series2.setThickness(8);

        // Ermittle den minimalen und maximalen x- & y-Wert der Punkte
//        if(! setScaling) {
//            setScaling = true;
//            for (Pair p : Service.getListOfPoints()) {
//                double x = p.getX();
//                double y = p.getY();
//
//                if (x < minXValue) {
//                    minXValue = (int) x;
//                }
//                if (x > maxXValue) {
//                    maxXValue = (int) x;
//                }
//                if (y < minYValue) {
//                    minYValue = (int) y;
//                }
//                if (y > maxYValue) {
//                    maxYValue = (int) y;
//                }
//            }
//        }
        // Setze die max und min werte für die achsen
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-100);
        graph.getViewport().setMaxY(100);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(-100);
        graph.getViewport().setMaxX(100);
//
//        // enable scaling and scrolling
////        graph.getViewport().setScalable(true);
////        graph.getViewport().setScalableY(true);
//
//        graph.getViewport().setScrollable(true); // enables horizontal scrolling
//        graph.getViewport().setScrollableY(true); // enables vertical scrolling

//        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[] {
//                new DataPoint(-3, -2),
//                new DataPoint(-2, -5),
//                new DataPoint(-1, -3),
//                new DataPoint(0, 1),
//                new DataPoint(1, 5),
//                new DataPoint(2, 3),
//                new DataPoint(3, 2),
//                new DataPoint(4, 6)
//        });
        // Zeilen zum Aktivieren des Skalieren und Zoomens
        // set manual X bounds
//        graph.getViewport().setYAxisBoundsManual(true);
//        graph.getViewport().setMinY(-150);
//        graph.getViewport().setMaxY(150);
//
//        graph.getViewport().setXAxisBoundsManual(true);
//        graph.getViewport().setMinX(4);
//        graph.getViewport().setMaxX(80);

        // enable scaling and scrolling
//        graph.getViewport().setScalable(true);
//        graph.getViewport().setScalableY(true);

        graph.addSeries(series);
        //graph.addSeries(series2);

        Service.getThread().start();
    }
}
