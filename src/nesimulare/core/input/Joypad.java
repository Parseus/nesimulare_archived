/*
 * The MIT License
 *
 * Copyright 2013-2014 Parseus.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package nesimulare.core.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.Preferences;
import nesimulare.gui.PrefsSingleton;
import nesimulare.gui.Tools;
import net.java.games.input.*;

/**
 *
 * @author Parseus
 * This class uses the JInput Java game controller API
 * (cf. http://java.net/projects/jinput).
 */
public class Joypad extends java.awt.Component implements KeyListener {
    private final java.awt.Component parent;
    private Controller gameController;
    private Component[] buttons;
    private final ScheduledExecutorService thread = Executors.newSingleThreadScheduledExecutor();
    private int latchbyte = 0, controllerbyte = 0, prevbyte = 0, outbyte = 0, gamepadbyte = 0;
    private static final double threshold = 0.25;
    private final HashMap<Integer, Integer> m = new HashMap<>(10);
    private final int controllerNumber;

    public Joypad(final java.awt.Component parent, int controllerNumber) {
        super();
        
        if (parent == null) {
            throw new NullPointerException("Parent not found!");
        }
        
        this.parent = parent;
        this.controllerNumber = controllerNumber;
        setButtons();
        parent.addKeyListener(this);
    }

    @Override
    public void keyPressed(final KeyEvent arg0) {
        //enable the byte of whatever is found
        prevbyte = controllerbyte;
        final int kepressed = arg0.getKeyCode();
        
        if (!m.containsKey(kepressed)) {
            return;
        }
        //enable the corresponding bit to the key
        controllerbyte |= m.get(kepressed);
        
        //special case: if up and down are pressed at once, use whichever was pressed previously
        if (Tools.getbit(controllerbyte, 4) && Tools.getbit(controllerbyte, 5)) {
            controllerbyte &= ~(Tools.BIT4 | Tools.BIT5);
            controllerbyte |= (prevbyte & ~(Tools.BIT4 | Tools.BIT5));
        }
        
        //same for left and right
        if (Tools.getbit(controllerbyte, 6) && Tools.getbit(controllerbyte, 7)) {
            controllerbyte &= ~(Tools.BIT6 | Tools.BIT7);
            controllerbyte |= (prevbyte & ~(Tools.BIT6 | Tools.BIT7));
        }

    }

    @Override
    public void keyReleased(final KeyEvent arg0) {
        prevbyte = controllerbyte;
        final int kepressed = arg0.getKeyCode();
        
        if (!m.containsKey(kepressed)) {
            return;
        }
        
        controllerbyte &= ~m.get(kepressed);
    }

    public int getbyte() {
        return outbyte;
    }

    @Override
    public void keyTyped(final KeyEvent arg0) {
        // TODO Auto-generated method stub
    }

    public void strobe() {
        //shifts a byte out
        outbyte = latchbyte & 1;
        latchbyte = ((latchbyte >> 1) | 0x100);
    }

    public void output(final boolean state) {
        latchbyte = gamepadbyte | controllerbyte;
    }

    /**
     * Start in a separate thread the processing of the controller event queue.
     * Must be called after construction of the class to enable the processing
     * of the joystick / gamepad events.
     */
    public void startEventQueue() {
        thread.execute(eventQueueLoop());
    }

