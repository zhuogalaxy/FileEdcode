# FileEdcode
Large file splitting and encryption. Decryption and file merging
本项目的功能是实现对大文件的拆分和SM4加密，解密和文件合并
1. 拆分，目前的拆分有两种思路，一是顺序读文件, 按照固定大小顺序写小文件，二是非顺序读文件，固定大小异步写。
   目前采用的是第一种，第二种也可以实现，但目前没有这方面的需求。
2. 加密， 采用异步多线程加密，考虑到环境因素，采用的线程池的方式，核心线程数为 2*N+1, 代表CPU核数。本项目采用国密SM4算法，密钥为32字节随机数。
3. 解密， 原理和加密类似。
4. 合并， 采用序列流的方式合并，目前未找到更好的合并方法，待解决。
   Enumeration<FileInputStream> en = Collections.enumeration(al);            
   SequenceInputStream sis = new SequenceInputStream(en);
