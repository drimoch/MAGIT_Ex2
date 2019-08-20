package main.Exceptions;

public class RepoXmlNotValidException extends Exception {
    private String m_message;

    public RepoXmlNotValidException(String i_message) {
        m_message = i_message;
    }

    @Override
    public String getMessage() {
        return m_message;
    }
}
