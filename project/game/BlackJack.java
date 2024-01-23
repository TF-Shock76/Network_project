/** BlackJack.java
 *  @author Guillaume Coufourier
 *  @author Théo Crauffon
 *  @author Steven Hue
 */


package project.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.IOException;

import project.client.ClientManager;
import project.server.SimpleServer;


public class BlackJack implements IGame {

	private static final int NB_PLAYERS_MIN = 2;
	private static final int NB_PLAYERS_MAX = 7;
	private static final int END_ERROR      = 2;

	private static int instances;
	private final int instanceNumber;

	private SimpleServer   server;
	private ClientManager[]  players;
	private ArrayList<Card> cardSet;

	private ArrayList<Card>   dealerHand;
	private HashMap<ClientManager, ArrayList<Card>> playersHandArray;
	private int currentPlayer;
	private boolean continueStatus;

	public BlackJack(SimpleServer server, ClientManager[] players) {
		// Constructeur ressemblant énormément à celui du Morpion
		// faire une classe mère ?

		if (players == null) {
			throw new NullPointerException();
		}
		if (players.length < NB_PLAYERS_MIN || players.length > NB_PLAYERS_MAX) {
			throw new IllegalArgumentException(
				"Le jeu se joue avec minimum " + NB_PLAYERS_MIN +
				" joueurs et maximum " + NB_PLAYERS_MAX + " joueurs.");
		}

		this.server = server;
		this.players = players;
		this.instanceNumber = ++instances;
		this.continueStatus = false;

		// Déclaration de la main du dealer
		dealerHand = new ArrayList<Card>();

		// Déclaration des mains des joueurs
		playersHandArray = new HashMap <ClientManager, ArrayList<Card>>();
		for(ClientManager gc : players) {
			playersHandArray.put(gc, new ArrayList<Card>());
		}

		// Initialisation des paquets de cartes
		cardSet = new ArrayList<Card>();
		setCardsSet(6);
	}

	public void run() {
		int gameEndReason = -1;
		startGame();
		try {
			do{
				resetHands();
				gameEndReason = play();
			}while(gameEndReason != END_ERROR && allContinue());
		}catch (IOException e) { gameEndReason = END_ERROR; }

		if(!continueStatus)
			server.sendMessage("Un joueur a quitté la partie, de retour au menu des jeux", players);

		end(gameEndReason);
	}

	private void startGame() {
		// Etant donné que l'ordre de jeu dans le BlackJack est le sens horaire
		// du placement des joueurs sur la table, il n'y a pas lieu de faire
		// appel à un random pour déterminer le joueur qui doit commencer
		for(int i = 0; i < players.length; i++) {
			players[i].receiveMessage("Vous êtes le joueur " + (i+1));
		}
		currentPlayer = 0;
		server.sendMessage("Le joueur " + (currentPlayer + 1) + " commence", players);
	}

	private int play() throws IOException {

		boolean betsDone = askBets();
		if(!betsDone) return END_ERROR;
		dealCards();
		showCards(null);
		checkBlackJack();

		// System.out.println("Coucou Je staaaaaaaaart");
		for(int i = 0; i < playersHandArray.size(); i++) {
			// Placement de la mise
			showCards(players[i]);

			boolean continueDrawing = true;
			boolean alreadyDraw = false;
			while(getValue(playersHandArray.get(players[i]), false) < 21 && continueDrawing && !players[i].getBlackJack()) {
				sendChoices(players[i], alreadyDraw);
				String action = players[i].getBF().readLine().toLowerCase();
				if(action.equals("exit")) {
					server.disconnectClient(players[i]);
					return END_ERROR;
				}
				switch(action) {
					case "stand" :
						continueDrawing = false;
						break;
					case "hit" :
						playersHandArray.get(players[i]).add(drawCard());
						sort(playersHandArray.get(players[i]));
						showCards(players[i]);
						alreadyDraw = true;
						break;
					case "double" :
						if(alreadyDraw) {
							players[i].receiveMessage("Vous avez déjà draw, vous ne pouvez plus double");
							break;
						}
						playersHandArray.get(players[i]).add(drawCard());
						sort(playersHandArray.get(players[i]));
						showCards(players[i]);
						continueDrawing = false;
						players[i].setBet(players[i].getBet()*2);
						break;
					default :
						players[i].receiveMessage("Action incorrecte, veuillez recommencer");
						break;
				}
			}
			players[i].receiveMessage("Fin de votre tour");
		}

		while(getValue(dealerHand, true) < 17) {
			dealerHand.add(drawCard());
		}
		checkWinAndLose();
		return 0;
	}

