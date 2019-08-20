package main;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class CommitObj {
    public Map<String, String> added;
    public Map<String, String> changed;
    public Map<String, String> deleted;
    String rootDirSha1;
    String PreviousCommitSha1;
    private String m_submitterName;
    String dateCreated;
    String commitMessage;
    public String PreviousCommitID;
    public String rootFolderID;



    public CommitObj() {

        deleted = new HashMap<>();
        changed = new HashMap<>();
        added = new HashMap<>();
        DateFormat dateFormat = new SimpleDateFormat("dd.mm.yyyy-hh:mm:ss:sss");
        Date date = new Date();
        dateCreated = dateFormat.format(date);
    }
    public void setPreviousCommitSha1(String commitSHA1){
        PreviousCommitSha1 =commitSHA1;
    }
    public void setCommitSHA1(String commitSHA1){
        rootDirSha1=commitSHA1;
    }


    CommitObj(String i_dateCreated, String i_commitMessage, String i_submitterName, String i_previousCommit, String i_rootFolderID) {
        dateCreated = i_dateCreated;
        commitMessage = i_commitMessage;
        m_submitterName = i_submitterName;
        PreviousCommitID = i_previousCommit;
        rootFolderID = i_rootFolderID;
    }

    public String getUserName() {
        return m_submitterName;
    }

    public void setUserName(String i_userNAme) {
        m_submitterName = i_userNAme;
    }


    public void setCommitMessage(String message) {
        this.commitMessage = message;
    }

    @Override
    public String toString() {
        return rootDirSha1 + "\n" + PreviousCommitSha1 +"\n" + m_submitterName + "\n" + dateCreated + "\n" + commitMessage+ "\ncommit" ;


    }

}