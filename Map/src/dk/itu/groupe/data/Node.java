package dk.itu.groupe.data;

/**
 * An object storing the raw node data from the parsed krak data file.
 *
 * @author Peter Bindslev (plil@itu.dk), Rune Henriksen (ruju@itu.dk) & Mikael
 * Jepsen (mlin@itu.dk)
 */
public class Node
{

    private final int id;
    private final float x;
    private final float y;
    
    public Node(int id, float x, float y)
    {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public int id()
    {
        return id;
    }

    public float x()
    {
        return x;
    }

    public float y()
    {
        return y;
    }
}
