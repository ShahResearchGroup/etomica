/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial;

/**
 * @author andrew
 *
 * Clusters implementing ClusterWeight must return positive "values"
 */
public interface ClusterWeight extends ClusterAbstract {

    public interface Factory {
        public ClusterWeight makeWeightCluster(ClusterAbstract[] clusters);
    }
    
}
