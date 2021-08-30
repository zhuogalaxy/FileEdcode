package com.zjb.encode;

import com.fri.alg.sm.sm4.SM4Utils;
import com.fri.alg.sm.sm4.SM4_Context;
import com.zjb.Config.Constant;
import com.zjb.Config.EncodeThread;
import com.zjb.Config.ThreadPoolConfig;
import com.zjb.fileutils.OperateFile;
import com.zjb.fileutils.Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EncodeFile {

    private static final int SINGLE_SIZE = 512 * 1024 * 1024;  //定义单个文件的大小 1024MB
    private static final int SIZE = 16 * 1024 * 1024;  // 缓冲区大小  16M
    private static final String SPLIT_SUFFIX=".spi";
    private static final String DATA_SUFFIX=".dat";


    public static void main(String[] args) {

        //String encodeDestDir = "D:\\test\\test-20210823\\encode";
        //String srcFile="D:\\test\\test-20210823\\designer_v8.3_20210608.zip";

        //mulitEnFile(srcFile,encodeDestDir);
        mulitEnFile(args[0],args[1]);



    }

    /**
     * 加密
     * @param srcFilePath  源文件，绝对路径
     * @param destFilePath  目标文件，绝对路径
     * @param randomN  随机数
     * @return
     */
    public boolean enFile(String srcFilePath, String destFilePath,String randomN) {
        if (srcFilePath == null || srcFilePath.isEmpty()) {
            System.out.println("enFile: srcFilePath is null or empty");
            return false;
        }
        if (destFilePath == null || destFilePath.isEmpty()){
            System.out.println("enFile: destFilePath is null or empty");
            return false;
        }
        boolean b=false;
        OperateFile of = new OperateFile();
        //String randomN = java.util.UUID.randomUUID().toString().replace("-","");

        //新的加密，以 16M 为界限，每16M使用doDecrypt_ecb加密，最后left使用doFinalDecrypt_ecb加密
        //解密，以16M 为界限，每16M使用doDecrypt_ecb解密，最后left使用doFinalDecrypt_ecb解密
        //有个问题，假如 源文件正好16M，加密后会变成（16M + 16）字节，也即是最后一个left要以（16M+16字节）为界限
        InputStream input =null;
        RandomAccessFile out_raf=null;
        try {
            File file = new File(srcFilePath);
            if(file.isFile()){  //判断是否是目录，是目录会报错
                input = new FileInputStream(file);
            }else{
                return b;
            }
            long fileLength = file.length();
            if(fileLength<=0){
                //b=false;
                return b;
            }
            //定义一个16MB缓冲区
            long BUFF_LEN = 16 * 1024 * 1024;  //16M
            long left = fileLength % BUFF_LEN;
            long times = fileLength / BUFF_LEN+1;
            //byte[] buf = new byte[(int) BUFF_LEN];
            SM4Utils sm4 = new SM4Utils();
            //int len=0;
            destFilePath=destFilePath.split("\\.")[0] + DATA_SUFFIX;
            out_raf = new RandomAccessFile(destFilePath, "rw");
            out_raf.seek(0);
            if(fileLength<=BUFF_LEN){
                byte[] buf = new byte[(int) fileLength];
                sm4.setSecretKey(Base64.getDecoder().decode(randomN));  //随机密钥
                //sm4.setSecretKey(randomN);
                //buff = sm4.doEncrypt_ecb(buff);
                input.read(buf);
                buf = sm4.doFinalEncrypt_ecb(buf);
                out_raf.write(buf);
            }else{
                for(int i = 0; i < times; i++){
                    if(i==(times-1)){
                        byte[] buf = new byte[(int) left];
                        sm4.setSecretKey(Base64.getDecoder().decode(randomN));  //随机密钥
                        //buff = sm4.doEncrypt_ecb(buff);
                        input.read(buf);
                        buf = sm4.doFinalEncrypt_ecb(buf);
                        out_raf.write(buf);
                    }else{
                        byte[] buf = new byte[(int) BUFF_LEN];
                        sm4.setSecretKey(Base64.getDecoder().decode(randomN));  //随机密钥
                        //buff = sm4.doEncrypt_ecb(buff);
                        input.read(buf);
                        buf = sm4.doEncrypt_ecb(buf);
                        out_raf.write(buf);
                    }
                }
            }
            //if(file.)
            if(input!=null){
                input.close();
            }
            if(out_raf!=null){
                out_raf.close();
            }
            b=true;
        }catch (IOException e){
            System.out.println("ExceptionTest Exception: "+ e.getStackTrace());
            //logger.info("ExceptionTest Exception:",e);
            //logger.error("ExceptionTest Exception:",e);
        }finally {
            try {
                if(input!=null){
                    input.close();
                }
                if(out_raf!=null){
                    out_raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        b = of.getDigestAndRename(destFilePath);
        return b;
    }

    /**
     * 批量文件加密
     * @param srcFile  源文件
     * @param encodeDestDir  目标文件存储目录
     */
    public static void mulitEnFile(String srcFile,String encodeDestDir){
        if(srcFile==null || srcFile.isEmpty()){
            System.out.println("mulitEnFile,file path is null or empty");
            return;
        }
        if(encodeDestDir==null || encodeDestDir.isEmpty()){
            System.out.println("mulitEnFile,file path is null or empty");
            return;
        }
        File file = new File(srcFile);

        if(Utils.judgeSystem()==0){
            encodeDestDir = encodeDestDir + "\\" + file.getName().split("\\.")[0];
        }else{
            encodeDestDir = encodeDestDir + "/" + file.getName().split("\\.")[0];
        }
        File dir = new File(encodeDestDir);
        if(!dir.isDirectory()){
            //System.out.println("\""+encodeDestDir+"\""+" is not a directory or not exist");
            //System.out.println("create a new directory "+"\""+encodeDestDir+"\"");
            dir.mkdir();
            //return;
        }

        OperateFile of = new OperateFile();
        long startTime= System.currentTimeMillis();
        of.splitFile(srcFile,encodeDestDir);

        EncodeFile en = new EncodeFile();
        //String randomN = java.util.UUID.randomUUID().toString().replace("-","");
        String randomN = getRandomN(encodeDestDir);
        //System.out.println("randomN = "+randomN);

        File[] proFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(Constant.PRO_SUFFIX);
            }
        });

        File[] partFiles = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(Constant.SPLIT_SUFFIX);
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
        System.out.println("split file take "+ (float)(endTime-startTime)/1000 + " s");
        //判断所有线程结束
        threadPool.shutdown();  //启动顺序关闭，执行之前提交的线程任务，但不接受新任务
        while (true){
            if(threadPool.isTerminated()){
                //System.out.println("all sub thread end");
                break;
            }
        }
        of.getDigestAndRename(proFiles[0].getAbsolutePath());


    }

    /**
     * 获取 properties 文件中的 randomN
     * @param encodeDestDir  目标文件存储目录
     * @return
     */
    public static String getRandomN(String encodeDestDir) {
        if(encodeDestDir==null || encodeDestDir.isEmpty()){
            System.out.println("getRandomN,file path is null or empty");
            return null;
        }
        String randomN = null;
        FileInputStream fis = null;
        File destDir = new File(encodeDestDir);
        File[] proFiles = destDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(Constant.PRO_SUFFIX);
            }
        });
        if(proFiles.length!=1){
            System.out.println("\""+encodeDestDir+"\""+", properties not exist or not unique");
            return null;
        }
        // 读取properties文件的拆分信息
        try {
            // 获取该文件的信息
            Properties pro = new Properties();
            fis = new FileInputStream(proFiles[0]);
            pro.load(fis);
            randomN = pro.getProperty("randomN");
            //int splitCount = Integer.valueOf(pro.getProperty("partCount"));
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return randomN;
    }




}
