package etomica.virial;

import etomica.atom.AtomArrayList;
import etomica.potential.PotentialPolarizable;


public class ClusterSumPolarizable implements ClusterAbstract, java.io.Serializable {

    /**
     * Constructor for ClusterSum.
     */
    public ClusterSumPolarizable(ClusterBonds[] subClusters, double[] subClusterWeights, MayerFunction[] fArray) {
        if (subClusterWeights.length != subClusters.length) throw new IllegalArgumentException("number of clusters and weights must be the same");
        clusters = new ClusterBonds[subClusters.length];
        clusterWeights = subClusterWeights;
        int pointCount = subClusters[0].pointCount();
        for(int i=0; i<clusters.length; i++) {
            clusters[i] = subClusters[i];
            if(clusters[i].pointCount() != pointCount) throw new IllegalArgumentException("Attempt to construct ClusterSum with clusters having differing numbers of points");
        }
        f = fArray;
        fValues = new double[pointCount][pointCount][fArray.length];
        uijPol = new double[pointCount][pointCount];

        scfAtoms = new AtomArrayList(); // USE THIS LIST FOR ALL ATOMS, WHETHER 3 OR 4; KMB, 8/16/06
    }

    // equal point count enforced in constructor 
    public int pointCount() {
        return clusters[0].pointCount();
    }
    
    public ClusterAbstract makeCopy() {
        ClusterSumPolarizable copy = new ClusterSumPolarizable(clusters,clusterWeights,f);
        copy.setTemperature(1/beta);
        copy.setDeltaDCut(Math.sqrt(deltaDCut2));
        return copy;
    }

