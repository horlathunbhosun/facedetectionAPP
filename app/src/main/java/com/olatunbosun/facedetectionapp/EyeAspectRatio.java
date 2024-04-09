package com.olatunbosun.facedetectionapp;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;


public class EyeAspectRatio {
    // Threshold for EAR below which we consider the eye to be closed
    private static final double EYE_OPEN_THRESHOLD = 0.3;

    public static double calculateEAR(MatOfPoint2f eye) {
        Point[] points = eye.toArray();

        // Calculate distances between vertical eye landmarks
        double a = euclideanDistance(points[1], points[5]);
        double b = euclideanDistance(points[2], points[4]);

        // Calculate distance between horizontal eye landmarks
        double c = euclideanDistance(points[0], points[3]);

        // Calculate EAR
        return (a + b) / (2.0 * c);
    }

    public static boolean isEyeClosed(MatOfPoint2f eye) {
        return calculateEAR(eye) < EYE_OPEN_THRESHOLD;
    }

    private static double euclideanDistance(Point p1, Point p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
    }
}