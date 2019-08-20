package main;

import main.jaxbClasses.MagitBlob;
import main.jaxbClasses.MagitSingleFolder;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;

public class FolderItem implements Comparable {
    private String m_sha1;
    private String m_itemName;
    private String m_userName;
    private String m_lastModified;
    private String m_type;

    public FolderItem(String i_sha1, String i_itemName, String i_userName, String i_lastModified, String i_type) {
        m_sha1 = i_sha1;
        m_itemName = i_itemName;
        m_userName = i_userName;
        m_lastModified = i_lastModified;
        m_type = i_type;
    }

    public FolderItem(String[] i_itemDetails) {
        m_sha1 = i_itemDetails[0];
        m_itemName = i_itemDetails[1];
        m_userName = i_itemDetails[2];
        m_lastModified = i_itemDetails[3];
        m_type = i_itemDetails[4];
    }

    public FolderItem(MagitBlob i_magitBlob) {
        m_itemName = i_magitBlob.getName();
        m_lastModified = i_magitBlob.getLastUpdateDate();
        m_type = "file";
        m_userName = i_magitBlob.getLastUpdater();
        m_sha1 = DigestUtils.sha1Hex(i_magitBlob.getContent());
    }

    public FolderItem(MagitSingleFolder i_magitSingleFolder, List<FolderItem> i_folderItems) {
        m_itemName = i_magitSingleFolder.getName();
        m_lastModified = i_magitSingleFolder.getLastUpdateDate();
        m_type = "folder";
        m_userName = i_magitSingleFolder.getLastUpdater();
        m_sha1 = MainEngine.calculateFileSHA1(i_folderItems);
    }


    public String getItemName() {
        return m_itemName;
    }

    public String getSha1() {
        return m_sha1;
    }

    public String getType() {
        return m_type;
    }


    @Override
    public int compareTo(Object i_FolderItemToCompare) {
        FolderItem folderItemToCompare = (FolderItem) i_FolderItemToCompare;

        return m_itemName.compareTo(folderItemToCompare.getItemName());
    }

    @Override
    public String toString() {
        return m_sha1 + "," +
                m_itemName + "," +
                m_userName + "," + m_lastModified + "," + m_type;

    }
    public String getDetails(){
        return "Sha1:"+ m_sha1+"\nLast modifier:"+ m_userName+ "\nDate last modified:" + m_lastModified+ "\nType:"+m_type;
    }


}