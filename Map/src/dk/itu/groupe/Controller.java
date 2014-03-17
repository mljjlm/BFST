package dk.itu.groupe;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import javax.swing.AbstractAction;
import javax.swing.JFrame;

/**
 *
 * @author Peter Bindslev <plil@itu.dk>, Rune Henriksen <ruju@itu.dk> & Mikael
 * Jepsen <mlin@itu.dk>
 */
public class Controller implements 
        MouseListener, 
        MouseMotionListener, 
        MouseWheelListener, 
        ComponentListener, 
        WindowStateListener
{

    private final Model model;
    private final View view;

    public Controller(Model model, View view)
    {
        this.model = model;
        this.view = view;
    }

    @Override
    public void mouseMoved(MouseEvent me)
    {
        model.setMouseMapCoordinates(me.getX(), me.getY());
        model.updateRoadname();
        model.notifyObservers("updateRoadname");
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        model.setMouseMapCoordinates(e.getX(), e.getY());
        Point.Double topLeft = model.getTopLeft(), bottomRight = model.getBottomRight();
        double mapX = model.getMapX(), highX = bottomRight.x, lowX = topLeft.x,
                mapY = model.getMapY(), highY = topLeft.y, lowY = bottomRight.y;
        // Only zoom if the mouse is within the actual map
        if (mapX < highX && mapX > lowX && mapY < highY && mapY > lowY) {
            // calculate the ratio of the distances from the mouse to the edges (up, down, left, right)
            double ls = mapX - lowX;
            double rs = highX - mapX;
            double lsp = ls / (ls + rs);
            double rsp = rs / (ls + rs);
            double ds = mapY - lowY;
            double us = highY - mapY;
            double dsp = ds / (ds + us);
            double usp = us / (ds + us);
            if (e.getWheelRotation() < 0) {
                model.zoomScrollIn(usp, dsp, lsp, rsp);

            } else {
                model.zoomScrollOut(usp, dsp, lsp, rsp);
            }
            model.calculateFactor();
            model.notifyObservers();
        }
    }

    @Override
    public void mouseClicked(MouseEvent me)
    {

    }

    @Override
    public void mouseEntered(MouseEvent me)
    {

    }

    @Override
    public void mousePressed(MouseEvent me)
    {
        //Right click to reset.
        if (me.getButton() == 3) {
            model.reset();
            model.calculateFactor();
            model.notifyObservers();
        } else {
            model.setPressed(me);
            model.setDragged(me);
        }
    }

    @Override
    public void mouseReleased(MouseEvent me)
    {
        if (me.getButton() == 1 && model.getMouse() == MouseTool.ZOOM) {
            model.setReleased(me);
            if (model.getPressed().getX() == model.getReleased().getX() && model.getPressed().getY() == model.getReleased().getY()) {
                model.setReleased(null);
                model.setPressed(null);
                model.setDragged(null);
                return;
            }
            model.setPressed(null);
            model.setReleased(null);
            model.setDragged(null);
            model.zoomRect();
            model.calculateFactor();
            model.notifyObservers();
        }
    }

    @Override
    public void mouseExited(MouseEvent me)
    {

    }

    @Override
    public void mouseDragged(MouseEvent me)
    {
        model.setMouseMapCoordinates(me.getX(), me.getY());
        if (model.getMouse() == MouseTool.MOVE) {
            if (model.getPressed() != null) {
                model.moveMap(model.getDragged().getX() - me.getX(), model.getDragged().getY() - me.getY());
            }
        }
        model.setDragged(me);
        model.notifyObservers();
    }

    @Override
    public void componentResized(ComponentEvent e)
    {
        model.setSize(view.getMap().getSize());
        model.calculateFactor();
        model.notifyObservers();
    }

    @Override
    public void componentMoved(ComponentEvent e)
    {
        model.setSize(view.getMap().getSize());
        model.calculateFactor();
        model.notifyObservers();
    }

    @Override
    public void componentShown(ComponentEvent e)
    {
        componentMoved(e);
    }

    @Override
    public void componentHidden(ComponentEvent e)
    {

    }

    @Override
    public void windowStateChanged(WindowEvent e)
    {
        model.setSize(view.getMap().getSize());
        model.calculateFactor();
        model.notifyObservers();
    }
    
    public static class Listener extends AbstractAction
    {
        private final Model model;
        private final Action action;
        
        public Listener(Model model, Action action)
        {
            this.model = model;
            this.action = action;
        }
        
        @Override
        public void actionPerformed(ActionEvent e)
        {
            switch(action) {
                case RESET:
                    model.reset();
                    break;
                case UP:
                    model.goUp(30);
                    break;
                case DOWN:
                    model.goDown(30);
                    break;
                case LEFT:
                    model.goLeft(30);
                    break;
                case RIGHT:
                    model.goRight(30);
                    break;
                case ZOOM_IN:
                    model.zoomIn();
                    break;
                case ZOOM_OUT:
                    model.zoomOut();
                    break;
                case MOUSE_MOVE:
                    model.setMouse(MouseTool.MOVE);
                    return;
                case MOUSE_ZOOM:
                    model.setMouse(MouseTool.ZOOM);
                    return;
            }
            model.calculateFactor();
            model.notifyObservers();
        }
    }
    
    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Model model = new Model();
        View view = new View(model);
        model.addObserver(view);
        Controller controller = new Controller(model, view);
        view.addComponentListener(controller);
        frame.addWindowStateListener(controller);
        frame.setContentPane(view);
        frame.getContentPane().addMouseListener(controller);
        frame.getContentPane().addMouseMotionListener(controller);
        frame.getContentPane().addMouseWheelListener(controller);
        frame.pack();
        frame.setVisible(true);
    }
}