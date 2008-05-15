package etomica.integrator.mcmove;

import etomica.action.AtomActionTranslateTo;
import etomica.api.IAtomPositionDefinition;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.ISpecies;
import etomica.atom.AtomArrayList;
import etomica.atom.AtomPositionCOM;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorArrayListSimple;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.space.ISpace;

/**
 * Basic Monte Carlo move for semigrand-ensemble simulations.  Move consists
 * of selecting a molecule at random and changing its species identity.  More precisely,
 * the molecule is removed and another molecule of a different species replaces it.
 * An arbitrary number of species may be designated as subject to these exchange moves.
 * Acceptance is regulated by a set of fugacity fractions that are specified at design time.
 *
 * @author Jhumpa Adhikari
 * @author David Kofke
 */
public class MCMoveSemigrand extends MCMoveBox {
    
    private static final long serialVersionUID = 2L;
    private ISpecies[] speciesSet;
    private AtomArrayList[] reservoirs;
    private double[] fugacityFraction;
    private int nSpecies;
    private final AtomArrayList affectedAtomList;
    private final AtomIteratorArrayListSimple affectedAtomIterator; 
    private final MeterPotentialEnergy energyMeter;
    private final AtomActionTranslateTo moleculeTranslator;
    private IAtomPositionDefinition atomPositionDefinition;
    private final IRandom random;
    
    private transient IMolecule deleteMolecule, insertMolecule;
    private transient double uOld;
    private transient double uNew = Double.NaN;
    private transient int iInsert, iDelete;

    public MCMoveSemigrand(IPotentialMaster potentialMaster, IRandom random,
    		               ISpace _space) {
        super(potentialMaster);
        this.random = random;
        energyMeter = new MeterPotentialEnergy(potentialMaster);
        affectedAtomList = new AtomArrayList(2);
        affectedAtomIterator = new AtomIteratorArrayListSimple(affectedAtomList);
        affectedAtomIterator.setList(affectedAtomList);
        perParticleFrequency = true;
        energyMeter.setIncludeLrc(true);
        moleculeTranslator = new AtomActionTranslateTo(_space);
        setAtomPositionDefinition(new AtomPositionCOM(_space));
    }
    
    /**
     * Extends the superclass method to initialize the exchange-set species agents for the box.
     */
    public void setBox(IBox p) {
        super.setBox(p);
        energyMeter.setBox(box);
    }//end setBox
    
    /**
     * Mutator method for the set of species that can participate in an exchange move.
     */
    public void setSpecies(ISpecies[] species) {
        nSpecies = species.length;
        if(nSpecies < 2) throw new IllegalArgumentException("Wrong size of species array in MCMoveSemigrand");
        speciesSet = new ISpecies[nSpecies];
        fugacityFraction = new double[nSpecies];
        reservoirs = new AtomArrayList[nSpecies];
        for(int i=0; i<nSpecies; i++) {
            speciesSet[i] = species[i];
            fugacityFraction[i] = 1.0/nSpecies;
            reservoirs[i] = new AtomArrayList();
        }
    }
    
    /**
     * Accessor method for the set of species that can participate in an exchange move.
     */
    public ISpecies[] getSpecies() {return speciesSet;}
    
    /**
     * Specifies the fugacity fractions for the set of species that can participate in
     * an exchange move.  The given array must have the same dimension as the array of
     * species that was previously set in a call to setSpecies.  If the given set of "fractions"
     * does not sum to unity, the values will be normalized (e.g., sending the set {1.0, 1.0} 
     * leads to fugacity fractions of {0.5, 0.5}).
     */
    public void setFugacityFraction(double[] f) {
        if(f.length != nSpecies || speciesSet == null) 
            throw new IllegalArgumentException("Wrong size of fugacity-fraction array in MCMoveSemigrand");
            
        double sum = 0.0;
        for(int i=0; i<nSpecies; i++) {
            fugacityFraction[i] = f[i]; 
            if(f[i] < 0.0) throw new IllegalArgumentException("Negative fugacity-fraction MCMoveSemigrand");
            sum += f[i];
        }
        for(int i=0; i<nSpecies; i++) {fugacityFraction[i] /= sum;}//normalize to unity
    }

    public double getFugacityFraction(int i) {
        if(i < 0 || i >= nSpecies) 
            throw new IllegalArgumentException("Illegal fugacity-fraction index in MCMoveSemigrand");
        return fugacityFraction[i];
    }

    /**
     * Accessor method for the set of fugacity fractions.
     */
    public double[] getFugacityFraction() {return fugacityFraction;}
    
    public boolean doTrial() {
        //select species for deletion
        iDelete = random.nextInt(nSpecies);//System.out.println("Random no. :"+randomNo);
        if(box.getNMolecules(speciesSet[iDelete]) == 0) {
            uNew = uOld = 0.0;
            return false;
        }

        //select species for insertion
        iInsert = iDelete;
        if(nSpecies == 2) iInsert = 1 - iDelete;
        else while(iInsert == iDelete) {iInsert = random.nextInt(nSpecies);}
  
        IAtomSet moleculeList = box.getMoleculeList(speciesSet[iDelete]);
        deleteMolecule = (IMolecule)moleculeList.getAtom(random.nextInt(moleculeList.getAtomCount()));
        energyMeter.setTarget(deleteMolecule);
        uOld = energyMeter.getDataAsScalar();
        box.removeMolecule(deleteMolecule);
        
        int size = reservoirs[iInsert].getAtomCount();
        if(size>0) {
            insertMolecule = (IMolecule)reservoirs[iInsert].remove(size-1);
            box.addMolecule(insertMolecule);
        }
        else {
            insertMolecule = (IMolecule)box.addNewMolecule(speciesSet[iInsert]);
        }
        moleculeTranslator.setDestination(atomPositionDefinition.position(deleteMolecule));
        moleculeTranslator.actionPerformed(insertMolecule);
        //in general, should also randomize orintation and internal coordinates
        uNew = Double.NaN;
        return true;
    }//end of doTrial
    
    public double getA() {
        return (double)(box.getNMolecules(speciesSet[iDelete])+1)/(double)box.getNMolecules(speciesSet[iInsert])
                *(fugacityFraction[iInsert]/fugacityFraction[iDelete]);
    }
    
    public double getB() {
        energyMeter.setTarget(insertMolecule);
        uNew = energyMeter.getDataAsScalar();
        return -(uNew - uOld);
    }
    
    public void acceptNotify() {
        //put deleted molecule in reservoir
        reservoirs[iDelete].add(deleteMolecule);
    }

    public void rejectNotify() {
        //put deleted molecule back into box
        box.addMolecule(deleteMolecule);
        //remove inserted molecule and put in reservoir
        box.removeMolecule(insertMolecule);
        reservoirs[iInsert].add(insertMolecule);
    }
    
    

    public double energyChange() {return uNew - uOld;}
    
    public final AtomIterator affectedAtoms() {
        
        affectedAtomList.clear();
        affectedAtomList.add(insertMolecule);
        affectedAtomList.add(deleteMolecule);
        affectedAtomIterator.reset();
        return affectedAtomIterator;
    }

    /**
     * @return Returns the positionDefinition.
     */
    public IAtomPositionDefinition geAtomPositionDefinition() {
        return atomPositionDefinition;
    }
    /**
     * @param positionDefinition The positionDefinition to set.
     */
    public void setAtomPositionDefinition(IAtomPositionDefinition positionDefinition) {
        this.atomPositionDefinition = positionDefinition;
        moleculeTranslator.setAtomPositionDefinition(positionDefinition);
    }

}