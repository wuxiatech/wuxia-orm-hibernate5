/**
 * Copyright (c) 2005-2010 springside.org.cn Licensed under the Apache License,
 * Version 2.0 (the "License"); $Id: SimpleHibernateDao.java 1205 2010-09-09
 * 15:12:17Z calvinxiu $
 */
package cn.wuxia.common.hibernate.dao;

import cn.wuxia.common.util.ArrayUtil;
import cn.wuxia.common.util.ListUtil;
import cn.wuxia.common.util.MapUtil;
import cn.wuxia.common.util.reflection.ReflectionUtil;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.query.Query;
import org.hibernate.query.criteria.internal.OrderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Package Hibernate native API the DAO generic base class. Direct use in the
 * Service layer can also be extended to the generic DAO subclass, see the
 * comments of two constructors. The example of Petlinc reference Spring2.5
 * comes canceled HibernateTemplate, directly using Hibernate native APIs.
 *
 * @param <T>  The DAO operation of the object type
 * @param <PK> The type of the primary key
 * @author calvin
 */
@SuppressWarnings("unchecked")
public class SimpleHibernateDao<T, PK extends Serializable> {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected SessionFactory sessionFactory;

    protected Class<T> entityClass;

    /**
     * Dao layer subclass constructor for object type class defined by the
     * generic subclass. eg. public class UserDao extends
     * SimpleHibernateDao<User, Long>
     */
    public SimpleHibernateDao() {
        this.entityClass = ReflectionUtil.getSuperClassGenricType(getClass());
    }

    /**
     * Is used for the omitted Dao layer and Service layer directly General
     * SimpleHibernateDao constructor. Define object types Class constructor.
     * eg. SimpleHibernateDao<User, Long> userDao = new SimpleHibernateDao<User,
     * Long>(sessionFactory, User.class);
     */

    public SimpleHibernateDao(final SessionFactory sessionFactory, final Class<T> entityClass) {
        this.sessionFactory = sessionFactory;
        this.entityClass = entityClass;
    }

    /**
     * @description : get sessionFactory.
     */
    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * @description : when have
     * multiple SesionFactory of to reload this function in a
     * subclass.
     */
    @Autowired
    protected void setSessionFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * @description : get the current Session.
     */
    public Session getSession() {
        return sessionFactory.getCurrentSession();
    }

    /**
     * @description : Save the new or modified objects.
     */
    public void save(final T entity) {
        Assert.notNull(entity, "entity Can not be null");
        getSession().saveOrUpdate(entity);
        if (logger.isDebugEnabled()) {
            logger.debug("save entity: {}", entity);
        }
    }

    /**
     * 批量操作
     *
     * @param entitys
     * @author songlin
     */
    public void batchSave(final Collection<T> entitys) {
        Assert.notEmpty(entitys, "entity Can not be null");
        /**
         * java.lang.IllegalStateException: Transaction already active
         */
//        Transaction tx = getSession().beginTransaction();
        int i = 0;
        for (T entity : entitys) {
            getSession().saveOrUpdate(entity);
            if (i % 30 == 0) { //单次批量操作的数目为30
                getSession().flush(); //清理缓存，执行批量插入20条记录的SQL insert语句
                getSession().clear(); //清空缓存中的对象
            }
            i++;
        }
        getSession().flush();
        getSession().clear();
//        tx.commit();
        //getSession().close();
        if (logger.isDebugEnabled()) {
            logger.debug("save entity: {}", entitys);
        }
    }

    /**
     * @param entity
     * @description : Description of the method
     * @author songlin.li
     */
    public void merge(final T entity) {
        Assert.notNull(entity, "entity Can not be null");
        getSession().merge(entity);
        if (logger.isDebugEnabled()) {
            logger.debug("save entity: {}", entity);
        }
    }

