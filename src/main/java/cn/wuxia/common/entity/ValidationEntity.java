package cn.wuxia.common.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Transient;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cn.wuxia.common.exception.ValidateException;
import cn.wuxia.common.util.StringUtil;
import cn.wuxia.common.util.ValidatorUtil;

/**
 * common
 * 
 * @author songlin.li
 */
public class ValidationEntity implements Serializable {

    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 1L;

    @JsonIgnore
    public void validate() throws ValidateException {
        ValidatorUtil.validate(this);
    }

    @Transient
    @JsonIgnore
    public List<String> getViolations() {
        return ValidatorUtil.getViolations(this);
    }

    @JsonIgnore
    @Transient
    public String getViolation(String separator) {
        return StringUtil.join(getViolations(), separator);
    }

    @Override
    public String toString() {
        Logger logger = LoggerFactory.getLogger(getClass());
        if (logger.isDebugEnabled()) {
            return ToStringBuilder.reflectionToString(this);
        } else {
            return this.getClass().getSimpleName() + "@toString is closed! looger level = " + logger.isDebugEnabled();
        }
    }

}
