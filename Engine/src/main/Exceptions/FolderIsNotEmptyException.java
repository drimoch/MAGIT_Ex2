package main.Exceptions;

public class FolderIsNotEmptyException extends Exception {
    private final String EXCEPTION_MESSAGE = "%s folder is not empty";
    private String folderName;

    public FolderIsNotEmptyException(String i_folderNAme) {
        folderName = i_folderNAme;
    }

    public String getFolderName() {
        return folderName;
    }

    @Override
    public String getMessage() {
        return String.format(EXCEPTION_MESSAGE, folderName);
    }
}
