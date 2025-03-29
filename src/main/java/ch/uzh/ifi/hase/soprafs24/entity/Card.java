package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.CardColor;

public class Card {
    private String word;
    private CardColor color;
    private boolean guessed = false; 

    public Card() {}

    public Card(String word, CardColor color) {
        this.word = word;
        this.color = color;
        this.guessed = false; 
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public CardColor getColor() {
        return color;
    }

    public void setColor(CardColor color) {
        this.color = color;
    }

    public boolean isGuessed() {
        return guessed;
    }

    public void setGuessed(boolean guessed) {
        this.guessed = guessed;
    }
}