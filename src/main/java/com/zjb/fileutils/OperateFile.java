package com.zjb.fileutils;

import com.fri.alg.sm.sm3.SM3Digest;
import com.zjb.Config.Constant;
import com.zjb.Config.EncodeThread;
import com.zjb.Config.ThreadPoolConfig;
import com.zjb.encode.EncodeFile;
import org.springframework.core.task.TaskExecutor;

import java.io.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OperateFile {

    private static final int SINGLE_SIZE = 512 * 1024 * 1024;  //定义单个文件的大小 1024MB
    private static final int SIZE = 16 * 1024 * 1024;  // 缓冲区大小  16M
    private static final String SUFFIX=".spi";

    public static void main(String[] args) throws Exception {
        String encodeDestDir = "D:\\test\\test-20210823\\encode";
        String splitDestDir="D:\\test\\test-20210823\\splitfile";
        String srcFile="D:\\test\\test-20210823\\designer-origin-7G.rar";
        //File file=new File(fileName);
        //File file=new File(args[0]);
        long startTime= System.currentTimeMillis();
        splitFile(srcFile,splitDestDir);

        EncodeFile en = new EncodeFile();
        String randomN = java.util.UUID.randomUUID().toString().replace("-","");
        File dir = new File(splitDestDir);
        File[] partFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(SUFFIX);
            }
        });
//        for (int i = 0; i < partFiles.length; i++) {
//            System.out.println(partFiles[i].getName());
//            String destPath = encodeDestDir + "\\"+partFiles[i].getName();
//            en.enFile(partFiles[i].getAbsolutePath(),destPath,randomN);
//        }
        List<File> allFileList = new ArrayList<>();
        for (int i = 0; i < partFiles.length; i++) {
            //System.out.println(partFiles[i].getAbsolutePath());
            allFileList.add(partFiles[i]);
        }
        List<List<File>> ave = Utils.averageAssign(allFileList,partFiles.length);