	private boolean askBets() throws IOException{
		String temp = null;
		for(int i = 0; i < playersHandArray.size(); i++) {
			int bet = 0;
			players[i].receiveMessage("Votre argent : " + players[i].getMoney() + "\n Combien voulez-vous miser ? (2-100)");
			do {
				temp = players[i].getBF().readLine();
				if(temp.equals("exit")) {
					server.disconnectClient(players[i]);
					emergencyShutDown();
					return false;
				}
				if(temp.matches("[0-9]+")) {
					bet = Integer.parseInt(temp);
					if(bet < 2 || bet > 100)
						players[i].receiveMessage("La valeur de la mise est erronnée, veuillez réessayer");
				} else {
					players[i].receiveMessage("Ce n'est pas un nombre");
				}

			}while(bet < 2 || bet > 100);

			players[i].setBet(bet);
		}
		return true;
	}

	private void dealCards() {
		dealerHand.add(drawCard());
		for(ClientManager player : playersHandArray.keySet()) {
			playersHandArray.get(player).add(drawCard());
			playersHandArray.get(player).add(drawCard());
			sort(playersHandArray.get(player));
		}
	}

	private Card drawCard() {
		return cardSet.remove((int)(Math.random() * cardSet.size()));
	}

	private void showCards(ClientManager player) {
		// Cas de l'affichage de toutes les mains pour tout le monde
		if(player == null) {
			String table = "Main du croupier : (" + getValue(dealerHand, true) + " points)\n";
			for(Card card : dealerHand) {
				table += "\t" + card.toString() + "\n";
			}
			for(ClientManager p : playersHandArray.keySet()) {
				table += "Main du joueur " + p.getName() + ": \n";
				for(Card card : playersHandArray.get(p)) {
					table += "\t"+  card.toString() + "\n";
				}
			}
			table += "----------------------\n";
			server.sendMessage(table, players);
		} else {
			// Cas de l'affichage de la main pour le joueur en cours
			String main = "Votre main : \n";
			for(Card card : playersHandArray.get(player)) {
				main += "\t" + card.toString() + "\n";
			}
			player.receiveMessage(main);
		}
	}

	private void checkBlackJack() {
		for(ClientManager player : playersHandArray.keySet()) {
			if(playersHandArray.get(player).get(0).isPicture() && playersHandArray.get(player).get(1).getName().equals("As") ||
			   playersHandArray.get(player).get(1).isPicture() && playersHandArray.get(player).get(0).getName().equals("As") ) {
				player.setBlackJack(true);
				player.receiveMessage("Vous avez Black-Jack");
			}
		}
	}

	private void checkWinAndLose() {
		// Si le croupier a BlackJack, il ramasse les mises sauf des joueurs
		// ayant fait BlackJack aussi. Dans ce cas, c'est nul.
		// Croupier qui fait BlackJack vs joueur à 21 points = win du croupier !

		server.sendMessage("Résultats : ");
		showCards(null);

		// Si le dealer a fait BlackJack
		if(dealerHand.get(0).isPicture() && dealerHand.get(1).getName().equals("As") ||
		   dealerHand.get(1).isPicture() && dealerHand.get(0).getName().equals("As")) {
			for(ClientManager player : playersHandArray.keySet()) {
				if(player.getBlackJack()) {
					player.receiveMessage("Egalité, vous récupérez votre mise");
				}
				else
					player.receiveMessage("Perdu, vous perdez votre mise");
			}
			return;
		}


		int dealerHandValue = getValue(dealerHand, true);
		for(ClientManager player : playersHandArray.keySet()) {
			int playerHandValue = getValue(playersHandArray.get(player), true);
			// Cas d'égalité sans dépassement
			if(playerHandValue < 22 && dealerHandValue < 22 && playerHandValue == dealerHandValue) {
				player.receiveMessage("Egalité, vous récupérez votre mise");
				continue;
			}
			// Cas où le joueur dépasse 21 et perd automatiquement et de victoire du croupier
			if(playerHandValue > 21 || (dealerHandValue < 22 && playerHandValue < dealerHandValue)) {
				player.receiveMessage("Perdu, vous perdez votre mise");
				player.earnMoney(player.getBet()*-1);
			} else {
				// Cas où le croupier dépasse ou que le joueur bat le croupier
				// if(dealerHandValue > 21 || playerHandValue > dealerHandValue)
				player.receiveMessage("Gagné, vous gagnez votre mise");
				player.earnMoney(player.getBet());

				// Cas de victoire en blackJack
				if(player.getBlackJack())
					player.earnMoney((double)(player.getBet()*0.5));
			}
		}
	}

