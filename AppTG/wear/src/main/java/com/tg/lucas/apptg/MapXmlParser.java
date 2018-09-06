package com.tg.lucas.apptg;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MapXmlParser {

    private static final String ns = null;

    public static class Entry{
        public final int node_id;
        public final double node_lat;
        public final double node_lng;
        public String way_name;

        private Entry(int _nodeId, double _nodeLat, double _nodeLng, String _wayName){
            this.node_id = _nodeId;
            this.node_lat = _nodeLat;
            this.node_lng = _nodeLng;
            this.way_name = _wayName;
        }

    }

    public void parse(InputStream in) throws XmlPullParserException, IOException{
        try{
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            readXml(parser);
        }finally {
            in.close();
        }
    }

    private void readXml(XmlPullParser parser) throws XmlPullParserException, IOException{
        List nodeLatLng = new ArrayList();
        List nodeWay = new ArrayList();

        parser.require(XmlPullParser.START_TAG, ns, "osm");
        while(parser.next() != XmlPullParser.END_TAG){
            if(parser.getEventType() != XmlPullParser.START_TAG){
                continue;
            }
            String tagName = parser.getName();
            // As tags que serão reconhecidas pelo parser são: node, way, nd, tag. Destas, apenas
            // node e way estão diretamente disponíveis na raíz, então apenas o reconhecimento
            // destas deve ser implementado neste momento.
            if(tagName.equals("node")){
                nodeLatLng.add(readNode(parser));
            }else if(tagName.equals("way")){
                readWay(parser);
            }else{
                skip(parser);
            }
        }

        // Temos as duas listas, agora é preciso inserí-las na tabela. Ambas as inserções devem
        // verificar se já existe o nodeId já existe antes de inserir o novo conjunto de dados.

    }

    private Entry readNode(XmlPullParser parser) throws XmlPullParserException, IOException{
        int _nodeId = Integer.parseInt(null);
        double _nodeLat = Double.parseDouble(null);
        double _nodeLng = Double.parseDouble(null);
        String _wayName = null;

        parser.require(XmlPullParser.START_TAG, ns, "node");
        String tag = parser.getName();
        if(tag.equals("node")){
            _nodeId = Integer.parseInt(parser.getAttributeValue(null, "id"));
            _nodeLat = Double.parseDouble(parser.getAttributeValue(null, "lat"));
            _nodeLng = Double.parseDouble(parser.getAttributeValue(null, "lon"));
        }
        parser.require(XmlPullParser.END_TAG, ns, "node");

        return new Entry(_nodeId, _nodeLat, _nodeLng, _wayName);
    }

    private List readWay(XmlPullParser parser) throws IOException, XmlPullParserException {
        int _nodeId = Integer.parseInt(null);
        double _nodeLat = Double.parseDouble(null);
        double _nodeLng = Double.parseDouble(null);
        String _wayName = null;
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
                _nodeId = Integer.parseInt(parser.getAttributeValue(null, "ref"));
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
