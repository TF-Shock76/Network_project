/** GerantClient.java
 *  @author Guillaume Coufourier
 *  @author Théo Crauffon
 *  @author Steven Hue
 */


package project.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import project.server.IServer;
import project.game.*;

public class ClientManager implements Runnable {

	private static long clientId = 0;
	private long id;

	private IServer server;
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private IGame game;
	private Thread thread;

	private String name;
	private double money;
	private int bet;
	private boolean isPlaying;
	private boolean myTurn;
	private boolean blackJack;
	private boolean inQueue;
	private boolean infoReceived;


	/**
	 * Constructeur du gérant de client
	 * @param s le socket propre au client
	 * @param server le serveur auquel le client appartient
	 */
	public ClientManager(Socket s, IServer server) throws IOException {
		id     = clientId++;
		socket = s;
		in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
		money     = 1000; // Possède 1000 golds par default
		isPlaying = false;
		inQueue   = false;
		myTurn    = false;
		blackJack = false;
		game      = null;
		infoReceived = false;
		this.server  = server;
	}

	public void run() {
		System.out.println("Client " + id + " connecté");

		try {
			askName();
			String msg = "";
			do {
				// Déconnection d'urgence
				if(!isConnected()) {
					if(game != null)
						game.emergencyShutDown();

					server.disconnectClient(this);
				}

				if(!inQueue && !infoReceived && !isPlaying) {
					infoReceived = true;
					out.println("A quel jeu voulez-vous jouer ? \n" +
					            "\t (1) Morpion \n" +
								"\t (2) Black-Jack \n" +
								"\t (exit) Pour quitter");
				}

				while(in.ready()) {
					msg = in.readLine();
					infoReceived = false;
				}

				if(msg.equals("exit")) {
					socket.close();
					server.disconnectClient(this);
				}

				if(msg.equals("leave") && inQueue) {
					server.exitQueue(this);
				}

				if(!inQueue && msg != "") {
					server.getInQueue(this, msg);
				}

				// Evite que d'anciens messages écrits avant le tour du joueur viennent
				// entrâver la lecture des messages de celui-ci
				if(!myTurn) {
					msg = "";
				}

				Thread.sleep(100);
			} while (msg != null || !msg.equals("exit"));
			socket.close();
		} catch (IOException e)          { System.out.println("IOException catched"); }
		  catch (InterruptedException e) { System.out.println("InterruptedException catched"); }

		System.out.println(id + " s'est déconnecté");
	}


	private void askName() throws IOException {
		out.println("Votre nom ?");
		// Nom du client (maximum 32 caractères)
		name = in.readLine();
		if (name.length() > 32) {
			name = name.substring(0, 32);
		}
		if (name.equals("exit")) {
			socket.close();
			server.disconnectClient(this);
		}
		// info serveur
		System.out.println("Client " + id + " : " + name);
		thread.setName(thread.getName().replace("null", name));

		out.println("Bienvenue " + name + " !");
	}


	/**
	 * Méthode appelant checkNumbersInString pour demander avec combien d'autres
	 * joueurs le client veut jouer au Black-Jack
	 */
	public int askBJPlayers() { return checkNumbersInString(2, 7); }


	/**
	 * Méthode permettant de vérifier si un nombre entré est bien un nombre
	 * et qu'il est situé dans l'intervale donné en paramètre
	 * @param min la borne inférieure
	 * @param max la borne supérieure
	 * @return nb le nombre converti en int
	 */
	public int checkNumbersInString(int min, int max) {
		int nb = 0;
		try {
			do{
				String entry = in.readLine();
				if(entry.matches("[0-9]+")) {
					nb = Integer.parseInt(entry);
					if(nb < min || nb > max)
						out.println("Erreur, veuillez entrer un nombre entre " + min + " et " + max + " (compris)");
				}
				else
					out.println("Erreur, vous n'avez pas entré un nombre correct");
			}while(nb < min || nb > max);
		}catch(IOException e)          { System.out.println("IOException catched"); }
		 catch(NullPointerException e) { System.out.println("NullPointerException catched"); }

		return nb;
	}

	/**
	 * Méthode permettant de recevoir des messages de l'exétérieur destinées uniquement
	 * au joueur
	 * @param msg le message que doit recevoir le joueur
	 */
	public void receiveMessage(String msg) {
		out.println(msg);
	}

	/**
	 * Méthode mettant fin au thread et détruit le GerantClient
	 */
	public void kill() {
		thread.interrupt();
	}


	/**
	 * Méthode appelée en début de jeu afin de "set" tous les attributs nécessaires
	 */
	public void startPlaying(IGame game)    {
		isPlaying = true ;
		this.game = game;
		inQueue = false;
	}


	/**
	 * Méthode appelée en début de jeu afin de "reset" tous les attributs nécessaires
	 */
	public void endPlaying() {
		isPlaying = false;
		game = null;
		myTurn = false;
	}


  	/**
	 * Setters
	 */

	public void yourTurn()                  { myTurn = true;       }
	public void standby()                   { myTurn = false;      }
	public void enterQueue()                { inQueue = true;      }
	public void exitQueue()                 { inQueue = false;     }
	public void setThread(Thread t )        { thread = t;          }
	public void setBlackJack(Boolean state) { blackJack = state;   }
	public void setBet(int bet)				{ this.bet = bet;      }
	public void earnMoney(double money)     { this.money += money; }


	/**
	 * Getters
	 */

	public boolean isConnected()  { return !socket.isClosed(); }
	public boolean isPlaying()    { return this.isPlaying;  }
	public boolean isInQueue()    { return this.inQueue;    }
	public BufferedReader getBF() { return this.in;         }
	public Socket getSocket()     { return this.socket;     }
	public String getName()       { return this.name;       }
	public long getId()           { return this.id;         }
	public int getBet()           { return this.bet;        }
	public boolean getBlackJack() { return this.blackJack;  }
	public double getMoney()      { return this.money;      }



	public String toString() {
		String s = String.format("Client %d \"%s\"", id, name);
		if (inQueue) {
			s += " en attente d'une partie";
		} else if (game != null) {
			s += " joue : " + game.getInfo();
		}
		return s;
	}
}
