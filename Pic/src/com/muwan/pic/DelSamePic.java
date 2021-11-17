package com.muwan.pic;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author MuWan
 */
public class DelSamePic {
    public final static String DST_PATH = "C:\\BaiduNetdiskWorkspace\\School\\Codes\\测试";

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        // 获取图片文件
        ArrayList<String> picSuffixList = new ArrayList<>();
        Collections.addAll(picSuffixList, ".jpg", ".png", ".jpeg");
        File file = new File(DST_PATH);
        File[] files = file.listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null) {
            fileList = Arrays.stream(files).filter(file1 -> {
                String fileName = file1.getName();
                int dotIndex = fileName.indexOf(".");
                if (dotIndex > 0) {
                    return picSuffixList.contains(fileName.substring(dotIndex).toLowerCase());
                } else {
                    return false;
                }
            }).collect(Collectors.toList());
        }
        // 多线程IO加速
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(6, 8, 10, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5));
        ArrayList<Future<Map<File, HashBytes>>> futureList = new ArrayList<>();
        int threadCount = 5;
        for (int i = 0; i < threadCount; i++) {
            futureList.add(tpe.submit(new FindSameThread(fileList.subList(fileList.size() * i / threadCount, fileList.size() * (i + 1) / threadCount))));
        }
        Map<File, HashBytes> fileHashMap = new HashMap<>(100);
        try {
            for (Future<Map<File, HashBytes>> f : futureList) {
                fileHashMap.putAll(f.get());
            }
            tpe.shutdownNow();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        // 获取重复文件[[1组], [1组]]
        ArrayList<ArrayList<File>> sameFilesList = new ArrayList<>();
        for (Map.Entry<File, HashBytes> oneFileHash : fileHashMap.entrySet()) {
            // 获取一组
            ArrayList<File> sameFiles = new ArrayList<>();
            for (Map.Entry<File, HashBytes> nextFileHash : fileHashMap.entrySet()) {
                File nextK = nextFileHash.getKey();
                HashBytes nextV = nextFileHash.getValue();
                // 判断哈希值相同(需要重写byte[]的equals) + 不是自身
                if (nextV.equals(oneFileHash.getValue())
                        && !(nextK.getName().equals(oneFileHash.getKey().getName()))) {
                    // 判断是否已经添加过
                    boolean hasAdd = false;
                    for (ArrayList<File> oneFilesList : sameFilesList) {
                        if (oneFilesList.contains(nextK)) {
                            hasAdd = true;
                            break;
                        }
                    }
                    if (!hasAdd) {
                        sameFiles.add(nextK);
                    }
                }
            }
            // 添加自身 + 将一组添加到容器s
            if (sameFiles.size() > 0) {
                sameFiles.add(oneFileHash.getKey());
                sameFilesList.add(sameFiles);
            }
        }
        Path movePath = Path.of(DST_PATH, "重复图片");
        try {
            if (!Files.exists(movePath)) {
                Files.createDirectory(movePath);
            }
            int groupCount = 0, sameCount = 0;
            for (ArrayList<File> tempFiles : sameFilesList) {
                groupCount++;
                sameCount++;
                for (File tempFile : tempFiles.subList(1, tempFiles.size())) {
                    String newName = groupCount + "(" + sameCount + ")_" + tempFile.getName();
                    Files.move(Path.of(tempFile.toString()), Path.of(movePath.toString(), newName));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        long endTime = System.currentTimeMillis();
        System.out.println((float)(endTime - startTime) / 1000);
    }
}

class FindSameThread implements Callable<Map<File, HashBytes>> {
    private List<File> fileList;

    public FindSameThread(List<File> fileList) {
        this.fileList = fileList;
    }

    @Override
    public Map<File, HashBytes> call() {
        // 获取{文件:哈希值}的Map
        Map<File, HashBytes> fileHashMap = new HashMap<>(100);
        for (File temp : this.fileList) {
            try (InputStream is = new FileInputStream(temp)) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                fileHashMap.put(temp, new HashBytes(md.digest(IOUtils.toByteArray(is))));
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return fileHashMap;
    }
}