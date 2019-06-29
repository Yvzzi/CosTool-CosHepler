package com.qcloud.cos.demo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;

public class CreateDirectoryDemo {
	public static void createDirectory() {
		// 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials("AKIDJVUFm8MCsLdhW7bdXGm7E7lCSDnBhWQD", "qgOAqTzSo050iCWgdMvX7nIbgVIrAD2a");
        // 2 设置bucket的区域, COS地域的简称请参照 https://www.qcloud.com/document/product/436/6224
        ClientConfig clientConfig = new ClientConfig(new Region("ap-shenzhen-fsi"));
        // 3 生成cos客户端
        COSClient cosclient = new COSClient(cred, clientConfig);
        // bucket名称, 需包含appid
        String bucketName = "test-1259353497";
        
		String key = "yyy/";
		// 目录对象即是一个/结尾的空文件，上传一个长度为 0 的 byte 流
		InputStream input = new ByteArrayInputStream(new byte[0]);
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setContentLength(0);

		PutObjectRequest putObjectRequest =
		new PutObjectRequest(bucketName, key, input, objectMetadata);
		cosclient.putObject(putObjectRequest);
	}
	public static void main(String[] args) {
		createDirectory();
	}
}
