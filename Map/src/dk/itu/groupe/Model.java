package dk.itu.groupe;

import dk.itu.groupe.data.*;
import dk.itu.groupe.pathfinding.*;
import dk.itu.groupe.loading.*;
import dk.itu.groupe.util.*;
import java.awt.Point;
import java.awt.geom.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * The model contains all the information about the map.
 *
 * It includes methods to change what part of the map to look at, and
 * zoom-algorithms as well.
 *
 * @author Peter Bindslev (plil@itu.dk), Rune Henriksen (ruju@itu.dk) &amp;
 * Mikael Jepsen (mlin@itu.dk)
 */
public class Model extends Observable
{

    private final double lowestX_COORD, highestX_COORD, lowestY_COORD, highestY_COORD;
    private final double minFactor = 0.5;
    private final int maxNodes;
    private final Graph g;
    private final Loader loader;
    private final Map<CommonRoadType, KDTree> treeMap;
    private final String dir;

    private boolean reset, pathByDriveTime;
    private double leftX, bottomY, rightX, topY, factor, ratioX, ratioY, initialFactor;
    private int from, to, screenWidth, screenHeight;
    private MouseTool mouseTool;
    private Node[] nodeMap;
    private Point2D pressed, dragged, moved;
    private ShortestPath shortestPath;
    private String roadname;
    private boolean sourceChanged;

    /**
     * On creation of the Model, it will start to load in the data.
     *
     * This takes around 10 seconds on a decent computer. After loading it will
     * create the 2DTree structures for every roadtype in the dataset.
     *
     * @param data
     */
    public Model(String data)
    {
        from = -1;
        to = -1;
        // Sets the directory depending on the user selection (OSM vs Krak).
        if (data.equals("OpenStreetMap")) {
            dir = "./res/data/osm/";
        } else {
            dir = "./res/data/krak/";
        }
        Loader.Info info = Loader.loadInfo(dir);

        // Defines the edges of the dataset
        lowestX_COORD = info.xLow;
        lowestY_COORD = info.yLow;
        highestX_COORD = info.xHigh;
        highestY_COORD = info.yHigh;
        maxNodes = info.maxNodes;
        mouseTool = MouseTool.MOVE;
        treeMap = new HashMap<>();
        loader = new Loader();
        g = new Graph(maxNodes);
        // Sets pathfinding to use fastest path as default.
        pathByDriveTime = true;
    }

    /**
     * Loads the coasline from datafiles.
     *
     * Creates the structures responsible for the KD-Tree containing coastline.
     * Also initializes the view.
     */
    public void loadCoastline()
    {
        LinkedList<Edge> edges = loader.loadCoastline("./res/data/coastline/");
        treeMap.put(CommonRoadType.COASTLINE, new KDTree(edges, lowestX_COORD, lowestY_COORD, highestX_COORD, highestY_COORD));

        screenHeight = java.awt.Toolkit.getDefaultToolkit().getScreenSize().height - 110;
        screenWidth = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;

        reset();
        initialFactor = factor;
    }

    /**
     * Loads the nodes from datafiles.
     */
    public void loadNodes()
    {
        nodeMap = loader.loadNodes(dir + "nodes.bin", maxNodes);
    }

    /**
     * Loads the specified roadtype. When the loading is done, the building of
     * structures is submitted to a threadpool to speed up loading.
     *
     * @param rt The specified roadtype.
     * @param es The thread pool to use for faster building of structures.
     */
    public void loadRoadType(final CommonRoadType rt, ExecutorService es)
    {
        final LinkedList<Edge> edgeList = loader.loadEdges(rt, dir, nodeMap);
        es.execute(new Runnable()
        {

            @Override
            public void run()
            {
                if (rt != CommonRoadType.PEDESTRIAN) {
                    for (Edge edge : edgeList) {
                        g.addEdge(edge);
                    }
                }
                if (!edgeList.isEmpty()) {
                    treeMap.put(rt, new KDTree(edgeList, lowestX_COORD, lowestY_COORD, highestX_COORD, highestY_COORD));
                }
            }
        });
    }

    /**
     * Sets the maps coordinates to the initial value (Show all Denmark).
     */
    public final void reset()
    {
        bottomY = lowestY_COORD / 1.001;
        topY = highestY_COORD * 1.001;

        // These paddings make sure that the screen will center the map.
        double xPadding = ((((topY - bottomY) / screenHeight) * screenWidth) - (highestX_COORD - lowestX_COORD)) / 2;
        double yPadding = ((((rightX - leftX) / screenWidth) * screenHeight) - (highestY_COORD - lowestY_COORD)) / 2;

        if (xPadding > 0) {
            leftX = lowestX_COORD - xPadding;
            rightX = highestX_COORD + xPadding;
        }

        if (yPadding > 0) {
            bottomY = lowestY_COORD - yPadding;
            topY = highestY_COORD + yPadding;
        }
        reset = true;
        calculateFactor();
        setChanged();
    }

