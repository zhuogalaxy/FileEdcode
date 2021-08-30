package com.zjb.Config;

import com.zjb.decode.DecodeFile;
import com.zjb.encode.EncodeFile;
import com.zjb.fileutils.Utils;

import java.io.File;
import java.util.List;

public class DecodeThread implements Runnable{

    private String decodeDestDir;
    private String randomN;
    private List<File> list;

    public DecodeThread (List<File> list, String decodeDestDir,String randomN) {
        this.decodeDestDir = decodeDestDir;
        this.randomN = randomN;
        this.list = list;
    }

    @Override
    public void run() {
        long startTime= System.currentTimeMillis();
        //EncodeFile en = new EncodeFile();
        DecodeFile de = new DecodeFile();
        Utils utils = new Utils();
        boolean flag = false;
        for (int j = 0; j < list.size(); j++) {
            //System.out.println(partFiles[i].getName());
            //String destPath = decodeDestDir + "\\"+ list.get(j).getName();
            flag = de.deFile(list.get(j).getAbsolutePath(),decodeDestDir,randomN);
            if(flag){
                //utils.deleteFile(list.get(j).getAbsolutePath());
            }else{
                System.out.println(list.get(j).getAbsolutePath() + " ,delete split file fail");
            }
        }
        long endTime= System.currentTimeMillis();
        System.out.println(Thread.currentThread().getName()+" decode file take "+ (float)(endTime-startTime)/1000+ " s");

    }
}
