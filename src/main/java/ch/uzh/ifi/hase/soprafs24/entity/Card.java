package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.CardColor;

public class Card {
    private String word;
    private CardColor color;
    private boolean guessed = false; 
    private boolean selectedByRedTeam = false; 
    private boolean selected = false;

    public Card() {}

    public Card(String word, CardColor color) {
        this.word = word;
        this.color = color;
        this.guessed = false; 
        this.selected = false;  
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

    public boolean isSelected() { 
        return selected; 
    
    }
    public void setSelected(boolean selected) { 
        this.selected = selected; 
    }
}