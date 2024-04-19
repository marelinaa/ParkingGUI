/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package parking;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


public class Commands {

    public interface ReadAction {

        void action(Parking parking, long pos, boolean zipped, String key);
    };

    static String fileName;
    static String idxName;
    static String fileNameBak;
    static String idxNameBak;
    static String path;

    static final String IDX_EXT = "idx";
    static String fileExt;

    static public synchronized void setFile(File file) {
        fileName = file.getName();
        path = file.getPath();
        path = path.substring(0, path.indexOf(fileName));
        String[] str = fileName.split("\\.");
        fileExt = str[1];
        idxName = str[0] + "." + IDX_EXT;
        fileNameBak = str[0] + ".~" + fileExt;
        idxNameBak = str[0] + ".~" + IDX_EXT;

        fileName = path + fileName;
        idxName = path + idxName;
        fileNameBak = path + fileNameBak;
        idxNameBak = path + idxNameBak;
    }

    private static synchronized void deleteBackup() {
        new File(fileNameBak).delete();
        new File(idxNameBak).delete();
    }

    private static synchronized void backup() {
        deleteBackup();
        new File(fileName).renameTo(new File(fileNameBak));
        new File(idxName).renameTo(new File(idxNameBak));
    }

    public static void deleteFile() {
        deleteBackup();
        new File(fileName).delete();
        new File(idxName).delete();
    }

    public static void fileCopy(String from, String to) throws IOException {
        byte[] buf = new byte[8192];
        int size;
        try ( FileInputStream fis = new FileInputStream(from);  FileOutputStream fos = new FileOutputStream(to)) {
            while ((size = fis.read(buf)) > 0) {
                fos.write(buf, 0, size);
            }
        }
    }

    private static synchronized void backupCopy() throws IOException {
        try {
            backup();
        } catch (Error | Exception ex) {
        }
        fileCopy(fileNameBak, fileName);
        fileCopy(idxNameBak, idxName);
    }

    public static synchronized void appendFile(boolean zipped, Parking parking)
            throws FileNotFoundException, IOException, ClassNotFoundException,
            KeyNotUniqueException {
        try {
            backupCopy();
        } catch (FileNotFoundException ex) {
        }
        try ( Index idx = Index.load(idxName);  RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
            if (parking == null) {
                return;
            }
            idx.test(parking);
            long pos = Buffer.writeObject(raf, parking, zipped);
            idx.put(parking, pos);
        }
    }

    public static synchronized List<String> readFile()
            throws FileNotFoundException, IOException, ClassNotFoundException {
        ArrayList<String> list = new ArrayList<>();
        readFile(new ReadAction() {
            @Override
            public void action(Parking parking, long pos, boolean zipped, String key) {
                assert (parking != null);
                list.add(parking.toString());
            }
        });
        return list;
    }

    public static synchronized long readFile(ReadAction ra)
            throws FileNotFoundException, IOException, ClassNotFoundException {
        assert (ra != null);
        boolean[] wasZipped = new boolean[]{false};
        long pos, num = 0;
        try ( RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
            while ((pos = raf.getFilePointer()) < raf.length()) {
                Parking parking = (Parking) Buffer.readObject(raf, pos, wasZipped);
                ra.action(parking, pos, wasZipped[0], null);
                num++;
            }
            System.out.flush();
        }
        return num;
    }

    private static IndexBase indexByArg(String arg, Index idx)
            throws IllegalArgumentException {
        switch (arg) {
            case "Car Number":
            case "c":
                return idx.carNum;
            case "Time":
            case "t":
                return idx.time;
            case "Name":
            case "n":
                return idx.names;
            default:
                throw new IllegalArgumentException("Invalid index specified: " + arg);
        }
    }

    public static synchronized List<String> readFile(String arg, boolean reverse)
            throws ClassNotFoundException, IOException, IllegalArgumentException {
        ArrayList<String> list = new ArrayList<>();
        readFile(arg, reverse, new ReadAction() {
            @Override
            public void action(Parking parking, long pos, boolean zipped, String key) {
                assert (parking != null);
                list.add(parking.toString());
            }
        });
        return list;
    }

