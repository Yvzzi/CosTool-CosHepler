package com.leaves;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import com.leaves.util.COSUtil;
import com.leaves.util.DifferenceAnalyzer;

public class Main {
	/**
	 * Main of program
	 * Format of args:
	 * 	java -jar CosHelper.jar [<option> [<arg>]] <op>
	 * 		<option> <arg>
	 * 				-c <config-path>	Specific config file
	 * 				-f					Force to get or put
	 * 				-a					Force to get or put, don't delete conflict files
	 * 				-b					Backup current version
	 * 				-l					List all available version
	 * 				-d <version>		Go back to <version>
	 * 				-s <version>		Clear version, if you go back to <version>, you will be not able to get to the originally newest version such as <version> + 1
	 * 				-t					Test config, get files list when config is ok
	 * 				-v					Get version information
	 * 				-h					Get Help
	 * 		<op>
	 * 				init	Init config file with current path
	 * 				put		Upload to remote
	 * 				get		Download to local
	 * @param args
	 */
	public static final String NAME = "COSHelper";
	public static final String HELP_STRING = 
		"java -jar CosHelper.jar [<option> <arg>] <op>\n"
		+ "\t<option> <arg>\n"
		+ "\t\t-c <config-path>\tSpecific config file\n"
		+ "\t\t-f\t\t\t\t\tForce to get or put\n"
		+ "\t\t-a\t\t\t\t\tForce to get or put, don't delete conflict files\n"
		+ "\t\t-l\t\t\t\t\tList all available version\n"
		+ "\t\t-d <version>\t\tGo back to <version>\n"
		+ "\t\t-s <version>\t\tClear version, if you go back to <version>, you will be not able to get to the originally newest version such as <version> + 1\n"
		+ "\t\t-t\t\t\t\t\tTest config, get current version of bucket if it is ok\n"
		+ "\t\t-v\t\t\t\t\tGet version information\n"
		+ "\t\t-h\t\t\t\t\tGet Help\n"
		+ "\t<op>\n"
		+ "\t\tinit\tInit config file and data directory with current path\n"
		+ "\t\tput\tUpload to remote\n"
		+ "\t\tget\tDownload to local";
	public static final String DEAFAULT_CONFIG_PATH = "./config.properties";
	public static final String DEAFAULT_STORE_PATH = "./data";
	private static final int CONFIG_MASK = 0x01;
	private static final int FORCE_MASK = 0x02;
	private static final int APPEND_MASK = 0x04;
	private static final int BACKUP_MASK = 0x08;
	private static final int LIST_MASK = 0x10;
	private static final int HEAD_MASK = 0x20;
	private static final int STORE_MASK = 0x40;
	private static final int VERSION_MASK = 0x80;
	private static final int HELP_MASK = 0x100;
	private static final int TEST_MASK = 0x200;
	private static int option = 0x00;
	private static HashMap<Integer, String> optionArg = new HashMap<Integer, String>();
	private static String op = "";
	
	private static String bucketName = null;
	private static String region = null;
	private static String secretId = null;
	private static String secretKey = null;
	private static String runtimePath = null;
	
	private static COSUtil cosUtil = null;
	private static DifferenceAnalyzer differenceAnalyzer = null;
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
//		args = new String[] {"get"};
		runtimePath = System.getProperty("user.dir") + "/";
//		runtimePath = "C:\\Users\\Christy\\Desktop" + "/";
		if (!loadArg(args)) {
			System.out.println(HELP_STRING);
			System.exit(1);
		}
		// load config
		String configPath = getRuntimePath() + DEAFAULT_CONFIG_PATH;
		if ((option & CONFIG_MASK) != 0) {
			configPath = optionArg.get(CONFIG_MASK);
		}
		File file = new File(configPath);
		Properties properties = new Properties();
		
		// switch function
		if ((option & TEST_MASK) != 0) {
			load(file, properties);
			int version = differenceAnalyzer.getVersion();
			System.out.printf("The current version of bucket is %d.\n", version);
			System.exit(0);
		}
		
		if ((option & VERSION_MASK) != 0) {
			System.out.println(NAME + ".jar version 1.0.0 made by Yuzzi (2433988494@qq.com).");
			System.exit(0);
		}
		
		if ((option & HELP_MASK) != 0) {
			System.out.println(HELP_STRING);
			System.exit(0);
		}
		
