package cn.wuxia.common.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

/**
 * common
 * 
 * @author songlin.li
 */
public class IdEntity implements Serializable {
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1L;

    private String id;

    @GenericGenerator(name = "hibernate-uuid", strategy = "cn.wuxia.common.entity.Base64UuidGenerator")
    @GeneratedValue(generator = "hibernate-uuid")
    @Column(name = "ID", unique = true, nullable = false)
    @Id
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
