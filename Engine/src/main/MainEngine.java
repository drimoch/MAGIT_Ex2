package main;

import main.Exceptions.FolderIsNotEmptyException;
import main.Exceptions.RepoXmlNotValidException;
import main.Exceptions.RepositoryAlreadyExistException;
import main.jaxbClasses.*;
import main.mainWindow.MainWindowController;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.Equator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

import main.mainWindow.*;

public class MainEngine {
    private MainWindowController m_mainWindowController;

    public MainEngine() {
    }

    public MainEngine(MainWindowController i_mainWindowController) {
        this.m_mainWindowController = i_mainWindowController;

    }

    public static String scanWorkingCopy(String currentRepository1, Map<String, List<FolderItem>> foldersMap) throws IOException {

        //compare WC to the master commit
        //create a temp file
        Path dirPath = Paths.get(currentRepository1);
        File dir = dirPath.toFile();
        List<FolderItem> filesList = new LinkedList<>();
        //foldersMap = new HashMap<String, List<main.FolderItem>>();
        walk(dir, foldersMap, filesList);
        String rootSha1 = calculateFileSHA1(filesList);
        foldersMap.put(rootSha1, filesList);
        return rootSha1;

    }

    //TODO: Handle exceptions in walk!
    public static void walk(File dir, Map<String, List<FolderItem>> foldersMap, List<FolderItem> parentFolder) throws IOException {
        String fileContent;
        Path path;
        BasicFileAttributes attr;
        List<FolderItem> subFiles;
        FolderItem currentFolderItem;
        for (final File folderItem : dir.listFiles()) {
            if (!folderItem.getName().endsWith(".magit")) {
                path = Paths.get(folderItem.getPath());
                attr = Files.readAttributes(path, BasicFileAttributes.class);

                if (folderItem.isDirectory()) {
                    if (folderItem.list().length == 0)
                        folderItem.delete();
                    else {
                        subFiles = new LinkedList<FolderItem>();
                        walk(folderItem, foldersMap, subFiles);
                        Collections.sort(subFiles, FolderItem::compareTo);
                        String key = calculateFileSHA1(subFiles);
                        foldersMap.put(key, subFiles);

                        currentFolderItem = new FolderItem(key, folderItem.getName(), "user name", attr.lastModifiedTime().toString(), "folder");
                        parentFolder.add(currentFolderItem);
                    }

                }

                if (folderItem.isFile()) {
                    fileContent = EngineUtils.readFileToString(folderItem.getPath());
                    currentFolderItem = new FolderItem(DigestUtils.sha1Hex(fileContent), folderItem.getName(), "user name", attr.lastModifiedTime().toString(), "file");
                    parentFolder.add(currentFolderItem);


                }

            }
        }
        return;

    }

    public static String calculateFileSHA1(List<FolderItem> folderContent) {
        String res = "";
        for (FolderItem fItem : folderContent) {
            res = res + fItem.getSha1();
        }
        return DigestUtils.sha1Hex(res);
    }


    public static void initRepo(String rootDirPath, String name) throws IOException, RepoXmlNotValidException {
        //take care of  exceptions when using this
        if (rootDirPath != null && name != null && name != "" && rootDirPath != "") {
            File rootDirFileObj = FileUtils.getFile(rootDirPath);
            if (rootDirFileObj.exists()) {
                File[] dirItems = rootDirFileObj.listFiles();
                if (dirItems.length > 0) {
                    for (File file : dirItems
                    ) {
                        FileUtils.deleteQuietly(file);
                    }
                }
            }
            FileUtils.forceMkdir(rootDirFileObj);
            Path objectsDirPath = java.nio.file.Paths.get(rootDirPath + "\\" + ".magit\\objects");
            Files.createDirectories(objectsDirPath);
            File branchesFileObj = FileUtils.getFile(rootDirPath + "\\" + ".magit\\branches");
            FileUtils.forceMkdir(branchesFileObj);
            String headContent = "master";
            String branchesPath = branchesFileObj.getAbsolutePath();
            File headFile = new File(branchesPath + "\\HEAD");
            File masterFile = new File(branchesPath + "\\master");
            File nameFile = new File(rootDirPath + "\\.magit\\name");
            Files.write(Paths.get(nameFile.getPath()), name.getBytes());
            FileUtils.touch(masterFile);
            Files.write(Paths.get(headFile.getPath()), headContent.getBytes());

            System.out.println("Create " + rootDirPath + " success. ");
        } else {
            throw new RepoXmlNotValidException("Root directory location does not exist");
        }
    }


