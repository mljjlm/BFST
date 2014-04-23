package dk.itu.groupe.parsing.osm;

import java.awt.geom.Point2D;
import java.util.Locale;

/**
 * An object storing the raw node data from the parsed krak data file.
 *
 * @author Peter Bindslev (plil@itu.dk), Rune Henriksen (ruju@itu.dk) & Mikael
 * Jepsen (mlin@itu.dk)
 */
public class Node
{

    private final long id;
    private final double x;
    private final double y;

    public Node(long id, double x, double y)
    {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public long getId()
    {
        return id;
    }

    public Point2D.Double getPoint()
    {
        return new Point2D.Double(x, y);
    }

    /**
     * Returns a string representing the node data in the same format as used in
     * the nodes.csv file.
     *
     * @return
     */
    @Override
    public String toString()
    {
        return id + ","
                + String.format(Locale.ENGLISH, "%.2f,", x)
                + String.format(Locale.ENGLISH, "%.2f", y);
    }
}