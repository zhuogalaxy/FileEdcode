package com.zjb.Config;

import com.zjb.encode.EncodeFile;
import com.zjb.fileutils.Utils;

import java.io.File;
import java.util.List;

public class EncodeThread implements Runnable {

    private String encodeDestDir;
    private String randomN;
    private List<File> list;

    public EncodeThread (String encodeDestDir,String randomN, List<File> list) {
        this.encodeDestDir = encodeDestDir;
        this.randomN = randomN;
        this.list = list;
    }

    @Override
    public void run() {
        long startTime= System.currentTimeMillis();
        EncodeFile en = new EncodeFile();
        Utils utils = new Utils();
        boolean flag = false;
        String destPath = null;
        for (int j = 0; j < list.size(); j++) {
            //System.out.println(partFiles[i].getName());
            if(Utils.judgeSystem()==0){
                destPath = encodeDestDir + "\\"+ list.get(j).getName();
            }else{
                destPath = encodeDestDir + "/"+ list.get(j).getName();
            }
            flag = en.enFile(list.get(j).getAbsolutePath(),destPath,randomN);
            if(flag){
                utils.deleteFile(list.get(j).getAbsolutePath());
            }else{
                System.out.println(list.get(j).getAbsolutePath() + " ,delete split file fail");
            }
        }
        long endTime= System.currentTimeMillis();
        System.out.println(Thread.currentThread().getName()+" encode file take "+ (float)(endTime-startTime)/1000+ " s");

    }
}
