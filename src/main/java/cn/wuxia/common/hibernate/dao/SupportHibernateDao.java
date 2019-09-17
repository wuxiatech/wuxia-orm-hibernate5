package cn.wuxia.common.hibernate.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.Entity;

import cn.wuxia.common.exception.AppServiceException;
import cn.wuxia.common.orm.PageSQLHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.*;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cn.wuxia.common.orm.query.*;
import cn.wuxia.common.util.*;
import cn.wuxia.common.util.reflection.ReflectionUtil;

/**
 * Package SpringSide extension of a Hibernate DAO generic base class extended
 * features including paging query , query by attribute filter criteria list.
 * Can use directly in the Service layer , can also be extended to the use of
 * the generic DAO subclass , see the comment of the two constructors.
 *
 * @param <T>  DAOOperation of the object type
 * @param <PK> Primary key type
 * @author songlin.li
 */
@SuppressWarnings("unchecked")
public class SupportHibernateDao<T, PK extends Serializable> extends SimpleHibernateDao<T, PK> {
    /**
     * For the constructor of the subclass of the Dao layer to obtain the object
     * type by the generic definition of the subclass Class. eg. public class
     * UserDao extends HibernateDao<User, Long>{ }
     */
    public SupportHibernateDao() {
        super();
    }

    /**
     * Used to omit the Dao layer , Service layer directly the the Universal
     * HibernateDao constructor . Object type defined in the constructor Class.
     * eg. HibernateDao<User, Long> userDao = new HibernateDao<User,
     * Long>(sessionFactory, User.class);
     */
    public SupportHibernateDao(final SessionFactory sessionFactory, final Class<T> entityClass) {
        super(sessionFactory, entityClass);
    }

    /**
     * get all data by sort
     *
     * @param sort
     * @return
     * @author songlin
     */

    public List<T> findAll(final Sort sort) {
        Criteria criteria = createCriteria();
        /**
         * set sort order
         */
        setSortOrder(criteria, sort);
        return criteria.list();
    }

    // -- Page query function --//

    /**
     * get all.
     */
    public Pages<T> findAll(final Pages<T> page) {
        return findPage(page);
    }

    /**
     * query by HQL.
     *
     * @param page
     * @param clazz
     * @param hql
     * @param values values Variable number of parameters, in order to bind.
     * @return Paging query results , with the list of results and all of the
     * query input parameters.
     */
    public <X> Pages<X> findPage(final Pages<X> page, final Class<X> clazz, final String hql, final Object... values) {
        Assert.notNull(page, "page can not be null");
        /**
         * 动态拼接参数
         */
        List<Object> paramValue = ListUtil.arrayToList(values);
        String queryHql = PageSQLHandler.dualDynamicCondition(hql, page.getConditions(), paramValue);
        if (page.isAutoCount()) {
            long totalCount = countHqlResult(queryHql, paramValue.toArray());
            page.setTotalCount(totalCount);
            if (totalCount == 0) {
                return page;
            }
        }

        queryHql += appendOrderBy(queryHql, page.getSort());

        Query<X> q = createQuery(queryHql, clazz, paramValue.toArray());

        setPageParameterToQuery(q, page);

        List<X> result = q.list();
        page.setResult(result);
        return page;
    }

    public <T> Pages<T> findPage(final Pages page, final String hql, final Object... values) {
        return findPage(page, entityClass, hql, values);
    }

    /**
     * query by HQL.
     *
     * @param page
     * @param hql
     * @param values Named parameters, bind by name.
     * @return Paging query results , with the list of results and all of the
     * query input parameters.
     */
    public <X> Pages<X> findPage(final Pages<X> page, final String hql, final Map<String, ?> values) {
        Assert.notNull(page, "page can not be null");
        /**
         * 动态拼接参数
         */
        Map<String, Object> paramValue = Maps.newHashMap(values);
        String queryHql = PageSQLHandler.dualDynamicCondition(hql, page.getConditions(), paramValue);
        if (page.isAutoCount()) {
            long totalCount = countHqlResult(queryHql, paramValue);
            page.setTotalCount(totalCount);
            if (totalCount == 0) {
                return page;
            }
        }

        queryHql += appendOrderBy(queryHql, page.getSort());

        Query<X> q = createQuery(queryHql, paramValue);

        setPageParameterToQuery(q, page);

        List<X> result = q.list();
        page.setResult(result);
        return page;
    }

