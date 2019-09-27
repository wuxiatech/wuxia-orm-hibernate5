package cn.wuxia.common.hibernate.dao;

import cn.wuxia.common.entity.ValidationEntity;
import cn.wuxia.common.exception.AppDaoException;
import cn.wuxia.common.exception.ValidateException;
import cn.wuxia.common.util.ListUtil;
import cn.wuxia.common.util.StringUtil;
import com.google.common.collect.Lists;
import org.hibernate.Hibernate;
import org.springframework.util.Assert;

import java.io.*;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * [ticket id] Description of the class
 *
 * @author songlin.li @ Version : V<Ver.No> <Oct 26, 2012>
 */
public class BasicHibernateDao<T extends ValidationEntity, PK extends Serializable> extends SupportHibernateDao<T, Serializable> {

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
     * rewrite for service partner business
     *
     * @see cn.wuxia.common.hibernate.dao.SimpleHibernateDao#save(java.lang.Object)
     */
    public void saveEntity(T entity) throws AppDaoException {
        try {
            entity.validate();
            super.save(entity);
        } catch (Exception e) {
            throw new AppDaoException(e);
        }
//            super.flush();
//            super.clear();
    }

    public void saveEntity(Collection<T> entitys) throws AppDaoException {
        List<String> ex = Lists.newArrayListWithCapacity(1);
        for (T entity : entitys) {
            try {
                entity.validate();
            } catch (ValidateException e) {
                ex.add(e.getMessage());
            }
        }
        if (ListUtil.isNotEmpty(ex)) {
            throw new AppDaoException(StringUtil.join(ex, "/r/n"));
        }
        super.batchSave(entitys);
    }

    /**
     * Description of the method
     *
     * @param sql
     * @param values
     * @return
     * @author songlin
     */
    protected int batchExecuteSql(final String sql, final Object... values) {
        return createSQLQuery(sql, values).executeUpdate();
    }

    /**
     * getEntityById
     *
     * @param id
     * @return T class
     * @author Cy.zhong
     */
    public T getEntityById(final Serializable id) {
        return get(id);
    }

    /**
     * physically deleted
     *
     * @param id
     * @author songlin.li
     */
    public void deleteEntityById(final Serializable id) throws AppDaoException {
        Assert.notNull(id, "id Can not be null");
        logger.debug("delete entity {}, id is {}", "", id);
        super.delete(id);
    }

    /**
     * physically deleted
     *
     * @param entity
     * @author songlin.li
     */
    public void deleteByEntity(final T entity) throws AppDaoException {
        Assert.notNull(entity, "entity Can not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("delete entity {}", entity);
        }
        super.delete(entity);
    }


}