    /**
     * @param entity The object must be an object in the session or the
     *               transient object with id attribute.
     * @description : Delete objects.
     */
    public void delete(final T entity) {
        Assert.notNull(entity, "entity Can not be null");
        getSession().delete(entity);
        if (logger.isDebugEnabled()) {
            logger.debug("delete entity: {}", entity);
        }
    }

    /**
     * @description : Delete an object by id.
     */
    public void delete(final PK id) {
        Assert.notNull(id, "id Can not be null");
        delete(get(id));
    }

    /**
     * @description : Get the object by id.
     */
    public T get(final PK id) {
        Assert.notNull(id, "id Can not be null");
        return (T) getSession().get(entityClass, id);
    }

    /**
     * @description : Id list to get a list of objects.
     */
    public List<T> get(final Collection<PK> ids) {
        return findIn(getIdName(), ids);
    }

    /**
     * @description : Get all the objects.
     */
    public List<T> getAll() {
        return find();
    }

    /**
     * @description : Get all objects support line sequence by attribute.
     */
    public List<T> getAll(String orderByProperty, boolean isAsc) {
        CriteriaQuery crq = createCriteriaQuery();
        Root<T> root = crq.from(entityClass);
        crq.select(root);
        if (isAsc) {
            crq.orderBy(new OrderImpl(root.get(orderByProperty)));
        } else {
            crq.orderBy(new OrderImpl(root.get(orderByProperty), false));
        }
        return find(crq);
    }

    /**
     * @description : Find a list of objects by attributes match the way for
     * equal.
     */
    public List<T> findBy(final String propertyName, final Object value) {
        Assert.hasText(propertyName, "propertyName Can not be null");
        CriteriaQuery crq = createCriteriaQuery();
        Root<T> root = crq.from(entityClass);
        crq.select(root);
        crq.where(createCriteriaBuilder().equal(root.get(propertyName), value));
        return getSession().createQuery(crq).getResultList();
    }

    /**
     * @param propertyName
     * @param values
     * @return
     * @description : Find a list of objects by attributes match the way for in.
     * @author songlin
     */
    public List<T> findIn(final String propertyName, final Collection<?> values) {
        Assert.hasText(propertyName, "propertyName Can not be null");
        CriteriaQuery crq = createCriteriaQuery();
        Root<T> root = crq.from(entityClass);
        crq.select(root);
        CriteriaBuilder.In in = createCriteriaBuilder().in(root.get(propertyName));
        values.forEach(value -> in.value(value));
        crq.where(in);
        return find(crq);
    }

    /**
     * @param propertyName
     * @param values
     * @return
     * @description : Find a list of objects by attributes match the way for in.
     * @author songlin
     */
    public List<T> findIn(final String propertyName, final Object... values) {
        Assert.hasText(propertyName, "propertyName Can not be null");
        Assert.notEmpty(values, "values must have one value");
        if (values.length == 1) {
            return findBy(propertyName, values[0]);
        } else {
            return findIn(propertyName, ListUtil.arrayToList(values));
        }
    }

    /**
     * @param propertyName
     * @param values
     * @return
     * @description : Find a list of objects by attributes match the way for
     * notin.
     * @author songlin
     */
    public List<T> findNotIn(final String propertyName, final Collection<?> values) {
        Assert.hasText(propertyName, "propertyName Can not be null");
        CriteriaQuery crq = createCriteriaQuery();
        Root<T> root = crq.from(entityClass);
        crq.select(root);
        CriteriaBuilder.In in = createCriteriaBuilder().in(root.get(propertyName));
        values.forEach(value -> in.value(value));
        crq.where(createCriteriaBuilder().not(in));
        return find(crq);
    }

    /**
     * @param propertyName
     * @param values
     * @return
     * @description : Find a list of objects by attributes match the way for
     * notin.
     * @author songlin
     */
    public List<T> findNotIn(final String propertyName, final Object... values) {
        return findNotIn(propertyName, ListUtil.arrayToList(values));
    }