    public double value(BoxCluster box) {
        CoordinatePairSet cPairs = box.getCPairSet();
        AtomPairSet aPairs = box.getAPairSet();
        int thisCPairID = cPairs.getID();
//        System.out.println(thisCPairID+" "+cPairID+" "+lastCPairID+" "+value+" "+lastValue+" "+f[0].getClass());
        if (thisCPairID == cPairID) return value;
        if (thisCPairID == lastCPairID) {
            // we went back to the previous cluster, presumably because the last
            // cluster was a trial that was rejected.  so drop the most recent value/ID
            cPairID = lastCPairID;
            value = lastValue;
            return value;
        }
        // a new cluster
        lastCPairID = cPairID;
        lastValue = value;
        cPairID = thisCPairID;
        
        int nPoints = pointCount();
        
        // recalculate all f values for all pairs
        PotentialPolarizable scfPotential = null;
        for(int i=0; i<nPoints-1; i++) {
            for(int j=i+1; j<nPoints; j++) {
                for(int k=0; k<f.length; k++) {
                    fValues[i][j][k] = f[k].f(aPairs.getAPair(i,j),beta);
                    fValues[j][i][k] = fValues[i][j][k];
                    scfPotential = (PotentialPolarizable) f[k].getPotential();
                    if (k==0) uijPol[i][j] = scfPotential.getLastPolarizationEnergy();
                }
            }
        }

        value = 0.0;

        for(int i=0; i<clusters.length; i++) {
            //System.out.println("clusters.length = " + clusters.length);
            // clusters.length = 1 for B3
            double v = clusters[i].value(fValues);
            value += clusterWeights[i] * v;
            //System.out.println("value = " + value);
            //System.out.println("clusterWeights["+i+"] = " + clusterWeights[i]);
        }
        
        if (nPoints == 3) {
            // check that no pair of molecules is overlapped (overlap => gij=0)
            // if any pair is overlapped, then deltaC=0
            double f12 = fValues[0][1][0];
            double f13 = fValues[0][2][0];
            double f23 = fValues[1][2][0];
            double g12 = f12+1; //Math.exp(-beta*u12);
            double g13 = f13+1; //Math.exp(-beta*u13);
            double g23 = f23+1; //Math.exp(-beta*u23);
            if (g12*g13*g23 != 0) {
                // Get a handle on the list of atoms from the AtomPairSet
                scfAtoms.clear();
                scfAtoms.add(box.molecule(0));
                scfAtoms.add(box.molecule(1));
                scfAtoms.add(box.molecule(2));
                double u123Pol = scfPotential.getPolarizationEnergy(scfAtoms);

                //deltaC = Math.exp(-beta*u123) - Math.exp(-beta*(u12 + u13 + u23));
                double deltau123 = u123Pol-(uijPol[0][1] + uijPol[0][2] + uijPol[1][2]);
                double betaU123 = beta*deltau123;
                double expBetaU123;
                if (Math.abs(betaU123) < 1.e-8) {
                    // for small x, exp(-x)-1 ~= -x
                    // for x < 1E-8, the approximation is value within machine precision
                    // for x < 1E-15, exp(-x) is 1, so the approximation is more accurate
                    //   than simply doing the math.
                    expBetaU123 = -betaU123;
                }
                else {
                    expBetaU123 = Math.exp(-beta*deltau123)-1;
                }

                double deltaC = expBetaU123*g12*g13*g23;
                
                // deltaC has to be multiplied by clusterWeights, just like v was multiplied by
                // clusterWeights above to get value
                deltaC = deltaC*clusterWeights[0];

                if (Double.isInfinite(deltaC)) {
                    System.out.println("deltaC = " + deltaC);
                }
                value += deltaC;
                //System.out.println("u12 = " + u12 + ", u13 = " + u13 + ", u23 = " + u23 + ", u123 = " + u123 + ", deltaC = " + deltaC + ", value(before) = " + value);
            }
        }

        else if (nPoints == 4) {
            // deltaD runs into precision problems for long distances
            boolean truncateLong = false;
            for (int i=0; i<nPoints-1; i++) {
                for (int j=i+1; j<nPoints; j++) {
                    if (cPairs.getr2(i,j) > deltaDCut2) {
                        truncateLong = true;
                        break;
                    }
                }
            }

            if (!truncateLong) {
                double f12 = fValues[0][1][0];
                double f13 = fValues[0][2][0];
                double f14 = fValues[0][3][0];
                double f23 = fValues[1][2][0];
                double f24 = fValues[1][3][0];
                double f34 = fValues[2][3][0];
                double g12 = f12+1; //Math.exp(-beta*u12);
                double g13 = f13+1; //Math.exp(-beta*u13);
                double g14 = f14+1; //Math.exp(-beta*u14);
                double g23 = f23+1; //Math.exp(-beta*u23);
                double g24 = f24+1; //Math.exp(-beta*u24);
                double g34 = f34+1; //Math.exp(-beta*u34);

                scfAtoms.clear();
                // we need to properly construct these lists even if we don't use them
                // (due to overlaps) because the next list is obtained by removing/adding
                // atoms from this one.
                scfAtoms.add(box.molecule(0));
                scfAtoms.add(box.molecule(1));
                scfAtoms.add(box.molecule(2));

                double deltaD = 0;
                // if 12 13 or 23 is overlapped, then we can't calculate u123Pol and
                // couldn't calculate the uijPol.  Fortunately, gij is 0, so the 123
                // term is 0.
                if (g12*g13*g23 != 0) {
                    double u123Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                    double deltaU123 = u123Pol - (uijPol[0][1] + uijPol[0][2] + uijPol[1][2]);
                    double beta123 = beta*deltaU123;
                    // for small x, exp(-x)-1 ~= -x
                    // for x < 1E-8, the approximation is value within machine precision
                    // for x < 1E-15, exp(-x) is 1, so the approximation is more accurate
                    //   than simply doing the math.
                    double exp123 = -beta123;
                    if (Math.abs(beta123) > 1E-8) {
                        exp123 = Math.exp(-beta123) - 1;
                    }
                    // original formula has g14+g24+g34-2 instead of f14+f24+f34+1.
                    // Using f should have better precision since
                    // the g's will all be close to 1 (or even equal to 1) for 
                    // systems with molecules having large separations.
                    deltaD += -exp123*g12*g13*g23*((f14+f24+f34)+1);
                }

                scfAtoms.remove(2);
                scfAtoms.add(box.molecule(3));
                if (g12*g14*g24 != 0) {
                    double u124Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                    double deltaU124 = u124Pol-(uijPol[0][1]+uijPol[0][3]+uijPol[1][3]);
                    double beta124 = beta*deltaU124;
                    double exp124 = -beta124;
                    if (Math.abs(beta124) > 1E-8) {
                        exp124 = Math.exp(-beta124) - 1;
                    }
                    deltaD +=  -exp124*g12*g14*g24*((f13+f23+f34)+1);
                }

                scfAtoms.remove(1);
                scfAtoms.add(box.molecule(2));
                if (g13*g14*g34 != 0) {
                    double u134Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                    double deltaU134 = u134Pol-(uijPol[0][2]+uijPol[0][3]+uijPol[2][3]);
                    double beta134 = beta*deltaU134;
                    double exp134 = -beta134;
                    if (Math.abs(beta134) > 1E-8) {
                        exp134 = Math.exp(-beta134) - 1;
                    }
                    deltaD += -exp134*g13*g14*g34*((f12+f23+f24)+1);
                }

                scfAtoms.remove(0);
                scfAtoms.add(box.molecule(1));
                if (g23*g24*g34 != 0) {
                    double u234Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                    double deltaU234 = u234Pol-(uijPol[1][2]+uijPol[1][3]+uijPol[2][3]);
                    double beta234 = beta*deltaU234;
                    double exp234 = -beta234;
                    if (Math.abs(beta234) > 1E-8) {
                        exp234 = Math.exp(-beta234) - 1;
                    }
                    deltaD += -exp234*g23*g24*g34*((f12+f13+f14)+1);
                }

                scfAtoms.add(box.molecule(0));
                if (g12*g13*g14*g23*g24*g34 != 0) {
                    double u1234Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                    // deltaU1234 would have deltaUabc subtracted off, but we'd also add it back
                    // in for expU1234, so just don't subtract in the first place 
                    double deltaU1234 = u1234Pol-(uijPol[0][1]+uijPol[0][2]+uijPol[0][3]+uijPol[1][2]+uijPol[1][3]+uijPol[2][3]); //-deltaU123-deltaU124-deltaU134-deltaU234;
                    double beta1234 = beta*deltaU1234; //deltaU123+deltaU124+deltaU134+deltaU234+deltaU1234);
                    double exp1234 = -beta1234;
                    if (Math.abs(beta1234) > 1E-8) {
                        exp1234 = Math.exp(-beta1234) - 1;
                    }
                    deltaD += exp1234*g12*g13*g14*g23*g24*g34;
                }

                // Mason and Spurling book deltaD; 5/11/07

                // deltaU1234 = u1234-u12-u13-u14-u23-u24-u34;

//          deltaD = 2*Math.exp(-beta*(u12+u13+u23))*(Math.exp(-beta*deltaU123)-1) + 2*Math.exp(-beta*(u12+u14+u24))*(Math.exp(-beta*deltaU124)-1)
//                  + 2*Math.exp(-beta*(u13+u14+u34))*(Math.exp(-beta*deltaU134)-1) + 2*Math.exp(-beta*(u23+u24+u34))*(Math.exp(-beta*deltaU234)-1)
//                  + Math.exp(-beta*(u12+u13+u23-u14))*(1-Math.exp(-beta*deltaU123)) + Math.exp(-beta*(u12+u13+u23-u24))*(1-Math.exp(-beta*deltaU123))
//                  + Math.exp(-beta*(u12+u13+u23-u34))*(1-Math.exp(-beta*deltaU123)) + Math.exp(-beta*(u12+u14+u24-u13))*(1-Math.exp(-beta*deltaU124))
//                  + Math.exp(-beta*(u12+u14+u24-u23))*(1-Math.exp(-beta*deltaU124)) + Math.exp(-beta*(u12+u14+u24-u34))*(1-Math.exp(-beta*deltaU124))
//                  + Math.exp(-beta*(u13+u14+u34-u12))*(1-Math.exp(-beta*deltaU134)) + Math.exp(-beta*(u13+u14+u34-u23))*(1-Math.exp(-beta*deltaU134))
//                  + Math.exp(-beta*(u13+u14+u34-u24))*(1-Math.exp(-beta*deltaU134)) + Math.exp(-beta*(u23+u24+u34-u12))*(1-Math.exp(-beta*deltaU234))
//                  + Math.exp(-beta*(u23+u24+u34-u13))*(1-Math.exp(-beta*deltaU234)) + Math.exp(-beta*(u23+u24+u34-u14))*(1-Math.exp(-beta*deltaU234))
//                  + Math.exp(-beta*(u12+u13+u14+u23+u24+u34))*(Math.exp(-beta*deltaU1234)-1);


//                       kmb added this line 8/16/06
                // deltaD has to be multiplied by weightPrefactor from Standard class, just like deltaC was multiplied by
                // clusterWeights above to get value; note, for B3 clusterWeights = weightPrefactor

                // -(1/8) is the B4 prefactor multiplying all diagrams.
                // coefficient is -(1-4)/4! = -1/8
                deltaD = -0.125*deltaD;
                value += deltaD;
            }
        }
        else if (nPoints == 5) {
            final double f12 = fValues[0][1][0];
            final double f13 = fValues[0][2][0];
            final double f14 = fValues[0][3][0];
            final double f15 = fValues[0][4][0];
            final double f23 = fValues[1][2][0];
            final double f24 = fValues[1][3][0];
            final double f25 = fValues[1][4][0];
            final double f34 = fValues[2][3][0];
            final double f35 = fValues[2][4][0];
            final double f45 = fValues[3][4][0];
            final double g12 = f12+1; //Math.exp(-beta*u12);
            final double g13 = f13+1; //Math.exp(-beta*u13);
            final double g14 = f14+1; //Math.exp(-beta*u14);
            final double g15 = f15+1; //Math.exp(-beta*u14);
            final double g23 = f23+1; //Math.exp(-beta*u23);
            final double g24 = f24+1; //Math.exp(-beta*u24);
            final double g25 = f25+1; //Math.exp(-beta*u14);
            final double g34 = f34+1; //Math.exp(-beta*u34);
            final double g35 = f35+1; //Math.exp(-beta*u14);
            final double g45 = f45+1; //Math.exp(-beta*u14);
            
            double beta123 = Double.NaN;
            double beta124 = Double.NaN;
            double beta125 = Double.NaN;
            double beta134 = Double.NaN;
            double beta135 = Double.NaN;
            double beta145 = Double.NaN;
            double beta234 = Double.NaN;
            double beta235 = Double.NaN;
            double beta245 = Double.NaN;
            double beta345 = Double.NaN;
            
            double deltaE = 0;

            scfAtoms.clear();
            // we need to properly construct these lists even if we don't use them
            // (due to overlaps) because the next list is obtained by removing/adding
            // atoms from this one.
            scfAtoms.add(box.molecule(0));
            scfAtoms.add(box.molecule(1));
            scfAtoms.add(box.molecule(2));  // 123

            if (g12*g13*g23 != 0) {
                double u123Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU123 = u123Pol-uijPol[0][1]-uijPol[0][2]-uijPol[1][2];
                beta123 = deltaU123*beta;
                deltaE += -(1-Math.exp(-beta123))*g12*g13*g23*(2*(f14 + f15 + f24 + f25 + f34 + f35 + 2*f45 + 2)
                        + f14*f25 + f14*f35 + f14*f45 + f15*f24 + f15*f34 + f15*f45
                        + f24*f35 + f24*f45 + f25*f34 + f25*f45 + f34*f45 + f35*f45
                        + 2*f14*f15 + 2*f24*f25 + 2*f34*f35);
            }
            
            scfAtoms.remove(2);
            scfAtoms.add(box.molecule(3));  // 124

            if (g12*g14*g24 != 0) {
                double u124Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU124 = u124Pol-uijPol[0][1]-uijPol[0][3]-uijPol[1][3];
                beta124 = deltaU124*beta;
                deltaE += -(1-Math.exp(-beta124))*g12*g14*g24*(2*(f13 + f15 + f23 + f25 + f34 + f45 + 2*f35 + 2)
                        + f13*f25 + f13*f35 + f13*f45 + f15*f23 + f15*f34 + f15*f35
                        + f23*f35 + f23*f45 + f25*f34 + f25*f35 + f34*f35 + f35*f45
                        + 2*f13*f15 + 2*f23*f25 + 2*f34*f45);
            }

            scfAtoms.remove(2);
            scfAtoms.add(box.molecule(4));  // 125

            if (g12*g15*g25 != 0) {
                double u125Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU125 = u125Pol-uijPol[0][1]-uijPol[0][4]-uijPol[1][4];
                beta125 = deltaU125*beta;
                deltaE += -(1-Math.exp(-beta125))*g12*g15*g25*(2*(f13 + f14 + f23 + f24 + 2*f34 + f45 + f35 + 2)
                        + f13*f24 + f13*f34 + f13*f45 + f14*f23 + f14*f34 + f14*f35
                        + f23*f34 + f23*f45 + f24*f34 + f24*f35 + f35*f34 + f45*f34
                        + 2*f13*f14 + 2*f23*f24 + 2*f35*f45);
            }

            scfAtoms.remove(1);
            scfAtoms.add(box.molecule(2));  // 153
            
            if (g13*g15*g35 != 0) {
                double u135Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU135 = u135Pol-uijPol[0][2]-uijPol[0][4]-uijPol[2][4];
                beta135 = deltaU135*beta;
                deltaE += -(1-Math.exp(-beta135))*g13*g15*g35*(2*(f12 + f14 + f23 + f25 + f34 + f45 + 2*f24 + 2)
                        + f12*f24 + f12*f34 + f12*f45 + f14*f23 + f14*f25 + f14*f24
                        + f23*f24 + f23*f45 + f25*f24 + f25*f34 + f24*f34 + f24*f45
                        + 2*f12*f14 + 2*f23*f34 + 2*f25*f45);
                
                if (!Double.isNaN(124)) {
                    deltaE += (1-Math.exp(-beta124-beta135))*g12*g14*g24*g13*g15*g35;
                }

            }

            scfAtoms.remove(1);
            scfAtoms.add(box.molecule(3));  // 134
            
            if (g13*g14*g34 != 0) {
                double u134Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU134 = u134Pol-uijPol[0][2]-uijPol[0][3]-uijPol[2][3];
                beta134 = deltaU134*beta;
                deltaE += -(1-Math.exp(-beta134))*g13*g14*g34*(2*(f12 + f15 + f23 + f24 + f35 + f45 + 2*f25 + 2)
                        + f12*f25 + f12*f35 + f12*f45 + f15*f23 + f15*f24 + f15*f25
                        + f23*f25 + f23*f45 + f24*f25 + f24*f35 + f25*f35 + f25*f45
                        + 2*f12*f15 + 2*f23*f35 + 2*f24*f45);

                if (!Double.isNaN(beta125)) {
                    deltaE += (1-Math.exp(-beta125-beta134))*g12*g15*g25*g13*g14*g34;
                }
            }

            scfAtoms.remove(1);
            scfAtoms.add(box.molecule(4));  // 145

            if (g14*g15*g45 != 0) {
                double u145Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU145 = u145Pol-uijPol[0][4]-uijPol[0][3]-uijPol[3][4];
                beta145 = deltaU145*beta;
                deltaE += -(1-Math.exp(-beta145))*g14*g15*g45*(2*(f12 + f13 + f24 + f25 + f34 + f35 + 2*f23 + 2)
                        + f12*f23 + f12*f34 + f12*f35 + f13*f24 + f13*f25 + f13*f23
                        + f23*f24 + f24*f35 + f25*f23 + f25*f34 + f23*f34 + f23*f35
                        + 2*f12*f13 + 2*f24*f34 + 2*f25*f35);

                if (!Double.isNaN(beta123)) {
                    deltaE += (1-Math.exp(-beta123-beta145))*g12*g13*g23*g14*g15*g45;
                }
            }

            scfAtoms.remove(0);
            scfAtoms.add(box.molecule(1));  // 452
            
            if (g24*g25*g45 != 0) {
                double u245Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU245 = u245Pol-uijPol[1][4]-uijPol[1][3]-uijPol[3][4];
                beta245 = deltaU245*beta;
                deltaE += -(1-Math.exp(-beta245))*g24*g25*g45*(2*(f12 + f14 + f15 + f23 + f34 + f35 + 2*f13 + 2)
                        + f12*f13 + f12*f34 + f12*f35 + f14*f23 + f14*f13 + f14*f35
                        + f15*f13 + f15*f23 + f15*f34 + f13*f23 + f13*f34 + f13*f35
                        + 2*f12*f23 + 2*f14*f34 + 2*f15*f35);

                if (!Double.isNaN(beta123)) {
                    deltaE += (1-Math.exp(-beta123-beta245))*g12*g13*g23*g24*g25*g45;
                }
                if (!Double.isNaN(beta134)) {
                    deltaE += (1-Math.exp(-beta134-beta245))*g13*g14*g34*g24*g25*g45;
                }
                if (!Double.isNaN(beta135)) {
                    deltaE += (1-Math.exp(-beta135-beta245))*g13*g15*g35*g24*g25*g45;
                }
            }

            scfAtoms.remove(1);
            scfAtoms.add(box.molecule(2));  // 423
            
            if (g23*g24*g34 != 0) {
                double u234Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU234 = u234Pol-uijPol[1][2]-uijPol[1][3]-uijPol[2][3];
                beta234 = deltaU234*beta;
                deltaE += -(1-Math.exp(-beta234))*g23*g24*g34*(2*(f12 + f13 + f14 + f25 + f35 + f45 + 2*f15 + 2)
                        + f12*f15 + f12*f35 + f12*f45 + f13*f25 + f13*f15 + f13*f45
                        + f14*f15 + f14*f25 + f14*f35 + f15*f25 + f15*f35 + f15*f45
                        + 2*f12*f25 + 2*f13*f35 + 2*f14*f45);

                if (!Double.isNaN(beta125)) {
                    deltaE += (1-Math.exp(-beta125-beta234))*g12*g15*g25*g23*g24*g34;
                }
                if (!Double.isNaN(beta135)) {
                    deltaE += (1-Math.exp(-beta135-beta234))*g13*g15*g35*g23*g24*g34;
                }
                if (!Double.isNaN(beta145)) {
                    deltaE += (1-Math.exp(-beta145-beta234))*g14*g15*g45*g23*g24*g34;
                }
            }

            scfAtoms.remove(0);
            scfAtoms.add(box.molecule(4));  // 235
            
            if (g23*g25*g35 != 0) {
                double u235Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU235 = u235Pol-uijPol[1][2]-uijPol[1][4]-uijPol[2][4];
                beta235 = deltaU235*beta;
                deltaE += -(1-Math.exp(-beta235))*g23*g25*g35*(2*(f12 + f13 + f15 + f24 + f34 + f45 + 2*f14 + 2)
                        + f12*f14 + f12*f34 + f12*f45 + f13*f24 + f13*f14 + f13*f45
                        + f15*f14 + f15*f24 + f15*f34 + f14*f24 + f14*f34 + f14*f45
                        + 2*f12*f24 + 2*f13*f34 + 2*f15*f45);

                if (!Double.isNaN(beta124)) {
                    deltaE += (1-Math.exp(-beta124-beta235))*g12*g14*g24*g23*g25*g35;
                }
                if (!Double.isNaN(beta134)) {
                    deltaE += (1-Math.exp(-beta134-beta235))*g13*g14*g34*g23*g25*g35;
                }
                if (!Double.isNaN(beta145)) {
                    deltaE += (1-Math.exp(-beta145-beta235))*g14*g15*g45*g23*g25*g35;
                }
            }

            scfAtoms.remove(0);
            scfAtoms.add(box.molecule(3));  // 354
            
            if (g34*g35*45 != 0) {
                double u345Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU345 = u345Pol-uijPol[2][3]-uijPol[2][4]-uijPol[3][4];
                beta345 = deltaU345*beta;
                deltaE += -(1-Math.exp(-beta345))*g34*g35*g45*(2*(f13 + f14 + f15 + f23 + f24 + f25 + 2*f12 + 2)
                        + f13*f12 + f13*f24 + f13*f25 + f14*f23 + f14*f12 + f14*f25
                        + f15*f12 + f15*f23 + f15*f24 + f12*f23 + f12*f24 + f12*f25
                        + 2*f13*f23 + 2*f14*f24 + 2*f15*f25);
                
                if (!Double.isNaN(beta123)) {
                    deltaE += (1-Math.exp(-beta123-beta345))*g12*g13*g23*g34*g35*g45;
                }
                if (!Double.isNaN(beta124)) {
                    deltaE += (1-Math.exp(-beta124-beta345))*g12*g14*g24*g34*g35*g45;
                }
                if (!Double.isNaN(beta125)) {
                    deltaE += (1-Math.exp(-beta125-beta345))*g12*g15*g25*g34*g35*g45;
                }
            }

            scfAtoms.add(box.molecule(0));  // 3541
            if (g13*g14*g15*g34*g35*g45 != 0) {
                double u1345Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU1345 = u1345Pol-uijPol[0][2]-uijPol[0][3]-uijPol[0][4]-uijPol[2][3]-uijPol[2][4]-uijPol[3][4];
                double beta1345 = deltaU1345*beta;
                deltaE += (1-Math.exp(-beta1345))*g13*g14*g15*g34*g35*g45*(f12 + f23 + f24 + f25 + 1);
            }

            scfAtoms.remove(1);
            scfAtoms.add(box.molecule(1));  // 3412
            if (g12*g13*g14*g23*g24*g34 != 0) {
                double u1234Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU1234 = u1234Pol-uijPol[0][1]-uijPol[0][2]-uijPol[0][3]-uijPol[1][2]-uijPol[1][3]-uijPol[2][3];
                double beta1234 = deltaU1234*beta;
                deltaE += (1-Math.exp(-beta1234))*g12*g13*g14*g23*g24*g34*(f15 + f25 + f35 + f45 + 1);
            }

            scfAtoms.remove(1);
            scfAtoms.add(box.molecule(4));  // 3125
            if (g12*g13*g15*g23*g25*g35 != 0) {
                double u1235Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU1235 = u1235Pol-uijPol[0][1]-uijPol[0][2]-uijPol[0][4]-uijPol[1][2]-uijPol[1][4]-uijPol[2][4];
                double beta1235 = deltaU1235*beta;
                deltaE += (1-Math.exp(-beta1235))*g12*g13*g15*g23*g25*g35*(f14 + f24 + f34 + f45 + 1);
            }

            scfAtoms.remove(0);
            scfAtoms.add(box.molecule(3));  // 1254
            if (g12*g14*g15*g24*g25*g45 != 0) {
                double u1245Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU1245 = u1245Pol-uijPol[0][1]-uijPol[0][3]-uijPol[0][4]-uijPol[1][3]-uijPol[1][4]-uijPol[3][4];
                double beta1245 = deltaU1245*beta;
                deltaE += (1-Math.exp(-beta1245))*g12*g14*g15*g24*g25*g45*(f13 + f23 + f34 + f35 + 1);
            }

            scfAtoms.remove(0);
            scfAtoms.add(box.molecule(2));  // 2543
            if (g23*g24*g25*g34*g35*g45 != 0) {
                double u2345Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU2345 = u2345Pol-uijPol[1][2]-uijPol[1][3]-uijPol[1][4]-uijPol[2][3]-uijPol[2][4]-uijPol[3][4];
                double beta2345 = deltaU2345*beta;
                deltaE += (1-Math.exp(-beta2345))*g23*g24*g25*g34*g35*g45*(f12 + f13 + f14 + f15 + 1);
            }

            scfAtoms.add(box.molecule(0));  // 25431
            if (g12*g13*g14*g15*g23*g24*g25*g34*g35*g45 != 0) {
                double u12345Pol = scfPotential.getPolarizationEnergy(scfAtoms);
                double deltaU12345 = u12345Pol-uijPol[0][1]-uijPol[0][2]-uijPol[0][3]-uijPol[0][4]-uijPol[1][2]-uijPol[1][3]-uijPol[1][4]
                                              -uijPol[2][3]-uijPol[2][4]-uijPol[3][4];
                double beta12345 = deltaU12345*beta;
                deltaE += -(1-Math.exp(-beta12345))*g12*g13*g14*g15*g23*g24*g25*g34*g35*g45;
            }

            //System.out.println("deltaE = " + deltaE);
            // coefficient is -(1-5)/5! = -1/30
            value += -deltaE/30.0;
        }

        return value;
    }
    
    public ClusterBonds[] getClusters() {return clusters;}
    /**
     * @return Returns the temperature.
     */
    public double getTemperature() {
        return 1/beta;
    }
    /**
     * @param temperature The temperature to set.
     */
    public void setTemperature(double temperature) {
        beta = 1/temperature;
    }
    
    public void setDeltaDCut(double newDeltaDCut) {
        deltaDCut2 = newDeltaDCut*newDeltaDCut;
    }
    
    public double getDeltaDCut() {
        return Math.sqrt(deltaDCut2);
    }
    
    private static final long serialVersionUID = 1L;
    private final ClusterBonds[] clusters;
    private final double[] clusterWeights;
    private final MayerFunction[] f;
    private final double[][][] fValues;
    private int cPairID = -1, lastCPairID = -1;
    private double value, lastValue;
    private double beta;
    protected final AtomArrayList scfAtoms;
    protected double deltaDCut2 = Double.POSITIVE_INFINITY;
    protected final double[][] uijPol;
}
