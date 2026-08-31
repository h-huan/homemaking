package com.hm.homemaking.mapper;

import com.hm.homemaking.domain.HmMiniHomeConfig;
import com.hm.homemaking.domain.HmMiniNavConfig;
import com.hm.homemaking.domain.HmPortalContent;
import java.util.List;

public interface HmConfigMapper
{
    List<HmMiniHomeConfig> selectHomeConfigList();

    int upsertHomeConfig(HmMiniHomeConfig config);

    List<HmMiniNavConfig> selectNavConfigList();

    int deleteAllNavConfig();

    int insertNavConfig(HmMiniNavConfig config);

    List<HmPortalContent> selectPortalContentList(HmPortalContent query);

    HmPortalContent selectPortalContentById(Long contentId);

    int insertPortalContent(HmPortalContent content);

    int updatePortalContent(HmPortalContent content);

    int deletePortalContentById(Long contentId);

    HmPortalContent selectMiniContentByType(String contentType);
}