    /**
     * @description : Find a list of objects by attributes, matching equal find
     * only objects by attributes, matching equal.
     */
    public T findUniqueBy(final String propertyName, final Object value) {
        Assert.hasText(propertyName, "propertyName Can not be null");
        CriteriaQuery crq = createCriteriaQuery();
        Root<T> root = crq.from(entityClass);
        crq.select(root);
        crq.where(createCriteriaBuilder().equal(root.get(propertyName), value));

        return (T) getSession().createQuery(crq).uniqueResult();
    }

    /**
     * @param values A variable number of parameters, in order binding.
     * @description : HQL query object list.
     */
    protected <X> List<X> find(final String hql, final Object... values) {
        Query<X> query = createQuery(hql, values);
        return query.list();
    }

    /**
     * @param values Named parameters, bind by name.
     * @description : HQL query object list.
     */
    protected <X> List<X> find(final String hql, final Map<String, ?> values) {
        Query<X> query = createQuery(hql, values);
        return query.list();
    }

    /**
     * @param values A variable number of parameters, in order binding.
     * @description : The HQL queries a unique object.
     */
    protected <X> X findUnique(final String hql, final Object... values) {
        return (X) createQuery(hql, values).uniqueResult();
    }

    /**
     * @param values Named parameters, bind by name.
     * @description : The HQL queries a unique object.
     */
    protected <X> X findUnique(final String hql, final Map<String, ?> values) {
        return (X) createQuery(hql, values).uniqueResult();
    }

    /**
     * FIXME songlin.li add 调用executeUpdate方法如果service报错无法回滚
     *
     * @param values A variable number of parameters, in order binding.
     * @return Updating the number of records.
     * @description : Execute HQL bulk modify / delete operations.
     */
    protected int batchExecute(final String hql, final Object... values) {
        return createQuery(hql, values).executeUpdate();
    }

    /**
     * FIXME songlin.li add 调用executeUpdate方法如果service报错无法回滚
     *
     * @param values Named parameters, bind by name.
     * @return Updating the number of records.
     * @description : Execute HQL bulk modify / delete operations.
     */
    protected int batchExecute(final String hql, final Map<String, ?> values) {
        return createQuery(hql, values).executeUpdate();
    }

    /**
     * Based on the query HQL parameter list to create a Query object. And find
     * () function can be more flexible operation.
     *
     * @param values Variable number of parameters, in order to bind.
     */
    protected <X> Query<X> createQuery(final String hql, final Object... values) {
        Assert.hasText(hql, "queryString can not be null");
        Query<X> query = getSession().createQuery(hql);
        if (ArrayUtil.isNotEmpty(values)) {
            for (int i = 0; i < values.length; i++) {
                query.setParameter(i, values[i]);
            }
        }
        return query;
    }

    /**
     * Based on the query HQL parameter list to create a Query object. And find
     * () function can be more flexible operation.
     *
     * @param values Named parameters, bind by name.
     */
    protected <X> Query<X> createQuery(final String hql, final Map<String, ?> values) {
        Assert.hasText(hql, "queryString Can not be null");
        Query<X> query = getSession().createQuery(hql);
        if (MapUtil.isNotEmpty(values)) {
            query.setProperties(values);
        }
        return query;
    }

    /**
     * Based on the query HQL parameter list to create a Query object. And find
     * () function can be more flexible operation.
     *
     * @param values Variable number of parameters, in order to bind.
     */
    protected <X> Query<X> createQuery(final String hql, final Class<X> clazz, final Object... values) {
        Assert.hasText(hql, "queryString can not be null");
        Query<X> query = getSession().createQuery(hql, clazz);
        if (ArrayUtil.isNotEmpty(values)) {
            for (int i = 0; i < values.length; i++) {
                query.setParameter(i, values[i]);
            }
        }
        return query;
    }

