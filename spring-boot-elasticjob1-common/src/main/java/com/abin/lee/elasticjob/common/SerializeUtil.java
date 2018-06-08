package com.abin.lee.elasticjob.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class SerializeUtil {

	private static byte[] nullType = new byte[]{0};
	private static boolean isNullType(byte[] bytes){
		if(bytes == null || 
				(bytes.length==1&&bytes[0]==0)){
			return true;
		}
		return false;
	}
	
	/**
	 * 对null做处理，用于缓存
	 * @param object
	 * @return
	 */
	public static byte[] serializeTransNull(Object object) {
		if(object == null){
			return nullType;
		}
		return serialize(object);
	}
	
	/**
	 * 对null做处理，用于缓存
	 * @param object
	 * @return
	 */
	public static Object unserializeTransNull(byte[] bytes) {
		if(isNullType(bytes)){
			return null;
		}
		return unserialize(bytes);
	}
		
	/**
	 * 序列化
	 * 
	 * @param object
	 * @return
	 */
	public static byte[] serialize(Object object) {
		if(object == null){
			return null;
		}
		ObjectOutputStream oos = null;
		ByteArrayOutputStream baos = null;
		try {
			// 序列化
			baos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			byte[] bytes = baos.toByteArray();
			return bytes;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 反序列化
	 * 
	 * @param bytes
	 * @return
	 */
	public static Object unserialize(byte[] bytes) {
		ByteArrayInputStream bais = null;
		if(bytes==null || bytes.length==0){
			return null;
		}
		try {
			// 反序列化
			bais = new ByteArrayInputStream(bytes);
			ObjectInputStream ois = new ObjectInputStream(bais);
			return ois.readObject();
		} catch (Exception e) {
			e.printStackTrace();

		}
		return null;
	}
	
	public static void main(String[] args) {
		
	}
}