		if ((option & BACKUP_MASK) != 0) {
			load(file, properties);
			differenceAnalyzer.backup();
			System.out.println("Backup complete.");
			System.exit(0);
		}
		
		if ((option & HEAD_MASK) != 0) {
			load(file, properties);
			int version = Integer.parseInt(optionArg.get(HEAD_MASK));
			differenceAnalyzer.head(version);
			System.out.printf("Headed to version %d.\n", version);
			System.exit(0);
		}
		
		if ((option & STORE_MASK) != 0) {
			load(file, properties);
			differenceAnalyzer.store();
			System.out.println("Cleared extra versions.");
			System.exit(0);
		}
		
		if ((option & LIST_MASK) != 0) {
			load(file, properties);
			System.out.printf("Current version is %d.\n", differenceAnalyzer.getVersion());
			HashSet<Integer> list = differenceAnalyzer.list();
			System.out.printf("%d available version:\n", list.size());
			for (Integer integer : list) {
				System.out.println("\t" + integer);
			}
			System.exit(0);
		}
		
		switch (op) {
			case "init":
				try {
					File dataPath = new File(getDataPath());
					if (!dataPath.exists()) {
						dataPath.mkdirs();
					}
					if (!file.exists())
						file.createNewFile();
					properties.load(new FileInputStream(file));
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Fail to init config with path: " + configPath + ".");
					System.exit(1);
				}
				properties.setProperty("bucketName", "");
				properties.setProperty("region", "");
				properties.setProperty("secretId", "");
				properties.setProperty("secretKey", "");
				properties.store(new FileOutputStream(file), null);
				System.out.println("Init Complete.");
				return;
			case "put":
			case "get":
				load(file, properties);
				if ("put".equals(op)) {
					differenceAnalyzer.PUT((option & FORCE_MASK) != 0, (option & APPEND_MASK) != 0);
				} else if ("get".equals(op)) {
					differenceAnalyzer.GET((option & FORCE_MASK) != 0, (option & APPEND_MASK) != 0);
				}
				break;
		}
	}
	
	public static String getDataPath() {
		return getRuntimePath() + DEAFAULT_STORE_PATH + "/";
	}
	
	public static String getRuntimePath() {
		return runtimePath;
	}
	
	private static void load(File file, Properties properties) {
		if (!file.exists()) {
			System.out.println("Couldn't find config.");
			System.exit(1);
		}
		try {
			properties.load(new FileInputStream(file));
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Fail to load config with path: " + file.getAbsolutePath() + ".");
			System.exit(1);
		}
		bucketName = properties.getProperty("bucketName");
		region = properties.getProperty("region");
		secretId = properties.getProperty("secretId");
		secretKey = properties.getProperty("secretKey");
		if (bucketName == null || region == null || secretId == null || secretKey == null) {
			System.out.println("Fail to load config with path: " + file.getAbsolutePath() + ", because empty propertise.");
			System.exit(1);
		}
		cosUtil = new COSUtil(region, secretId, secretKey, bucketName);
		differenceAnalyzer = new DifferenceAnalyzer(cosUtil, getDataPath());
	}
	
	private static boolean loadArg(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("init") || args[i].equals("put") || args[i].equals("get")) {
				op = args[i];
			} else if (args[i].startsWith("-")) {
				String string = args[i];
				for (int j = 1; j < string.length(); j++) {
					char c = string.charAt(j);
					if (c == 'c') {
						option |= CONFIG_MASK;
						optionArg.put(CONFIG_MASK, args[++i]);
					} else if (c == 'f') {
						option |= FORCE_MASK;
					} else if (c == 'a') {
						option |= APPEND_MASK;
					} else if (c == 'v') {
						option |= VERSION_MASK;
					} else if (c == 'h') {
						option |= HELP_MASK;
					} else if (c == 'b') {
						option |= BACKUP_MASK;
					} else if (c == 'l') {
						option |= LIST_MASK;
					} else if (c == 'd') {
						option |= HEAD_MASK;
						optionArg.put(HEAD_MASK, args[++i]);
					} else if (c == 's') {
						option |= STORE_MASK;
					} else if (c == 't') {
						option |= TEST_MASK;
					} else {
						return false;
					}
				}
			} else {
				return false;
			}
		}
		return true;
	}
}
