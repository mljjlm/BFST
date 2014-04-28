package dk.itu.groupe;

import dk.itu.groupe.loading.DataLine;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the raw data from a line in edges.csv.
 *
 * @author Peter Bindslev (plil@itu.dk), Rune Henriksen (ruju@itu.dk) & Mikael
 * Jepsen (mlin@itu.dk)
 */
public class Edge
{

    private static HashMap<Integer, CommonRoadType> rtMap;

    private final long id;
    private final CommonRoadType type;
    private final String roadname;
    private final double length;
    private final int exitNumber;
    private final int speedLimit;
    private final double driveTime;
    private final OneWay oneWay;
    private final Shape path;
    private final Node[] nodes;

    @Override
    public String toString()
    {
        throw new UnsupportedOperationException("Edges should not be printed");
        /*return id + ","
         + type.getTypeNo() + ","
         + "`" + roadname + "`,"
         + exitNumber + ","
         + speedLimit + ","
         + oneWay.getNumber();*/
    }

    protected Edge(Node[] nodes)
    {
        id = 0;
        type = null;
        roadname = null;
        length = 0;
        exitNumber = 0;
        speedLimit = 0;
        driveTime = 0;
        oneWay = null;
        Path2D p = new Path2D.Double();
        p.moveTo(nodes[0].X_COORD, nodes[0].Y_COORD);
        for (int i = 1; i < nodes.length; i++) {
            p.lineTo(nodes[i].X_COORD, nodes[i].Y_COORD);
        }
        p.closePath();
        path = new Area(p);
        this.nodes = nodes;
    }

    public Edge(String line, Map<Long, Node> nodeMap)
    {
        if (rtMap == null) {
            rtMap = new HashMap<>();
            for (CommonRoadType rt : CommonRoadType.values()) {
                rtMap.put(rt.getTypeNo(), rt);
            }
        }
        DataLine dl = new DataLine(line);
        id = dl.getLong();
        int typ = dl.getInt();
        type = rtMap.get(typ);
        if (type == null) {
            System.err.println(typ);
            assert (type != null);
        }
        roadname = dl.getString();
        length = dl.getDouble();
        exitNumber = dl.getInt();
        speedLimit = dl.getInt();
        driveTime = dl.getDouble();
        switch (dl.getInt()) {
            case -1:
                oneWay = OneWay.TO_FROM;
                break;
            case 0:
                oneWay = OneWay.NO;
                break;
            case 1:
                oneWay = OneWay.FROM_TO;
                break;
            default:
                oneWay = OneWay.NO;
                System.err.println("Assuming no restrictions on edge.");
        }
        List<Node> nodeList = new ArrayList<>();
        while (dl.hasNext()) {
            nodeList.add(nodeMap.get(dl.getLong()));
        }
        this.nodes = nodeList.toArray(new Node[0]);
        if (nodeList.size() > 1) {
            Path2D p = new Path2D.Double();
            p.moveTo(nodeList.get(0).X_COORD, nodeList.get(0).Y_COORD);
            for (int i = 1; i < nodeList.size(); i++) {
                p.lineTo(nodeList.get(i).X_COORD, nodeList.get(i).Y_COORD);
            }
            path = p;
        } else {
            Path2D p = new Path2D.Double();
            p.moveTo(nodeList.get(0).X_COORD, nodeList.get(0).Y_COORD);
            p.lineTo(nodeList.get(0).X_COORD, nodeList.get(0).Y_COORD);
            path = p;
        }
    }

    public CommonRoadType getType()
    {
        return type;
    }

    public Shape getShape()
    {
        return path;
    }

    public String getRoadname()
    {
        return roadname;
    }
    
    public Node[] getNodes()
    {
        return nodes;
    }

    public double getLength()
    {
        return length;
    }

    public double getDriveTime()
    {
        return driveTime;
    }
    
    
}
