package etomica.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import etomica.api.IAtomList;
import etomica.api.IAtomPositioned;
import etomica.api.IBox;
import etomica.api.IVectorMutable;

/**
 * reads configuration coordinates from a file and assigns them to the leaf atoms in a box
 */
public class ConfigurationFile implements Configuration, java.io.Serializable {

    public ConfigurationFile(String aConfName) {
        confName = aConfName;
    }
    
    public void initializeCoordinates(IBox box) {
        IAtomList leafList = box.getLeafList();
        String fileName = confName+".pos";
        FileReader fileReader;
        try {
            fileReader = new FileReader(fileName);
        }catch(IOException e) {
            throw new RuntimeException("Cannot open "+fileName+", caught IOException: " + e.getMessage());
        }
        try {
            BufferedReader bufReader = new BufferedReader(fileReader);
            int nLeaf = leafList.getAtomCount();
            for (int iLeaf=0; iLeaf<nLeaf; iLeaf++) {
                IAtomPositioned a = (IAtomPositioned)leafList.getAtom(iLeaf);
                setPosition(a,bufReader.readLine());
            }
            fileReader.close();
        } catch(IOException e) {
            throw new RuntimeException("Problem writing to "+fileName+", caught IOException: " + e.getMessage());
        }
    }
        
    private void setPosition(IAtomPositioned atom, String string) {
        String[] coordStr = string.split(" +");
        IVectorMutable pos = atom.getPosition();
        for (int i=0; i<pos.getD(); i++) {
            pos.setX(i, Double.valueOf(coordStr[i]).doubleValue());
        }
    }
    
    private static final long serialVersionUID = 2L;
    private String confName;
}
