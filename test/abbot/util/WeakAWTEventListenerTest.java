package abbot.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.util.List;

import abbot.Log;
import junit.extensions.abbot.*;

public class WeakAWTEventListenerTest extends ComponentTestFixture {

    private class Listener implements AWTEventListener {
        public List events = new ArrayList();
        public List contexts = new ArrayList();
        public void eventDispatched(AWTEvent e) {
            events.add(e);
            class Context extends RuntimeException { }
            contexts.add(new Context());
        }
    }

    public void testAddAndRemove() {
        ArrayList<AWTEventListener> list = new ArrayList<AWTEventListener>();
       final  ArrayList<WeakAWTEventListener> weakList = new ArrayList<WeakAWTEventListener>();

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        int SIZE = 1000;
        Listener l1 = new Listener();
        toolkit.addAWTEventListener(l1, -1);

        for (int i=0;i < SIZE;i++) {
            Listener listener = new Listener();
            list.add(listener);
            // Warning this constructor adds a listener
            weakList.add(new WeakAWTEventListener(listener, -1));
        }
        Listener l2 = new Listener();
        toolkit.addAWTEventListener(l2, -1);
        list.clear();
        
        // We need to gc until such time as we are sure that the references are indeed tidied up
        // System.gc() on it's own just won't cut it
        abbot.tester.Robot.wait(new Condition()
             {
            @Override
            public boolean test() {
                System.gc();
                for (WeakAWTEventListener next : weakList) {
                    if (!next.isDisposed()) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public String toString() {
                return "Need to let everything be disposed";
            }
        });
            
        

        // All unreferenced listeners should be removed on the next posted
        // event. 
        showFrame(new JLabel(getName()));


        Throwable t1 = (Throwable)l1.contexts.get(l1.contexts.size()-1);
        Throwable t2 = (Throwable)l2.contexts.get(l2.contexts.size()-1);

        try {
            int MAX_STACK = 2000;
            String stack1 = Log.getStack(t1);
            assertTrue("Stack trace too long ("
                       + stack1.length() + "): " + stack1,
                       stack1.length() < MAX_STACK);
            String stack2 = Log.getStack(t2);
            assertTrue("Stack trace too long ("
                       + stack2.length() + "): " + stack2,
                       stack2.length() < MAX_STACK);
        }
        finally {
            toolkit.removeAWTEventListener(l1);
            toolkit.removeAWTEventListener(l2);
        }
    }

    public WeakAWTEventListenerTest(String name) { super(name); }
    public static void main(String[] args) {
        TestHelper.runTests(args, WeakAWTEventListenerTest.class);
    }
}
