package cn.wuxia.common.hibernate.dao;

import org.hibernate.Hibernate;

import java.io.*;
import java.sql.SQLException;

/**
 * [ticket id] Description of the class
 *
 * @author songlin.li @ Version : V<Ver.No> <Oct 26, 2012>
 */
public class BasicHibernateDao<T, PK extends Serializable> extends SupportHibernateDao<T, Serializable> {

    public java.sql.Blob objectToBlob(Object obj) throws IOException {
        try {
            ByteArrayOutputStream saos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(saos);
            oos.writeObject(obj);
            oos.flush();
            return Hibernate.getLobCreator(getSession()).createBlob(saos.toByteArray());
        } catch (Exception e) {
            logger.error("", e.getMessage());
            return null;
        }
    }

    public Object blobToObject(java.sql.Blob desBlob) throws IOException, SQLException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(desBlob.getBinaryStream());
        Object obj = in.readObject();
        in.close();
        return obj;
    }

    /**
     * @param id
     * @return T class
     */
    public T findById(final Serializable id) {
        return get(id);
    }

}
