/** IServer.java
 *  @author Guillaume Coufourier
 *  @author Théo Crauffon
 *  @author Steven Hue
 */


package project.server;

import project.client.ClientManager;

public interface IServer {

	public void disconnectClient(ClientManager c);

	public void getInQueue(ClientManager cli, String game);

	public void exitQueue(ClientManager cli);
}