    public static synchronized long readFile(String arg, boolean reverse, ReadAction ra)
            throws ClassNotFoundException, IOException, IllegalArgumentException {

        assert (ra != null);
        boolean[] wasZipped = new boolean[]{false};
        long num = 0;
        try ( Index idx = Index.load(idxName);  RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
            IndexBase pidx = indexByArg(arg, idx);
            String[] keys
                    = pidx.getKeys(reverse ? new KeyCompReverse() : new KeyComp());
            for (String key : keys) {
                Long[] poss = pidx.get(key);
                for (long pos : poss) {
                    Parking parking = (Parking) Buffer.readObject(raf, pos, wasZipped);
                    if (arg.equals("Name") || arg.equals("n")) {
                        parking.setName(key);
                    }
                    ra.action(parking, pos, wasZipped[0], key);
                    num++;
                }
            }
        }
        return num;
    }

    public static synchronized List<String> findByKey(String type, String value)
            throws ClassNotFoundException, IOException, IllegalArgumentException {
        ArrayList<String> list = new ArrayList<>();
        findByKey(type, value, new ReadAction() {
            @Override
            public void action(Parking parking, long pos, boolean zipped, String key) {
                assert (parking != null);
                list.add(parking.toString());
            }
        });
        return list;
    }

    public static synchronized long findByKey(String type, String value, ReadAction ra)
            throws ClassNotFoundException, IOException, IllegalArgumentException {
        assert (ra != null);
        boolean[] wasZipped = new boolean[]{false};
        long num = 0;
        try ( Index idx = Index.load(idxName);  RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
            IndexBase pidx = indexByArg(type, idx);
            if (pidx.contains(value) == false) {
                throw new IOException("Deleted by key: " + value);
            }
            Long[] poss = pidx.get(value);
            for (long pos : poss) {
                Parking parking = (Parking) Buffer.readObject(raf, pos, null);
                ra.action(parking, pos, wasZipped[0], value);
            }
        }
        return num;
    }

    public static synchronized List<String> findByKey(String type, String value, int cmp)
            throws ClassNotFoundException, IOException, IllegalArgumentException {
        return findByKey(type, value, (cmp == 2) ? new KeyComp() : new KeyCompReverse());
    }

    public static synchronized List<String> findByKey(String type, String value, Comparator<String> comp)
            throws ClassNotFoundException, IOException, IllegalArgumentException {
        ArrayList<String> list = new ArrayList<>();
        findByKey(type, value, comp, new ReadAction() {
            @Override
            public void action(Parking parking, long pos, boolean zipped, String key) {
                assert (parking != null);
                list.add(parking.toString());
            }
        });
        return list;
    }

    public static synchronized long findByKey(String type, String value, Comparator<String> comp, ReadAction ra)
            throws ClassNotFoundException, IOException, IllegalArgumentException {
        long num = 0;
        boolean[] wasZipped = new boolean[]{false};
        try ( Index idx = Index.load(idxName);  RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
            IndexBase pidx = indexByArg(type, idx);
            if (pidx.contains(value) == false) {
                throw new IOException("Key not found: " + value);
            }
            String[] keys = pidx.getKeys(comp);
            for (String key : keys) {
                if (!key.equals(value)) {
                    Long[] poss = pidx.get(key);
                    for (long pos : poss) {
                        Parking parking = (Parking) Buffer.readObject(raf, pos, wasZipped);
                        ra.action(parking, pos, wasZipped[0], key);
                    }
                }
                else {
                    break;
                }
            }
        }
        return num;
    }

    public static synchronized void deleteFile(String type, String value)
            throws ClassNotFoundException, IOException, KeyNotUniqueException,
            IllegalArgumentException {
        Long[] poss;
        try ( Index idx = Index.load(idxName)) {
            IndexBase pidx = indexByArg(type, idx);
            if (pidx.contains(value) == false) {
                throw new IOException("Key not found: " + value);
            }
            poss = pidx.get(value);
        }
        backup();
        Arrays.sort(poss);
        try ( Index idx = Index.load(idxName);  RandomAccessFile fileBak = new RandomAccessFile(fileNameBak, "rw");  RandomAccessFile file = new RandomAccessFile(fileName, "rw")) {
            boolean[] wasZipped = new boolean[]{false};
            long pos;
            while ((pos = fileBak.getFilePointer()) < fileBak.length()) {
                Parking parking = (Parking) Buffer.readObject(fileBak, pos, wasZipped);
                if (Arrays.binarySearch(poss, pos) < 0) { // if not found in deleted
                    long ptr = Buffer.writeObject(file, parking, wasZipped[0]);
                    idx.put(parking, ptr);
                }
            }
        }
    }
}
