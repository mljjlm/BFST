package dk.itu.groupe.parsing.krak;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JOptionPane;


/**
 *
 * @author Mikael
 */
public class KrakParser
{
    private static Set<Integer> usedNodes;
    private static List<EdgeData> edges;

    public static void main(String[] args)
    {
        String dir = "./res/data/";
        usedNodes = new TreeSet<>();
        edges = new LinkedList<>();
        final Map<Integer, NodeData> nodeMap = new HashMap<>();
        KrakLoader loader = new KrakLoader()
        {

            @Override
            public void processNode(NodeData nd)
            {
                nodeMap.put(nd.ID, nd);
            }

            @Override
            public void processEdge(EdgeData ed)
            {
                edges.add(ed);
            }
        };
        
        try {
            loader.load(dir + "kdv_node_unload.txt", dir + "kdv_unload.txt", nodeMap);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    null,
                    "An unexpected error has occured.\nThis program will exit.",
                    "Error loading",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace(System.err);
            System.exit(300);
        }
        
        try {
            double xMin, xMax, yMin, yMax;
            xMin = yMin = Double.MAX_VALUE;
            xMax = yMax = Double.MIN_VALUE;
            PrintWriter nodeStream = new PrintWriter(dir + "krak/nodes.csv");
            nodeStream.println("id,x,y");
            PrintWriter edgeStream = new PrintWriter(dir + "krak/edges.csv");
            edgeStream.println("FNODE,TNODE,LENGTH,DAV_DK,TYPE,VEJNAVN,FRAKOERSEL,SPEED,DRIVETIME,ONE_WAY");
            for (EdgeData ed : edges) {
                usedNodes.add(ed.FNODE);
                usedNodes.add(ed.TNODE);
                edgeStream.println(ed.toString());
            }
            for (Integer i : usedNodes) {
                NodeData n = nodeMap.get(i);
                if (n.X_COORD < xMin) xMin = n.X_COORD;
                else if (n.X_COORD > xMax) xMax = n.X_COORD;
                if (n.Y_COORD < yMin) yMin = n.Y_COORD;
                else if (n.Y_COORD > yMax) yMax = n.Y_COORD;
                nodeStream.println(n.toString());
            }
            PrintWriter info = new PrintWriter(dir + "krak/info.csv");
            info.println(xMin);
            info.println(yMin);
            info.println(xMax);
            info.println(yMax);
            info.println(usedNodes.size());
            info.println(edges.size());
            
            info.close();
            nodeStream.close();
            edgeStream.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace(System.err);
        }
    }
}
