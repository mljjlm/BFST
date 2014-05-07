package dk.itu.groupe;

import dk.itu.groupe.data.CommonRoadType;
import dk.itu.groupe.loading.LoadingPanel;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;

/**
 *
 * @author Peter Bindslev (plil@itu.dk), Rune Henriksen (ruju@itu.dk) & Mikael
 * Jepsen (mlin@itu.dk)
 */
public class Controller extends ComponentAdapter implements
        MouseListener,
        MouseMotionListener,
        MouseWheelListener,
        WindowStateListener
{

    private final Model model;
    private final View view;

    public Controller(final Model model, final View view)
    {
        this.model = model;
        this.view = view;
        view.getActionMap().put(Action.UP, Action.UP.getListener(model));
        view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), Action.UP);
        view.getActionMap().put(Action.RIGHT, Action.RIGHT.getListener(model));
        view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), Action.RIGHT);
        view.getActionMap().put(Action.LEFT, Action.LEFT.getListener(model));
        view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), Action.LEFT);
        view.getActionMap().put(Action.DOWN, Action.DOWN.getListener(model));
        view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), Action.DOWN);
        view.getActionMap().put(Action.ZOOM_IN, Action.ZOOM_IN.getListener(model));
        view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), Action.ZOOM_IN);
        view.getActionMap().put(Action.ZOOM_OUT, Action.ZOOM_OUT.getListener(model));
        view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), Action.ZOOM_OUT);
        view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), Action.ZOOM_IN);
        view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), Action.ZOOM_OUT);
    }

    @Override
    public void mouseMoved(MouseEvent me)
    {
        Point2D p = model.translatePoint(me.getX(), me.getY());
        model.updateRoadname(p.getX(), p.getY());
        model.notifyObservers("updateRoadname");
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if (e.getWheelRotation() < 0) {
            model.zoomInScroll(e.getX(), e.getY());
        } else {
            model.zoomOutScroll(e.getX(), e.getY());
        }
        model.notifyObservers();
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
        if (model.getMouseTool() != MouseTool.PATH && SwingUtilities.isLeftMouseButton(me)) {
            model.setPressed(me.getPoint());
            model.setDragged(me.getPoint());
        }
    }

    @Override
    public void mouseReleased(MouseEvent me)
    {
        if (SwingUtilities.isLeftMouseButton(me) && model.getMouseTool() == MouseTool.ZOOM) {
            if (model.getPressed().getX() != model.getDragged().getX() && model.getPressed().getY() != model.getDragged().getY()) {
                model.zoomRect(model.getPressed().getX(), model.getPressed().getY(), model.getDragged().getX(), model.getDragged().getY());
                model.notifyObservers();
            }
            model.setPressed(null);
            model.setDragged(null);
        }
        if (SwingUtilities.isRightMouseButton(me)) {
            view.showContextMenu(me.getPoint());
        }
    }

    @Override
    public void mouseExited(MouseEvent me)
    {

    }

    @Override
    public void mouseDragged(MouseEvent me)
    {
        if (SwingUtilities.isLeftMouseButton(me)) {
            if (model.getMouseTool() == MouseTool.MOVE) {
                if (model.getPressed() != null) {
                    Point2D then = model.getDragged();
                    Point2D now = model.translatePoint(me.getX(), me.getY());
                    model.moveMap(then.getX() - now.getX(), now.getY() - then.getY());
                }
            }
            model.setDragged(me.getPoint());
            model.notifyObservers();
        }
    }

    @Override
    public void componentResized(ComponentEvent e)
    {
        model.setSize(view.getMap().getSize().width, view.getMap().getSize().height);
        model.notifyObservers();
    }

    @Override
    public void windowStateChanged(WindowEvent e)
    {
        model.setSize(view.getMap().getSize().width, view.getMap().getSize().height);
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
            switch (action) {
                case RESET:
                    model.reset();
                    break;
                case UP:
                    model.goUp(30 * model.getFactor());
                    break;
                case DOWN:
                    model.goDown(30 * model.getFactor());
                    break;
                case LEFT:
                    model.goLeft(30 * model.getFactor());
                    break;
                case RIGHT:
                    model.goRight(30 * model.getFactor());
                    break;
                case ZOOM_IN:
                    model.zoomIn();
                    break;
                case ZOOM_OUT:
                    model.zoomOut();
                    break;
                case MOUSE_PATH:
                    model.setMouseTool(MouseTool.PATH);
                    return;
                case MOUSE_MOVE:
                    model.setMouseTool(MouseTool.MOVE);
                    return;
                case MOUSE_ZOOM:
                    model.setMouseTool(MouseTool.ZOOM);
                    return;
            }
            model.notifyObservers();
        }
    }

    public static void main(String[] args)
    {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace(System.err);
        }
        String dataset = (String) JOptionPane.showInputDialog(null,
                "Do you want to use Krak data or OpenStreetMap-data?\n"
                + "Krak is a smaller and older dataset, but loads faster\n"
                + "OpenStreetMap is newer and contains more data.",
                "Choose data",
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{"Krak", "OpenStreetMap"},
                "OpenStreetMap");
        if (dataset == null) {
            System.exit(0);
        }
        long time = System.currentTimeMillis();
        JFrame frame = new JFrame("GroupE-map");
        frame.setIconImage(new ImageIcon("./res/Icon.png").getImage());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new LoadingPanel());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        final Model model = new Model(dataset);
        model.loadCoastline();
        System.out.println("Loaded coastline in " + (System.currentTimeMillis() - time) / 1000.0 + " s");
        time = System.currentTimeMillis();
        final View view = new View(model);
        model.addObserver(view);
        Controller controller = new Controller(model, view);
        view.getMap().addComponentListener(controller);
        view.getMap().addMouseListener(controller);
        view.getMap().addMouseMotionListener(controller);
        view.getMap().addMouseWheelListener(controller);
        frame.addWindowStateListener(controller);
        JPanel glassPane = new JPanel(new BorderLayout());
        glassPane.setOpaque(false);
        frame.setGlassPane(glassPane);
        frame.setVisible(false);
        frame.getContentPane().removeAll();
        frame.add(view);
        frame.pack();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
        model.loadNodes(); 
        System.out.println("Loaded nodes in " + (System.currentTimeMillis() - time) / 1000.0 + " s");
        ExecutorService es = Executors.newFixedThreadPool(2);
        CommonRoadType[] roadtypePriority = new CommonRoadType[]{
            CommonRoadType.MOTORWAY,
            CommonRoadType.MOTORWAY_LINK,
            CommonRoadType.TRUNK,
            CommonRoadType.TRUNK_LINK,
            CommonRoadType.TUNNEL,
            CommonRoadType.FERRY,
            CommonRoadType.PRIMARY,
            CommonRoadType.PRIMARY_LINK,
            CommonRoadType.SECONDARY,
            CommonRoadType.SECONDARY_LINK,
            CommonRoadType.TERTIARY,
            CommonRoadType.TERTIARY_LINK,
            CommonRoadType.ROAD,
            CommonRoadType.UNCLASSIFIED,
            CommonRoadType.RESIDENTIAL,
            CommonRoadType.PEDESTRIAN,
            CommonRoadType.TRACK,
            CommonRoadType.PATH,
            CommonRoadType.PLACES
        };
        for (final CommonRoadType rt : roadtypePriority) {
            es.execute(new Runnable()
            {

                @Override
                public void run()
                {
                    model.loadRoadType(rt);
                    if (rt.isEnabled(model.getFactor())) {
                        view.getMap().repaint();
                    }
                }
            });
        }
    }
}