    /**
     * query by Criteria.
     *
     * @param page
     * @param criterions
     * @return Paging query results. Comes with a list of results and all of the
     * query input parameters.
     */
    public <X> Pages<X> findPage(final Pages<X> page, final Criterion... criterions) {
        Assert.notNull(page, "page can not be null");

        //转换为Criterion

        Conditions[] conditions = ListUtil.listToArray(page.getConditions());
        Criterion[] cris = buildCriterion(conditions);
        if (ArrayUtils.isNotEmpty(criterions)) {
            cris = ArrayUtil.addAll(criterions, cris);
        }

        Criteria c = createCriteria(cris);
        if (page.isAutoCount()) {
            long totalCount = countCriteriaResult(c);
            page.setTotalCount(totalCount);
            if (totalCount == 0) {
                return page;
            }
        }
        setPageParameterToCriteria(c, page);

        List<X> result = c.list();
        page.setResult(result);
        return page;
    }

    /**
     * build the queryString to append the sort order by
     *
     * @param queryString
     * @param sort
     * @return
     * @author songlin
     */
    protected String appendOrderBy(String queryString, Sort sort) {
        String orderBy = "";
        if (sort != null) {
            Assert.doesNotContain(queryString, "order by",
                    "duplicate order by,hql already has the sort: " + StringUtil.substringAfter(queryString, "order by"));
            orderBy = " order by " + sort.toString();
        }
        return orderBy;
    }


    /**
     * Set the paging parameters to the Query object , the auxiliary function.
     */
    protected <X> void setPageParameterToQuery(Query<X> q, final Pages<?> page) {
        if (page.getPageSize() > 0) {
            // hibernate firstResult start with 0
            q.setFirstResult(page.getFirst() - 1);
            q.setMaxResults(page.getPageSize());
        }
    }

    /**
     * Set the paging parameters to the Criteria object , the auxiliary
     * function.
     */
    protected void setPageParameterToCriteria(Criteria c, final Pages<?> page) {
        if (page.getPageSize() > 0) {
            // hibernate firstResult start with 0
            c.setFirstResult(page.getFirst() - 1);
            c.setMaxResults(page.getPageSize());
        }
        if (page.getSort() != null) {
            setSortOrder(c, page.getSort());
        }
    }

    /**
     * set sort order to hibernate order
     *
     * @param c
     * @param sort
     * @return
     * @author songlin
     */
    protected void setSortOrder(Criteria c, final Sort sort) {
        Iterator<cn.wuxia.common.orm.query.Sort.Order> it = sort.iterator();
        while (it.hasNext()) {
            cn.wuxia.common.orm.query.Sort.Order order = it.next();
            if (order.isAscending()) {
                c.addOrder(Order.asc(order.getProperty()));
            }else {
                c.addOrder(Order.desc(order.getProperty()));
            }
        }
    }

    /**
     * set condition
     *
     * @param conditions
     * @return
     * @author songlin
     */
    public Criterion[] buildCriterion(final Conditions... conditions) {
        Criterion[] array = new Criterion[]{};
        if (ArrayUtil.isEmpty(conditions)) {
            return array;
        }
        for (Conditions condition : conditions) {
            if (StringUtil.isBlank(condition.getProperty())) {
                continue;
            }
            Criterion criterion = null;
            if (condition.getMatchType().compareTo(MatchType.BW) == 0) {

                criterion = buildCriterion(condition.getProperty(), condition.getMatchType(), condition.getValue(), condition.getAnotherValue());
            } else {
                if (condition.getValue() instanceof Object[]) {
                    criterion = buildCriterion(condition.getProperty(), condition.getMatchType(), (Object[]) condition.getValue());
                } else if (condition.getValue() instanceof List) {
                    criterion = buildCriterion(condition.getProperty(), condition.getMatchType(), ListUtil.listToArray((List) condition.getValue()));
                } else {
                    criterion = buildCriterion(condition.getProperty(), condition.getMatchType(), condition.getValue());
                }
            }
            if (null != criterion) {
                array = ArrayUtil.add(array, criterion);
            }
        }
        return array;
    }

