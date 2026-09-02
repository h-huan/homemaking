package com.hm.module.homemaking.service;

import com.hm.framework.common.enums.UserTypeEnum;
import com.hm.framework.security.core.util.SecurityFrameworkUtils;
import com.hm.module.homemaking.dal.HmRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.Map;

@Service("hmCustomerAccess")
public class CustomerAccess {
    private final HmRepository repo;
    public CustomerAccess(HmRepository repo) { this.repo=repo; }
    public long current() {
        var user=SecurityFrameworkUtils.getLoginUser();
        if (user==null || !UserTypeEnum.MEMBER.getValue().equals(user.getUserType()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录客户账号");
        long count=repo.jdbc().queryForObject("SELECT COUNT(*) FROM hm_customer_tenant r JOIN hm_customer c ON c.id=r.customer_id WHERE r.tenant_id=? AND r.customer_id=? AND r.status='ACTIVE' AND c.status='ACTIVE'", Long.class,repo.tenant(),user.getId());
        if(count!=1) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"客户关系不可用");
        return user.getId();
    }
    public Map<String,Object> own(String table,long id,boolean lock) {
        var row=repo.require(table,id,lock);
        if(HmRepository.number(row,"customer_id")!=current()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"记录不存在");
        return row;
    }
}
