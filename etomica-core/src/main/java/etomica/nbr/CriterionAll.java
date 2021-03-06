/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.nbr;

import etomica.atom.IAtom;
import etomica.box.Box;

/**
 * Specifies that all atoms pairs are to be considered neighbors.  Should
 * not be used for species in which atoms are being added/removed by integrator.
 */
public class CriterionAll implements NeighborCriterion {

    /**
     * Always returns false, indicating that neighbor list never needs updating.
     * This is appropriate if atoms are never added to or removed from box,
     * because all atoms are always on neighbor list.
     */
    public boolean needUpdate(IAtom atom) {
        return false;
    }

    /**
     * Performs no action.
     */
    public void setBox(Box box) {
    }

    /**
     * Always returns false, indicating that neighbor list never needs updating.
     * This is appropriate if atoms are never added to or removed from box,
     * because all atoms are always on neighbor list.
     */
    public boolean unsafe() {
        return false;
    }

    /**
     * Performs no action.
     */
    public void reset(IAtom atom) {
    }

    /**
     * Always returns true, indicating that all atoms pairs are neighbors.
     *
     * @param atom1
     * @param atom2
     */
    public boolean accept(IAtom atom1, IAtom atom2) {
        return true;
    }

}
