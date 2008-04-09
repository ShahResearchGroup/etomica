package etomica.action;

import etomica.api.IAtom;
import etomica.api.IAtomPositioned;
import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IVector;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorAllMolecules;
import etomica.atom.iterator.AtomIteratorBoxDependent;
import etomica.atom.iterator.AtomIteratorLeafAtoms;
import etomica.space.ISpace;

/**
 * Action that imposes the central-image effect of a box having periodic
 * boundaries. Causes all atoms with coordinates outside the box boundaries to
 * be moved to the central-image location (inside the boundaries).
 */

public class BoxImposePbc extends BoxActionAdapter {

	/**
	 * Creates the action without specifying a box. Requires call to setBox
	 * before action can have any effect. Default is to apply central-imaging at
	 * the atom rather than molecule level.
	 */
	public BoxImposePbc(ISpace space) {
		setApplyToMolecules(false);
		this.space = space;
	}
    
    public int getPriority() {return 100;}//100-199 is priority range for classes imposing PBC

	/**
	 * Creates the action ready to perform on the given box.
	 * 
	 * @param box
	 */
	public BoxImposePbc(IBox box, ISpace space) {
		this(space);
		setBox(box);
	}

	public void actionPerformed() {
		IBoundary boundary = box.getBoundary();
		iterator.reset();
        if (applyToMolecules) {
            for (IAtom molecule = iterator.nextAtom(); molecule != null;
                 molecule = iterator.nextAtom()) {
                IVector shift;
                if (molecule instanceof IAtomPositioned) {
                    IVector position = ((IAtomPositioned)molecule).getPosition();
                    shift = boundary.centralImage(position);
                    position.PE(shift);
                }
                else {
                    shift = boundary.centralImage(molecule.getType().getPositionDefinition().position(molecule));
                }
                if (!shift.isZero()) {
                    translator.setTranslationVector(shift);
                    moleculeTranslator.actionPerformed(molecule);
                }
            }
        }
        else {
            for (IAtomPositioned atom = (IAtomPositioned)iterator.nextAtom(); atom != null;
                 atom = (IAtomPositioned)iterator.nextAtom()) {
                IVector shift = boundary.centralImage(atom.getPosition());
                if (!shift.isZero()) {
                    atom.getPosition().PE(shift);
                }
            }
        }
	}

	public void setBox(IBox box) {
		super.setBox(box);
		iterator.setBox(box);
        if (space.D() != box.getBoundary().getDimensions().getD()) {
            throw new IllegalArgumentException("Cannot change dimension of BoxImosePbc");
        }
	}

	/**
	 * Returns the iterator that gives the atoms to which central imaging is
	 * applied.
	 * 
	 * @return AtomIteratorList
	 */
	public AtomIterator getIterator() {
		return iterator;
	}

	/**
	 * Sets the iterator the gives the atoms for central imaging. Normally this
	 * does not need to be set, but if central imaging scheme is desired for
	 * application at a level other than the molecules or the leaf atoms, or if
	 * it is to be applied only to a subet of the atoms in a box, this can be
	 * invoked by setting this iterator appropriately.
	 * 
	 * @param iterator
	 *            The iterator to set
	 */
	public void setIterator(AtomIteratorBoxDependent iterator) {
		this.iterator = iterator;
		iterator.setBox(box);
	}

	/**
	 * Returns the value of applyToMolecules.
	 * 
	 * @return boolean
	 */
	public boolean isApplyToMolecules() {
		return applyToMolecules;
	}

	/**
	 * Sets a flag indicating whether periodic boundaries are applied to the
	 * molecules (true), or to the atoms (false). If applied to the atoms (the
	 * default case), then central imaging is done to each atom individually,
	 * which could cause a molecule to be split, with some of its atoms on one
	 * edge of the simulation box, and others on the other edge. If applied to
	 * molecules, the entire molecule will be shifted as a whole when enforcing
	 * central imaging.
	 * 
	 * @param applyToMolecules
	 *            The new value of the flag.
	 */
	public void setApplyToMolecules(boolean applyToMolecules) {
		this.applyToMolecules = applyToMolecules;
		if (applyToMolecules) {
			iterator = new AtomIteratorAllMolecules();
	        translator = new AtomActionTranslateBy(space);
	        moleculeTranslator = new AtomGroupAction(translator);
		}
		else {
			iterator = new AtomIteratorLeafAtoms();
		}
        if (box != null) {
            iterator.setBox(box);
        }
	}

    private static final long serialVersionUID = 1L;
	private AtomIteratorBoxDependent iterator;
    private AtomActionTranslateBy translator;
    private AtomGroupAction moleculeTranslator;
    private ISpace space;

	private boolean applyToMolecules;
}