    /**
     * Executing count query to obtain the total number of objects the HQL query
     * that can be obtained by this function can automatically handle simple hql
     * statement , the complexity of the hql query please write separately count
     * statement to query.
     */
    protected long countHqlResult(final String hql, final Object... values) {
        String countHql = prepareCountHql(hql);
        Object count = findUnique(countHql, values);
        return NumberUtil.toLong(count, 0L);
    }

    /**
     * Executing count query to obtain the total number of objects the HQL query
     * that can be obtained by this function can automatically handle simple hql
     * statement , the complexity of the hql query please write separately count
     * statement to query .
     */
    protected long countHqlResult(final String hql, final Map<String, ?> values) {
        String countHql = prepareCountHql(hql);
        Object count = findUnique(countHql, values);
        return NumberUtil.toLong(count, 0L);
    }


    private String prepareCountHql(String orgHql) {
        String fromHql = orgHql;
        // the select clause and order by clause will affect the count query for
        // simple exclusion.
        int start = StringUtils.indexOfIgnoreCase(fromHql, "from ");
        if (start < 0) {
            throw new AppServiceException("查询语句有误，缺少from： [" + orgHql + "]");
        }
        int groupby = StringUtils.indexOfIgnoreCase(fromHql, " group by ");
        if (groupby > 0) {
            throw new AppServiceException("查询语句有误，暂不支持group by，请使用sql查询：" + orgHql);
        }
        int end = StringUtils.indexOfIgnoreCase(fromHql, " order by ");

        if (end > 0) {
            return "select count(*) " + StringUtil.substring(fromHql, start, end);
        } else {
            return "select count(*) " + StringUtil.substring(fromHql, start);
        }
    }

    /**
     * Executing count query to obtain the total number of the Criteria query
     * object can be obtained .
     */
    protected long countCriteriaResult(final Criteria c) {
        CriteriaImpl impl = (CriteriaImpl) c;

        // get out Projection、ResultTransformer、OrderBy, after that clean Count
        Projection projection = impl.getProjection();
        ResultTransformer transformer = impl.getResultTransformer();

        List<CriteriaImpl.OrderEntry> orderEntries = null;
        try {
            orderEntries = (List) ReflectionUtil.getFieldValue(impl, "orderEntries");
            ReflectionUtil.setFieldValue(impl, "orderEntries", Lists.newArrayList());
        } catch (Exception e) {
            logger.error("Can not throw exception:{}", e.getMessage());
        }
        c.setProjection(Projections.rowCount());

        // query Count
        Object totalCountObject = c.uniqueResult();
        long totalCount = (totalCountObject != null) ? NumberUtil.toLong(totalCountObject) : 0;
        // set Projection,ResultTransformer and OrderBy
        c.setProjection(projection);

        if (projection == null) {
            c.setResultTransformer(CriteriaSpecification.ROOT_ENTITY);
        }
        if (transformer != null) {
            c.setResultTransformer(transformer);
        }
        try {
            ReflectionUtil.setFieldValue(impl, "orderEntries", orderEntries);
        } catch (Exception e) {
            logger.error("Can not throw exception:{}", e.getMessage());
        }

        return totalCount;
    }

    /**
     * count record
     *
     * @param sql
     * @param values
     * @return
     * @author songlin.li
     */
    protected long countSQLResult(String sql, Object... values) {
        long recordTotal;
        int classNameIndex = sql.toLowerCase().indexOf("from");
        if (classNameIndex == -1) {
            return 0;
        }else {
            sql = "select count(1) as count from (" + sql + ") orgi";
        }

        NativeQuery<Object> query = createSQLQuery(sql, values);

        recordTotal = NumberUtil.toLong(query.uniqueResult(), 0L);
        logger.debug("Total: " + recordTotal);
        return recordTotal;
    }

