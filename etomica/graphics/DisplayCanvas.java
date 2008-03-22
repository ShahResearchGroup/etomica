package etomica.graphics;
import java.awt.Graphics;
import java.awt.Image;

import etomica.units.Pixel;

/**
 * Superclass for classes that display information from simulation by painting to a canvas.
 * Defines methods useful for dealing with mouse and key events targeted at the display.
 * Much of the class is involved with defining event handling methods to permit display 
 * to be moved or resized; in the future these functions will be handled instead using awt component functions.
 * 
 * @see DisplayBox.Canvas
 */
public abstract class DisplayCanvas extends javax.swing.JPanel implements DisplayCanvasInterface {

    protected Image offScreen;
    protected Graphics osg;
            
    protected DisplayBox displayBox;

    /**
    * Variable specifying whether a line tracing the boundary of the display should be drawn
    * Default value is <code>BOUNDARY_OUTLINE</code>
    */
    int drawBoundary = DRAW_BOUNDARY_OUTLINE;

    /**
     * Flag to indicate if display can be resized
     */
    boolean resizable = false;
    /**
     * Flag to indicate if display can be moved
     */
    boolean movable = false;

    /** 
     * Flag to indicate if value of scale should be superimposed on image
     */
    boolean writeScale = false;
    
    protected Pixel pixel;

    public DisplayCanvas() {
        setBackground(java.awt.Color.white);
    }
    public void createOffScreen () {
        if (offScreen == null) { 
            createOffScreen(getSize().width, getSize().height);
        }
    }
    public void createOffScreen (int p) {
        createOffScreen(p,p);
    }
    public void createOffScreen(int w, int h) {
        offScreen = createImage(w,h);
        if(offScreen != null) osg = offScreen.getGraphics();
    }
    
    public abstract void doPaint(Graphics g);
    
    public void update(Graphics g) {paint(g);}
        
    public void paint(Graphics g) {
        createOffScreen();
        doPaint(osg);
        g.drawImage(offScreen, 0, 0, null);
    }

    /**
     * Same as setSize, but included to implement DisplayCanvasInterface,
     * which has this for compatibility with OpenGL.
     */
    public void reshape(int width, int height) {
        setSize(width, height);
    }
    
    public void setMovable(boolean b) {movable = b;}
    public boolean isMovable() {return movable;}
    public void setResizable(boolean b) {resizable = b;}
    public boolean isResizable() {return resizable;}

    public void setWriteScale(boolean s) {writeScale = s;}
    public boolean getWriteScale() {return(writeScale);}

    public void setDrawBoundary(int b) {
      drawBoundary = b;
    }
    public int getDrawBoundary() {return drawBoundary;}

    public void setShiftX(float x) {}
    public void setShiftY(float y) {}
    public void setPrevX(float x) {}
    public void setPrevY(float y) {}
    public void setXRot(float x) {}
    public void setYRot(float y) {}
    public void setZoom(float z) {}
    public float getShiftX() {return(0f);}
    public float getShiftY() {return(0f);}
    public float getPrevX() {return(0f);}
    public float getPrevY() {return(0f);}
    public float getXRot() {return(0f);}
    public float getYRot() {return(0f);}
    public float getZoom() {return(1f);}
    public void startRotate(float x, float y) {};
    public void stopRotate() {};

    /**
     * Returns unit for conversion between simulation units and display pixels.
     */
    public Pixel getPixelUnit() {
        return pixel;
    }
    
    /**
     * Sets unit for conversion between simulation units and display pixels.
     */
    public void setPixelUnit(Pixel pixel) {
        this.pixel = pixel;
    }

} //end of DisplayCanvas class

