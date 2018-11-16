package cn.wuxia.common.hibernate;

import java.sql.Types;

public class EnhancementDialect extends org.hibernate.dialect.MySQLDialect {

    public EnhancementDialect() {
        super();
        registerColumnType(Types.NULL, "null");
        registerHibernateType(Types.NULL, "null");
    }

}
