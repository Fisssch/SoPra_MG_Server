package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class GiveHintDTO {
    private String hint;
    private Integer wordsCount;

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public Integer getWordsCount() {
        return wordsCount;
    }

    public void setWordsCount(Integer wordsCount) {
        this.wordsCount = wordsCount;
    }
}
