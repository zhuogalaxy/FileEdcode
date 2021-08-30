package com.zjb.fileutils;

import java.io.File;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        //System.out.println(Runtime.getRuntime().availableProcessors());
        String source = "D:\\test\\test-20210823\\encode\\designer-origin-7G\\designer-origin-7G_key.properties";
        String dest = "D:\\test\\test-20210823\\decode\\designer-origin-7G\\designer-origin-7G_key.properties";
        File sourceFile = new File(source);
        File destFile = new File(dest);

        Utils.copyFileUsingApacheCommonsIO(sourceFile,destFile);


    }




}
