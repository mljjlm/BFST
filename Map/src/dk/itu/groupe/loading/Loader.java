package dk.itu.groupe.loading;

import dk.itu.groupe.data.*;
import dk.itu.groupe.util.LinkedList;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Parse Krak data files (kdv_node_unload.txt, kdv_unload.txt).
 *
 * Customize to your needs by overriding processNode and processEdge. See
 * example in main.
 *
 * @author Peter Bindslev (plil@itu.dk), Rune Henriksen (ruju@itu.dk) &amp;
 * Mikael Jepsen (mlin@itu.dk).
 *
 */
public class Loader
{

    private final Map<Integer, CommonRoadType> rtMap;
    private final Map<String, String> interner;

    public Loader()
    {
        interner = new HashMap<>();
        rtMap = new HashMap<>();
        for (CommonRoadType rt : CommonRoadType.values()) {
            rtMap.put(rt.getTypeNo(), rt);
        }
    }

    /**
     * Load krak-data from given files, invoking processNode and processEdge
     * once for each node- and edge- specification in the input file,
     * respectively.
     *
     * @param nodeFile The path to the file containing the nodes.
     * @param numberOfNodes The number of nodes that should be loaded (The
     * number of nodes in the file).
     * @return An array containing all the nodes at their rightful place (Sorted
     * by ID).
     */
    public Node[] loadNodes(String nodeFile, int numberOfNodes)
    {
        Node[] nodes = new Node[numberOfNodes];
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(nodeFile)))) {
            int available;
            while ((available = dis.available()) > 0) {
                for (int i = 0; i < available; i += 12) {
                    Node n = new Node(dis.readInt(), dis.readFloat(), dis.readFloat());
                    nodes[n.id()] = n;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        return nodes;
    }

    /**
     *
     *
     * @param rt The roadtypes chosen to be shown.
     * @param edgeDir The directory for edges, differs from OSM to Krak.
     * @param nodeMap An array of type Node containing nodes.
     * @return A LinkedList containing all the edges of this roadtype.
     */
    public LinkedList<Edge> loadEdges(CommonRoadType rt, String edgeDir, final Node[] nodeMap)
    {
        LinkedList<Edge> edges = new LinkedList<>();
        File f = new File(edgeDir + "edges" + rt.getTypeNo() + ".bin");
        if (!f.exists()) {
            return edges;
        }
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            int available;
            while ((available = dis.available()) > 0) {
                for (int i = 0; i < available; i += 22) {
                    CommonRoadType type = rtMap.get(dis.readInt());
                    assert rt == type;
                    String roadname = intern(dis.readUTF());
                    i += roadname.getBytes().length;
                    float length = dis.readFloat();
                    float driveTime = dis.readFloat();
                    OneWay oneWay;
                    switch (dis.readInt()) {
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
                    Node[] nodes = new Node[dis.readInt()];
                    i += 4 * nodes.length;
                    for (int j = 0; j < nodes.length; j++) {
                        nodes[j] = nodeMap[dis.readInt()];
                    }
                    edges.add(new Edge(type, roadname, length, driveTime, oneWay, nodes));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        return edges;
    }

    /**
     * Loads coastline data from given files: info.bin, nodes.bin, edges.bin
     *
     * @param dir The directory for coastline data.
     * @return A LinkedList containing all the coastline edges.
     */
    public LinkedList<Edge> loadCoastline(String dir)
    {
        LinkedList<Edge> edges = new LinkedList<>();
        Node[] coastlinemap = null;
        try (DataInputStream dinfo = new DataInputStream(new BufferedInputStream(new FileInputStream(dir + "info.bin")))) {
            coastlinemap = new Node[dinfo.readInt()];
            dinfo.close();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        assert coastlinemap != null;
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(dir + "nodes.bin")))) {
            for (int i = 0; i < coastlinemap.length; i++) {
                Node n = new Node(dis.readInt(), dis.readFloat(), dis.readFloat());
                coastlinemap[n.id()] = n;
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(dir + "edges.bin")))) {
            int available;
            while ((available = dis.available()) > 0) {
                for (int j = 0; j < available; j += 4) {
                    Node[] nodes = new Node[dis.readInt()];
                    j += 4 * nodes.length;
                    for (int i = 0; i < nodes.length; i++) {
                        nodes[i] = coastlinemap[dis.readInt()];
                    }
                    edges.add(new Edge(nodes));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        return edges;
    }

    public static Info loadInfo(String dir)
    {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(dir + "info.bin"))) {
            return new Info(dis.readDouble(), dis.readDouble(), dis.readDouble(), dis.readDouble(), dis.readInt(), dis.readInt());
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        throw new RuntimeException("Something went terribly wrong!");
    }

    /**
     * The Info class contains information about the nodes and edges which we
     * need in order to draw the map.
     *
     */
    public static class Info
    {

        public final double xLow, yLow, xHigh, yHigh;
        public final int maxNodes, maxEdges;

        public Info(double xLow, double yLow, double xHigh, double yHigh, int maxNodes, int maxEdges)
        {
            this.xLow = xLow;
            this.yLow = yLow;
            this.xHigh = xHigh;
            this.yHigh = yHigh;
            this.maxNodes = maxNodes;
            this.maxEdges = maxEdges;
        }

    }

    private String intern(String s)
    {
        if (!interner.containsKey(s)) {
            interner.put(s, s);
            return s;
        } else {
            return interner.get(s);
        }
    }
}
