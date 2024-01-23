/** SimpleServer.java
 *  @author Guillaume Coufourier
 *  @author Théo Crauffon
 *  @author Steven Hue
 */


package project.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import project.client.ClientManager;
import project.game.IGame;
import project.game.Morpion;
import project.game.BlackJack;

public class SimpleServer implements IServer {

	/**
	 * Instance unique du serveur.
	 */
	private static SimpleServer instance;

	private static Random random = new Random();

	/**
	 * Contient l'ensemble des clients connectés au serveur.
	 */
	ArrayList<ClientManager> clients;

	/**
	 * Contient les clients n'étant pas actuellement en partie.
	 * Chaque ArrayList correspond à une queue, et est reliée à une clé correspondant au jeu
	 */
	HashMap<String, ArrayList<ClientManager>> queues;
	//ArrayList<GerantClient> queue;

	private ServerSocket ss;

	private SimpleServer() throws IOException {
		clients = new ArrayList<ClientManager>();
		queues  = new HashMap<String, ArrayList<ClientManager>>();
		queues.put("Morpion2p", new ArrayList<ClientManager>());
		queues.put("Black-Jack2p", new ArrayList<ClientManager>());
		queues.put("Black-Jack3p", new ArrayList<ClientManager>());
		queues.put("Black-Jack4p", new ArrayList<ClientManager>());
		queues.put("Black-Jack5p", new ArrayList<ClientManager>());
		queues.put("Black-Jack6p", new ArrayList<ClientManager>());
		queues.put("Black-Jack7p", new ArrayList<ClientManager>());
		ss = new ServerSocket(6000);
	}

	/**
	 * Méthode demarrant le serveur.
	 */
	private void launch() {
		listen(); // démarre thread écoute
		matchmaking(); // démarre thread matchmaking
		ServerCommands.initialize(this);
		home(); // boucle principale de l'administration du serveur
	}

	/**
	 * Méthode instanciant le thread d'écoute
	 */
	private void listen() {
		Thread listenThread = new Thread(new Listen(), "Listening thread");
		listenThread.start();
	}

	/**
	 * Méthode instanciant le thread de matchmaking
	 */
	private void matchmaking() {
		Thread matchmakingThread = new Thread(new Matchmaking(), "Matchmaking thread");
		matchmakingThread.start();
	}

	/**
	 * Gère l'administration et les commandes du serveur.
	 */
	private void home() {
		String entry;
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);