    /**
     * Based on the query HQL parameter list to create a Query object. And find
     * () function can be more flexible operation.
     *
     * @param values Named parameters, bind by name.
     */
    protected <X> Query<X> createQuery(final String hql, final Class<X> clazz, final Map<String, ?> values) {
        Assert.hasText(hql, "queryString Can not be null");
        Query<X> query = getSession().createQuery(hql, clazz);
        if (MapUtil.isNotEmpty(values)) {
            query.setProperties(values);
        }
        return query;
    }

    /**
     * @param criterions Variable number of Criterion.
     * @description : Criteria query object list.
     * instead of {@link #find(CriteriaQuery)}
     */
    @Deprecated
    public List<T> find(final Criterion... criterions) {
        return createCriteria(criterions).list();
    }


    /**
     * @param criterions Variable number of Criterion.
     * @description : Criteria query a unique object.
     * instead of {@link #findUnique(CriteriaQuery)}
     */
    @Deprecated
    public T findUnique(final Criterion... criterions) {
        return (T) createCriteria(criterions).uniqueResult();
    }

    /**
     * @param criterions Variable number of Criterion.
     * @description : Created under Criterion conditions with the find ()
     * function can be more flexible operation.
     * instead of @see {@link #createCriteriaQuery()}
     */
    @Deprecated
    public Criteria createCriteria(final Criterion... criterions) {
        Criteria criteria = getSession().createCriteria(entityClass);
        for (Criterion c : criterions) {
            criteria.add(c);
        }
        return criteria;
    }

    protected CriteriaBuilder createCriteriaBuilder() {
        return getSession().getCriteriaBuilder();
    }

    protected CriteriaQuery<T> createCriteriaQuery() {
        return createCriteriaBuilder().createQuery(entityClass);
    }

    /**
     * @param criteriaQuery
     * @description : Criteria query object list.
     */
    public List<T> find(final CriteriaQuery criteriaQuery) {
        return getSession().createQuery(criteriaQuery).list();
    }


    /**
     * @param criteriaQuery
     * @description : Criteria query a unique object.
     */
    public T findUnique(final CriteriaQuery criteriaQuery) {
        return (T) getSession().createQuery(criteriaQuery).uniqueResult();
    }

    /**
     * @description : Initialize the object. Use the load () method to get only
     * the object Proxy needs to be initialized before reached the
     * View layer. Incoming entity, only initialize the entity
     * directly attribute, but it will not initializelazy
     * associations collections and properties For initialization
     * associated attributes, to be performed:
     * Hibernate.initialize(user.getRoles ()), direct
     * initialization User attributes and associated collection.
     * Hibernate.initialize(user.getDescription())，Direct
     * initialization User properties and delay the loading of the
     * Description property.
     */
    public void initProxyObject(Object proxy) {
        Hibernate.initialize(proxy);
    }

    /**
     * @description : Flush Current Session.
     */
    public void flush() {
        getSession().flush();
    }

    public void clear() {
        getSession().clear();
    }

    public void evict(final T entity) {
        getSession().evict(entity);
    }

    /**
     * @description : Add Criteria distinct transformer. HQL pre-load the
     * associated object will cause duplication of the main object,
     * the need for a distinct processing.
     */
    @Deprecated
    public Criteria distinct(Criteria criteria) {
        criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
        return criteria;
    }

    /**
     * @description : Get the primary key of the object name.
     */
    public String getIdName() {
        ClassMetadata meta = getSessionFactory().getClassMetadata(entityClass);
        return meta.getIdentifierPropertyName();
    }

    /**
     * @description : Judgment object attribute values ​​in the database is
     * unique. Modify the object scenarios, if the attribute value
     * (value) of the newly amended compare equal are not the
     * property of the original value (orgValue).
     */
    public boolean isPropertyUnique(final String propertyName, final Object newValue, final Object oldValue) {
        if (newValue == null || newValue.equals(oldValue)) {
            return true;
        }
        Object object = findUniqueBy(propertyName, newValue);
        return (object == null);
    }
}
