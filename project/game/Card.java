/** Card.java
 *  @author Guillaume Coufourier
 *  @author Théo Crauffon
 *  @author Steven Hue
 */

package project.game;

import java.util.ArrayList;

public class Card {

    private static final String[] SUIT = {"Heart", "Diamond", "Club", "Spade"};
    private static final String[] NAME = {"As", "Two", "Three", "Four", "Five",
                                          "Six", "Seven", "Eight", "Nine", "Ten",
                                          "Jack", "Queen", "King"};

    private int    value;
    private String suit;
    private String name;

    private Card(String suit, int value) {
        this.suit = suit;
        this.value = value;
        name = Card.NAME[value-1];
    }


    /**
     * Getters
     */

    public boolean isPicture() { return name.equals("Ten")  ||
                                        name.equals("Jack")  ||
                                        name.equals("Queen") ||
                                        name.equals("King"); }
    public int getValue()   { return value; }
    public String getSuit() { return suit;  }
    public String getName() { return name;  }


    public String toString() {
        return this.name + " of " + this.suit;
    }



    /**
     * Méthode permettant de créer un "deck" de carte en fonction des paramètres entrés
     * @param nbCards  le nombre de cartes que va posséder un "deck"
     * @param nbSets   le nombre de sets voulus
     * @param cardList l'ArrayList de cartes qui contiendra tous les set, alimentée en récurrence
     * @return cardList, l'ArrayList de cartes complétée
     */
    public static ArrayList<Card> cardSet(int nbCards, int nbSets, ArrayList<Card> cardList) {
        // Le nbCards est donné par le Constructeur du jeu qui utilise
        // les cartes, nous partons donc du principe qu'il est inutile
        // de vérifier le paramètre
        if(nbSets == 0)
            return cardList;
        else {
            // Première boucle pour l'enseigne de la carte
            for(int i = 0; i < 4; i++) {
                // Deuxième boucle pour la valeur de la carte, de 1 à 13 (roi)
                if(nbCards == 52) {
                    for(int j = 1; j < nbCards/4 +1 ; j++) {
                        cardList.add(new Card(Card.SUIT[i], j));
                    }
                }
            }
            return Card.cardSet(nbCards, nbSets-1, cardList);
        }
    }
}