//        for (int i=0; i<ave.size();i++){
//            System.out.println(ave.get(i));
//            int finalI = i;
//            Thread th = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    for (int j = 0; j < ave.get(finalI).size(); j++) {
//                        //System.out.println(partFiles[i].getName());
//                        String destPath = encodeDestDir + "\\"+ ave.get(finalI).get(j).getName();
//                        en.enFile(ave.get(finalI).get(j).getAbsolutePath(),destPath,randomN);
//                    }
//                    long endTime= System.currentTimeMillis();
//                }
//            });
//            long thstartTime= System.currentTimeMillis();
//            th.start();
//            long thendTime= System.currentTimeMillis();
//            System.out.println("thread-" + i +" encode file use time: "+ (float)(thendTime-thstartTime)/1000 + " s");
//        }
        //corePoolSize 2*N+1(N表示CPU核数)，maxPoolSize Integer.MAX_VALUE, keepAliveTime 10s
        ThreadPoolConfig thconfig = new ThreadPoolConfig(2*Runtime.getRuntime().availableProcessors()+1,Integer.MAX_VALUE,30,10);

        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(thconfig.getCorePoolSize(), thconfig.getMaxPoolSize(), thconfig.getKeepAliveTime(), TimeUnit.SECONDS, new LinkedBlockingQueue<>(thconfig.getQueueCapacity()),new ThreadPoolExecutor.CallerRunsPolicy());
        threadPool.allowCoreThreadTimeOut(true);
        for (int i=0; i<ave.size();i++){
            threadPool.execute(new EncodeThread(encodeDestDir,randomN,ave.get(i)));
            //taskExecutor.execute(new EncodeThread(encodeDestDir,randomN,ave.get(i)));
        }
        //mergeFile(destDir);
        long endTime=System.currentTimeMillis();
        System.out.println("split file use time: "+ (float)(endTime-startTime)/1000 + " s");
    }


    /**
     * java 文件流
     * @param srcFile
     * @param destDir
     */
    public static void splitFile(String srcFile, String destDir) {

        if(srcFile==null || srcFile.isEmpty()){
            System.out.println("source file path is incorrect, null or is empty");
            return;
        }
        if(destDir==null || destDir.isEmpty()){
            System.out.println("dest path is incorrect, null or is empty");
            return;
        }
        File file = new File(srcFile);

//        if(Utils.judgeSystem()==0){
//            destDir = destDir + "\\" + file.getName().split("\\.")[0];
//        }else{
//            destDir = destDir + "/" + file.getName().split("\\.")[0];
//        }

        File destFile = new File(destDir);
        if(!file.isFile()){
            System.out.println("\""+srcFile+"\""+" is not a file or not exist");
            return;
        }
        if(!destFile.isDirectory()){
            System.out.println("\""+destDir+"\""+" is not a directory or not exist");
            System.out.println("create a new directory "+"\""+destDir+"\"");
            destFile.mkdir();
            //return;
        }
        FileInputStream fs = null;
        FileOutputStream fo = null;
        Properties pro = null;
        byte[] b = new byte[SIZE];
        int len = 0;
        int count = 0;
        int fileCount = 0;
        long fileLength = 0;
        String randomN = null;

        EncodeFile en = new EncodeFile();
        try {
            fs = new FileInputStream(file);
            /**
             * 切割文件时，记录 切割文件的名称和切割的子文件个数以方便合并
             * 这个信息为了简单描述，使用键值对的方式，用到了properties对象
             */
            pro = new Properties();
            fileLength=file.length();
            for( count =0; count<Math.ceil((float)fileLength/SINGLE_SIZE); count++){
                fileCount = 0;
                fo = new FileOutputStream(new File(destDir, file.getName().split("\\.")[0]+"@"+(int)Math.ceil((float)fileLength/SINGLE_SIZE)+"_"+count + SUFFIX));
                // 切割文件
                while ((len = fs.read(b)) != -1) {
                    //randomN = java.util.UUID.randomUUID().toString().replace("-","");
                    //b=en.sm4Encode_final(randomN ,b);
                    fo.write(b, 0, len);
                    fileCount += len;
                    if(fileCount>=SINGLE_SIZE){
                        break;
                    }
                }
                fo.flush();
                fo.close();
            }
            randomN = java.util.UUID.randomUUID().toString().replace("-","");
            // 将被切割的文件信息保存到properties中
            pro.setProperty("randomN",randomN);
            pro.setProperty("partCount", count + "");
            pro.setProperty("fileName", file.getName());

            //fo = new FileOutputStream(new File(destDir, file.getName().split("\\.")[0] + "_key" + ".properties"));

            fo = new FileOutputStream(new File(destDir, "properties#" + file.getName().split("\\.")[0] + ".properties"));

            // 写入properties文件
            pro.store(fo, "save file info");
            fo.close();
            fs.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

    }

    //备份
    public static void splitFile_2(String srcFile, String destDir) {

        if(srcFile==null || srcFile.isEmpty()){
            System.out.println("source file path is incorrect, null or is empty");
            return;
        }
        if(destDir==null || destDir.isEmpty()){
            System.out.println("dest path is incorrect, null or is empty");
            return;
        }
        File file = new File(srcFile);
        File destFile = new File(destDir);
        if(!file.isFile()){
            System.out.println("\""+srcFile+"\""+" is not a file or not exist");
            return;
        }
        if(!destFile.isDirectory()){
            System.out.println("\""+destDir+"\""+" is not a directory or not exist");
            System.out.println("create a new directory "+"\""+destDir+"\"");
            destFile.mkdir();
            //return;
        }
        FileInputStream fs = null;
        FileOutputStream fo = null;
        Properties pro = null;
        byte[] b = new byte[SIZE];
        int len = 0;
        int count = 0;
        int fileCount = 0;
        long fileLength = 0;

        try {
            fs = new FileInputStream(file);
            /**
             * 切割文件时，记录 切割文件的名称和切割的子文件个数以方便合并
             * 这个信息为了简单描述，使用键值对的方式，用到了properties对象
             */
            pro = new Properties();
            fileLength=file.length();
            for( count =0; count<Math.ceil((float)fileLength/SINGLE_SIZE); count++){
                fileCount = 0;
                fo = new FileOutputStream(new File(destDir, count + ".zip"));
                // 切割文件
                while ((len = fs.read(b)) != -1) {
                    fo.write(b, 0, len);
                    fileCount += len;
                    if(fileCount>=SINGLE_SIZE){
                        break;
                    }
                }
                fo.flush();
                fo.close();
            }

            // 将被切割的文件信息保存到properties中
            pro.setProperty("partCount", count + "");
            pro.setProperty("fileName", file.getName());
            fo = new FileOutputStream(new File(destDir, (count++) + ".properties"));
            // 写入properties文件
            pro.store(fo, "save file info");
            fo.close();
            fs.close();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     *
     * @param destDir
     */
    public static void mergeFile(String destDir) {
        if(destDir==null || destDir.isEmpty()){
            System.out.println("dest path is incorrect, null or is empty");
            return;
        }
        File dir = new File(destDir);
        if(!dir.isDirectory()){
            System.out.println("\""+destDir+"\""+" is not a directory or not exist");
            return;
        }
        // 读取properties文件的拆分信息
        File[] proFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".properties");
            }
        });
        if(proFiles.length!=1){
            System.out.println("\""+destDir+"\""+", properties not exist or not unique");
            return;
        }

        try{
            File file = proFiles[0];
            // 获取该文件的信息
            Properties pro = new Properties();
            FileInputStream fis = new FileInputStream(file);
            pro.load(fis);
            String fileName = pro.getProperty("fileName");
            int splitCount = Integer.valueOf(pro.getProperty("partCount"));

            //System.out.println("splitCount = "+splitCount);

            File[] partFiles = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(SUFFIX);
                }
            });
            if(partFiles.length != splitCount){
                System.out.println("\""+destDir+"\""+", part files number is incorrect");
                return;
            }
            //System.out.println(partFiles.length);
            // 将碎片文件存入到集合中
            List<FileInputStream> al = new ArrayList<FileInputStream>();
            // 获取该目录下所有的碎片文件
            for (int i = 0; i < splitCount; i++) {
                //System.out.println(partFiles[i].getName());
                al.add(new FileInputStream(partFiles[i]));
            }
            // 构建文件流集合
            Enumeration<FileInputStream> en = Collections.enumeration(al);
            // 将多个流合成序列流
            SequenceInputStream sis = new SequenceInputStream(en);
            FileOutputStream fos = new FileOutputStream(new File(destDir, fileName));
            byte[] b = new byte[SIZE];
            int len = 0;
            while ((len = sis.read(b)) != -1) {
                fos.write(b, 0, len);
            }
            fis.close();
            for(int i=0;i<al.size();i++){
                if(al.get(i)!=null){
                    al.get(i).close();
                }
            }
            fos.flush();
            fos.close();
            sis.close();


        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }

    }


    /**
     * 获取文件的sm3摘要值, 部分摘要
     * @param destFilePath
     * @param buffsize   对结尾的(length%buffsize)字节做摘要，此处 buffsize=16*1024*1024, 若文件小于buffsize, 则对整个文件摘要
     * @return
     */
    public String getPartSm3Digest(String destFilePath,long buffsize)  {
        if (destFilePath == null || destFilePath.isEmpty()){
            System.out.println(destFilePath + " is null or empty");
            //logger.error(destFilePath + " is null or empty");
            return null;
        }
        String hashData64=null;
        InputStream in=null;
        byte[] md=new byte[32];
        File file = new File(destFilePath);
        try {
            if (file.isFile()) {  //判断是否是目录，是目录会报错
                in = new FileInputStream(file);
            } else {
                System.out.println(destFilePath + " is not file");
                //logger.error(destFilePath + " is not file");
                return null;
            }
            long fileLength = file.length();
            if (fileLength <= 0) {
                System.out.println(destFilePath + " length is 0");
                //logger.error(destFilePath + " length is 0");
                return null;
            }
            //定义一个16MB缓冲区
            //long BUFF_LEN = 16 * 1024 * 1024;  //16M
            long BUFF_LEN = buffsize;  //32
            long left = fileLength % BUFF_LEN;
            long times = fileLength / BUFF_LEN+1;
            SM3Digest sm3 = new SM3Digest();
            Base64.Encoder b64e = Base64.getEncoder();
            if(fileLength<=BUFF_LEN){
                byte[] buf = new byte[(int) fileLength];
                in.read(buf);
                sm3.update(buf,0, (int) fileLength);
                sm3.doFinal(md,0);
                //return b64e.encodeToString(md);
            }else{
                for(int i = 0; i < times; i++){
                    if(i==(times-1)){
                        byte[] buf = new byte[(int) left];
                        in.read(buf);
                        sm3.update(buf,0, (int) left);
                        sm3.doFinal(md,0);
                    }else{
                        byte[] buf = new byte[(int) BUFF_LEN];
                        in.read(buf);
                        //
                    }
                }
            }
            in.close();
            hashData64 = b64e.encodeToString(md);
        } catch (IOException e) {
            e.printStackTrace();
            //logger.error(e.getMessage());
            return null;
        }finally {
            try {
                if(in!=null){
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return hashData64;
    }

    /**
     *  获取部分摘要，并对文件重命名
     * @param srcFilePath
     * @return
     */
    public boolean getDigestAndRename(String srcFilePath){
        boolean b=false;
        if (srcFilePath == null || srcFilePath.isEmpty()) {
            System.out.println("doFileDigest: srcFilePath is null or empty");
            //logger.error("doFileDigest: srcFilePath is null or empty");
            return b;
        }
        File file = new File(srcFilePath);
        if(!file.isFile()){
            System.out.println("doFileDigest: srcFilePath is not file");
            //logger.error("doFileDigest: srcFilePath is not file");
            return b;
        }
        String hashData64 = getPartSm3Digest(srcFilePath, Constant.BUFFIZE);
        if(hashData64!=null){
            File oldFile = new File(srcFilePath);
            //String newName = oldFile.getName().substring(0,oldFile.getName().length()-4)+"@"+hashData64+".dat";
            String newName = oldFile.getName().split("\\.")[0]+"@"+hashData64+".dat";
            String destPath = srcFilePath.split(oldFile.getName(),2)[0];
            String destFilePath = destPath+newName;
            destFilePath = destFilePath.replace("/","-");
            File newFile = new File(destFilePath);
            if(oldFile.renameTo(newFile)){
                //System.out.println(destFilePath + " rename success");
                b=true;
            }else{
                //System.out.println(destFilePath + " rename fail");
            }
        }else{
            System.out.println(srcFilePath + "getPartSm3Digest is null");
        }
        return b;
    }




}


