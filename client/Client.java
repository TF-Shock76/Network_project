/** Client.java
 *  @author Guillaume Coufourier
 *  @author Théo Crauffon
 *  @author Steven Hue
 */


package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private Scanner sc;


	/**
	 * Constructeur du client
	 * Initialise l'input, l'output, et la gestion du ShutDown en cas de CTRL+C
	 * @param s le socket propre au client
	 */
	public Client(Socket s) {
		// Ajoute une action effectuée lors d'un ctrl+c
		try {
			Runtime.getRuntime().addShutdownHook(new ShutdownThread(this));
		} catch (Throwable t) {
		}

		try{
			socket = s;
			in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);
			sc  = new Scanner(System.in);
			String entry = null;

			Thread readOnly = new Thread(new Read(), "Client reading");
			readOnly.start();

			while(true) {
				try{
					entry = sc.next();
					out.println(entry);
					if(entry.toLowerCase().equals("exit")) {
						kill();
					}
					Thread.sleep(100);
				}catch (InterruptedException e) {}

			}

		} catch(IOException e) { System.out.println("Erreur"); }
	}

	public void stopThread() {
		try {
			out.println("exit");
			socket.close();
		} catch(IOException e) {
			System.out.println("Erreur pendant la déconnexion");
		}

	}

	private void kill() {
		try{
			socket.close();
			System.exit(1);
		}catch(IOException e) {}
	}

	/**
	 * Classe privée dédiée au thread d'écriture du client
	 * Permet de ne pas être gêné par des attentes bloquantes sur la lecture
	 */
	private class Read implements Runnable {
		public void run() {
			String serverMsg;
			while(true) {
				try{
					while(in.ready()) {
						serverMsg = in.readLine();
						System.out.println(serverMsg);
						// message envoyé par le serveur lorsqu'il ferme.
						if ("Le serveur va close".equals(serverMsg)) {
							kill();
						}
					}
					Thread.sleep(100);
				} catch(IOException e) {
					System.out.println("Problème de lecture des données du serveur");
				} catch(InterruptedException e) {

				}
			}
		}
	}


	/**
	 * Main du Client
	 * @param args 0 = nom du serveur, 1 = port d'écoute du serveur
	 */
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Erreur dans les arguments, veuillez relancer le programme avec les bons arguments");
			System.out.println("Usage : nom_de_serveur port");
			System.exit(1);
		}

		Socket socket = null;
		try {
			socket = new Socket(args[0], Integer.parseInt(args[1]));
		} catch (NumberFormatException e) {
			System.out.println(args[1] + " n'est pas un numéro de port.");
			System.exit(2);
		} catch (IOException e) {
			System.out.println("Pas de réponse du serveur"); // timeout
			System.exit(1);
		}

		new Client(socket);
	}
}
