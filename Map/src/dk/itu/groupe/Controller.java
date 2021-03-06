package dk.itu.groupe;

import dk.itu.groupe.data.CommonRoadType;
import dk.itu.groupe.loading.LoadingPanel;
import dk.itu.groupe.pathfinding.NoPathFoundException;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.*;

/**
 * The Controller is responsible for dealing with events generated on the View.
 *
 * It handles mouseevents and other actions.
 *
 * @author Peter Bindslev (plil@itu.dk), Rune Henriksen (ruju@itu.dk) &amp;
 * Mikael Jepsen (mlin@itu.dk)
 */
public class Controller extends ComponentAdapter implements
        MouseListener,
        MouseMotionListener,
        MouseWheelListener
{

    private final Model model;
    private final View view;
    private static Point lastRightClick;

    /**
     * Creates a new Controller that sets a lot of keybindings on the keyboard
     * and listeners for mouse-input and frame-size changes.
     *
     * @param model The model the controller should change.
     * @param view The view the controller should recieve input from.
     */
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
        addListeners();
    }

    private void addListeners()
    {
        view.getMap().addComponentListener(this);
        view.getMap().addMouseListener(this);
        view.getMap().addMouseMotionListener(this);
        view.getMap().addMouseWheelListener(this);
    }

    @Override
    public void mouseMoved(MouseEvent me)
    {
        // The two if statements decides whether the left panel is shown or not.
        if (me.getX() < 15) {
            view.openLeftPanel();
        }
        if (me.getX() > 250) {
            view.closeLeftPanel();
        }
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
        if (SwingUtilities.isLeftMouseButton(me)) {
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
            lastRightClick = me.getPoint();
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
                case FASTEST:
                    model.setPathByDriveTime(true);
                    model.notifyObservers("updateRoadList");
                    break;
                case SHORTEST:
                    model.setPathByDriveTime(false);
                    model.notifyObservers("updateRoadList");
                    break;
                case RESET_DIRECTIONS:
                    model.resetPointSet();
                    model.notifyObservers("updateRoadList");
                    break;
                case SET_FROM:
                    try {
                        assert lastRightClick != null;
                        model.setFromNode(model.translatePoint(lastRightClick.x, lastRightClick.y));
                    } catch (NoPathFoundException ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage());
                    }
                    model.notifyObservers("updateRoadList");
                    break;
                case SET_TO:
                    try {
                        model.setToNode(model.translatePoint(lastRightClick.x, lastRightClick.y));
                    } catch (NoPathFoundException ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage());
                    }
                    model.notifyObservers("updateRoadList");
                    break;
                case ZOOM_OUT:
                    model.zoomOut();
                    break;
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
            System.setErr(new PrintStream("log-" + DateFormat.getDateInstance().format(new Date()) + ".log"));
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (FileNotFoundException | ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace(System.err);
        }
        ImageIcon icon = new ImageIcon("./res/Icon.png");
        String dataset = (String) JOptionPane.showInputDialog(null,
                "Do you want to use Krak data or OpenStreetMap-data?\n"
                + "Krak is a smaller and older dataset, but loads faster\n"
                + "OpenStreetMap is newer and contains more data.",
                "Choose data",
                JOptionPane.QUESTION_MESSAGE,
                icon,
                new String[]{"Krak", "OpenStreetMap"},
                "OpenStreetMap");
        if (dataset == null) {
            System.exit(0);
        }
        long time = System.currentTimeMillis();
        JFrame frame = new JFrame("GroupE-map");
        JPanel glassPane = new JPanel(new BorderLayout());
        glassPane.setOpaque(false);
        frame.setGlassPane(glassPane);
        frame.setIconImage(icon.getImage());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        LoadingPanel lp = new LoadingPanel();
        frame.getContentPane().add(lp);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        final Model model = new Model(dataset);
        System.out.println("Created model in " + (System.currentTimeMillis() - time) / 1000.0 + " s");
        //Loading the actual map.
        time = System.currentTimeMillis();
        model.loadCoastline();
        lp.elementLoaded();
        System.out.println("Loaded coastline in " + (System.currentTimeMillis() - time) / 1000.0 + " s");
        time = System.currentTimeMillis();
        model.loadNodes();
        lp.elementLoaded();
        System.out.println("Loaded nodes in " + (System.currentTimeMillis() - time) / 1000.0 + " s");
        time = System.currentTimeMillis();
        try {
            ExecutorService es = Executors.newFixedThreadPool(4);
            for (final CommonRoadType rt : CommonRoadType.values()) {
                model.loadRoadType(rt, es);
                lp.elementLoaded();
            }
            es.shutdown();
            es.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }
        System.out.println("Loaded edges in " + (System.currentTimeMillis() - time) / 1000.0 + " s");
        // Finished loading.

        final View view = new View(model);
        model.addObserver(view);
        Controller controller = new Controller(model, view);
        frame.setVisible(false);
        frame.getContentPane().removeAll();
        frame.add(view);
        frame.pack();
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    }
}
