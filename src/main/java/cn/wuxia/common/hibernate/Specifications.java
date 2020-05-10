package cn.wuxia.common.hibernate;

import cn.wuxia.common.orm.query.Conditions;
import cn.wuxia.common.spring.orm.core.PropertyFilter;
import cn.wuxia.common.spring.orm.core.RestrictionNames;
import cn.wuxia.common.spring.orm.core.jpa.specification.support.PropertyFilterSpecification;
import cn.wuxia.common.spring.orm.core.jpa.specification.support.PropertySpecification;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

/**
 * Specification工具类,帮助通过PropertyFilter和属性名创建Specification
 *
 * @author songlin.li
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Specifications {


    /**
     * 通过属性过滤器集合，创建Specification
     *
     * @param conditions 属性过滤器集合
     * @return {@link Specification}
     */
    public static ConditionsSpecification get(Conditions... conditions) {
        return new ConditionsSpecification(conditions);
    }

    /**
     * 通过属性过滤器，创建Specification
     *
     * @param condition 属性过滤器
     * @return {@link Specification}
     */
    public static ConditionsSpecification get(Conditions condition) {
        return new ConditionsSpecification(condition);
    }

    /**
     * 通过类属性名称，创建Specification
     *
     * @param propertyName 属性名
     * @param value        值
     * @return {@link Specification}
     */
    public static ConditionsSpecification get(String propertyName, Object value) {
        return get(Conditions.eq(propertyName, value));
    }

}
