/** IGame.java
 *  @author Guillaume Coufourier
 *  @author Théo Crauffon
 *  @author Steven Hue
 */


package project.game;

public interface IGame extends Runnable {

	public void emergencyShutDown();

	public default int getInstanceNumber() {
		return 0;
	}

	public default String getInfo() {
		return getClass().getSimpleName() + " " + getInstanceNumber();
	}

}