    private Runnable eventQueueLoop() {
        return new Runnable() {
            @Override
            public void run() {
                if (gameController != null) {
                    Event event = new Event();
                    
                    while (!Thread.interrupted()) {
                        gameController.poll();
                        EventQueue queue = gameController.getEventQueue();
                        
                        while (queue.getNextEvent(event)) {
                            Component component = event.getComponent();
                            
                            if (component.getIdentifier() == Component.Identifier.Axis.X) {
                                if (event.getValue() > threshold) {
                                    gamepadbyte |= Tools.BIT7;//left on, right off
                                    gamepadbyte &= ~Tools.BIT6;
                                } else if (event.getValue() < -threshold) {
                                    gamepadbyte |= Tools.BIT6;
                                    gamepadbyte &= ~Tools.BIT7;
                                } else {
                                    gamepadbyte &= ~(Tools.BIT7 | Tools.BIT6);
                                }
                            } else if (component.getIdentifier() == Component.Identifier.Axis.Y) {
                                if (event.getValue() > threshold) {
                                    gamepadbyte |= Tools.BIT5;//up on, down off
                                    gamepadbyte &= ~Tools.BIT4;
                                } else if (event.getValue() < -threshold) {
                                    gamepadbyte |= Tools.BIT4;//down on, up off
                                    gamepadbyte &= ~Tools.BIT5;
                                } else {
                                    gamepadbyte &= ~(Tools.BIT4 | Tools.BIT5);
                                }
                            } else if (component == buttons[0]) {
                                if (isPressed(event)) {
                                    gamepadbyte |= Tools.BIT0;
                                } else {
                                    gamepadbyte &= ~Tools.BIT0;
                                }
                            } else if (component == buttons[1]) {
                                if (isPressed(event)) {
                                    gamepadbyte |= Tools.BIT1;
                                } else {
                                    gamepadbyte &= ~Tools.BIT1;
                                }
                            } else if (component == buttons[2]) {
                                if (isPressed(event)) {
                                    gamepadbyte |= Tools.BIT2;
                                } else {
                                    gamepadbyte &= ~Tools.BIT2;
                                }
                            } else if (component == buttons[3]) {
                                if (isPressed(event)) {
                                    gamepadbyte |= Tools.BIT3;
                                } else {
                                    gamepadbyte &= ~Tools.BIT3;
                                }
                            }
                        }

                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            // Preserve interrupt status
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        };
    }

    private boolean isPressed(Event event) {
        Component component = event.getComponent();
        if (component.isAnalog()) {
            return Math.abs(event.getValue()) > 0.2f;
        } else return event.getValue() != 0;
    }

    /**
     * Stop the controller event queue thread. Must be called before closing the
     * application.
     */
    public void stopEventQueue() {
        thread.shutdownNow();
    }

    /**
     * This method detects the available joysticks / gamepads on the computer
     * and return them in a list.
     *
     * @return List of available joysticks / gamepads connected to the computer
     */
    private static Controller[] getAvailablePadControllers() {
        List<Controller> gameControllers = new ArrayList<>();
        // Get a list of the controllers JInput knows about and can interact
        // with
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        // Check the useable controllers (gamepads or joysticks with at least 2
        // axis and 2 buttons)
        for (Controller controller : controllers) {
            if ((controller.getType() == Controller.Type.GAMEPAD) || (controller.getType() == Controller.Type.STICK)) {
                int nbOfAxis = 0;
                // Get this controllers components (buttons and axis)
                Component[] components = controller.getComponents();
                
                // Check the availability of X/Y axis and at least 2 buttons
                // (for A and B, because select and start can use the keyboard)
                for (Component component : components) {
                    if ((component.getIdentifier() == Component.Identifier.Axis.X)
                            || (component.getIdentifier() == Component.Identifier.Axis.Y)) {
                        nbOfAxis++;
                    }
                }
                
                if ((nbOfAxis >= 2) && (getButtons(controller).length >= 2)) {
                    // Valid game controller
                    gameControllers.add(controller);
                }
            }
        }
        
        return gameControllers.toArray(new Controller[0]);
    }

    /**
     * Return the available buttons on this controller (by priority order).
     *
     * @param controller
     * @return
     */
    private static Component[] getButtons(Controller controller) {
        List<Component> buttons = new ArrayList<>();
        // Get this controllers components (buttons and axis)
        Component[] components = controller.getComponents();
        
        for (Component component : components) {
            if (component.getIdentifier() instanceof Component.Identifier.Button) {
                buttons.add(component);
            }
        }
        
        return buttons.toArray(new Component[0]);
    }
    

    public final void setButtons() {
        Preferences prefs = PrefsSingleton.get();
        
        //reset the buttons from prefs
        m.clear();
        
        switch (controllerNumber) {
            case 1:
            default:
                m.put(prefs.getInt("keyUp1", KeyEvent.VK_UP), Tools.BIT4);
                m.put(prefs.getInt("keyDown1", KeyEvent.VK_DOWN), Tools.BIT5);
                m.put(prefs.getInt("keyLeft1", KeyEvent.VK_LEFT), Tools.BIT6);
                m.put(prefs.getInt("keyRight1", KeyEvent.VK_RIGHT), Tools.BIT7);
                m.put(prefs.getInt("keyA1", KeyEvent.VK_X), Tools.BIT0);
                m.put(prefs.getInt("keyB1", KeyEvent.VK_Z), Tools.BIT1);
                m.put(prefs.getInt("keySelect1", KeyEvent.VK_SHIFT), Tools.BIT2);
                m.put(prefs.getInt("keyStart1", KeyEvent.VK_ENTER), Tools.BIT3);
                break;
            case 2:
                m.put(prefs.getInt("keyUp2", KeyEvent.VK_W), Tools.BIT4);
                m.put(prefs.getInt("keyDown2", KeyEvent.VK_S), Tools.BIT5);
                m.put(prefs.getInt("keyLeft2", KeyEvent.VK_A), Tools.BIT6);
                m.put(prefs.getInt("keyRight2", KeyEvent.VK_D), Tools.BIT7);
                m.put(prefs.getInt("keyA2", KeyEvent.VK_G), Tools.BIT0);
                m.put(prefs.getInt("keyB2", KeyEvent.VK_F), Tools.BIT1);
                m.put(prefs.getInt("keySelect2", KeyEvent.VK_R), Tools.BIT2);
                m.put(prefs.getInt("keyStart2", KeyEvent.VK_T), Tools.BIT3);
                break;

        }
        
        Controller[] controllers = getAvailablePadControllers();
        
        if (controllers.length > controllerNumber) {
            this.gameController = controllers[controllerNumber];
            PrefsSingleton.get().put("controller" + controllerNumber, gameController.getName());
            this.buttons = getButtons(controllers[controllerNumber]);
        } else {
            PrefsSingleton.get().put("controller" + controllerNumber,"");
            this.gameController = null;
            this.buttons = null;
        }
    }
}