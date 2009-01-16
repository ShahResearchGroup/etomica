package etomica.api;

import etomica.exception.ConfigurationOverlapException;

public interface IIntegrator {

    /**
     * Performs the elementary integration step, such as a molecular dynamics
     * time step, or a Monte Carlo trial.
     */
    public void doStep();

    /**
     * Returns the number of steps performed by the integrator since it was
     * initialized.
     */
    public long getStepCount();

    /**
     * Defines the actions taken by the integrator to reset itself, such as
     * required if a perturbation is applied to the simulated box (e.g.,
     * addition or deletion of a molecule). Also invoked when the
     * integrator is started or initialized.
     */
    public void reset() throws ConfigurationOverlapException;

    /**
     * This method resets the step counter.
     */
    public void resetStepCount();

    /**
     * Returns true if initialize method has been called.
     */
    public boolean isInitialized();

    /**
     * Adds the given interval action to those that receive interval events
     * fired by this integrator.  Do not add an interval action that is already
     * an interval listener to this integrator.  Do not add a null listener.
     */
    public void addIntervalAction(IAction newIntervalAction);

    /**
     * Sets the "priority" of the given intervalAction (which must have already
     * been added).  The priority determines the order in which interval
     * actions are performed.  Low priorities are fired first and high
     * priorities are fired last.  Priorities can range from 0 to 200.  If PBC
     * are enforced, they are enforced with a priority of 100.  Interval
     * actions with the same priority may be fired in any order.
     */
    public void setIntervalActionPriority(IAction intervalAction, int newPriority);

    /**
     * Sets the number of integration steps between calls to the
     * intervalAction's actionPerformed method.  The interval must be positive.
     */
    public void setActionInterval(IAction intervalAction, int newInterval);

    /**
     * Removes given interval action from those notified of interval events
     * fired by this integrator.  Do not try to remove an interval action that
     * is not a listener to this integrator.
     */
    public void removeIntervalAction(IAction intervalAction);

    /**
     * Returns the number of interval actions held by the integrator.
     */
    public int getIntervalActionCount();

    /**
     * Returns the requested interval action.
     */
    public IAction getIntervalAction(int index);
}