    //COMMIT RELATED FUNCTIONS

    public static void compareWCtoCommit(Map<String, List<FolderItem>> WCmap,
                                         Map<String, List<FolderItem>> LastCommitMap,
                                         String currentWCKey,
                                         String currentCommitKey,
                                         String path,
                                         Map<String, String> deletedList, Map<String, String> addedList, Map<String, String> changedList) {
        FolderItemEquator itemsEquator = new FolderItemEquator();
        if (currentCommitKey.equals(currentWCKey))
            return;
        else {
            List<FolderItem> currentCommitFolder = LastCommitMap.get(currentCommitKey);
            List<FolderItem> currentWCFolder = WCmap.get(currentWCKey);

            //deleted files= commitmap-wcmap
            if (!LastCommitMap.isEmpty()) {
                List<FolderItem> deleted = (List<FolderItem>) CollectionUtils.removeAll(currentCommitFolder, currentWCFolder, itemsEquator);
                deleted.stream().
                        forEach(o -> mapLeavesOfPathTree(LastCommitMap, o, path, deletedList));
            }
            //added files = wcmap-commitmap
            if (!WCmap.isEmpty()) {
                List<FolderItem> added = (List<FolderItem>) CollectionUtils.removeAll(WCmap.get(currentWCKey), LastCommitMap.get(currentCommitKey), itemsEquator);
                added.stream().
                        forEach(o -> mapLeavesOfPathTree(WCmap, o, path, addedList));
            }
            //we remain with the common files. go through them and compare
            if (!LastCommitMap.isEmpty()) {
                List<FolderItem> changed = (List<FolderItem>) CollectionUtils.retainAll(WCmap.get(currentWCKey), LastCommitMap.get(currentCommitKey), itemsEquator);
                for (FolderItem item : changed) {
                    Optional<FolderItem> alteredCopy = LastCommitMap.get(currentCommitKey).stream().filter(i -> i.getItemName().equals(item.getItemName()) && i.getType().equals(item.getType())).findFirst();
                    if (item.getType().equals("folder")) {
                        compareWCtoCommit(WCmap,
                                LastCommitMap,
                                item.getSha1(),
                                alteredCopy.get().getSha1(),
                                path + "\\" + item.getItemName(),
                                deletedList, addedList, changedList);

                    } else if (!alteredCopy.get().getSha1().equals(item.getSha1()))
                        changedList.put(item.getSha1(), path + "\\" + item.getItemName());
                }
            }
        }

    }

    public static void mapLeavesOfPathTree(Map<String, List<FolderItem>> mapOfPath, FolderItem item, String path, Map<String, String> leaves) {
        if (!item.getType().equals("folder"))
            leaves.put(item.getSha1(), path + "\\" + item.getItemName());

        else {
            mapOfPath.get(item.getSha1()).stream().forEach(i -> mapLeavesOfPathTree(mapOfPath, i, path + "\\" + item.getItemName(), leaves));
        }

    }

    public static boolean validateRepo(MagitRepository repoToParse) throws RepoXmlNotValidException, FolderIsNotEmptyException, RepositoryAlreadyExistException, IOException {
        return EngineUtils.isRepoLocationValid(repoToParse.getLocation());

    }