	private void sendChoices(ClientManager player, boolean alreadyDraw) {
		String msg = "Quelle action voulez-vous effectuer ? \n" +
		                "\t Stand \n" +
		                "\t Hit \n";

		if(!alreadyDraw) {
			msg += "\t Double \n";
		}
		player.receiveMessage(msg);
	}


	private int getValue(ArrayList<Card> playerHand, boolean bigAs) {
		int value = 0;
		boolean firstAs = true;
		for(Card card : playerHand) {
			// Cas du Roi, Reine, Valet
			if(card.isPicture()) {
				value += 10;
				continue;
			}

			// Cas de l'As que l'on compte comme 11
			// Si le joueur possède plus d'1 As, le premier aura une valeur de 11
			// et les suivants auront 1 comme valeur, car 2 As à 11 fait automatiquement perdre
			if(card.getName().equals("As") && bigAs) {
				if(firstAs && (value+11) < 22) {
					value += 11;
					firstAs = false;
				}
				else
					value += 1;
				continue;
			}

			// Cas basique avec l'As comme 1
			value += card.getValue();
		}
		return value;
	}

	private int nbAs(ArrayList<Card> playerHand) {
		int nbAs = 0;
		for(Card card : playerHand) {
			if(card.getName().equals("As"))
				nbAs++;
		}
		return nbAs;
	}

	private void resetHands() {
		dealerHand = new ArrayList<Card>();
		for(ClientManager player : playersHandArray.keySet()) {
			playersHandArray.replace(player, new ArrayList<Card>());
			player.setBlackJack(false);
		}
	}

	// Méthode de tri permettant de mettre tous les As à la fin de la liste
	// afin de rendre le calcul des points plus facile
	private void sort(ArrayList<Card> hand) {
		ArrayList<Card> temp = new ArrayList<Card>();

		for(Card card : hand) {
			if(card.getName().equals("As")) {
				temp.add(card);
			}
		}

		hand.removeAll(temp);

		for(int i = 0 ; i <temp.size(); i++) {
			hand.add(temp.remove(i));
		}
	}

	private boolean allContinue()  {
		boolean allContinue = true;
		String temp = "";
		ClientManager playerTemp = null;
		try {
			for(ClientManager player : playersHandArray.keySet()) {
				playerTemp = player;
				player.receiveMessage("Voulez-vous continuer la partie ? (O-N)");
				do {
					temp = player.getBF().readLine().toUpperCase();
					if(temp == null || temp.equals("exit")) {
						server.disconnectClient(player);
						continueStatus = false;
						return false;
					}

					if(temp.equals("N")) {
						continueStatus = false;
						return continueStatus;
					}
				}while(!temp.equals("O"));
			}
		} catch(IOException e) { continueStatus = false;  return false; }
		  catch(NullPointerException e) { server.disconnectClient(playerTemp); continueStatus = false; return false;}

		server.sendMessage("Une nouvelle partie commence", players);
		return allContinue;
	}

	private void setCardsSet(int nbSets) {
		cardSet = Card.cardSet(52, 6, cardSet);
	}

	/**
	 * Retour sur le serveur principal.
	 * Affiche un message sur le serveur indiquant la fin de la partie.
	 */
	private void end(int end) {
		String endMessage = Thread.currentThread().getName() + " fini : ";
		String reason = "fin normale";
		if (end == END_ERROR)
			reason = "Erreur de communication";

		System.out.println(endMessage + reason);

		for (ClientManager p : players) {
			p.endPlaying();
		}
		// fin de la partie
	}

	public void emergencyShutDown() {
		for (ClientManager p : players) {
			p.receiveMessage("Un autre joueur a quitté");
			p.endPlaying();
		}
		Thread.currentThread().interrupt();
	}

	public int getInstanceNumber() {
		return instanceNumber;
	}

}
