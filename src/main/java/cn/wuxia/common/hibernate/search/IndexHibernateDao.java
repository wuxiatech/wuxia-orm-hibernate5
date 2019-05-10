/*
* Created on :2016年3月29日
* Author     :songlin
* Change History
* Version       Date         Author           Reason
* <Ver.No>     <date>        <who modify>       <reason>
* Copyright 2014-2020 武侠科技 All right reserved.
*/
package cn.wuxia.common.hibernate.search;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;

import cn.wuxia.common.orm.PageSQLHandler;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.transform.Transformers;
import org.springframework.util.Assert;

import cn.wuxia.common.entity.ValidationEntity;
import cn.wuxia.common.hibernate.dao.BasicHibernateDao;
import cn.wuxia.common.orm.query.Pages;
import cn.wuxia.common.util.ListUtil;

public class IndexHibernateDao<T extends ValidationEntity, PK extends Serializable> extends BasicHibernateDao<T, Serializable> {

    /**
     * 判断是否有需要索引
     * @author songlin
     * @return
     */
    private boolean hasIndex() {
        Indexed index = entityClass.getAnnotation(Indexed.class);
        return null != index;
    }

    /**
     * 查询索引表
     * @author songlin
     * @param property
     * @param value
     * @return
     */
    public List<T> queryIndex(String[] property, String value) {
        if (hasIndex()) {
            FullTextSession fullTextSession = Search.getFullTextSession(getSession());
            try {
                fullTextSession.createIndexer().startAndWait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            SearchFactory sf = fullTextSession.getSearchFactory();
            QueryBuilder qb = sf.buildQueryBuilder().forEntity(entityClass).get();
            org.apache.lucene.search.Query luceneQuery = qb.keyword().onFields(property).matching(value).createQuery();
            FullTextQuery hibQuery = fullTextSession.createFullTextQuery(luceneQuery, entityClass);
            return hibQuery.list();
        } else
            return null;
    }

    /**
     * query by HQL.
     * 
     * @param page
     * @param hql
     * @param values values Variable number of parameters, in order to bind.
     * @return Paging query results , with the list of results and all of the
     *         query input parameters.
     */
    public <X> Pages<X> findIndexPage(final Pages<X> page, final Class<X> clas, final String hql, final Object... values) {
        Assert.notNull(page, "page can not be null");
        List<Object> paramValue = ListUtil.arrayToList(values);
        String queryHql = PageSQLHandler.dualDynamicCondition(hql, page.getConditions(), paramValue);
        queryHql += appendOrderBy(queryHql, page.getSort());
        Query q = null;
        if (clas != null) {
            Entity entity = clas.getAnnotation(Entity.class);
            if (entity != null) {
                q = createIndexQuery(queryHql, clas, paramValue.toArray());
            } else {
                q = createIndexQuery(queryHql, paramValue.toArray());
                q.setResultTransformer(Transformers.aliasToBean(clas));
            }
        } else {
            q = createIndexQuery(queryHql, paramValue.toArray());
            q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        }
        setPageParameterToQuery(q, page);
        List<X> result = q.list();
        page.setResult(result);
        return page;
    }

    /**
     * query by HQL.
     * 
     * @param page
     * @param clas
     * @param sql
     * @param values values Variable number of parameters, in order to bind.
     * @return Paging query results , with the list of results and all of the
     *         query input parameters.
     */
    public <X> Pages<X> findIndexPageBySql(final Pages<X> page, final Class<X> clas, final String sql, final Object... values) {
        Assert.notNull(page, "page can not be null");
        List<Object> paramValue = ListUtil.arrayToList(values);
        String queryHql = PageSQLHandler.dualDynamicCondition(sql, page.getConditions(), paramValue);
        queryHql += appendOrderBy(queryHql, page.getSort());
        NativeQuery q = createIndexSQLQuery(queryHql, paramValue.toArray());
        setPageParameterToQuery(q, page);
        if (clas != null) {
            Entity entity = clas.getAnnotation(Entity.class);
            if (entity != null) {
                q.addEntity(clas);
            } else {
                q.setResultTransformer(Transformers.aliasToBean(clas));
            }
        } else {
            q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        }
        List<X> result = q.list();
        page.setResult(result);
        return page;
    }

    /**
     * 创建索引
     * @author songlin
     * @param hql
     * @param values
     * @return
     */
    protected Query createIndexQuery(String hql, Object... values) {
        Assert.hasText(hql, "queryString can not be null");
        if (hasIndex()) {
            //            Query query = getSession().createQuery(hql);
        }
        FullTextSession fullTextSession = Search.getFullTextSession(getSession());
        try {
            fullTextSession.createIndexer().startAndWait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Query query = fullTextSession.createQuery(hql);

        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                query.setParameter(i, values[i]);
            }
        }
        return query;
    }

    /**
     * 创建索引
     * @author songlin
     * @param hql
     * @param values
     * @return
     */
    protected Query createIndexQuery(String hql, Class clazz, Object... values) {
        Assert.hasText(hql, "queryString can not be null");
        if (hasIndex()) {
            //            Query query = getSession().createQuery(hql);
        }
        FullTextSession fullTextSession = Search.getFullTextSession(getSession());
        try {
            fullTextSession.createIndexer().startAndWait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Query query = fullTextSession.createQuery(hql, clazz);

        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                query.setParameter(i, values[i]);
            }
        }
        return query;
    }

    /**
     * 创建索引
     * @author songlin
     * @param sql
     * @param values
     * @return
     */
    protected NativeQuery createIndexSQLQuery(String sql, Object... values) {
        Assert.hasText(sql, "queryString can not be null");
        if (hasIndex()) {
            //            Query query = getSession().createQuery(hql);
        }
        FullTextSession fullTextSession = Search.getFullTextSession(getSession());
        try {
            fullTextSession.createIndexer().startAndWait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        NativeQuery query = fullTextSession.createNativeQuery(sql);

        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                query.setParameter(i + 1, values[i]);
            }
        }
        return query;
    }
}
