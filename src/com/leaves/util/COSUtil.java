package com.leaves.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.leaves.Main;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.Copy;
import com.qcloud.cos.transfer.Download;
import com.qcloud.cos.transfer.Transfer;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferProgress;
import com.qcloud.cos.transfer.Upload;

public class COSUtil {
	private COSClient cosClient = null;
	private String bucketName = null;
	private String region = null;
	
	public COSUtil(String region, String secretId, String secretKey, String bucketName) {
		this.bucketName = bucketName;
		this.region = region;
		COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
		ClientConfig clientConfig = new ClientConfig(new Region(region));
		this.cosClient = new COSClient(cred, clientConfig);
	}
	
	public String getBucketName() {
		return bucketName;
	}
	
	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}
	
	public void close() {
		this.cosClient.shutdown();
	}
	
	public void createKey(String key) {
		InputStream input = new ByteArrayInputStream(new byte[0]);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(0);

		PutObjectRequest putObjectRequest =
		new PutObjectRequest(bucketName, key, input, objectMetadata);
		cosClient.putObject(putObjectRequest);
	}
	
	public ArrayList<String> listAllObjects() {
		ArrayList<String> ret = new ArrayList<String>();
		ListObjectsRequest listObjectsRequest = new ListObjectsRequest();
		listObjectsRequest.setBucketName(bucketName);
		listObjectsRequest.setPrefix("");
		listObjectsRequest.setDelimiter("");
		listObjectsRequest.setMaxKeys(1000);
		ObjectListing objectListing = null;
		do {
		 try {
			 objectListing = cosClient.listObjects(listObjectsRequest);
		 } catch (CosServiceException e) {
			 e.printStackTrace();
			 return null;
		 } catch (CosClientException e) {
			 e.printStackTrace();
			 return null;
		 }
		 List<COSObjectSummary> cosObjectSummaries = objectListing.getObjectSummaries();
		 for (COSObjectSummary cosObjectSummary : cosObjectSummaries) {
			ret.add(cosObjectSummary.getKey());
		 }
		 String nextMarker = objectListing.getNextMarker();
		 listObjectsRequest.setMarker(nextMarker);
		} while (objectListing.isTruncated());
		return ret;
	}
	
	/*
	 * 删除单个文件(不带版本号, 即bucket未开启多版本)
	 */
   public void delSingleFile(String key) {
       try {
           cosClient.deleteObject(bucketName, key);
       } catch (CosServiceException e) { // 如果是其他错误, 比如参数错误， 身份验证不过等会抛出CosServiceException
           e.printStackTrace();
       } catch (CosClientException e) { // 如果是客户端错误，比如连接不上COS
           e.printStackTrace();
       }
   }
	
    // Prints progress while waiting for the transfer to finish.
   private static void showTransferProgress(Transfer transfer) {
       do {
           try {
               Thread.sleep(2000);
           } catch (InterruptedException e) {
               return;
           }
           TransferProgress progress = transfer.getProgress();
           long so_far = progress.getBytesTransferred();
           long total = progress.getTotalBytesToTransfer();
           makeProgress(so_far, total);
       } while (transfer.isDone() == false);
       System.out.println("State: " + transfer.getState());
   }
    
   private static void makeProgress(double so_far, double total) {
	   double ratio = 0;
	   if (total == 0 || so_far == total) {
		   ratio = 10;
	   } else {
		   ratio = so_far * 10 / (double) total;
	   }
	   System.out.print("[");
	   for (int i = 0; i < 10; i++) {
		   if (i < ratio) {
			   System.out.print("=");
		   } else {
			   System.out.print(" ");
		   }
	   }
	   System.out.print("]");
	   System.out.printf(" [%d / %d]\n", (int) so_far, (int) total);
   }
    
   /**
    * 上传文件, 根据文件大小自动选择简单上传或者分块上传
    */
   public void uploadFile(File file, String key) {
       ExecutorService threadPool = Executors.newFixedThreadPool(32);
       // 传入一个threadpool, 若不传入线程池, 默认TransferManager中会生成一个单线程的线程池
       TransferManager transferManager = new TransferManager(cosClient, threadPool);
       
       PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, file);
       try {
           // 返回一个异步结果Upload, 可同步的调用waitForUploadResult等待upload结束, 成功返回UploadResult, 失败抛出异常.
           long startTime = System.currentTimeMillis();
           Upload upload = transferManager.upload(putObjectRequest);
           showTransferProgress(upload);
           upload.waitForUploadResult();
           long endTime = System.currentTimeMillis();
           System.out.println("Used time: " + (endTime - startTime) / 1000 + " s");
       } catch (CosServiceException e) {
           e.printStackTrace();
       } catch (CosClientException e) {
           e.printStackTrace();
       } catch (InterruptedException e) {
           e.printStackTrace();
       }

       transferManager.shutdownNow();
   }

   /**
    * 将文件下载到本地
    */
   public void downloadFile(String key) {
       ExecutorService threadPool = Executors.newFixedThreadPool(32);
       // 传入一个threadpool, 若不传入线程池, 默认TransferManager中会生成一个单线程的线程池
       TransferManager transferManager = new TransferManager(cosClient, threadPool);

       GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
       try {
           // 返回一个异步结果copy, 可同步的调用waitForCompletion等待download结束, 成功返回void, 失败抛出异常.
           Download download = transferManager.download(getObjectRequest, new File(Main.getDataPath() + key));
           download.waitForCompletion();
       } catch (CosServiceException e) {
           e.printStackTrace();
       } catch (CosClientException e) {
           e.printStackTrace();
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
       transferManager.shutdownNow();
   }
    
   /**
    * copy接口支持根据文件大小自动选择copy或者分块copy
    * 同园区拷贝
    */
   public void copyFileForSameRegion(String srcKey, String destKey, String destBucketName) {
       ExecutorService threadPool = Executors.newFixedThreadPool(32);
       // 传入一个threadpool, 若不传入线程池, 默认TransferManager中会生成一个单线程的线程。
       TransferManager transferManager = new TransferManager(cosClient, threadPool);

       CopyObjectRequest copyObjectRequest = new CopyObjectRequest(new Region(region), bucketName, srcKey, destBucketName, destKey);
       try {
           Copy copy = transferManager.copy(copyObjectRequest);
           // 返回一个异步结果copy, 可同步的调用waitForCopyResult等待copy结束, 成功返回CopyResult, 失败抛出异常.
           copy.waitForCopyResult();
       } catch (CosServiceException e) {
           e.printStackTrace();
       } catch (CosClientException e) {
           e.printStackTrace();
       } catch (InterruptedException e) {
           e.printStackTrace();
       }
       transferManager.shutdownNow();
   }
}