    /**
     * Moves the map <code>distance</code> pixel towards the bottom, to get the
     * feeling that we look at a higher point on the map.
     *
     * @param distance The distance to move the map in meters.
     */
    public void goUp(double distance)
    {
        reset = false;
        moveVertical(distance);
        setChanged();
    }

    /**
     * Moves the map <code>distance</code> pixel towards the right side, to get
     * the feeling that we look at a point further to the left on the map.
     *
     * @param distance The distance to move the map in meters.
     */
    public void goLeft(double distance)
    {
        reset = false;
        moveHorizontal(-distance);
        setChanged();
    }

    /**
     * Moves the map <code>distance</code> pixel towards the left side, to get
     * the feeling that we look at a point further to the right on the map.
     *
     * @param distance The distance to move the map in meters.
     */
    public void goRight(double distance)
    {
        reset = false;
        moveHorizontal(distance);
        setChanged();
    }

    /**
     * Moves the map <code>distance</code> pixel towards the top, to get the
     * feeling that we look at a lower point on the map.
     *
     * @param distance The distance to move the map in meters.
     */
    public void goDown(double distance)
    {
        reset = false;
        moveVertical(-distance);
        setChanged();
    }

    /**
     * Moves the map x pixels horizontally and y meters vertically.
     *
     * @param x The amount in meters to move the map in horizontal direction.
     * @param y The amount in meters to move the map in vertical direction.
     */
    public void moveMap(double x, double y)
    {
        reset = false;
        moveHorizontal(x);
        moveVertical(-y);
        setChanged();
    }

    /**
     * Zooms in the map, and centers to the center point of the current view.
     */
    public void zoomIn()
    {
        // Checks that the calculated factor is not smaller than the minimum factor
        // to prevent zooming further in than a certain zoom level
        if (factor > minFactor) {
            reset = false;
            double x = (rightX + leftX) / 2;
            double y = (topY + bottomY) / 2;
            leftX = leftX + (30 * ratioX);
            rightX = rightX - (30 * ratioX);
            topY = topY - (30 * ratioY);
            bottomY = (topY - (rightX - leftX) / ((double) screenWidth / (double) screenHeight));
            center(x, y);
            calculateFactor();
            setChanged();
        }
    }

    /**
     * Zooms out the map, and centers to the center point of the current view.
     */
    public void zoomOut()
    {
        // Checks that the calculated factor is not greater than the initial factor
        // to prevent zooming further out than the original zoom level
        if (factor < initialFactor) {
            reset = false;
            double x = (rightX + leftX) / 2;
            double y = (topY + bottomY) / 2;
            leftX = leftX - (30 * ratioX);
            rightX = rightX + (30 * ratioX);
            topY = topY + (30 * ratioY);
            bottomY = (topY - (rightX - leftX) / ((double) screenWidth / (double) screenHeight));
            center(x, y);
            calculateFactor();
            setChanged();
        }
    }

    /**
     * Zooms in on the map, and keeps the point specified at the same place on
     * the map after zooming.
     *
     * Google like zooming, so the mouse always point on the same thing on the
     * map.
     *
     * @param x The screen-x-coordinate for the mouse-pointer.
     * @param y The screen-y-coordinate for the mouse-pointer.
     */
    public void zoomInScroll(int x, int y)
    {
        // Checks that the calculated factor is not smaller than the minimum factor
        // to prevent zooming further in than a certain zoom level
        if (factor > minFactor) {
            reset = false;
            // Map coordinates before zoom
            Point2D p = translatePoint(x, y);
            zoomIn();
            // Map coordinates after zoom
            Point2D p1 = translatePoint(x, y);

            // Restore the previous map-coordinates to (x, y)
            moveHorizontal(p.getX() - p1.getX());
            moveVertical(p.getY() - p1.getY());

            setChanged();
        }
    }

    /**
     * Zooms out on the map, and keeps the point specified at the same place on
     * the map after zooming.
     *
     * Google like zooming, so the mouse always point on the same thing on the
     * map.
     *
     * @param x The screen-x-coordinate for the mouse-pointer.
     * @param y The screen-y-coordinate for the mouse-pointer.
     */
    public void zoomOutScroll(int x, int y)
    {
        // Checks that the calculated factor is not larger than the initial factor
        // to prevent zooming further out than the original zoom level    
        if (factor < initialFactor) {
            reset = false;
            // Map coordinates before zoom
            Point2D p = translatePoint(x, y);
            zoomOut();
            // Map coordinates after zoom
            Point2D p1 = translatePoint(x, y);

            // Restore the previous map-coordinates to (x, y)
            moveHorizontal(p.getX() - p1.getX());
            moveVertical(p.getY() - p1.getY());

            setChanged();
        }
    }

