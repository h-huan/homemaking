package com.hm.homemaking.domain;

public class HmMiniNavConfig
{
    private Long navId;
    private Long orgId;
    private String navName;
    private String navIcon;
    private String navPath;
    private Integer sortNo;
    private String status;

    public Long getNavId()
    {
        return navId;
    }

    public void setNavId(Long navId)
    {
        this.navId = navId;
    }

    public Long getOrgId()
    {
        return orgId;
    }

    public void setOrgId(Long orgId)
    {
        this.orgId = orgId;
    }

    public String getNavName()
    {
        return navName;
    }

    public void setNavName(String navName)
    {
        this.navName = navName;
    }

    public String getNavIcon()
    {
        return navIcon;
    }

    public void setNavIcon(String navIcon)
    {
        this.navIcon = navIcon;
    }

    public String getNavPath()
    {
        return navPath;
    }

    public void setNavPath(String navPath)
    {
        this.navPath = navPath;
    }

    public Integer getSortNo()
    {
        return sortNo;
    }

    public void setSortNo(Integer sortNo)
    {
        this.sortNo = sortNo;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }
}
