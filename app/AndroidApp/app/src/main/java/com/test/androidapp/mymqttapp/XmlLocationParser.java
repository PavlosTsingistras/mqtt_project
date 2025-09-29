package com.test.androidapp.mymqttapp;

import android.content.res.AssetManager;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class XmlLocationParser {

    private static final String TAG = "XmlLocationParser";

    public static class LocationVector {
        public double latitude;
        public double longitude;

        public LocationVector(double lat, double lon) {
            this.latitude = lat;
            this.longitude = lon;
        }
    }


    public static List<LocationVector> parseXmlFile(AssetManager assets, String fileName)
            throws IOException, XmlPullParserException {

        List<LocationVector> list = new ArrayList<>();
        try (InputStream is = assets.open(fileName)) {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(is, "UTF-8");

            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && "vehicle".equals(xpp.getName())) {
                    String lonStr = xpp.getAttributeValue(null, "x");
                    String latStr = xpp.getAttributeValue(null, "y");
                    if (lonStr != null && latStr != null) {
                        double lon = Double.parseDouble(lonStr);
                        double lat = Double.parseDouble(latStr);
                        list.add(new LocationVector(lat, lon));
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing " + fileName, e);
            throw e;
        }
        return list;
    }
}
