package cn.wuxia.common.hibernate;

import cn.wuxia.common.orm.query.Conditions;
import cn.wuxia.common.orm.query.MatchType;
import cn.wuxia.common.spring.orm.core.jpa.JpaRestrictionBuilder;
import cn.wuxia.common.spring.orm.core.jpa.specification.SpecificationModel;
import cn.wuxia.common.util.ArrayUtil;
import cn.wuxia.common.util.ListUtil;
import cn.wuxia.common.util.StringUtil;
import com.google.common.collect.Lists;
import org.springframework.data.jpa.domain.Specification;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * 实现spring data jpa的{@link Predicate}接口，通过该类支持{@link Conditions}
 * 以及表达式查询方法
 *
 * @param <T> orm对象
 * @author songlin.li
 */
public class ConditionsSpecification<T> {

    private List<Conditions> conditions = new ArrayList<Conditions>();

    public ConditionsSpecification() {

    }

    /**
     * 通过属性过滤器构建
     *
     * @param condition 属性过滤器
     */
    public ConditionsSpecification(Conditions condition) {
        this.conditions.add(condition);
    }

    /**
     * 通过属性过滤器集合构建
     *
     * @param conditions 集合
     */
    public ConditionsSpecification(List<Conditions> conditions) {
        if(ListUtil.isNotEmpty(conditions)) {
            this.conditions.addAll(conditions);
        }
    }

    /**
     * 通过属性过滤器集合构建
     *
     * @param conditions 集合
     */
    public ConditionsSpecification(Conditions... conditions) {
        if(ArrayUtil.isNotEmpty(conditions)) {
            this.conditions.addAll(Lists.newArrayList(conditions));
        }
    }

    /**
     * @param root
     * @param query
     * @param builder
     * @return
     */
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {

        List<Predicate> list = new ArrayList<Predicate>();

        for (Conditions condition : conditions) {
            /**
             * 除了is null or is not null 条件外，其他条件必须带值
             */
            if (condition.getMatchType() != MatchType.ISN && condition.getMatchType() != MatchType.INN) {
                if (StringUtil.isBlank(condition.getValue())) {
                    continue;
                }
            }
            list.add(JpaRestrictionBuilder.getRestriction(condition, new SpecificationModel(root, query, builder)));
        }

        return list.size() > 0 ? builder.and(list.toArray(new Predicate[list.size()])) : null;

    }

    public List<Conditions> getConditions() {
        return conditions;
    }

    public void setConditions(List<Conditions> conditions) {
        this.conditions = conditions;
    }
}
