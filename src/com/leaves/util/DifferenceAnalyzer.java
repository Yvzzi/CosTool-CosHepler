package com.leaves.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import com.leaves.Main;

public class DifferenceAnalyzer {
	private COSUtil cosUtil = null;
	private String path = null;
	public static final String VERSION_PREFIX = "VERSION";
	private static final String VERSION_FILE_PREFIX = VERSION_PREFIX + "_";
	private static final String VERSION_CTRL_PREFIX = VERSION_PREFIX + "__";
	private static final int DEAFAULT_VERSION = 0;
	
	public DifferenceAnalyzer(COSUtil cosUtil, String path) {
		this.cosUtil = cosUtil;
		this.path = path;
	}
	
	public HashSet<Integer> list() {
		HashSet<Integer> tmp = new HashSet<Integer>();
		ArrayList<String> keys = cosUtil.listAllObjects();
		for (String v : keys) {
			if (v.startsWith(VERSION_FILE_PREFIX) && !v.startsWith(VERSION_CTRL_PREFIX)) {
				tmp.add(Integer.valueOf(v.substring((VERSION_FILE_PREFIX).length(), v.indexOf("/"))));
			}
		}
		return tmp;
	}
	
	public void head(int version) {
		ArrayList<String> keys = getKeyList();
		for (String v : keys) {
			cosUtil.delSingleFile(v);
		}
		keys = getKeyList(version);
		int index = (VERSION_FILE_PREFIX + version + "/").length();
		for (String v : keys) {
			cosUtil.copyFileForSameRegion(v, v.substring(index), cosUtil.getBucketName());
		}
		cosUtil.delSingleFile(VERSION_CTRL_PREFIX + getVersion());
		cosUtil.createKey(VERSION_CTRL_PREFIX + version);
	}
	
	public void store() {
		ArrayList<String> keys = cosUtil.listAllObjects();
		int version = getVersion();
		for (String v : keys) {
			if (v.startsWith(VERSION_FILE_PREFIX) && !v.startsWith(VERSION_CTRL_PREFIX)) {
				System.out.println(v);
				int kv = Integer.parseInt(v.substring((VERSION_FILE_PREFIX).length(), v.indexOf("/")));
				if (kv >= version) {
					cosUtil.delSingleFile(v);
				}
			}
		}
	}
	
	public int getVersion() {
		ArrayList<String> keys = cosUtil.listAllObjects();
		for (String v : keys) {
			if (v.startsWith(VERSION_CTRL_PREFIX)) {
				return Integer.parseInt(v.substring((VERSION_CTRL_PREFIX).length()));
			}
		}
		cosUtil.createKey(VERSION_CTRL_PREFIX + DEAFAULT_VERSION);
		return DEAFAULT_VERSION;
	}
	
	public void backup() {
		ArrayList<String> keys = getKeyList();
		int version = getVersion();
		for (String key : keys) {
			cosUtil.copyFileForSameRegion(key, VERSION_FILE_PREFIX + version + "/" + key, cosUtil.getBucketName());
		}
		cosUtil.delSingleFile(VERSION_CTRL_PREFIX + version);
		cosUtil.createKey(VERSION_CTRL_PREFIX + (version + 1));
	}
	
	public ArrayList<String> getKeyList() {
		return getKeyList(-1);
	}
	
	public ArrayList<String> getKeyList(int version) {
		ArrayList<String> keys = cosUtil.listAllObjects();
		ArrayList<String> tmp = new ArrayList<String>();
		for (String v : keys) {
			if (version == -1) {
				if (!v.endsWith("/") && !v.startsWith(VERSION_PREFIX)) {
					tmp.add(v);
				}
			} else {
				if (!v.endsWith("/") && v.startsWith(VERSION_FILE_PREFIX + version)) {
					tmp.add(v);
				}
			}
		}
		return tmp;
	}
	
	@SuppressWarnings("unchecked")
	public void GET(boolean force, boolean append) {
		File file = new File(path);
		ArrayList<String> keys = getKeyList();
		ArrayList<String> remoteKeys = (ArrayList<String>) keys.clone();
		ArrayList<String> localKeys = new ArrayList<String>();
		scanRootDirectory(file, localKeys, remoteKeys);
		if (localKeys.size() != 0 && !force) {
			System.out.println("There are some file existing in local, they aren't found in remote.");
			System.out.println("You can use java -jar CosHelper.jar -f get to force to get, it will delete conflict files, or get in another dir and then merge manually.");
			System.out.printf("%d files in total:\n", remoteKeys.size());
			for (String v : localKeys) {
				System.out.println("\t" + v);
			}
		} else {
			for (String v : remoteKeys) {
				System.out.printf("Download file %s\n", v);
				cosUtil.downloadFile(v);
			}
			if (localKeys.size() != 0 && !append) {
				for (String v : localKeys) {
					new File(Main.getDataPath() + v).delete();
					System.out.printf("Delete file %s\n", v);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void PUT(boolean force, boolean append) {
		File file = new File(path);
		ArrayList<String> keys = getKeyList();;
		ArrayList<String> remoteKeys = (ArrayList<String>) keys.clone();
		ArrayList<String> localKeys = new ArrayList<String>();
		scanRootDirectory(file, localKeys, remoteKeys);
		if (remoteKeys.size() != 0 && !force) {
			System.out.println("There are some file existing in remote, they aren't found in local.");
			System.out.println("You can use java -jar CosHelper.jar -f put to force to put, it will delete conflict files, or get in another dir and then merge manually.");
			System.out.printf("%d files in total:\n", remoteKeys.size());
			for (String v : remoteKeys) {
				System.out.println("\t" + v);
			}
		} else {
			for (String v : localKeys) {
				System.out.printf("Upload file %s\n", v);
				cosUtil.uploadFile(new File(Main.getDataPath() + v), v);
			}
			if (remoteKeys.size() != 0 && !append) {
				for (String v : remoteKeys) {
					cosUtil.delSingleFile(v);
					System.out.printf("Delete file %s\n", v);
				}
			}
		}
	}
	
	private void scanRootDirectory(File file, ArrayList<String> localKeys, ArrayList<String> remoteKeys) {
		if (!file.isDirectory())
			return;
		File[] files = file.listFiles();
		for (File f : files)
			scan(f, "", localKeys, remoteKeys);
	}
	
	private void scan(File file, String prefix, ArrayList<String> localKeys, ArrayList<String> remoteKeys) {
		if (file.isFile()) {
			String localKey = prefix + file.getName();
			if (remoteKeys.contains(localKey)) {			
				System.out.println(">> Exists local file " + localKey + ".");
				remoteKeys.remove(localKey);
			} else {
				localKeys.add(localKey);
			}
		} else {			
			File[] files = file.listFiles();
			for (File f : files) {
				scan(f, prefix + file.getName() + "/", localKeys, remoteKeys);
			}
		}
	}
}
