/*
* Created on :4 Aug, 2014
* Author     :songlin
* Change History
* Version       Date         Author           Reason
* <Ver.No>     <date>        <who modify>       <reason>
* Copyright 2014-2020 武侠科技 All right reserved.
*/
package cn.wuxia.common.entity;

import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * 
 * [ticket id]
 * Base64压缩UUID长度替换Hibernate原有UUID生成器
 * {@link http://my.oschina.net/noahxiao/blog/132277?p=1}
 * @author songlin
 * @ Version : V<Ver.No> <4 Aug, 2014>
 */
public class Base64UuidGenerator implements IdentifierGenerator {

    public abstract static class UuidUtils {

        public static String uuid() {
            UUID uuid = UUID.randomUUID();
            return uuid.toString();
        }

        public static String compressedUuid() {
            UUID uuid = UUID.randomUUID();
            return compressedUUID(uuid);
        }

        protected static String compressedUUID(UUID uuid) {
            byte[] byUuid = new byte[16];
            long least = uuid.getLeastSignificantBits();
            long most = uuid.getMostSignificantBits();
            long2bytes(most, byUuid, 0);
            long2bytes(least, byUuid, 8);
            String compressUUID = Base64.encodeBase64URLSafeString(byUuid);
            return compressUUID;
        }

        protected static void long2bytes(long value, byte[] bytes, int offset) {
            for (int i = 7; i > -1; i--) {
                bytes[offset++] = (byte) ((value >> 8 * i) & 0xFF);
            }
        }

        public static String compress(String uuidString) {
            UUID uuid = UUID.fromString(uuidString);
            return compressedUUID(uuid);
        }

        public static String uncompress(String compressedUuid) {
            if (compressedUuid.length() != 22) {
                throw new IllegalArgumentException("Invalid uuid!");
            }
            byte[] byUuid = Base64.decodeBase64(compressedUuid + "==");
            long most = bytes2long(byUuid, 0);
            long least = bytes2long(byUuid, 8);
            UUID uuid = new UUID(most, least);
            return uuid.toString();
        }

        protected static long bytes2long(byte[] bytes, int offset) {
            long value = 0;
            for (int i = 7; i > -1; i--) {
                value |= (((long) bytes[offset++]) & 0xFF) << 8 * i;
            }
            return value;
        }

        public static void main(String[] args) {
            System.out.println(UuidUtils.uuid().length());
            System.out.println(UuidUtils.compressedUuid().length());
        }
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor arg0, Object arg1) throws HibernateException {
        return UuidUtils.compressedUuid();
    }
}