    public boolean checkForChanges(Map<String, List<FolderItem>> mapOfdif, CommitObj commit, String currentRepo) throws IOException {
        Map<String, List<FolderItem>> mapOfLatestCommit = new HashMap<>();
        Map<String, List<FolderItem>> mapOfWC = new HashMap<>();
        String latestCommitRoot;
        String latestCommitSha1 = EngineUtils.readFileToString(currentRepo + "\\.magit\\branches\\" + EngineUtils.readFileToString(currentRepo + "\\.magit\\branches\\HEAD"));

        latestCommitRoot = EngineUtils.getLastCommitRoot(currentRepo);
        String WCSha1 = scanWorkingCopy(currentRepo, mapOfWC);
        if (!WCSha1.equals(latestCommitRoot)) {
            commit.setCommitSHA1(WCSha1);
            commit.setPreviousCommitSha1(latestCommitSha1);
            if (!latestCommitSha1.equals(""))
                mapOfLatestCommit = createLatestCommitMap(latestCommitRoot, currentRepo);
            compareWCtoCommit(mapOfWC, mapOfLatestCommit, WCSha1, latestCommitRoot, currentRepo, commit.deleted, commit.added, commit.changed);

            for (String key : mapOfWC.keySet()) {
                if (!mapOfLatestCommit.containsKey(key)) {
                    mapOfdif.put(key, mapOfWC.get(key));
                }
            }
            return true;
        } else
            return false;

    }

    public void createRepoFromXML(MagitRepository repoToParse) throws RepositoryAlreadyExistException, IOException, FolderIsNotEmptyException, RepoXmlNotValidException {

        Map<String, MagitBlob> repoBlobs = EngineUtils.createRepoBlobsMap(repoToParse.getMagitBlobs());
        Map<String, MagitSingleFolder> repoFolders = EngineUtils.createRepoFoldersMaps(repoToParse.getMagitFolders(), repoBlobs);
        Map<String, CommitObj> commitObjectsMap = EngineUtils.createRepoCommitMap(repoToParse.getMagitCommits(), repoFolders);// map where the key is id and the values are commit objects
        Map<String, String> branchesMap = EngineUtils.createRepoBranchMap(repoToParse.getMagitBranches(), commitObjectsMap.keySet());
        EngineUtils.isHEADValid(repoToParse, branchesMap.keySet());

        List<MagitSingleCommit> firstCommit = repoToParse.getMagitCommits().getMagitSingleCommit().stream()
                .filter(magitSingleCommit -> magitSingleCommit.getPrecedingCommits() == null
                        || magitSingleCommit.getPrecedingCommits().getPrecedingCommit().size() == 0)
                .collect(Collectors.toList());

        initRepo(repoToParse.getLocation(), repoToParse.getName());
        String fullPath = repoToParse.getLocation();
        createRepositoryRec(firstCommit.get(0).getId(), "", repoFolders, repoBlobs, commitObjectsMap, branchesMap, repoToParse.getLocation());
        EngineUtils.overWriteFileContent(fullPath + "\\.magit\\branches\\HEAD", repoToParse.getMagitBranches().getHead());
        Map<String, List<FolderItem>> map = createLatestCommitMap(EngineUtils.getLastCommitRoot(fullPath), fullPath);
        parseMapToWC(map, EngineUtils.getLastCommitRoot(fullPath), fullPath + "\\.magit\\objects\\", fullPath);

    }


