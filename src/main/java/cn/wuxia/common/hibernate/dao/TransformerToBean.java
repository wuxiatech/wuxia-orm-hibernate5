/*
* Created on :2016年8月4日
* Author     :songlin
* Change History
* Version       Date         Author           Reason
* <Ver.No>     <date>        <who modify>       <reason>
* Copyright 2014-2020 武侠科技 All right reserved.
*/
package cn.wuxia.common.hibernate.dao;

import java.util.List;

import org.hibernate.transform.ResultTransformer;

public class TransformerToBean implements ResultTransformer{

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = -3116596466030859560L;

    public TransformerToBean(Class resultClass) {

    }

    @Override
    public Object transformTuple(Object[] tuple, String[] aliases) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List transformList(List collection) {
        // TODO Auto-generated method stub
        return null;
    }

}
