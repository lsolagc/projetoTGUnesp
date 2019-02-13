package com.tg.lucas.apptg;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.Log;
import android.util.Xml;
import android.view.View;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class MapXmlParser {

    private static final String TAG = "mobile/" + MainActivity.class.getSimpleName() + "/";
    private static final String ns = null;
    private List nodeLatLng = new ArrayList();
    private List nodeWay = new ArrayList();
    private Bounds newBounds = null;
    private Bounds registeredBounds;
    private final Context context;

    List getNodeLatLng() {
        return nodeLatLng;
    }

    List getNodeWay() {
        return nodeWay;
    }

    public Bounds getNewBounds() {
        return newBounds;
    }

    public static class Entry{
        final double node_id;
        final double node_lat;
        final double node_lng;
        String way_name;

        private Entry(double _nodeId, double _nodeLat, double _nodeLng, String _wayName){
            this.node_id = _nodeId;
            this.node_lat = _nodeLat;
            this.node_lng = _nodeLng;
            this.way_name = _wayName;
        }

    }

    public static class Bounds{
        final double _maxLat;
        final double _minLat;
        final double _maxLng;
        final double _minLng;

        Bounds(double maxLat, double maxLon, double minLat, double minLon){
            this._maxLat = maxLat;
            this._maxLng = maxLon;
            this._minLat = minLat;
            this._minLng = minLon;
        }
    }

    public MapXmlParser(Context _context) {
        this.context = _context;
    }

    void parse(InputStream in) throws XmlPullParserException, IOException{
        try{
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            //parser.nextTag();
            readXml(parser);
        }finally {
            in.close();
        }
    }

    private void readXml(XmlPullParser parser) throws XmlPullParserException, IOException{
        int eventType = parser.getEventType();
        CoordinatesDbHelper dbHelper = new CoordinatesDbHelper(this.context);
        registeredBounds = dbHelper.getBounds(dbHelper.getWritableDatabase());
        //parser.require(XmlPullParser.START_TAG, ns, "osm");
        // As tags que serão reconhecidas pelo parser são: node, way, nd, tag. Destas, apenas
        // node e way estão diretamente disponíveis na raíz, então apenas o reconhecimento
        // destas deve ser implementado neste momento.
        while(eventType != XmlPullParser.END_DOCUMENT){
            if(newBounds != null && registeredBounds != null &&
                    newBounds._maxLat == registeredBounds._maxLat &&
                    newBounds._minLat == registeredBounds._minLat &&
                    newBounds._maxLng == registeredBounds._maxLng &&
                    newBounds._minLng == registeredBounds._minLng){

                eventType = XmlPullParser.END_DOCUMENT;
            }
            else{
                switch (eventType){
                    case XmlPullParser.START_TAG:
                        String tagName = parser.getName();
                        switch (tagName) {
                            case "bounds":
                                parseBounds(parser);
                                break;
                            case "node":
                                Log.d(TAG, "readXml: NODE encontrado");
                                getNodeLatLng().add(readNode(parser));
                                break;
                            case "way":
                                Log.d(TAG, "readXml: WAY encontrado");
                                getNodeWay().add(readWay(parser));
                                break;
                        }
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }
        }
        // Temos as duas listas, agora é preciso inserí-las na tabela. Ambas as inserções devem
        // verificar se o nodeId já existe antes de inserir o novo conjunto de dados.
        dbHelper.close();
    }

    private void parseBounds(XmlPullParser parser) {
        double _maxLat = -1;
        double _minLat = -1;
        double _maxLng = -1;
        double _minLng = -1;

        String tag = parser.getName();
        if (tag.equals("bounds")){
            _maxLat = Double.parseDouble(parser.getAttributeValue(null, "maxlat"));
            _minLat = Double.parseDouble(parser.getAttributeValue(null, "minlat"));
            _maxLng = Double.parseDouble(parser.getAttributeValue(null, "maxlon"));
            _minLng = Double.parseDouble(parser.getAttributeValue(null, "minlon"));

            newBounds = new Bounds(_maxLat, _maxLng,_minLat, _minLng);
        }

    }

    private Entry readNode(XmlPullParser parser) throws XmlPullParserException, IOException{
        double _nodeId = -1;
        double _nodeLat = -1;
        double _nodeLng = -1;
        String _wayName = "";

        parser.require(XmlPullParser.START_TAG, ns, "node");
        String tag = parser.getName();
        if(tag.equals("node")){
            _nodeId = Double.parseDouble(parser.getAttributeValue(null, "id"));
            _nodeLat = Double.parseDouble(parser.getAttributeValue(null, "lat"));
            _nodeLng = Double.parseDouble(parser.getAttributeValue(null, "lon"));
        }
        parser.nextTag();
        while(parser.getEventType() != XmlPullParser.END_TAG || !parser.getName().equals("node")){
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, "node");
        return new Entry(_nodeId, _nodeLat, _nodeLng, _wayName);
    }

    private List readWay(XmlPullParser parser) throws IOException, XmlPullParserException {
        double _nodeId = -1;
        double _nodeLat = -1;
        double _nodeLng = -1;
        String _wayName = "";
        List<Entry> nodeWay = new ArrayList();

        // A tag way possui tags filhas: nd e tag. Estas filhas devem ser reconhecidas e manipuladas
        // corretamente.
        parser.require(XmlPullParser.START_TAG, ns, "way");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String childName = parser.getName();
            if (childName.equals("nd")) {
                parser.require(XmlPullParser.START_TAG, ns, "nd");
                _nodeId = Double.parseDouble(parser.getAttributeValue(null, "ref"));
                nodeWay.add(new Entry(_nodeId, _nodeLat, _nodeLng, _wayName));
            } else if (childName.equals("tag")){
                parser.require(XmlPullParser.START_TAG, ns, "tag");
                if(parser.getAttributeValue(null, "k").equals("name")){
                    _wayName = parser.getAttributeValue(null, "v");
                }
            } else {
                skip(parser);
            }
        }

        for(Entry entry : nodeWay){
            entry.way_name = _wayName;
        }

        return nodeWay;
    }

    private void skip(XmlPullParser parser) {
    }

}