    /**
     * count record
     *
     * @param sql
     * @param values
     * @return
     * @author songlin.li
     */
    protected long countSQLResult(String sql, Map<String, ?> values) {
        long recordTotal;
        int classNameIndex = sql.toLowerCase().indexOf("from");
        if (classNameIndex == -1)
            return 0;
        else {
            sql = "select count(1) as count from (" + sql + ") orgi";
        }

        NativeQuery<Object> query = createSQLQuery(sql, values);
        recordTotal = NumberUtil.toLong(query.uniqueResult(), 0L);
        logger.debug("Total: " + recordTotal);
        return recordTotal;
    }

    protected <X> NativeQuery<X> createSQLQuery(final String sql, final Object... values) {
        Assert.hasText(sql, "queryString can not be null");
        NativeQuery<X> query = getSession().createNativeQuery(sql);
        if (ArrayUtils.isNotEmpty(values)) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    /**
                     * NativeQuery position start form 1 not 0
                     */
                    query.setParameter(i + 1, values[i]);
                }
            }
            logger.debug("values: {}", values);
        }
        return query;
    }

    protected <X> NativeQuery<X> createSQLQuery(final String sql, final Map<String, ?> values) {
        Assert.hasText(sql, "queryString can not be null");
        NativeQuery<X> query = getSession().createNativeQuery(sql);
        if (MapUtil.isNotEmpty(values)) {
            query.setProperties(values);
            logger.debug("values: {}", values);
        }
        return query;
    }

    protected <X> NativeQuery<X> createSQLQuery(final String sql, final Class<X> clazz, final Object... values) {
        Assert.hasText(sql, "queryString can not be null");
        NativeQuery<X> query = getSession().createNativeQuery(sql, clazz);
        if (ArrayUtils.isNotEmpty(values)) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    /**
                     * NativeQuery position start form 1 not 0
                     */
                    query.setParameter(i + 1, values[i]);
                }
            }
            logger.debug("values: {}", values);
        }
        return query;
    }

    protected <X> NativeQuery<X> createSQLQuery(final String sql, final Class<X> clazz, final Map<String, ?> values) {
        Assert.hasText(sql, "queryString can not be null");
        NativeQuery<X> query = getSession().createNativeQuery(sql, clazz);
        if (MapUtil.isNotEmpty(values)) {
            query.setProperties(values);
            logger.debug("values: {}", values);
        }
        return query;
    }

    /**
     * 根据sql查询返回Map
     *
     * @param sql
     * @param values
     * @return
     * @author songlin
     */
    protected List<Map<String, Object>> queryToMap(String sql, Object... values) {
        logger.debug("sql: " + sql);

        NativeQuery query = this.createSQLQuery(sql, values);
        query.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP);
        List<Map<String, Object>> result = query.list();

        logger.debug("size: " + result.size());
        return result;
    }

    /**
     * 根据sql查询返回Map
     *
     * @param sql
     * @param values
     * @return
     * @author songlin
     */
    @Deprecated
    protected List<Map<String, Object>> queryToMap(String sql, Map<String, ?> values) {
        logger.debug("sql: " + sql);
        NativeQuery query = this.createSQLQuery(sql, values);
        query.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP);
        List<Map<String, Object>> result = query.list();

        logger.debug("size: " + result.size());
        return result;
    }

    /**
     * get unique result by sql, if result is empty then return null
     *
     * @param sql
     * @param objs
     * @return
     * @author songlin.li
     */
    protected <X> X queryUnique(String sql, Class<X> clazz, Object... objs) {
        List<X> list = query(sql, clazz, objs);
        if (ListUtil.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * get unique result by sql, if result is empty then return null
     *
     * @param sql
     * @param objs
     * @return
     * @author songlin.li
     */
    protected <X> X queryUnique(String sql, Object... objs) {
        List<X> list = query(sql, objs);
        if (ListUtil.isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * sql query, return list
     *
     * @param sql
     * @param objs
     * @return List
     * @author Songlin.Li
     */
    protected <X> List<X> query(String sql, Object... objs) {
        logger.debug("sql: " + sql);
        NativeQuery<X> query = this.createSQLQuery(sql, objs);
        List<X> result = query.list();
        logger.debug("size: " + result.size());
        return result;
    }

    /**
     * sql query, return entity list
     *
     * @param sql
     * @param clazz
     * @return List
     * @author Songlin.Li
     */
    protected <X> List<X> query(String sql, Class<X> clazz, Object... values) {
        logger.debug("sql: " + sql);

        NativeQuery<X> query = null;
        if (clazz == null) {
            query = this.createSQLQuery(sql, values);
        } else {
            Entity entity = clazz.getAnnotation(Entity.class);
            if (entity != null) {
                query = this.createSQLQuery(sql, clazz, values);
            } else {
                query = this.createSQLQuery(sql, values);
                query.setResultTransformer(Transformers.aliasToBean(clazz));
            }
        }
        List<X> result = query.list();
        logger.debug("size: " + result.size());
        return result;
    }

    /**
     * @param sql
     * @param objs
     * @return
     * @author songlin.li
     */
    protected <X> List<X> query(String sql, Map<String, ?> objs) {
        NativeQuery<X> query = this.createSQLQuery(sql, objs);
        List<X> result = query.list();
        logger.debug("size: " + result.size());
        return result;
    }

    /**
     * @param sql
     * @param clazz
     * @param values
     * @return
     * @author songlin.li
     */
    protected <X> List<X> query(String sql, Class<X> clazz, Map<String, ?> values) {
        NativeQuery<X> query = null;
        if (clazz == null) {
            query = this.createSQLQuery(sql, values);
        } else {
            Entity entity = clazz.getAnnotation(Entity.class);
            if (entity != null) {
                query = this.createSQLQuery(sql, clazz, values);
            } else {
                query = this.createSQLQuery(sql, values);
                query.setResultTransformer(Transformers.aliasToBean(clazz));
            }
        }
        List<X> result = query.list();
        logger.debug("size: " + result.size());
        return result;
    }

    /**
     * same as createSQLQuery(sql, values).executeUpdate()
     *
     * @param values Variable number of parameters, in order to bind.
     */
    protected void queryUpdate(String sql, Object... values) {
        logger.debug("sql: " + sql);
        int result = this.createSQLQuery(sql, values).executeUpdate();
        logger.debug("size: " + result);

    }

    /**
     * same as createSQLQuery(sql, values).executeUpdate()
     *
     * @param values Named parameters, bind by name.
     */
    protected void queryUpdate(String sql, Map<String, ?> values) {
        logger.debug("sql: " + sql);
        int result = this.createSQLQuery(sql, values).executeUpdate();
        logger.debug("size: " + result);
    }

    /**
     * @param page
     * @param sql
     * @param values
     * @return
     * @author songlin.li
     */
    protected Pages<Map<String, Object>> findPageBySql(final Pages page, final String sql, final Object... values) {
        return findPageBySql(page, null, sql, values);
    }

    /**
     * 支持简单的Conditions 赋值查询，复杂sql请自行处理再调用本方法
     *
     * @param page
     * @param clas
     * @param sql
     * @param values
     * @return
     * @author songlin
     */
    protected <X> Pages<X> findPageBySql(final Pages<X> page, final Class<X> clas, final String sql, final Object... values) {
        int classNameIndex = sql.toLowerCase().indexOf("from");
        if (classNameIndex == -1)
            return null;
        Assert.notNull(page, "page can not be null");
        /**
         * 动态拼接参数
         */
        List<Object> paramValue = ListUtil.arrayToList(values);
        String querySql = PageSQLHandler.dualDynamicCondition(sql, page.getConditions(), paramValue);
        if (ListUtil.isNotEmpty(paramValue)) {
            logger.debug("array param:{}", ToStringBuilder.reflectionToString(values));
        }
        if (page.isAutoCount()) {
            long totalCount = countSQLResult(querySql, paramValue.toArray());
            page.setTotalCount(totalCount);
            if (totalCount == 0) {
                return page;
            }
        }
        querySql += appendOrderBy(querySql, page.getSort());
        NativeQuery q = null;
        if (clas != null) {
            Entity entity = clas.getAnnotation(Entity.class);
            if (entity != null) {
                q = createSQLQuery(querySql, clas, paramValue.toArray());
            } else {
                q = createSQLQuery(querySql, paramValue.toArray());
                q.setResultTransformer(Transformers.aliasToBean(clas));
            }
        } else {
            q = createSQLQuery(querySql, paramValue.toArray());
            q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        }
        setPageParameterToQuery(q, page);
        page.setResult(q.list());
        return page;
    }

    protected Pages<Map<String, Object>> findPageBySql(final Pages page, final String sql, final Map<String, ?> values) {
        return findPageBySql(page, null, sql, values);
    }

    protected <X> Pages<X> findPageBySql(final Pages<X> page, final Class<X> clas, final String sql, final Map<String, ?> values) {
        Assert.notNull(page, "page can not be null");

        int classNameIndex = sql.toLowerCase().indexOf("from");
        if (classNameIndex == -1)
            return null;
        /**
         * 动态拼接参数
         */
        Map<String, Object> paramValue = Maps.newHashMap(values);
        String querySql = PageSQLHandler.dualDynamicCondition(sql, page.getConditions(), paramValue);
        if (MapUtil.isNotEmpty(paramValue)) {
            logger.debug("map param:{}", ToStringBuilder.reflectionToString(values));
        }
        if (page.isAutoCount()) {
            long totalCount = countSQLResult(querySql, paramValue);
            page.setTotalCount(totalCount);
            if (totalCount == 0) {
                return page;
            }
        }
        querySql += appendOrderBy(querySql, page.getSort());

        Query q = null;
        if (clas != null) {
            Entity entity = clas.getAnnotation(Entity.class);
            if (entity != null) {
                q = createSQLQuery(querySql, clas, paramValue);
            } else {
                q = createSQLQuery(querySql, paramValue);
                q.setResultTransformer(Transformers.aliasToBean(clas));
            }
        } else {
            q = createSQLQuery(querySql, paramValue);
            q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        }
        setPageParameterToQuery(q, page);
        page.setResult(q.list());
        return page;
    }


    // -- PropertyFilter --//

    /**
     * @param propertyName
     * @param value
     * @param orderBy
     * @param isAsc
     * @return
     * @description : Find the list of Entity List by propertyName
     * @author songlin.li
     */
    public List<T> findBy(String propertyName, Object value, String orderBy, boolean isAsc) {
        Assert.hasText(propertyName, "propertyName Can not be null");
        Criterion criterion = Restrictions.eq(propertyName, value);
        Criteria c = createCriteria(criterion);
        c.add(criterion);

        if (isAsc) {
            c.addOrder(Order.asc(orderBy));
        } else {
            c.addOrder(Order.desc(orderBy));
        }

        return c.list();
    }

    /**
     * @param matchType matching mode,Currently supports the values ​​of
     *                  PropertyFilter's MatcheType enum.
     * @description : Find the list of Entity List by propertyName and support
     * for multiple matches .
     */
    public List<T> findBy(final String propertyName, final MatchType matchType, final Object... value) {

        Criterion criterion = buildCriterion(propertyName, matchType, value);
        return find(criterion);
    }

    /**
     * @param filters
     * @return
     * @description : Find a list of objects attribute to filter list of
     * conditions.
     */
    public List<T> find(List<PropertyFilter> filters) {
        Criterion[] criterions = buildCriterionByPropertyFilter(filters);
        return find(criterions);
    }

    /**
     * @param page
     * @param filters
     * @return
     * @description : Find an object attribute filtering condition list page.
     */
    public Pages<T> findPage(final Pages<T> page, final List<PropertyFilter> filters) {
        Criterion[] criterions = buildCriterionByPropertyFilter(filters);
        return findPage(page, criterions);
    }

    /**
     * @param propertyName
     * @param propertyValue
     * @param matchType
     * @return
     * @description :Criterion, the auxiliary function created by the property
     * condition parameters.
     * @author songlin
     */
    protected Criterion buildCriterion(final String propertyName, final MatchType matchType, final Object... propertyValue) {
        Assert.hasText(propertyName, "property Name Can not be empty");
        Criterion criterion = null;
        // based on MatchType create criterion
        switch (matchType) {
            case EQ:
                if (StringUtil.isNotBlank(propertyValue[0])) {
                    criterion = Restrictions.eq(propertyName, propertyValue[0]);
                }
                break;
            case NE:
                if (StringUtil.isNotBlank(propertyValue[0])) {
                    criterion = Restrictions.ne(propertyName, propertyValue[0]);
                }
                break;
            case ISN:
                criterion = Restrictions.isNull(propertyName);
                break;
            case INN:
                criterion = Restrictions.isNotNull(propertyName);
                break;
            case FL:
                /**
                 * if value is blank, ignore the condition
                 */
                if (StringUtil.isNotBlank(propertyValue[0])) {
                    criterion = Restrictions.like(propertyName, "" + propertyValue[0], MatchMode.ANYWHERE);
                }
                break;
            case LL:
                /**
                 * if value is blank, ignore the condition
                 */
                if (StringUtil.isNotBlank(propertyValue[0])) {
                    criterion = Restrictions.like(propertyName, "" + propertyValue[0], MatchMode.START);
                }
                break;
            case RL:
                /**
                 * if value is blank, ignore the condition
                 */
                if (StringUtil.isNotBlank(propertyValue[0])) {
                    criterion = Restrictions.like(propertyName, "" + propertyValue[0], MatchMode.END);
                }
                break;
            case LTE:
                if (StringUtil.isNotBlank(propertyValue[0])) {
                    criterion = Restrictions.le(propertyName, propertyValue[0]);
                }
                break;
            case LT:
                if (StringUtil.isNotBlank(propertyValue[0])) {
                    criterion = Restrictions.lt(propertyName, propertyValue[0]);
                }
                break;
            case GTE:
                if (StringUtil.isNotBlank(propertyValue[0])) {
                    criterion = Restrictions.ge(propertyName, propertyValue[0]);
                }
                break;
            case GT:
                if (StringUtil.isNotBlank(propertyValue[0])) {
                    criterion = Restrictions.gt(propertyName, propertyValue[0]);
                }
                break;
            case IN:
                criterion = Restrictions.in(propertyName, propertyValue);
                break;
            case NIN:
                criterion = Restrictions.not(Restrictions.in(propertyName, propertyValue));
                break;
            case BW:
                if (propertyValue[0] != null && propertyValue[1] != null) {
                    criterion = Restrictions.between(propertyName, propertyValue[0], propertyValue[1]);
                }
                break;
            default:
                logger.warn("unsupport matchType:{} yet!", matchType);
                break;
        }
        return criterion;
    }

    /**
     * @param filters
     * @return
     * @description : Criterion array of auxiliary functions according to the
     * attribute list of conditions to create.
     */
    protected Criterion[] buildCriterionByPropertyFilter(final List<PropertyFilter> filters) {
        List<Criterion> criterionList = new ArrayList<Criterion>();
        for (PropertyFilter filter : filters) {
            if (!filter.hasMultiProperties()) { // Only need to compare a
                // property.
                Criterion criterion = buildCriterion(filter.getPropertyName(), filter.getMatchType(), filter.getMatchValue());
                criterionList.add(criterion);
            } else {// Contain the need to compare multiple properties , or
                // processing.
                Disjunction disjunction = Restrictions.disjunction();
                for (String param : filter.getPropertyNames()) {
                    Criterion criterion = buildCriterion(param, filter.getMatchType(), filter.getMatchValue());
                    disjunction.add(criterion);
                }
                criterionList.add(disjunction);
            }
        }
        return criterionList.toArray(new Criterion[criterionList.size()]);
    }
}
