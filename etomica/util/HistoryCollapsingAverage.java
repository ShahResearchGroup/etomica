package etomica.util;


/**
 * History that records a number of values.  When existing  space is 
 * insufficient to hold new data, the existing data is "collapsed" such that 
 * all existing data is stored in the first half of existing storage by 
 * averaging each consecutive pair of data points.  After that point, data 
 * will be stored as average block data.
 * 
 * @author Andrew Schultz
 */
public class HistoryCollapsingAverage extends HistoryCollapsing {
    
    public HistoryCollapsingAverage() {this(100);}
    public HistoryCollapsingAverage(int n) {
        super(n, 2);
        tempXBin = 0;
        tempBin = 0;
    }
    
   /**
     * adds data to the history.  If insufficient space exists
     * to store the data, existing data is collapsed by 1/2 and 
     * future data is taken half as much.
     */
    public void addValue(double x, double y) {
        if (cursor == history.length) {
            collapseData();
        }
        tempXBin += x;
        tempBin += y;
        if (++intervalCount == interval) {
            intervalCount = 0;
            xValues[cursor] = tempXBin / interval;
            history[cursor] = tempBin / interval;
            cursor++;
            tempXBin = 0;
            tempBin = 0;
        }
    }

    public void reset() {
        super.reset();
        tempXBin = 0;
        tempBin = 0;
    }
    
    protected void collapseData() {
        for (int i=0; i<cursor/2; i++) {
            xValues[i] = (xValues[i*2] + xValues[i*2+1])*0.5;
            history[i] = (history[i*2] + history[i*2+1])*0.5;
        }
        for (int i=cursor/2; i<history.length; i++) {
            xValues[i] = Double.NaN;
            history[i] = Double.NaN;
        }
        cursor /= 2;
        interval *= 2;
    }

    private double tempBin;
    private double tempXBin;
}