    public void createRepositoryRec(String currCommitID,
                                    String prevCommitSha1,
                                    Map<String, MagitSingleFolder> repoFolders,
                                    Map<String, MagitBlob> repoBlobs,
                                    Map<String, CommitObj> commitObjectsMap,
                                    Map<String, String> branchesMap,
                                    String repo) {

        CommitObj currCommit = commitObjectsMap.get(currCommitID);
        currCommit.rootDirSha1 = magitSingleCommitToMap(currCommit.rootFolderID, repoFolders, repoBlobs, repo);
        currCommit.PreviousCommitSha1 = prevCommitSha1;
        String newCommitContent = currCommit.toString();
        String Sha1 = DigestUtils.sha1Hex(newCommitContent);
        branchesMap.forEach((name, cID) -> {
            if (cID.equals(currCommitID))
                EngineUtils.stringToTextFile(repo + "\\.magit\\branches\\", Sha1, name);
        });

        try {
            EngineUtils.StringToZipFile(newCommitContent, repo + "\\.magit\\objects\\", Sha1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        commitObjectsMap.forEach((key, commitObj) ->
        {
            if (commitObj.PreviousCommitID.equals(currCommitID))
                createRepositoryRec(key, Sha1, repoFolders, repoBlobs, commitObjectsMap, branchesMap, repo);
        });

    }


    public String magitSingleCommitToMap(String i_parentFolderID, Map<String, MagitSingleFolder> repoFolders, Map<String, MagitBlob> repoBlobs, String path) {
        MagitSingleFolder rootFolder = repoFolders.get(i_parentFolderID);
        List<Item> folderItems = rootFolder.getItems().getItem();
        List<FolderItem> folderContents = new LinkedList<>();
        String folderSha1 = null;
        String itemSha1;
        try {
            for (Item folderItem : folderItems) {
                String itemType = folderItem.getType();
                String itemID = folderItem.getId();
                if (itemType.equals("blob")) {
                    MagitBlob blob = repoBlobs.get(itemID);
                    String content = blob.getContent().replaceAll("\\r", "");
                    itemSha1 = DigestUtils.sha1Hex(content);
                    FolderItem blobItem = new FolderItem(itemSha1, blob.getName(), blob.getLastUpdater(), blob.getLastUpdateDate(), "file");
                    EngineUtils.StringToZipFile(content, path + "\\.magit\\objects\\", itemSha1);

                    folderContents.add(blobItem);

                } else if (itemType.equals("folder")) {

                    itemSha1 = magitSingleCommitToMap(itemID, repoFolders, repoBlobs, path);
                    MagitSingleFolder folder = repoFolders.get(itemID);
                    FolderItem item = new FolderItem(itemSha1, folder.getName(), folder.getLastUpdater(), folder.getLastUpdateDate(), "folder");
                    folderContents.add(item);
                }
            }
            Collections.sort(folderContents, FolderItem::compareTo);
            folderSha1 = calculateFileSHA1(folderContents);
            EngineUtils.StringToZipFile(EngineUtils.listToString(folderContents, "\n"), path + "\\.magit\\objects\\", folderSha1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return folderSha1;

    }

    public List<String> displayLastCommitDetails(String currRepo) throws IOException {
        Map<String, List<FolderItem>> result;
        String rootSha1 = EngineUtils.getLastCommitRoot(currRepo);
        result = createLatestCommitMap(rootSha1, currRepo);
        List<String> objects = new LinkedList<>();
        stringifyRepo(result, currRepo, rootSha1, objects);
        return objects;


    }

    public void stringifyRepo(Map<String, List<FolderItem>> map, String repo, String root, List<String> objects) {
        List<FolderItem> lst = map.get(root);
        for (FolderItem i : lst) {
            objects.add(i.getDetails() + "\n" +
                    "Path:" + repo + "\\" + i.getItemName());
            if (i.getType().equals("folder"))
                stringifyRepo(map, repo + "\\" + i.getItemName(), i.getSha1(), objects);
        }
    }


    public Map<String, List<FolderItem>> createLatestCommitMap(String i_rootDirSha, String currentRepo) throws IOException {
        Map<String, List<FolderItem>> result = new HashMap<String, List<FolderItem>>();
        createCommitMapRec(i_rootDirSha, result, currentRepo);
        return result;
    }

    private void createCommitMapRec(String i_rootDirSha, Map<String, List<FolderItem>> i_commitMap, String currentRepo) throws IOException {
        List<FolderItem> rootDir = EngineUtils.parseToFolderList(currentRepo + "\\.magit\\objects\\" + i_rootDirSha + ".zip");
        i_commitMap.put(i_rootDirSha, rootDir);
        for (FolderItem item : rootDir) {
            if (item.getType().equals("folder")) {
                createCommitMapRec(item.getSha1(), i_commitMap, currentRepo);
            }
        }
    }

    public List<String> listAllBranches(String currentRepo) throws IOException, RepoXmlNotValidException {
        File branches = FileUtils.getFile(currentRepo + "\\.magit\\branches");
        String sha1;
        List<String> branchesList = new LinkedList<>();
        for (File i : branches.listFiles()) {
            if (!i.getPath().equals(currentRepo + "\\.magit\\branches\\HEAD")) {
                sha1 = EngineUtils.readFileToString(i.getPath());
                if (sha1.isEmpty()) {
                    throw new RepoXmlNotValidException(String.format("invalid branch file, %s branch file is empty", i.getName()));
                }
                branchesList.add(i.getName().toUpperCase() + ":\n%s" +
                        EngineUtils.listToString(EngineUtils.getZippedFileLines(currentRepo + "\\.magit\\objects\\" + sha1 + ".zip").subList(0, 5), "%s"));

            }

        }
        return branchesList;
    }


    public void finalizeCommit(CommitObj obj, Map<String, List<FolderItem>> mapOfdif, String currentRepo) throws IOException {
        String targetPath = currentRepo + "\\.magit\\objects\\";
        obj.changed.forEach((key, string) -> EngineUtils.ZipFile(key, string, targetPath));
        obj.added.forEach((key, string) -> EngineUtils.ZipFile(key, string, targetPath));
        foldersToFile(mapOfdif, targetPath);

        String newCommitContent = obj.toString();
        String newCommitSha1 = DigestUtils.sha1Hex(newCommitContent);

        EngineUtils.StringToZipFile(newCommitContent, targetPath, newCommitSha1);
        String currentHead = EngineUtils.readFileToString(currentRepo + "\\.magit\\branches\\HEAD");
        EngineUtils.overWriteFileContent(currentRepo + "\\.magit\\branches\\" + currentHead, newCommitSha1);
    }

    public void foldersToFile(Map<String, List<FolderItem>> mapOfdif, String targetPath) {

        mapOfdif.forEach((key, item) -> {
            try {

                EngineUtils.StringToZipFile(EngineUtils.listToString(item, "\n"), targetPath, key);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    public List<String> displayLatestCommitHistory(String currRepo) throws IOException {
        String curCommit = EngineUtils.readFileToString(currRepo + "\\.magit\\branches\\" + EngineUtils.readFileToString(currRepo + "\\.magit\\branches\\HEAD"));
        List<String> res = new LinkedList<>();
        List<String> commitContent;
        while (!curCommit.equals("")) {
            commitContent = EngineUtils.getZippedFileLines(currRepo + "\\.magit\\objects\\" + curCommit + ".zip");
            res.add("Commit SHA1:" + curCommit + "%s " + EngineUtils.listToString(commitContent.subList(2, 5), " %s "));
            curCommit = commitContent.get(1);
        }
        return res;

    }

    public void switchHeadBranch(String branchName, String currentRepository) {

        String branchFile = currentRepository + "\\.magit\\branches\\" + branchName;

        try {
            String skip = currentRepository + "\\.magit";
            String Commitsha1 = EngineUtils.readFileToString(branchFile), rootsha1;
            for (File i : FileUtils.getFile(currentRepository).listFiles()) {
                if (!i.getPath().contains(skip))
                    FileUtils.deleteQuietly(i);
            }

            EngineUtils.overWriteFileContent(currentRepository + "\\.magit\\branches\\HEAD", branchName);
            rootsha1 = EngineUtils.getLastCommitRoot(currentRepository);
            Map<String, List<FolderItem>> mapOfCommit = createLatestCommitMap(rootsha1, currentRepository);
            parseMapToWC(mapOfCommit, rootsha1, currentRepository + "\\.magit\\objects\\", currentRepository);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public void parseMapToWC(Map<String, List<FolderItem>> dirMap, String dirRootSHA1, String sourcePath, String destPath) {
        String newDestPath;
        List<FolderItem> items = dirMap.get(dirRootSHA1);
        for (FolderItem i : items) {
            if (i.getType().equals("folder")) {
                File folder = new File(destPath + "\\" + i.getItemName());
                folder.mkdir();
                parseMapToWC(dirMap, i.getSha1(), sourcePath, destPath + "\\" + i.getItemName());
            } else {
                EngineUtils.extractFile(sourcePath + i.getSha1() + ".zip", i.getSha1(), destPath + "\\" + i.getItemName());

            }
        }


    }

    public static class FolderItemEquator implements Equator<FolderItem> {
        @Override
        public boolean equate(FolderItem t1, FolderItem t2) {
            return (t1.getItemName().equals(t2.getItemName()) && t1.getType().equals(t2.getType()));
        }

        @Override
        public int hash(FolderItem folderItem) {
            return (folderItem.getItemName() + folderItem.getType()).hashCode();
        }

    }
}
