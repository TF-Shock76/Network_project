/** ShutdownThread.java
 *  @author Guillaume Coufourier
 *  @author Théo Crauffon
 *  @author Steven Hue
 */

package client;

/**
 * Classe permettant d'éviter de laisser des threads actifs
 * chez le serveur quand le client se déconnecte, notamment lorsqu'il CTRL+C
 */
class ShutdownThread extends Thread {
	private Client client;

	public ShutdownThread(Client client) {
		super();
		this.client = client;
	}

	public void run() {
		client.stopThread();
	}
}
