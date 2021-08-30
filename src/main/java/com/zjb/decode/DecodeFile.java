package com.zjb.decode;

import com.fri.alg.sm.sm4.SM4Utils;
import com.zjb.Config.Constant;
import com.zjb.Config.DecodeThread;
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

public class DecodeFile {

    public static void main(String[] args) {
        //String srcFilePath = "D:\\test\\test-20210823\\encode\\designer_v8";
        //String destPath = "D:\\test\\test-20210823\\decode";

        //String decodeDir = mulitDeFile(srcFilePath,destPath);

        String decodeDir = mulitDeFile(args[0],args[1]);
        System.out.println("decode success");

        OperateFile of =  new OperateFile();

        of.mergeFile(decodeDir);

        System.out.println("merge success");

    }

    /**
     * 解密
     * @param srcFilePath  源文件
     * @param destPath  目标文件存放路径
     * @param randomN  随机数
     * @return
     */
    public boolean deFile(String srcFilePath, String destPath, String randomN) {
        if (srcFilePath == null || srcFilePath.isEmpty()) {
            System.out.println("deFile: srcDir is null");
            return false;
        }
        if (destPath == null || destPath.isEmpty()){
            System.out.println("deFile: destDir is null");
            return false;
        }
        String destFilePath = null;
        InputStream input = null;
        RandomAccessFile out_raf=null;
        boolean b=false;
        try {

            //新的加密，以 16M 为界限，每16M使用doDecrypt_ecb加密，最后left使用doFinalDecrypt_ecb加密
            //解密，以16M 为界限，每16M使用doDecrypt_ecb解密，最后left使用doFinalDecrypt_ecb解密
            //有个问题，假如 源文件正好16M，加密后会变成（16M + 16）字节，也即是最后一个left要以（16M+16字节）为界限
            //(1) 假如最后一块 <=16M && >16字节，正常
            //（2）假如最后一块为<=16字节，则将倒数第二块和最后一块，一并使用doFinalDecrypt_ecb解密

            File file = new File(srcFilePath);
            //destFilePath = destPath + file.getName().split("@",2)[0];
            if(Utils.judgeSystem()==0){
                destFilePath = destPath + "\\" + file.getName().split("\\.")[0]+Constant.SPLIT_SUFFIX;
            }else{
                destFilePath = destPath + "/" + file.getName().split("\\.")[0]+Constant.SPLIT_SUFFIX;
            }
            //InputStream input = null;
            if(file.isFile()){
                input =new FileInputStream(file);
            }else{
                return false;
            }
            long fileLength = file.length();
            if(fileLength<=0){
                //b=false;
                return false;
            }
            //定义一个16MB缓冲区
            long BUFF_LEN = 16 * 1024 * 1024;  //16M
            //long BUFF_LEN = Constant.SIZE;
            long left = fileLength % BUFF_LEN;
            long times = fileLength / BUFF_LEN+1;
            //byte[] buf = new byte[(int) BUFF_LEN];
            SM4Utils sm4 = new SM4Utils();

            sm4.setSecretKey(Base64.getDecoder().decode(randomN));
            //sm4.setSecretKey(randomN);
            int len=0;
            out_raf = new RandomAccessFile(destFilePath, "rw");
            out_raf.seek(0);
            if(fileLength<=BUFF_LEN){
                byte[] buf = new byte[(int) fileLength];
                //sm4.setSecretKey(Base64.getDecoder().decode(deEvpData));  //随机密钥
                //buff = sm4.doEncrypt_ecb(buff);
                input.read(buf);
                buf = sm4.doFinalDecrypt_ecb(buf);
                out_raf.write(buf);
            }else{
                if(left<=16){
                    if(times>2){
                        for(int i = 0; i < times-2; i++){
                            byte[] buf = new byte[(int) BUFF_LEN];
                            //随机密钥
                            //buff = sm4.doEncrypt_ecb(buff);
                            input.read(buf);
                            buf = sm4.doDecrypt_ecb(buf);
                            out_raf.write(buf);
                        }
                        byte[] buf = new byte[(int) (BUFF_LEN+left)];
                        //sm4.setSecretKey(Base64.getDecoder().decode(deEvpData));  //随机密钥
                        //buff = sm4.doEncrypt_ecb(buff);
                        input.read(buf);
                        buf = sm4.doFinalDecrypt_ecb(buf);
                        out_raf.write(buf);
                    }else {
                        byte[] buf = new byte[(int) (BUFF_LEN+left)];
                        //sm4.setSecretKey(Base64.getDecoder().decode(deEvpData));  //随机密钥
                        //buff = sm4.doEncrypt_ecb(buff);
                        input.read(buf);
                        buf = sm4.doFinalDecrypt_ecb(buf);
                        out_raf.write(buf);
                    }
                }else{
                    for(int i = 0; i < times; i++){
                        if(i==(times-1)){
                            byte[] buf = new byte[(int) left];
                            //sm4.setSecretKey(Base64.getDecoder().decode(deEvpData));  //随机密钥
                            //buff = sm4.doEncrypt_ecb(buff);
                            input.read(buf);
                            buf = sm4.doFinalDecrypt_ecb(buf);
                            out_raf.write(buf);
                        }else{
                            byte[] buf = new byte[(int) BUFF_LEN];
                            //sm4.setSecretKey(Base64.getDecoder().decode(deEvpData));  //随机密钥
                            //buff = sm4.doEncrypt_ecb(buff);
                            input.read(buf);
                            buf = sm4.doDecrypt_ecb(buf);
                            out_raf.write(buf);
                        }
                    }
                }

            }
            if(input!=null){
                input.close();
            }
            out_raf.close();
            b=true;

            if(b){
                //System.out.println("deFile: decode file success");
                //logger.info("deFile: decode file success");
            }else{
                System.out.println("deFile: decode file fail");
                //logger.error("deFile: decode file fail");
            }
            //long end_3=System.currentTimeMillis();
            //logger.info("writeFile use time: " + (end_3-start_3) + " ms");
        } catch (IOException e) {
            System.out.println(e.getStackTrace());
            //e.printStackTrace();
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
        return b;
    }

    public static String mulitDeFile(String srcDir, String destDir){
        if(srcDir==null || srcDir.isEmpty()){
            System.out.println("mulitDeFile,file path is null or empty");
            return null;
        }
        if(destDir==null || destDir.isEmpty()){
            System.out.println("mulitDeFile,file path is null or empty");
            return null;
        }

        File srcFile = new File(srcDir);
        if(!srcFile.isDirectory()){
            System.out.println("\""+srcDir+"\""+" is not a directory or not exist");
            return null;
        }

        File[] bef_proFiles = srcFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(Constant.PROPERTIES);
                //return name.endsWith(".properties");
            }
        });
        if(bef_proFiles.length!=1){
            System.out.println("properties is not unique");
            return null;
        }
        System.out.println(bef_proFiles[0].getAbsolutePath());
        boolean renameFlag = bef_proFiles[0].renameTo(new File(bef_proFiles[0].getAbsolutePath().split("\\.")[0]+Constant.PRO_SUFFIX));
        if(!renameFlag){
            System.out.println("rename fail");
            return null;
        }
        File[] datFiles = srcFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(Constant.DATA_SUFFIX);
            }
        });

        File[] proFiles = srcFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                //return name.startsWith(Constant.PROPERTIES);
                return name.endsWith(".properties");
            }
        });
        if(proFiles.length!=1){
            System.out.println("properties is not unique");
            return null;
        }
        String randomN = null;
        int splitCount = 0;
        FileInputStream fis = null;
        boolean b = false;
        String destFileName = null;
        String newProFile = null;
        try{
            Properties pro = new Properties();
            fis = new FileInputStream(proFiles[0]);
            pro.load(fis);
            randomN = pro.getProperty("randomN");
            splitCount = Integer.valueOf(pro.getProperty("partCount"));
            destFileName = pro.getProperty("fileName");
            //新建个目标路径，
            if(Utils.judgeSystem()==0){
                destDir = destDir + "\\" + destFileName.split("\\.")[0];
                newProFile = destDir + "\\" + proFiles[0].getName();
            }else{
                destDir = destDir + "/" + destFileName.split("\\.")[0];
                newProFile = destDir + "/" + proFiles[0].getName();
            }
            File destFileDir = new File(destDir);
            if(!destFileDir.isDirectory()){
                System.out.println("\""+destFileDir+"\""+" is not a directory or not exist");
                System.out.println("create a new directory "+"\""+destFileDir+"\"");
                destFileDir.mkdir();
                //return;
            }
            if(splitCount!=datFiles.length){
                System.out.println("inconsistent number of data files");
                fis.close();
                return null;
            }
            fis.close();

            Utils.copyFileUsingApacheCommonsIO(proFiles[0],new File(newProFile));

            List<File> allFileList = new ArrayList<>();
            for (int i = 0; i < datFiles.length; i++) {
                //System.out.println(partFiles[i].getAbsolutePath());
                allFileList.add(datFiles[i]);
            }
            List<List<File>> ave = Utils.averageAssign(allFileList,datFiles.length);

            ThreadPoolConfig thconfig = new ThreadPoolConfig(2*Runtime.getRuntime().availableProcessors()+1,Integer.MAX_VALUE,30,10);

            ThreadPoolExecutor threadPool = new ThreadPoolExecutor(thconfig.getCorePoolSize(), thconfig.getMaxPoolSize(), thconfig.getKeepAliveTime(), TimeUnit.SECONDS, new LinkedBlockingQueue<>(thconfig.getQueueCapacity()),new ThreadPoolExecutor.CallerRunsPolicy());
            threadPool.allowCoreThreadTimeOut(true);
            for (int i=0; i<ave.size();i++){
                threadPool.execute(new DecodeThread(ave.get(i),destDir,randomN));
            }
            //判断所有线程结束
            threadPool.shutdown();  //启动顺序关闭，执行之前提交的线程任务，但不接受新任务
            while (true){
                if(threadPool.isTerminated()){
                    //System.out.println("all sub thread end");
                    break;
                }
            }
//            if(threadPool.getActiveCount()==0){
//                b = true;
//            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return destDir;
    }

}
