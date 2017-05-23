package util;

import java.util.HashMap;
import java.util.Map;

public class MatrixView_copy {
    private DoubleMatrix mat = null;
    private HashMap<Key, Double> updates = new HashMap<>();
    private boolean batchIsOn = true; 
    
    public MatrixView_copy(DoubleMatrix mat, boolean batchIsOn) {
        this.mat = mat;
        this.batchIsOn = batchIsOn;
    }
    
    public double getValue(int row, int col) {
        return mat.getValue(row, col);
    }
    
    public double getRowSum(int row) {
        return mat.getRowSum(row);
    }
    
    public void incValue(int row, int col) {
        incValue(row, col, 1);
    }
    
    public void incValue(int row, int col, double val) {
        if (batchIsOn) {
            Key key = new Key(row, col);
            Double prevVal = updates.getOrDefault(key, 0.0);
            updates.put(key, prevVal+val);
        } else {
            mat.incValue(row, col, val);
        }
    }
    
    public void decValue(int row, int col) {
        decValue(row, col, 1);
    }
    
    public void decValue(int row, int col, double val) {
        if (batchIsOn) {
            Key key = new Key(row, col);
            Double prevVal = updates.getOrDefault(key, 0.0);
            updates.put(key, prevVal-val);
        } else {
            mat.incValue(row, col, val);
        }
    }
    
    public void commit() {
        for (Map.Entry<Key, Double> entry : updates.entrySet()) {
            Key key = entry.getKey();
            double val = entry.getValue();
            mat.incValue(key.X, key.Y, val);
        }
        updates.clear();
    }
    
    class Key {

        public final int X;
        public final int Y;

        public Key(final int X, final int Y) {
          this.X = X;
          this.Y = Y;
        }

        public boolean equals (final Object O) {
          if (!(O instanceof Key)) return false;
          if (((Key) O).X != X) return false;
          if (((Key) O).Y != Y) return false;
          return true;
        }

        public int hashCode() {
          return (X << 16) + Y;
        }

      }

}