    /**
     * This zoom-method zooms in to the specified rectangle.
     *
     * If the rectangle doesn't match the ratio between screen width and
     * screenHeight, the right or bottom side will be moved to fit.
     *
     * @param xLeft Map coordinate for the left side of the rectangle.
     * @param yTop Screen coordinate for the top side of the rectangle.
     * @param xRight Screen coordinate for the right side of the rectangle.
     * @param yBottom Screen coordinate for the bottom side of the rectangle.
     */
    public void zoomRect(double xLeft, double yTop, double xRight, double yBottom)
    {
        // Checks that the calculated factor is not smaller than the minimum factor
        // to prevent zooming further in than a certain zoom level
        if (factor > minFactor) {
            reset = false;

            double x2 = xRight, x1 = xLeft;
            double y2 = yBottom, y1 = yTop;

            if (x1 > x2) {
                double tmp = x1;
                x1 = x2;
                x2 = tmp;
            }
            if (y2 > y1) {
                double tmp = y1;
                y1 = y2;
                y2 = tmp;
            }
            double ratio = (double) screenWidth / (double) screenHeight;
            leftX = x1;
            topY = y1;
            if (Math.abs(x2 - x1) / screenWidth > Math.abs(y1 - y2) / screenHeight) {
                rightX = x2;
                bottomY = (topY - (rightX - leftX) / ratio);
            } else {
                bottomY = y2;
                rightX = leftX + (topY - bottomY) * ratio;
            }
            calculateFactor();
            setChanged();
        }
    }

    /**
     * Updates the field roadname to correspond with the road nearest to the
     * mouse pointer.
     *
     * @param x The on-screen x-coordinate of the mouse.
     * @param y The on-screen y-coordinate of the mouse.
     */
    public void updateRoadname(double x, double y)
    {
        Edge near = nearest(new Point2D.Double(x, y), true);
        // If there are no "nearest" edges
        if (near != null) {
            roadname = near.getRoadname() + " ";
        } else {
            roadname = " ";
        }
        setChanged();
    }

    /**
     * Sets the mouse click/drag-action.
     *
     * As of now this is either drag-to-zoom, or drag-to-move.
     *
     * @param mouseTool The mouse function.
     */
    public void setMouseTool(MouseTool mouseTool)
    {
        this.mouseTool = mouseTool;
        setChanged();
    }

    /**
     * Returns the current mouse function.
     *
     * @return the current mouse function which is either drag-to-zoom or
     * drag-to-move.
     */
    public MouseTool getMouseTool()
    {
        return mouseTool;
    }

    /**
     * Returns the current width used for calculating the view of the map.
     *
     * @return the current width.
     */
    public int getScreenWidth()
    {
        return screenWidth;
    }

    /**
     * Returns the current screenHeight used for calculating the view of the
     * map.
     *
     * @return the current screenHeight.
     */
    public int getScreenHeight()
    {
        return screenHeight;
    }

    /**
     * Sets the size of the map.
     *
     * This should be used whenever the view changes size, so the model can
     * return the correct data.
     *
     * @param width The new width.
     * @param height The new screenHeight.
     */
    public void setSize(int width, int height)
    {
        this.screenWidth = width;
        this.screenHeight = height;
        if (reset) {
            reset();
        }
        calculateFactor();
        setChanged();
    }

    /**
     * Returns the edges of roadtype <code>rt</code> and within the specified
     * rectangle-coordinates.
     *
     * @param rt The roadtype of interest.
     * @param xLeft The left x-coordinate.
     * @param yBottom The bottom y-coordinate.
     * @param xRight The right x-coordinate.
     * @param yTop The top y-coordinate.
     * @return A list of edges, containing the edges of roadtype <code>rt</code>
     * within the specified rectangle.
     */
    @SuppressWarnings("unchecked")
    public Set<Edge> getEdges(CommonRoadType rt, double xLeft, double yBottom, double xRight, double yTop)
    {
        if (treeMap.get(rt) != null) {
            return treeMap.get(rt).getEdges(xLeft, yBottom, xRight, yTop); //(xLeft, yBottom, xRight, yTop);
        } else {
            return new HashSet<>();
        }
    }

