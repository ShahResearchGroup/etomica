package etomica.graph.property;


import etomica.graph.model.Graph;
import etomica.graph.traversal.DepthFirst;
import etomica.graph.traversal.Traversal;

public class IsConnected implements Property {

  private Traversal dfTraversal;

  public IsConnected() {

    this.dfTraversal = new DepthFirst();
  }

  public boolean check(Graph graph) {

    // by definition, a null graph and a singleton graph are connected
    if (graph.getNodeCount() <= 1) {
      return true;
    }
    // invariant: a connected graph has at least N-1 edges
    if (graph.getEdgeCount() < (graph.getNodeCount() - 1)) {
      return false;
    }
    // traverse the graph starting at nodeID and return true IFF all nodes in the graph
    // are traversed in the same connected component
    return dfTraversal.traverseComponent((byte) 0, graph, null);
  }
}