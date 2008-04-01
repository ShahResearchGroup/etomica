package g3dsys.control;

import java.awt.Graphics;

import javax.swing.JPanel;
import javax.vecmath.Matrix3f;

import org.jmol.g3d.Graphics3D;

/**
 *	Represents the panel on which all graphical data is actually drawn.
 *	Intercepts mouse events, etc. 
 *
 */

class Display extends JPanel {

  private G3DSys gsys;
  private Matrix3f m = new Matrix3f(1,0,0,0,1,0,0,0,1);

  public Display(G3DSys g) {
    gsys = g;
  }

  public void paint(Graphics g) {
    Graphics3D g3d = gsys.getG3D();

    //Rotation matrix m may be needed to draw figures with orientation,
    //i.e.; cylinders. Currently unimplemented, but will be needed later. 
    g3d.beginRendering(m); // ?!?!
    gsys.draw(); //dispatch to FigureManager
    g3d.endRendering();
    g.drawImage(g3d.getScreenImage(), 0, 0, null);
    g3d.releaseScreenImage();

  }

  public void update(Graphics g) {
    paint(g);
  }

  public void setSize(int width, int height) {
    /*
     * Fixes the resize race condition that caused random improper
     * scaling at startup; changes in display size are propagated
     * to this panel, but the TransformManager was not made aware of
     * it -- until now.
     * Have been unable to reproduce the bug since.
     */
    super.setSize(width, height);
    TransformManager tm = gsys.getTM();
    if(tm != null) {
      tm.setScreenDimension(width, height);
      tm.scaleFitToScreen();
    }
  }



}
