package dk.itu.groupe;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Peter Bindslev (plil@itu.dk), Rune Henriksen (ruju@itu.dk) & Mikael
 * Jepsen (mlin@itu.dk)
 */
public class KDTree
{

    enum Dimension
    {

        X, Y
    };
    @SuppressWarnings("unchecked")
    private final List<Edge> empty = (List<Edge>) Collections.EMPTY_LIST;
    private KDTree HIGH, LOW;
    private final Edge centerEdge;
    private final double xmin, ymin, xmax, ymax;
    private final Dimension dim;

    /**
     * Creates the KDTree-structure until there are no more elements.
     *
     * @param edges A list of edges that should be spread out on the tree.
     * @param xLeft The left x-coordinate.
     * @param yBottom The bottom y-coordinate.
     * @param xRight The right x-coordinate.
     * @param yTop The top y-coordinate.
     */
    public KDTree(List<Edge> edges, double xLeft, double yBottom, double xRight, double yTop)
    {
        xmin = xLeft;
        ymin = yBottom;
        xmax = xRight;
        ymax = yTop;

        int size = edges.size();

        List<Edge> low = new LinkedList<>(), high = new LinkedList<>();
        centerEdge = edges.remove(edges.size() / 2);
        if (ymax - ymin < xmax - xmin) {
            // Delta x is gratest.
            dim = Dimension.X;
            // Put the right elements where it belongs.
            while (!edges.isEmpty()) {
                Edge edge = edges.remove(0);
                if (edge.getLine().getX1() < centerEdge.getLine().getX1()) {
                    low.add(edge);
                } else {
                    high.add(edge);
                }
            }
        } else {
            // Delta y is the same size or greater.
            dim = Dimension.Y;
            // Put the right elements where it belongs.
            while (!edges.isEmpty()) {
                Edge edge = edges.remove(0);
                if (edge.getLine().getY1() < centerEdge.getLine().getY1()) {
                    low.add(edge);
                } else {
                    high.add(edge);
                }
            }
        }
        assert (edges.isEmpty());
        assert (size == high.size() + 1 + low.size());
        double[] lowBounds = new double[4], highBounds = new double[4];
        if (dim == Dimension.X) {
            lowBounds[0] = xmin;
            lowBounds[1] = ymin;
            lowBounds[2] = centerEdge.getLine().getX1();
            lowBounds[3] = ymax;

            highBounds[0] = centerEdge.getLine().getX1();
            highBounds[1] = ymin;
            highBounds[2] = xmax;
            highBounds[3] = ymax;
        } else {
            lowBounds[0] = xmin;
            lowBounds[1] = ymin;
            lowBounds[2] = xmax;
            lowBounds[3] = centerEdge.getLine().getY1();

            highBounds[0] = xmin;
            highBounds[1] = centerEdge.getLine().getY1();
            highBounds[2] = xmax;
            highBounds[3] = ymax;
        }

        if (!low.isEmpty()) {
            LOW = new KDTree(low, lowBounds[0], lowBounds[1], lowBounds[2], lowBounds[3]);
        }
        if (!high.isEmpty()) {
            HIGH = new KDTree(high, highBounds[0], highBounds[1], highBounds[2], highBounds[3]);
        }

    }

    /**
     * Returns the nearest edge.
     *
     * @param x The x-coordinate to look near.
     * @param y The y-coordinate to look near.
     * @return The edge that are nearest to the coordinates.
     */
    public Edge getNearest(double x, double y)
    {
        List<Edge> ns = getEdges(x, y, x, y);
        double dist = 100;
        Edge nearest = null;
        for (Edge edge : ns) {
            double d = edge.getLine().ptSegDist(x, y);
            if (d < dist) {
                dist = d;
                nearest = edge;
            }
        }
        return nearest;
    }

    /**
     * Returns the edges with the four parameters as a bounding rectangle.
     *
     * It actually adds a little more to the distance.
     *
     * @param leftX The left x-coordinate
     * @param bottomY The bottom y-coordinate
     * @param rightX The right x-coordinate
     * @param topY The top y-coordinate.
     * @return A list of edgedata containing the edges that are inside the
     * rectangle.
     */
    public List<Edge> getEdges(double leftX, double bottomY, double rightX, double topY)
    {
        int offset = 1000;
        if (dim == Dimension.X) {
            if (rightX + offset < xmin || leftX - offset > xmax) {
                return empty;
            }
        } else {
            if (topY + offset < ymin || bottomY - offset > ymax) {
                return empty;
            }
        }

        List<Edge> edgeList;
        if (LOW != null) {
            edgeList = LOW.getEdges(leftX, bottomY, rightX, topY);
            if (edgeList.isEmpty()) {
                edgeList = new LinkedList<>();
            }
            edgeList.add(centerEdge);
            if (HIGH != null) {
                edgeList.addAll(HIGH.getEdges(leftX, bottomY, rightX, topY));
            }
        } else if (HIGH
                != null) {
            edgeList = HIGH.getEdges(leftX, bottomY, rightX, topY);
            if (edgeList.isEmpty()) {
                edgeList = new LinkedList<>();
            }
            edgeList.add(centerEdge);
        } else {
            edgeList = new LinkedList<>();
            edgeList.add(centerEdge);
        }
        return edgeList;
    }
}
