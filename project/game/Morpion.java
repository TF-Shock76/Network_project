/** Morpion.java
 *  @author Guillaume Coufourier
 *  @author Théo Crauffon
 *  @author Steven Hue
 */


package project.game;

import java.io.IOException;

import project.client.ClientManager;
import project.server.SimpleServer;

public class Morpion implements IGame {

	/**
	 * Incrémenté à chaque nouvelle instance de jeu créée.
	 */
	private static int instances;
	private final int instanceNumber;

	private static final int NB_PLAYERS = 2;
	private static final char[] symbol = new char[] {'O', 'X'};
	private static final String[] prompt = new String[] {"Ligne   : ", "Colonne : "};

	private static final int END_NORMAL = 0;
	private static final int END_TURNS  = 1;
	private static final int END_ERROR  = 2;

	private SimpleServer server;
	private ClientManager[] players;
	private char[][] grid;
	private int currentPlayer;
	private int turnCounter = 0;

	public Morpion(SimpleServer server, ClientManager[] players) {
		if (players == null) {
			throw new NullPointerException();
		}
		if (players.length != NB_PLAYERS) {
			throw new IllegalArgumentException("Le jeu se joue avec 2 players.");
		}

		this.instanceNumber = ++instances;
		this.server = server;
		this.players = players;
		grid = new char[3][3];
	}


	public void run() {
			int gameEndReason = -1;
			startGame();
			try {
				gameEndReason = play();
			} catch (IOException e) {
				e.printStackTrace();
			}
			end(gameEndReason);
		}


	/**
	 * Lance la partie. Détermine au hasard la personne qui commence.
	 */
	private void startGame() {
		for (int i = 0; i < grid.length; i++) {
			for (int j = 0; j < grid[i].length; j++) {
				grid[i][j] = ' ';
			}
		}
		players[0].receiveMessage("Vous êtes le joueur 1 (O)");
		players[1].receiveMessage("Vous êtes le joueur 2 (X)");
		currentPlayer = (int)(Math.random() * NB_PLAYERS);
		server.sendMessage("Le joueur " + (currentPlayer + 1) + " commence", players);
	}


	/**
	 * Boucle du jeu.
	 */
	private int play() throws IOException {
		server.sendMessage(getGrid(), players);
		while (!isGameOver()) {
			// demander réponse du joueur
			server.sendMessage("Au tour du joueur " + (currentPlayer + 1), players);
			players[(1-currentPlayer)].standby();
			server.sendMessage("Votre tour", players[currentPlayer]);
			players[currentPlayer].yourTurn();

			int[] coords = getCoords(players[currentPlayer]);
			// cas déconnexion
			if (coords == null) {
				return END_ERROR;
			}
			grid[coords[0]][coords[1]] = symbol[currentPlayer];
			// on change de tour
			currentPlayer = 1 - currentPlayer;
			server.sendMessage(getGrid(), players);
			if (++turnCounter == 9 && !isGameOver()) {
				return END_TURNS;
			}
		}
		return END_NORMAL;
	}


	/**
	 * Retour sur le serveur principal.
	 * Affiche un message sur le serveur indiquant la fin de la partie.
	 */
	private void end(int end) {
		String endMessage = Thread.currentThread().getName() + " fini : ";
		String reason;
		if (end == END_TURNS) {
			reason = "Egalité";
		} else if (end == END_ERROR) {
			reason = "Erreur de communication";
		} else {
			currentPlayer = 1 - currentPlayer;
			reason = "Le joueur " + (currentPlayer + 1) + " a gagné";
		}

		System.out.println(endMessage + reason);
		if (end == END_NORMAL) {
			players[currentPlayer].receiveMessage("Vous avez gagné !");
			players[1 - currentPlayer].receiveMessage("Vous avez perdu...");
		} else {
			server.sendMessage(reason, players);
		}
		for (ClientManager p : players) {
			p.endPlaying();
		}
		// fin de la partie
	}


	/**
	 * Demande au joueur les coordonnées où il veut placer son symbole.
	 * @param player le joueur courant.
	 * @return les coordonnées entrées par le joueur.
	 */
	private int[] getCoords(ClientManager player) throws IOException {
		int[] coords = new int[2];
		String msg = null;
		while (true) {
			for (int i = 0; i < 2; i++) {
				do {
					player.receiveMessage(prompt[i]);
					if (player.isConnected()) {
						msg = player.getBF().readLine();
					}
					if (msg == null) {
						//System.out.println("C'est viiiiiiiiiide");
						server.disconnectClient(player);
						emergencyShutDown();
						return null;
					}
				} while (!msg.matches("[1-3]"));
				coords[i] = Integer.parseInt(msg) - 1;
			}
			if (grid[coords[0]][coords[1]] != ' ') {
				player.receiveMessage("Case déjà occupée");
			} else {
				break;
			}
		}
		return coords;
	}


	private boolean isGameOver() {
		return checkRows() || checkCols() || checkDiags();
	}


	private boolean checkRows() {
		boolean bOk = false;
		for (int i = 0; i < grid.length; i++) {
			if (grid[i][0] != ' ') {
				bOk = (grid[i][0] == grid[i][1] && grid[i][1] == grid[i][2]);
			}
			if (bOk) break;
		}
		return bOk;
	}


	private boolean checkCols() {
		boolean bOk = false;
		for (int i = 0; i < grid.length || bOk; i++) {
			if (grid[0][i] != ' ') {
				bOk = (grid[0][i] == grid[1][i] && grid[1][i] == grid[2][i]);
			}
			if (bOk) break;
		}
		return bOk;
	}


	private boolean checkDiags() {
		boolean bOk = false;
		if (grid[1][1] != ' ') {
			bOk = (grid[0][0] == grid[1][1] && grid[1][1] == grid[2][2]) ||
				  (grid[2][0] == grid[1][1] && grid[1][1] == grid[0][2]);
		}
		return bOk;
	}


	public void emergencyShutDown() {
		for (ClientManager p : players) {
			p.receiveMessage("L'autre joueur a quitté");
			p.endPlaying();
		}
		Thread.currentThread().interrupt();
	}


	public String getGrid() {
		String s = "+---+---+---+\n";
		for (int i = 0; i < grid.length; i++) {
			s += "| ";
			for (int j = 0; j < grid[i].length; j++) {
				s += grid[i][j] + " | ";
			}
			s += "\n+---+---+---+\n";
		}
		return s;
	}


	public int getInstanceNumber() {
		return instanceNumber;
	}
}
