package cn.wuxia.common.hibernate.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.SQLException;

import org.hibernate.Hibernate;
import org.springframework.util.Assert;

import cn.wuxia.common.entity.ValidationEntity;
import cn.wuxia.common.exception.AppDaoException;
import cn.wuxia.common.orm.query.Pages;
import cn.wuxia.common.orm.query.QueryUtil;
import cn.wuxia.common.util.StringUtil;

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
            // super.evict(entity);
            super.save(entity);
            // super.merge(entity);
            super.flush();
            super.clear();
        } catch (Exception e) {
            throw new AppDaoException(e);
        }
    }

    /**
     * Description of the method
     * 
     * @author songlin
     * @param sql
     * @param values
     * @return
     */
    protected int batchExecuteSql(final String sql, final Object... values) {
        return createSQLQuery(sql, values).executeUpdate();
    }

    /**
     * getEntityById
     * 
     * @author Cy.zhong
     * @param id
     * @return T class
     */
    public T getEntityById(final Serializable id) {
        return get(id);
    }

    /**
     * getEntityByIdForLog
     * 
     * @author Cy.zhong
     * @param id
     * @return
     */
    public T getEvictEntityById(final Serializable id) {
        T t = getEntityById(id);
        this.evict(t);
        return t;
    }

    /**
     * physically deleted
     * 
     * @author songlin.li
     * @param id
     */
    public void deleteEntityById(final Serializable id) throws AppDaoException {
        Assert.notNull(id, "id Can not be null");
        logger.debug("delete entity {}, id is {}", "", id);
        super.delete(id);
    }

    /**
     * physically deleted
     * 
     * @author songlin.li
     * @param entity
     */
    public void deleteByEntity(final T entity) throws AppDaoException {
        Assert.notNull(entity, "entity Can not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("delete entity {}", entity);
        }
        super.delete(entity);
    }

    /**
     * @param queryUtil
     * @author songlin.li
     * @return
     */
    public QueryUtil find(QueryUtil queryUtil) {
        if (StringUtil.isBlank(queryUtil.getQueryString()) || StringUtil.isBlank(queryUtil.getQueryType())) {
            return queryUtil;
        }

        boolean isHql = queryUtil.getQueryType().equals("hql");

        Object[] values = queryUtil.getQueryValues().toArray();
        queryUtil.combineQueryString();
        Pages<?> result = isHql ? this.findPage(queryUtil.getPages(), queryUtil.getQueryString(), values)
                : this.findPageBySql(queryUtil.getPages(), queryUtil.getQueryString(), values);

        queryUtil.setPages(result);
        return queryUtil;
    }

}