		System.out.println("Administration du serveur - tapez \"help\" pour l'aide.");
		while(true) {
			entry = sc.nextLine();
			ServerCommands.executeCommand(entry);
		}
	}

	/**
	 * Envoie un message aux clients.
	 * @param msg Le message à envoyer.
	 * @param clients Les clients à qui envoyer le message.
	 */
	public void sendMessage(String msg, ClientManager... clients) {
		if (clients == null) {
			return;
		}

		for (ClientManager c : clients) {
			if (c.isConnected()) {
				c.receiveMessage(msg);
			}
		}
	}

	/**
	 * Place le client dans la bonne queue en fonction du jeu voulu
	 * @param cli le client à placer
	 * @param game le jeu auquel le client veut jouer, sous forme de String
	 */
	public void getInQueue(ClientManager cli, String game) {
		if(cli.isInQueue() || cli.isPlaying())
			return;

		switch(game) {
			case "1" : // 1 = Morpion
				queues.get("Morpion2p").add(cli);
				cli.enterQueue();
				break;
			case "2" :
				// Demande au joueur dans quelle room il veut jouer
				cli.receiveMessage("Avec combien de joueurs voulez-vous jouer ? (2-7)");
				int nbPlayers = 0;

				// Sécurité afin de bien déconnecter et de ne pas afficher de message d'exception en cas
				// de ctrl+c pendant la demande de joueurs
				try{
					nbPlayers = cli.askBJPlayers();

					// Affecte le joueur à la bonne room
					queues.get("Black-Jack"+nbPlayers+"p").add(cli);
					cli.enterQueue();
				}catch(NullPointerException e) {disconnectClient(cli); break;}

				break;
			default :
				cli.receiveMessage("Votre entrée correspond à aucun jeu disponible");
				break;
		}
	}

	/**
	 * Permet au client de sortir de la queue s'il ne veut plus jouer et qu'il attendait
	 * une partie
	 * @param cli le client à retirer de la queue
	 */
	public void exitQueue(ClientManager cli) {
		if(cli.isInQueue()) {
			for (ArrayList<ClientManager> listeClient : queues.values()) {
				if (listeClient.remove(cli)) {
					cli.exitQueue();
					cli.receiveMessage("Vous avez quitté la queue.");
					break;
				}
			}
		} else {
			cli.receiveMessage("Vous n'êtes pas actuellement dans une queue");
		}
	}

	/**
	 * Déconnecte un client.
	 * Cette méthode n'est appelée que par le client lui-même ou le jeu.
	 * @param cli le client à retirer de la queue
	 */
	public void disconnectClient(ClientManager cli)  {
		cli.kill();
		clients.remove(cli);
	}


	/**
	 * Renvoie l'instance serveur, ou en créé une si elle n'existe pas
	 */
	public static SimpleServer getInstance() {
		if (instance == null) {
			try {
				instance = new SimpleServer();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		return instance;
	}

	/**
	 * Classe privée du thread d'écoute du serveur
	 */
	private class Listen implements Runnable {
		public void run() {
			try {
				while (true) {
					Socket s = ss.accept();
					ClientManager cli = new ClientManager(s, getInstance());
					clients.add(cli);
					Thread t = new Thread(cli, String.format("Client %d \"%s\"",
						cli.getId(), cli.getName()));
					cli.setThread(t);
					t.start();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	/**
	 * Classe privée du thread de matchmaking du serveur
	 */
	private class Matchmaking implements Runnable {
		public void run() {
			while (true) {
				// Vérification que tous les sockets présent dans l'ArrayList sont connectés, par mesure de sécurité
				// Efface les sockets "fantômes" de personnes qui se seraient "furieusement" déconnectées
				for (ClientManager c : clients) {
					if(c.getSocket().isClosed()) {
						disconnectClient(c);
						continue;
					}
				}

				// On affiche au client, à intervales réguliers, dans quelle queue il est
				for (String jeu : queues.keySet()) {
					for(ClientManager cli : queues.get(jeu)) {
						cli.receiveMessage("Tu es dans la queue pour un " + jeu +
						" - tape \"leave\" pour sortir de la queue.");
					}
				}

				// Créé et attribue une instance de jeu aux joueurs attendant dans la queue
				// si le nombre de joueurs requis est atteint
				for (String gameKey : queues.keySet()) {
					ArrayList<ClientManager> gameQueue = queues.get(gameKey);
					String gameName = "";
					int nbPlayersRequired = 0;

					// Boucle permettant de récupérer le nom du jeu ainsi que le nombre de joueurs requis
					// Ex : Black-Jack3p = jeu "Black-Jack" et "3" joueurs
					for(int i = 0;i < gameKey.length(); i++) {
						if(Character.isDigit(gameKey.charAt(i))) {
							nbPlayersRequired = Integer.parseInt(gameKey.charAt(i)+"");
							break;
						}
						gameName += gameKey.charAt(i);
					}

					// Tant qu'il reste assez de joueurs dans la queue, on créé une nouvelle instance
					// de jeu et on fait jouer les joueurs.
					while(gameQueue.size() >= nbPlayersRequired) {
						ClientManager[] players = new ClientManager[nbPlayersRequired];
						IGame game = null;
						for(int i = 0; i < nbPlayersRequired; i++) {
							players[i] = gameQueue.remove(random.nextInt(gameQueue.size()));
						}

						switch(gameName) {
							case "Morpion":
								game = new Morpion(getInstance(), players);
								break;
							case "Black-Jack" :
								game = new BlackJack(getInstance(), players);
								break;
							default:
								continue;
						}
						for(int i = 0; i < players.length; i++)
							players[i].startPlaying(game);

						// Lancement du thread associé au jeu
						Thread tGame = new Thread(game, gameName + game.getInstanceNumber());
						tGame.start();
						System.out.println("Lancement de " + tGame.getName() + " avec clients " +
							players[0].getId() + " et " + players[1].getId());
					}
				}
				try {
					// Le matchmaking s'actualise toutes les 5s
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}


	/**
	 * Classe privée réunissant toutes les commandes que l'administrateur du serveur
	 * peut demander
	 */
	private static class ServerCommands {
		private static final String nl = "\n\t";
		private static SimpleServer server;

		private static void initialize(SimpleServer server) {
			if (ServerCommands.server == null) {
				ServerCommands.server = server;
			}
		}

		private static void executeCommand(String entry) {
			switch (entry) {
			case "threads":
				commandThreads();
				break;
			case "clients":
				commandClients();
				break;
			case "queue":
				commandQueue();
				break;
			case "cpu":
				commandCpu();
				break;
			case "close":
			case "q":
			case "exit":
				commandExit();
				break;
			case "help":
				commandHelp();
				break;
			default:
				System.out.println(entry + " n'est pas une commande. Tapez help pour l'aide.");
				break;
			}
		}

		private static void commandHelp() {
			System.out.println(
				"Liste des commandes : " + nl +
				"threads - Affiche les threads actifs." + nl +
				"clients - Affiche les clients connectés." + nl +
				"queue - Affiche l'état de la queue." + nl +
				"cpu - Affiche l'utilisation du CPU." + nl +
				"exit - Ferme le serveur." + nl +
				"help - Affiche ce message."
			);
		}

		/**
		 * Méthode listant les threads en cours créés par le serveur
		 * S'ignore lui-même
		 */
		private static void commandThreads() {
			System.out.println("Liste des threads actifs : " + (Thread.activeCount()-3)	);
			for (Thread t : Thread.getAllStackTraces().keySet()) {
				String name = t.getName();
				if (name.matches(".*\\d.*"))
					System.out.println(t.getName());
			}
		}

		/**
		 * Méthode permettant de tester la consommation du programme sur le CPU
		 * permet de détecter des boucles while(true) trop consommatrice ou d'autres problèmes
		 */
		private static void commandCpu() {
			try {
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				ObjectName name = ObjectName.getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
				AttributeList list = mbs.getAttributes(name, new String[] { "ProcessCpuLoad" });

				Attribute att = (Attribute) list.get(0);
				Double value = (Double) att.getValue();

				System.out.format("CPU Usage : %.1f %%\n", (value * 100));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		private static void commandClients() {
			for (ClientManager c : server.clients) {
				System.out.println(c);
			}
		}

		private static void commandQueue() {
			for (Map.Entry<String, ArrayList<ClientManager>> gameName : server.queues.entrySet()) {
				System.out.println(gameName.getKey());
				ArrayList<ClientManager> listClients = gameName.getValue();
				if (listClients.isEmpty()) {
					System.out.println("\tvide");
				} else {
					listClients.forEach(client -> System.out.println("\t" + client));
				}
			}
		}

		private static void commandExit() {
			System.out.println("Fermeture du serveur");
			for (ClientManager cl : server.clients) {
				// Déconnecte les clients
				cl.receiveMessage("Le serveur va close");
			}
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		SimpleServer.getInstance().launch();
	}
}