    /**
     *
     * @return A Point(x,y) containing the left and top coordinates.
     */
    public Point.Double getLeftTop()
    {
        return new Point.Double(leftX, topY);
    }

    /**
     *
     * @return A point (x,y) containing the right and bottom coordinates.
     */
    public Point.Double getRightBottom()
    {
        return new Point.Double(rightX, bottomY);
    }

    /**
     * Returns the current factor between the map-size and the on-screen
     * map-size.
     *
     * @return The factor used to draw the map in the right
     */
    public double getFactor()
    {
        return factor;
    }

    /**
     * Sets the point of last time the mousebutton was pressed.
     *
     * @param e The onscreen point where the mousebutton was pressed.
     */
    public void setPressed(Point e)
    {
        pressed = (e != null) ? translatePoint(e.x, e.y) : null;
        setChanged();
    }

    /**
     * Sets the point of where the mouse was last dragged to.
     *
     * @param e The onscreen point.
     */
    public void setDragged(Point e)
    {
        dragged = (e != null) ? translatePoint(e.x, e.y) : null;
        setChanged();
    }

    /**
     * Sets the point of where the mouse was last moved (but not dragged).
     *
     * @param e The onscreen point.
     */
    public void setMoved(Point e)
    {
        moved = (e != null) ? translatePoint(e.x, e.y) : null;
        setChanged();
    }

    /**
     * Changes the way the path should be calculated.
     *
     * @param b Boolean reflecting whether the path should be calculated by time
     * (true) or distance (false).
     */
    public void setPathByDriveTime(boolean b)
    {
        pathByDriveTime = b;
        setChanged();
    }

    /**
     * States whether the current path is calculated by time or distance.
     *
     * @return True if path is calculated by time, false otherwise.
     */
    public boolean getPathByDriveTime()
    {
        return pathByDriveTime;
    }

    /**
     * States whether the two endpoints of the routing is set or not.
     *
     * @return true if both points are set, false otherwise.
     */
    public boolean pathPointsSet()
    {
        return from >= 0 && to >= 0;
    }

    /**
     * Returns the node that should be used as first point in routing.
     *
     * @return the node that should be used as first point in routing.
     */
    public Node fromPoint()
    {
        return from >= 0 ? nodeMap[from] : null;
    }

    /**
     * Returns the node that should be used as last point in routing.
     *
     * @return the node that should be used as last point in routing.
     */
    public Node toPoint()
    {
        return to >= 0 ? nodeMap[to] : null;
    }

    /**
     * Resets both points, and the last calculated route.
     */
    public void resetPointSet()
    {
        shortestPath = null;
        from = -1;
        to = -1;
        setChanged();
    }

    /**
     * Sets the fromnode to the node nearest to the on-map coordinates specified
     * by e.
     *
     * @param e On map coordinates.
     * @throws NoPathFoundException If no point is within a distance specified
     * by the kd-tree.
     */
    public void setFromNode(Point2D e) throws NoPathFoundException
    {
        Edge near = nearest(e, false);
        if (near == null) {
            throw new NoPathFoundException("No nearest point was found");
        }
        Node nodeFrom = near.from();
        Node nodeTo = near.to();

        if (new Point.Double(nodeFrom.x(), nodeFrom.y()).distance(e)
                < new Point.Double(nodeTo.x(), nodeTo.y()).distance(e)) {
            from = nodeFrom.id();
        } else {
            from = nodeTo.id();
        }
        sourceChanged = true;
        setChanged();
    }

    /**
     * Sets the to node to the node nearest to the on-map coordinates specified
     * by e.
     *
     * @param e On map coordinates.
     * @throws NoPathFoundException If no point is within a distance specified
     * by the kd-tree.
     */
    public void setToNode(Point2D e) throws NoPathFoundException
    {
        Edge near = nearest(e, false);
        if (near == null) {
            throw new NoPathFoundException("No nearest point was found");
        }
        Node nodeFrom = near.from();
        Node nodeTo = near.to();

        if (new Point.Double(nodeFrom.x(), nodeFrom.y()).distance(e) < new Point.Double(nodeTo.x(), nodeTo.y()).distance(e)) {
            this.to = nodeFrom.id();
        } else {
            this.to = nodeTo.id();
        }
        setChanged();
    }

    /**
     * Returns the path if one is found.
     *
     * @return The path if one is found.
     * @throws NoPathFoundException If no path is found.
     */
    public Stack<Edge> getPath() throws NoPathFoundException
    {
        if (!sourceChanged && shortestPath.pathByDriveTime() == pathByDriveTime) {
            if (shortestPath.hasPathTo(to)) {
                setChanged();
                return shortestPath.pathTo(to);
            }
        }
        shortestPath = new ShortestPath(g, from, to, pathByDriveTime, nodeMap);
        sourceChanged = false;
        if (shortestPath.hasPathTo(to)) {
            setChanged();
            return shortestPath.pathTo(to);
        }
        setChanged();
        throw new NoPathFoundException("No path was found");
    }

    /**
     * @return The on-map point of where the mouse was last dragged.
     */
    public Point2D getDragged()
    {
        return dragged;
    }

    /**
     * @return The on-map point of where the mouse was last pressed.
     */
    public Point2D getPressed()
    {
        return pressed;
    }

    /**
     * @return The on-map point of where the mouse was last dragged.
     */
    public Point2D getMoved()
    {
        return moved;
    }

    /**
     *
     * @return The nearest roadname.
     */
    public String getRoadname()
    {
        return roadname;
    }

    /**
     * Calculates the factor that is used to calculate where the roads should be
     * drawn.
     */
    private void calculateFactor()
    {
        // Ensures that zoom retains the correct ratio between width and screenHeight.
        ratioX = (rightX - leftX) / screenWidth;
        ratioY = (topY - bottomY) / screenHeight;
        // This factor determines how big the Map will be drawn.
        factor = Math.max(ratioX, ratioY);
        assert (factor != 0);
    }

    /**
     * Centers the screen on the map-coordinates supplied.
     *
     * @param x
     * @param y
     */
    private void center(double x, double y)
    {
        double currentCenterX = (rightX + leftX) / 2;
        double currentCenterY = (topY + bottomY) / 2;

        moveHorizontal(x - currentCenterX);
        moveVertical(y - currentCenterY);
    }

    /**
     * Moves the map horizontally.
     *
     * @param distance The "on map"-distance to move the map.
     */
    private void moveHorizontal(double distance)
    {
        double centerX = (rightX + leftX) / 2;
        if (distance > 0 && centerX < highestX_COORD || distance < 0 && centerX > lowestX_COORD) {
            leftX += distance;
            rightX += distance;
        }
    }

    /**
     * Moves the map vertically.
     *
     * @param distance The "on map"-distance to move the map.
     */
    private void moveVertical(double distance)
    {
        double centerY = (topY + bottomY) / 2;
        if (distance > 0 && centerY < highestY_COORD || distance < 0 && centerY > lowestY_COORD) {
            bottomY += distance;
            topY += distance;
        }
    }

    /**
     * Finds the nearest edge given a point.
     *
     * @param p the point to get nearest edge from
     * @param factorAware
     * @return
     */
    private Edge nearest(Point2D p, boolean factorAware)
    {
        LinkedList<Edge> edges = new LinkedList<>();
        for (CommonRoadType rt : CommonRoadType.values()) {
            if (rt == CommonRoadType.PLACES || rt == CommonRoadType.COASTLINE) {
                continue;
            }
            if ((!factorAware || rt.isEnabled(factor)) && treeMap.get(rt) != null) {
                Edge e = treeMap.get(rt).getNearest(p.getX(), p.getY());
                if (e != null) {
                    edges.add(e);
                }
            }
        }

        Edge near = null;
        double dist = Double.MAX_VALUE;
        for (Edge edge : edges) {
            Point2D start = null;
            Point2D last = null;
            for (PathIterator pi = edge.getShape().getPathIterator(null); !pi.isDone(); pi.next()) {
                double[] coords = new double[6];
                int type = pi.currentSegment(coords);
                switch (type) {
                    case PathIterator.SEG_MOVETO:
                        start = last = new Point2D.Double(coords[0], coords[1]);
                        break;
                    case PathIterator.SEG_LINETO:
                        Point2D.Double pd = new Point2D.Double(coords[0], coords[1]);
                        Line2D line = new Line2D.Double(last, pd);
                        last = pd;
                        double d = line.ptSegDist(p);
                        if (d < dist) {
                            dist = d;
                            near = edge;
                        }
                        break;
                    case PathIterator.SEG_CLOSE:
                        line = new Line2D.Double(last, start);
                        d = line.ptSegDist(p);
                        if (d < dist) {
                            dist = d;
                            near = edge;
                        }
                        break;
                }
            }
        }
        return near;
    }

    /**
     * Translates screen-coordinates into map-coordinates.
     *
     * @param x On screen-x-coordinate.
     * @param y On-screen-y-coordinate.
     * @return The on-map point representation of the supplied screen-point.
     */
    public Point2D translatePoint(int x, int y)
    {
        double xMap = x * factor + leftX;
        double yMap = (screenHeight - y) * factor + bottomY;
        return new Point2D.Double(xMap, yMap);
    }
}
